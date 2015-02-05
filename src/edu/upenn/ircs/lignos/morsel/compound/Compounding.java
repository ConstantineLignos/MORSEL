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

import edu.upenn.ircs.lignos.morsel.TransformInference;
import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;
import edu.upenn.ircs.lignos.morsel.lexicon.WordSet;
import edu.upenn.ircs.lignos.morsel.transform.Affix;
import edu.upenn.ircs.lignos.morsel.transform.Transform;
import edu.upenn.ircs.lignos.morsel.transform.WordPair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Suppor the analysis of compound words
 *
 */
public class Compounding {
	/** The set of transform relations to take into account */
	public static boolean TRANSFORM_RELATIONS;
	/** The minimum length of a compound word */
	static int MIN_COMPOUND_LENGTH = 4;
	/** The beam size in the search for compounds */
	static int BEAM_SIZE = 200;
	
	/**
	 * Break any compounds in the given wordset, using the lexicon and optionally
	 * using the learnedTransforms.
	 * @param lex the learner's lexicon
	 * @param set the WordSet to break compounds in
	 * @param learnedTransforms the transforms learned so far, which should be
	 * null if transforms should not be used.
	 * @param hypTransforms the transforms currently hypothesized by the learner
	 * @param opt whether optimization is on
	 * @param reEval whether transform reevaluation is on
	 * @param doubling whether doubling is on
	 * @param transInf the inferred relationships between transforms
	 * @return the number of compounds that were broken
	 */
	public static int breakCompounds(Lexicon lex, WordSet set, 
			Collection<Transform> learnedTransforms, List<Transform> hypTransforms, 
			boolean opt, boolean reEval, boolean doubling, TransformInference transInf) {
		// Count of split compounds
		int nCompounds = 0;
		
		// Make the possible fillers
		// If null was passed for learnedTransforms, just make filler null,
		// otherwise make the filler from the learned transforms
		Filler filler = learnedTransforms == null ? null : new Filler(learnedTransforms);

		// Track all the words that need to be created by the transform
		// that created them
		Map<Transform, Set<WordPair>> transformPairs = 
			new THashMap<Transform, Set<WordPair>>();
		// Loop over each word in the appropriate set and try to break it
		for (Word word : lex.getSetWords(set)) {
			// Skip words that are already compounds or are too short
			if (word.isCompound() || word.length() < MIN_COMPOUND_LENGTH)
				continue;
			
			// Get the best compounding hypothesis for each word
			Hypothesis bestHyp = breakWord(word, filler, lex, transInf, doubling);
			// If the hypothesis is good, break the word
			if (bestHyp != null) {
				// Count the compound and change the word to compound if the
				// derivation is more than one word
				if (bestHyp.words.size() > 1) {
					nCompounds++;
					word.makeCompoundWord(bestHyp.words);
				}
				
				// Keep track of new words, we'll move them later
				for (Word compoundElement : bestHyp.words) {
					// If a word is a duplicate of something in the lexicon,
					// find the existing word and set it up
					boolean duplicate = compoundElement.isDuplicate();
					if (duplicate) {
						// Get the version of the word in the lexicon
						Word lexWord = lex.getWord(compoundElement.getText()); 
						
						// Mark up the lexWord appropriately
						lexWord.setBase(compoundElement.getBase());
						lexWord.setTransform(compoundElement.getDerivation());
						
						
						// Now replace the compound element with the original
						// word
						compoundElement = lexWord;
					}
						
					
					// Add changed words (were duplicates) or new words to the lexicon
					if (duplicate || !lex.contains(compoundElement)) {							
						// Add the wordpair to the set for the appropriate
						// transform
						addWordPair(compoundElement.getDerivation(),
								compoundElement.getBase(),
								compoundElement, transformPairs);
					}
				}
			}
		}
		
		// Now move all the new words and create the base/derived relationship
		for (Entry<Transform, Set<WordPair>> e : transformPairs.entrySet()) {
			// Skip transforms with no pairs
			if (e.getValue() == null)
				continue;
				
			// First do a loop to add each word to the lexicon
			for (WordPair pair : e.getValue()) {
				// Add the word to the lexicon if it's just been created. Words
				// that have been created have analyze set to false
				Word derived = pair.getDerived();
				if (!derived.shouldAnalyze()) {
					lex.addWord(derived);	
				}
			}
			
			// Then have the lexicon move all the pairs
			lex.moveWordPairs(e.getKey(), hypTransforms, opt, reEval, doubling, 
					e.getValue());		
		}
		
		return nCompounds;
	}

