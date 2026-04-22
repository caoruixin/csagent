import { useState, useEffect } from 'react';
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

function StepCard({ step, expanded, onToggle }: { step: TraceStep; expanded: boolean; onToggle: () => void }) {
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
