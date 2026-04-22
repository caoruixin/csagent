import { useState, type FormEvent, type CSSProperties } from 'react';
import { TOPIC_OPTIONS } from '../../types';
import type { CreateSessionRequest } from '../../types';

interface Props {
  onSubmit: (data: CreateSessionRequest) => void;
  loading: boolean;
  error: string | null;
}

const fieldStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 4,
};

const labelStyle: CSSProperties = {
  fontSize: '0.8rem',
  fontWeight: 500,
  color: 'var(--color-text-secondary)',
};

export default function PreChatForm({ onSubmit, loading, error }: Props) {
  const [firstName, setFirstName] = useState('');
  const [email, setEmail] = useState('');
  const [topicSubject, setTopicSubject] = useState('');
  const [adId, setAdId] = useState('');
  const [description, setDescription] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!firstName || !email || !topicSubject || !description) return;
    onSubmit({
      first_name: firstName,
      email,
      topic_subject: topicSubject,
      ad_id: adId || undefined,
      description,
    });
  };

  return (
    <form
      onSubmit={handleSubmit}
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
        padding: '20px 16px',
        overflowY: 'auto',
        flex: 1,
      }}
    >
      <div style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-primary)' }}>
        How can we help?
      </div>
      <p style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', margin: 0 }}>
        Please fill in the details below so we can assist you.
      </p>

      <div style={fieldStyle}>
        <label style={labelStyle}>First Name *</label>
        <input
          type="text"
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          placeholder="John"
          required
        />
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle}>Email *</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="john@example.com"
          required
        />
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle}>Topic *</label>
        <select
          value={topicSubject}
          onChange={(e) => setTopicSubject(e.target.value)}
          required
          style={{ padding: '10px 12px' }}
        >
          <option value="">Select a topic...</option>
          {TOPIC_OPTIONS.map((topic) => (
            <option key={topic} value={topic}>
              {topic}
            </option>
          ))}
        </select>
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle}>Ad ID (optional)</label>
        <input
          type="text"
          value={adId}
          onChange={(e) => setAdId(e.target.value)}
          placeholder="e.g. 1234567"
        />
      </div>

      <div style={fieldStyle}>
        <label style={labelStyle}>Description *</label>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Describe your issue..."
          rows={3}
          required
          style={{ resize: 'vertical' }}
        />
      </div>

      {error && (
        <div
          style={{
            background: '#FEF2F2',
            color: '#B91C1C',
            padding: '8px 12px',
            borderRadius: 'var(--radius)',
            fontSize: '0.85rem',
          }}
        >
          {error}
        </div>
      )}

      <button
        type="submit"
        className="btn btn-cta"
        disabled={loading}
        style={{
          padding: '12px',
          fontSize: '0.95rem',
          opacity: loading ? 0.6 : 1,
          marginTop: 4,
        }}
      >
        {loading ? 'Starting Chat...' : 'Start Chat'}
      </button>
    </form>
  );
}
