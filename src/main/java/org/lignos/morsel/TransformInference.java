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
package org.lignos.morsel;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Map;
import java.util.Map.Entry;
import org.lignos.morsel.lexicon.Lexicon;
import org.lignos.morsel.lexicon.Word;
import org.lignos.morsel.lexicon.WordSet;
import org.lignos.morsel.transform.Transform;
import org.lignos.morsel.transform.TransformRelation;

/** Enable inferring sequential relationships between learned transforms. */
public class TransformInference {
  private static final int MIN_COUNT = 1;
  private Map<Transform, TransformRelation> relations;
  private Object2ObjectOpenHashMap<Transform, ObjectOpenHashSet<Transform>> goodRelations;

  /**
   * Infer the relationships between transforms as shown by the derived forms in the lexicon.
   *
   * @param lex the lexicon to examine
   */
  public void inferRelations(Lexicon lex) {
    // Reset the relations
    relations = new Object2ObjectOpenHashMap<>();
    goodRelations = new Object2ObjectOpenHashMap<>();

    // Loop over the derived words to learn the relationships
    for (Word w : lex.getSetWords(WordSet.DERIVED)) {
      // Ignore any words not in the original data
      if (!w.shouldAnalyze()) continue;

      // The deriving transform
      Transform followingTransform = w.getDerivation();

      // The deriving transform of its base, which will be null for
      // root words
      Transform precedingTransform = w.getBase().getDerivation();

      // Skip it if they're not of the same type
      if (precedingTransform != null
          && followingTransform.getAffixType() != precedingTransform.getAffixType()) continue;

      // Add this to the known relations
      addRelation(precedingTransform, followingTransform);
    }

    // Compute good relations
    markGoodRelations();
  }

  /** Add any observed relationship between transforms to the good relations map. */
  private void markGoodRelations() {
    // For now, just any seen relationship
    for (Entry<Transform, TransformRelation> e1 : relations.entrySet()) {
      // Mark each relation good by a nested map of following, then preceding
      Transform followingTransform = e1.getKey();
      TransformRelation tRelation = e1.getValue();

      // Make a new nested map
      ObjectOpenHashSet<Transform> goodPreceders = new ObjectOpenHashSet<>();
      goodRelations.put(followingTransform, goodPreceders);
      for (Entry<Transform, Integer> e2 : tRelation.getPrecedingTransformCounts()) {
        // Add it if the amount is > 1
        if (e2.getValue() > MIN_COUNT) {
          goodPreceders.add(e2.getKey());
        }
      }
    }
  }

  /**
   * Add the preceding/following relationship to the relations map.
   *
   * @param precedingTransform the preceding transform
   * @param followingTransform the following transform
   */
  private void addRelation(Transform precedingTransform, Transform followingTransform) {
    // Add the preceding transform to the map if needed
    if (!relations.containsKey(followingTransform)) {
      relations.put(followingTransform, new TransformRelation(followingTransform));
    }
    relations.get(followingTransform).incrementPreceder(precedingTransform);
  }

  /**
   * Return whether the sequence a transform following another is believed to be good.
   *
   * @param preceding the first transform, which may be null to signify a root
   * @param following the second transform
   * @return whether the second can follow the first
   */
  public boolean isGoodRelation(Transform preceding, Transform following) {
    // If the two are of two different affix types, treat preceding
    // as root
    if (preceding != null && preceding.getAffixType() != following.getAffixType()) {
      preceding = null;
    }

    ObjectOpenHashSet<Transform> goodPreceders = goodRelations.get(following);

    // If the preceding transform is unheard of, we throw an exception
    // because it means our information must be out of date.
    if (goodPreceders == null) throw new RuntimeException("Transform relations are out of date.");

    // Just return whether preceding is in the set
    return goodPreceders.contains(preceding);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    // Print out each transform and the counts of what preceded it
    StringBuilder out = new StringBuilder();
    for (Entry<Transform, TransformRelation> e1 : relations.entrySet()) {
      Transform preceding = e1.getKey();
      TransformRelation tRelation = e1.getValue();
      out.append(preceding.toString()).append(" \t");
      for (Entry<Transform, Integer> e2 : tRelation.getPrecedingTransformCounts()) {
        // Skip low counts
        if (e2.getValue() <= MIN_COUNT) continue;

        String preceder = e2.getKey() == null ? "root" : e2.getKey().toString();
        out.append(preceder).append(":").append(e2.getValue()).append(" ");
      }
      out.append("\n");
    }
    return out.toString();
  }
}