	/**
	 * Add a word pair for a transform to the map that tracks them.
	 * @param derivingTransform The deriving transform.
	 * @param base Base of the word pair
	 * @param derived Derived word of the word pair
	 * @param transformPairs The map of transforms to their word pairs
	 */
	private static void addWordPair(Transform derivingTransform, Word base,
			Word derived, Map<Transform, Set<WordPair>> transformPairs) {
		// If the transform doesn't have any pairs yet, create the set
		if (!transformPairs.containsKey(derivingTransform)) {
			transformPairs.put(derivingTransform, new THashSet<WordPair>());
		}
		
		// Add a new pair
		transformPairs.get(derivingTransform).add(new WordPair(base, derived, false));
	}

	/**
	 * Break a word using the given filler and lexicon.
	 * @param word the word to break
	 * @param filler the filler elements that can be inserted between words
	 * @param lex the learner's lexicon
	 * @param transInf the inferred relationships between transforms
	 * @param doubling as used by scoreWord
	 * @return the best hypothesis for breaking the word, null if the word 
	 * should not be broken
	 */
	protected static Hypothesis breakWord(Word word, Filler filler, Lexicon lex,
			TransformInference transInf, boolean doubling) {
		// Create a queue for the search and a list for the complete hypotheses
		PriorityQueue<Hypothesis> currHyps = new PriorityQueue<Hypothesis>(BEAM_SIZE, 
				new Hypothesis.HypothesisScoreRanker());
		// Track the new hypotheses for each round
		PriorityQueue<Hypothesis> newHyps = new PriorityQueue<Hypothesis>(BEAM_SIZE, 
					new Hypothesis.HypothesisScoreRanker());
		List<Hypothesis> completeHyps = new LinkedList<Hypothesis>();
		// Temporary variable for swapping lists
		PriorityQueue<Hypothesis> swap;
		
		// Initialize the set of hypotheses with a null hypothesis
		currHyps.add(new Hypothesis(word.getText()));
		
		// Flag for going over the beam so it's announced only once per word
		boolean announced = false;
		
		// Keep trying to break the compound as long as there are hypotheses left
		while (!currHyps.isEmpty()) {
			// Extend each hypothesis within the beam and add the new hypotheses 
			// to the queue as appropriate
			int i;
			for (i=0; i < BEAM_SIZE; i++) {
				// Get the best hypothesis off the queue, giving up if there is none
				Hypothesis hyp = currHyps.poll();
				if (hyp == null) {
					break;
				}
				
				// Extend this hypothesis
				List<Hypothesis> results = hyp.extendAll(lex, filler, transInf, 
						doubling);
				// Check each extended one to see if it's complete
				for (Hypothesis result : results) {
					// Add any complete results except any one-word hypotheses 
					// without a derivation
					if (result.isComplete() && !(result.words.size() == 1 && 
							result.words.get(0).getDerivation() == null)) {
						completeHyps.add(result);
					}
					else {
						newHyps.add(result);
					}
				}
			}
			
			// Output a warning if we went over the beam size
			if (!announced && i == BEAM_SIZE) {
				System.err.println("Word over beam size: " + word);
				announced = true;
			}
			
			// Now update the current hypotheses to the hypotheses generated
			// this round, and recycle the current hypotheses queue
			swap = currHyps;
			currHyps = newHyps;
			newHyps = swap;
			newHyps.clear();
		}
		
		// Pick the best compounding breakdown, or return null if it wasn't
		// a compound at all. Note that pickHypothesis can also return null
		return completeHyps.isEmpty() ? null : 
			Hypothesis.pickHypothesis(completeHyps, word);
	}


	
	protected static List<Word> getPrefixes(String word, Lexicon lex, 
			boolean allowFull, Filler fillers, TransformInference transInf,
			boolean doubling) {
		// Return a list all words that are a prefix of the passed string
		
		// We loop over possible ending indices of the prefix
		// The index can range from MIN_COMPOUND_LENGTH to either 1 less than the length of the string
		// or the full length of the string if allowFull is on
		List<Word> prefixes = new LinkedList<Word>();
		int max = allowFull ? word.length() : word.length() - 1;
		
		for (int i = MIN_COMPOUND_LENGTH; i <= max; i++) {
			// First get a normal prefix of the string
			String prefix = word.substring(0, i);
			
			Word prefixWord = lex.getWord(prefix);
			// Add it if the word was found, and then continue 
			if (prefixWord != null) {
				prefixes.add(prefixWord);
			}
		}
		
		// Try adding filler suffixes if fillers were given
		if (fillers != null) {
			for (int i = MIN_COMPOUND_LENGTH; i <= max; i++) {
				// First get a normal prefix of the string
				String prefix = word.substring(0, i);
				// If the prefix isn't a word, continue
				Word baseWord = lex.getWord(prefix);
				if (baseWord == null) {continue;}
				
				// If the word is in unmodeled, continue
				if (baseWord.getSet() == WordSet.UNMODELED) {continue;}
				
				// Then try each prefix with a filler suffix- these are derived
				// forms
				for (FillerResult result : fillers.getFilledSuffixes(prefix, 
						word, baseWord, doubling)) {
					
					// If it's a duplicate, we can still use the word if it's
					// from unmodeled. Mark it as a duplicate so we know later.
					Word dupeWord = lex.getWord(result.derivedText);
					boolean duplicate = false;
					if (dupeWord != null) {
						if (dupeWord.getSet() == WordSet.UNMODELED) {
							duplicate = true;
						}
						else
							continue;
					}
					
					// Skip if it's an illegal combination of transforms
					if (TRANSFORM_RELATIONS && 
							!transInf.isGoodRelation(result.baseWord.getDerivation(), 
							result.derivation)) {
						continue;
					}
					
					// Create word with half the count of the prefix we used, 
					// or the count of the original word if this is a duplicate.
					// This word will be added to the lexicon later if the hypothesized
					// compound is accepted
					long count = duplicate ? dupeWord.getCount() : 
						lex.getWord(prefix).getCount() / 2;
					Word prefixWord = new Word(result.derivedText, 
							count , false);
					
					// Mark as duplicate if needed
					if (duplicate) {
						prefixWord.markDuplicate();
					}
					
					// Add it to our list of prefixes
					prefixes.add(prefixWord);
					
					// We sneak the base/transform information into the new word 
					// here. This is later set correctly if this compounding is
					// used.
					prefixWord.setBase(result.baseWord);
					prefixWord.setTransform(result.derivation);
				}
			}
		}
		
		return prefixes;
	}
	

