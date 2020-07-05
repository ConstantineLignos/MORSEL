#! /usr/bin/env python

"""
Combine the BPE segmentation of stems with MORSEL's analyis of affixes.
"""

import argparse
from typing import List, Dict


def apply_bpe_to_segmentation(
    input_segmentation_path: str, stem_path: str, output_path: str
) -> None:
    """Combine segmentation output with BPE for the roots."""
    # Load the BPE for each stem
    stem_units = {}
    with open(stem_path, encoding="utf8") as stem_file:
        for line in stem_file:
            units = line.rstrip("\n").split(" ")
            # Add stem prefix
            stem_units["_" + _remove_bpe(units)] = units

    with open(input_segmentation_path, encoding="utf8") as input_file, open(
        output_path, "w", encoding="utf8"
    ) as output_file:
        for line in input_file:
            word, segmentation = line.rstrip("\n").split("\t")
            orig_units = segmentation.split(" ")
            stem_bpe_units = _apply_bpe_to_stems(orig_units, stem_units)
            final_bpe_units = _format_bpe(stem_bpe_units)

            print(word, " ".join(final_bpe_units), sep="\t", file=output_file)


def _remove_bpe(units: List[str]) -> str:
    """Remove BPE and join units."""
    # Could probably do this faster by making into a str first then regex replace
    assert not units[-1].endswith("@@")
    assert all(unit.endswith("@@") for unit in units[:-1])
    return "".join([unit[:-2] for unit in units[:-1]] + [units[-1]])


def _apply_bpe_to_stems(
    units: List[str], stem_units: Dict[str, List[str]]
) -> List[str]:
    """Replace stems with their BPE-applied forms and strip affix prefixes."""
    new_units = []
    for unit in units:
        if unit.startswith("_"):
            if unit in stem_units:
                new_units.extend(stem_units[unit])
            else:
                print(f"Missing unit: {unit}")
                new_units.append(unit[1:])
        elif unit.startswith("+"):
            new_units.append(unit[1:])
        else:
            raise ValueError(f"Unrecognized unit: {repr(unit)}")

    return new_units


def _format_bpe(units: List[str]) -> List[str]:
    """Format units such that all non-final units end in '@@' and final units do not."""
    new_units = []
    for unit in units[:-1]:
        if not unit.endswith("@@"):
            unit += "@@"
        new_units.append(unit)

    final_unit = units[-1]
    if final_unit.endswith("@@"):
        final_unit = final_unit[:-2]
    new_units.append(final_unit)

    return new_units


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "segmentation_file",
        help="path to a segmentation file created by process_segmentation",
    )
    parser.add_argument(
        "bpe_applied_stem_file",
        help="path to a file containing the segmentation roots with BPE applied to them (from subword-nmt apply-bpe)",
    )
    parser.add_argument("output", help="output file path")
    args = parser.parse_args()
    apply_bpe_to_segmentation(
        args.segmentation_file, args.bpe_applied_stem_file, args.output
    )


if __name__ == "__main__":
    main()
