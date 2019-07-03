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
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.lignos.morsel.compound.Compounding;
import org.lignos.morsel.lexicon.Lexicon;
import org.lignos.morsel.lexicon.Word;
import org.lignos.morsel.lexicon.WordSet;
import org.lignos.morsel.transform.Affix;
import org.lignos.morsel.transform.AffixType;
import org.lignos.morsel.transform.Transform;
import org.lignos.morsel.transform.WordPair;

/**
 * Main class for the MORSEL unsupervised morphology learning system. Provides main method and core
 * learning loop.
 */
public class MorphLearner {
  /** Path of the wordlist to analyze */
  private final Path corpusPath;
  /** Character encoding to use for reading and writing */
  private final Charset charset;
  // Output options
  private final boolean outputBaseInf;
  private final boolean outputConflation;
  private final boolean outputCompounds;
  /** Base path string for analysis */
  private final String analysisBase;
  /** Path for analysis output */
  private final Path outputPath;
  /** The lexicon being learned over */
  private Lexicon lex;
  /* The following parameters are documented in the README. See the
   * implementation of setParams for the mapping between the names
   * of these members and the official parameter names.
   */
  private int MAX_ITER;
  private int TOP_AFFIXES;
  private boolean REEVAL_DERIVATION;
  private boolean SCORE_REEVAL;
  private boolean USE_DOUBLING;
  private int TYPE_THRESHOLD;
  private int STEM_LENGTH;
  private double OVERLAP_THRESHOLD;
  private double PRECISION_THRESHOLD;
  private int WINDOW_SIZE;
  private boolean HYPHENATION;
  private boolean FINAL_COMPOUNDING;
  private boolean ITER_COMPOUNDING;
  private boolean AGGR_COMPOUNDING;
  private boolean BASE_INFERENCE;
  private boolean TRANSFORM_OPTIMIZATION;
  private boolean TRANSFORM_DEBUG;
  private boolean ITERATION_ANALYSIS;
  private boolean WEIGHTED_TRANSFORMS;
  private boolean WEIGHTED_AFFIXES;
  private boolean TRANSFORM_RELATIONS;
  private boolean ANALYZE_SIMPLEX_WORDS;

  /**
   * Create a new learner using the given paths for I/O.
   *
   * @param wordlistPath the path of the input wordlist
   * @param outputPath the path for the analysis output
   * @param paramPath the path of the parameter file
   * @param charset the encoding of the input corpus file
   * @param outputBaseInf Whether to output the examples of base inference
   * @param outputConflation Whether to output conflation sets
   * @param outputCompounds Whether to output an analysis before compounding
   * @throws FileNotFoundException if the corpus or parameter files do not exist.
   * @throws IOException if there are other IO errors.
   */
  public MorphLearner(
      Path wordlistPath,
      Path outputPath,
      Path paramPath,
      Charset charset,
      boolean outputBaseInf,
      boolean outputConflation,
      boolean outputCompounds)
      throws IOException {
    this.corpusPath = wordlistPath;
    this.outputPath = outputPath;
    this.charset = charset;
    this.outputBaseInf = outputBaseInf;
    this.outputConflation = outputConflation;
    this.outputCompounds = outputCompounds;

    System.out.println("Setting parameters from " + paramPath);
    this.setParams(paramPath);

    System.out.println("Loading wordlist from " + wordlistPath + " using charset " + charset);
    loadCorpus(charset);

    // Shorten the analysis path down to a base to concatenate to
    final String outputPathString = outputPath.toString();
    analysisBase =
        outputPathString.contains(".")
            ? outputPathString.substring(0, outputPathString.lastIndexOf('.'))
            : outputPathString;
  }

  private static boolean isTie(Transform trans1, Transform trans2) {
    return (trans1.getAffix1() == trans2.getAffix2()
        && trans1.getAffix2() == trans2.getAffix1()
        && trans1.getTypeCount() == trans2.getTypeCount());
  }

