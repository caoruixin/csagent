import { useState, useEffect, type CSSProperties } from 'react';
import { getTrace, listSessions } from '../../api/client';
import type { LlmCall, Session, ToolCall, TraceResponse, TraceStep } from '../../types';

interface Props {
  sessionId: string;
  onBack: () => void;
}

export default function TraceViewer({ sessionId, onBack }: Props) {
  const [trace, setTrace] = useState<TraceResponse | null>(null);
  const [sessionMeta, setSessionMeta] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedSteps, setExpandedSteps] = useState<Set<number>>(new Set());

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getTrace(sessionId)
      .then((data) => { if (!cancelled) setTrace(data); })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load trace'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    // #4 — fetch session-level metadata (terminal handling_state +
    // escalation_reason) for the header. The /trace endpoint is per-turn and
    // carries no session disposition, so we read it from the session list.
    // Best-effort: header degrades gracefully if this fails.
    listSessions()
      .then((sessions) => {
        if (cancelled) return;
        setSessionMeta(sessions.find((s) => s.session_id === sessionId) ?? null);
      })
      .catch(() => { /* header simply omits the disposition row */ });
    return () => { cancelled = true; };
  }, [sessionId]);

  const toggleStep = (step: number) => {
    setExpandedSteps((prev) => {
      const next = new Set(prev);
      if (next.has(step)) next.delete(step);
      else next.add(step);
      return next;
    });
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <button className="btn btn-ghost" onClick={onBack} style={{ padding: '6px 12px' }}>
          &larr; Back
        </button>
        <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>
          Trace: <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{sessionId.slice(0, 12)}...</span>
        </h3>
      </div>

      {sessionMeta && <TerminalStateHeader session={sessionMeta} />}

      {loading && <div style={{ color: 'var(--color-text-muted)' }}>Loading trace...</div>}
      {error && <div style={{ color: '#B91C1C' }}>Error: {error}</div>}

      {trace && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {trace.steps.map((step) => (
            <StepCard
              key={step.step}
              step={step}
              expanded={expandedSteps.has(step.step)}
              onToggle={() => toggleStep(step.step)}
            />
          ))}
          {trace.steps.length === 0 && (
            <div style={{ color: 'var(--color-text-muted)', padding: 16 }}>No trace steps recorded.</div>
          )}
        </div>
      )}
    </div>
  );
}

// #4 — terminal handling_state colours, mirroring SessionList's StatusBadge
// so the same value reads the same way in both surfaces.
const HANDLING_STATE_BADGES: Record<string, { bg: string; text: string; label: string }> = {
  bot_handling: { bg: '#DCFCE7', text: '#166534', label: 'Bot Handling' },
  queue_to_human: { bg: '#FEF3C7', text: '#92400E', label: 'Queued to Human' },
  human_handling: { bg: '#DBEAFE', text: '#1E40AF', label: 'Human Handling' },
  closed: { bg: '#E0E7FF', text: '#3730A3', label: 'Closed' },
  active: { bg: '#DCFCE7', text: '#166534', label: 'Active' },
  escalated: { bg: '#FEF3C7', text: '#92400E', label: 'Escalated' },
  ended: { bg: '#E0E7FF', text: '#3730A3', label: 'Ended' },
};

// #4 — render the session's terminal disposition prominently at the trace
// header. handling_state answers "where did this session end up"; for
// escalations the escalation_reason enum is surfaced alongside it.
function TerminalStateHeader({ session }: { session: Session }) {
  const raw = session.handling_state || session.status || 'unknown';
  const cfg = HANDLING_STATE_BADGES[raw.toLowerCase()] ??
    { bg: 'var(--color-bg-secondary)', text: 'var(--color-text-muted)', label: raw };
  return (
    <div
      data-testid="terminal-handling-state"
      data-handling-state={raw}
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: 10,
        marginBottom: 16,
        padding: '8px 12px',
        background: 'var(--color-bg-secondary)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius)',
        fontSize: '0.85rem',
      }}
    >
      <span style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}>Terminal state:</span>
      <span
        style={{
          padding: '2px 10px',
          borderRadius: 999,
          fontWeight: 600,
          background: cfg.bg,
          color: cfg.text,
        }}
      >
        {cfg.label}
      </span>
      {session.escalation_reason && session.escalation_reason !== '' && (
        <span data-testid="escalation-reason" style={{ color: 'var(--color-text-secondary)' }}>
          escalation_reason: <code style={{ fontSize: '0.8rem' }}>{session.escalation_reason}</code>
        </span>
      )}
    </div>
  );
}

