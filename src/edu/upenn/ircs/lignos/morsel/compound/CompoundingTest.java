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
package edu.upenn.ircs.lignos.morsel.compound;

import java.util.LinkedList;
import java.util.List;

import edu.upenn.ircs.lignos.morsel.CorpusLoader;
import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;
import edu.upenn.ircs.lignos.morsel.lexicon.WordSet;
import edu.upenn.ircs.lignos.morsel.transform.Affix;
import edu.upenn.ircs.lignos.morsel.transform.AffixType;
import edu.upenn.ircs.lignos.morsel.transform.Transform;

import junit.framework.TestCase;

/**
 * Test Compounding and Hypothesis
 *
 */
public class CompoundingTest extends TestCase{
	Lexicon lex;
	
	public void setUp() {
		lex = CorpusLoader.loadWordlist("data/test/compounding_test_eng.txt", "ISO8859_1", false);
	}
	
	/**
	 * Test getPrefixes without filler
	 */
	public void testgetPrefixes() {
		// Set the min compound length to zero
		Compounding.MIN_COMPOUND_LENGTH = 0;
		
		// With allowFull off, the whole word isn't returned
		List<Word> result = Compounding.getPrefixes("applesauce", lex, false, 
				null, null, false);
		assertTrue(result.contains(lex.getWord("a")));
		assertTrue(result.contains(lex.getWord("ap")));
		assertTrue(result.contains(lex.getWord("app")));
		assertTrue(result.contains(lex.getWord("apple")));
		assertTrue(result.contains(lex.getWord("apples")));
		assertEquals(5, result.size());
		
		result = Compounding.getPrefixes("app", lex, false, null, null, false);
		assertTrue(result.contains(lex.getWord("a")));
		assertTrue(result.contains(lex.getWord("ap")));
		assertEquals(2, result.size());
		
		// With allowFull on, the whole word is returned
		result = Compounding.getPrefixes("applesauce", lex, true, null, null, false);
		assertTrue(result.contains(lex.getWord("a")));
		assertTrue(result.contains(lex.getWord("ap")));
		assertTrue(result.contains(lex.getWord("app")));
		assertTrue(result.contains(lex.getWord("apple")));
		assertTrue(result.contains(lex.getWord("apples")));
		assertTrue(result.contains(lex.getWord("applesauce")));
		assertEquals(6, result.size());
		
		result = Compounding.getPrefixes("app", lex, true, null, null, false);
		assertTrue(result.contains(lex.getWord("a")));
		assertTrue(result.contains(lex.getWord("ap")));
		assertTrue(result.contains(lex.getWord("app")));
		assertEquals(result.size(), 3);
		
		// Test it at 4
		Compounding.MIN_COMPOUND_LENGTH = 4;
		
		// Check the compound length limit
		// With allowFull on, the whole word is returned
		result = Compounding.getPrefixes("applesauce", lex, true, null, null, false);
		assertTrue(result.contains(lex.getWord("apple")));
		assertTrue(result.contains(lex.getWord("apples")));
		assertTrue(result.contains(lex.getWord("applesauce")));
		assertEquals(3, result.size());
	}
	
	/**
	 * Test getPrefixes with filler
	 */
	public void testgetPrefixesFiller() {
		Transform plural = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("s", AffixType.SUFFIX));
		Transform weird = new Transform(new Affix("e", AffixType.SUFFIX), 
				new Affix("ey", AffixType.SUFFIX));
		Transform pre = new Transform(new Affix("", AffixType.PREFIX), 
				new Affix("pre", AffixType.PREFIX));
		List<Transform> learnedTransforms = new LinkedList<Transform>();
		learnedTransforms.add(plural);
		learnedTransforms.add(pre);
		learnedTransforms.add(weird);
		Compounding.Filler filler = new Compounding.Filler(learnedTransforms);
		
		// Move "sausage" to base so it will be used in compounds
		lex.moveWord(lex.getWord("sausage"), WordSet.BASE);
		
		List<Word> result = Compounding.getPrefixes("sausagesmarket", lex, false, 
				filler, null, false);
		assertTrue(result.contains(new Word("sausages", 0, false)));
		assertTrue(result.contains(new Word("sausage", 0, false)));
		assertEquals(2, result.size());
		
