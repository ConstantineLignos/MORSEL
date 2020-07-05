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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.lignos.morsel.lexicon.Lexicon;
import org.lignos.morsel.lexicon.Word;
import org.lignos.morsel.lexicon.WordSet;
import org.lignos.morsel.transform.Transform;

/** Allow inference of unseen forms using learned transforms. */
public class RuleInference {
  private final Set<String> inferredBases;

  /** Create an new inference instance with an empty set of inferred bases. */
  public RuleInference() {
    inferredBases = new ObjectOpenHashSet<>();
  }

  /**
   * Infer new bases using the given lexicon and transforms
   *
   * @param lex the lexicon
   * @param transform the learned transforms
   * @return a Set of inferred words
   */
  private Collection<Word> inferBases(Lexicon lex, Transform transform) {
    // Go over each unmodeled word with affix2 of the transform. If
    // its hypothesized base is not a word, infer it
    Set<Word> newWords = new ObjectOpenHashSet<>();
    for (Word w : transform.getAffix2().getWordSet()) {
      // Skip anything not unmodeled
      if (w.getSet() != WordSet.UNMODELED) {
        continue;
      }

      // Hypothesize the base
      String baseText = Transform.inferBase(w, transform);

      // If the base does not exist, try to infer it
      if (lex.getWord(baseText) == null) {
        // If it was already inferred, add it to the lexicon
        if (inferredBases.contains(baseText)) {
          // Create a new word using the token count of the word
          // that ended up promoting it
          Word newWord = new Word(baseText, w.getCount(), false, true);
          newWords.add(newWord);
        } else {
          // Otherwise, infer it
          inferredBases.add(baseText);
        }
      }
    }

    return newWords;
  }

  /**
   * Infer the bases from the latest transform and then process them.
   *
   * @param lex the lexicon
   * @param learnedTransforms the learned transforms
   * @param hypTransforms the hypothesized transforms
   * @param reEval as used by scoreWord
   * @param doubling as used by scoreWord
   * @param deriveInferredForms as used by scoreWord
   * @param optimization as used by moveTransformPairs
   * @param out the destination for any printing to the log
   */
  @SuppressWarnings("ReferenceEquality")
  public void conservInference(
      Lexicon lex,
      List<Transform> learnedTransforms,
      List<Transform> hypTransforms,
      boolean reEval,
      boolean doubling,
      boolean deriveInferredForms,
      boolean optimization,
      PrintWriter out) {
    int newBaseCount = 0;
    int newPairCount = 0;

    // Get the latest transform from the learned list
    Transform newestTransform = learnedTransforms.get(learnedTransforms.size() - 1);
    Collection<Word> newWords = inferBases(lex, newestTransform);
    for (Word newBase : newWords) {
      // Add each new base to the lexicon and let the lexicon move it
      lex.addWord(newBase);
      lex.moveWord(newBase, WordSet.BASE);
      newBaseCount++;

      // Score each new base for every learned transform that can apply
      // to it, counting new pairs
      for (Transform trans : learnedTransforms) {
        if (newBase.hasAffix(trans.getAffix1())) {
          // Scoring the word has the side effect of adding it if appropriate
          final boolean added = Transform.scoreWord(trans, newBase, lex, reEval, doubling, deriveInferredForms);
          if (added) {
            newPairCount++;
            // Move right away so that we can't accidentally derive the word twice later
            lex.moveTransformPairs(trans, hypTransforms, optimization, reEval, doubling, deriveInferredForms);
          }
        }
      }
    }

    // If we added words, update the frequencies
    if (newBaseCount > 0) {
      lex.updateFrequencies();
    }

    // If optimization is on, score for all hypothesized transforms other
    // than the one just learned
    if (optimization) {
      for (Word newBase : newWords) {
        for (Transform trans : hypTransforms) {
          // Reference equality is correct here
          if (trans != newestTransform && newBase.hasAffix(trans.getAffix1())) {
            Transform.scoreWord(trans, newBase, lex, reEval, doubling, deriveInferredForms);
          }
        }
      }
    }

    // Put the new words out to the log if we're outputting
    if (out != null) {
      out.println("# Learned transform " + newestTransform.toString());
      for (Word newBase : newWords) {
        out.println(newBase.toDerivedWordsString());
      }
      out.println("");
    }

    // Output the results
    System.out.println(newPairCount + " new pairs inferred by conservative inference.");
  }
}
