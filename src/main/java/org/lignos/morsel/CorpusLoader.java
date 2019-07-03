/*
 * Copyright 2009-2019 Constantine Lignos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lignos.morsel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lignos.morsel.lexicon.Lexicon;
import org.lignos.morsel.lexicon.Word;

/**
 * The CorpusLoader class provides static methods for creating a lexicon from a corpus or wordlist.
 */
public class CorpusLoader {

  /**
   * Loads a wordlist file into a lexicon, returning null if the file could not be read. The
   * wordlist should contain one word per line, with any whitespace between the word and the count.
   * For example, a line could be as follows:
   *
   * <pre>500 dog</pre>
   *
   * Warnings are printed to stderr if duplicate words are found in the wordlist.
   *
   * @param wordListPath The path to the wordlist file.
   * @param charset Character set of the wordlist file.
   * @param verbose Whether to print information about the lexicon.
   * @return A lexicon representing the wordlist.
   * @throws IOException if the wordlist file could not be parsed.
   */
  public static Lexicon loadWordlist(
      final Path wordListPath, final Charset charset, final boolean verbose) throws IOException {
    Lexicon lex = new Lexicon();

    long typesLoaded = 0L;
    long tokensLoaded = 0L;

    // Open the word list and get each word
    try (final BufferedReader input = Files.newBufferedReader(wordListPath, charset)) {
      int lineNum = 0;
      String line;
      while ((line = input.readLine()) != null) {
        lineNum++;

        // Skip lines with only whitespace
        if (line.trim().length() == 0) {
          continue;
        }

        final Word word = parseWordlistEntry(line);
        if (word != null) {
          // Add the word, print a warning if it's a duplicate
          if (!lex.addWord(word)) {
            System.err.println("Warning: Duplicate word in wordlist: " + word.getText());
            continue;
          }

          // Add the token and type counts
          tokensLoaded += word.getCount();
          typesLoaded++;
        } else {
          throw new IOException(
              String.format(
                  "Could not parse line %d of wordlist file containing text:\n%s", lineNum, line));
        }
      }
    }

    if (verbose) {
      System.out.println(typesLoaded + " types loaded.");
      System.out.println(tokensLoaded + " tokens loaded.");
    }

    // Set the word frequencies in the lexicon
    lex.updateFrequencies();

    return lex;
  }

  /**
   * Parse a string into a word and its count and create a matching Word instance. Returns null if
   * the string could not be parsed.
   *
   * @param line The line to be parsed, which should be in the format of a word, any amount of
   *     whitespace, and the count of the word.
   * @return A Word parsed from the line.
   */
  @SuppressWarnings("StringSplitter")
  static Word parseWordlistEntry(String line) {
    // Parse the line, return null if parsing fails
    String[] parts = line.split("\\s");
    if (parts.length != 2) {
      return null;
    }

    // Parse the second item as a count
    try {
      long count = Long.parseLong(parts[0]);
      return new Word(parts[1], count, true);
    } catch (NumberFormatException e) {
      // Return null if the string could not be parsed
      return null;
    }
  }
}
