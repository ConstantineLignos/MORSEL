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
package edu.upenn.ircs.lignos.morsel.transform;

import java.util.Arrays;

import edu.upenn.ircs.lignos.morsel.lexicon.Word;

import junit.framework.TestCase;

/**
 * Test basic functions of the affix class
 *
 */
public class AffixTest extends TestCase{

	/**
	 * Test getAffixes for suffixes of a long word
	 */
	public void testBasicSuffixes() {
		Word w = new Word("hamburger", 1, true);
		String[] expected = {"", "urger", "rger", "ger", "er", "r"};
		assertTrue(Arrays.equals(expected, Affix.getAffixes(w, AffixType.SUFFIX)));
	}
		
	/**
	 * Test getAffixes for suffixes of a short word
	 */
	public void testShortSuffixes() {
		Word w = new Word("ha", 1, true);
		String[] expected = {};
		assertTrue(Arrays.equals(expected, Affix.getAffixes(w, AffixType.SUFFIX)));
	}
	
	/**
	 * Test getAffixes for suffixes of a word of the minimum stem length
	 */
	public void testOnlyNullSuffix() {
		Word w = new Word("ham", 1, true);
		String[] expected = {""};
		assertTrue(Arrays.equals(expected, Affix.getAffixes(w, AffixType.SUFFIX)));
	}
	
	/**
	 * Test getAffixes for prefixes of a long word
	 */
	public void testBasicPrefixes() {
		Word w = new Word("hamburger", 1, true);
		String[] expected = {"", "h", "ha", "ham", "hamb", "hambu"};
		assertTrue(Arrays.equals(expected, Affix.getAffixes(w, AffixType.PREFIX)));
	}
		
	/**
	 * Test getAffixes for prefixes of a short word
	 */
	public void testShortPrefixes() {
		Word w = new Word("ha", 1, true);
		String[] expected = {};
		assertTrue(Arrays.equals(expected, Affix.getAffixes(w, AffixType.PREFIX)));
	}
	
	/**
	 * Test getAffixes for prefixes of a word of the minimum stem length
	 */
	public void testOnlyNullPrefix() {
		Word w = new Word("ham", 1, true);
		String[] expected = {""};
		assertTrue(Arrays.equals(expected, Affix.getAffixes(w, AffixType.PREFIX)));
	}
	
	/**
	 * Test the pair exclusion criteria for combinations of prefixes
	 */
	public void testisBadAffixPairPrefix() {
		Affix dePrefix = new Affix("de", AffixType.PREFIX);
		Affix unPrefix = new Affix("un", AffixType.PREFIX);
		Affix ePrefix = new Affix("e", AffixType.PREFIX);
		Affix iPrefix = new Affix("i", AffixType.PREFIX);
		Affix nullPrefix = new Affix("", AffixType.PREFIX);
		Affix bePrefix = new Affix("be", AffixType.PREFIX);
		Affix abePrefix = new Affix("abe", AffixType.PREFIX);
		
		// Standard cases
		assertTrue(Affix.isBadAffixPair(ePrefix, dePrefix, AffixType.PREFIX));
		assertTrue(Affix.isBadAffixPair(bePrefix, dePrefix, AffixType.PREFIX));
		
		// Special cases- affix1 null
		assertFalse(Affix.isBadAffixPair(nullPrefix, dePrefix, AffixType.PREFIX));
		
		// Varying length relationships
		assertFalse(Affix.isBadAffixPair(dePrefix, unPrefix, AffixType.PREFIX));
		assertFalse(Affix.isBadAffixPair(iPrefix, dePrefix, AffixType.PREFIX));
		assertTrue(Affix.isBadAffixPair(bePrefix, abePrefix, AffixType.PREFIX));
	}