	/**
	 * The FillerResult class is a simple structure to represent the
	 * result of adding filler to a word.
	 *
	 */
	protected static class FillerResult {
		String derivedText;
		Word baseWord;
		Transform derivation;
		
		/**
		 * Create a FillerResult instance.
		 * @param derivedText the text of the derived form
		 * @param baseWord the Word of the base form
		 * @param derivation the deriving transform to connect the base and 
		 * derived forms
		 */
		public FillerResult(String derivedText, Word baseWord, 
				Transform derivation) {
			this.derivedText = derivedText;
			this.baseWord = baseWord;
			this.derivation = derivation;			
		}
	}
	
	/**
	 * The Filler class takes learned transforms and can apply them as needed
	 * to word prefixes to form derived words inside a compound.
	 *
	 */
	protected static class Filler {
		private List<Transform> prefixes;
		private List<Transform> suffixes;
		
		/**
		 * Create a Filler instance from the given transforms.
		 * @param transforms the transforms to be used as filler
		 */
		public Filler(Collection<Transform> transforms) {
			prefixes = new LinkedList<Transform>();
			suffixes  = new LinkedList<Transform>();
			for (Transform transform : transforms) {
				switch(transform.getAffixType()) {
				case PREFIX:
					prefixes.add(transform);
					break;
				case SUFFIX:
					suffixes.add(transform);
					break;
				default:
					throw new RuntimeException("Unhandled AffixType");
				}
			}
		}

		/**
		 * Return a list of FillerResults representing all legal combinations
		 * of the prefix and fillers.
		 * @param prefix the text to attempt to add filler to
		 * @param fullWord the text that the prefix is from
		 * @param prefixWord the Word object corresponding to the prefix
		 * @param doubling as used by scorewWord
		 * @return a list of FillerResults representing all legal combinations
		 * of the prefix and fillers
		 */
		public List<FillerResult> getFilledSuffixes(String prefix, String fullWord, 
				Word prefixWord, boolean doubling) {
			List<FillerResult> filled = new LinkedList<FillerResult>();
			for (Transform transform : suffixes) {
				String derived = makeFillerDerivedFromPrefix(fullWord, prefixWord.getText(),
						doubling, transform.getAffix1(), transform.getAffix2());
				
				// Add a FillerResult for this to the list if there was a good
				// derivation.
				if (derived != null) {
					filled.add(new FillerResult(derived, prefixWord, transform));
				}
			}
			
			return filled;
		}

