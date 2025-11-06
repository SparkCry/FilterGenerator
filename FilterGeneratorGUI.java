//====================================================================================================================
// Sparkkrye
// FilterGeneratorGUI
// Filter words up to 15 letters
// Includes gender variation (Spanish only)
// Note:
// This script automatically creates regular expressions for chat plugins like: (ChatFilter, VentureChat, SimpleChat, ChatSentry).
// Blocks words that contain gradients and rainbows
// The filters will be saved in FilteredWord.yml
//======================================================================================================================
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FilterGeneratorGUI extends JFrame {
    private static final String OUTPUT_FILE = "FilteredWords.yml";
    private static final String INPUT_FILE = "words.txt";

    private static final String SEP = "(?>[\\p{Punct}\\p{Space}]|§.)*";

    private static final String NORMAL_PREFIX_REGEX = "(?i)(?<=^|[^a-z0-9§])";
    private static final String NORMAL_SUFFIX_REGEX = "(?![a-z0-9§])";

    private static final String STRONG_PREFIX_REGEX = "(?i)";
    private static final String STRONG_BODY_PREFIX = "(?:";
    private static final String STRONG_BODY_SUFFIX = ")";
    
    private static final String STRONG_FINAL_REGEX_SEP = SEP + "?"; 
    private static final String COMMON_YAML_SUFFIX = ",§cx"; 


    private static final Map<Character, String> VARIANTS = new HashMap<>();
    static {
        VARIANTS.put('a', "aáÁäÄ"); VARIANTS.put('b', "bv"); VARIANTS.put('c', "ckçÇ");
        VARIANTS.put('d', "d"); VARIANTS.put('e', "eéÉëË3"); VARIANTS.put('f', "f");
        VARIANTS.put('g', "g9"); VARIANTS.put('h', "h"); VARIANTS.put('i', "i1!íl|");
        VARIANTS.put('j', "jJ"); VARIANTS.put('k', "kK"); VARIANTS.put('l', "l|!");
        VARIANTS.put('m', "m"); VARIANTS.put('n', "nñÑ"); VARIANTS.put('o', "o0óÓ");
        VARIANTS.put('p', "p"); VARIANTS.put('q', "q"); VARIANTS.put('r', "r");
        VARIANTS.put('s', "s$5"); VARIANTS.put('t', "t7"); VARIANTS.put('u', "uú");
        VARIANTS.put('v', "vb"); VARIANTS.put('w', "w"); VARIANTS.put('x', "x×");
        VARIANTS.put('y', "y"); VARIANTS.put('z', "z2");
    }

    private boolean isDirty = false;
    private enum Language { SPANISH, ENGLISH }
    private Language currentLanguage = Language.SPANISH;

    private boolean useQuotes = false;
    private int indentationLevel = 0;

    private JTextArea wordsTextArea;
    private JCheckBox enableGenderVariantsCheckbox;
    private JCheckBox enableTitlesCheckbox;
    private JButton saveFilesButton;
    private JButton languageButton;
    private JButton quoteButton;
    private JButton indentButton;
    
    private JTextPane outputTextPane; 
    private StyledDocument outputDoc;
    private Style styleDefault;
    private Style styleComment;
    private Style styleWarning;

    private static final Color COLOR_YML_COMMENT = new Color(0, 150, 136);
    private static final Color COLOR_WARNING = new Color(200, 100, 0);

    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel instructionLabel;
    
    private TitledBorder inputBorder;
    private TitledBorder outputBorder;
    private JPanel inputPanel;
    private JPanel outputPanel;
    
    private boolean isUpdatingFromLoad = false;

    public FilterGeneratorGUI() {
        initComponents();
        setupStyles();
        updateLanguage();
        loadExistingWords();
        setupSaveOnExit();
        setupSaveShortcut();
        setLocationRelativeTo(null);
        setVisible(true);
        updateOutputPreview();
    }
    
    private void initComponents() {
        titleLabel = new JLabel("", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        descriptionLabel = new JLabel();
        descriptionLabel.setHorizontalAlignment(JLabel.CENTER);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        instructionLabel = new JLabel();
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        wordsTextArea = new JTextArea();
        wordsTextArea.setLineWrap(true);
        wordsTextArea.setWrapStyleWord(true);
        JScrollPane wordsScrollPane = new JScrollPane(wordsTextArea);
        
        enableGenderVariantsCheckbox = new JCheckBox();
        enableTitlesCheckbox = new JCheckBox();
        
        saveFilesButton = new JButton();
        languageButton = new JButton();
        
        quoteButton = new JButton();
        indentButton = new JButton();

        outputTextPane = new JTextPane();
        outputTextPane.setEditable(false);
        outputDoc = outputTextPane.getStyledDocument();
        JScrollPane outputScrollPane = new JScrollPane(outputTextPane);
        
        setLayout(new BorderLayout(10, 10));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(titleLabel);
        titlePanel.add(descriptionLabel);
        titlePanel.add(instructionLabel);
        add(titlePanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        
        inputPanel = new JPanel(new BorderLayout(5, 5));
        inputBorder = BorderFactory.createTitledBorder("");
        inputPanel.setBorder(inputBorder);
        inputPanel.add(wordsScrollPane, BorderLayout.CENTER);
        
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(enableGenderVariantsCheckbox);
        optionsPanel.add(enableTitlesCheckbox);
        inputPanel.add(optionsPanel, BorderLayout.SOUTH);
        centerPanel.add(inputPanel);
        
        outputPanel = new JPanel(new BorderLayout(5, 5));
        outputBorder = BorderFactory.createTitledBorder("");
        outputPanel.setBorder(outputBorder);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);
        centerPanel.add(outputPanel);
        
        add(centerPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(saveFilesButton);
        buttonPanel.add(languageButton);
        buttonPanel.add(quoteButton);
        buttonPanel.add(indentButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        saveFilesButton.addActionListener(e -> saveFiles());
        languageButton.addActionListener(e -> toggleLanguage());
        
        quoteButton.addActionListener(e -> toggleQuotes());
        indentButton.addActionListener(e -> cycleIndentation());

        wordsTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateLive(); }
            @Override public void removeUpdate(DocumentEvent e) { updateLive(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
        enableGenderVariantsCheckbox.addActionListener(e -> { updateOutputPreview(); isDirty = true; });
        enableTitlesCheckbox.addActionListener(e -> { updateOutputPreview(); isDirty = true; });
        
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        setSize(800, 750); 
    }
    
    private void setupStyles() {
        styleDefault = outputTextPane.addStyle("Default", null);
        StyleConstants.setFontFamily(styleDefault, "Monospaced");
        StyleConstants.setFontSize(styleDefault, 12);
        StyleConstants.setForeground(styleDefault, outputTextPane.getForeground());

        styleComment = outputTextPane.addStyle("Comment", styleDefault);
        StyleConstants.setForeground(styleComment, COLOR_YML_COMMENT);
        StyleConstants.setItalic(styleComment, true);

        styleWarning = outputTextPane.addStyle("Warning", styleDefault);
        StyleConstants.setForeground(styleWarning, COLOR_WARNING);
        StyleConstants.setBold(styleWarning, true);
    }
    
    private void toggleLanguage() {
        currentLanguage = (currentLanguage == Language.SPANISH) ? Language.ENGLISH : Language.SPANISH;
        updateLanguage();
        updateOutputPreview(); 
    }
    
    private void updateLanguage() {
        Font descriptionFont = new Font("Arial", Font.BOLD, 12);
        Color descriptionColor = Color.DARK_GRAY;

        if (currentLanguage == Language.SPANISH) {
            setTitle("Generador de Filtros (Fuerte/Normal + Vista Previa)");
            titleLabel.setText("Generador de Filtros (Modo Fuerte/Normal)");
            descriptionLabel.setText("<html>Palabras Normales: 'sex' (no bloquea 'Essex')<br>Palabras Fuertes: '!bitch' (bloquea siempre)</html>");
            instructionLabel.setText("Escribe las palabras (usa '!' para formato fuerte)");
            inputBorder.setTitle("");
            outputBorder.setTitle("Salida (Vista previa en Vivo)");
            enableGenderVariantsCheckbox.setText("Variante de genero: puto = +puta");
            enableTitlesCheckbox.setText("Mostrar títulos (#palabra) sobre cada filtro");
            saveFilesButton.setText("Guardar Archivos");
            languageButton.setText("English");
        } else {
            setTitle("Filter Generator (Strong/Normal + Live Preview)");
            titleLabel.setText("Filter Generator (Strong/Normal Mode)");
            descriptionLabel.setText("<html>Normal Words: 'sex' (won't block 'Essex')<br>Strong Words: '!bitch' (always blocks)</html>");
            instructionLabel.setText("Enter Words (use '!' for strong format)");
            inputBorder.setTitle("");
            outputBorder.setTitle("Output (Live Preview)");
            enableGenderVariantsCheckbox.setText("Enable Gender Variants (Only Spanish)");
            enableTitlesCheckbox.setText("Show Titles (#word) Above Each Filter");
            saveFilesButton.setText("Save Files");
            languageButton.setText("Español");
        }

        descriptionLabel.setFont(descriptionFont);
        descriptionLabel.setForeground(descriptionColor);
        instructionLabel.setFont(descriptionFont);
        instructionLabel.setForeground(descriptionColor);

        outputBorder.setTitleFont(descriptionFont);
        outputBorder.setTitleColor(descriptionColor);
        outputBorder.setTitleJustification(TitledBorder.CENTER);

        updateQuoteButtonText();
        updateIndentButtonText();

        inputPanel.repaint();
        outputPanel.repaint();
    }
    
    private void setupSaveOnExit() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                promptToSaveAndExit();
            }
        });
    }
    
    private void promptToSaveAndExit() {
        if (!isDirty) {
            System.exit(0);
            return;
        }

        String title, message;
        String[] options;
        
        if (currentLanguage == Language.SPANISH) {
            title = "Cambios sin guardar";
            message = "¿Deseas guardar los cambios antes de salir?";
            options = new String[]{"Guardar", "No Guardar", "Cancelar"};
        } else {
            title = "Unsaved Changes";
            message = "Do you want to save your changes before exiting?";
            options = new String[]{"Save", "Don't Save", "Cancel"};
        }

        int choice = JOptionPane.showOptionDialog(
            this, message, title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null, options, options[0]
        );

        switch (choice) {
            case 0: saveFiles(); System.exit(0); break;
            case 1: System.exit(0); break;
            case 2: default: break;
        }
    }

    private void setupSaveShortcut() {
        JRootPane rootPane = getRootPane();
        int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask);
        
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(saveKeyStroke, "saveAction");
        actionMap.put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFiles();
            }
        });
    }
    
    private void clearOutput() {
        try {
            outputDoc.remove(0, outputDoc.getLength());
        } catch (Exception e) {}
    }

    private void appendToOutput(String msg, Style style) {
        try {
            outputDoc.insertString(outputDoc.getLength(), msg, style);
        } catch (Exception e) {}
    }

    private void toggleQuotes() {
        useQuotes = !useQuotes;
        updateQuoteButtonText();
        updateOutputPreview();
        isDirty = true;
    }

    private void cycleIndentation() {
        indentationLevel = (indentationLevel + 2) % 6; 
        updateIndentButtonText();
        updateOutputPreview();
        isDirty = true;
    }

    private void updateQuoteButtonText() {
        if (useQuotes) {
            String text = (currentLanguage == Language.SPANISH) ? "Modo: '...' (Prefijo)" : "Mode: '...' (Prefix)";
            quoteButton.setText(text);
        } else {
            String text = (currentLanguage == Language.SPANISH) ? "Modo: '- ...' (Comillas)" : "Mode: '- ...' (Quotes)";
            quoteButton.setText(text);
        }
    }

    private void updateIndentButtonText() {
        String text = (currentLanguage == Language.SPANISH) ? "Aplicar: " : "Indent: ";
        String spaces = (currentLanguage == Language.SPANISH) ? " espacios" : " spaces";
        indentButton.setText(text + indentationLevel + spaces);
    }

    private void updateLive() {
        if (!isUpdatingFromLoad) {
            SwingUtilities.invokeLater(() -> {
                isDirty = true;
                updateOutputPreview();
            });
        }
    }
    
    private void loadExistingWords() {
        File inputFile = new File(INPUT_FILE);
        clearOutput();
        if (inputFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
                isUpdatingFromLoad = true;
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                wordsTextArea.setText(sb.toString());
                isUpdatingFromLoad = false;
                isDirty = false;
            } catch (IOException e) {
                appendToOutput("Error loading words: " + e.getMessage() + "\n", styleWarning);
                isUpdatingFromLoad = false;
            }
        } else {
            createSampleWordsFile();
            loadExistingWords();
        }
    }
    
    private void createSampleWordsFile() {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(INPUT_FILE), StandardCharsets.UTF_8))) {
            bw.write("sex\n");
            bw.write("!bitch\n");
            bw.write("!pene\n");
            bw.write("puto\n");
        } catch (IOException e) {
            appendToOutput("Error creating sample file: " + e.getMessage() + "\n", styleWarning);
        }
    }
    
    private void updateOutputPreview() {
        clearOutput();
        java.util.List<String> filters = getFiltersList();
        
        if (filters.isEmpty() && wordsTextArea.getText().trim().isEmpty()) {
             String warningMsg = (currentLanguage == Language.SPANISH ? "⚠️ No hay palabras para procesar." : "⚠️ No words to process.");
             appendToOutput(warningMsg, styleWarning);
        } else {
            for (String line : filters) {
                if (line.trim().startsWith("#")) {
                    appendToOutput(line + "\n", styleComment);
                } else {
                    appendToOutput(line + "\n", styleDefault);
                }
            }
        }
    }
    
    private java.util.List<String> getFiltersList() {
        String[] lines = wordsTextArea.getText().split("\n");
        java.util.List<String> raw = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                raw.add(line);
            }
        }
        
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        
        boolean enableGenderVariants = enableGenderVariantsCheckbox.isSelected();
        boolean enableTitles = enableTitlesCheckbox.isSelected();
        String indent = " ".repeat(indentationLevel);

        java.util.List<String> words = enableGenderVariants ? addGenderVariantsPreserveBang(raw) : raw;
        java.util.List<String> entries = new ArrayList<>();
        
        for (String r : words) {
            boolean isStrong = r.startsWith("!");
            String clean = isStrong ? r.substring(1) : r;
            clean = clean.toLowerCase(Locale.ROOT);
            
            if (clean.isEmpty()) continue;
            
            String rawRegex = isStrong ? generateStrongRegex(clean) : generateNormalRegex(clean);
            
            if (isStrong) {
                rawRegex += STRONG_FINAL_REGEX_SEP;
            }

            if (enableTitles) {
                entries.add(indent + "# " + clean);
            }
            
            String finalLine;
            if (useQuotes) {
                finalLine = indent + "'" + rawRegex + COMMON_YAML_SUFFIX + "'";
            } else {
                finalLine = indent + "- " + rawRegex + COMMON_YAML_SUFFIX;
            }
            
            entries.add(finalLine);
        }
        
        return entries;
    }

    private void saveFiles() {
        clearOutput();
        
        String savingMsg = (currentLanguage == Language.SPANISH) ? "Guardando archivos..." : "Saving files...";
        appendToOutput(savingMsg + "\n", styleDefault);
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(INPUT_FILE), StandardCharsets.UTF_8))) {
            writer.write(wordsTextArea.getText());
            String savedWordsMsg = (currentLanguage == Language.SPANISH) ? "Palabras guardadas en " : "Input words saved to ";
            appendToOutput(savedWordsMsg + INPUT_FILE + "\n", styleDefault);
        } catch (IOException e) {
            String errorMsg = (currentLanguage == Language.SPANISH) ? "Error guardando palabras en " : "Error saving words to ";
            appendToOutput(errorMsg + INPUT_FILE + ": " + e.getMessage() + "\n", styleWarning);
            return;
        }
        
        java.util.List<String> entries = getFiltersList();
        if (entries.isEmpty()) {
            String noWordsMsg = (currentLanguage == Language.SPANISH) ? "⚠️ No se encontraron palabras. No se guardó nada en " : "⚠️ No words found. Nothing saved to ";
            appendToOutput(noWordsMsg + OUTPUT_FILE + "\n", styleWarning);
            isDirty = false;
            return;
        }

        try {
            writeFile(OUTPUT_FILE, "filters:", entries);
            String successMsg = (currentLanguage == Language.SPANISH) ? "\n¡Guardado exitoso en " : "\nSuccessfully saved to ";
            appendToOutput(successMsg + INPUT_FILE + " y " + OUTPUT_FILE + "\n", styleDefault);
            isDirty = false;
        } catch (IOException e) {
            String errorMsg = (currentLanguage == Language.SPANISH) ? "Error escribiendo en el archivo de salida: " : "Error writing to output file: ";
            appendToOutput(errorMsg + e.getMessage() + "\n", styleWarning);
        }
    }
    
    private static java.util.List<String> addGenderVariantsPreserveBang(java.util.List<String> input) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String s : input) {
            if (s.startsWith("!")) {
                String core = s.substring(1);
                set.add("!" + core);
                if (core.endsWith("o")) set.add("!" + core.substring(0, core.length()-1) + "a");
            } else {
                set.add(s);
                if (s.endsWith("o")) set.add(s.substring(0, s.length()-1) + "a");
            }
        }
        return new ArrayList<>(set);
    }

    private static String generateNormalRegex(String word) {
        char[] chars = word.toCharArray();
        if (chars.length == 0) return "";

        char firstChar = chars[0];
        char lastChar = chars[chars.length - 1];
        String firstVariantGroup = VARIANTS.getOrDefault(firstChar, Pattern.quote(Character.toString(firstChar)));
        String lastVariantGroup = VARIANTS.getOrDefault(lastChar, Pattern.quote(Character.toString(lastChar)));
        
        String precheck = "";
        if (chars.length > 1) {
            precheck = "(?=.*[" + firstVariantGroup + "].*[" + lastVariantGroup + "])";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(NORMAL_PREFIX_REGEX);
        if (chars.length > 1) {
            sb.append(precheck); 
        }
        sb.append(SEP);
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            String group = VARIANTS.getOrDefault(c, Pattern.quote(Character.toString(c)));
            sb.append("[").append(group).append("]+");
            sb.append(SEP);
        }
        sb.append("?");
        sb.append(NORMAL_SUFFIX_REGEX);
        return sb.toString();
    }

    private static String generateStrongRegex(String word) {
        char[] chars = word.toCharArray();
        if (chars.length == 0) return "";
        
        char firstChar = chars[0];
        char lastChar = chars[chars.length - 1];
        String firstVariantGroup = VARIANTS.getOrDefault(firstChar, Pattern.quote(Character.toString(firstChar)));
        String lastVariantGroup = VARIANTS.getOrDefault(lastChar, Pattern.quote(Character.toString(lastChar)));
        
        String precheck = "";
        if (chars.length > 1) {
            precheck = "(?=.*[" + firstVariantGroup + "].*[" + lastVariantGroup + "])";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(STRONG_PREFIX_REGEX);
        if (chars.length > 1) {
            sb.append(precheck); 
        }
        sb.append(STRONG_BODY_PREFIX);
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            String group = VARIANTS.getOrDefault(c, Pattern.quote(Character.toString(c)));
            sb.append("[").append(group).append("]+");
            if (i < chars.length - 1) {
                sb.append(SEP);
            }
        }
        sb.append(STRONG_BODY_SUFFIX);
        return sb.toString();
    }
    
    private static void writeFile(String filename, String header, java.util.List<String> entries) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            writer.write(header);
            writer.newLine();
            writer.newLine();
            for (String e : entries) {
                writer.write(e);
                writer.newLine();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new FilterGeneratorGUI());
    }
}
