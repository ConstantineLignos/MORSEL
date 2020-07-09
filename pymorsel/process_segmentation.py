#! /usr/bin/env python

"""
Processes MORSEL's segmentation into a more usable format.

The segmentation output file format consists of tab-separated columns:
1. The original word form ("kicked")
2. Space-delimited segments identified by MORSEL, with roots prefixed by _ and affixes
   prefixed by + ("_kick +ed")
"""

import argparse
from collections import Counter
from typing import List


BASE_PREFIX = "_"
DOUBLE_PLUS = "⧺"


def process_segmentation(
    analysis_path: str, wordlist_path: str, segmentation_path: str, dict_path: str
) -> None:
    """Process MORSEL's segmentation output into a more usable format."""
    word_units = {}
    with open(analysis_path, encoding="utf8") as input_file, open(
        segmentation_path, "w", encoding="utf8"
    ) as output_file:
        for line_num, line in enumerate(input_file, 1):
            line = line.rstrip()
            fields = line.split("\t")
            if len(fields) != 3:
                raise ValueError(
                    f"Expected 3 fields on line {line_num}, found {len(fields)}: {repr(line)}"
                )
            word, _, morsel_segmentation = fields

            # Split segmentation into individual compound words
            compound_segmentations = morsel_segmentation.split(" || ")

            # Join together segmentations across compounds
            seg_units = []
            for segmentation in compound_segmentations:
                segments = parse_segmentation(segmentation)
                clean_segments = _cleanup_prefixes(segments)
                seg_units.extend(clean_segments)

            joined_word = join_segmentation(seg_units)
            # Correct it if needed
            if joined_word != word:
                seg_units = correct_segmentation(seg_units, word)
                joined_word = join_segmentation(seg_units)
                assert joined_word == word, (
                    f"Joined word {repr(joined_word)} does not match "
                    f"original {repr(word)}, units: {repr(seg_units)}"
                )

            word_units[word] = seg_units
            print(word, " ".join(seg_units), sep="\t", file=output_file)

    # Get counts from the wordlist
    word_counts = {}
    with open(wordlist_path, encoding="utf8") as wordlist_file:
        for line_num, line in enumerate(wordlist_file):
            fields = line.rstrip("\n").split(" ")
            if len(fields) != 2:
                raise ValueError(
                    f"Cannot parse line {line_num} of {wordlist_path} into two fields: "
                    f"{repr(line)}"
                )
            word_counts[fields[1]] = int(fields[0])

    root_counts = Counter()
    affix_counts = Counter()
    for word, units in word_units.items():
        for unit in units:
            if unit.startswith(BASE_PREFIX):
                root_counts[unit] += word_counts[word]
            else:
                affix_counts[unit] += word_counts[word]

    with open(dict_path, "w", encoding="utf8") as dict_file:
        for root, count in root_counts.most_common():
            # Strip root prefix
            word = root[1:]
            print(word, count, file=dict_file)

    print("Number of unique affixes:", len(affix_counts))


def parse_segmentation(segmentation: str) -> List[str]:
    """Parse MORSEL's segmentation string into a list of units."""
    source = segmentation.split(" ")
    segments = []
    for idx, segment in enumerate(source):
        operator = segment[0]
        segment_text = segment[1:]
        if operator == BASE_PREFIX:
            # Root segment
            segments.append(segment)
        elif operator == "+":
            # Concatenative affix
            segments.append(segment)
        elif operator == "$":
            # Handle accommodation at end of previous string
            assert (
                segments
            ), f"Segment {repr(segment)} accommodates without a prior segment: {repr(segmentation)}"
            inner_operator = segment_text[0]
            inner_segment_text = segment_text[1:]
            if inner_operator == "-":
                _end_delete(inner_segment_text, segments, segmentation)
            elif inner_operator == "+":
                # Add to the last segment
                segments[-1] = segments[-1] + inner_segment_text
        elif operator == "^":
            # Handle accommodation at start of next string
            target_idx = idx + 1
            assert target_idx < len(
                source
            ), f"Segment {repr(segment)} accommodates without a following segment: {repr(segmentation)}"
            operator = segment_text[0]
            segment_text = segment_text[1:]
            # Get the next segment from the source but skip the prefix
            source_segment = source[target_idx]
            assert source_segment.startswith(BASE_PREFIX) or source_segment.startswith(
                "+"
            ), f"Segment {repr(segment)} accommodates a non-root following segment: {repr(segmentation)}"
            source_segment_text = source_segment[1:]
            if operator == "-":
                if not len(segment_text) < len(source_segment_text):
                    raise NotImplementedError(
                        "Deletion of a full segment or more is not implemented for prefix accommodation"
                    )
                # Remove from the front of the next segment
                assert source_segment_text.startswith(segment_text)
                source[target_idx] = (
                    source_segment[0] + source_segment_text[len(segment_text) :]
                )
            elif operator == "+":
                # Add to the front of the next segment
                source[target_idx] = (
                    source_segment[0] + source_segment_text[0] + source_segment_text
                )
        else:
            raise ValueError(
                f"Unknown operator {repr(operator)} in segmentation: {repr(segmentation)}"
            )

    assert segments, f"Empty segments for segmentation: {repr(segmentation)}"

    return segments


