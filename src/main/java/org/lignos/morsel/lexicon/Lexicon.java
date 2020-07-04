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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lignos.morsel.Util;
import org.lignos.morsel.transform.Affix;
import org.lignos.morsel.transform.Affix.WeightedUnmodTypeCountComparator;
import org.lignos.morsel.transform.AffixType;
import org.lignos.morsel.transform.Transform;
import org.lignos.morsel.transform.TransformPair;
import org.lignos.morsel.transform.WordPair;

/** The representation of all words in the language being learned. */
public class Lexicon {
  private final Map<String, Word> lex;
  private final Map<String, Affix> prefixes;
  private final Map<String, Affix> suffixes;
  private final Set<Word> base;
  private final Set<Word> derived;
  private final Set<Word> unmod;
  private long tokenCount;
  private boolean validSetCounts;

  /** Create a new empty lexicon. */
  public Lexicon() {
    lex = new Object2ObjectOpenHashMap<>();

    prefixes = new Object2ObjectOpenHashMap<>();
    suffixes = new Object2ObjectOpenHashMap<>();

    base = new ObjectOpenHashSet<>();
    derived = new ObjectOpenHashSet<>();
    unmod = new ObjectOpenHashSet<>();
    validSetCounts = false;
  }

  /** @return a string representation of the size of the lexicon */
  public String getStatus() {
    return "Types: " + lex.size() + " Tokens: " + tokenCount;
  }

  /** @return the total token count of all items in the lexicon */
  public long getTokenCount() {
    return tokenCount;
  }

  /**
   * Add a word to the lexicon.
   *
   * @param word the word to add
   * @return true if the word was successfully added, false if it was already present
   */
  public boolean addWord(Word word) {
    // Add the word to the lexicon. If a word was already there, return false
    if (lex.put(word.getKey(), word) != null) {
      return false;
    }
    // Add its affixes
    addAffixes(word, AffixType.PREFIX);
    addAffixes(word, AffixType.SUFFIX);

    // Put it in unmodeled. Note the the word constructor has already set its set.
    unmod.add(word);

    // Count its token frequency
    tokenCount += word.getCount();

    return true;
  }

  /** Update all data structures that depend on the frequency of words in the lexicon */
  public void updateFrequencies() {
    // Set the frequencies on all words
    for (Word w : lex.values()) {
      w.setFrequency(tokenCount);
    }

    // Have the affixes update their counts on frequent words
    for (Affix a : prefixes.values()) {
      a.countFreqWords();
    }

    // Count suffixes
    for (Affix a : suffixes.values()) {
      a.countFreqWords();
    }
  }

  /**
   * Return the requested set of words
   *
   * @param set the set to return
   * @return the specified set of words
   */
  public Set<Word> getSetWords(WordSet set) {
    // Return the right set
    switch (set) {
      case BASE:
        return base;
      case DERIVED:
        return derived;
      case UNMODELED:
        return unmod;
      default:
        throw new RuntimeException("Unhandled WordSet.");
    }
  }

  /** @return all words in the lexicon */
  public Collection<Word> getWords() {
    return lex.values();
  }

  /** @return the sorted strings for all words in the lexicon */
  public List<String> getWordStrings() {
    final List<String> sortedWords = new ArrayList<>(lex.keySet());
    Collections.sort(sortedWords);
    return sortedWords;
  }

  /**
   * Add all affixes of a given word to the word and affix counts
   *
   * @param word the word
   * @param type the type of affixes to count
   */
  private void addAffixes(Word word, AffixType type) {
    // Get the prefixes and count them
    String[] wordAffixes = Affix.getAffixes(word, type);

    // Pick the right map to put entries in based on AffixType
    Map<String, Affix> affixes;
    if (type == AffixType.PREFIX) {
      affixes = prefixes;
    } else if (type == AffixType.SUFFIX) {
      affixes = suffixes;
    } else {
      throw new RuntimeException("Unhandled affix type.");
    }

    // Count the affixes
    for (String affixText : wordAffixes) {
      // Try to look up the affix
      Affix affix = affixes.get(affixText);
      // If the affix has not been seen, create it
      if (affix == null) {
        affix = new Affix(affixText, type);
        affixes.put(affixText, affix);
      }
      // Increase the counts for this affix
      affix.addWord(word);

      // Add the affix to the word
      word.addAffix(affix);
    }
  }

  /**
   * Returns true if the specified word is in the lexicon.
   *
   * @param word the word
   * @return true if the word is in the lexicon
   */
  public boolean contains(Word word) {
    return lex.containsKey(word.getKey());
  }