  private static Transform breakTie(Transform trans1, Transform trans2) {
    int baseScore = 0;
    int derivedScore = 0;

    // Give a point to whichever member of the pair is more frequent
    for (WordPair pair : trans1.getWordPairs()) {
      if (pair.getBase().getCount() > pair.getDerived().getCount()) {
        baseScore++;
      } else if (pair.getDerived().getCount() > pair.getBase().getCount()) {
        derivedScore++;
      }
      // Award no points in a tie
    }

    // Choose the transform with more frequent bases
    // Ties go to the first one
    if (baseScore >= derivedScore) {
      return trans1;
    } else {
      return trans2;
    }
  }

  /**
   * Call the learner on the specified arguments. Exit using standard error codes (sysexits.h) if an
   * error is encountered.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    // create the command line parser
    CommandLineParser parser = new DefaultParser();
    // Set up command line arguments
    Options options = new Options();
    options.addOption(new Option("h", "help", false, "dislay this help and exit"));
    Option encodingOption =
        new Option("e", "encoding", true, "input and output file encoding. Defaults to ISO8859_1.");
    encodingOption.setArgName("encoding");
    options.addOption(encodingOption);
    options.addOption(
        new Option(
            "b",
            "base-inf",
            false,
            "output the examples of base inference. This does not change whether base inference"
                + " is used; it simply outputs the examples that used it."));
    options.addOption(
        new Option("s", "conflation-sets", false, "output the learner's conflation sets"));
    options.addOption(
        new Option(
            "c",
            "compounds",
            false,
            "output the learner's analsyis before final compounding is used"));
    HelpFormatter formatter = new HelpFormatter();
    String usage = "MorphLearner [options] wordlist paramfile output";

    final CommandLine line;
    final String[] otherArgs;
    try {
      // Parse the command line arguments
      line = parser.parse(options, args);

      // Handle help
      if (line.hasOption("help")) {
        formatter.printHelp(usage, options);
        System.exit(0);
      }

      otherArgs = line.getArgs();
      if (otherArgs.length != 3) {
        throw new ParseException("Incorrect number of required arguments specified");
      }
    } catch (ParseException exp) {
      // We use stdout instead of stderr since help is written to stdout
      System.out.println(exp.getMessage());
      formatter.printHelp(usage, options);
      // Exit code 64: command line usage error
      System.exit(64);
      // Explicit return to appease compiler
      return;
    }

    // Get arguments
    final Path wordlistPath = Paths.get(otherArgs[0]).toAbsolutePath();
    final Path paramPath = Paths.get(otherArgs[1]).toAbsolutePath();
    final Path outputPath = Paths.get(otherArgs[2]).toAbsolutePath();

    // Check file arguments early just to be safe
    boolean badPath = false;
    if (!wordlistPath.toFile().isFile()) {
      System.err.println("Cannot read wordlist file " + wordlistPath);
      badPath = true;
    }
    if (!paramPath.toFile().isFile()) {
      System.err.println("Cannot read parameter file " + paramPath);
      badPath = true;
    }
    // There's no good way to check whether we can write to the output file other than just trying
    // it. We go to the trouble so that we fail fast rather than at the end of learning.
    try (final Writer output = Files.newBufferedWriter(outputPath)) {
      output.write('\n');
    } catch (IOException e) {
      System.err.println("Cannot write output file " + outputPath);
      badPath = true;
    }
    if (badPath) {
      // Exit code 74: input/output error
      System.exit(74);
    }

    // Get options
    final String encodingName = line.getOptionValue("encoding", "ISO8859_1");
    final Charset encoding;
    try {
      encoding = Charset.forName(encodingName);
    } catch (UnsupportedCharsetException e) {
      System.err.println("Unsupported file encoding: " + encodingName);
      // Exit code 74: input/output error
      System.exit(74);
      // Explicit return to appease compiler
      return;
    }
    final boolean outputBaseInf = line.hasOption("base-inf");
    final boolean outputConflation = line.hasOption("conflation-sets");
    final boolean outputCompounds = line.hasOption("compounds");

    // Time initialization
    long start = System.currentTimeMillis();

    // Initialize the learner
    final MorphLearner learner;
    try {
      learner =
          new MorphLearner(
              wordlistPath,
              outputPath,
              paramPath,
              encoding,
              outputBaseInf,
              outputConflation,
              outputCompounds);
    } catch (IOException e) {
      System.err.println(e.toString());
      // Exit code 66: cannot open input
      System.exit(66);
      // Explicit return to appease compiler
      return;
    }

    // Output time stats
    float elapsedSeconds = (System.currentTimeMillis() - start) / 1000F;
    System.out.println("Init time: " + elapsedSeconds + "s\n");

    // Time learning
    start = System.currentTimeMillis();

    try {
      learner.learn();
    } catch (IOException e) {
      System.err.println("Error during learning:");
      System.err.println(e.toString());
      // Exit code 74: input/output error
      System.exit(66);
    }

    // Output time stats
    elapsedSeconds = (System.currentTimeMillis() - start) / 1000F;
    System.out.println("\nLearning time: " + elapsedSeconds + "s");
  }

  /**
   * Loads the corpus located at corpusPath.
   *
   * @param encoding the encoding of the corpus
   * @throws IOException if the file at corpusPath could not be read.
   */
  private void loadCorpus(Charset encoding) throws IOException {
    System.out.println("Loading words...");
    lex = CorpusLoader.loadWordlist(corpusPath, encoding, true);
  }

