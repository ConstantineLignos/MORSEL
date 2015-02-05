/*******************************************************************************
 * Copyright (C) 2012 Constantine Lignos
 * 
 * This file is a part of MORSEL.
 * 
 * MORSEL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * MORSEL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MORSEL.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.upenn.ircs.lignos.morsel;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.upenn.ircs.lignos.morsel.compound.Compounding;
import edu.upenn.ircs.lignos.morsel.lexicon.Lexicon;
import edu.upenn.ircs.lignos.morsel.lexicon.Word;
import edu.upenn.ircs.lignos.morsel.lexicon.WordSet;
import edu.upenn.ircs.lignos.morsel.transform.Affix;
import edu.upenn.ircs.lignos.morsel.transform.AffixType;
import edu.upenn.ircs.lignos.morsel.transform.Transform;
import edu.upenn.ircs.lignos.morsel.transform.WordPair;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

/**
 * Main class for the MORSEL unsupervised morphology learning
 * system. Provides main method and core learning loop.
 *
 */
public class MorphLearner {
	// I/O Variables
	String corpusPath;
	PrintWriter output;
	PrintStream log;
	
	/** The lexicon being learned over */
	Lexicon lex;
	/* The following parameters are documented in the README. See the
	 * implementation of setParams for the mapping between the names
	 * of these members and the official parameter names.
	 */
	private int MAX_ITER;
	private int TOP_AFFIXES;
	private boolean REEVAL;
	private boolean SCORE_REEVAL;
	private boolean DOUBLING;
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
	private boolean ITERATION_ANALYSIS;
	private boolean WEIGHTED_TRANSFORMS;
	private boolean WEIGHTED_AFFIXES;
	private boolean TRANSFORM_RELATIONS;
	private boolean ANALYZE_SIMPLEX_WORDS;
	
	// Output options
	protected String analysisBase;
	private boolean outputBaseInf;
	private boolean outputConflation;
	private boolean outputCompounds;
	
