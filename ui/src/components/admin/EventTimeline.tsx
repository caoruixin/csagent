import { useState, useEffect } from 'react';
import { getEvents, listSessions } from '../../api/client';
import type { SessionEvent, Session } from '../../types';

export default function EventTimeline() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [selectedSession, setSelectedSession] = useState<string>('');
  const [events, setEvents] = useState<SessionEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listSessions()
      .then(setSessions)
      .catch(() => {/* ignore */});
  }, []);

  useEffect(() => {
    if (!selectedSession) {
      setEvents([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    getEvents(selectedSession)
      .then((data) => { if (!cancelled) setEvents(data); })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load events'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [selectedSession]);

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
        <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--color-text-secondary)' }}>
          Session:
        </label>
        <select
          value={selectedSession}
          onChange={(e) => setSelectedSession(e.target.value)}
          style={{ padding: '8px 12px', minWidth: 240 }}
        >
          <option value="">Select a session...</option>
          {sessions.map((s) => (
            <option key={s.session_id} value={s.session_id}>
              {s.session_id.slice(0, 12)}... ({s.topic_subject})
            </option>
          ))}
        </select>
      </div>

      {loading && <div style={{ color: 'var(--color-text-muted)' }}>Loading events...</div>}
      {error && <div style={{ color: '#B91C1C' }}>Error: {error}</div>}

      {!selectedSession && !loading && (
        <div style={{ color: 'var(--color-text-muted)', padding: 24 }}>
          Select a session to view its event timeline.
        </div>
      )}

      {events.length > 0 && (
        <div style={{ position: 'relative', paddingLeft: 32 }}>
          {/* Vertical line */}
          <div
            style={{
              position: 'absolute',
              left: 11,
              top: 8,
              bottom: 8,
              width: 2,
              background: 'var(--color-border)',
            }}
          />

          {events.map((event, idx) => (
            <div
              key={event.event_id}
              style={{
                position: 'relative',
                marginBottom: idx < events.length - 1 ? 20 : 0,
              }}
            >
              {/* Dot */}
              <div
                style={{
                  position: 'absolute',
                  left: -27,
                  top: 6,
                  width: 12,
                  height: 12,
                  borderRadius: '50%',
                  background: eventColor(event.event_type),
                  border: '2px solid #fff',
                  boxShadow: '0 0 0 2px var(--color-border)',
                }}
              />

              <div
                style={{
                  padding: '10px 14px',
                  background: '#fff',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <span
                    style={{
                      fontSize: '0.8rem',
                      fontWeight: 600,
                      color: eventColor(event.event_type),
                      textTransform: 'uppercase',
                      letterSpacing: '0.03em',
                    }}
                  >
                    {event.event_type}
                  </span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                    {new Date(event.timestamp).toLocaleTimeString()}
                  </span>
                </div>
                {Object.keys(event.data).length > 0 && (
                  <pre
                    style={{
                      margin: 0,
                      padding: 8,
                      background: 'var(--color-bg-secondary)',
                      borderRadius: 4,
                      fontSize: '0.78rem',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {JSON.stringify(event.data, null, 2)}
                  </pre>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedSession && !loading && events.length === 0 && !error && (
        <div style={{ color: 'var(--color-text-muted)', padding: 24 }}>No events recorded for this session.</div>
      )}
    </div>
  );
}

function eventColor(type: string): string {
  const map: Record<string, string> = {
    session_created: '#0D475C',
    message_received: '#3B82F6',
    message_sent: '#10B981',
    intent_classified: '#8B5CF6',
    tool_called: '#F59E0B',
    escalation: '#EF4444',
    resolution: '#22C55E',
    session_ended: '#6B7280',
  };
  return map[type?.toLowerCase()] ?? 'var(--color-primary)';
}
