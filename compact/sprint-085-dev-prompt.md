# Dev prompt — Sprint 085 / S-Auto-30 (M-Auto-7 S-X) — CS3 synthesised-null-turn provenance gate

## 1. Role identity

你是 dev agent for **Sprint 085 / S-Auto-30**, the **second autoloop launch
blocker** of milestone **M-Auto-7**. Goal: **a runtime-synthesised DISCOVER
placeholder must not be counted as a clarification round.** Layer: `infra`
(R2.a counter provenance) + `prompt_projection` (a complementary soft cue).
§7-REQUIRED.

## 2. Read order (minimal)

- `AGENTS.md` (auto-loaded).
- This prompt.
- Anchors (verified at HEAD `auto-loop-branch`, 2026-06-08):
  - `server/src/main/java/com/gumtree/csagent/model/ParsedAction.java` — `@Data @Builder` model (fields: toolCalls, userMessage, reasoning).
  - `server/src/main/java/com/gumtree/csagent/service/runtime/ActionParser.java:64-79` (null-turn fallback at :70-72) + `:120-130` (parse-failure `buildFallback`, a DIFFERENT string — do not flag).
  - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java:411` (`userMsg = action.getUserMessage()`), `:438-463` (no-tool branch + R2.a counter), `:1171-1181` (`isClarificationMessage`), `:1202-1210` (`isDiscoverFreeTextClarification`).
  - `server/src/main/resources/config/control-policy.yaml:1-2` (`max-clarification-rounds: 2` — DO NOT change).
  - `server/src/main/resources/skills/discover_triage.yaml` (procedure + Sprint-33 cue ~:19-31 — soft cue goes here).

Do NOT read `docs/sprints/*` or `docs/archive/*`.

## 3. Embedded contract

### 3.1 Background (trace-confirmed root cause)

CS3 session `33edc1eb` (`eval_interactive/results/20260607-095759/results.json`,
case `iwzx_uc_k_advert_on_hold_restore`). Turn-2 LLM raw output was
`{"user_message":"", "tool_calls":[]}` — a true null turn. `ActionParser`
substitutes the placeholder AT PARSE TIME:

```java
// ActionParser.java:70-72
if (toolCalls.isEmpty() && userMessage.isBlank()) {
    userMessage = "I'm looking into this for you.";
}
```

So `action.getUserMessage()` returns the NON-BLANK placeholder; by
`AgentRunLoopImpl.java:411` `userMsg` is non-blank, and the R2.a counter at
:455 (`isDiscoverFreeTextClarification(... userMsg)` requires only non-blank)
increments on it. Counter went 1→2, and turn-3's pre-LLM budget gate
force-escalated `clarification_budget_exhausted` before the LLM saw the
user's "Thank you." (Per-turn trace confirms: count 0→1 after the legit
turn-1 "?"-clarification, then the placeholder pushed it to 2.)

**Why the proposal's fix is wrong:** the original Option C3.A keyed off
`userMsg.isBlank()`, but `userMsg` is NEVER blank at the loop (ActionParser
already substituted). It is INERT. The fix must use a **structural provenance
flag**, NOT `userMsg.isBlank()` and NOT a content heuristic
(`isClarificationMessage`) — keeping the R2.a "structural cardinality only"
design intact.

### 3.2 What to change (Scope)

1. **`ParsedAction.java`** — add `private boolean userMessageSynthesised;`
   (Lombok generates `isUserMessageSynthesised()` + builder method; primitive
   default `false`).
2. **`ActionParser.java`** — in `parse(...)`, track whether the null-turn
   fallback fired and set the flag:
   ```java
   boolean synthesised = false;
   if (toolCalls.isEmpty() && userMessage.isBlank()) {
       log.warn("Both tool_calls and user_message empty ... using fallback message");
       userMessage = "I'm looking into this for you.";
       synthesised = true;
   }
   return ParsedAction.builder()
           .toolCalls(toolCalls)
           .userMessage(userMessage)
           .reasoning(reasoning)
           .userMessageSynthesised(synthesised)
           .build();
   ```
   Leave `buildFallback()` (:120-130) untouched (its flag stays false — it is
   the parse-failure handover apology, a different case).
3. **`AgentRunLoopImpl.java:455-457`** — exclude synthesised turns:
   ```java
   if (isDiscoverFreeTextClarification(
           plan.phase(), false, ucCommittedThisTurn, userMsg)
           && !action.isUserMessageSynthesised()) {
       session.setClarificationCount(session.getClarificationCount() + 1);
   }
   ```
   Keep everything else (the `isClarificationMessage(finalText)` routing at
   :459 is UNCHANGED). Do NOT add a content gate to the counter.
4. **Trace event** — when `action.isUserMessageSynthesised()`, surface a
   per-turn diagnostic `user_message_synthesised=true` (or a
   `discover_null_turn_synthesised` event) on the existing per-turn
   trace/diagnostic surface, for eval/admin visibility. Use the
   least-invasive existing mechanism; a one-line log is the minimum.
5. **`discover_triage.yaml`** — add ONE §1.3-soft sentence near the existing
   Sprint-33 cue: every DISCOVER turn should either call `classify_use_case`
   or ask one focused clarifying question; do not emit an empty
   `user_message` / empty `tool_calls` / placeholder filler. Soft cue only.

### 3.3 Hard fences / STOP

- No `max-clarification-rounds` change; no shared-placeholder-string edit;
  no budget-gate touch; no `isClarificationMessage`/content/keyword gate on
  the counter; no RESOLVE-intake bucket change; no Java guard on empty
  user_message.
- If excluding synthesised turns regresses a genuine over-clarification
  session (test #6), STOP and surface an OQ — do not weaken the structural
  guard.

### 3.4 Tests (all 6 mandatory)

1. ActionParser: blank user_message + no tool_calls → placeholder text AND
   `isUserMessageSynthesised()==true`.
2. AgentRunLoop: synthesised placeholder turn → `clarificationCount` NOT
   incremented.
3. AgentRunLoop: genuine `?`-clarification (non-blank, not synthesised) →
   `clarificationCount` increments (anti-误杀).
4. AgentRunLoop: non-synthesised non-blank reply → existing structural
   counting unchanged.
5. CS3 replay (3-turn DISCOVER stall) → placeholder doesn't push count to 2;
   no premature force-escalate; turn-3 reaches the LLM.
6. anti-误杀: a genuinely over-clarifying session still hits cap=2 and
   escalates.
- Focused Java suite: no new regression vs post-S-A baseline. Report counts.
- **No real-LLM re-bless** — Java/unit + diff verification only.

### 3.5 §7 / Codex

§7-REQUIRED (stanza is in `docs/sprint_objective.md`). Per-sub-sprint Codex
DEFERRED to the M-Auto-7 milestone-shared close (no §4.3 trigger). Do NOT
dispatch Codex.

### 3.6 Handoff + commit

Author `docs/sprints/sprint-085-handoff.md` (§0 evidence; §11 Codex
deferral + OQs; §12 re-bless deferred to post-S-Y1). Stage only:
`ParsedAction.java`, `ActionParser.java`, `AgentRunLoopImpl.java`,
`discover_triage.yaml`, test file(s). No `git add -A`. New commit per fix;
no `--amend` across a failed hook.

## 4. Self-check checklist

- [ ] `ParsedAction.userMessageSynthesised` added (default false).
- [ ] `ActionParser` sets the flag ONLY at the :70-72 null-turn fallback.
- [ ] Counter excludes `action.isUserMessageSynthesised()`; structural guards intact; no content gate added.
- [ ] Trace diagnostic surfaced on synthesised turns.
- [ ] discover_triage.yaml soft cue added (one sentence, §1.3-soft).
- [ ] All 6 tests green; focused Java suite no new regression (counts reported).
- [ ] control-policy / budget gate / placeholder strings / RESOLVE bucket untouched.
- [ ] Handoff authored; only authorized files staged.