	/**
	 * Test the pair exclusion criteria for combinations of suffixes
	 */
	public void testisBadAffixPairSuffix() {
		Affix edSuffix = new Affix("ed", AffixType.SUFFIX);
		Affix eSuffix = new Affix("e", AffixType.SUFFIX);
		Affix iSuffix = new Affix("i", AffixType.SUFFIX);
		Affix ingSuffix = new Affix("ing", AffixType.SUFFIX);
		Affix nullSuffix = new Affix("", AffixType.SUFFIX);
		Affix leSuffix = new Affix("le", AffixType.SUFFIX);
		Affix lySuffix = new Affix("ly", AffixType.SUFFIX);
		Affix lysSuffix = new Affix("lys", AffixType.SUFFIX);
		
		
		// Standard cases
		assertTrue(Affix.isBadAffixPair(eSuffix, edSuffix, AffixType.SUFFIX));
		assertTrue(Affix.isBadAffixPair(leSuffix, lySuffix, AffixType.SUFFIX));
		
		// Special cases- affix1 null
		assertFalse(Affix.isBadAffixPair(ingSuffix, edSuffix, AffixType.SUFFIX));
		
		// Varying length relationships
		assertFalse(Affix.isBadAffixPair(nullSuffix, edSuffix, AffixType.SUFFIX));
		assertFalse(Affix.isBadAffixPair(iSuffix, edSuffix, AffixType.SUFFIX));
		assertTrue(Affix.isBadAffixPair(leSuffix, lysSuffix, AffixType.SUFFIX));
	}
	
	/**
	 * Test hasAffix for prefixes and suffixes
	 */
	public void testhasAffixBasic() {
		// Note that it checks for min stem length, so short things are always
		// false
		Affix nullSuffix = new Affix("", AffixType.SUFFIX);
		assertFalse(Affix.hasAffix("a", nullSuffix));
		assertFalse(Affix.hasAffix("ab", nullSuffix));
		assertTrue(Affix.hasAffix("abc", nullSuffix));
		assertTrue(Affix.hasAffix("abcd", nullSuffix));
		
		Affix nullPrefix = new Affix("", AffixType.PREFIX);
		assertFalse(Affix.hasAffix("a", nullPrefix));
		assertFalse(Affix.hasAffix("ab", nullPrefix));
		assertTrue(Affix.hasAffix("abc", nullPrefix));
		assertTrue(Affix.hasAffix("abcd", nullPrefix));
		
		Affix eSuffix = new Affix("e", AffixType.SUFFIX);
		assertFalse(Affix.hasAffix("a", eSuffix));
		assertFalse(Affix.hasAffix("e", eSuffix));
		assertFalse(Affix.hasAffix("ae", eSuffix));
		assertTrue(Affix.hasAffix("aaae", eSuffix));
		assertFalse(Affix.hasAffix("eaad", eSuffix));
		
		Affix ePrefix = new Affix("e", AffixType.PREFIX);
		assertFalse(Affix.hasAffix("a", ePrefix));
		assertFalse(Affix.hasAffix("e", ePrefix));
		assertFalse(Affix.hasAffix("ea", ePrefix));
		assertFalse(Affix.hasAffix("abcde", ePrefix));
		assertTrue(Affix.hasAffix("eaaa", ePrefix));
	}
	
	/**
	 * Test hasAffix in cases where the affix can be longer than the string
	 */
	public void testhasAffixBounds() {
		// Test cases where the affixes push word boundaries
		Affix bePrefix = new Affix("be", AffixType.PREFIX);
		assertFalse(Affix.hasAffix("", bePrefix));
		assertFalse(Affix.hasAffix("b", bePrefix));
		assertFalse(Affix.hasAffix("be", bePrefix));
		assertFalse(Affix.hasAffix("bea", bePrefix));
		assertFalse(Affix.hasAffix("bear", bePrefix));
		assertTrue(Affix.hasAffix("bears", bePrefix));
		
		Affix earsSuffix = new Affix("rs", AffixType.SUFFIX);
		assertFalse(Affix.hasAffix("", earsSuffix));
		assertFalse(Affix.hasAffix("s", earsSuffix));
		assertFalse(Affix.hasAffix("rs", earsSuffix));
		assertFalse(Affix.hasAffix("ars", earsSuffix));
		assertFalse(Affix.hasAffix("ears", earsSuffix));
		assertTrue(Affix.hasAffix("bears", earsSuffix));
	}
}
