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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** Representation of the relationship between transforms. */
public class TransformRelation {
  private final Transform mainTransform;
  private final Map<Transform, Integer> precedingTransforms;

  /**
   * Create a new transformRelation for the given transform with empty relationships.
   *
   * @param t the transform to track relations to
   */
  public TransformRelation(Transform t) {
    this.mainTransform = t;
    precedingTransforms = new Object2ObjectOpenHashMap<>();
  }

  /**
   * Increment the count for a preceding transform.
   *
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

  /** @return the entries mapping preceding transforms to their counts */
  public Set<Entry<Transform, Integer>> getPrecedingTransformCounts() {
    return precedingTransforms.entrySet();
  }

  @SuppressWarnings("StringSplitter")
  public String toString() {
    StringBuilder out = new StringBuilder(mainTransform.toString() + "\n");

    for (Entry<Transform, Integer> e : precedingTransforms.entrySet()) {
      out.append(e.getKey()).append(" ").append(e.getValue());
    }

    return out.toString();
  }
}
