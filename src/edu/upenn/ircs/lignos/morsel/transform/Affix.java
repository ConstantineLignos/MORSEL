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

import edu.upenn.ircs.lignos.morsel.lexicon.Word;
import gnu.trove.THashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * The Affix class represents affixes, tracking the words they contain and
 * statistics about each affix.
 *
 */
public class Affix {
	private final static int MAX_AFFIX_LENGTH = 5;
	private final static int MIN_STEM_LENGTH = 3;
	
	private String text;
	private AffixType type;
	private long typeCount = 0L;
	private long freqTypeCount = -1L;
	private long tokenCount = 0L;
	private Set<Word> wordSet;
	private int length;
	private int weight;
	// Set to -1 so it is clear when the counting functions have never been called
	private long baseTypeCount = -1L;
	private long derivedTypeCount = -1L;
	private long unmodTypeCount = -1L;
	
	/**
	 * Create an affix, setting its counts to zero and making an empty wordset.
	 * @param text The text of the affix, such as "ed"
	 * @param type The AffixType of the affix, such as prefix or suffix.
	 */
	public Affix(String text, AffixType type) {
		this.text = text;
		this.type = type;
		typeCount = 0;
		freqTypeCount = 0;
		baseTypeCount = -1;
		derivedTypeCount = -1;
		unmodTypeCount = -1;
		tokenCount = 0L;
		wordSet = new THashSet<Word>();
		
		length = text.length();
		weight = Math.max(length, 1);
	}
	
	
	/**
	 * Returns true if the affix is null.
	 * 
	 * @return true if the affix is null, otherwise false
	 */
	public boolean isNull() {return text.length() == 0;}
	
	/**
	 * Returns the text the affix represents.
	 * 
	 * @return the text of the affix
	 */
	public String getText() {return text;}

	/**
	 * Returns the number of word types that contain the affix.
	 * 
	 * @return the type count of the affix
	 */
	public long getTypeCount() {return typeCount;}
	
	/**
	 * Returns the number of frequent word types (as given by Word.isFrequent() 
	 * that contain the affix.
	 * 
	 * @return the number of frequent word types for the affix
	 */
	public long getFreqTypeCount() {return freqTypeCount;}
	
	/**
	 * Returns the number of word types that contain the affix multiplied by the
	 * weight of the affix.
	 * 
	 * @return the weighted type count of the affix
	 */
	public long getWeightedTypeCount() {return weight * typeCount;}

	/**
	 * Returns the number of word types in the Base set that contain the affix.
	 * 
	 * @return the Base type count of the affix
	 */
	public long getBaseTypeCount() {return baseTypeCount;}
	
	/**
	 * Returns the number of word types in the Base set that contain the affix, 
	 * multiplied by the weight of the affix.
	 * 
	 * @return the weighted Base type count of the affix
	 */
	public long getWeightedBaseTypeCount() {return weight * baseTypeCount;}

	/**
	 * Returns the number of word types in the Derived set that contain the 
	 * affix.
	 * 
	 * @return the Derived type count of the affix
	 */
	public long getDerivedTypeCount() {return derivedTypeCount;}

	/**
	 * Returns the number of word types in the Derived set that contain the 
	 * affix, multiplied by the weight of the affix.
	 * 
	 * @return the weighted Derived type count of the affix
	 */
	public long getWeightedDerivedTypeCount() {return weight * derivedTypeCount;}
	
	/**
	 * Returns the number of word types in the Unmodeled set that contain the 
	 * affix.
	 * 
	 * @return the Unmodeled type count of the affix
	 */
	public long getUnmodTypeCount() {return unmodTypeCount;}
	
	/**
	 * Returns the number of word types in the Unmodeled set that contain the 
	 * affix, multiplied by the weight of the affix.
	 * 
	 * @return the weighted Unmodeled type count of the affix
	 */
	public long getWeightedUnmodTypeCount() {return weight * unmodTypeCount;}

	/**
	 * Returns the number of word tokens that contain the affix.
	 * 
	 * @return the token count of the affix
	 */
	public long getTokenCount() {return tokenCount;}

	/**
	 * Returns the AffixType of the affix (e.g. prefix, suffix).
	 * 
	 * @return the AffixType of this affix.
	 */
	public AffixType getType() {return type;}

	/**
	 * Returns the words that contain the affix.
	 * 
	 * @return the words that contain the affix.
	 */
	public Set<Word> getWordSet() {return wordSet;}
	
	/**
	 * Counts the number of frequent words in the affix's wordset. This needs
	 * to be called each time the lexicon changes, as word frequency changes with
	 * the addition of words to the lexicon.
	 */
	public void countFreqWords() {
		// Count the number of frequent types
		freqTypeCount = 0;
		for (Word word: wordSet) {
			if (word.isFrequent()) {
				freqTypeCount++;
			}
		}
	}
	
	/**
	 * Count the number of frequent words in each WordSet (Base, Derived, 
	 * Unmodeled). This needs to be called each time the lexicon changes, as 
	 * word frequency changes with the addition of words to the lexicon.
	 */
	public void countWordSets() {
		// Reset the counts
		baseTypeCount = derivedTypeCount = unmodTypeCount = 0;

		// Count each word toward the correct set, but only if it's frequent
		for (Word word: wordSet) {
			if (!word.isFrequent()) {
				continue;
			}
			
			switch (word.getSet()) {
			case BASE: baseTypeCount++; break;
			case DERIVED: derivedTypeCount++; break;
			case UNMODELED: unmodTypeCount++; break;
			// Ignore all other word sets
			default:
				break;
			}
		}
	}

