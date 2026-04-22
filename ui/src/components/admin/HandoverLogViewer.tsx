import { useState, useEffect } from 'react';
import { getHandoverLogs } from '../../api/client';
import type { HandoverLog } from '../../types';

export default function HandoverLogViewer() {
  const [logs, setLogs] = useState<HandoverLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getHandoverLogs()
      .then((data) => { if (!cancelled) setLogs(data); })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load handover logs'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  if (loading) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>Loading handover logs...</div>;
  if (error) return <div style={{ padding: 24, color: '#B91C1C' }}>Error: {error}</div>;
  if (logs.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>No handover logs found.</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {logs.map((log) => {
        const isExpanded = expandedId === log.log_id;
        return (
          <div
            key={log.log_id}
            style={{
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius)',
              overflow: 'hidden',
            }}
          >
            <div
              onClick={() => setExpandedId(isExpanded ? null : log.log_id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 16px',
                cursor: 'pointer',
                background: isExpanded ? 'var(--color-bg-secondary)' : '#fff',
                transition: 'background 0.15s',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <PriorityBadge priority={log.priority} />
                <span style={{ fontWeight: 500, fontSize: '0.9rem' }}>
                  Case {log.case_number}
                </span>
                <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                  {log.reason}
                </span>
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                {new Date(log.created_at).toLocaleString()}
              </span>
            </div>

            {isExpanded && (
              <div style={{ padding: 16, borderTop: '1px solid var(--color-border)' }}>
                <div style={{ marginBottom: 10, fontSize: '0.85rem' }}>
                  <strong>Session:</strong>{' '}
                  <span style={{ fontFamily: 'monospace' }}>{log.session_id}</span>
                </div>
                <div style={{ fontSize: '0.8rem', fontWeight: 500, marginBottom: 6, color: 'var(--color-text-secondary)' }}>
                  Payload:
                </div>
                <pre
                  style={{
                    background: 'var(--color-bg-secondary)',
                    padding: 12,
                    borderRadius: 'var(--radius)',
                    fontSize: '0.8rem',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    maxHeight: 400,
                    overflow: 'auto',
                    margin: 0,
                  }}
                >
                  {JSON.stringify(log.payload, null, 2)}
                </pre>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

function PriorityBadge({ priority }: { priority: string }) {
  const colors: Record<string, { bg: string; text: string }> = {
    high: { bg: '#FEE2E2', text: '#991B1B' },
    medium: { bg: '#FEF3C7', text: '#92400E' },
    low: { bg: '#DCFCE7', text: '#166534' },
  };
  const c = colors[priority?.toLowerCase()] ?? { bg: 'var(--color-bg-secondary)', text: 'var(--color-text-muted)' };

  return (
    <span
      style={{
        padding: '2px 8px',
        borderRadius: 12,
        fontSize: '0.7rem',
        fontWeight: 600,
        textTransform: 'uppercase',
        background: c.bg,
        color: c.text,
      }}
    >
      {priority}
    </span>
  );
}
