**Regex Engine & Performance Optimization**
**• Refactored Separator Logic:** Replaced the old, manual separator list with a single, highly optimized atomic group: (?>[\p{Punct}\p{Space}]|§.)*. This universally matches any punctuation, whitespace, or Minecraft color code (§.) with superior performance.

**• Drastic Regex Size Reduction:** The character count for generated filters has been drastically reduced. A filter that was previously 900+ characters long is now consistently around 250 characters.

**• Implemented "Guardian" Lookahead:** Added a (?=.*[first].*[last]) positive lookahead to the start of each pattern. This checks for the presence of the word's first and last letters before attempting the expensive full match, providing a massive performance boost by allowing the engine to "fail fast".

**• Optimized Lazy Quantifier:** Moved to a single, lazy quantifier (*?) at the end of the main pattern, rather than after each individual letter. This improves matching efficiency and helps prevent false positives.

**• Zero-Width Word Boundaries:** Implemented zero-width lookarounds ((?<=...) and (?!...)) for "Normal" filters. This correctly enforces word boundaries (e.g., blocks sex but not essex) without consuming adjacent characters, preventing issues with text or color code "bleeding".

**• User Interface (UI) & Feature Enhancements
GUI Implementation:** Migrated the entire tool from a console-based script to a full Java Swing GUI. This provides a user-friendly interface with live-preview for input and output.

**• Runnable Jar Package:** The script is now packaged as a runnable .jar, allowing it to be executed with a simple double-click (no console commands required).

**• Interactive Toggles:** Configuration options that previously required editing and recompiling the code (like "Gender Variants" and "Show Titles") can now be toggled on/off instantly from the GUI.
