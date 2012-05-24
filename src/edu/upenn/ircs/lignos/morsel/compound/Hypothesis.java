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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import edu.upenn.ircs.lignos.morsel.TransformInference;
import edu.upenn.ircs.lignos.morsel.compound.Compounding.Filler;
import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;

public class Hypothesis {
	List<Word> words; // Words used in the compound, in order
	String remainingText; // Text remaining after those words

	public Hypothesis(String remainingText) {
		this.remainingText = remainingText;
		words = new LinkedList<Word>();
	}

	public Hypothesis(String remainingText, List<Word> hypWords) {
		this.remainingText = remainingText;
		words = new LinkedList<Word>(hypWords);
	}

	private void add(Word newWord) {
		// Add a new word to the CompoundingHypothesis and reduce the text by it
		words.add(newWord);
		remainingText = remainingText.substring(newWord.length());
	}

	private Hypothesis extend(Word newWord) {
		// Return a new CompoundingHypothesis that extends the current one by a word
		Hypothesis newHyp = new Hypothesis(remainingText, words);
		newHyp.add(newWord);
		return newHyp;
	}

	public double getScore() {
		// The score of a hypothesis is the geometric mean of the scores
		// of its words
		// Multiply out the scores of each word
		double prod = 1;
		for (Word word : words) {
			prod *= scoreWord(word);
		}

		// Now take it to the 1/n power
		return Math.pow(prod, 1.0/(double) words.size());
	}

	public boolean isComplete() {
		// Returns true if the remaining text is empty
		return remainingText.isEmpty();
	}

	public List<Hypothesis> extendAll(Lexicon lex, Filler filler, 
			TransformInference transInf, boolean doubling) {
		// Extend the hypothesis by one word, if possible
		List<Hypothesis> extended = new LinkedList<Hypothesis>();

		// Get all prefix words of the remaining text
		// If words is not empty (i.e. this is after the first iteration
		// on this word), we can allow a prefix to be the whole
		// remaining text.
		List<Word> prefixWords = Compounding.getPrefixes(remainingText, lex, 
				!words.isEmpty(), filler, transInf,
				doubling);

		// Add a new hypothesis for every prefix found
		for (Word prefixWord : prefixWords) {
			extended.add(extend(prefixWord));
		}

		return extended;
	}

	public String toString() {
		StringBuilder out = new StringBuilder();
		for (Word word : words) {
			out.append(word.getText() + '|');
		}

		out.append(remainingText);

		return out.toString();
	}
	
	public static Hypothesis pickHypothesis(
			List<Hypothesis> completeHyps, Word word) {
		// Pick the best compounding hypothesis or the hypothesis that this
		// is not a compound at all
		Hypothesis bestHyp = null;
		double bestScore = 0;
		for (Hypothesis hyp : completeHyps) {
			double score = hyp.getScore();
			if(score > bestScore) {
				bestHyp = hyp;
				bestScore = score;
			}
		}
		
		// Return the best hyp if it as good or better than the original word
		double wordScore = scoreWord(word);
		return bestScore >= wordScore ? bestHyp : null;
	}

	
	/**
	 *  Allow comparison of two hypotheses by their scores. To get a descending 
	 *  list, the comparator inverts the usual ordering.
	 *
	 */
	public static class HypothesisScoreRanker implements Comparator<Hypothesis> {
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Hypothesis h1, Hypothesis h2) {
			// Intentionally 2 - 1 to get a descending sort
			return (int) (h2.getScore() - h1.getScore());
		}
	}
	
	private static int scoreWord(Word word) {
		return word.getCount();
	}
}

