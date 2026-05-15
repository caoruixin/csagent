import { type CSSProperties } from 'react';

const container: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 4,
  padding: '12px 16px',
  background: 'var(--color-primary)',
  borderRadius: 'var(--radius)',
  borderBottomLeftRadius: 0,
  width: 'fit-content',
  maxWidth: '80%',
};

const dot: CSSProperties = {
  width: 8,
  height: 8,
  borderRadius: '50%',
  background: 'rgba(255,255,255,0.7)',
};

export default function TypingIndicator() {
  return (
    <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: 8 }}>
      <div style={container}>
        <style>{`
          @keyframes typing-bounce {
            0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
            30% { transform: translateY(-6px); opacity: 1; }
          }
        `}</style>
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            style={{
              ...dot,
              animation: `typing-bounce 1.2s ease-in-out ${i * 0.2}s infinite`,
            }}
          />
        ))}
      </div>
    </div>
  );
}