  /**
   * Return true if there is a word matching the specified text in the specified set
   *
   * @param wordText the text of the word
   * @param set the set to check
   * @return true if the word is in the specified set
   */
  public boolean isWordInSet(String wordText, WordSet set) {
    Word word = lex.get(wordText);
    // If the word was found, return whether the set was correct,
    // otherwise false
    return word != null && word.getSet() == set;
  }

  /**
   * Return true if a word is in the specified set
   *
   * @param word the word
   * @param set the set
   * @return true if the word is in the specified set
   */
  public boolean isWordInSet(Word word, WordSet set) {
    return word.getSet() == set;
  }

  /** Update counts for all affix types of which affixes are common in each word set. */
  private void countAffixWordSets() {
    // Count prefixes
    for (Affix a : prefixes.values()) {
      a.countWordSets();
    }

    // Count suffixes
    for (Affix a : suffixes.values()) {
      a.countWordSets();
    }

    validSetCounts = true;
  }

  /**
   * Return the affix map for the specified affix type
   *
   * @param type an affix type
   * @return the affix map for the specific type
   */
  private Map<String, Affix> getAffixMap(AffixType type) {
    switch (type) {
      case PREFIX:
        return prefixes;
      case SUFFIX:
        return suffixes;
      default:
        throw new RuntimeException("Unhandled AffixType.");
    }
  }

  /**
   * Return the n most frequent affixes in the specified set
   *
   * @param n the number of affixes
   * @param type the type of affixes
   * @param set the set of affixes to compute counts over
   * @param weighted whether to weight the count of frequencies
   * @return a List of the n most frequent affixes in the specified set
   */
  public List<Affix> topAffixes(int n, AffixType type, AffixSet set, boolean weighted) {
    // Make sure the counts are up to date before doing anything. Counting
    // will automatically reset the flag
    if (!validSetCounts) {
      countAffixWordSets();
    }

    // Get the list of affixes, sort them
    List<Affix> orderedAffixes = new ArrayList<>(getAffixMap(type).values());
    switch (set) {
      case ALL:
        if (weighted) orderedAffixes.sort(Affix.Comparators.byWeightedAllTypeCount.reversed());
        else orderedAffixes.sort(Affix.Comparators.byAllTypeCount.reversed());
        break;
      case BASEUNMOD:
        if (weighted)
          orderedAffixes.sort(Affix.Comparators.byWeightedBaseUnmodTypeCount.reversed());
        else orderedAffixes.sort(Affix.Comparators.byBaseUnmodTypeCount.reversed());
        break;
      case UNMOD:
        if (weighted) orderedAffixes.sort(Affix.Comparators.byWeightedUnmodTypeCount.reversed());
        else orderedAffixes.sort(Affix.Comparators.byUnmodTypeCount.reversed());
    }

    // Truncate the list
    return Util.truncateCollection(orderedAffixes, n);
  }

  /**
   * Return the top n unmodeled affixes
   *
   * @param n the number of affixes
   * @param type the type of affixes
   * @return the top n unmodeled affixes
   */
  public List<Affix> topUnmodAffixes(int n, AffixType type) {
    // Make sure the counts are up to date before doing anything. Counting
    // will automatically reset the flag
    if (!validSetCounts) {
      countAffixWordSets();
    }

    // Get the list of affixes, sort them, truncate them
    List<Affix> orderedAffixes = new ArrayList<>(getAffixMap(type).values());
    orderedAffixes.sort(Collections.reverseOrder(new WeightedUnmodTypeCountComparator()));
    return Util.truncateCollection(orderedAffixes, n);
  }

  /**
   * @param word the word
   * @return the specified word from the lexicon
   */
  public Word getWord(String word) {
    return lex.get(word);
  }

  /**
   * Move a word between word sets
   *
   * @param word the wod
   * @param set the destination set
   */
  public void moveWord(Word word, WordSet set) {
    // Take it out of its old set
    switch (word.getSet()) {
      case BASE:
        base.remove(word);
        break;
      case DERIVED:
        derived.remove(word);
        break;
      case UNMODELED:
        unmod.remove(word);
        break;
      default:
        throw new RuntimeException("Unhandled WordSet.");
    }

    // Set the word's set
    word.setSet(set);

    // Put it in the new set
    switch (set) {
      case BASE:
        base.add(word);
        break;
      case DERIVED:
        derived.add(word);
        break;
      case UNMODELED:
        unmod.add(word);
        break;
      default:
        throw new RuntimeException("Unhandled WordSet.");
    }
  }

