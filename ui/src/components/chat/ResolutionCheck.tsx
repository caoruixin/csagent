import { useState } from 'react';

interface Props {
  onResponse: (helpful: boolean) => void;
}

export default function ResolutionCheck({ onResponse }: Props) {
  const [answered, setAnswered] = useState(false);

  const handleClick = (helpful: boolean) => {
    if (answered) return;
    setAnswered(true);
    onResponse(helpful);
  };

  if (answered) {
    return (
      <div
        style={{
          padding: '8px 12px',
          background: 'var(--color-bg-secondary)',
          borderRadius: 'var(--radius)',
          fontSize: '0.85rem',
          color: 'var(--color-text-muted)',
        }}
      >
        Thanks for your feedback!
      </div>
    );
  }

  return (
    <div
      style={{
        padding: '10px 14px',
        background: '#fff',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius)',
      }}
    >
      <div style={{ fontSize: '0.85rem', fontWeight: 500, marginBottom: 8 }}>
        Did that help?
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className="btn btn-cta"
          style={{ fontSize: '0.8rem', padding: '6px 16px' }}
          onClick={() => handleClick(true)}
        >
          Yes
        </button>
        <button
          className="btn btn-outline"
          style={{ fontSize: '0.8rem', padding: '6px 16px' }}
          onClick={() => handleClick(false)}
        >
          No
        </button>
      </div>
    </div>
  );
}