	/**
	 * Returns the length of the text of the affix.
	 * 
	 * @return length of the affix's text
	 */
	public int length() {return length;}

	/**
	 * Add a word to the affix's word set and update type and token counts
	 * accordingly.
	 * 
	 * @param word the word to be added
	 */
	public void addWord(Word word) {
		// Update counts
		incTypeCount();
		incTokenCount(word.getCount());
		
		// Add the word to the word set
		wordSet.add(word);
	}
	
	/**
	 * Increment the affix's type count.
	 */
	public void incTypeCount () {this.typeCount += 1;}
	
	/**
	 * Increment the affix's token count by the specified amount.
	 * 
	 * @param amount the amount to increment by
	 */
	public void incTokenCount (long amount) {this.tokenCount += amount;}
	
	/**
	 * Return all the affix strings in a word of the specified AffixType.
	 * 
	 * @param word the word to process
	 * @param type the type of affixes to extract
	 * @return an array of Strings containing the text of each affix in the word
	 */
	public static String[] getAffixes(Word word, AffixType type){
		int length = word.length();
		
		List<String> affixes = new LinkedList<String>();
		
		if (length >= MIN_STEM_LENGTH) {
			affixes.add("");
		}
		
		// Move through the word, getting an affix from the current position
		for (int i = 1; i < length; i++) {
			if (type == AffixType.PREFIX) {
				if (i <= MAX_AFFIX_LENGTH && length - i >= MIN_STEM_LENGTH) {
					affixes.add(word.getText(0, i));
				}
			}
			else if (type == AffixType.SUFFIX) {
				if (length - i <= MAX_AFFIX_LENGTH && i >= MIN_STEM_LENGTH) {
					affixes.add(word.getText(i, length));
				}
			}
			else {
				throw new RuntimeException("Unhandled affix type.");
			}
		}

		return affixes.toArray(new String[0]);
	}
	
	public String toString() {
		// Return the text, but if we're null return "$"
		if (text.length() > 0) {
			return(text);
		}
		else {
			return "$";
		}
	}
	
	/**
	 * Return a detailed string representation of the transform.
	 * 
	 * @return the transform's details
	 */
	public String toVerboseString() {
		return(toString() + '\n' + "Types: " + typeCount + ", Tokens: " + tokenCount);
	}

	/**
	 * Returns true if the affix contains the specified word in its word set, 
	 * false otherwise.
	 * 
	 * @param word the word to check
	 * @return true if the word is in the affix's word set, false otherwise
	 */
	public boolean hasWord(Word word) {
		return wordSet.contains(word);
	}


	/**
	 * Returns true if affix1 is removed and then just stuck back on,
	 * for example the suffixes (e, ed) or prefixes (e, de). This also applies 
	 * to substrings of the whole affix, like the cases +(le, ly) and (be, de)+
	 * @param affix1 The affix being replaced
	 * @param affix2 The affix being added
	 * @param type The type of the affixes
	 * @return true if the combination is bad, false otherwise.
	 */
	public static boolean isBadAffixPair(Affix affix1, Affix affix2, AffixType type) {
		// Special case- if affix1 is null, the pair cannot be bad
		if (affix1.length() == 0) {
			return false;
		}
		
		String slice1;
		String slice2;
		// Take increasingly larger pieces off of affix1 to test, starting
		// with length 1
		for (int i = 1; i <= affix1.length(); i++) {
			// If the slice is too big for affix2, bail
			if (i > affix2.length()) {
				break;
			}
			switch (type) {
			case PREFIX:
				slice1 = affix1.getText().substring(affix1.length() - i, affix1.length());
				slice2 = affix2.getText().substring(affix2.length() - i, affix2.length());
				if (slice1.equals(slice2)) {
					return true;
				}
				break;
			case SUFFIX:
				slice1 = affix1.getText().substring(0, i); 
				slice2 = affix2.getText().substring(0, i);
				if (slice1.equals(slice2)) {
					return true;
				}
				break;
			default: throw new RuntimeException("Unhandled AffixType");
			}
		}
		
		// If we make it through without an issue, it's a good pair
		return false;
	}
	
	
	/**
	 * Return whether text has an affix, taking into account whether the stem
	 * would be too short.
	 * @param text the text
	 * @param affix the affix
	 * @return whether the text has the affix
	 */
	public static boolean hasAffix(String text, Affix affix) {		
		// Answer false for words that are too short
		int affixLen = affix.length();
		if (text.length() - affixLen < MIN_STEM_LENGTH)
			return false;
		
		if (affix.getType() == AffixType.PREFIX) { 
			return affix.getText().equals(text.substring(0, affixLen));
		}
		else if (affix.getType() == AffixType.SUFFIX) { 
			return affix.getText().equals(text.substring(text.length() - affixLen, text.length()));
		}
		else {
			throw new RuntimeException("Unhandled Affix Type");
		}
	}
}
