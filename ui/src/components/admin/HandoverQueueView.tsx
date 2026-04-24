import { useState, useEffect, useMemo, type CSSProperties } from 'react';
import { getHandoverLogs } from '../../api/client';
import type { HandoverLog, TranscriptEntry } from '../../types';

// ── Styles ──

const card: CSSProperties = {
  background: '#fff',
  borderRadius: 'var(--radius)',
  boxShadow: 'var(--shadow)',
  padding: 20,
  marginBottom: 16,
};

const badge = (bg: string, fg: string): CSSProperties => ({
  display: 'inline-block',
  padding: '2px 10px',
  borderRadius: 12,
  fontSize: '0.72rem',
  fontWeight: 600,
  textTransform: 'uppercase',
  background: bg,
  color: fg,
  letterSpacing: '0.03em',
});

const filterRow: CSSProperties = {
  display: 'flex',
  gap: 12,
  marginBottom: 20,
  flexWrap: 'wrap',
  alignItems: 'center',
};

const filterSelect: CSSProperties = {
  padding: '6px 12px',
  borderRadius: 'var(--radius)',
  border: '1px solid var(--color-border)',
  fontSize: '0.85rem',
  background: '#fff',
};

// ── Helpers ──

function shortId(id: string): string {
  if (!id) return '--';
  return id.length > 12 ? id.substring(0, 8) + '...' : id;
}

function extractUc(log: HandoverLog): string {
  return (log.payload?.primary_use_case as string) ?? '--';
}

function extractReason(log: HandoverLog): string {
  return log.reason || (log.payload?.escalation_reason as string) || '--';
}

function extractTurns(log: HandoverLog): number {
  return (log.payload?.total_bot_turns as number) ?? 0;
}

function extractConfidence(log: HandoverLog): string {
  const c = log.payload?.intent_confidence;
  if (c == null) return '--';
  if (typeof c === 'number') return (c * 100).toFixed(0) + '%';
  return String(c);
}

function extractDuration(log: HandoverLog): string {
  const s = log.payload?.handling_duration_seconds;
  if (s == null) return '--';
  const secs = Number(s);
  if (secs < 60) return `${secs}s`;
  return `${Math.floor(secs / 60)}m ${secs % 60}s`;
}

function transferBadge(result: string) {
  if (result === 'transferred') return badge('#DCFCE7', '#166534');
  if (result === 'offline_logged') return badge('#FEF3C7', '#92400E');
  return badge('#F1F1F1', '#635B67');
}

// ── Component ──

