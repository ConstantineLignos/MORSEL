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

import edu.upenn.ircs.lignos.morsel.Util;
import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;
import edu.upenn.ircs.lignos.morsel.lexicon.WordSet;
import gnu.trove.THashSet;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Transform {
	private static final int N_SAMPLES = 3;
	private Affix affix1;
	private Affix affix2;
	private AffixType affixType;
	private int typeCount = 0;
	private int tokenCount = 0;
	private int normalPairCount = 0;
	private int accomPairCount = 0; // Pairs of words formed by an accomodation
	private Set<WordPair> derivationPairs;
	private Set<WordPair> unmovedDerivationPairs;
	private boolean learned;
	private int length;
	private int hash;
	
	public Transform(Affix affix1, Affix affix2) {
		this.affix1 = affix1;
		this.affix2 = affix2;
		derivationPairs = new THashSet<WordPair>();
		unmovedDerivationPairs = null;
		
		// Make sure the affixes are of the same type, and set the affixType
		// to that
		if (affix1.getType() != affix2.getType()) {
			throw new RuntimeException("A transform can only be created from" + 
					" two affixes of the same type.");
		}
		affixType = affix1.getType();
		
		length = Math.abs(affix2.length() - affix1.length());
		
		learned = false;
		
		hash = (affix1.getText() + affix2.getText() + affixType.ordinal()).hashCode();
	}
	
	public void addWordPair(Word base, Word derived, boolean isAccomodated) {
		WordPair pair = new WordPair(base, derived, isAccomodated);
		
		// Increment counts if both words in the pair are frequent enough
		if (base.isFrequent() && derived.isFrequent()) {
			typeCount++;
			tokenCount += base.getCount() + derived.getCount();
			
			if (isAccomodated) {
				accomPairCount++;
			}
			else {
				normalPairCount++;
			}
		}
		
		// Add the pair
		derivationPairs.add(pair);
		
		// If the transform has already been learned, track unmoved pairs
		if (learned) {
			unmovedDerivationPairs.add(pair);
		}
	
		// Add the transform to the words
		TransformPair tPair = new TransformPair(this, pair);
		base.addTransformPair(tPair);
		derived.addTransformPair(tPair);
	}
	
	public void removeWordPair(WordPair pair) {
		Word base = pair.getBase();
		Word derived = pair.getDerived();
		
		// Remove the pair and decrement counts
		if (!derivationPairs.remove(pair)) {
			throw new RuntimeException("Cannot remove pair: " + pair);
		}
		
		// Decrement count only if the pair was frequent
		if (base.isFrequent() && derived.isFrequent()) {
			typeCount--;
			tokenCount -= base.getCount() + derived.getCount();
			
			// Change the normal/accom count based on the pair
			if (pair.isAccomodated()) {
				accomPairCount--;
			}
			else {
				normalPairCount--;
			}
		}
		
		// The caller is responsible for removing the transform from the word.
		// WordPairs should never be removed after learning, so we do not
		// remove from unmovedDerivationPairs.
	}
	
	public int length() {return length;}
		
	public boolean isLearned() {return learned;}
	
	public void markLearned() {
		learned = true;
		// As an optimization we lazily allocate this
		unmovedDerivationPairs = new THashSet<WordPair>();	
	}
	
	public Affix getAffix1() {return affix1;}

	public Affix getAffix2() {return affix2;}

	public int getTypeCount() {return typeCount;}
	
	public int getWeightedTypeCount() {
		// If the length is zero, count it as one
		return typeCount * Math.max(length(), 1);
	}

	public int getTokenCount() {return tokenCount;}

	public Set<WordPair> getWordPairs() {return derivationPairs;}

	public int getNormalPairCount() {return normalPairCount;}

	public int getAccomPairCount() {return accomPairCount;}

	public AffixType getAffixType() {return affixType;}

	public Set<WordPair> getUnmovedDerivationPairs() {return unmovedDerivationPairs;}

	public void resetUnmoved() {unmovedDerivationPairs = new THashSet<WordPair>();}

	public static void scoreTransform(Transform trans, Lexicon lex, 
			boolean reEval, boolean doubling) {
		// Get the words from the first affix of the transform
		Set<Word> affix1Words = trans.getAffix1().getWordSet();
		
		// Check each word
		for (Word base: affix1Words) {
			scoreWord(trans, base, lex, reEval, doubling);
		}
	}
	
	public static boolean scoreWord(Transform trans, Word base, Lexicon lex,
			boolean reEval, boolean doubling) {

		// If the base is illegal, skip it
		if (!isLegalBaseSet(base.getSet(), reEval)) {
			return false;
		}
		
		// Extract fields
		Affix affix1 = trans.getAffix1();
		Affix affix2 = trans.getAffix2();
		WordSet baseSet = base.getSet();
		
		// Make the normal derived form
		String stem = makeStem(base, affix1);
		
		// Try to get the derived word
		Word derived = lex.getWord(makeDerived(stem, affix2, false, false));
		
		// If we got a word and the pair is legal, add it and move on
		if (derived != null && isLegalPairSets(baseSet, derived.getSet(), reEval) &&
				affix2.hasWord(derived)) {
			trans.addWordPair(base, derived, false);
			return true;
		}
		
		// Otherwise, try some other forms if doubling is on
		// If affix1 is null, try doubling and undoubling
		if (doubling && affix1.isNull()) {
			// Get doubled stem and see if it's a word
			derived = lex.getWord(makeDerived(stem, affix2, true, false));
			
			// If we got a word and the pair is legal, add it and move on
			if (derived != null && affix2.hasWord(derived) &&  
					isLegalPairSets(baseSet, derived.getSet(), reEval)) {
				trans.addWordPair(base, derived, true);
				return true;
			}
			
			// If the last letter of the stem and the first of affix2 are
			// the same, try undoubling.
			if (stem.substring(stem.length() - 1, stem.length()).equals(
					affix2.getText().substring(0, 1))) {
				// Get undoubled stem and see if it's a word
				derived = lex.getWord(makeDerived(stem, affix2, false, true));
				
				// If we got a word, the derived form is not the same as the base,
				// and the pair is legal, add it and move on
				if (derived != null && derived != base && isLegalPairSets(baseSet, 
						derived.getSet(), reEval) && affix2.hasWord(derived)) {
					trans.addWordPair(base, derived, true);
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isLegalBaseSet(WordSet set, boolean reEval) {
		if (reEval) {
			// Anything can be a base
			return true;
		}
		else {
			// Everything except derived is ok
			return set != WordSet.DERIVED;
		}
	}
	
	private static boolean isLegalPairSets(WordSet baseSet, WordSet derivedSet, 
			boolean reEval) {
		// reEval false, legal pairs: (B, U) and (U, U)
		// reEval true, additionally: (B, B) and (D, U)
		switch(baseSet) {
		case BASE:
			switch (derivedSet) {
			// (B, U) always allowed
			case UNMODELED: return true;
			// (B, B) allowed if reEval
			case BASE: return reEval;
			// All else illegal
			default: return false;
			}
		case DERIVED:
			switch (derivedSet) {
			// (D, U) is legal if reEval is
			case UNMODELED: return reEval;
			// All else illegal
			default: return false;
			}
		case UNMODELED:
			switch (derivedSet) {
			// (U, U) always allowed
			case UNMODELED: return true;
			// All else illegal
			default: return false;
			}
		default: return false;
		}
	}
	
	public static String makeStem(Word word, Affix affix) {
		int affixLen = affix.length();
		if (affix.getType() == AffixType.PREFIX) { 
			return word.getText(affixLen, word.length());
		}
		else if (affix.getType() == AffixType.SUFFIX) { 
			return word.getText(0, word.length() - affixLen);
		}
		else {
			throw new RuntimeException("Unhandled Affix Type");
		}
	}
	
	public static String makeStem(String word, Affix affix) {
		int affixLen = affix.length();
		if (affix.getType() == AffixType.PREFIX) { 
			return word.substring(affixLen, word.length());
		}
		else if (affix.getType() == AffixType.SUFFIX) { 
			return word.substring(0, word.length() - affixLen);
		}
		else {
			throw new RuntimeException("Unhandled Affix Type");
		}
	}
	
	
	public static String makeDerived(String stem, Affix affix, boolean doubling,
			boolean undoubling) {
		
		// Only allow double or undouble, not both
		if (doubling && undoubling) {
			throw new RuntimeException("Can only specify double or undouble, not both.");
		}
		
		if (doubling) {
			// Repeat the last character of the stem
			stem = stem + stem.substring(stem.length() - 1, stem.length());
		}
		else if (undoubling) {
			// Remove the last character of the stem
			stem = stem.substring(0, stem.length() - 1);
		}
		
		if (affix.getType() == AffixType.PREFIX) { 
			return affix.getText() + stem;
		}
		else if (affix.getType() == AffixType.SUFFIX) { 
			return stem + affix.getText();
		}
		else {
			throw new RuntimeException("Unhandled Affix Type");
		}
	}
	
	public String getPairsText() {
		List<String> pairs = new LinkedList<String>();
		for (WordPair pair: derivationPairs) {
			pairs.add(pair.toString());
		}
		Collections.sort(pairs);
		return Util.join(pairs, " ");
	}
	
	public String getSamplePairs() {
		int n = 0;
		List<String> pairs = new LinkedList<String>();
		for (WordPair pair : derivationPairs) {
			if (n++ >= N_SAMPLES) {break;}
			pairs.add(pair.toString());
		}
		Collections.sort(pairs);
		return "(" + derivationPairs.size() + ") " + Util.join(pairs, ", ");
	}
	
	public String toString() {
		String affixes = '(' + affix1.toString() + ", " + affix2.toString() + ')';
		// Put the sign in the right place
		switch(affixType) {
		case PREFIX: return affixes + '+';
		case SUFFIX: return  '+' + affixes;
		default: throw new RuntimeException("Unhandled AffixType");
		}
	}
	
	public String toVerboseString() {
		return(toString() + '\n' + "Weighted Types: " + getWeightedTypeCount() + 
				", Types: " + typeCount + ", Tokens: " + 
				tokenCount + ", Pairs: " + derivationPairs.size() +
				", Normal/Accom. Pairs: " + normalPairCount + "/" + accomPairCount + 
				"\nSamples: " + getSamplePairs());
	}
	
	public String toDumpString() {
		StringBuilder out = new StringBuilder(toString() + '\n' + "Weighted Types: " + 
				getWeightedTypeCount() + ", Types: " + typeCount + ", Tokens: " + 
				tokenCount + ", Pairs: " + derivationPairs.size() +
				", Normal/Accom. Pairs: " + normalPairCount + "/" + accomPairCount);
		for (WordPair pair : derivationPairs) {
			out.append(pair.toString() + " ");
		}
		out.append("\n");
		return out.toString();
	}

	public String analyze() {
		StringBuilder analysis = new StringBuilder();

		// Put in an inital parens
		analysis.append('(');
		
		String sign = "";
		if (affix1.isNull()) {
			// If affix1 is null, only output affix2 and note an addition
			analysis.append(affix2.toString());
			sign = "+";
		}
		else if (affix2.isNull()) {
			// If affix2 is null, only output affix1, and note a subtraction
			analysis.append(affix1.toString());
			sign = "-";
		}
		else {
			// Otherwise output only affix2
			analysis.append(affix2.toString());
			sign = "+";
		}
		
		// Put in a closing parens
		analysis.append(')');
		
		// Put the sign in the right place
		switch(affixType) {
		case PREFIX: analysis.append(sign); break;
		case SUFFIX: analysis.insert(0, sign); break;
		default: throw new RuntimeException("Unhandled AffixType");
		}
        
        return analysis.toString();
	}

	public String toKey() {
		String transType;
		switch (affixType) {
		case PREFIX: transType = "p"; break;
		case SUFFIX: transType = "s"; break;
		default: throw new RuntimeException("Unhandled AffixType");
		}
		
		return transType + ':' + affix1 + ',' + affix2;
	}
	
	public static String inferBase(Word w, Transform trans) {
		// "Undo" the transform on word w
		switch (trans.getAffixType()) {
		case PREFIX:
			return trans.getAffix1().getText() + 
				w.getText(trans.getAffix2().length(), w.length());
		case SUFFIX:
			return w.getText(0, w.length() - trans.getAffix2().length()) + 
				trans.getAffix1().getText();
		default: throw new RuntimeException("Unhandled AffixType");
		}
	}
	
	public static float calcSegPrecision(Transform trans) {
		// Segmentation precision is the number of type count of the transform
		// divided by the type count of affix2
		return trans.typeCount / (float) trans.affix2.getFreqTypeCount();
	}
	
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Transform))
			return false;
		Transform otherTransform = (Transform) other;
		return otherTransform.getAffix1() == affix1 && otherTransform.getAffix2() == affix2;
	}
	
	public int hashCode() {
		return hash;
	}
}