  /**
   * Learn from the loaded lexicon and output the results.
   *
   * @throws IOException if output could not be written.
   */
  private void learn() throws IOException {
    List<Transform> learnedTransforms = new ArrayList<>();
    Set<Transform> badTransforms = new ObjectOpenHashSet<>();
    Map<String, Transform> indexedTransforms = new Object2ObjectOpenHashMap<>();
    RuleInference ruleInf = new RuleInference();
    TransformInference transInf = new TransformInference();
    PrintWriter baseLog = null;

    // Print lexicon status
    System.out.println("Lexicon stats:");
    System.out.println(lex.getStatus());
    System.out.println();

    // Pre-processing
    if (HYPHENATION) {
      System.out.println("Handling hyphenation...");
      // Perform hyphenation
      lex.processHyphenation();

      // Print lexicon status
      System.out.println("Lexicon stats:");
      System.out.println(lex.getStatus());
    }

    System.out.println(memoryStatus());
    System.out.println();

    // The main learning loop
    System.out.println("Starting learning...");
    for (int i = 0; i < MAX_ITER; i++) {
      System.out.println("Iteration " + (i + 1));
      // Print lexicon status
      System.out.println("Lexicon stats:");
      System.out.println(lex.getStatus());
      // Print set status
      System.out.println("Base size: " + lex.getSetWords(WordSet.BASE).size());
      System.out.println("Derived size: " + lex.getSetWords(WordSet.DERIVED).size());
      System.out.println("Unmodeled size: " + lex.getSetWords(WordSet.UNMODELED).size());

      // Hypothesize transforms
      System.out.println("Hypothesizing and scoring transforms...");
      ArrayList<Transform> hypTransforms = hypothesizeTransforms(badTransforms);

      // Score the transforms
      // Always pass false for doubling to ensure that rules cannot get high scores solely by using
      // orthographic accommodation.
      // Reeval is set by the SCORE_REEVAL parameter
      final boolean doubling = false;
      final boolean reeval = SCORE_REEVAL;
      if (TRANSFORM_OPTIMIZATION) {
        // If this is the first iteration, score from scratch
        if (i == 0) {
          for (Transform trans : hypTransforms) {
            Transform.scoreTransform(trans, lex, reeval, doubling);
          }
        }
        // Otherwise, incrementally score, using what we had from last round
        else {
          incrementalScoreTransforms(hypTransforms, indexedTransforms, lex, reeval, doubling);
        }
        // Update indexedTransforms for the next round
        indexedTransforms = indexTransforms(hypTransforms);
      } else {
        for (Transform trans : hypTransforms) {
          Transform.scoreTransform(trans, lex, reeval, doubling);
        }
      }

      // Rank the transforms
      sortTransforms(hypTransforms);

      // Print all transforms if needed
      if (TRANSFORM_DEBUG) {
        for (Transform transform : hypTransforms) {
          System.out.println(transform.toDebugString());
        }
      }

      // Trim them down, score them, and pick one
      final List<Transform> topTransforms = Util.truncateCollection(hypTransforms, TOP_AFFIXES);
      Transform bestTransform = selectTransform(topTransforms, learnedTransforms, badTransforms);

      // Quit if no good transform was found
      if (bestTransform == null) {
        System.out.println("Out of good transforms to learn. Learning complete.\n\n");
        break;
      }

      System.out.println("Selected " + bestTransform.toString());

      // Re-evaluate the best transform
      if (REEVAL_DERIVATION) {
        final Transform reEvalTransform =
            new Transform(bestTransform.getAffix1(), bestTransform.getAffix2());
        Transform.scoreTransform(reEvalTransform, lex, REEVAL_DERIVATION, USE_DOUBLING);
        bestTransform = reEvalTransform;
      }

      // Accept the best transform
      System.out.println("Learned " + bestTransform.toVerboseString());
      // Reeval should be false for moving the words
      lex.moveTransformPairs(bestTransform, hypTransforms, TRANSFORM_OPTIMIZATION, false, doubling);

      // Mark learned after words have been moved
      bestTransform.markLearned();
      learnedTransforms.add(bestTransform);

      // Check for relationships between transforms
      if (TRANSFORM_RELATIONS) {
        transInf.inferRelations(lex);
        System.out.println("Transform relationships:\n" + transInf.toString());
      }

      // Handle the results of inference
      if (BASE_INFERENCE) {
        // Open the output file on the first iteration if we're outputting
        if (outputBaseInf && i == 0) {
          try {
            baseLog =
                new PrintWriter(
                    Files.newBufferedWriter(Paths.get(analysisBase + "_baseinf.txt"), charset));
          } catch (FileNotFoundException e) {
            System.err.println("Couldn't output base inference dump.");
          }
        }

        ruleInf.conservInference(
            lex,
            learnedTransforms,
            hypTransforms,
            REEVAL_DERIVATION,
            USE_DOUBLING,
            TRANSFORM_OPTIMIZATION,
            baseLog);
      }

      //  Do iteration/aggressive compounding
      if (ITER_COMPOUNDING) {
        System.out.println("Handling iteration compounding...");
        int nCompounds =
            Compounding.breakCompounds(
                lex,
                WordSet.BASE,
                learnedTransforms,
                hypTransforms,
                TRANSFORM_OPTIMIZATION,
                REEVAL_DERIVATION,
                USE_DOUBLING,
                transInf);
        System.out.println("Broke " + nCompounds + " compounds in base");
        if (AGGR_COMPOUNDING) {
          nCompounds =
              Compounding.breakCompounds(
                  lex,
                  WordSet.UNMODELED,
                  learnedTransforms,
                  hypTransforms,
                  TRANSFORM_OPTIMIZATION,
                  REEVAL_DERIVATION,
                  USE_DOUBLING,
                  transInf);
          System.out.println("Broke " + nCompounds + " compounds in unmodeled");
        }
      }

      // Add space between transforms
      System.out.println(memoryStatus());
      System.out.println();
      System.out.println();

      // Output the analysis on the first and then every 5th iteration starting with 5
      // Analyses should be one-indexed, so add one
      if (ITERATION_ANALYSIS && (i == 0 || (i + 1) % 5 == 0)) {
        outputIterAnalysis(Integer.toString(i + 1));
      }
    } // End of main learning loop

    // Clean up for base inference logging
    if (baseLog != null) {
      baseLog.close();
    }

    // After learning is complete, perform the last round of compounding
    if (FINAL_COMPOUNDING) {
      // Output an analysis pre-compounding
      if (outputCompounds) {
        outputIterAnalysis("precompound");
      }

      // If ITER_COMPOUNDING was on, pass in the learned rules
      List<Transform> fillerRules = ITER_COMPOUNDING ? learnedTransforms : null;

      System.out.println("Handling final compounding...");
      System.out.println(memoryStatus());
      // Break compounds in base and unmodeled
      // We sneakily pass false for opt so we can just pass
      // null for hypTransforms- since we're done learning transforms,
      // we don't care that hypothesized transforms won't be updated
      int nCompounds =
          Compounding.breakCompounds(
              lex, WordSet.BASE, fillerRules, null, false, REEVAL_DERIVATION, USE_DOUBLING, transInf);
      System.out.println("Broke " + nCompounds + " compounds in base");
      System.out.println(memoryStatus());
      nCompounds =
          Compounding.breakCompounds(
              lex, WordSet.UNMODELED, fillerRules, null, false, REEVAL_DERIVATION, USE_DOUBLING, transInf);
      System.out.println("Broke " + nCompounds + " compounds in unmodeled");
    }

    // Finally, analyze simplexes as the final step
    if (ANALYZE_SIMPLEX_WORDS) {
      System.out.println("Analyzing simplex words...");
      int nAnalyzed =
          Compounding.analyzeSimplexWords(
              lex, WordSet.UNMODELED, learnedTransforms, USE_DOUBLING, transInf);
      System.out.println("Analyzed " + nAnalyzed + " words in unmodeled");
    }

    System.out.println("Learning complete. Analyzing...");
    System.out.println("Writing output to " + outputPath + " using charset " + charset);
    try (final Writer output = Files.newBufferedWriter(outputPath, charset)) {
      outputAnalysis(output);
    }
    if (outputConflation) {
      outputConflationSets();
    }
  }

