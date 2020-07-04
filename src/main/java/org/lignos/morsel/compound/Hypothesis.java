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
package org.lignos.morsel.compound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lignos.morsel.TransformInference;
import org.lignos.morsel.lexicon.Lexicon;
import org.lignos.morsel.lexicon.Word;

/** Represents a hypothesis in beam search for compounding */
public class Hypothesis {
  /** Words used in the compound, in order */
  final List<Word> words;
  /** Text remaining after those words */
  String remainingText;

  /**
   * Create a empty hypothesis with the specified remaining text
   *
   * @param remainingText the remaining text to be analyzed
   */
  public Hypothesis(String remainingText) {
    this.remainingText = remainingText;
    words = new ArrayList<>();
  }

  /**
   * Create a partial hypothesis with the specified remaining text and hypothesized words
   *
   * @param remainingText the remaining text to be analyzed
   * @param hypWords the hypothesized words that precede the remainingText
   */
  public Hypothesis(String remainingText, List<Word> hypWords) {
    this.remainingText = remainingText;
    words = new ArrayList<>(hypWords);
  }

  /**
   * Pick the best hypothesis for a word. The best hypothesis is the one with the highest score that
   * also has a higher score than the original word.
   *
   * @param completeHyps the complete hypotheses
   * @param word the word to split
   * @return the best hypothesis for splitting the word
   */
  public static Hypothesis pickHypothesis(List<Hypothesis> completeHyps, Word word) {
    // Pick the best compounding hypothesis or the hypothesis that this
    // is not a compound at all
    Hypothesis bestHyp = null;
    double bestScore = 0;
    for (Hypothesis hyp : completeHyps) {
      double score = hyp.getScore();
      if (score > bestScore) {
        bestHyp = hyp;
        bestScore = score;
      }
    }

    // Return the best hyp if it as good or better than the original word
    double wordScore = scoreWord(word);
    return bestScore >= wordScore ? bestHyp : null;
  }

  private static long scoreWord(Word word) {
    return word.getCount();
  }

  /**
   * Add a new word to the CompoundingHypothesis and reduce the text by it
   *
   * @param newWord the new word
   */
  private void add(Word newWord) {
    words.add(newWord);
    remainingText = remainingText.substring(newWord.length());
  }

  /**
   * Return a new CompoundingHypothesis that extends the current one by a word
   *
   * @param newWord the new word
   * @return the new hypothesis including the new word
   */
  private Hypothesis extend(Word newWord) {
    Hypothesis newHyp = new Hypothesis(remainingText, words);
    newHyp.add(newWord);
    return newHyp;
  }

  /**
   * Return the score of the hypothesis. The score of is the geometric mean of the scores of its
   * words
   *
   * @return the score
   */
  public double getScore() {
    // Multiply out the scores of each word
    double prod = 1;
    for (Word word : words) {
      prod *= scoreWord(word);
    }

    // Now take it to the 1/n power
    return Math.pow(prod, 1.0 / (double) words.size());
  }

  /**
   * Return whether the hypothesis is a complete analysis of the text
   *
   * @return true if the remaining text is empty
   */
  public boolean isComplete() {
    return remainingText.isEmpty();
  }

  /**
   * Extend the hypothesis by all possible descendant hypotheses
   *
   * @param lex the lexcion
   * @param filler the fillers that can be used between words
   * @param transInf the rules for combining transforms
   * @param doubling as used by scoreWord
   * @return a List of the new hypotheses
   */
  public List<Hypothesis> extendAll(
      Lexicon lex, Compounding.Filler filler, TransformInference transInf, boolean doubling) {
    // Extend the hypothesis by one word, if possible
    List<Hypothesis> extended = new ArrayList<>();

    // Get all prefix words of the remaining text
    // If words is not empty (i.e. this is after the first iteration
    // on this word), we can allow a prefix to be the whole
    // remaining text.
    List<Word> prefixWords =
        Compounding.getPrefixes(remainingText, lex, !words.isEmpty(), filler, transInf, doubling);

    // Add a new hypothesis for every prefix found
    for (Word prefixWord : prefixWords) {
      extended.add(extend(prefixWord));
    }

    return extended;
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    for (Word word : words) {
      out.append(word.getText()).append('|');
    }

    out.append(remainingText);

    return out.toString();
  }

  /**
   * Allow comparison of two hypotheses by their scores. To get a descending list, the comparator
   * inverts the usual ordering.
   */
  public static class HypothesisScoreRanker implements Comparator<Hypothesis> {

    @Override
    public int compare(Hypothesis h1, Hypothesis h2) {
      // Intentionally 2 - 1 to get a descending sort
      return Double.compare(h2.getScore(), h1.getScore());
    }
  }
}
