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
package org.lignos.morsel.lexicon;

/**
 * The WordSet enumeration gives the sets words can belong to in the learner. These sets represent
 * the status of the word in the learner's representation.
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
