"""S-Y1.5c — defensive guard for the propose.txt clarification.

The "# Acceptable patterns" section added to `prompts/propose.txt`
explains to the proposer that few-shot illustrative examples,
principle-level narrative, sequence narration, and Tool/enum surface
legibility are NOT §1.7 rule dumps. Without this guard a future edit
could silently drop the clarification, re-introducing the
over-conservative proposer behaviour S-Y1.5c set out to fix.
"""

from __future__ import annotations

from autoloop.meta_agent.proposer import _PROMPT_PATH


def test_propose_txt_contains_acceptable_patterns_section():
    text = _PROMPT_PATH.read_text(encoding="utf-8")
    assert "# Acceptable patterns" in text, (
        "propose.txt lost its '# Acceptable patterns' header"
    )
    assert "Few-shot illustrative examples" in text, (
        "propose.txt lost the few-shot illustrative-examples guidance"
    )