// #3 (R3.c) — INFORMATIONAL guard-rejection allow-list. Each key is a verbatim
// reject-reason constant exported by the Java guard layer; matching a tool
// call's error_message against this set lets the UI distinguish a CORRECT
// §1.4 guard rejection (the runtime holding a tool call back) from a BLOCKING
// runtime error (transport / 5xx / OOM / crash). The runtime is unchanged —
// this is a read-side display distinction only.
//
// Source (server/src/main/java/com/gumtree/csagent/service/runtime/skill/
// SkillGuardrailDispatcher.java):
//   - progressive_resolve_record_outcome_premature   → PROGRESSIVE_RESOLVE_REJECT_REASON (line 89-90)
//   - intake_required_fields_missing_for_intake_complete → INTAKE_INCOMPLETE_REJECT_REASON (line 86-87)
//   - s1_resolve_required_before_faq_miss_handover    → FAQ_MISS_REJECT_REASON (line 83-84)
//   - s1_citation_presence_required                   → S1_CITATION_PRESENCE_REQUIRED (line 92-93)
// All four are §1.4 runtime-owned guards (grounding floor / progressive
// resolve / intake completeness), i.e. correct behaviour, not crashes.
// SAFER DEFAULT: any error_message NOT in this set renders as a blocking
// error so a genuine failure is never visually downgraded.
const INFORMATIONAL_GUARD_REJECTIONS: Record<string, string> = {
  progressive_resolve_record_outcome_premature:
    'Phase ≠ CONFIRM/CLOSE — the bot tried to record a resolve outcome before the deterministic terminal condition was met. The guard correctly held it back (ResolveDispositionEvaluator §M1).',
  intake_required_fields_missing_for_intake_complete:
    'request_handover(intake_complete_for_uc_*) was submitted before all required intake fields were collected. The guard correctly held it back (SkillGuardrailDispatcher §I2).',
  s1_resolve_required_before_faq_miss_handover:
    'Handover with faq_miss_threshold_exceeded was attempted before resolve_article ran on the top search_knowledge hit. The S1 grounding guard correctly held it back (SkillGuardrailDispatcher §G2).',
  s1_citation_presence_required:
    'A factual answer was produced without a source citation. The S1 grounding-floor guard correctly required a citation (SkillGuardrailDispatcher §9.4).',
};

type RejectionKind = 'informational' | 'blocking' | null;

// Classify a tool_call's failure. A failed call whose error_message is a known
// §1.4 guard reject reason is INFORMATIONAL; any other failure is BLOCKING
// (the SAFER default). A successful call is null (neither).
function classifyToolCall(tc: ToolCall): RejectionKind {
  if (tc.success !== false) return null;
  const code = tc.error_message ?? '';
  return code && code in INFORMATIONAL_GUARD_REJECTIONS ? 'informational' : 'blocking';
}

// #2 (R3.b) — does this tool_call refer to the same dispatch (tool + args) as
// another? A1 dedup keys on tool_name + a canonical arguments hash, so a
// deduplicated repeat carries the same tool_name and arguments as its original.
function sameDispatch(a: ToolCall, b: ToolCall): boolean {
  const an = a.tool_name ?? a.tool ?? '';
  const bn = b.tool_name ?? b.tool ?? '';
  if (an !== bn) return false;
  const aa = JSON.stringify(a.arguments ?? a.args ?? {});
  const ba = JSON.stringify(b.arguments ?? b.args ?? {});
  return aa === ba;
}

interface ToolCallGroup {
  primary: ToolCall;
  dups: ToolCall[];
}

