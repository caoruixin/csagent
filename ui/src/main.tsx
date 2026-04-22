import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './styles/gumtree-theme.css';
import App from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