  /**
   * Move pairs of words between sets based on the transform learned
   *
   * @param learnedTransform the learned transforms
   * @param hypTransforms other transforms being tracked
   * @param opt whether to optimize performance by maintaining other transforms
   * @param reEval as used by scoreWord
   * @param doubling as used by scoreWord
   * @param deriveInferredForms as used by scoreWord
   */
  public void moveTransformPairs(
      Transform learnedTransform,
      List<Transform> hypTransforms,
      boolean opt,
      boolean reEval,
      boolean doubling,
      boolean deriveInferredForms) {
    // Select the list of moved words based on whether the transform
    // has already been learned or not
    Set<WordPair> pairs =
        learnedTransform.isLearned()
            ? learnedTransform.getUnmovedDerivationPairs()
            : learnedTransform.getWordPairs();

    // Call moveWordPairs to do all the work
    moveWordPairs(learnedTransform, hypTransforms, opt, reEval, doubling, deriveInferredForms, pairs);
  }

  /**
   * Move pairs of words between sets based on the transform learned
   *
   * @param derivingTransform the transform that derived the words
   * @param hypTransforms other transforms being tracked
   * @param opt whether to optimize performance by maintaining other transforms
   * @param reEval as used by scoreWord
   * @param doubling as used by scoreWord
   * @param deriveInferredForms as used by scoreWord
   * @param pairs the pairs of words to operate on
   */
  public void moveWordPairs(
      Transform derivingTransform,
      List<Transform> hypTransforms,
      boolean opt,
      boolean reEval,
      boolean doubling,
      boolean deriveInferredForms,
      Set<WordPair> pairs) {
    // Keep track of words that moved based on the sets
    List<Word> unmodBaseWords = new ArrayList<>();
    List<Word> baseDerivedWords = new ArrayList<>();
    List<Word> unmodDerivedWords = new ArrayList<>();

    // Handle duplicates
    Set<WordPair> prunedPairs;
    // Keep track of all of the derivations
    Map<Word, WordPair> derivedPairs = new Object2ObjectOpenHashMap<>();
    prunedPairs = new ObjectOpenHashSet<>();

    for (WordPair pair : pairs) {
      // Check whether this derived form has been derived already
      Word derived = pair.getDerived();
      if (derivedPairs.containsKey(derived)) {
        // If it has, pick which derivation to keep
        WordPair oldPair = derivedPairs.get(derived);

        // If the old pair is accommodated, we may need to replace it
        if (oldPair.isAccommodated()) {
          // If this pair is not accommodated, definitely replace
          if (!pair.isAccommodated()) {
            // Remove it from the pairs
            prunedPairs.remove(oldPair);
          } else {
            // Otherwise, we prefer the doubled pair to
            // undoubled. The easy way to tell the two
            // apart is the the doubled base is shorter
            if (pair.getBase().length() < oldPair.getBase().length()) {
              // Remove if this pair has a shorter base
              prunedPairs.remove(oldPair);
            } else {
              // Otherwise, this pair should not be added
              continue;
            }
          }
        } else {
          // Otherwise, this pair should not be added since we
          // already have a better derivation
          // Skip over the adding
          continue;
        }
      }
      // Add this pair to the map and pairs set
      prunedPairs.add(pair);
      derivedPairs.put(derived, pair);
    }

    // Move each pair
    for (WordPair pair : prunedPairs) {
      // Unpack the pair
      Word base = pair.getBase();
      Word derived = pair.getDerived();

      // Create the base/derived relationship
      base.addDerived(derived);
      derived.setBase(base);
      derived.setTransform(derivingTransform);

      // If the base has no root, set it to itself, which also sets the
      // derived word's root
      if (base.getRoot() == null) {
        base.setRoot(base);
      } else {
        // Otherwise just set the derived word's root to that
        // of the base
        derived.setRoot(base.getRoot());
      }

      // If the base is in unmod, move it to base
      // Note that if it is already in derived, it should stay there
      if (base.getSet() == WordSet.UNMODELED) {
        moveWord(base, WordSet.BASE);
        unmodBaseWords.add(base);
      }

      // If the derived word is not in derived, move it there
      // It may have moved from base (demotion upon re-analysis) or
      // from unmodeled (first analysis)
      if (derived.getSet() == WordSet.BASE) {
        baseDerivedWords.add(derived);
        moveWord(derived, WordSet.DERIVED);
      } else if (derived.getSet() == WordSet.UNMODELED) {
        unmodDerivedWords.add(derived);
        moveWord(derived, WordSet.DERIVED);
      }
    }

    // If the transform has already been learned, reset the word pairs
    if (derivingTransform.isLearned()) {
      derivingTransform.resetUnmoved();
    }

    // Invalidate the set counts since we moved words
    validSetCounts = false;

    // If we're not doing optimization, we're done
    if (!opt) {
      return;
    }

    // Now, do the accounting for the moved words by going over each list
    // of moved words

    // Remove all the moved words from their transforms
    removeWordsTransforms(unmodBaseWords);
    removeWordsTransforms(baseDerivedWords);
    removeWordsTransforms(unmodDerivedWords);

    // Now, rescore the new bases for each transform
    scoreWordsTransforms(unmodBaseWords, hypTransforms, reEval, doubling, deriveInferredForms);
  }