  private String memoryStatus() {
    // Check the memory
    Runtime runtime = Runtime.getRuntime();
    long usage = runtime.totalMemory() - runtime.freeMemory();
    long remaining = runtime.maxMemory() - usage;

    // Conver to megabytes
    usage /= 1048576L;
    remaining /= 1048576L;

    return "Memory status: " + usage + "MB Used, " + remaining + "MB Remaining";
  }

  private Map<String, Transform> indexTransforms(List<Transform> hypTransforms) {
    Map<String, Transform> index = new Object2ObjectOpenHashMap<>();
    for (Transform transform : hypTransforms) {
      index.put(transform.toKey(), transform);
    }

    return index;
  }

  private void incrementalScoreTransforms(
      ArrayList<Transform> hypTransforms,
      Map<String, Transform> indexedTransforms,
      Lexicon lex,
      boolean reEval,
      boolean doubling) {
    // If the transform is in the index, reuse it
    for (int i = 0; i < hypTransforms.size(); i++) {
      Transform hypTransform = hypTransforms.get(i);
      Transform scoredTransform = indexedTransforms.get(hypTransform.toKey());

      // Reuse the old one if possible, otherwise score from scratch
      if (scoredTransform != null) {
        hypTransforms.set(i, scoredTransform);
      } else {
        Transform.scoreTransform(hypTransform, lex, reEval, doubling);
      }
    }
  }

