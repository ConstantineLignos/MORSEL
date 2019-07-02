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

/**
 * Represent a combination of a word pair and the transform that
 * relates the words of the pair. This is used to allow a Word to
 * keep track of what transforms cover it.
 *
 */
public class TransformPair {
	private Transform transform;
	private WordPair pair;
	
	/**
	 * Create a TranformPair from a Transform and a WordPair
	 * @param transform the Transform
	 * @param pair the WordPair
	 */
	public TransformPair(Transform transform, WordPair pair) {
		this.transform = transform;
		this.pair = pair;
	}
	
	/**
	 * @return the Transform
	 */
	public Transform getTransform() {return transform;}

	/**
	 * @return the WordPair
	 */
	public WordPair getPair() {return pair;}

	public String toString() {
		return transform + ": " + pair;
	}
}
