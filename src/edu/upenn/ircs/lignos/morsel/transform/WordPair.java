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

/**
 * Represent two related words
 *
 */
public class WordPair {
	private Word base;
	private Word derived;
	private boolean accomodated;
	private int hash;
	
	/**
	 * Creat a WordPair from based and derived words and whether they were accommodated
	 * @param base the base
	 * @param derived the derived word
	 * @param accomodated whether orthographic accommodation was used to produce the pair
	 */
	public WordPair(Word base, Word derived, boolean accomodated) {
		this.base = base;
		this.derived = derived;
		this.accomodated = accomodated;
		
		hash = (base.getText() + derived.getText() + (accomodated ? 'a' : 'n')).hashCode();
	}

	/**
	 * @return the base of the pair
	 */
	public Word getBase() {return base;}

	/**
	 * @return the derived form of the pair
	 */
	public Word getDerived() {return derived;}
	
	/**
	 * @return whether orthographic accommodation was used to create the pair
	 */
	public boolean isAccomodated() {return accomodated;}
	
	public boolean equals(Object other) {
		if (other == null || !(other instanceof WordPair))
			return false;
		WordPair otherWord = (WordPair) other;
		return otherWord.getBase() == base && otherWord.getDerived() == derived && 
			otherWord.isAccomodated() == accomodated;
	}
	
	public int hashCode() {
		return hash;
	}

	public String toString() {
		return base + "/" + derived;
	}
}
