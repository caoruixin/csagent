"""autoloop.scoring — fitness evaluation surface for the auto-loop.

S-Auto-2 deliverable. Three modules:

- `tier_evaluator` — the 5-layer lexicographic verdict.
- `eval_runner` — subprocess wrapper around `eval-interactive run`.
- `baseline_loader` — frozen baseline snapshot reader.

The public surface re-exported here is what S-Auto-3 wires into the
loop orchestrator. New names go through this `__init__.py` so the
import surface stays stable across sub-sprints.
"""

from .baseline_loader import (
    BaselineLoadError,
    BaselineSnapshot,
    SuiteSnapshot,
    load,
)
from .eval_runner import (
    EvalRunnerConfigError,
    EvalRunnerTimeoutError,
    SuiteRunResult,
    SuiteRunSpec,
    run_suite,
    run_v1_fitness_suite,
)
from .gaming import GamingFlag, detect
from .tier_evaluator import (
    LayerResult,
    LexicographicVerdict,
    ShadowAuditDetail,
    evaluate,
)

__all__ = [
    "BaselineLoadError",
    "BaselineSnapshot",
    "EvalRunnerConfigError",
    "EvalRunnerTimeoutError",
    "GamingFlag",
    "LayerResult",
    "LexicographicVerdict",
    "ShadowAuditDetail",
    "SuiteRunResult",
    "SuiteRunSpec",
    "SuiteSnapshot",
    "detect",
    "evaluate",
    "load",
    "run_suite",
    "run_v1_fitness_suite",
]