  /**
   * Score transforms by the words they may apply to
   *
   * @param words the words to check
   * @param transforms the transforms to check
   * @param reEval whether to allow words to change word sets
   * @param doubling as used by scoreWord
   * @param deriveInferredForms as used by scoreWord
   */
  public void scoreWordsTransforms(
      Collection<Word> words, Collection<Transform> transforms, boolean reEval, boolean doubling,
      boolean deriveInferredForms) {
    // Score each word for each transform
    for (Word word : words) {
      for (Transform transform : transforms) {
        // Score only if the transform applies
        if (word.hasAffix(transform.getAffix1())) {
          Transform.scoreWord(transform, word, this, reEval, doubling, deriveInferredForms);
        }
      }
    }
  }

  /**
   * Remove words from transform scoring.
   *
   * @param newDerivedWords words to remove from scoring
   */
  @SuppressWarnings("ReferenceEquality")
  private void removeWordsTransforms(Collection<Word> newDerivedWords) {
    for (Word movedWord : newDerivedWords) {
      // Remove a word entirely from transform scoring
      // Because we are removing, use the full iterator syntax
      Iterator<TransformPair> iter = movedWord.getTransforms().iterator();
      while (iter.hasNext()) {

        TransformPair tPair = iter.next();
        WordPair wordPair = tPair.getPair();
        Transform transform = tPair.getTransform();

        // Always remove the pair from the transform and from the current
        // word, but we still need to find out whether this word was the
        // base or the derived to remove the pair from the other word
        transform.removeWordPair(wordPair);
        iter.remove(); // Removes from the current word in the pair
        // We only care about reference equality here
        if (wordPair.getBase() == movedWord) {
          // If the current word was the base, remove from the derived word
          wordPair.getDerived().removeTransformPair(tPair);
        } else {
          // If the current word was the derived, remove from the base word
          wordPair.getBase().removeTransformPair(tPair);
        }
      }
    }
  }

  /** Add new entries to the lexicon based on which existing forms are hyphenated. */
  @SuppressWarnings("StringSplitter")
  public void processHyphenation() {
    // Loop over all words and process any hyphenated ones
    final List<String> lexWords = getWordStrings();
    for (String text : lexWords) {
      final Word w = lex.get(text);
      // Check for a hyphen before doing the full split
      if (text.indexOf('-') != -1) {
        final String[] componentTexts = text.split("-");

        // Make a list of words that make this up, creating words if needed
        final List<Word> componentWords = new ArrayList<>();
        for (String componentText : componentTexts) {
          // If the text is empty, keep going
          if (componentText.equals("")) {
            continue;
          }

          // If the word hasn't been seen before, add it
          Word componentWord = lex.get(componentText);
          if (componentWord == null) {
            // Use the frequency of the original
            componentWord = new Word(componentText, w.getCount(), false, false);
            addWord(componentWord);
          } else {
            // If the word has been seen before, increment its
            // token count
            componentWord.addCount(w.getCount());
          }

          componentWords.add(componentWord);
        }

        // If we actually ended up with more than one component,
        // make the original word hyphenated
        if (componentWords.size() > 1) {
          w.makeCompoundWord(componentWords);
        }
      }
    }

    // When we're done, since we added words we have to recalculate frequencies
    updateFrequencies();
  }

  /** The word sets that affix counts are computed over */
  public enum AffixSet {
    /** All words */
    ALL,
    /** Unmodeled words */
    UNMOD,
    /** Unmodeled and base words */
    BASEUNMOD
  }
}