		result = Compounding.getPrefixes("sausageymarket", lex, false, filler, null, false);
		assertTrue(result.contains(new Word("sausage", 0, false)));
		assertTrue(result.contains(new Word("sausagey", 0, false)));
		assertEquals(2, result.size());
		
	}
	
	/**
	 * Do an end-to-end compounding test on a small lexicon without filler
	 */
	public void testinferCompoundsQuick() {
		Compounding.breakCompounds(lex, WordSet.UNMODELED, null, null, false, false,
				false, null);
		// Check that the analysis of applesauce is now compound and is APPLE
		// SAUCE instead of APPLES AUCE, which checks the frequency heuristic
		assertEquals("APPLE SAUCE", lex.getWord("applesauce").analyze());
	}
	
	/**
	 * Do an end-to-end compounding test on the Brown corpus without filler
	 */
	public void testinferCompoundsBrown() {
		lex = CorpusLoader.loadWordlist("data/test/brown_wordlist.txt", "ISO8859_1", false);
		Compounding.breakCompounds(lex, WordSet.UNMODELED, null, null, false, false,
				false, null);
		// Check that the analysis of shorthand is now compound
		assertEquals("SHORT HAND", lex.getWord("shorthand").analyze());
	}
	
	/**
	 * Do an end-to-end compounding test on the Brown corpus using filler
	 */
	public void testinferCompoundsBrownFiller() {
		// Load the Brown corpus
		lex = CorpusLoader.loadWordlist("data/test/brown_wordlist.txt", "ISO8859_1", false);
		
		// Set up bake-baker properly, so that baker is in derived and thus
		// can have rules applied to it when splitting compounds
		Transform agentive = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("er", AffixType.SUFFIX));
		agentive.addWordPair(lex.getWord("bake"), lex.getWord("baker"), true);		
		lex.moveTransformPairs(agentive, null, false, true, true);
		
		// Add an add -s rule
		Transform plural = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("s", AffixType.SUFFIX));
		List<Transform> learnedTransforms = new LinkedList<Transform>();
		learnedTransforms.add(agentive);
		learnedTransforms.add(plural);
		
		// Now split the compounds
		Compounding.breakCompounds(lex, WordSet.UNMODELED, learnedTransforms, null, false, false,
				false, null);
		// Check that the analysis of bakersfield is now compound with transforms
		// Since "bakers" isn't in the Brown corpus
		String result = lex.getWord("bakersfield").analyze();
		assertEquals("BAKE +(er) +(s) FIELD", result);
	}
	
	
	/**
	 * Test the simplex word analysis feature
	 */
	public void testanalyzeSimplexWords() {
		// Load a test data set
		lex = CorpusLoader.loadWordlist("data/test/test_wordlist.txt", "ISO8859_1", false);
		
		// Set up rules to be used for simplex analysis
		Transform agentive = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("er", AffixType.SUFFIX));
		Transform ing = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("ing", AffixType.SUFFIX));
		// Add an add -s rule, and add a pair
		Transform plural = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("s", AffixType.SUFFIX));
		List<Transform> learnedTransforms = new LinkedList<Transform>();
		plural.addWordPair(lex.getWord("bake"), lex.getWord("bakes"), true);	
		plural.addWordPair(lex.getWord("smack"), lex.getWord("smacks"), true);	
		lex.moveTransformPairs(plural, null, false, true, true);
		learnedTransforms.add(agentive);
		learnedTransforms.add(plural);
		learnedTransforms.add(ing);
		
		// Now try to get an analysis of baker and bakings 
		Compounding.analyzeSimplexWords(lex, WordSet.UNMODELED, learnedTransforms,
				true, null);
		// Note that this depends on undoubling working
		String result = lex.getWord("baker").analyze();
		assertEquals("BAKE +(er)", result);
		result = lex.getWord("smackings").analyze();
		assertEquals("SMACK +(ing) +(s)", result);
	}
	
	
	/**
	 * Regression test for repeated morphemes in a compounding analysis
	 */
	public void testdoubleRuleEnding1() {
		// Targeted test for buggy analyses such as MAIN HAUSEN = MAIN HAUSEN +(en)
		// This test looks for the case where an new word is created while 
		// applying rules in compounding that is already in the lexicon
		lex = CorpusLoader.loadWordlist("data/test/compounding_test_ger.txt", "ISO8859_1", false);
		Transform en = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("en", AffixType.SUFFIX));
		en.addWordPair(lex.getWord("haus"), lex.getWord("hausen"), false);
		lex.moveTransformPairs(en, null, false, true, true);
		List<Transform> learnedTransforms = new LinkedList<Transform>();
		learnedTransforms.add(en);
		Compounding.breakCompounds(lex, WordSet.UNMODELED, learnedTransforms, null, false, false,
				false, null);
		
		// Check that the analysis is correct
		String result = lex.getWord("mainhausen").analyze();
		assertEquals("MAIN HAUS +(en)", result);
	}
	
	/**
	 * Regression test for repeated morphemes in a compounding analysis
	 */
	public void testdoubleRuleEnding2() {
		// Targeted test for buggy analyses such as MAIN HAUSEN = MAIN HAUSEN +(en)
		// This checks that analyses of words created in compounding are
		// correct in the simple case.
		lex = CorpusLoader.loadWordlist("data/test/compounding_test_ger.txt", "ISO8859_1", false);
		Transform er = new Transform(new Affix("", AffixType.SUFFIX), 
				new Affix("er", AffixType.SUFFIX));
		// Move "haus" to Base so it will be used
		lex.moveWord(lex.getWord("haus"), WordSet.BASE);
		List<Transform> learnedTransforms = new LinkedList<Transform>();
		learnedTransforms.add(er);
		Compounding.breakCompounds(lex, WordSet.UNMODELED, learnedTransforms, null, false, false,
				false, null);
		
		// Check that the analysis is correct
		String result = lex.getWord("mainhauser").analyze();
		assertEquals("MAIN HAUS +(er)", result);
	}
}