  private Transform selectTransform(
      List<Transform> topTransforms,
      List<Transform> learnedTransforms,
      Set<Transform> badTransforms) {
    // Search through the transforms for the best one
    Transform bestTransform = null;
    Transform secondTransform;

    System.out.println("Selecting a transform...");
    int nBadTransforms = 0;
    boolean isGood = false;
    for (int i = 0; i < topTransforms.size(); i++) {
      // If we've had too many bad transforms, give up
      // Note that only some bad transforms count against the bad transform
      // count
      if (nBadTransforms >= WINDOW_SIZE) {
        return null;
      }

      System.out.println();
      bestTransform = topTransforms.get(i);
      System.out.println("Vetting transform " + bestTransform.toVerboseString());

      // Keep going if the best doesn't have enough transforms
      if (bestTransform.getTypeCount() < TYPE_THRESHOLD) {
        System.out.println("Transform has too few types.");
        nBadTransforms++;
        // Add this to the set of bad transforms
        badTransforms.add(bestTransform);
        continue;
      }

      // Check for a conflict with existing transforms
      if (isConflict(bestTransform, learnedTransforms)) {
        // Add this to the set of bad transforms
        badTransforms.add(bestTransform);
        continue;
      }

      // Check the base-stem overlap
      int baseOverlap = baseOverlap(bestTransform);
      int stemOverlap = stemOverlap(bestTransform);
      double overlapRatio;

      // If both ratios are zero count this as OK
      if (baseOverlap + stemOverlap == 0) {
        System.out.println("Overlap Ratio: 0/0");
        overlapRatio = 0;
      }
      // If only the base is 0, make sure this is rejected
      else if (baseOverlap == 0) {
        System.out.println("Overlap ratio: " + baseOverlap + "/0");
        overlapRatio = OVERLAP_THRESHOLD + 1.0;
      } else {
        overlapRatio = (double) stemOverlap / (double) baseOverlap;
        System.out.println("Overlap ratio: " + overlapRatio);
      }

      // Go to the next transform if the overlap ratio is too high
      if (overlapRatio > OVERLAP_THRESHOLD) {
        System.out.println("Overlap ratio too high.");
        continue;
      }

      // If there's still a second-best left, check it for tie-breaks
      if (i + 1 < topTransforms.size()) {
        secondTransform = topTransforms.get(i + 1);

        // Check for a tie and resolve it
        if (isTie(bestTransform, secondTransform)) {
          System.out.println("Breaking tie between " + bestTransform + " and " + secondTransform);
          bestTransform = breakTie(bestTransform, secondTransform);
        }
      }

      // Check that the segmentation precision is acceptable
      double segPrecision = Transform.calcSegPrecision(bestTransform);
      System.out.println("Seg. Precision: " + segPrecision);
      if (segPrecision < PRECISION_THRESHOLD) {
        System.out.println("Seg. Precision too low");
        nBadTransforms++;

        // Add this to the set of bad transforms
        badTransforms.add(bestTransform);

        // Move on
        continue;
      }

      // If we didn't continue, that means the transform is good, break out
      isGood = true;
      break;
    }

    // Return the best transform if it is good
    return isGood ? bestTransform : null;
  }

