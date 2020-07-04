/*
 * Copyright 2009-2019 Constantine Lignos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lignos.morsel.transform;

import junit.framework.TestCase;

/** Test the Transform representation. */
public class TransformTest extends TestCase {
  private static final Affix edSuffix = new Affix("ed", AffixType.SUFFIX);
  private static final Affix unPrefix = new Affix("un", AffixType.PREFIX);

  /** Test making a derived form without accommodation. */
  public void testmakeDerivedBasic() {
    assertEquals("pined", Transform.makeDerived("pin", edSuffix, false, false));
    assertEquals("unpin", Transform.makeDerived("pin", unPrefix, false, false));
  }

  /** Test making a derived form with doubling. */
  public void testmakeDerivedDoubled() {
    assertEquals("pinned", Transform.makeDerived("pin", edSuffix, true, false));
    assertEquals("unppin", Transform.makeDerived("pin", unPrefix, true, false));
  }

  /** Test making a derived form with undoubling. */
  public void testmakeDerivedUndoubled() {
    assertEquals("caned", Transform.makeDerived("cane", edSuffix, false, true));
    assertEquals("uneed", Transform.makeDerived("need", unPrefix, false, true));
  }
}
