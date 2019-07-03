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

/**
 * Represent a combination of a word pair and the transform that relates the words of the pair. This
 * is used to allow a Word to keep track of what transforms cover it.
 */
public class TransformPair {
  private final Transform transform;
  private final WordPair pair;

  /**
   * Create a TranformPair from a Transform and a WordPair
   *
   * @param transform the Transform
   * @param pair the WordPair
   */
  public TransformPair(Transform transform, WordPair pair) {
    this.transform = transform;
    this.pair = pair;
  }

  /** @return the Transform */
  public Transform getTransform() {
    return transform;
  }

  /** @return the WordPair */
  public WordPair getPair() {
    return pair;
  }

  public String toString() {
    return transform + ": " + pair;
  }
}
