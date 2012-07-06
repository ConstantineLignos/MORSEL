MORSEL
======

*MORphological Sparsity Embiggens Learning: A simple, unsupervised morphological learning model*

MORSEL is an unsupervised morphology learner created for Morpho Challenge 2010. Its input is a wordlist with
frequency information for each word, and its output is a morphological analysis for each word. Crucially, it
isn't a segmenter; it's a morphological analyzer. Its goal is to learn the morphological grammar of the language
and report what morphemes are present in each word.

Using MORSEL
============
MORSEL is distributed under the GPLv3. You are welcome to use it for performing evaluations, for bootstrapping
a supervised learner, or for any other purpose you can think of as long as you comply with the license. Please
get in touch with me (http://www.seas.upenn.edu/~lignos/) if you're using it; while the documentation should
be adequate it's always good to hear what applications other people have in mind for this system. I ask that
if you use it you cite my Morpho Challenge 2010 paper (BibTeX information below).

## Dependencies
- Java JDK 1.6 (newer versions will almost certainly work but have never been tested)
- ant (tested with 1.8.2)

## Building
Once you've got the dependencies installed, you can simply run ant in the root of the repository and it will compile
all source and create morsel.jar. The jar depends on the other jars in lib/ (cli, junit, and trove), so if you move 
the jar you'll need to move lib/ along with it.

## Taking it for a spin
You run MORSEL by running the jar and providing:
* A wordlist formatted with a word and its frequency on each line separated by whitespace, i.e., `69971 the`. 
  See data/test/brown_wordlist.txt for an example.
* An output file where the morphological analyses will be stored. The output will look like this:

```text
accelerate      ACCELERATE
accelerated     ACCELERATE +(ed)
accelerating    ACCELERATE +(ing)
acceleration    ACCELERATE +(ion)
accelerations   ACCELERATE +(ion) +(s)
```

* A log file to record what the learner is doing while it learns. If you'd like it to write to standard out, just enter 
  `-` for the log file.
* A parameter file to configure the segmenter. There are two files in the params folder corresponding to the 
  parameters used for the Morpho Challenge 2010 evaluation. Both settings (aggressive and conservative) produced state 
  of the art results for English in Morpho Challenge 2010, and aggressive produced state of the art results for Finnish.
  I've done the best that I can to document these parameters between the param files and the code, but if you want
  to try changing them I'd recommend getting in touch.
* The rest of the parameters are documented by running `java -jar morsel.jar --help`. With the exception of encoding,
  unless you are interested in the algorithm's internals there isn't much to see here.
  
For example, if you want to run on the Brown corpus wordlist, write the analysis to out.txt, write the log to log.txt,
and use the conservative parameter set do the following:

`java -jar morsel.jar data/test/brown_wordlist.txt out.txt log.txt params/conservative.txt`

## Evaluating MORSEL
MORSEL was designed for Morpho Challenge, so it's best evaluated using the Morpho Challenge metrics (which you
can get from their website) or the EMMA metric. I have three requests for people performing evaluations:
* MORSEL is a rule-based system for affixal morphology. It is not designed for templatic morphology (e.g., Arabic,
  Hebrew), so please don't bother evaluating it in those languages. It can be adapted to work in these languages
  in theory, but it's a fair amount of work.
* MORSEL is a morphological analyzer. If you are evaluating it in a segmentation task, you'll have to adapt its
  output as a segmentation, which will distort its performance. If you evaluate in this manner, please make it clear 
  how you adapted it and that the segmentation is derived from MORSEL's output.
* Please cite the following paper:

```latex
 @InProceedings{lignos:2010:MC,
  title={{Learning from Unseen Data}},
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

Constantine Lignos  
Institute for Research in Cognitive Science  
Computer and Information Science Department  
University of Pennsylvania
