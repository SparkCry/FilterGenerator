# -*- coding: utf-8 -*-
"""
generate_vc_filters_fast.py
High-performance VentureChat regex generator using lookahead-based matching.
Accepts color codes, separators, and letter variations.
"""

import os
import re

# --------------------------
# CONFIGURATION
# --------------------------
REPLACEMENT = "x"
INPUT_FILE = "words.txt"
OUTPUT_FILE = "generated_filters_fast.yml"

words = [
    "cow",
]

# --------------------------
# CONSOLE COLORS
# --------------------------
CYAN = "\033[96m"
MAGENTA = "\033[95m"
RESET = "\033[0m"

# --------------------------
# CHARACTER VARIANTS
# --------------------------
variants = {
    "a": "aAáÁàâäãåāǎăąȁȃǻǟȧạả",
    "b": "bBvVßƀƁɓʙ",
    "c": "cCçÇćĉċč",
    "d": "dDďđÐ",
    "e": "eEéÉèêëēĕėęě",
    "f": "fFƒ",
    "g": "gGĝğġģ",
    "h": "hHĥħ",
    "i": "iI1l|!íÍìïîįīǐĭỉị",
    "j": "jJĵ",
    "k": "kKķƙ",
    "l": "lLĺļľł",
    "m": "mM",
    "n": "nNñÑńņň",
    "o": "oO0òóôöõøōǒŏȯőȫ",
    "p": "pPρΡ",
    "q": "qQ",
    "r": "rRŕř",
    "s": "sSśŝşš",
    "t": "tTţťŧƫț",
    "u": "uUúÚùûüũūŭů",
    "v": "vVʋ",
    "w": "wWŵẁẃẅ",
    "x": "xX×",
    "y": "yYýÿŷ",
    "z": "zZźżž",
}

# --------------------------
# REGEX COMPONENTS
# --------------------------
COLOR = r"(?:§[a-f0-9x])*"
SEP = r"(?:[\s._@%\":;()¿?=!&/\^*_\-<>]*)"
PREFIX = r"(?<![A-Za-z0-9§])"
SUFFIX = r"(?![A-Za-z0-9§])"

# --------------------------
# COMPILED VARIANT MAP
# --------------------------
def build_variant_map():
    """Create precompiled substitution patterns for all characters."""
    table = {}
    for k, v in variants.items():
        chars = re.escape(v)
        table[k] = f"[{chars}]"
    return table

VARIANT_MAP = build_variant_map()

# --------------------------
# GENERATE FAST REGEX
# --------------------------
def generate_fast_regex(word):
    """
    Generate a compact regex using lookaheads.
    Example: word "gil" -> (?<![A-Za-z0-9§])(?=(?:.*g)(?:.*i)(?:.*l))pattern(?![A-Za-z0-9§])
    """
    # Convert each letter to its variant group
    parts = []
    for ch in word:
        key = ch.lower()
        group = VARIANT_MAP.get(key, re.escape(ch))
        # Accept separators and color codes between characters
        parts.append(f"{COLOR}{group}{COLOR}{SEP}?")

    body = "".join(parts)
    # Build regex with lookaheads to prevent heavy backtracking
    regex = f"{PREFIX}({COLOR}{body}{COLOR}){SUFFIX}"
    return regex

# --------------------------
# READ WORDS
# --------------------------
if os.path.isfile(INPUT_FILE):
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        words = [line.strip() for line in f if line.strip()]

# --------------------------
# GENERATE AND SAVE
# --------------------------
with open(OUTPUT_FILE, "w", encoding="utf-8") as out:
    out.write("filters:\n")
    for w in words:
        regex = generate_fast_regex(w)
        out.write(f"# {w}\n- {regex}, {REPLACEMENT}\n")

        # Colored preview in console
        print(f"{MAGENTA}#{w}{RESET}")
        print(f"{CYAN}- {regex}, {REPLACEMENT}{RESET}\n")

print(f"\n✅ Filters written to {OUTPUT_FILE} (optimized lookahead mode).\n")
