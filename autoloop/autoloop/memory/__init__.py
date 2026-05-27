"""autoloop.memory — three-layer persistence for the auto-evolution loop.

S-Auto-3 deliverable. Three modules form the durable memory of the
loop per `docs/solutions/auto_evolution_skill_driven_v1.md` §3.4:

- `experiments_log` — append-only JSONL, one row per iteration.
- `iterations_index` — sqlite index for fingerprint / target lookups.
- `lessons_log` — markdown lessons file, compacted every K iterations.

Layered design (verbatim from the proposal):

    Layer A (experiments_log.jsonl) — raw audit trail; never queried hot.
    Layer B (iterations.sqlite)     — random-access lookup by fingerprint
                                       / target / decision for proposer
                                       anti-repeat + audit.
    Layer C (lessons.md)            — LLM-compacted prose patterns; fed
                                       back into propose-prompt.

All three are append-only at the iteration boundary. The loop never
mutates a past row; corrections are recorded as new rows that
reference the parent_iteration_id.
"""

from .experiments_log import (
    append as experiments_log_append,
    read_all as experiments_log_read_all,
    read_recent as experiments_log_read_recent,
)
from .iterations_index import (
    IterationRecord,
    init_db,
    insert as iterations_index_insert,
    query_by_fingerprint,
    query_by_target,
    query_recent as iterations_index_query_recent,
)
from .lessons_log import (
    append_lesson,
    count_lessons,
    read_all as lessons_log_read_all,
)

__all__ = [
    "IterationRecord",
    "append_lesson",
    "count_lessons",
    "experiments_log_append",
    "experiments_log_read_all",
    "experiments_log_read_recent",
    "init_db",
    "iterations_index_insert",
    "iterations_index_query_recent",
    "lessons_log_read_all",
    "query_by_fingerprint",
    "query_by_target",
]