		public static String makeFillerDerivedFromPrefix(String fullWord,
				String prefixWord, boolean doubling, Affix affix1, Affix affix2) {
			String derived;
			// Try possible derived forms using each transform
			// Check for affix1
			if (!Affix.hasAffix(prefixWord, affix1))
				return null;
			
			String stem = Transform.makeStem(prefixWord, affix1);
			derived = Transform.makeDerived(stem, affix2, 
					false, false);
			
			// Check derived, break if it's good
			if (fullWord.startsWith(derived))
				return derived;
			
			// Try doubled and undoubled forms if needed and do the same
			if (doubling && affix1.isNull()) {
				// Doubled
				derived = Transform.makeDerived(stem, affix2, 
						true, false);
				if (fullWord.startsWith(derived))
					return derived;
				
				// Undoubled, make sure it's not the same as the stem
				derived = Transform.makeDerived(stem, affix2, 
						false, true);
				if (fullWord.startsWith(derived) && !derived.equals(stem))
					return derived;
			}
			// Return null if we didn't find anything
			return null;
		}
	}
	
	/**
	 * Perform simplex word analysis. This is an experimental feature which tries
	 * to segment words without using base/derived pairs, just by splitting a word
	 * into morphemes if it remains unmodeled at the end of learning.
	 * @param lex the lexicon
	 * @param set the word set to examine
	 * @param learnedTransforms the learned transforms
	 * @param doubling as used by scoreWord
	 * @param transInf the inferred relationships between transforms
	 * @return the number of words analyzed
	 */
	public static int analyzeSimplexWords(Lexicon lex, WordSet set, 
			Collection<Transform> learnedTransforms, boolean doubling, 
			TransformInference transInf) {
		// Count of split compounds
		int newAnalyses = 0;
		
		// Make the possible fillers
		// If null was passed for learnedTransforms, just make filler null,
		// otherwise make the filler from the learned transforms
		Filler filler = learnedTransforms == null ? null : new Filler(learnedTransforms);

		// Loop over each word in the appropriate set and try to analyze it
		for (Word word : lex.getSetWords(set)) {
			// Skip words that are compounds or are too short
			if (word.isCompound() || word.length() < MIN_COMPOUND_LENGTH)
				continue;
			
			// Get the best compounding hypothesis for each word
			AnalysisResult bestHyp = analyzeWord(word, filler, lex, transInf, doubling);
			// If the hypothesis is good, analyze the word
			if (bestHyp != null) {
				// Set the word's analysis
				StringBuilder analysis = new StringBuilder(bestHyp.base.analyze());
				for (Transform t : bestHyp.derivingTransforms) {
					analysis.append(" " + t.analyze());
				}
				word.setExternalAnalysis(analysis.toString());
				newAnalyses++;
			}
		}
		
		return newAnalyses;
	}

	private static AnalysisResult analyzeWord(Word word, Filler filler,
			Lexicon lex, TransformInference transInf, boolean doubling) {
		// We take a strategy like filler making, first getting the prefixes,
		// then trying to expand each one to see if it works
		
		// Keep a list of good analysis hypotheses
		PriorityQueue<AnalysisResult> completeResults = 
			new PriorityQueue<AnalysisResult>(BEAM_SIZE, new AnalysisResult.AnalysisScoreRanker());
		PriorityQueue<AnalysisResult> currResults = 
			new PriorityQueue<AnalysisResult>(BEAM_SIZE, new AnalysisResult.AnalysisScoreRanker());
		PriorityQueue<AnalysisResult> newResults = 
			new PriorityQueue<AnalysisResult>(BEAM_SIZE, new AnalysisResult.AnalysisScoreRanker());
		PriorityQueue<AnalysisResult> swap;
		
		// We loop over possible ending indices of the prefix and seed the results
		// with the prefixes
		// The index can range from MIN_COMPOUND_LENGTH to 1 less than 
		// the length of the string
		for (int i = MIN_COMPOUND_LENGTH; i <= word.length() - 1; i++) {
			// First get a normal prefix of the string
			Word prefixWord = lex.getWord(word.getText(0, i));
			
			// Add it if the word was found and the word is not in unmod
			if (prefixWord != null && prefixWord.getSet() != WordSet.UNMODELED) {
				currResults.add(new AnalysisResult(prefixWord));
			}
		}
		
		// Keep trying to analyze the word as long as there are hypotheses left
		boolean announced = false;
		int count = 0; // Safety counter
		while (!currResults.isEmpty()) {
			// Extend each hypothesis within the beam and add the new hypotheses 
			// to the queue as appropriate
			int i;
			for (i=0; i < BEAM_SIZE; i++) {
				// Get the best hypothesis off the queue, giving up if there is none
				AnalysisResult hyp = currResults.poll();
				if (hyp == null) {
					break;
				}
				
				// Extend this hypothesis
				List<AnalysisResult> results = hyp.extendAll(lex, filler, transInf, 
						doubling, word.getText());
				// Check each extended one to see if it's complete
				for (AnalysisResult result : results) {
					// Add any complete results except any one-word hypotheses 
					// without a derivation
					if (result.isComplete()) {
						completeResults.add(result);
					}
					else {
						newResults.add(result);
					}
				}
			}
			
			// Output a warning if we went over the beam size
			if (!announced && i == BEAM_SIZE) {
				System.err.println("Word over beam size: " + word);
				announced = true;
			}
			
			// Now update the current hypotheses to the hypotheses generated
			// this round, and recycle the current hypotheses queue
			swap = currResults;
			currResults = newResults;
			newResults = swap;
			newResults.clear();
			
			// Safety break for infinite loops
			if (count++ > 1000) {
				System.err.println("Infinite loop on " + word);
				break;
			}
		}
		
		// Return the best
		return completeResults.poll();
	}
	
