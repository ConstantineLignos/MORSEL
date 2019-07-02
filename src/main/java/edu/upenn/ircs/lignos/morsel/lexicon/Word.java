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
import edu.upenn.ircs.lignos.morsel.transform.Transform;
import edu.upenn.ircs.lignos.morsel.transform.TransformPair;
import gnu.trove.THashSet;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The representation of a word
 *
 */
public class Word {
	// These are set by the main learner, but defaults are provided here
	/** Default minimum frequency threshold for a word to count toward transform scoring */
	public static double FREQ_THRESHOLD = .000001F;
	/** Default minimum count threshold for a word to count toward transform scoring */
	public static int COUNT_THRESHOLD = 1;
	
	private long count;
	private double freq;
	private WordSet set;
	private Word base;
	private Word root;
	List<Word> componentWords; // Roots of the word if it is a compound
	private Set<Word> derivedWords;
	private Set<Affix> prefixes;
	private Set<Affix> suffixes;
	private Set<TransformPair> transformPairs; // All possible transforms the word is in and what role it has
	private Transform derivation; // The transform that actually derives the word
	private boolean analyze; // Use to block inferred words from analysis
	private boolean duplicate; // Used to mark temporary words generated in compounding
	private String externalAnalysis; // Analysis if set externally
	private boolean compound;
	protected String text;
	
	/**
	 * Create a new word
	 * @param text the text of the word
	 * @param count its count in the input
	 * @param analyze whether it should be analyzed
	 */
	public Word(String text, long count, boolean analyze) {
		this.text = text;
		this.count = count;
		this.freq = -1.0F; // Indicator that it has not been computed
		this.analyze = analyze;
		set = WordSet.UNMODELED;
		
		base = root = null;
		derivation = null;
		componentWords = null;
		externalAnalysis = null;
		compound = false;
		
		transformPairs = new THashSet<TransformPair>();
		
		derivedWords = new THashSet<Word>();
		prefixes = new THashSet<Affix>();
		suffixes = new THashSet<Affix>();
	}
	
	/**
	 * Generate the morphological analysis of a word
	 * @return the analysis
	 */
	public String analyze() {
		if (externalAnalysis != null) 
			return externalAnalysis;
		
		List<String> prefixes = new LinkedList<String>();
		List<String> suffixes = new LinkedList<String>();
		
		String rootText = analyzeRoot();
		
		// If the root word was inferred, add an asterisk
		if (root != null && !root.shouldAnalyze()) {
			rootText = rootText + '*';
		}
		
		// Traverse through the derivation chain
		Word currentWord = this;
		while (currentWord.getDerivation() != null) {
			// Get the transform that derived this word and place it
			// correctly based on whether it is a prefix or affix
			switch (currentWord.getDerivation().getAffixType()) {
			case PREFIX:
				prefixes.add(currentWord.getDerivation().analyze()); break;
			case SUFFIX:
				suffixes.add(0, currentWord.getDerivation().analyze()); break;
			default:
				throw new RuntimeException("Unhandled AffixType");
			}
			
			currentWord = currentWord.getBase();
		}
		
		// Build up the analysis string
		StringBuilder out = new StringBuilder();
		if (prefixes.size() > 0) {
			out.append(Util.join(prefixes, " ") + ' ');
		}
		
		out.append(rootText);
		
		if (suffixes.size() > 0) {
			out.append(' ' + Util.join(suffixes, " "));
		}
		
		return out.toString();
	}
	
	/**
	 * @return whether this word should be analyzed
	 */
	public boolean shouldAnalyze() {return analyze;}
	
	/**
	 * @return the count of the word
	 */
	public long getCount() {return count;}
	
	/**
	 * @return the word set the word belongs to
	 */
	public WordSet getSet() {return set;}

	/**
	 * Change the word set a word belongs to
	 * @param set the destination word set
	 */
	public void setSet(WordSet set) {this.set = set;}

	/**
	 * @return the word's base
	 */
	public Word getBase() {return base;}

	/**
	 * Set the word's base
	 * @param base the word's new base
	 */
	public void setBase(Word base) {this.base = base;}

	/**
	 * @return the word's root
	 */
	public Word getRoot() {return root;}

	/**
	 * Set the word's root
	 * @param root the word's new root
	 */
	public void setRoot(Word root) {
		this.root = root;
		
		// Set all forms derived from this one to have the same root
		for (Word derived: derivedWords) {
			if (derived == this) {
				throw new RuntimeException("Circular derivation: " + this);
			}
			
			derived.setRoot(root);
		}
	}
	
	/**
	 * Force the analysis of the word to be as specified 
	 * @param externalAnalysis the new analysis
	 */
	public void setExternalAnalysis(String externalAnalysis) {
		this.externalAnalysis = externalAnalysis;
	}

	/**
	 * Return true if this word has the specified affix
	 * @param affix the affix
	 * @return true if the word has the affix
	 */
	public boolean hasAffix(Affix affix) {
		switch (affix.getType()) {
		case PREFIX: return prefixes.contains(affix);
		case SUFFIX: return suffixes.contains(affix);
		default: throw new RuntimeException("Invalid AffixType");
		}
	}

