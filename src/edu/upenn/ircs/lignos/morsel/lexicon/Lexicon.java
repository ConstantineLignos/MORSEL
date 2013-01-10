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
package edu.upenn.ircs.lignos.morsel.lexicon;

import edu.upenn.ircs.lignos.morsel.Util;
import edu.upenn.ircs.lignos.morsel.transform.Affix;
import edu.upenn.ircs.lignos.morsel.transform.AffixType;
import edu.upenn.ircs.lignos.morsel.transform.Transform;
import edu.upenn.ircs.lignos.morsel.transform.TransformPair;
import edu.upenn.ircs.lignos.morsel.transform.WordPair;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lexicon {
	private Map<String, Word> lex;
	private Map<String, Affix> prefixes;
	private Map<String, Affix> suffixes;
	private long tokenCount;
	
	private Set<Word> base;
	private Set<Word> derived;
	private Set<Word> unmod;
	private boolean validSetCounts;
	
	public Lexicon() {
		lex = new THashMap<String, Word>();
		
		prefixes = new THashMap<String, Affix>();
		suffixes = new THashMap<String, Affix>();
		tokenCount = 0L;
		
		base = new THashSet<Word>();
		derived = new THashSet<Word>();
		unmod = new THashSet<Word>();
		validSetCounts = false;
	}
	
	public String getStatus() {
		return "Types: " + lex.size() + " Tokens: " + tokenCount;
	}
	
	public long getTokenCount() { return tokenCount; }
	
	public boolean addWord(Word word) {
		// Add the word to the lexicon. If a word was already there, return false
		if (lex.put(word.getKey(), word) != null) {
			return false;
		}
		// Add its affixes
		addAffixes(word, AffixType.PREFIX);
		addAffixes(word, AffixType.SUFFIX);
		
		// Put it in unmodeled. Note the the word constructor has already set its set.
		unmod.add(word);
		
		// Count its token frequency
		tokenCount += word.getCount();
		
		return true;
	}
	
	public void updateFrequencies() {
		// Set the frequencies on all words
		for (Word w: lex.values()) {
			w.setFrequency(tokenCount);
		}
		
		// Have the affixes update their counts on frequent words
		for (Affix a: prefixes.values()) {
			a.countFreqWords();
		}
		
		// Count suffixes
		for (Affix a: suffixes.values()) {
			a.countFreqWords();
		}
	}
	
	public Set<Word> getSetWords(WordSet set) {
		// Return the right set
		switch (set) {
		case BASE: return base;
		case DERIVED: return derived;
		case UNMODELED: return unmod;
		default: throw new RuntimeException("Unhandled WordSet.");
		}
	}
	
	public Collection<Word> getWords() {
		return lex.values();
	}
	
	public Set<String> getWordKeys() {
		return lex.keySet();
	}
	
	private void addAffixes(Word word, AffixType type) {
		// Get the prefixes and count them
		String[] wordAffixes = Affix.getAffixes(word, type);
		
		// Pick the right map to put entries in based on AffixType
		Map<String, Affix> affixes = null;
		if (type == AffixType.PREFIX) {
			affixes = prefixes;
		}
		else if (type == AffixType.SUFFIX) {
			affixes = suffixes;
		}
		else {
			throw new RuntimeException("Unhandled affix type.");
		}
		
		// Count the affixes
		for (String affixText: wordAffixes) {
			// Try to look up the affix
			Affix affix = affixes.get(affixText);
			// If the affix has not been seen, create it
			if (affix == null) {
				affix = new Affix(affixText, type);
				affixes.put(affixText, affix);
			}
			// Increase the counts for this affix
			affix.addWord(word);
			
			// Add the affix to the word
			word.addAffix(affix);
		}

	}
	
	public boolean contains(Word word) {
		return lex.containsKey(word.getKey());
	}
	
	public boolean isWordInSet(String wordText, WordSet set) {
		Word word = lex.get(wordText);
		// If the word was found, return whether the set was correct,
		// otheriwse false
		return word != null ? word.getSet() == set: false;
	}
	
	public boolean isWordInSet(Word word, WordSet set) {
		return word.getSet() == set;
	}

	private void countAffixWordSets() {
		// Count prefixes
		for (Affix a: prefixes.values()) {
			a.countWordSets();
		}
		
		// Count suffixes
		for (Affix a: suffixes.values()) {
			a.countWordSets();
		}
		
		validSetCounts = true;
	}
	
	private Map<String, Affix> getAffixMap(AffixType type) {
		switch (type) {
		case PREFIX: return prefixes;
		case SUFFIX: return suffixes;
		default: throw new RuntimeException("Unhandled AffixType.");
		}
	}

	public static enum AffixSet {
		ALL, UNMOD, BASEUNMOD
	}
	
	public List<Affix> topAffixes(int n, AffixType type, AffixSet set, boolean weighted) {
		// Make sure the counts are up to date before doing anything. Counting
		// will automatically reset the flag
		if (!validSetCounts) {
			countAffixWordSets();
		}
		
		// Get the list of affixes, sort them
		List<Affix> orderedAffixes = new ArrayList<Affix>(getAffixMap(type).values());
		switch (set) {
		case ALL:
			if (weighted)
				Collections.sort(orderedAffixes, 
						Collections.reverseOrder(new WeightedAllTypeCountComparator()));
			else 
				Collections.sort(orderedAffixes, 
						Collections.reverseOrder(new AllTypeCountComparator()));
			break;
		case BASEUNMOD:
			if (weighted)
				Collections.sort(orderedAffixes, 
						Collections.reverseOrder(new WeightedBaseUnmodTypeCountComparator()));
			else
				Collections.sort(orderedAffixes, 
						Collections.reverseOrder(new BaseUnmodTypeCountComparator()));
			break;
		case UNMOD:
			if (weighted)
				Collections.sort(orderedAffixes, 
						Collections.reverseOrder(new WeightedUnmodTypeCountComparator()));
			else
				Collections.sort(orderedAffixes, 
						Collections.reverseOrder(new UnmodTypeCountComparator()));
				return Util.truncateCollection(orderedAffixes, n);				
		}

		// Truncate the list
		return Util.truncateCollection(orderedAffixes, n);
	}
	
	
	
	public List<Affix> topUnmodAffixes(int n, AffixType type) {
		// Make sure the counts are up to date before doing anything. Counting
		// will automatically reset the flag
		if (!validSetCounts) {
			countAffixWordSets();
		}
		
		// Get the list of affixes, sort them, truncate them
		List<Affix> orderedAffixes = new ArrayList<Affix>(getAffixMap(type).values());
		Collections.sort(orderedAffixes, 
				Collections.reverseOrder(new WeightedUnmodTypeCountComparator()));
		return Util.truncateCollection(orderedAffixes, n);
	}
	
	public List<Affix> getSortedAffixes() {
		List<Affix> orderedAffixes = Util.concatCollections(prefixes.values(), 
				suffixes.values());
		Collections.sort(orderedAffixes,
				Collections.reverseOrder(new WeightedTypeCountComparator()));
		return orderedAffixes;
	}
	
	public List<Affix> getSortedPrefixes() {
		List<Affix> orderedPrefixes = new ArrayList<Affix>(prefixes.values());
		Collections.sort(orderedPrefixes, 
				Collections.reverseOrder(new WeightedTypeCountComparator()));
		return orderedPrefixes;
	}
	
	public List<Affix> getSortedSuffixes() {
		List<Affix> orderedSuffixes = new ArrayList<Affix>(suffixes.values());
		Collections.sort(orderedSuffixes, 
				Collections.reverseOrder(new WeightedTypeCountComparator()));
		return orderedSuffixes;
	}
	
	public void dumpAffixes() {
		List<Affix> orderedAffixes = getSortedAffixes();
		for (Affix affix: orderedAffixes) {
			System.out.println(affix);
		}
	}
	
	public static class TypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					((Affix) affix1).getTypeCount(),
					((Affix) affix2).getTypeCount());
		}
	}
	
	public static class WeightedTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					((Affix) affix1).getWeightedTypeCount(),
					((Affix) affix2).getWeightedTypeCount());
		}
	}

	public static class AllTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					(((Affix) affix1).getBaseTypeCount() +
					 ((Affix) affix1).getUnmodTypeCount() +
					 ((Affix) affix1).getDerivedTypeCount()),
					(((Affix) affix2).getBaseTypeCount() +
					 ((Affix) affix2).getUnmodTypeCount() +
					 ((Affix) affix2).getDerivedTypeCount()));
		}
	}
	
	public static class WeightedAllTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					(((Affix) affix1).getWeightedBaseTypeCount() +
					 ((Affix) affix1).getWeightedUnmodTypeCount() +
					 ((Affix) affix1).getWeightedDerivedTypeCount()),
				    (((Affix) affix2).getWeightedBaseTypeCount() +
				     ((Affix) affix2).getWeightedUnmodTypeCount() +
				     ((Affix) affix2).getWeightedDerivedTypeCount()));
		}
	}
	
	public static class BaseUnmodTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					(((Affix) affix1).getBaseTypeCount() + ((Affix) affix1).getUnmodTypeCount()),
					(((Affix) affix2).getBaseTypeCount() + ((Affix) affix2).getUnmodTypeCount()));
		}
	}
	
	public static class UnmodTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					((Affix) affix1).getUnmodTypeCount(),
					((Affix) affix2).getUnmodTypeCount());
		}
	}
	
	public static class WeightedBaseUnmodTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					(((Affix) affix1).getWeightedBaseTypeCount() +
					 ((Affix) affix1).getWeightedUnmodTypeCount()),
					(((Affix) affix2).getWeightedBaseTypeCount() +
					 ((Affix) affix2).getWeightedUnmodTypeCount()));
		}
	}
	
	public static class WeightedUnmodTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					((Affix) affix1).getWeightedUnmodTypeCount(),
					((Affix) affix2).getWeightedUnmodTypeCount());
		}
	}
	
	public static class TokenCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object affix1, Object affix2) {
			return Long.compare(
					((Affix) affix1).getTokenCount(),
					((Affix) affix2).getTokenCount());
		}
	}

	public Word getWord(String word) {
		return lex.get(word);
	}
	
	public void moveWord(Word word, WordSet set) {
		// Take it out of its old set
		switch (word.getSet()) {
		case BASE: base.remove(word); break;
		case DERIVED: derived.remove(word); break;
		case UNMODELED: unmod.remove(word); break;
		default: throw new RuntimeException("Unhandled WordSet.");
		}
		
		// Set the word's set
		word.setSet(set);
		
		// Put it in the new set
		switch (set) {
		case BASE: base.add(word); break;
		case DERIVED: derived.add(word); break;
		case UNMODELED: unmod.add(word); break;
		default: throw new RuntimeException("Unhandled WordSet.");
		}
	}

	public void moveTransformPairs(Transform learnedTransform, 
			List<Transform> hypTransforms, boolean opt, boolean reEval, 
			boolean doubling) {
		// Select the list of moved words based on whether the transform
		// has already been learned or not
		Set<WordPair> pairs = learnedTransform.isLearned() ?
				learnedTransform.getUnmovedDerivationPairs() :
				learnedTransform.getWordPairs();
		
		// Call moveWordPairs to do all the work
		moveWordPairs(learnedTransform, hypTransforms, opt, reEval, doubling,
				pairs);
	}

	public void moveWordPairs(Transform derivingTransform,
			List<Transform> hypTransforms, boolean opt, boolean reEval,
			boolean doubling, Set<WordPair> pairs) {
		// Keep track of words that moved based on the sets
		List<Word> unmodBaseWords = new LinkedList<Word>();
		List<Word> baseDerivedWords = new LinkedList<Word>();
		List<Word> unmodDerivedWords = new LinkedList<Word>();
		
		// Handle duplicates
		Set<WordPair> prunedPairs;
		// Keep track of all of the derivations
		Map<Word, WordPair> derivedPairs = new THashMap<Word, WordPair>();
		prunedPairs = new THashSet<WordPair>();
	
		for (WordPair pair: pairs) {
			// Check whether this derived form has been derived already
			Word derived = pair.getDerived();
			if (derivedPairs.containsKey(derived)) {
				// If it has, pick which derivation to keep
				WordPair oldPair = derivedPairs.get(derived);
				
				// If the old pair is accomodated, we may need to replace it
				if (oldPair.isAccomodated()){
					// If this pair is not accomodated, definitely replace
					if (!pair.isAccomodated()) {
						// Remove it from the pairs
						prunedPairs.remove(oldPair);
					}
					else {
						// Otherwise, we prefer the doubled pair to
						// undoubled. The easy way to tell the two 
						// apart is the the doubled base is shorter
						if (pair.getBase().length() < oldPair.getBase().length()) {
							// Remove if this pair has a shorter base
							prunedPairs.remove(oldPair);
						}
						else {
							// Otherwise, this pair should not be added
							continue;
						}
					}
				}
				else {
					// Otherwise, this pair should not be added since we 
					// already have a better derivation
					// Skip over the adding
					continue;
				}
			}
			// Add this pair to the map and pairs set
			prunedPairs.add(pair);
			derivedPairs.put(derived, pair);
		}
						
		// Move each pair
		for (WordPair pair: prunedPairs) {
			// Unpack the pair
			Word base = pair.getBase();
			Word derived = pair.getDerived();
			
			// Create the base/derived relationship
			base.addDerived(derived);
			derived.setBase(base);
			derived.setTransform(derivingTransform);
			
			// If the base has no root, set it to itself, which also sets the
			// derived word's root
			if (base.getRoot() == null) {
				base.setRoot(base);
			}
			else {
				// Otherwise just set the derived word's root to that
				// of the base
				derived.setRoot(base.getRoot());
			}
						
			// If the base is in unmod, move it to base
			// Note that if it is already in derived, it should stay there
			if (base.getSet() == WordSet.UNMODELED) {
				moveWord(base, WordSet.BASE);
				unmodBaseWords.add(base);
			}
			
			// If the derived word is not in derived, move it there
			// It may have moved from base (demotion upon re-analysis) or 
			// from unmodeled (first analysis)
			if (derived.getSet() == WordSet.BASE) {
				baseDerivedWords.add(derived);
				moveWord(derived, WordSet.DERIVED);
			}
			else if (derived.getSet() == WordSet.UNMODELED) {
				unmodDerivedWords.add(derived);
				moveWord(derived, WordSet.DERIVED);
			}
		}
		
		// If the transform has already been learned, reset the word pairs
		if (derivingTransform.isLearned()) {
			derivingTransform.resetUnmoved();
		}
		
		// Invalidate the set counts since we moved words
		validSetCounts = false;
		
		// If we're not doing optimization, we're done
		if (!opt) {return;}
		
		// Now, do the accounting for the moved words by going over each list
		// of moved words

		// Remove all the moved words from their transforms
		removeWordsTransforms(unmodBaseWords);
		removeWordsTransforms(baseDerivedWords);
		removeWordsTransforms(unmodDerivedWords);
		
		// Now, rescore the new bases for each transform
		scoreWordsTransforms(unmodBaseWords, hypTransforms, reEval, doubling);
	}
	
	public void scoreWordsTransforms(Collection<Word> words,
			Collection<Transform> transforms, boolean reEval, boolean doubling) {
		// Score each word for each transform
		for (Word word : words) {
			for (Transform transform : transforms) {
				// Score only if the transform applies
				if (word.hasAffix(transform.getAffix1())) {
					Transform.scoreWord(transform, word, this, reEval, doubling);
				}
			}
		}
	}

	private void removeWordsTransforms(Collection<Word> newDerivedWords) {
		for (Word movedWord: newDerivedWords) {
			// Remove a word entirely from transform scoring
			// Because we are removing, use the full iterator syntax
			Iterator<TransformPair> iter = movedWord.getTransforms().iterator();
			while(iter.hasNext()) {
				
				TransformPair tPair = iter.next();
				WordPair wordPair = tPair.getPair();
				Transform transform = tPair.getTransform();
				
				// Always remove the pair from the transform and from the current
				// word, but we still need to find out whether this word was the 
				// base or the derived to remove the pair from the other word
				transform.removeWordPair(wordPair);
				iter.remove(); // Removes from the current word in the pair
				if (wordPair.getBase() == movedWord) {
					// If the current word was the base, remove from the derived word
					wordPair.getDerived().removeTransformPair(tPair); 
				}
				else {
					// If the current word was the derived, remove from the base word
					wordPair.getBase().removeTransformPair(tPair); 
				}
			}
		}
	}

	public void processHyphenation() {
		// Loop over all words and process any hyphenated ones		
		// Copy the values of the lexicon so we can modify it as we iterate
		List<Word> lexWords = new ArrayList<Word>(lex.values());
		for (Word w : lexWords) {
			// Check for a hyphen before doing the full split
			String text = w.getText();
			if (text.indexOf('-') != -1) {
				String[] componentTexts = text.split("-");
				
				// Make a list of words that make this up, creating words if needed
				List<Word> componentWords = new LinkedList<Word>();
				for (String componentText : componentTexts) {
					// If the text is empty, keep going
					if (componentText.equals("")) {
						continue;
					}
					
					// If the word hasn't been seen before, add it
					Word componentWord = lex.get(componentText);
					if (componentWord == null) {
						// Use the frequency of the original
						componentWord = new Word(componentText, w.getCount(), false);
						addWord(componentWord);
					}
					else {
						// If the word has been seen before, increment its
						// token count
						componentWord.addCount(w.getCount());
					}
					
					componentWords.add(componentWord);
				}
				
				// If we actually ended up with more than one component,
				// make the original word hyphenated
				if (componentWords.size() > 1) {
					w.makeCompoundWord(componentWords);
				}
			}
		}
		
		// When we're done, since we added words we have to recalculate frequencies
		updateFrequencies();
	}
}