// #2 (R3.b) — fold A1-dedup repeats under their original dispatch. Walk the
// tool_calls in order; a deduplicated=true event is attached to the most
// recent matching primary (same tool + args). Defensive: a dedup event with
// no matching primary becomes its own group so no audit record is dropped.
function groupDedupToolCalls(toolCalls: ToolCall[]): ToolCallGroup[] {
  const groups: ToolCallGroup[] = [];
  for (const tc of toolCalls) {
    if (tc.deduplicated === true) {
      let attached = false;
      for (let i = groups.length - 1; i >= 0; i--) {
        if (sameDispatch(groups[i].primary, tc)) {
          groups[i].dups.push(tc);
          attached = true;
          break;
        }
      }
      if (attached) continue;
    }
    groups.push({ primary: tc, dups: [] });
  }
  return groups;
}

const jsonPreBlockStyle: CSSProperties = {
  margin: 0,
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
  fontSize: '0.78rem',
  overflowX: 'auto',
  maxHeight: 400,
  overflowY: 'auto',
  whiteSpace: 'pre',
};

function parseLlmRawResponse(raw: string | undefined): { parsed: unknown | null; parseError: boolean } {
  if (raw == null || raw === '') return { parsed: null, parseError: false };
  try {
    return { parsed: JSON.parse(raw) as unknown, parseError: false };
  } catch {
    return { parsed: null, parseError: true };
  }
}

function getReasoningText(parsed: unknown | null, parseError: boolean): string | null {
  if (parseError || parsed == null || typeof parsed !== 'object') return null;
  if (!('reasoning' in parsed)) return null;
  const v = (parsed as { reasoning?: unknown }).reasoning;
  if (v === undefined || v === null) return '';
  return typeof v === 'string' ? v : JSON.stringify(v);
}

function isEmptyRecord(obj: Record<string, unknown> | undefined): boolean {
  return obj == null || Object.keys(obj).length === 0;
}