  private boolean isConflict(Transform bestTransform, List<Transform> learnedTransforms) {

    // Already learned
    if (learnedTransforms.contains(bestTransform)) {
      System.out.println("Conflict: Transform already learned.");
      return true;
    }

    // Is inverse of it already learned?
    if (learnedTransforms.contains(
        new Transform(bestTransform.getAffix2(), bestTransform.getAffix1()))) {
      System.out.println("Conflict: Inverse of transform already learned.");
      return true;
    }

    return false;
  }

  private int baseOverlap(Transform transform) {
    int overlap = 0;
    for (WordPair pair : transform.getWordPairs()) {
      if (lex.isWordInSet(pair.getBase(), WordSet.BASE)) {
        overlap++;
      }
    }
    return overlap;
  }

  private int stemOverlap(Transform transform) {
    int overlap = 0;

    // Create a set of all stems in the base words
    Set<String> baseStems = new ObjectOpenHashSet<>();
    for (Word word : lex.getSetWords(WordSet.BASE)) {
      if (word.length() > STEM_LENGTH) {
        baseStems.add(word.getText().substring(0, STEM_LENGTH));
      }
    }

    // Count the overlap with the transform
    for (WordPair pair : transform.getWordPairs()) {
      Word base = pair.getBase();

      // Skip the word if it's too short
      if (base.length() < STEM_LENGTH) {
        continue;
      }

      String stem = base.getText().substring(0, STEM_LENGTH);
      if (baseStems.contains(stem)) {
        overlap++;
      }
    }

    return overlap;
  }

  @SuppressWarnings("unused")
  private void printTransforms(List<Transform> transforms) {
    for (Transform transform : transforms) {
      System.out.println(transform.getPairsText());
    }
  }

