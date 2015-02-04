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

import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;
import junit.framework.TestCase;

/**
 * Test basic wordlist loading capabilities.
 *
 */
public class CorpusLoaderTest extends TestCase {
	String wordListPath = "data/test/test_wordlist.txt";
	String overflowListPath = "data/test/test_overflowlist.txt";
	
	/**
	 * Test that entries are added to the lexicon when processing a wordlist.
	 */
	public void testloadWordlist() {
		Lexicon lex = CorpusLoader.loadWordlist(wordListPath, "ISO8859_1", false);
		assertNotNull(lex.getWord("a"));
		assertEquals(500, lex.getWord("a").getCount());
		assertNotNull(lex.getWord("rat"));
		assertEquals(200, lex.getWord("rat").getCount());
		assertEquals(43295, lex.getTokenCount());
	}
	
	/**
	 * Test parsing a single line into a Word object.
	 */
	public void testparseWordlistEntry() {
		assertEquals(new Word("at", 400, true), 
				CorpusLoader.parseWordlistEntry("400 at"));
	}

	/**
	 * Test that counts bigger than MAX_INT do not cause overflow.
	 */
	public void testloadWordlistOverflow() {
		Lexicon lex = CorpusLoader.loadWordlist(overflowListPath, "ISO8859_1", false);
		assertEquals(2147483648L, lex.getWord("biggerthanmaxint").getCount());
		assertEquals(2147526943L, lex.getTokenCount());
	}
}
