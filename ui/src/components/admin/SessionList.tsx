import { useState, useEffect } from 'react';
import { listSessions } from '../../api/client';
import type { Session } from '../../types';

interface Props {
  onSelectSession: (sessionId: string) => void;
}

export default function SessionList({ onSelectSession }: Props) {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listSessions()
      .then((data) => {
        if (!cancelled) setSessions(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load sessions');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  if (loading) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>Loading sessions...</div>;
  if (error) return <div style={{ padding: 24, color: '#B91C1C' }}>Error: {error}</div>;
  if (sessions.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>No sessions found.</div>;

  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid var(--color-border)' }}>
            {['Session ID', 'Use Case', 'Status', 'Outcome', 'Turns', 'Created'].map((h) => (
              <th
                key={h}
                style={{
                  textAlign: 'left',
                  padding: '10px 12px',
                  fontWeight: 600,
                  color: 'var(--color-text-secondary)',
                  whiteSpace: 'nowrap',
                }}
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sessions.map((s) => (
            <tr
              key={s.session_id}
              onClick={() => onSelectSession(s.session_id)}
              style={{
                borderBottom: '1px solid var(--color-border)',
                cursor: 'pointer',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.background = 'var(--color-bg-secondary)'; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
            >
              <td style={{ padding: '10px 12px', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                {s.session_id.slice(0, 12)}...
              </td>
              <td style={{ padding: '10px 12px' }}>{s.use_case || '-'}</td>
              <td style={{ padding: '10px 12px' }}>
                <StatusBadge status={s.status} />
              </td>
              <td style={{ padding: '10px 12px' }}>{s.outcome || '-'}</td>
              <td style={{ padding: '10px 12px', textAlign: 'center' }}>{s.turns}</td>
              <td style={{ padding: '10px 12px', whiteSpace: 'nowrap' }}>
                {new Date(s.created_at).toLocaleString()}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, { bg: string; text: string }> = {
    active: { bg: '#DCFCE7', text: '#166534' },
    ended: { bg: '#E0E7FF', text: '#3730A3' },
    escalated: { bg: '#FEF3C7', text: '#92400E' },
  };
  const c = colors[status?.toLowerCase()] ?? { bg: 'var(--color-bg-secondary)', text: 'var(--color-text-muted)' };

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: 12,
        fontSize: '0.75rem',
        fontWeight: 500,
        background: c.bg,
        color: c.text,
      }}
    >
      {status || 'unknown'}
    </span>
  );
}