  private ArrayList<Transform> hypothesizeTransforms(Set<Transform> badTransforms) {
    List<Affix> topBUPrefixes =
        lex.topAffixes(TOP_AFFIXES, AffixType.PREFIX, Lexicon.AffixSet.BASEUNMOD, WEIGHTED_AFFIXES);
    List<Affix> topUPrefixes =
        lex.topAffixes(TOP_AFFIXES, AffixType.PREFIX, Lexicon.AffixSet.UNMOD, WEIGHTED_AFFIXES);
    List<Affix> topBUSuffixes =
        lex.topAffixes(TOP_AFFIXES, AffixType.SUFFIX, Lexicon.AffixSet.BASEUNMOD, WEIGHTED_AFFIXES);
    List<Affix> topUSuffixes =
        lex.topAffixes(TOP_AFFIXES, AffixType.SUFFIX, Lexicon.AffixSet.UNMOD, WEIGHTED_AFFIXES);

    final ArrayList<Transform> hypTransforms =
        makeTransforms(topBUPrefixes, topUPrefixes, topBUSuffixes, topUSuffixes, badTransforms);
    return hypTransforms;
  }

  private void outputAnalysis(final Writer output) throws IOException {
    // Output an analysis of all the words
    List<String> sortedWords = lex.getWordStrings();
    Collections.sort(sortedWords);
    for (String wordKey : sortedWords) {
      final Word w = lex.getWord(wordKey);
      if (w.shouldAnalyze()) {
        output.write(wordKey);
        output.write('\t');
        output.write(w.analyze());
        output.write('\n');
      }
    }
  }

  private void outputIterAnalysis(final String iter) throws IOException {
    // Output the analysis for the current iteration
    try (final Writer out =
        Files.newBufferedWriter(Paths.get(analysisBase + "_" + iter + ".txt"), charset)) {
      outputAnalysis(out);
    }
  }

  @SuppressWarnings("unused")
  private void outputTransforms(List<Transform> learnedTransforms) throws IOException {
    try (final Writer out =
        Files.newBufferedWriter(Paths.get(analysisBase + "_transforms.txt"), charset)) {
      for (Transform t : learnedTransforms) {
        out.write(t.toDumpString());
        out.write('\n');
      }
    }
  }

  private void outputConflationSets() throws IOException {
    try (final Writer out =
        Files.newBufferedWriter(Paths.get(analysisBase + "_conflations.txt"), charset)) {
      for (Word w : lex.getSetWords(WordSet.BASE)) {
        out.write(w.toDerivedWordsString());
        out.write('\n');
      }
    }
  }

  private ArrayList<Transform> makeTransforms(
      List<Affix> baseUnmodPrefixes,
      List<Affix> unmodPrefixes,
      List<Affix> baseUnmodSuffixes,
      List<Affix> unmodSuffixes,
      Set<Transform> badTransforms) {

    final ArrayList<Transform> transforms = new ArrayList<>();
    // Prefixes
    for (Affix affix1 : baseUnmodPrefixes) {
      for (Affix affix2 : unmodPrefixes) {
        // Skip if they're the same or  if they
        // do not meet the characteristics of a good transform
        if (affix1 == affix2 || Affix.isBadAffixPair(affix1, affix2, AffixType.PREFIX)) {
          continue;
        }

        // Otherwise pair them up if the transform isn't one we've
        // marked as bad already
        final Transform newTrans = new Transform(affix1, affix2);
        if (!badTransforms.contains(newTrans)) {
          transforms.add(newTrans);
        }
      }
    }

    // Suffixes
    for (Affix affix1 : baseUnmodSuffixes) {
      for (Affix affix2 : unmodSuffixes) {
        // Skip if they're the same or if they
        // do not meet the characteristics of a good transform
        if (affix1 == affix2 || Affix.isBadAffixPair(affix1, affix2, AffixType.SUFFIX)) {
          continue;
        }

        // Otherwise pair them up if the transform isn't one we've
        // marked as bad already
        final Transform newTrans = new Transform(affix1, affix2);
        if (!badTransforms.contains(newTrans)) {
          transforms.add(newTrans);
        }
      }
    }

    return transforms;
  }