	/**
	 * Create a new learner using the given paths for I/O.
	 * @param corpusPath the path of the input corpus
	 * @param outPath the path for the analysis output
	 * @param logPath the path for the log
	 * @param paramPath the path of the parameter file
	 * @param encoding the encoding of the input corpus file
	 * @param outputBaseInf Whether to output the examples of base inference
	 * @param outputConflation Whether to output conflation sets
	 * @param outputCompounds Whether to output an analysis before compounding
	 * @throws FileNotFoundException if the corpus or parameter files do not exist.
	 * @throws UnsupportedEncodingException if the corpus's encoding is not readable.
	 */
	public MorphLearner(String corpusPath, String outPath,
			String logPath, String paramPath, String encoding, boolean outputBaseInf, 
			boolean outputConflation, boolean outputCompounds) 
			throws FileNotFoundException, UnsupportedEncodingException {
		this.corpusPath = corpusPath;
		this.outputBaseInf = outputBaseInf;
		this.outputConflation = outputConflation;
		this.outputCompounds = outputCompounds;
		try {
			this.output = new PrintWriter(new OutputStreamWriter(
				new BufferedOutputStream(new FileOutputStream(outPath)), encoding));
		}
		catch (FileNotFoundException e) {
			throw new FileNotFoundException("Cannot open output file: " + outPath);
		}
			
		// If log is "-", don't redirect
		if (!"-".equals(logPath)) {
			this.log = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(logPath)), true, encoding);
			// Redirect output to log
			System.setOut(new PrintStream(logPath));
		}
		
		System.out.println("Setting parameters from " + paramPath);
		this.setParams(paramPath);
		
		System.out.println("Loading corpus from " + corpusPath);
		loadCorpus(encoding);
		
		// Shorten the analysis path down to a base for use in 
		analysisBase = outPath.contains(".") ? 
				outPath.substring(0, outPath.lastIndexOf('.')) :
				outPath;
	}
	
	/**
	 * Loads the corpus located at corpusPath.
	 * @param encoding the encoding of the corpus
	 * @throws FileNotFoundException if the file at corpusPath could not be read.
	 */
	public void loadCorpus(String encoding) throws FileNotFoundException {
		System.out.println("Loading words...");
		lex = CorpusLoader.loadWordlist(corpusPath, encoding, true);
		
		if (lex == null) {
			throw new FileNotFoundException("Cannot open wordlist: " + corpusPath);
		}
	}
	
	/**
	 * Clean up any open resources.
	 */
	protected void cleanUp() {
		output.close();
		// Log can be null, so check for it
		if (log != null) {log.close();}
	}
	
	/**
	 * Learn from the loaded lexicon and output the results.
	 */
	public void learn() {
		List<Transform> learnedTransforms = new LinkedList<Transform>();
		Set<Transform> badTransforms = new THashSet<Transform>();
		Map<String, Transform> indexedTransforms = new THashMap<String, Transform>();
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
			ArrayList<Transform> hypTransforms = hypothesizeTransforms(learnedTransforms, 
					badTransforms);
			
			// Score the transforms
			// Always pass false for doubling, this
			// is to ensure that rules cannot get high scores by using 
			// orthographic accomodation.
			// Reeval is set by the SCORE_REEVAL parameter
			boolean doubling = false;
			boolean reeval = SCORE_REEVAL;
			if (TRANSFORM_OPTIMIZATION) {
				// If this is the first iteration, score from scratch
				if (i == 0) {
					for (Transform trans: hypTransforms) {
						Transform.scoreTransform(trans, lex, reeval, doubling);
					}
				} 
				// Otherwise, incrementally score, using what we had from last round
				else {
					incrementalScoreTransforms(hypTransforms, indexedTransforms,
							lex, reeval, doubling);
				}
				// Update indexedTransforms for the next round
				indexedTransforms = indexTransforms(hypTransforms);
			}
			else {
				for (Transform trans: hypTransforms) {
					Transform.scoreTransform(trans, lex, reeval, doubling);
				}
			}
			
			// Rank the transforms
			sortTransforms(hypTransforms);
			
			// Trim them down, score them, and pick one
			List<Transform> topTransforms = Util.truncateCollection(hypTransforms, TOP_AFFIXES);
			Transform bestTransform = selectTransform(topTransforms, 
					learnedTransforms, badTransforms);
			
			// Quit if no good transform was found
			if (bestTransform == null) {
				System.out.println("Out of good transforms to learn. Giving up.");
				break;
			}
			
			System.out.println("Selected " + bestTransform.toString());
	
			// Re-evaluate the best transform
			if (REEVAL) {
				Transform reEvalTransform = new Transform(bestTransform.getAffix1(),
						bestTransform.getAffix2());
				Transform.scoreTransform(reEvalTransform, lex, REEVAL, DOUBLING);
				bestTransform = reEvalTransform;
			}
			
			// Accept the best transform
			System.out.println("Learned " + bestTransform.toVerboseString());
			// Reeval should be false for moving the words
			lex.moveTransformPairs(bestTransform, hypTransforms, TRANSFORM_OPTIMIZATION, 
					false, doubling);
	
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
				if (outputBaseInf && i==0) {
					try {
						baseLog = new PrintWriter(analysisBase + "_baseinf.txt");
					} catch (FileNotFoundException e) {
						System.err.println("Couldn't output base inference dump.");
					}
				}
				
				ruleInf.conservInference(lex, learnedTransforms, hypTransforms, REEVAL, 
						DOUBLING, TRANSFORM_OPTIMIZATION, baseLog);
			}
			
			//  Do iteration/aggressive compounding
			if (ITER_COMPOUNDING) {
				System.out.println("Handling iteration compounding...");
				int nCompounds = Compounding.breakCompounds(lex, WordSet.BASE, 
						learnedTransforms, hypTransforms, TRANSFORM_OPTIMIZATION,
						REEVAL, DOUBLING, transInf);
				System.out.println("Broke "  + nCompounds + " compounds in base");
				if (AGGR_COMPOUNDING) {
					nCompounds = Compounding.breakCompounds(lex, WordSet.UNMODELED, 
							learnedTransforms, hypTransforms, TRANSFORM_OPTIMIZATION,
							REEVAL, DOUBLING, transInf);
					System.out.println("Broke "  + nCompounds + " compounds in unmodeled");
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
			int nCompounds = Compounding.breakCompounds(lex, WordSet.BASE, 
					fillerRules, null, false,
					REEVAL, DOUBLING, transInf);
			System.out.println("Broke "  + nCompounds + " compounds in base");
			System.out.println(memoryStatus());
			nCompounds = Compounding.breakCompounds(lex, WordSet.UNMODELED, 
					fillerRules, null, false, REEVAL, DOUBLING, transInf);
			System.out.println("Broke "  + nCompounds + " compounds in unmodeled");	
		}
		
		// Finally, analyze simplexes as the final step
		if (ANALYZE_SIMPLEX_WORDS) {
			System.out.println("Analyzing simplex words...");
			int nAnalyzed = Compounding.analyzeSimplexWords(lex, WordSet.UNMODELED, 
					learnedTransforms, DOUBLING, transInf);
			System.out.println("Analyzed "  + nAnalyzed + " words in unmodeled");
		}
		
		System.out.println("Learning complete. Analyzing...");
		outputAnalysis(output);
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
		Map<String, Transform> index = new THashMap<String, Transform>();
		for(Transform transform : hypTransforms) {
			index.put(transform.toKey(), transform);
		}
		
		return index;
	}

	private void incrementalScoreTransforms(ArrayList<Transform> hypTransforms, 
			Map<String, Transform> indexedTransforms, Lexicon lex, 
			boolean reEval, boolean doubling) {
				// If the transform is in the index, reuse it
				for (int i = 0; i < hypTransforms.size(); i++) {
					Transform hypTransform = hypTransforms.get(i);
					Transform scoredTransform = indexedTransforms.get(hypTransform.toKey());
					
					// Reuse the old one if possible, otherwise score from scratch
					if (scoredTransform != null) {
						hypTransforms.set(i, scoredTransform);
					}
					else {
						Transform.scoreTransform(hypTransform, lex, reEval, doubling);
					}
				}
			}

	private Transform selectTransform(List<Transform> topTransforms, 
			List<Transform> learnedTransforms, Set<Transform> badTransforms) {
		// Search through the transforms for the best one
		Transform bestTransform = null;
		Transform secondTransform = null;
	
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
			double overlapRatio = 0;
			
	        // If both ratios are zero count this as OK
	        if (baseOverlap + stemOverlap == 0) {
	            System.out.println("Overlap Ratio: 0/0");
	        	overlapRatio = 0;
	        }
	        // If only the base is 0, make sure this is rejected
	        else if (baseOverlap == 0) {
	        	System.out.println("Overlap ratio: " + baseOverlap + "/0");
	        	overlapRatio = OVERLAP_THRESHOLD + 1.0;
	        }
	        else {
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
					System.out.println("Breaking tie between " + 
							bestTransform + " and " + secondTransform);
					bestTransform = breakTie(bestTransform, secondTransform);
				}
			}
			
			// Check that the segmentation precision is acceptable
			double segPrecision = Transform.calcSegPrecision(bestTransform);
			System.out.println("Seg. Precision: " + segPrecision);
			if(segPrecision < PRECISION_THRESHOLD) {
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
		if (learnedTransforms.contains(new Transform(bestTransform.getAffix2(), 
				bestTransform.getAffix1()))) {
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
	    Set<String> baseStems = new THashSet<String>();
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
		for(Transform transform : transforms) {
			System.out.println(transform.getPairsText());
		}
	}

	private ArrayList<Transform> hypothesizeTransforms(List<Transform> learnedTransforms,
			Set<Transform> badTransforms) {
		List<Affix> topBUPrefixes = lex.topAffixes(TOP_AFFIXES, AffixType.PREFIX, 
				Lexicon.AffixSet.BASEUNMOD, WEIGHTED_AFFIXES);
		List<Affix> topUPrefixes = lex.topAffixes(TOP_AFFIXES, AffixType.PREFIX,
				Lexicon.AffixSet.UNMOD, WEIGHTED_AFFIXES);
		List<Affix> topBUSuffixes = lex.topAffixes(TOP_AFFIXES, AffixType.SUFFIX,
				Lexicon.AffixSet.BASEUNMOD, WEIGHTED_AFFIXES);
		List<Affix> topUSuffixes = lex.topAffixes(TOP_AFFIXES, AffixType.SUFFIX,
				Lexicon.AffixSet.UNMOD, WEIGHTED_AFFIXES);
		
		ArrayList<Transform> hypTransforms = makeTransforms(topBUPrefixes,
				topUPrefixes, topBUSuffixes, topUSuffixes, badTransforms);
		return hypTransforms;
	}

	private static boolean isTie(Transform trans1, Transform trans2) {
		return (trans1.getAffix1() == trans2.getAffix2() &&
				trans1.getAffix2() == trans2.getAffix1() && 
				trans1.getTypeCount() == trans2.getTypeCount());
	}

	private static Transform breakTie(Transform trans1, Transform trans2) {
		int baseScore = 0;
		int derivedScore = 0;
		
		// Give a point to whichever member of the pair is more frequent
		for (WordPair pair: trans1.getWordPairs()) {
			if (pair.getBase().getCount() > pair.getDerived().getCount()) {
				baseScore++;
			}
			else if (pair.getDerived().getCount() > pair.getBase().getCount()) {
				derivedScore++;
			}
			// Award no points in a tie
		}
		
		// Choose the transform with more frequent bases
		// Ties go to the first one
		if (baseScore >= derivedScore) {
			return trans1;
		}
		else {
			return trans2;
		}
	}

	private void outputAnalysis(PrintWriter analysisOutput) {
		// Output an analysis of all the words
		List<String> sortedWords = new ArrayList<String>(lex.getWordKeys());
		Collections.sort(sortedWords);
		for (String wordKey: sortedWords) {
			Word w = lex.getWord(wordKey);
			if (w.shouldAnalyze()) {
				analysisOutput.println(wordKey + '\t' + w.analyze());
			}
		}
	}

	private void outputIterAnalysis(String iter) {
		// Output the analysis for the current iteration
		try {
			PrintWriter out = new PrintWriter(analysisBase + "_" + iter + ".txt");
			outputAnalysis(out);
			out.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't output iteration analysis.");
		}
	}
	
	
	@SuppressWarnings("unused")
	private void outputTransforms(List<Transform> learnedTransforms) {
		try {
			PrintWriter out = new PrintWriter(analysisBase + "_transforms.txt");
			for (Transform t : learnedTransforms) {
				out.println(t.toDumpString());
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't output transform dump.");
		}
	}
	
	
	private void outputConflationSets() {
		try {
			PrintWriter out = new PrintWriter(analysisBase + "_conflations.txt");
			for (Word w : lex.getSetWords(WordSet.BASE)) {
				out.println(w.toDerivedWordsString());
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't output conflation sets.");
		}
	}

	
	private ArrayList<Transform> makeTransforms(List<Affix> baseUnmodPrefixes, 
			List<Affix> unmodPrefixes, List<Affix> baseUnmodSuffixes,
			List<Affix> unmodSuffixes, Set<Transform> badTransforms) {

		ArrayList<Transform> transforms = new ArrayList<Transform>();
		// Prefixes
		for (Affix affix1: baseUnmodPrefixes) {
			for (Affix affix2: unmodPrefixes) {
				// Skip if they're the same or  if they
				// do not meet the characteristics of a good transform
				if (affix1 == affix2 || 
						Affix.isBadAffixPair(affix1, affix2, AffixType.PREFIX)) {
					continue;
				}

				// Otherwise pair them up if the transform isn't one we've
				// marked as bad already
				Transform newTrans = new Transform(affix1, affix2);
				if (!badTransforms.contains(newTrans)) {
					transforms.add(newTrans);
				}
			}
		}

		// Suffixes
		for (Affix affix1: baseUnmodSuffixes) {
			for (Affix affix2: unmodSuffixes) {
				// Skip if they're the same or if they
				// do not meet the characteristics of a good transform
				if (affix1 == affix2 ||
						Affix.isBadAffixPair(affix1, affix2, AffixType.SUFFIX)) {
					continue;
				}

				// Otherwise pair them up if the transform isn't one we've
				// marked as bad already
				Transform newTrans = new Transform(affix1, affix2);
				if (!badTransforms.contains(newTrans)) {
					transforms.add(newTrans);
				}
			}
		}

		return transforms;
	}

	/**
	 * Sort transforms in-place according to the previously set sorting method.
	 * Depends on the WEIGHTED_TRANSFORMS member for the sorting method.
	 * @param transforms the transforms to be sorted
	 */
	public void sortTransforms(List<Transform> transforms) {
		if (WEIGHTED_TRANSFORMS) {
			Collections.sort(transforms, 
					Collections.reverseOrder(new WeightedTypeCountComparator()));
		}
		else {
			Collections.sort(transforms, 
					Collections.reverseOrder(new TypeCountComparator()));
		}
	}

	/**
	 * Read in the properties at the specified path to set the parameters for learning.
	 * The mapping between the names of the MorphLearner members and the official parameter
	 * names used in the README is given in the implementation of this method.
	 * @param paramPath the path for the properties file
	 * @return true if the parameter file was successfully read
	 * @throws FileNotFoundException if the properties file cannot be read
	 */
	public boolean setParams(String paramPath) throws FileNotFoundException {
		// Read in the params as properties
		Properties props = new Properties();
		FileInputStream paramFile;
		
		try {
			paramFile = new FileInputStream(paramPath);
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Cannot open parameter file: " + paramPath);
		}
			
		try {
			props.load(paramFile);
		} catch (IOException e) {
			throw new FileNotFoundException("Problem reading parameter file: " + paramPath);
		}
		
		// Iteration parameters
		MAX_ITER = Integer.parseInt(props.getProperty("max_iter"));
		TOP_AFFIXES = Integer.parseInt(props.getProperty("top_affixes"));
		WINDOW_SIZE = Integer.parseInt(props.getProperty("window_size"));
		
		// Word scoring parameters
		Word.FREQ_THRESHOLD =  Double.parseDouble(props.getProperty("frequent_prob_threshold"));
		Word.COUNT_THRESHOLD = Integer.parseInt(props.getProperty("frequent_type_threshold"));
		
		// Transform scoring parameters
		REEVAL = Boolean.parseBoolean(props.getProperty("reeval"));
		SCORE_REEVAL = Boolean.parseBoolean(props.getProperty("score_reeval"));
		DOUBLING = Boolean.parseBoolean(props.getProperty("doubling"));
		
		// Transform selection parameters
		TYPE_THRESHOLD = Integer.parseInt(props.getProperty("type_threshold"));
	 	STEM_LENGTH = Integer.parseInt(props.getProperty("overlap_stem_length"));
		OVERLAP_THRESHOLD =  Double.parseDouble(props.getProperty("overlap_threshold"));
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
		ITERATION_ANALYSIS = Boolean.parseBoolean(props.getProperty("iteration_analysis"));
		
		WEIGHTED_TRANSFORMS = Boolean.parseBoolean(props.getProperty("weighted_transforms"));
		WEIGHTED_AFFIXES = Boolean.parseBoolean(props.getProperty("weighted_affixes"));
		
		// Compounding aggressiveness controls
		Compounding.TRANSFORM_RELATIONS = TRANSFORM_RELATIONS = 
			Boolean.parseBoolean(props.getProperty("transform_relations"));
		ANALYZE_SIMPLEX_WORDS = 
			Boolean.parseBoolean(props.getProperty("allow_unmod_simplex_word_analysis"));
		
		// Clean up
		try {
			paramFile.close();
		} catch (IOException e) {
			// Ignore it
		}
		
		return true;
	}

	/**
	 * Call the learner on the specified arguments. Exit using standard error codes
	 * (sysexits.h) if an error is encountered.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		// Set up command line arguments
		Options options = new Options();
		options.addOption(new Option("h", "help", false, "dislay this help and exit"));
		Option encodingOption = new Option("e", "encoding", true,
				"input and output file encoding. Defaults to ISO8859_1.");
		encodingOption.setArgName("encoding");
		options.addOption(encodingOption);
		options.addOption(new Option("b", "base-inf", false,
				"output the examples of base inference. This does not change whether base inference" +
				" is used; it simply outputs the examples that used it."));
		options.addOption(new Option("s", "conflation-sets", false,
				"output the learner's conflation sets"));
		options.addOption(new Option("c", "compounds", false,
				"output the learner's analsyis before final compounding is used"));
		HelpFormatter formatter = new HelpFormatter();
		String usage = "MorphLearner [options] wordlist outfile logfile paramfile";
		
		CommandLine line = null;
		String[] otherArgs = null;
	    try {
	        // Parse the command line arguments
	        line = parser.parse(options, args);
	        otherArgs = line.getArgs();
	        
		    // Handle help
			if (line.hasOption("help")) {
				formatter.printHelp(usage, options);
				System.exit(0);
			}
	        
	        if (otherArgs.length != 4) {
	        	throw new ParseException("Incorrect number of required arguments specified");
	        }
	    }
	    catch(ParseException exp) {
	        System.out.println(exp.getMessage());
	        formatter.printHelp(usage, options);
	        System.exit(64);
	    }
		
		// Get options
		String encoding = line.getOptionValue("encoding", "ISO8859_1");
		boolean outputBaseInf = line.hasOption("base-inf");
		boolean outputConflation = line.hasOption("conflation-sets");
		boolean outputCompounds = line.hasOption("compounds");
		
		// Setup timing
		long start;
		float elapsedSeconds;
		start = System.currentTimeMillis();

		// Initialize the learner
		MorphLearner learner = null;
		try {
			learner = new MorphLearner(otherArgs[0], otherArgs[1], otherArgs[2], otherArgs[3],
						encoding, outputBaseInf, outputConflation, outputCompounds);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			System.exit(66);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Unsupported file encoding: " + encoding);
			System.exit(74);
		}
		
		// Output time stats
		elapsedSeconds = (System.currentTimeMillis() - start)/1000F;
		System.out.println("Init time: " + elapsedSeconds + "s\n");
		
		// Time learning
		start = System.currentTimeMillis();
		
		learner.learn();
		learner.cleanUp();
		
		// Output time stats
		elapsedSeconds = (System.currentTimeMillis() - start)/1000F;
		System.out.println("\nLearning time: " + elapsedSeconds + "s");
	}

	/**
	 * Compare transforms by their weighted type count
	 *
	 */
	public static class WeightedTypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object transform1, Object transform2) {
			return Long.compare(((Transform) transform1).getWeightedTypeCount(), 
			((Transform) transform2).getWeightedTypeCount());
		}
	}

	/**
	 * Compare transforms by their type count
	 *
	 */
	public static class TypeCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object transform1, Object transform2) {
			return Long.compare(((Transform) transform1).getTypeCount(), 
			((Transform) transform2).getTypeCount());
		}
	}

	/**
	 * Compare transforms by their token count
	 *
	 */
	public static class TokenCountComparator implements Comparator<Object> {
		@Override
		public int compare(Object transform1, Object transform2) {
			return Long.compare(((Transform) transform1).getTokenCount(),
			((Transform) transform2).getTokenCount());
		}
	}
}
