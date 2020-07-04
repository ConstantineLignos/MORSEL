#! /usr/bin/env python

"""
Processes MORSEL's segmentation into a more usable format.

The output file format consists of three tab-separated columns:
1. The original word form ("kicked");
2. Space-delimited segments identified by MORSEL ("kick ed");
3. BPE-formatted space-delimited segments identified by MORSEL ("kick@@ ed").
"""

import argparse
from typing import List


def process_segmentation(input_path: str, output_path: str) -> None:
    """Process MORSEL's segmentation output into a more usable format."""
    with open(input_path, encoding="utf8") as input_file, open(
        output_path, "w", encoding="utf8"
    ) as output_file:
        for line_num, line in enumerate(input_file, 1):
            line = line.rstrip()
            fields = line.split("\t")
            if len(fields) != 3:
                raise ValueError(
                    f"Expected 3 fields on line {line_num}, found {len(fields)}: {repr(line)}"
                )
            word, _, segmentation = fields

            # Get a first segmentation
            seg_units = parse_segmentation(segmentation)
            joined_word = "".join(seg_units)
            # Correct it if needed
            if joined_word != word:
                seg_units = correct_segmentation(seg_units, word)
                joined_word = "".join(seg_units)
                assert (
                    joined_word == word
                ), f"Joined word does not match original: {repr(word)}, {repr(joined_word)}, {repr(seg_units)}"

            joined_units = " ".join(seg_units)
            bpe_units = " ".join(
                [
                    (unit if idx == len(seg_units) - 1 else unit + "@@")
                    for idx, unit in enumerate(seg_units)
                ]
            )
            print(word, joined_units, bpe_units, sep="\t", file=output_file)


def parse_segmentation(segmentation: str) -> List[str]:
    """Parse MORSEL's segmentation string into a list of units."""
    segments = []
    for segment in segmentation.split(" "):
        operator = segment[0]
        segment_text = segment[1:]
        if operator == "_":
            # Root segment
            segments.append(segment_text)
        elif operator == "+":
            # Concatenative affix
            segments.append(segment_text)
        elif operator == "-":
            # Subtractive affix
            assert (
                segments
            ), f"Segment {repr(segment)} removes without a prior segment: {repr(segmentation)}"
            # Remove from the last segment
            assert segments[-1].endswith(segment_text)
            segments[-1] = segments[-1][: -len(segment_text)]
        elif operator == "?":
            assert (
                segments
            ), f"Segment {repr(segment)} accommodates without a prior segment: {repr(segmentation)}"
            operator = segment_text[0]
            segment_text = segment_text[1:]
            if operator == "-":
                # Remove from the last segment
                assert segments[-1].endswith(segment_text)
                segments[-1] = segments[-1][: -len(segment_text)]
            elif operator == "+":
                # Add to the last segment
                segments[-1] = segments[-1] + segment_text

    return segments


def correct_segmentation(units: List[str], word: str) -> List[str]:
    """Correct a segmentation for missing hyphens.

    This is necessary because MORSEL splits on hyphens but does not include them in its
    analysis output."""
    new_units = []
    idx = 0
    for unit in units:
        if not word.startswith(unit, idx):
            if word[idx] == "-":
                idx = _add_hyphens(units, new_units, word, idx)
            else:
                raise ValueError(
                    f"Cannot correct segmentation at index {idx} of word {repr(word)}: {units}"
                )
        new_units.append(unit)
        idx += len(unit)

    if idx < len(word):
        # Address trailing hyphens
        if word[idx] == "-":
            idx = _add_hyphens(units, new_units, word, idx)

        # Check one more time
        if idx != len(word):
            raise ValueError(
                f"Units are shorter than word at index {idx} of word {repr(word)}: {units}"
            )

    return new_units


def _add_hyphens(units: List[str], new_units: List[str], word: str, idx: int) -> int:
    # Need a while loop to handle multiple hyphens
    while word[idx] == "-":
        new_units.append("-")
        idx += 1
        if idx >= len(word):
            break

    return idx


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_file",
                        help="path to a MORSEL analysis file with segmentations (make sure to enable segmentation)")
    parser.add_argument("output_file", help="output file path")
    args = parser.parse_args()
    process_segmentation(args.input_file, args.output_file)


if __name__ == "__main__":
    main()
