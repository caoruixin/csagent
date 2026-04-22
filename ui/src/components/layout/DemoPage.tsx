import ChatWidget from '../chat/ChatWidget';

export default function DemoPage() {
  return (
    <div style={{ position: 'relative', minHeight: 'calc(100vh - 64px)' }}>
      {/* Landing content */}
      <div
        style={{
          maxWidth: 720,
          margin: '0 auto',
          padding: '60px 24px',
          textAlign: 'center',
        }}
      >
        <h1
          style={{
            fontSize: '2.2rem',
            fontWeight: 700,
            color: 'var(--color-primary)',
            marginBottom: 16,
            lineHeight: 1.2,
          }}
        >
          Customer Support Agent Demo
        </h1>
        <p
          style={{
            fontSize: '1.1rem',
            color: 'var(--color-text-muted)',
            marginBottom: 40,
            lineHeight: 1.6,
          }}
        >
          Experience the AI-powered customer support agent for Gumtree.
          Click the green chat button in the bottom-right corner to start a conversation.
        </p>

        {/* Feature cards */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: 20,
            textAlign: 'left',
          }}
        >
          {[
            {
              title: 'Ad Support',
              desc: 'Help with ad visibility, editing, and promotion issues.',
            },
            {
              title: 'Account Support',
              desc: 'Password resets, profile updates, and account recovery.',
            },
            {
              title: 'Payments',
              desc: 'Payment failures, refunds, and billing questions.',
            },
            {
              title: 'Safety',
              desc: 'Report scams, suspicious listings, and safety concerns.',
            },
          ].map((card) => (
            <div
              key={card.title}
              style={{
                padding: '20px',
                background: '#fff',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius)',
                transition: 'box-shadow 0.2s',
              }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.boxShadow = 'var(--shadow)'; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.boxShadow = 'none'; }}
            >
              <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 8, color: 'var(--color-primary)' }}>
                {card.title}
              </h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', margin: 0, lineHeight: 1.5 }}>
                {card.desc}
              </p>
            </div>
          ))}
        </div>
      </div>

      <ChatWidget />
    </div>
  );
}
