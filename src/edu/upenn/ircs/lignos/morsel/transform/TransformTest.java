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

import junit.framework.TestCase;

/**
 * Test the Transform representation.
 *
 */
public class TransformTest extends TestCase {

	/**
	 * Test making a derived form without accommodation.
	 */
	public void testmakeDerivedBasic() {
		Affix edAffix = new Affix("ed", AffixType.SUFFIX);
		assertEquals("pined", Transform.makeDerived("pin", edAffix, false, false));
	}
	
	/**
	 * Test making a derived form with doubling.
	 */
	public void testmakeDerivedDoubled() {
		Affix edAffix = new Affix("ed", AffixType.SUFFIX);
		assertEquals("pinned", Transform.makeDerived("pin", edAffix, true, false));
	}
	
	/**
	 * Test making a derived form with undoubling.
	 */
	public void testmakeDerivedUndoubled() {
		Affix edAffix = new Affix("ed", AffixType.SUFFIX);
		assertEquals("caned", Transform.makeDerived("cane", edAffix, false, true));
	}
}
