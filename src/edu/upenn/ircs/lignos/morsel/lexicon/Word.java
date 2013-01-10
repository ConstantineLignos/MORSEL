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

public class Word {
	// These are set by the main learner, but defaults are provided here
	public static double FREQ_THRESHOLD = .000001F;
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
	
	public boolean shouldAnalyze() {return analyze;}
	
	public long getCount() {return count;}
	
	public WordSet getSet() {return set;}

	public void setSet(WordSet set) {this.set = set;}

	public Word getBase() {return base;}

	public void setBase(Word base) {this.base = base;}

	public Word getRoot() {return root;}

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
	
	public void setExternalAnalysis(String externalAnalysis) {
		this.externalAnalysis = externalAnalysis;
	}

	public boolean hasAffix(Affix affix) {
		switch (affix.getType()) {
		case PREFIX: return prefixes.contains(affix);
		case SUFFIX: return suffixes.contains(affix);
		default: throw new RuntimeException("Invalid AffixType");
		}
	}

	public void addDerived(Word derived) {this.derivedWords.add(derived);}
	
	public Set<Word> getDerived() {return derivedWords;}

	public Transform getDerivation() {return derivation;}

	public void setTransform(Transform transform) {this.derivation = transform;}

	public void addAffix(Affix affix) {
		switch (affix.getType()) {
		case PREFIX: prefixes.add(affix); break;
		case SUFFIX: suffixes.add(affix); break;
		default: throw new RuntimeException("Invalid AffixType");
		}
	}

	public Set<Affix> getAffixes() {
		Set<Affix> affixes = new THashSet<Affix>(prefixes);
		affixes.addAll(suffixes);
		return affixes;
	}
	
	public Collection<TransformPair> getTransforms() {return transformPairs;}

	public void addTransformPair(TransformPair pair) {
		transformPairs.add(pair);
	}
	
	public void removeTransformPair(TransformPair pair) {
		if (!transformPairs.remove(pair)) {
			throw new RuntimeException("Cannot remove pair: " + pair);
		}
	}

	public void setFrequency(long tokenCount) {
		freq = ((double) count)/tokenCount;
	}
	
	public boolean isFrequent() {
		return count > COUNT_THRESHOLD && freq > FREQ_THRESHOLD;
	}

	public void addCount(long count) {
		// Increment the token count by the specified amount
		this.count += count;
	}
	
	public void makeCompoundWord(List<Word> componentWords) {
		compound = true;
		setSet(WordSet.COMPOUND);
		this.componentWords = componentWords;		
	}
	
	public boolean isCompound() {
		return compound;
	}
	
	public boolean isDuplicate() {
		return duplicate;
	}
	
	public void markDuplicate() {
		duplicate = true;
	}

	public String analyzeRoot() {
		// Return the analysis of the word's root
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

	public int length() {
		return this.text.length();
	}

	public String getKey() {
		return text;
	}

	public String getText() {
		return text;
	}

	public String getText(int start, int end) {
		return text.substring(start, end);
	}

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
	
	public String toDerivedWordsString() {
		StringBuilder out = new StringBuilder();
		out.append(this.text);
		for (Word w : derivedWords)
			out.append("," + w.toString());
		return out.toString();
	}
}