function LlmDetailPanel({ step }: { step: TraceStep }) {
  const { parsed, parseError } = parseLlmRawResponse(step.llm_raw_response);
  const [projectedOpen, setProjectedOpen] = useState(false);
  const [rawOpen, setRawOpen] = useState(false);
  const [actionOpen, setActionOpen] = useState(false);

  const hasLlm = step.llm_raw_response != null && step.llm_raw_response !== '';
  const reasoningText = getReasoningText(parsed, parseError);
  const hasReasoningKey =
    hasLlm && !parseError && parsed !== null && typeof parsed === 'object' && 'reasoning' in parsed;
  const showNoLlm = !hasLlm;

  const rawJsonPretty = hasLlm
    ? parseError
      ? step.llm_raw_response
      : JSON.stringify(parsed, null, 2)
    : null;

  const projected = step.projected_context;
  const noProjected = projected == null || isEmptyRecord(projected);

  return (
    <div style={{ marginTop: 10, border: '1px solid var(--color-border)', borderRadius: 'var(--radius)', overflow: 'hidden' }}>
      <div style={{ padding: 12, borderBottom: '1px solid var(--color-border)' }}>
        <div
          data-testid="llm-reasoning"
          style={{
            padding: 12,
            background: '#F0F9FF',
            border: '1px solid #BAE6FD',
            borderRadius: 'var(--radius)',
          }}
        >
        {showNoLlm && (
          <span style={{ color: 'var(--color-text-secondary)' }}>No LLM call for this turn</span>
        )}
        {!showNoLlm && parseError && (
          <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
            Could not parse response as JSON. Open LLM Raw Response to inspect the raw string.
          </span>
        )}
        {!showNoLlm && !parseError && !hasReasoningKey && (
          <span style={{ color: 'var(--color-text-muted)' }}>No reasoning field in response</span>
        )}
        {!showNoLlm && !parseError && hasReasoningKey && (
          <pre
            style={{
              ...jsonPreBlockStyle,
              margin: 0,
              maxHeight: 320,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
            }}
          >
            {reasoningText === '' ? '—' : reasoningText}
          </pre>
        )}
        </div>
      </div>

      <div>
        <div
          data-testid="projected-context-toggle"
          onClick={() => setProjectedOpen((o) => !o)}
          style={{
            cursor: 'pointer',
            fontWeight: 500,
            padding: 8,
            background: 'var(--color-bg-secondary)',
            fontSize: '0.85rem',
            borderBottom: projectedOpen ? '1px solid var(--color-border)' : undefined,
          }}
        >
          {projectedOpen ? '▾ Projected Context' : '▸ Projected Context'}
        </div>
        {projectedOpen && (
          <div style={{ padding: 10, borderBottom: '1px solid var(--color-border)' }}>
            {noProjected ? (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>No projected context</span>
            ) : (
              <pre style={jsonPreBlockStyle}>{JSON.stringify(projected, null, 2)}</pre>
            )}
          </div>
        )}
      </div>

      <div>
        <div
          data-testid="llm-raw-response-toggle"
          onClick={() => setRawOpen((o) => !o)}
          style={{
            cursor: 'pointer',
            fontWeight: 500,
            padding: 8,
            background: 'var(--color-bg-secondary)',
            fontSize: '0.85rem',
            borderBottom: rawOpen ? '1px solid var(--color-border)' : undefined,
          }}
        >
          {rawOpen ? '▾ LLM Raw Response' : '▸ LLM Raw Response'}
        </div>
        {rawOpen && (
          <div style={{ padding: 10, borderBottom: '1px solid var(--color-border)' }}>
            {!hasLlm ? <span>—</span> : <pre style={jsonPreBlockStyle}>{rawJsonPretty}</pre>}
          </div>
        )}
      </div>

      {/* Sprint 51 / M5 S2 — Invocation list. Default-collapsed (per-step
          projections are heavy). Each invocation expands to its OWN
          projected context + raw response, distinct from the final-step
          summary above. */}
      {step.llm_calls && step.llm_calls.length > 0 && (
        <LlmInvocationsPanel calls={step.llm_calls} />
      )}

      {!isEmptyRecord(step.action_parameters) && (
        <div>
          <div
            data-testid="action-params-toggle"
            onClick={() => setActionOpen((o) => !o)}
            style={{
              cursor: 'pointer',
              fontWeight: 500,
              padding: 8,
              background: 'var(--color-bg-secondary)',
              fontSize: '0.85rem',
              borderBottom: actionOpen ? '1px solid var(--color-border)' : undefined,
            }}
          >
            {actionOpen ? '▾ Action Parameters' : '▸ Action Parameters'}
          </div>
          {actionOpen && (
            <div style={{ padding: 10 }}>
              <pre style={jsonPreBlockStyle}>{JSON.stringify(step.action_parameters, null, 2)}</pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function StepCard({ step, expanded, onToggle }: { step: TraceStep; expanded: boolean; onToggle: () => void }) {
  const [showLlmDetail, setShowLlmDetail] = useState(false);

  const showPhasePill = step.phase_before != null || step.phase_after != null;
  const sourceCount = step.source_ids?.length ?? 0;
  const phaseLabel =
    showPhasePill
      ? `${step.phase_before ?? '—'} \u2192 ${step.phase_after ?? '—'}`
      : '';

  return (
    <div
      style={{
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius)',
        overflow: 'hidden',
      }}
    >
      <div
        onClick={onToggle}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '10px 14px',
          cursor: 'pointer',
          background: expanded ? 'var(--color-bg-secondary)' : '#fff',
          transition: 'background 0.15s',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 24,
              height: 24,
              borderRadius: '50%',
              background: 'var(--color-primary)',
              color: '#fff',
              fontSize: '0.7rem',
              fontWeight: 600,
            }}
          >
            {step.step}
          </span>
          <span style={{ fontWeight: 500, fontSize: '0.9rem' }}>{step.action}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
            {new Date(step.timestamp).toLocaleTimeString()}
          </span>
          <span style={{ fontSize: '0.8rem', transition: 'transform 0.2s', transform: expanded ? 'rotate(180deg)' : 'rotate(0)' }}>
            &#9660;
          </span>
        </div>
      </div>

      {expanded && (
        <div style={{ padding: '14px', borderTop: '1px solid var(--color-border)', fontSize: '0.85rem' }}>
          <div style={{ marginBottom: 10 }}>
            <strong style={{ color: 'var(--color-text-secondary)' }}>Input:</strong>
            <pre style={{ margin: '4px 0 0', padding: 10, background: 'var(--color-bg-secondary)', borderRadius: 'var(--radius)', whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: '0.8rem' }}>
              {step.input}
            </pre>
          </div>
          <div style={{ marginBottom: 10 }}>
            <strong style={{ color: 'var(--color-text-secondary)' }}>Output:</strong>
            <pre style={{ margin: '4px 0 0', padding: 10, background: 'var(--color-bg-secondary)', borderRadius: 'var(--radius)', whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: '0.8rem' }}>
              {step.output}
            </pre>
          </div>
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              alignItems: 'center',
              gap: 8,
              marginBottom: 10,
              padding: '8px 10px',
              background: 'var(--color-bg-secondary)',
              borderRadius: 'var(--radius)',
              border: '1px solid var(--color-border)',
            }}
          >
            {showPhasePill && (
              <span
                style={{
                  fontSize: '0.72rem',
                  padding: '2px 8px',
                  borderRadius: 999,
                  background: '#fff',
                  border: '1px solid var(--color-border)',
                  color: 'var(--color-text-secondary)',
                }}
              >
                {phaseLabel}
              </span>
            )}
            {step.active_use_case != null && step.active_use_case !== '' && (
              <span
                style={{
                  fontSize: '0.72rem',
                  padding: '2px 8px',
                  borderRadius: 999,
                  background: 'var(--color-primary)',
                  color: '#fff',
                  fontWeight: 500,
                }}
              >
                {step.active_use_case}
              </span>
            )}
            {step.latency_ms != null && (
              <span
                style={{
                  fontSize: '0.72rem',
                  padding: '2px 8px',
                  borderRadius: 999,
                  background: '#fff',
                  border: '1px solid var(--color-border)',
                  color: 'var(--color-text-secondary)',
                }}
              >
                {step.latency_ms}ms
              </span>
            )}
            <span
              style={{
                fontSize: '0.72rem',
                padding: '2px 8px',
                borderRadius: 999,
                background: '#fff',
                border: '1px solid var(--color-border)',
                color: 'var(--color-text-secondary)',
              }}
            >
              {sourceCount} sources
            </span>
          </div>
          <div style={{ marginBottom: 10 }}>
            <button
              type="button"
              className="btn btn-ghost"
              data-testid="llm-detail-btn"
              onClick={(e) => {
                e.stopPropagation();
                setShowLlmDetail((v) => !v);
              }}
              style={{ padding: '6px 12px', fontSize: '0.8rem' }}
            >
              {showLlmDetail ? 'Hide LLM Detail' : 'View LLM Detail'}
            </button>
          </div>
          {showLlmDetail && <LlmDetailPanel step={step} />}

          {step.tool_calls && step.tool_calls.length > 0 && (
            <ToolCallsSection toolCalls={step.tool_calls} />
          )}
        </div>
      )}
    </div>
  );
}

// R3.b + R3.c — tool-calls section. Folds A1-dedup repeats under their
// original dispatch (#2) and renders each call with rejection classification
// (#3). Audit is never lost: dedup repeats and informational-rejection detail
// are both reachable by expanding.
function ToolCallsSection({ toolCalls }: { toolCalls: ToolCall[] }) {
  const groups = groupDedupToolCalls(toolCalls);
  return (
    <div>
      <strong style={{ color: 'var(--color-text-secondary)' }}>Tool Calls:</strong>
      {groups.map((g, i) => (
        <div key={i}>
          <ToolCallRow tc={g.primary} />
          {g.dups.length > 0 && <DedupRepeats dups={g.dups} />}
        </div>
      ))}
    </div>
  );
}

// R3.b — folded indicator for A1-dedup repeats served from the per-run cache.
// Collapsed by default to keep the trace readable; click expands the full list
// (step_index + original_at_step references) so the dedup audit is preserved.
function DedupRepeats({ dups }: { dups: ToolCall[] }) {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ margin: '0 0 6px 16px' }}>
      <div
        data-testid="dedup-indicator"
        onClick={() => setOpen((o) => !o)}
        style={{
          cursor: 'pointer',
          fontSize: '0.75rem',
          color: 'var(--color-text-muted)',
          padding: '2px 0',
          userSelect: 'none',
        }}
      >
        {open ? '▾' : '▸'} ↳ {dups.length} dedup&apos;d repeat{dups.length > 1 ? 's' : ''} (served from per-run cache)
      </div>
      {open && (
        <div data-testid="dedup-expanded" style={{ paddingLeft: 12 }}>
          {dups.map((d, i) => (
            <div
              key={i}
              data-testid="dedup-repeat-row"
              style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', padding: '1px 0' }}
            >
              step {d.step_index ?? '?'} · served from cache (original_at_step={d.original_at_step ?? '?'}) · {d.latency_ms ?? 0}ms
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// R3.b + R3.c — a single tool_call row. A successful or BLOCKING-error call
// keeps the original inline rendering (blue / red). An INFORMATIONAL guard
// rejection gets a distinct yellow badge and is collapsed by default; expand
// reveals the rejection-code string, the guard rationale, and the full
// args/result payload (no audit loss).
function ToolCallRow({ tc }: { tc: ToolCall }) {
  const kind = classifyToolCall(tc);
  const [open, setOpen] = useState(false);

  // Sprint 9 §O2 — tolerate legacy rows that ship only {tool, args, result}
  // as well as the new backend shape.
  const toolName = tc.tool_name ?? tc.tool ?? '(unknown tool)';
  const args = tc.arguments ?? tc.args ?? {};
  const success = tc.success;
  const errorMessage = tc.error_message;
  const resultData = tc.result_data ?? tc.result;
  const resultSummary = tc.result_summary;
  const latencyMs = tc.latency_ms;
  const hasResultPayload =
    errorMessage != null ||
    (resultSummary != null && resultSummary !== '') ||
    (resultData != null && !(typeof resultData === 'object' && Object.keys(resultData as Record<string, unknown>).length === 0));

  if (kind === 'informational') {
    const rationale = INFORMATIONAL_GUARD_REJECTIONS[errorMessage ?? ''] ?? '';
    return (
      <div
        data-testid="tool-call-row"
        data-rejection-kind="informational"
        style={{
          margin: '6px 0',
          padding: 10,
          background: '#FEFCE8',
          borderRadius: 'var(--radius)',
          border: '1px solid #FDE68A',
        }}
      >
        <div
          onClick={() => setOpen((o) => !o)}
          style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', userSelect: 'none' }}
        >
          <span style={{ fontWeight: 500, color: '#92400E' }}>{toolName}</span>
          <span
            data-testid="informational-guard-badge"
            style={{ fontSize: '0.7rem', padding: '1px 6px', borderRadius: 999, background: '#FEF3C7', color: '#92400E' }}
          >
            Informational guard rejection
          </span>
          {latencyMs != null && (
            <span style={{ fontSize: '0.7rem', color: 'var(--color-text-muted)' }}>{latencyMs}ms</span>
          )}
          <span style={{ marginLeft: 'auto', fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
            {open ? '▾' : '▸'}
          </span>
        </div>
        {open && (
          <div data-testid="tool-call-detail" style={{ marginTop: 6 }}>
            <div data-testid="rejection-code" style={{ fontSize: '0.8rem', color: '#92400E', fontFamily: 'monospace' }}>
              {errorMessage}
            </div>
            {rationale !== '' && (
              <div data-testid="rejection-rationale" style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: 2 }}>
                {rationale}
              </div>
            )}
            <pre style={{ margin: '6px 0 0', fontSize: '0.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
              {JSON.stringify(args, null, 2)}
            </pre>
            {resultSummary != null && resultSummary !== '' && (
              <div data-testid="tool-call-summary" style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: 2 }}>
                {resultSummary}
              </div>
            )}
            {resultData != null && (
              <pre style={{ margin: 0, fontSize: '0.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {JSON.stringify(resultData, null, 2)}
              </pre>
            )}
          </div>
        )}
      </div>
    );
  }

  // Successful call or BLOCKING runtime error — original inline rendering,
  // unchanged (red for blocking errors, blue for success). The SAFER default:
  // any unrecognised failure lands here, never visually downgraded.
  return (
    <div
      data-testid="tool-call-row"
      data-rejection-kind={kind ?? 'ok'}
      style={{
        margin: '6px 0',
        padding: 10,
        background: success === false ? '#FEF2F2' : '#EFF6FF',
        borderRadius: 'var(--radius)',
        border: success === false ? '1px solid #FECACA' : '1px solid #BFDBFE',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
        <span style={{ fontWeight: 500, color: success === false ? '#991B1B' : '#1E40AF' }}>{toolName}</span>
        {success === true && (
          <span style={{ fontSize: '0.7rem', padding: '1px 6px', borderRadius: 999, background: '#DCFCE7', color: '#166534' }}>ok</span>
        )}
        {success === false && (
          <span style={{ fontSize: '0.7rem', padding: '1px 6px', borderRadius: 999, background: '#FEE2E2', color: '#991B1B' }}>error</span>
        )}
        {latencyMs != null && (
          <span style={{ fontSize: '0.7rem', color: 'var(--color-text-muted)' }}>{latencyMs}ms</span>
        )}
        {tc.source != null && tc.source !== '' && (
          <span style={{ fontSize: '0.7rem', color: 'var(--color-text-muted)' }}>[{tc.source}]</span>
        )}
      </div>
      <pre style={{ margin: 0, fontSize: '0.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
        {JSON.stringify(args, null, 2)}
      </pre>
      <div style={{ marginTop: 6, fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Result:</div>
      {errorMessage != null && (
        <div data-testid="tool-call-error" style={{ fontSize: '0.8rem', color: '#991B1B', marginTop: 2 }}>
          {errorMessage}
        </div>
      )}
      {resultSummary != null && resultSummary !== '' && (
        <div data-testid="tool-call-summary" style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: 2 }}>
          {resultSummary}
        </div>
      )}
      {resultData != null && (
        <pre style={{ margin: 0, fontSize: '0.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
          {JSON.stringify(resultData, null, 2)}
        </pre>
      )}
      {!hasResultPayload && (
        <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>—</span>
      )}
    </div>
  );
}

// Sprint 51 / M5 S2 — LlmInvocationsPanel: one row per LLM call inside the
// AgentRunLoop step boundary for this turn. The base LlmDetailPanel above
// keeps showing the FINAL step's value (BotTurn's single column); this panel
// surfaces every step so the human can see the full multi-step exchange.
// Default-collapsed (per-step projections are heavy).
function LlmInvocationsPanel({ calls }: { calls: LlmCall[] }) {
  const [open, setOpen] = useState(false);
  return (
    <div
      style={{
        marginTop: 10,
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius)',
        overflow: 'hidden',
      }}
    >
      <div
        data-testid="llm-invocations-toggle"
        onClick={() => setOpen((o) => !o)}
        style={{
          cursor: 'pointer',
          fontWeight: 500,
          padding: 8,
          background: 'var(--color-bg-secondary)',
          fontSize: '0.85rem',
          borderBottom: open ? '1px solid var(--color-border)' : undefined,
        }}
      >
        {open ? '▾' : '▸'} LLM Invocations ({calls.length})
      </div>
      {open && (
        <div style={{ padding: 10 }} data-testid="llm-invocations-list">
          {calls.map((c, i) => (
            <LlmInvocationCard key={c.id ?? `${c.step_index}-${i}`} call={c} index={i} />
          ))}
        </div>
      )}
    </div>
  );
}

function LlmInvocationCard({ call, index }: { call: LlmCall; index: number }) {
  const [projOpen, setProjOpen] = useState(false);
  const [rawOpen, setRawOpen] = useState(false);
  const [toolsOpen, setToolsOpen] = useState(false);

  const parsedProjection = (() => {
    if (!call.projected_context) return null;
    try {
      return JSON.parse(call.projected_context) as unknown;
    } catch {
      return call.projected_context;
    }
  })();
  const parsedRaw = (() => {
    if (!call.llm_raw_response) return null;
    try {
      return JSON.stringify(JSON.parse(call.llm_raw_response), null, 2);
    } catch {
      return call.llm_raw_response;
    }
  })();
  const parsedTools = (() => {
    if (!call.tool_calls) return null;
    try {
      return JSON.parse(call.tool_calls) as unknown;
    } catch {
      return call.tool_calls;
    }
  })();

  return (
    <div
      data-testid="llm-invocation-card"
      style={{
        marginBottom: 8,
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius)',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 8,
          padding: '8px 10px',
          background: '#F9FAFB',
          alignItems: 'center',
          fontSize: '0.82rem',
        }}
      >
        <span style={{ fontWeight: 600 }}>Invocation {index + 1}</span>
        <span
          style={{
            padding: '1px 6px',
            borderRadius: 999,
            background: '#fff',
            border: '1px solid var(--color-border)',
            color: 'var(--color-text-secondary)',
            fontSize: '0.72rem',
          }}
        >
          step {call.step_index}
        </span>
        <span
          style={{
            padding: '1px 6px',
            borderRadius: 999,
            background:
              call.call_type === 'routing'
                ? '#FEF3C7'
                : call.call_type === 'rerank'
                  ? '#E0E7FF'
                  : '#DCFCE7',
            color:
              call.call_type === 'routing'
                ? '#92400E'
                : call.call_type === 'rerank'
                  ? '#3730A3'
                  : '#166534',
            fontSize: '0.72rem',
            fontWeight: 500,
          }}
        >
          {call.call_type}
        </span>
        {call.model != null && call.model !== '' && (
          <span style={{ fontSize: '0.72rem', color: 'var(--color-text-muted)' }}>
            {call.model}
          </span>
        )}
        {call.latency_ms != null && (
          <span style={{ fontSize: '0.72rem', color: 'var(--color-text-muted)' }}>
            {call.latency_ms}ms
          </span>
        )}
      </div>

      <div
        onClick={() => setProjOpen((o) => !o)}
        style={{
          cursor: 'pointer',
          padding: 6,
          background: 'var(--color-bg-secondary)',
          fontSize: '0.78rem',
          borderTop: '1px solid var(--color-border)',
        }}
      >
        {projOpen ? '▾ Projected Context (this step)' : '▸ Projected Context (this step)'}
      </div>
      {projOpen && (
        <div style={{ padding: 8 }}>
          {parsedProjection == null ? (
            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>—</span>
          ) : (
            <pre style={jsonPreBlockStyle}>
              {typeof parsedProjection === 'string'
                ? parsedProjection
                : JSON.stringify(parsedProjection, null, 2)}
            </pre>
          )}
        </div>
      )}

      <div
        onClick={() => setRawOpen((o) => !o)}
        style={{
          cursor: 'pointer',
          padding: 6,
          background: 'var(--color-bg-secondary)',
          fontSize: '0.78rem',
          borderTop: '1px solid var(--color-border)',
        }}
      >
        {rawOpen ? '▾ LLM Raw Response (this step)' : '▸ LLM Raw Response (this step)'}
      </div>
      {rawOpen && (
        <div style={{ padding: 8 }}>
          {parsedRaw == null ? (
            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>—</span>
          ) : (
            <pre style={jsonPreBlockStyle}>{parsedRaw}</pre>
          )}
        </div>
      )}

      {parsedTools != null && (
        <>
          <div
            onClick={() => setToolsOpen((o) => !o)}
            style={{
              cursor: 'pointer',
              padding: 6,
              background: 'var(--color-bg-secondary)',
              fontSize: '0.78rem',
              borderTop: '1px solid var(--color-border)',
            }}
          >
            {toolsOpen ? '▾ Tool Calls (this step)' : '▸ Tool Calls (this step)'}
          </div>
          {toolsOpen && (
            <div style={{ padding: 8 }}>
              <pre style={jsonPreBlockStyle}>
                {typeof parsedTools === 'string'
                  ? parsedTools
                  : JSON.stringify(parsedTools, null, 2)}
              </pre>
            </div>
          )}
        </>
      )}
    </div>
  );
}
