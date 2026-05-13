## Sprint Review Decision
decision: fix_required
blocking_count: 2
summary: No forbidden semantic hardcode was found in the Sprint 20 diff: Track A stays on eval-spec authoring, and Track B adds an observability-only `already_called` projection slot without prompt or dispatch enforcement. Sprint closure still requires fixes because Track B's regression evidence does not actually test runtime non-enforcement or the required cs_011 T2 target coverage, and the shadow-split access-boundary documentation claims a non-existent `--include-shadow` runner gate while the handoff says that flag was not implemented.

### Finding 1: Track B runtime non-enforcement / cs_011 target coverage is not actually tested

- file path: `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledProjectionTest.java:38`
- diff snippet:

```java
 * <p>This test covers the three sprint-objective behaviour bars at the
 * {@link ContextProjectionBuilder} layer (projection-assembly unit). The
 * "runtime does not short-circuit" bar is verified by inspecting the
 * {@link AgentRunLoopImpl} dispatch path: the slot is read in
 * {@code build(...)} only; {@code AgentRunLoopImpl.run} passes
 * {@code toolEvents} into the new {@code build(...)} overload but does
 * not consult the slot for dispatch decisions. The bar is therefore
 * verified here by demonstrating that two identical-args calls in a row
 * both produce {@link ToolEvent}s in the input list, regardless of
 * whether the slot is populated.
```

- file path: `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledProjectionTest.java:183`
- diff snippet:

```java
void alreadyCalled_includesEveryRepeatedSuccessfulDispatch() throws Exception {
    ...
    ToolEvent step0 = ToolEvent.of(0, call, ok, 1200L);
    ToolEvent step1 = ToolEvent.of(1, call, ok, 1180L);

    String projection = builder.build(
            session, List.of(), plan, "why can't I see my advert", null,
            List.of(step0, step1));
```

- file path: `docs/sprints/sprint-020-handoff.md:681`
- diff snippet:

```markdown
8. **Generalization eval coverage (target / neighbor / negative /
   shadow)?** Track A IS the coverage deliverable for downstream
   sprints — see §9 below. Track B's coverage: target = the
   manual-probe trace shape; neighbor = any multi-tool-call turn from
   the smoke suite; negative = the
   `alreadyCalled_excludesUnsuccessfulEvents` and
   `alreadyCalled_emptyArrayOnFirstStep` tests; shadow = deferred
```

- failing §4.1 question: Q8
- §3.1 layer: `prompt_projection`
- severity: blocking
- reasoning: The claimed bar (c) constructs two successful `ToolEvent`s by hand and calls `ContextProjectionBuilder`; it does not execute `AgentRunLoopImpl.run(...)`, does not verify two identical LLM-emitted calls reach `ToolDispatcher.dispatch(...)`, and would not fail if a future runtime short-circuit were added before event creation. The handoff also names only the manual-probe trace shape for Track B target coverage and omits the required cs_011 T2 target from the review prompt. Track B's slot is still a soft signal, not a hardcode, but the required regression coverage is incomplete.

### Finding 2: Shadow access boundary claims an `--include-shadow` runner gate that was not implemented

- file path: `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md:23`
- diff snippet:

```markdown
| Eval-harness runner (default invocation) | yes | no |
| Eval-harness runner (with `--include-shadow`) | yes | yes |
...
2. **Runner gate.** The `eval-interactive run` CLI command does not
   load shadow CaseSpecs unless `--include-shadow` is passed
   explicitly. The flag is intended for the human and the review
   agent. Sprint 20 does not add the flag to any documented dev-agent
   workflow.
```

- file path: `docs/sprints/sprint-020-handoff.md:444`
- diff snippet:

```markdown
Sprint 20 does **not** add a `--include-shadow` flag to the CLI; the
runner-flag gate is documented in `_ACCESS_BOUNDARY.md` as a v1
hardening direction, and the v0 mechanism relies on the directory
boundary + custom-path-only loading for shadow access.
```

- failing §4.1 question: Q8
- §3.1 layer: `infra`
- severity: blocking
- reasoning: Track A's family counts are present, but the shadow-split enforcement story is internally inconsistent. The committed access-boundary file describes `--include-shadow` as an active gate, while the handoff states that Sprint 20 did not add that flag. As committed, human/review access and dev-agent access both rely on the same custom-path mechanism plus documented self-restraint, so the claimed runner gate is not an implemented v0 enforcement boundary.
