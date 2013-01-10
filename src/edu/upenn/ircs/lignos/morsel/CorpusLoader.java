/*******************************************************************************
 * Copyright (C) 2012 Constantine Lignos
 * 
 * This file is a part of MORSEL.
 * 
 * MORSEL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * MORSEL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MORSEL.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.upenn.ircs.lignos.morsel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;

/**
 * The CorpusLoader class provides static methods for creating a lexicon from
 * a corpus or wordlist.
 *
 */
public class CorpusLoader {

	/**
	 * Loads a wordlist file into a lexicon, returning null if the file could 
	 * not be read. The wordlist should contain one word per line, with any 
	 * whitespace between the word and the count. For example, a line could be 
	 * as follows:
	 * 500 dog
	 * Warnings are printed to stderr if duplicate words are found in the 
	 * wordlist, and status is printed to stdout every 50,000 word types that 
	 * are loaded.
	 * @param wordListPath The path to the wordlist file.
	 * @param encoding Encoding of the wordlist file.
	 * @param verbose Whether to print information about the lexicon.
	 * @return A lexicon representing the wordlist.
	 */
	public static Lexicon loadWordlist(String wordListPath, String encoding, boolean verbose) {
		try {			
			Lexicon lex = new Lexicon();
		    
			// Open the word list and get each word
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(wordListPath), encoding));
			String line;
			long typesLoaded = 0;
			long tokensLoaded = 0;
			while ((line = input.readLine()) != null) {
				Word word = parseWordlistEntry(line);
				if (word != null) {
					// Add the word, print a warning if it's a duplicate
					if(!lex.addWord(word)) {
						System.err.println("Warning: Duplicate word in wordlist: " + word.getText());
						continue;
					}
					
					// Add the token count
					tokensLoaded += word.getCount();
					
					// Update status every 50,000 words
					if (verbose && ++typesLoaded % 50000 == 0) {
						System.out.print("\r" + typesLoaded + " types loaded...");
					}
				}
			}
			//Clean up
			input.close();
			
			if (verbose) {
				System.out.println("\r" + typesLoaded + " types loaded.");
				System.out.println(tokensLoaded + " tokens loaded.");
			}
				
			// Set the word frequencies in the lexicon
			lex.updateFrequencies();
			
			return lex;
		}
		catch (IOException e) {
			// If the file could not be loaded, just return null for the lexicon
			return null;
		}
	}

	/**
	 * Parse a string into a word and its count and create a matching Word 
	 * instance. Returns null if the string could not be parsed.
	 * @param line The line to be parsed, which should be in the format of
	 * a word, any amount of whitespace, and the count of the word.
	 * @return A Word parsed from the line.
	 */
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
		}
		catch (NumberFormatException e) {
			// Return null if the string could not be parsed
			return null;
		}
	}
}
