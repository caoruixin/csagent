import { useState, useEffect } from 'react';
import { getFunnelMetrics, getPerUCMetrics } from '../../api/client';
import type { FunnelMetrics, PerUCMetric } from '../../types';

export default function MetricsDashboard() {
  const [funnel, setFunnel] = useState<FunnelMetrics | null>(null);
  const [perUC, setPerUC] = useState<PerUCMetric[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([getFunnelMetrics(), getPerUCMetrics()])
      .then(([f, u]) => {
        if (!cancelled) {
          setFunnel(f);
          setPerUC(u);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load metrics');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  if (loading) return <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>Loading metrics...</div>;
  if (error) return <div style={{ padding: 24, color: '#B91C1C' }}>Error: {error}</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
      {/* Funnel */}
      {funnel && <FunnelChart funnel={funnel} />}

      {/* Per-UC Table */}
      {perUC.length > 0 && <PerUCTable data={perUC} />}
    </div>
  );
}

function FunnelChart({ funnel }: { funnel: FunnelMetrics }) {
  const steps = [
    { label: 'Total Sessions', value: funnel.total_sessions, color: 'var(--color-primary)' },
    { label: 'Self-Served', value: funnel.self_served, color: '#22C55E' },
    { label: 'Escalated', value: funnel.escalated, color: '#F59E0B' },
    { label: 'Abandoned', value: funnel.abandoned, color: '#EF4444' },
  ];

  const max = Math.max(funnel.total_sessions, 1);

  return (
    <div>
      <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 16, color: 'var(--color-text-secondary)' }}>
        Session Funnel
      </h3>

      <div style={{ display: 'flex', gap: 20, alignItems: 'flex-end', marginBottom: 24 }}>
        {steps.map((step) => (
          <div key={step.label} style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontWeight: 700, fontSize: '1.4rem', color: step.color, marginBottom: 4 }}>
              {step.value}
            </div>
            <div
              style={{
                height: `${Math.max((step.value / max) * 120, 8)}px`,
                background: step.color,
                borderRadius: '4px 4px 0 0',
                transition: 'height 0.4s ease-out',
                opacity: 0.85,
              }}
            />
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: 6 }}>
              {step.label}
            </div>
          </div>
        ))}
      </div>

      <div
        style={{
          display: 'flex',
          gap: 24,
          padding: '12px 16px',
          background: 'var(--color-bg-secondary)',
          borderRadius: 'var(--radius)',
          fontSize: '0.85rem',
        }}
      >
        <div>
          <strong>Avg. Turns:</strong> {funnel.avg_turns.toFixed(1)}
        </div>
        <div>
          <strong>Self-serve Rate:</strong>{' '}
          {funnel.total_sessions > 0 ? ((funnel.self_served / funnel.total_sessions) * 100).toFixed(1) : 0}%
        </div>
      </div>
    </div>
  );
}

function PerUCTable({ data }: { data: PerUCMetric[] }) {
  return (
    <div>
      <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 16, color: 'var(--color-text-secondary)' }}>
        Per Use-Case Metrics
      </h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid var(--color-border)' }}>
              {['Use Case', 'Total', 'Self-Served', 'Escalated', 'Avg Turns', 'Avg CSAT'].map((h) => (
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
            {data.map((row) => (
              <tr key={row.use_case} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <td style={{ padding: '10px 12px', fontWeight: 500 }}>{row.use_case}</td>
                <td style={{ padding: '10px 12px' }}>{row.total}</td>
                <td style={{ padding: '10px 12px' }}>
                  <span style={{ color: '#22C55E', fontWeight: 500 }}>{row.self_served}</span>
                </td>
                <td style={{ padding: '10px 12px' }}>
                  <span style={{ color: '#F59E0B', fontWeight: 500 }}>{row.escalated}</span>
                </td>
                <td style={{ padding: '10px 12px' }}>{row.avg_turns.toFixed(1)}</td>
                <td style={{ padding: '10px 12px' }}>
                  <CsatDisplay score={row.avg_csat} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function CsatDisplay({ score }: { score: number }) {
  const color = score >= 4 ? '#22C55E' : score >= 3 ? '#F59E0B' : '#EF4444';
  return (
    <span style={{ fontWeight: 600, color }}>
      {score.toFixed(1)} / 5
    </span>
  );
}