	private static class AnalysisResult {
		Word base;
		String text;
		LinkedList<Transform> derivingTransforms;
		boolean complete;
	
		public AnalysisResult(Word base) {
			this.base = base;
			this.text = base.getText();
			derivingTransforms = new LinkedList<Transform>();
		}
		
		public boolean isComplete() {
			return complete;
		}

		/**
		 * Return all possible ways to extend this analysis by one more filler.
		 * @param lex the lexicon
		 * @param filler the filler
		 * @param transInf transform inference information
		 * @param doubling whether doubling is on
		 * @param fullWord the full word being analyzed
		 * @return All possible extensions of this analysis
		 */
		public List<AnalysisResult> extendAll(Lexicon lex, Filler filler,
				TransformInference transInf, boolean doubling, String fullWord) {
			List<AnalysisResult> filled = new LinkedList<AnalysisResult>();
			// Try possible derived forms using each transform
			for (Transform transform : filler.suffixes) {
				// Skip the transform if it's not allowed
				if (TRANSFORM_RELATIONS && !derivingTransforms.isEmpty() &&
						!transInf.isGoodRelation(derivingTransforms.getLast(),
						transform)) 
					continue;
				
				String derived = Filler.makeFillerDerivedFromPrefix(fullWord, text,
						doubling, transform.getAffix1(), transform.getAffix2());
				
				// Add a new FillerResult for this to the list if there was a good
				// derivation. Mark it as done if this is the complete word. 
				if (derived != null) {
					AnalysisResult newResult = cloneResult(this);
					newResult.addTransform(transform);
					newResult.text = derived;
					if (derived.equals(fullWord)) {
						newResult.complete = true;
					}
					filled.add(newResult);
				}
			}
			
			return filled;
		}


		public void addTransform(Transform t) {
			derivingTransforms.add(t);
		}
		
		public double scoreTransforms() {
			// The score of a hypothesis is the geometric mean of the scores
			// of its words
			// Multiply out the scores of each word
			double prod = 1;
			for (Transform t : derivingTransforms) {
				prod *= t.getTypeCount();
			}

			// Now take it to the 1/n power
			return Math.pow(prod, 1.0/(double) derivingTransforms.size());
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			// Build up string for transforms
			StringBuilder out = new StringBuilder(base.getText());
			for (Transform t : derivingTransforms) {
				out.append(" " + t);
			}
			return out.toString(); 
		}
		
		
		public static AnalysisResult cloneResult(AnalysisResult result) {
			AnalysisResult copy = new AnalysisResult(result.base);
			copy.derivingTransforms = new LinkedList<Transform>(result.derivingTransforms);
			return copy;
		}
		
		/**
		 *  Allow comparison of two analyses by the derivations they rely
		 */
		public static class AnalysisScoreRanker implements Comparator<AnalysisResult> {
			/* (non-Javadoc)
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			public int compare(AnalysisResult h1, AnalysisResult h2) {
				// Try to differentiate by base first
				if (h1.base.getCount() != h2.base.getCount())
					return Long.compare(h1.base.getCount(), h2.base.getCount());
				else {
					// Differentiate by score of transforms
					return Double.compare(h1.scoreTransforms(), h2.scoreTransforms());
				}
			}
		}
	}

}
