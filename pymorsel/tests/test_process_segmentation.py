import pytest

from ..process_segmentation import parse_segmentation


def test_basic():
    assert parse_segmentation("+un _known +s") == ["+un", "_known", "+s"]
    assert parse_segmentation("_fake $-e +ing") == ["_fak", "+ing"]


def test_suffix_subtraction():
    # Subtract from only the previous segment
    assert parse_segmentation("_foo $-o +er") == ["_fo", "+er"]
    # Subtract more than the previous segment
    assert parse_segmentation("_gravity $-y +ate $-tate +s") == ["_gravi", "+s"]
    # Subtract exactly the previous segment
    assert parse_segmentation("_alb +a $-a +an") == ["_alb", "+an"]


def test_prefix_subtraction():
    # Subtract from only the next segment
    assert parse_segmentation("+bl ^-f _foo") == ["+bl", "_oo"]
    # Subtract more than the next segment
    # Not supported right now
    with pytest.raises(NotImplementedError):
        assert parse_segmentation("+un ^-ab +a _bdone") == ["+un", "_done"]


def test_suffix_accommodation():
    # Doubling
    assert parse_segmentation("_pin $+n +er") == ["_pinn", "+er"]
    # Undoubling
    assert parse_segmentation("_bake $-e +er") == ["_bak", "+er"]


def test_prefix_accommodation():
    # Doubling
    assert parse_segmentation("+un ^+p _pin") == ["+un", "_ppin"]
    # Undoubling
    assert parse_segmentation("+un ^-p _ppin") == ["+un", "_pin"]


def test_crazy():
    # Crazy segmentations seen in real output (with intentionally extreme parameters)
    assert parse_segmentation("+a ^+c +c _ess") == ["+a", "+cc", "_ess"]
    assert parse_segmentation("+a ^+b +b _ast $-t") == ["+a", "+bb", "_as"]
