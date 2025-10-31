//====================================================================================================================
//SparkCry
//FilterGenerator
//Regex Generator for plugins that support this format, supports rainbow hexadecimal colors.
//Filters words up to 15 letters, includes basic separators; if you know what you’re doing you can configure them.
//It includes letter variations and gender variations (for Spanish words).
//Note:
//This script automatically creates chat filters for Minecraft plugins (like ChatFilter, VentureChat, SimpleChat).
//It takes the words you write in words.txt and turns them into special regex filters that block those
//words — even if players try to hide them with colors, symbols, or rainbow text.
//
//When you run the script, it reads your list of words.txt, processes them,
//and saves the results in FilteredWords.yml, ready to use in your plugin
//=====================================================================================================================
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class FilterGenerator {
    private static final String OUTPUT_FILE = "FilteredWords.yml";
    private static final String INPUT_FILE = "words.txt";
    private static final String REPLACEMENT = "x";

    // Allowed colors and separators. Do not modify unless you know what you're doing.
    private static final String COLOR = "(?:§x(?=(?:§[0-9a-f]){6})(?:§[0-9a-f]){6}|§[0-9a-f])*";
    private static final String SEP = "[\\s._@\"':;()¿?=!&/\\^*\\-<>%]*?";

    // Prefixes/Suffixes to limit context. Do not modify unless you know what you're doing.
    private static final String PREFIX_NORMAL = "(?i)(?:^|[^a-z0-9§])";
    private static final String SUFFIX_NORMAL = "(?![a-z0-9§]), " + "§cx"; //Output text, you can only edit "§cx" (Do not touch the rest unless you know what you're doing)
    private static final String PREFIX_STRONG = "(?i)(?<![a-záéíóúüñ]{3})";
    private static final String SUFFIX_STRONG = "(?![a-záéíóúüñ]), " + "§cx"; //Output text, you can only edit "§cx" (Do not touch the rest unless you know what you're doing)

    // Variation map, you can expand if you want.
    private static final Map<Character, String> VARIANTS = new HashMap<>();
    static {
        VARIANTS.put('a', "aáÁäÄ");
        VARIANTS.put('b', "bv");
        VARIANTS.put('c', "ckçÇ");
        VARIANTS.put('d', "d");
        VARIANTS.put('e', "eéÉëË3");
        VARIANTS.put('f', "f");
        VARIANTS.put('g', "g9");
        VARIANTS.put('h', "h");
        VARIANTS.put('i', "i1!íl|");
        VARIANTS.put('j', "jJ");
        VARIANTS.put('k', "kK");
        VARIANTS.put('l', "l|!");
        VARIANTS.put('m', "m");
        VARIANTS.put('n', "nñÑ");
        VARIANTS.put('o', "o0óÓ");
        VARIANTS.put('p', "p");
        VARIANTS.put('q', "q");
        VARIANTS.put('r', "r");
        VARIANTS.put('s', "s$5");
        VARIANTS.put('t', "t7");
        VARIANTS.put('u', "uú");
        VARIANTS.put('v', "vb");
        VARIANTS.put('w', "w");
        VARIANTS.put('x', "x×");
        VARIANTS.put('y', "y");
        VARIANTS.put('z', "z2");
    }

    // Configurable flags
    private static boolean ENABLE_GENDER_VARIANTS = false; // Flag for gender variation (Spanish only)
    private static boolean ENABLE_TITLES = true; // Flag to enable titles (#word) in the file

    public static void main(String[] args) throws IOException {
        System.out.println("\u001B[92mGenerating Filters...\u001B[00m");
        List<String> raw = loadWords(INPUT_FILE);
        if (raw.isEmpty()) {
            System.out.println("⚠️ words.txt not found");
            return;
        }

        List<String> words = ENABLE_GENDER_VARIANTS ? addGenderVariantsPreserveBang(raw) : raw;

        List<String> entries = new ArrayList<>();

        for (String r : words) {
            boolean isStrong = r.startsWith("!");
            String clean = isStrong ? r.substring(1) : r;
            clean = clean.toLowerCase(Locale.ROOT);

            String regex = isStrong ? generateStrongRegex(clean) : generateNormalRegex(clean);

            if (ENABLE_TITLES) {
                entries.add("# " + (isStrong ? r.substring(1) : r));
            }
            entries.add("- " + regex);

            System.out.println("\u001B[94m#" + (isStrong ? r.substring(1) : r) + "\u001B[0m");
            System.out.println("\u001B[97m- " + regex + "\u001B[0m");
        }

        writeFile(OUTPUT_FILE, "filters:", entries);

        System.out.println("\u001B[92mNew words added to \u001B[92m" + OUTPUT_FILE + "\u001B[0m");
    }

    private static List<String> loadWords(String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            System.out.println("\u001B[91m" + filename + " does not exist, creating an example...\u001B[0m");
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                bw.write("# Passive words, for example: sex (Essex, Sussex, Sextant)\n");
                bw.write("# Offensive words: 'bitch' is blocked before any text\n");
                bw.write("# Passive words do not require symbols at the beginning\n");
                bw.write("# Offensive words require '!' before the word\n");
                bw.write("Passive\n!Hard\nsex\n!bitch");
            }
        }

        List<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) {
                l = l.trim();
                if (!l.isEmpty() && !l.startsWith("#")) words.add(l);
            }
        }
        return words;
    }

    // Adds gender variants but preserves '!' at the beginning if it exists
    private static List<String> addGenderVariantsPreserveBang(List<String> input) {
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

    // Generates regex exactly with the "normal" template
    private static String generateNormalRegex(String word) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX_NORMAL);
        sb.append(COLOR);

        for (char c : word.toCharArray()) {
            String group = VARIANTS.getOrDefault(c, Pattern.quote(Character.toString(c)));
            sb.append("[")
              .append(group)
              .append("]+")
              .append(COLOR)
              .append("(?:[§a-f0-9x]*[\\s._@\\-0o]*)*");
        }

        sb.append("(?![a-z0-9§]), §cx"); //Output text, you can only edit "§cx" (Do not touch the rest unless you know what you're doing)
        return sb.toString();
    }

    // Generates regex exactly with the "strong" template
    private static String generateStrongRegex(String word) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX_STRONG);

        for (char c : word.toCharArray()) {
            String group = VARIANTS.getOrDefault(c, Pattern.quote(Character.toString(c)));
            sb.append(COLOR)
              .append("[")
              .append(group)
              .append("]+")
              .append(COLOR)
              .append(SEP);
        }

        sb.append("(?![a-záéíóúüñ]), §cx"); //Output text, you can only edit "§cx" (Do not touch the rest unless you know what you're doing)
        return sb.toString();
    }

    private static void writeFile(String filename, String header, List<String> entries) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            writer.write(header);
            writer.newLine();
            writer.newLine();
            writer.write("# If you want to disable the '#Name' for each format, you can do so through the 'MAIN CONFIGURATION' in FilterGenerator.java");
            writer.newLine();
            for (String e : entries) {
                writer.write(e);
                writer.newLine();
            }
        }
    }
}
