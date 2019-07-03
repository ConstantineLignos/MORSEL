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

import java.util.Comparator;
import org.lignos.morsel.lexicon.Word;

/** Represent two related words */
public class WordPair {
  private final Word base;
  private final Word derived;
  private final boolean accommodated;
  private final int hash;

  /**
   * Creat a WordPair from based and derived words and whether they were accommodated
   *
   * @param base the base
   * @param derived the derived word
   * @param accommodated whether orthographic accommodation was used to produce the pair
   */
  public WordPair(Word base, Word derived, boolean accommodated) {
    this.base = base;
    this.derived = derived;
    this.accommodated = accommodated;

    hash = (base.getText() + derived.getText() + (accommodated ? 'a' : 'n')).hashCode();
  }

  /** @return the base of the pair */
  public Word getBase() {
    return base;
  }

  /** @return the derived form of the pair */
  public Word getDerived() {
    return derived;
  }

  /** @return whether orthographic accommodation was used to create the pair */
  public boolean isAccommodated() {
    return accommodated;
  }

  public boolean equals(Object other) {
    if (other == null || !(other instanceof WordPair)) return false;
    WordPair otherWord = (WordPair) other;
    return otherWord.getBase() == base
        && otherWord.getDerived() == derived
        && otherWord.isAccommodated() == accommodated;
  }

  public int hashCode() {
    return hash;
  }

  public String toString() {
    return base + "/" + derived;
  }

  /** Compare pairs by their base and derived forms */
  public static class PairStringComparator implements Comparator<WordPair> {
    @Override
    public int compare(WordPair pair1, WordPair pair2) {
      final String key1 = pair1.getBase().getText() + ' ' + pair1.getDerived().getText();
      final String key2 = pair2.getBase().getText() + ' ' + pair2.getDerived().getText();
      return key1.compareTo(key2);
    }
  }
}
