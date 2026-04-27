import { useState, useEffect, useMemo } from 'react';
import { listSessions } from '../../api/client';
import type { Session } from '../../types';

type SortKey = 'session_id' | 'use_case' | 'status' | 'outcome' | 'turns' | 'created_at';
type SortDir = 'asc' | 'desc';

interface Props {
  onSelectSession: (sessionId: string) => void;
}

const COLUMNS: { key: SortKey; label: string; align?: 'center' }[] = [
  { key: 'session_id', label: 'Session ID' },
  { key: 'use_case', label: 'Use Case' },
  { key: 'status', label: 'Status' },
  { key: 'outcome', label: 'Outcome' },
  { key: 'turns', label: 'Turns', align: 'center' },
  { key: 'created_at', label: 'Created' },
];

function compareSessions(a: Session, b: Session, key: SortKey, dir: SortDir): number {
  let av: string | number;
  let bv: string | number;

  switch (key) {
    case 'turns':
      av = a.turns ?? 0;
      bv = b.turns ?? 0;
      break;
    case 'created_at':
      av = new Date(a.created_at).getTime();
      bv = new Date(b.created_at).getTime();
      break;
    default:
      av = (a[key] ?? '').toString().toLowerCase();
      bv = (b[key] ?? '').toString().toLowerCase();
  }

  if (av < bv) return dir === 'asc' ? -1 : 1;
  if (av > bv) return dir === 'asc' ? 1 : -1;
  return 0;
}

export default function SessionList({ onSelectSession }: Props) {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortKey, setSortKey] = useState<SortKey>('created_at');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

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

  const sorted = useMemo(
    () => [...sessions].sort((a, b) => compareSessions(a, b, sortKey, sortDir)),
    [sessions, sortKey, sortDir],
  );

  const handleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('desc');
    }
  };

  if (loading) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>Loading sessions...</div>;
  if (error) return <div style={{ padding: 24, color: '#B91C1C' }}>Error: {error}</div>;
  if (sessions.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>No sessions found.</div>;

  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid var(--color-border)' }}>
            {COLUMNS.map((col) => (
              <th
                key={col.key}
                onClick={() => handleSort(col.key)}
                style={{
                  textAlign: col.align === 'center' ? 'center' : 'left',
                  padding: '10px 12px',
                  fontWeight: 600,
                  color: sortKey === col.key ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                  whiteSpace: 'nowrap',
                  cursor: 'pointer',
                  userSelect: 'none',
                  transition: 'color 0.15s',
                }}
              >
                {col.label}
                {sortKey === col.key ? (
                  <span style={{ marginLeft: 4, fontSize: '0.7rem' }}>
                    {sortDir === 'asc' ? ' \u25b2' : ' \u25bc'}
                  </span>
                ) : (
                  <span style={{ marginLeft: 4, fontSize: '0.7rem', opacity: 0.3 }}>
                    {' \u25bc'}
                  </span>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map((s) => (
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