export default function HandoverQueueView() {
  const [logs, setLogs] = useState<HandoverLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Filters
  const [ucFilter, setUcFilter] = useState('');
  const [reasonFilter, setReasonFilter] = useState('');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getHandoverLogs()
      .then((data) => { if (!cancelled) setLogs(data); })
      .catch((err) => { if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  // Derive unique UC and reason values for filter dropdowns
  const ucOptions = useMemo(() => [...new Set(logs.map(extractUc))].filter(v => v !== '--').sort(), [logs]);
  const reasonOptions = useMemo(() => [...new Set(logs.map(extractReason))].filter(v => v !== '--').sort(), [logs]);

  const filtered = useMemo(() => {
    return logs.filter((log) => {
      if (ucFilter && extractUc(log) !== ucFilter) return false;
      if (reasonFilter && extractReason(log) !== reasonFilter) return false;
      return true;
    });
  }, [logs, ucFilter, reasonFilter]);

  const selectedLog = selectedId ? logs.find(l => l.log_id === selectedId) : null;

  if (loading) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>Loading handover queue...</div>;
  if (error) return <div style={{ padding: 24, color: '#B91C1C' }}>Error: {error}</div>;

  // ── Detail view ──
  if (selectedLog) {
    return <DetailView log={selectedLog} onBack={() => setSelectedId(null)} />;
  }

  // ── List view ──
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--color-primary)', margin: 0 }}>
          Handover Queue ({filtered.length})
        </h2>
      </div>

      {/* Filters */}
      <div style={filterRow}>
        <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--color-text-secondary)' }}>Filter:</label>
        <select style={filterSelect} value={ucFilter} onChange={e => setUcFilter(e.target.value)}>
          <option value="">All Use Cases</option>
          {ucOptions.map(uc => <option key={uc} value={uc}>{uc}</option>)}
        </select>
        <select style={filterSelect} value={reasonFilter} onChange={e => setReasonFilter(e.target.value)}>
          <option value="">All Reasons</option>
          {reasonOptions.map(r => <option key={r} value={r}>{r}</option>)}
        </select>
        {(ucFilter || reasonFilter) && (
          <button
            onClick={() => { setUcFilter(''); setReasonFilter(''); }}
            style={{ fontSize: '0.8rem', color: 'var(--color-primary)', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}
          >
            Clear filters
          </button>
        )}
      </div>

      {filtered.length === 0 ? (
        <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>No handover logs match the current filters.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {/* Table header */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '120px 1fr 1fr 70px 80px 160px',
            gap: 8,
            padding: '8px 16px',
            fontSize: '0.75rem',
            fontWeight: 600,
            color: 'var(--color-text-muted)',
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
          }}>
            <span>Session</span>
            <span>Use Case</span>
            <span>Reason</span>
            <span>Turns</span>
            <span>Status</span>
            <span>Timestamp</span>
          </div>

          {/* Rows */}
          {filtered.map((log) => (
            <div
              key={log.log_id}
              onClick={() => setSelectedId(log.log_id)}
              style={{
                display: 'grid',
                gridTemplateColumns: '120px 1fr 1fr 70px 80px 160px',
                gap: 8,
                padding: '12px 16px',
                background: '#fff',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius)',
                cursor: 'pointer',
                transition: 'box-shadow 0.15s, border-color 0.15s',
                alignItems: 'center',
                fontSize: '0.85rem',
              }}
              onMouseEnter={e => {
                (e.currentTarget as HTMLElement).style.borderColor = 'var(--color-primary)';
                (e.currentTarget as HTMLElement).style.boxShadow = '0 2px 8px rgba(13,71,92,0.1)';
              }}
              onMouseLeave={e => {
                (e.currentTarget as HTMLElement).style.borderColor = 'var(--color-border)';
                (e.currentTarget as HTMLElement).style.boxShadow = 'none';
              }}
            >
              <span style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--color-primary)' }}>
                {shortId(log.session_id)}
              </span>
              <span style={{ fontWeight: 500 }}>{extractUc(log)}</span>
              <span style={{ color: 'var(--color-text-secondary)', fontSize: '0.82rem' }}>{extractReason(log)}</span>
              <span style={{ textAlign: 'center' }}>{extractTurns(log)}</span>
              <span style={transferBadge(log.priority)}>{log.priority}</span>
              <span style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)' }}>
                {new Date(log.created_at).toLocaleString()}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Detail View ──

function DetailView({ log, onBack }: { log: HandoverLog; onBack: () => void }) {
  return (
    <div>
      <button
        onClick={onBack}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
          marginBottom: 20,
          padding: '6px 14px',
          fontSize: '0.85rem',
          fontWeight: 500,
          color: 'var(--color-primary)',
          background: 'none',
          border: '1px solid var(--color-primary)',
          borderRadius: 'var(--radius)',
          cursor: 'pointer',
        }}
      >
        &larr; Back to queue
      </button>

      {/* Summary card */}
      <div style={card}>
        <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-primary)', marginBottom: 14 }}>
          Summary
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12 }}>
          <InfoItem label="Use Case" value={extractUc(log)} />
          <InfoItem label="Escalation Reason" value={extractReason(log)} />
          <InfoItem label="Case ID" value={(log.payload?.case_id as string) || '--'} />
          <InfoItem label="Confidence" value={extractConfidence(log)} />
          <InfoItem label="Total Turns" value={String(extractTurns(log))} />
          <InfoItem label="Duration" value={extractDuration(log)} />
          <InfoItem label="Session ID" value={log.session_id} mono />
          <InfoItem label="Transfer Result" value={log.priority} />
        </div>
      </div>

      {/* Customer message card */}
      {log.customer_message && (
        <div style={card}>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-primary)', marginBottom: 10 }}>
            Customer Message
          </h3>
          <div style={{
            padding: 14,
            background: 'var(--color-bg-secondary)',
            borderRadius: 'var(--radius)',
            fontSize: '0.9rem',
            lineHeight: 1.6,
            color: 'var(--color-text-secondary)',
          }}>
            {log.customer_message}
          </div>
        </div>
      )}

      {/* Handover payload */}
      <div style={card}>
        <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-primary)', marginBottom: 10 }}>
          Handover Payload
        </h3>
        <pre style={{
          background: 'var(--color-bg-secondary)',
          padding: 14,
          borderRadius: 'var(--radius)',
          fontSize: '0.8rem',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          maxHeight: 400,
          overflow: 'auto',
          margin: 0,
        }}>
          {JSON.stringify(log.payload, null, 2)}
        </pre>
      </div>

      {/* Conversation transcript */}
      {log.transcript && log.transcript.length > 0 && (
        <div style={card}>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-primary)', marginBottom: 14 }}>
            Conversation Transcript ({log.transcript.length} messages)
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {log.transcript.map((entry, i) => (
              <TranscriptBubble key={i} entry={entry} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Sub-components ──

function InfoItem({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <div style={{ fontSize: '0.72rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', marginBottom: 3 }}>
        {label}
      </div>
      <div style={{
        fontSize: '0.88rem',
        fontWeight: 500,
        fontFamily: mono ? 'monospace' : 'inherit',
        wordBreak: 'break-all',
      }}>
        {value || '--'}
      </div>
    </div>
  );
}

function TranscriptBubble({ entry }: { entry: TranscriptEntry }) {
  const isUser = entry.role === 'user';
  return (
    <div style={{ display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start' }}>
      <div style={{
        maxWidth: '75%',
        padding: '10px 14px',
        borderRadius: isUser ? '12px 12px 4px 12px' : '12px 12px 12px 4px',
        background: isUser ? 'var(--color-primary)' : 'var(--color-bg-secondary)',
        color: isUser ? '#fff' : 'var(--color-text)',
        fontSize: '0.85rem',
        lineHeight: 1.5,
      }}>
        <div style={{ fontSize: '0.7rem', fontWeight: 600, marginBottom: 4, opacity: 0.7 }}>
          {isUser ? 'Customer' : 'Bot'} (turn {entry.turn_index})
        </div>
        {entry.message}
      </div>
    </div>
  );
}