def _end_delete(segment_text: str, segments: List[str], segmentation: str) -> List[str]:
    """Remove text from the preceding segments.

    The segmentation is only provided to make errors more readable."""
    last_segment = segments[-1]
    if len(last_segment) > len(segment_text):
        # Easy case: everything can be removed from the previous segment
        assert segments[-1].endswith(segment_text), (
            f"Segment {repr(segments[-1])} should end with {segment_text} "
            f"in segmentation {repr(segmentation)}"
        )
        segments[-1] = segments[-1][: -len(segment_text)]
    else:
        # Hard case: remove segments as needed
        to_delete = len(segment_text)
        while to_delete:
            if not segments:
                raise ValueError(
                    f"Ran out of prior segments trying to delete {repr(segment)}"
                    f"in segmentation {repr(segmentation)}"
                )
            # Offset length by one since there's a prefix
            last_segment_len = len(segments[-1]) - 1
            if to_delete >= last_segment_len:
                segments.pop()
                to_delete -= last_segment_len
            else:
                segments[-1] = segments[-1][:-to_delete]
                to_delete = 0


def join_segmentation(units: List[str]) -> str:
    """Join a segmentation created by parse_segmentation into a str."""
    return "".join([unit[1:] for unit in units])


def correct_segmentation(units: List[str], word: str) -> List[str]:
    """Correct a segmentation for missing hyphens.

    This is necessary because MORSEL splits on hyphens but does not include them in its
    analysis output."""
    new_units = []
    idx = 0
    for unit in units:
        unit_text = unit[1:]
        if not word.startswith(unit_text, idx):
            if word[idx] == "-":
                idx = _add_hyphens(new_units, word, idx)
            else:
                raise ValueError(
                    f"Cannot correct segmentation at index {idx} of word {repr(word)}: {units}"
                )
        new_units.append(unit)
        idx += len(unit_text)

    if idx < len(word):
        # Address trailing hyphens
        if word[idx] == "-":
            idx = _add_hyphens(new_units, word, idx)

        # Check one more time
        if idx != len(word):
            raise ValueError(
                f"Units are shorter than word at index {idx} of word {repr(word)}: {units}"
            )

    return new_units


def _add_hyphens(new_units: List[str], word: str, idx: int) -> int:
    # Need a while loop to handle multiple hyphens
    while word[idx] == "-":
        new_units.append(BASE_PREFIX + "-")
        idx += 1
        if idx >= len(word):
            break

    return idx


def _cleanup_prefixes(segments: List[str]) -> List[str]:
    """Transform prefix segments from +foo to ⧺foo."""
    out_segments = []
    found_base = False
    for segment in segments:
        if segment.startswith(BASE_PREFIX):
            found_base = True

        if segment.startswith("+") and not found_base:
            segment = DOUBLE_PLUS + segment[1:]

        out_segments.append(segment)

    return out_segments


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "analysis_file",
        help="path to a MORSEL analysis file with segmentations (make sure to enable segmentation)",
    )
    parser.add_argument(
        "word_list",
        help="path to a Morpho Challenge format wordlist matching the analyses (for frequency information)",
    )
    parser.add_argument("output_segmentation", help="output segmentation file path")
    parser.add_argument(
        "output_root_dict", help="output root dictionary file path (for learn-BPE)"
    )
    args = parser.parse_args()
    process_segmentation(
        args.analysis_file,
        args.word_list,
        args.output_segmentation,
        args.output_root_dict,
    )


if __name__ == "__main__":
    main()
