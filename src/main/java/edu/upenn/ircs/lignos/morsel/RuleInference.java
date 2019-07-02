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
import edu.upenn.ircs.lignos.morsel.lexicon.WordSet;
import edu.upenn.ircs.lignos.morsel.transform.Transform;
import gnu.trove.THashSet;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Allow inference of unseen forms using learned transforms.
 *
 */
public class RuleInference {
	private Set<String> inferredBases;
	
	/**
	 * Create an new inference instance with an empty set of inferred bases.
	 */
	public RuleInference() {
		inferredBases = new THashSet<String>();
	}
	
	/**
	 * Infer new bases using the given lexicon and transforms
	 * @param lex the lexicon
	 * @param transform the learned transforms
	 * @return a Set of inferred words
	 */
	private Collection<Word> inferBases(Lexicon lex, Transform transform) {
		// Go over each unmodeled word with affix2 of the transform. If
		// its hypothesized base is not word, infer it
		Set<Word> newWords = new THashSet<Word>();
		for (Word w : transform.getAffix2().getWordSet()) {
			// Skip anything not unmodeled
			if (w.getSet() != WordSet.UNMODELED) {
				continue;
			}
			
			// Hypothesize the base
			String baseText = Transform.inferBase(w, transform);
			
			// If the base does not exist, try to infer it
			if (lex.getWord(baseText) == null) {
				// If it was already inferred, add it to the lexicon
				if (inferredBases.contains(baseText)) {
					// Create a new word using the token count of the word
					// that ended up promoting it
					Word newWord = new Word(baseText, w.getCount(), false);
					newWords.add(newWord);
				}
				else {
					// Otherwise, infer it
					inferredBases.add(baseText);
				}				
			}
		}

		return newWords;
	}

	/**
	 * Infer the bases from the latest transform and then process them.
	 * @param lex the lexicon
	 * @param learnedTransforms the learned transforms
	 * @param hypTransforms the hypothesized transforms
	 * @param reEval as used by scoreWord
	 * @param doubling as used by scoreWord
	 * @param optimization as used by moveTransformPairs
	 * @param out the destination for any printing to the log
	 */
	public void conservInference(Lexicon lex, List<Transform> learnedTransforms,
			List<Transform> hypTransforms, boolean reEval, boolean doubling, 
			boolean optimization, PrintWriter out) {
		int newBaseCount = 0;
		int newPairCount = 0;
		
		// Get either a the latest transform from the learned list or for
		// overlap get the one passed in
		Transform newestTransform = learnedTransforms.get(learnedTransforms.size() - 1);
		Collection<Word> newWords = inferBases(lex, newestTransform);
		for (Word newBase : newWords) {
			// Add each new base to the lexicon and let the lexicon move it
			lex.addWord(newBase);
			lex.moveWord(newBase, WordSet.BASE);
			newBaseCount++;
			
			// Score each new base for every learned transform that can apply
			// to it, counting new pairs
			for (Transform trans : learnedTransforms) {
				if (newBase.hasAffix(trans.getAffix1()) &&  
						Transform.scoreWord(trans, newBase, lex, reEval, doubling)) {
					newPairCount++;
				}
			}
		}
				
		// If we added words, update the frequencies
		if (newBaseCount > 0) {
			lex.updateFrequencies();
		}
		
		// Move all the words for each transform
		if (newPairCount > 0) {
			for (Transform trans : learnedTransforms) {
				lex.moveTransformPairs(trans, hypTransforms, optimization, 
						reEval, doubling);
			}
		}
				
		// If optimization is on, score for all hypothesized transforms other
		// than the one just learned
		if (optimization) {
			for (Word newBase : newWords) {
				for (Transform trans : hypTransforms) {
					if (trans != newestTransform && newBase.hasAffix(trans.getAffix1())) { 
							Transform.scoreWord(trans, newBase, lex, reEval, doubling);
					}
				}
			}
		}
		
		// Put the new words out to the log if we're outputting
		if (out != null) {
			for (Word newBase : newWords) {
				out.println(newBase.toDerivedWordsString());
			}
		}
		
		// Output the results
		System.out.println(newPairCount + " new pairs inferred by conservative inference.");
	}
}
