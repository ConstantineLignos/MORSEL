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

public class CorpusLoaderTest extends TestCase {
	String wordListPath = "data/test/test_wordlist.txt";
	
	public void testloadWordlist() {
		Lexicon lex = CorpusLoader.loadWordlist(wordListPath, "ISO8859_1", false);
		assertNotNull(lex.getWord("a"));
		assertEquals(500, lex.getWord("a").getCount());
		assertNotNull(lex.getWord("rat"));
		assertEquals(200, lex.getWord("rat").getCount());
	}
	
	public void testparseWordlistEntry() {
		assertEquals(new Word("at", 400, true), 
				CorpusLoader.parseWordlistEntry("400 at"));
	}

}