	/**
	 * Add a word as a derived form
	 * @param derived the derive word
	 */
	public void addDerived(Word derived) {this.derivedWords.add(derived);}
	
	/**
	 * @return the derived words
	 */
	public Set<Word> getDerived() {return derivedWords;}

	/**
	 * @return the deriving transform
	 */
	public Transform getDerivation() {return derivation;}

	/**
	 * Set the deriving transform
	 * @param transform the new deriving transform
	 */
	public void setTransform(Transform transform) {this.derivation = transform;}

	/**
	 * Add an affix to the word
	 * @param affix the affix
	 */
	public void addAffix(Affix affix) {
		switch (affix.getType()) {
		case PREFIX: prefixes.add(affix); break;
		case SUFFIX: suffixes.add(affix); break;
		default: throw new RuntimeException("Invalid AffixType");
		}
	}

	/**
	 * @return all affixes the word participates in
	 */
	public Set<Affix> getAffixes() {
		Set<Affix> affixes = new THashSet<Affix>(prefixes);
		affixes.addAll(suffixes);
		return affixes;
	}
	
	/**
	 * @return all transforms the word participates in
	 */
	public Collection<TransformPair> getTransforms() {return transformPairs;}

	/**
	 * Add a transform pair that the word participates in
	 * @param pair the pair to add
	 */
	public void addTransformPair(TransformPair pair) {
		transformPairs.add(pair);
	}
	
	/**
	 * Remove a transform pair that the word participates in
	 * @param pair the pair to remove
	 */
	public void removeTransformPair(TransformPair pair) {
		if (!transformPairs.remove(pair)) {
			throw new RuntimeException("Cannot remove pair: " + pair);
		}
	}

	/**
	 * Set the frequency of a word
	 * @param tokenCount the frequency
	 */
	public void setFrequency(long tokenCount) {
		freq = ((double) count)/tokenCount;
	}
	
	/**
	 * Return true if the word is about the frequency and count thresholds
	 * @return true if the word is about the frequency and count thresholds
	 */
	public boolean isFrequent() {
		return count > COUNT_THRESHOLD && freq > FREQ_THRESHOLD;
	}

	/**
	 * Increment the token count of the word by the specified amount
	 * @param count the amount of increase the count
	 */
	public void addCount(long count) {
		// Increment the token count by the specified amount
		this.count += count;
	}
	
	/**
	 * Mark the word as a compound
	 * @param componentWords the words that make up the compound
	 */
	public void makeCompoundWord(List<Word> componentWords) {
		compound = true;
		setSet(WordSet.COMPOUND);
		this.componentWords = componentWords;		
	}
	
	/**
	 * @return true if the word is a compount
	 */
	public boolean isCompound() {
		return compound;
	}
	
	/**
	 * @return true if the word is a duplicate
	 */
	public boolean isDuplicate() {
		return duplicate;
	}
	
	/**
	 * Mark the word as a duplicate. This is used to mark words generated as
	 * compound fillers that are identical to previously observed forms.
	 */
	public void markDuplicate() {
		duplicate = true;
	}

	/**
	 * Generate the analysis of the word's root
	 * @return the word's root's analysis
	 */
	public String analyzeRoot() {
		if (getSet() == WordSet.COMPOUND) {
			// If it's a compound, return the analysis of all roots
			List<String> analyses = new LinkedList<String>();
			for (Word w : componentWords) {
				analyses.add(w.analyze());
			}
			
			return Util.join(analyses, " ");
		}
		else {
			// Use the root text if there is a root, otherwise just use the word's
			// text as its root
			return root != null ? root.toString().toUpperCase() : 
				toString().toUpperCase();
		}
	}

	/**
	 * @return the length of the word
	 */
	public int length() {
		return this.text.length();
	}

	/**
	 * @return the key to use for the word in a hash map
	 */
	public String getKey() {
		return text;
	}

	/**
	 * @return the text of the word
	 */
	public String getText() {
		return text;
	}

	/**
	 * Return a substring of the word's text
	 * @param start start index for substring
	 * @param end end index for substring
	 * @return the substring of the word
	 */
	public String getText(int start, int end) {
		return text.substring(start, end);
	}

	/**
	 * Compare two words
	 * @param other the other word
	 * @return the comparison of the texts of the two words
	 */
	public int compareTo(Word other) {
		return text.compareTo(other.getText());
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Word))
			return false;
		return text.equals(((Word) other).getText());
	}

	public int hashCode() {
		return text.hashCode();
	}

	public String toString() {
		return this.text;
	}
	
	/**
	 * Generate a representation of the word and all its derived forms
	 * @return a string representation of the word and all derived words
	 */
	public String toDerivedWordsString() {
		StringBuilder out = new StringBuilder();
		out.append(this.text);
		for (Word w : derivedWords)
			out.append("," + w.toString());
		return out.toString();
	}
}
