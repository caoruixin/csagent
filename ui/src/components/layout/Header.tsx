import { Link, useLocation } from 'react-router-dom';

export default function Header() {
  const location = useLocation();
  const isAdmin = location.pathname.startsWith('/admin');

  return (
    <header
      style={{
        background: 'var(--color-primary)',
        color: '#fff',
        padding: '0 24px',
        height: 64,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexShrink: 0,
        position: 'sticky',
        top: 0,
        zIndex: 900,
      }}
    >
      <Link to="/" style={{ color: '#fff', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 10 }}>
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <path d="M8 12h8" />
          <path d="M12 8v8" />
        </svg>
        <span style={{ fontWeight: 700, fontSize: '1.1rem', letterSpacing: '-0.02em' }}>
          Gumtree CS Agent
        </span>
      </Link>

      <nav style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
        <Link
          to="/"
          style={{
            color: '#fff',
            textDecoration: 'none',
            fontSize: '0.9rem',
            opacity: isAdmin ? 0.7 : 1,
            fontWeight: isAdmin ? 400 : 600,
            transition: 'opacity 0.2s',
          }}
        >
          Demo
        </Link>
        <Link
          to="/admin"
          style={{
            color: '#fff',
            textDecoration: 'none',
            fontSize: '0.9rem',
            opacity: isAdmin ? 1 : 0.7,
            fontWeight: isAdmin ? 600 : 400,
            transition: 'opacity 0.2s',
          }}
        >
          Admin
        </Link>
      </nav>
    </header>
  );
}
