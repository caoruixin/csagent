import { useState, type CSSProperties } from 'react';
import type { AdminTab } from '../types';
import SessionList from '../components/admin/SessionList';
import TraceViewer from '../components/admin/TraceViewer';
import HandoverQueueView from '../components/admin/HandoverQueueView';
import EventTimeline from '../components/admin/EventTimeline';
import MetricsDashboard from '../components/admin/MetricsDashboard';

const TABS: { key: AdminTab; label: string }[] = [
  { key: 'sessions', label: 'Sessions' },
  { key: 'traces', label: 'Traces' },
  { key: 'handover', label: 'Handover Queue' },
  { key: 'events', label: 'Events' },
  { key: 'metrics', label: 'Metrics' },
];

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState<AdminTab>('sessions');
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);

  const handleSelectSession = (id: string) => {
    setSelectedSessionId(id);
    setActiveTab('traces');
  };

  const handleBackFromTrace = () => {
    setSelectedSessionId(null);
    setActiveTab('sessions');
  };

  const tabStyle = (key: AdminTab): CSSProperties => ({
    padding: '10px 20px',
    fontSize: '0.9rem',
    fontWeight: activeTab === key ? 600 : 400,
    color: activeTab === key ? 'var(--color-primary)' : 'var(--color-text-muted)',
    borderBottom: activeTab === key ? '2px solid var(--color-primary)' : '2px solid transparent',
    background: 'none',
    cursor: 'pointer',
    transition: 'color 0.2s, border-color 0.2s',
  });

  return (
    <div style={{ minHeight: 'calc(100vh - 64px)', background: 'var(--color-bg-secondary)' }}>
      <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-primary)', marginBottom: 20 }}>
          Admin Dashboard
        </h1>

        {/* Tab bar */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid var(--color-border)',
            marginBottom: 24,
            background: '#fff',
            borderRadius: 'var(--radius) var(--radius) 0 0',
          }}
        >
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              style={tabStyle(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Tab content */}
        <div
          style={{
            background: '#fff',
            borderRadius: '0 0 var(--radius) var(--radius)',
            padding: 24,
            minHeight: 400,
          }}
        >
          {activeTab === 'sessions' && (
            <SessionList onSelectSession={handleSelectSession} />
          )}
          {activeTab === 'traces' && selectedSessionId && (
            <TraceViewer sessionId={selectedSessionId} onBack={handleBackFromTrace} />
          )}
          {activeTab === 'traces' && !selectedSessionId && (
            <div style={{ padding: 24, color: 'var(--color-text-muted)' }}>
              Select a session from the Sessions tab to view its trace.
            </div>
          )}
          {activeTab === 'handover' && <HandoverQueueView />}
          {activeTab === 'events' && <EventTimeline />}
          {activeTab === 'metrics' && <MetricsDashboard />}
        </div>
      </div>
    </div>
  );
}
