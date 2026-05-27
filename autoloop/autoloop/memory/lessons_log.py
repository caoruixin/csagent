"""Markdown lessons file — Layer C of the three-layer memory.

The lessons file is the LLM-compacted prose of "what propose
patterns worked / what failed". The lessons_compactor produces one
new `## Lesson L-...` H2 section every K iterations; the proposer
reads the whole file (small, K≪1000) as a prompt input.

File format conventions:

    # Lessons (autoloop, M-Auto-1A)

    ## Lesson L-2026-05-28-001
    **Window**: iterations exp-1 through exp-10 (K=10)
    **Target observation**: ...
    **Pattern**: ...
    **Heuristic for future propose**: ...
    **Affected Skill × field**: ...

    ---

    ## Lesson L-2026-05-28-002
    ...

Sections are separated by `---` lines. The file starts empty (one
H1) when the autoloop is first installed. The compactor never
edits a prior section — corrections are appended as new sections
that name the prior section in their body.
"""

from __future__ import annotations

from pathlib import Path


_FILE_HEADER = "# Lessons (autoloop, M-Auto-1A)\n"


def read_all(lessons_path: Path) -> str:
    """Return the file contents verbatim.

    The proposer feeds this whole string into propose.txt (small;
    bounded by K × ~300 tokens per lesson). When the file does not
    exist yet, returns an empty header — the proposer is robust to
    "no lessons yet" via the prompt copy.
    """
    lessons_path = Path(lessons_path)
    if not lessons_path.exists():
        return _FILE_HEADER
    return lessons_path.read_text(encoding="utf-8")


def append_lesson(lessons_path: Path, lesson_md: str) -> None:
    """Append a `## Lesson ...` section to the file.

    Adds a `---` divider before the new section if the file is not
    empty. Creates the parent directory + file header on first call.
    """
    lessons_path = Path(lessons_path)
    lessons_path.parent.mkdir(parents=True, exist_ok=True)

    body = lesson_md.strip("\n")
    if not lessons_path.exists():
        lessons_path.write_text(_FILE_HEADER + "\n" + body + "\n", encoding="utf-8")
        return

    existing = lessons_path.read_text(encoding="utf-8").rstrip("\n")
    glue = "\n\n---\n\n"
    new_content = existing + glue + body + "\n"
    lessons_path.write_text(new_content, encoding="utf-8")


def count_lessons(lessons_path: Path) -> int:
    """Return the number of `## Lesson` H2 sections in the file."""
    lessons_path = Path(lessons_path)
    if not lessons_path.exists():
        return 0
    text = lessons_path.read_text(encoding="utf-8")
    n = 0
    for line in text.splitlines():
        if line.startswith("## Lesson "):
            n += 1
    return n