  /**
   * Sort transforms in-place according to the previously set sorting method. Depends on the
   * WEIGHTED_TRANSFORMS member for the sorting method.
   *
   * @param transforms the transforms to be sorted
   */
  private void sortTransforms(List<Transform> transforms) {
    if (WEIGHTED_TRANSFORMS) {
      transforms.sort(Transform.Comparators.byWeightedTypeCount.reversed());
    } else {
      transforms.sort(Transform.Comparators.byTypeCount.reversed());
    }
  }

  /**
   * Read in the properties at the specified path to set the parameters for learning. The mapping
   * between the names of the MorphLearner members and the official parameter names used in the
   * README is given in the implementation of this method.
   *
   * @param paramPath the path for the properties file
   * @throws IOException if the properties file cannot be read.
   */
  private void setParams(final Path paramPath) throws IOException {
    // Read in the params as properties
    final Properties props = new Properties();

    try (final BufferedReader reader = Files.newBufferedReader(paramPath)) {
      props.load(reader);
    } catch (IOException e) {
      throw new IOException("Problem reading parameter file: " + paramPath, e);
    }

    // Iteration parameters
    MAX_ITER = Integer.parseInt(props.getProperty("max_iter"));
    TOP_AFFIXES = Integer.parseInt(props.getProperty("top_affixes"));
    WINDOW_SIZE = Integer.parseInt(props.getProperty("window_size"));

    // Word scoring parameters
    Word.FREQ_THRESHOLD = Double.parseDouble(props.getProperty("frequent_prob_threshold"));
    Word.COUNT_THRESHOLD = Integer.parseInt(props.getProperty("frequent_type_threshold"));

    // Transform scoring parameters
    REEVAL_DERIVATION = Boolean.parseBoolean(props.getProperty("reeval"));
    SCORE_REEVAL = Boolean.parseBoolean(props.getProperty("score_reeval"));
    USE_DOUBLING = Boolean.parseBoolean(props.getProperty("doubling"));

    // Transform selection parameters
    TYPE_THRESHOLD = Integer.parseInt(props.getProperty("type_threshold"));
    STEM_LENGTH = Integer.parseInt(props.getProperty("overlap_stem_length"));
    OVERLAP_THRESHOLD = Double.parseDouble(props.getProperty("overlap_threshold"));
    PRECISION_THRESHOLD = Double.parseDouble(props.getProperty("precision_threshold"));

    // Preprocessing flags
    HYPHENATION = Boolean.parseBoolean(props.getProperty("hyphenation"));
    FINAL_COMPOUNDING = Boolean.parseBoolean(props.getProperty("compounding"));
    ITER_COMPOUNDING = Boolean.parseBoolean(props.getProperty("iter_compounding"));
    AGGR_COMPOUNDING = Boolean.parseBoolean(props.getProperty("aggr_compounding"));

    // Rule inference flags
    BASE_INFERENCE = Boolean.parseBoolean(props.getProperty("rule_inference_conservative"));

    // Implementation details
    TRANSFORM_OPTIMIZATION = Boolean.parseBoolean(props.getProperty("transform_optimization"));
    TRANSFORM_DEBUG = Boolean.parseBoolean(props.getProperty("transform_debug"));
    ITERATION_ANALYSIS = Boolean.parseBoolean(props.getProperty("iteration_analysis"));

    WEIGHTED_TRANSFORMS = Boolean.parseBoolean(props.getProperty("weighted_transforms"));
    WEIGHTED_AFFIXES = Boolean.parseBoolean(props.getProperty("weighted_affixes"));

    // Compounding aggressiveness controls
    Compounding.TRANSFORM_RELATIONS =
        TRANSFORM_RELATIONS = Boolean.parseBoolean(props.getProperty("transform_relations"));
    ANALYZE_SIMPLEX_WORDS =
        Boolean.parseBoolean(props.getProperty("allow_unmod_simplex_word_analysis"));
  }
}
