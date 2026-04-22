interface Props {
  title: string;
  snippet: string;
  url: string;
}

export default function ArticleCard({ title, snippet, url }: Props) {
  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      style={{
        display: 'block',
        background: '#fff',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius)',
        padding: '10px 14px',
        textDecoration: 'none',
        transition: 'box-shadow 0.2s',
      }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLElement).style.boxShadow = 'var(--shadow)';
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLElement).style.boxShadow = 'none';
      }}
    >
      <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-primary)', marginBottom: 4 }}>
        {title}
      </div>
      <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', lineHeight: 1.4 }}>
        {snippet}
      </div>
      <div style={{ fontSize: '0.75rem', color: 'var(--color-cta)', marginTop: 6, fontWeight: 500 }}>
        Read more
      </div>
    </a>
  );
}
