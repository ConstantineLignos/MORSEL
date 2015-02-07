MORSEL
======

*MORphological Sparsity Embiggens Learning: A simple, unsupervised morphological learning model*

MORSEL is an unsupervised morphology learner created for Morpho
Challenge 2010. Its input is a wordlist with frequency information for
each word, and its output is a morphological analysis for each
word. Crucially, it isn't a segmenter; it's a morphological
analyzer. Its goal is to learn the morphological grammar of the
language and report what morphemes are present in each word.

Using MORSEL
============

MORSEL is distributed under the GPLv3. You are welcome to use it for
performing evaluations, for bootstrapping a supervised learner, or for
any other purpose you can think of as long as you comply with the
license. Please get in touch with me (http://lignos.org/) if you're
using it; while the documentation should be adequate it's always good
to hear what applications other people have in mind for this system. I
ask that if you use it you cite my Morpho Challenge 2010 paper (BibTeX
information below).

## Dependencies

- Java JDK 1.6 or newer (tested with 1.6, 1.7, and 1.8)
- ant (tested with 1.8.2 and 1.9.4)

## Building

Once you've got the dependencies installed, you can simply run `ant`
in the root of the repository and it will compile all source and
create `morsel.jar`. The jar depends on the other jars in `/lib`
(Apache CLI, JUnit, and GNU Trove), so if you move the jar you'll need
to move `/lib` along with it.

## Taking it for a spin
You run MORSEL by running the jar and providing:
* A wordlist formatted with a word and its frequency on each line
  separated by whitespace, i.e., `69971 the`.  See
  `data/test/brown_wordlist.txt` for an example.
* An output file where the morphological analyses will be stored. The
  output will look like this:

```text
accelerate      ACCELERATE
accelerated     ACCELERATE +(ed)
accelerating    ACCELERATE +(ing)
acceleration    ACCELERATE +(ion)
accelerations   ACCELERATE +(ion) +(s)
```
* A log file to record what the learner is doing while it learns. If
  you'd like it to write to standard out, just enter `-` for the log
  file.
* A parameter file to configure the segmenter. There are two files in
  the params folder corresponding to the parameters used for the
  Morpho Challenge 2010 evaluation. Both settings (aggressive and
  conservative) produced state of the art results for English in
  Morpho Challenge 2010, and aggressive produced state of the art
  results for Finnish.
* The rest of the command-line parameters are documented by running
  `java -jar morsel.jar --help`. With the exception of encoding,
  unless you are interested in the algorithm's internals and want more
  debug output there isn't much to see here.  Almost everything you
  want to set is set via the parameter file, not command-line
  arguments.
  
For example, if you want to run on the Brown corpus wordlist, write
the analysis to `out.txt`, write the log to `log.txt`, and use the
conservative parameter set do the following:

`java -jar morsel.jar data/test/brown_wordlist.txt out.txt log.txt params/conservative.txt`

If you're using a data set of any significant size, you'll want to increase Java's maximum heap size, see Java's
`Xmx` flag.

## Evaluating MORSEL

MORSEL was designed for Morpho Challenge, so it's best evaluated using
the Morpho Challenge metrics (which you can get from their website) or
the EMMA metric. I have three requests for people performing
evaluations:
* MORSEL is a rule-based system for affixal morphology. It is not
  designed for templatic morphology (e.g., Arabic, Hebrew), so please
  don't bother evaluating it in those languages. It can be adapted to
  work in these languages in theory, but it's a fair amount of work.
* MORSEL is a morphological analyzer. If you are evaluating it in a
  segmentation task, you'll have to adapt its output as a
  segmentation, which will distort its performance. If you evaluate in
  this manner, please make it clear how you adapted it and that the
  segmentation is derived from MORSEL's output.
* Please cite the following paper:

```latex
 @InProceedings{lignos:2010:MC,
  title={Learning from Unseen Data},
  author={Lignos, Constantine},
  booktitle = {Proceedings of the Morpho Challenge 2010 Workshop},
  year = {2010},
  address = {Helsinki, Finland},
  month = {September 2--3},
  organization = {Aalto University School of Science and Technology},
  pages={35--38},
  editor={Kurimo, Mikko and Virpioja, Sami and Turunen, Ville T.}
}
```

Design
=====

## Overview

Some relevant facts about the design of MORSEL:
* MORSEL is single-threaded. It would be trivial to parallelize the
  transform scoring process that happens each iteration of learning,
  but as MORSEL already runs quite quickly I stuck with the simplicity
  of a serial implementation.
* MORSEL uses GNU Trove for high performance hash maps and
  sets. Because it does not create an additional object for every entry in
  the hash table, using Trove can result in almost 50% lower memory
  usage on large data sets in addition to some speed improvements.
* MORSEL uses a fairly large amount of memory in order to speed
  learning. It keeps all words and a large set of hypothesized
  transforms in memory at once. You may need up to 16GB of memory to
  run MORSEL on the Morpho Challenge 2010 Finnish data set. Small data
  sets take much less memory; running on the Brown Corpus only uses
  about 256MB of RAM.

## Parameter files

MORSEL's behavior is largely specified via the parameter file specified
on the command line. Here is a description of the parameters used. For
sensible defaults, see `params/conservative.txt`. Some of these
parameters are difficult to understand without understanding the
learning algorithm. I recommend you look at the following papers:

[A rule-based unsupervised morphology learning framework](http://lignos.org/papers/Lignos_etal_MC2009.pdf)  
Constantine Lignos, Erwin Chan, Mitchell P. Marcus, and Charles Yang  
Working Notes of the 10th Workshop of the Cross-Language Evaluation
Forum (CLEF), 2009.

[Learning from unseen data](http://lignos.org/papers/mc_2010_lignos.pdf)  
Constantine Lignos  
Proceedings of the Morpho Challenge 2010 Workshop, 35-38, 2010.

Learning iteration parameters:
* `max_iter`: The maximum number of transforms that will be learned
  from the corpus. One transform is learned per iteration. Usually,
  learning stops well before this is reached because other stopping
  criteria are met.
* `top_affixes`: The number of affixes considered in each learning
  iteration as part of a transform.
* `window_size`: The number of transforms that can be vetted in a
  single iteration. If this number of transforms is reached, learning
  stops.

Word scoring parameters:
* `frequent_type_threshold`: The frequency a word needs to be above
  to be counted towards the number of types a transform covers. This
  is useful for excluding extremely rare items. For example, setting
  this to one excludes all hapax legomena (words only seen once) from
  the transform selection process.
* `frequent_prob_threshold`: Similar to `frequent_type_threshold`,
  expect the cutoff is specified as a normalized frequency rather than
  a raw count.

Transform scoring parameters:
* `reeval`: Whether words identified originally as bases can later
  become derived forms. For example, when set to `true`, after the
  pairs (*bake*, *bakes*) and (*baker*, *bakers*) are identified
  through the transform (*$*, *s*), when
  the transform (*e*, *er*) is learned *baker* will change from being a
  base form to a derived one, allowing it to be related to both *bake*
  and *bakers*.
* `score_reeval`: Whether pairs that require `reeval` to be identified
  should be counted when transforms are scored.
* `doubling`: Whether to allow doubling and de-doubling of characters
  at the point of morpheme concatenation. For example, when doubling
  is set to `true`, *pin* + *-ing* can be *pinning*, and *bake* +
  *-ed* can be *baked*. This allows for some flexibility regarding
  orthographic conventions that the learner does not explicitly
  identify.

Transform selection parameters:
* `type_threshold`: The minimum number of word pairs
  a transform needs to model in order to be considered valid. This can
  be used to prevent the learning of transforms that only apply to a
  handful of words. (In the final selected parameters, this was set
  to two, effectively disabling any filtering.)
* `overlap_stem_length`: The length of the stem to be used in the
  *stem overlap* calculation.
* `overlap_threshold`: The threshold for *overlap ratio* above which a
  transform is rejected. While this parameter is set, the stem
  overlap filter never comes into play in any well-behaved data set.
* `precision_threshold`: The minimum required *segmentation precision*
  for a transform to be accepted.

Weighting parameters:
* `weighted_transforms`: Whether the score of transforms should be
  weighted by the number of characters they add to a word. Setting to
  `true` allows for rarer but more substantial transforms to outscore
  shorter but more frequent ones.
* `weighted_affixes`: Whether the score of affixes should be weighted
  by their length.  Setting to `true` allows for rarer but longer
  affixes to outscore shorter but more frequent ones.

Pre-processing parameters:
* `hyphenation`: Whether to always split words on hyphens.
* `compounding`: Whether to split compounds at the end of learning
  (basic compounding).
* `iter_compounding`: Whether to split compounds at the end of every
  iteration (iterative compounding).
* `aggr_compounding`: Whether to use the learned transforms to make
  transform splitting more aggressive (aggressive compounding).

Word/transform Inference parameters:
* `rule_inference_conservative`: Whether to use the transforms learned
    to infer missing bases (base inference).

Experimental features (features that appear to be implemented
correctly but do not improve performance):
* `transform_relations`: Whether to filter potential analyses by
whether the transforms in the analysis have appeared together in
previous analyses.
* `allow_unmod_simplex_word_analysis`: Whether to attempt to
  aggressively segment unmodeled words using the learned transforms.

Implementation-internal parameters:
* `transform_optimization`: Whether to dramatically improve
performance by keeping persistent data structures across
iterations. You want this set to `true` unless you're debugging
unexpected behavior.
* `iteration_analysis`: Whether to output the analysis of the lexicon
  at every single iteration. Generates a large amount of output but is
  useful for examining the learning trajectory.


## External libraries

MORSEL depends on:
* Apache Commons CLI library for parsing command-line arguments
* junit for unit testing
* GNU trove for hash maps and sets

Because Maven was not yet widely adopted by researchers when MORSEL
was first developed, JAR files for the dependencies are located in the
`/lib` folder. The JAR built by MORSEL (`morsel.jar`) will look for
these dependencies in the same directory.

## Building

The ant buildfile `build.xml` specifies how MORSEL should be
built. Just running `ant` will perform compilation (`compile` target)
and packaging the JAR (`dist` target). To run unit tests, run `ant
test`. `ant clean` cleans all compilation output, including the JAR.

The JAR manifest is set such that the main class
`edu.upenn.ircs.lignos.morsel.MorphLearner` is run when the jar is
executed. This is the the only main function in MORSEL.

Enjoy!

Constantine Lignos  
Institute for Research in Cognitive Science  
Computer and Information Science Department  
University of Pennsylvania
