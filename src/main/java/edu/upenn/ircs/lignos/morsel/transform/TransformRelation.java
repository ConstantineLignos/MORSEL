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

import gnu.trove.THashMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Representation of the relationship between transforms.
 */
public class TransformRelation {
	private final Transform mainTransform;
	private Map<Transform, Integer> precedingTransforms;
	
	/**
	 * Create a new transformRelation for the given transform with empty
	 * relationships.
	 * @param t the transform to track relations to
	 */
	public TransformRelation(Transform t) {
		this.mainTransform = t;
		precedingTransforms = new THashMap<Transform, Integer>();
	}
	
	
	/**
	 * Increment the count for a preceding transform.
	 * @param preceder the preceding transform
	 */
	public void incrementPreceder(Transform preceder) {
		// Add the transform if it's not there
		if (!precedingTransforms.containsKey(preceder)) {
			precedingTransforms.put(preceder, 0);
		}
		// Increment it
		precedingTransforms.put(preceder, precedingTransforms.get(preceder) + 1);
	}
	
	
	/**
	 * @return the entries mapping preceding transforms to their counts
	 */
	public Set<Entry<Transform, Integer>> getPrecedingTransformCounts() {
		return precedingTransforms.entrySet();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		StringBuilder out = new StringBuilder(mainTransform.toString() + "\n");
		
		for (Entry<Transform, Integer> e : precedingTransforms.entrySet()) {
			out.append(e.getKey() + " " + e.getValue());
		}
		
		return out.toString();
	}
}
