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

/**
 * The WordSet enumeration gives the sets words can belong to in the learner.
 * These sets represent the status of the word in the learner's representation.
 *
 */
public enum WordSet {
	/** Words that are unanalyzed */
	UNMODELED,
	/** Words that are the bases of transforms */
	BASE,
	/** Words derived by transforms */
	DERIVED,
	/** Words derived by compounding */
	COMPOUND
}
