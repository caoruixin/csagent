import { useState, useEffect, type CSSProperties } from 'react';
import { getTrace } from '../../api/client';
import type { TraceResponse, TraceStep } from '../../types';

interface Props {
  sessionId: string;
  onBack: () => void;
}

export default function TraceViewer({ sessionId, onBack }: Props) {
  const [trace, setTrace] = useState<TraceResponse | null>(null);
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
            <div>
              <strong style={{ color: 'var(--color-text-secondary)' }}>Tool Calls:</strong>
              {step.tool_calls.map((tc, i) => (
                <div
                  key={i}
                  style={{
                    margin: '6px 0',
                    padding: 10,
                    background: '#EFF6FF',
                    borderRadius: 'var(--radius)',
                    border: '1px solid #BFDBFE',
                  }}
                >
                  <div style={{ fontWeight: 500, marginBottom: 4, color: '#1E40AF' }}>{tc.tool}</div>
                  <pre style={{ margin: 0, fontSize: '0.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {JSON.stringify(tc.args, null, 2)}
                  </pre>
                  <div style={{ marginTop: 6, fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Result:</div>
                  <pre style={{ margin: 0, fontSize: '0.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                    {JSON.stringify(tc.result, null, 2)}
                  </pre>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
