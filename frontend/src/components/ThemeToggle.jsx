import { Moon, Sun } from 'lucide-react';
import { useState } from 'react';

const KEY = 'cc_theme'; // harmless UI preference only

function currentIsDark() {
  const set = document.documentElement.dataset.theme;
  if (set) return set === 'dark';
  return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

export default function ThemeToggle() {
  const [dark, setDark] = useState(() => {
    try {
      const saved = localStorage.getItem(KEY);
      if (saved) document.documentElement.dataset.theme = saved;
    } catch { /* storage unavailable */ }
    return currentIsDark();
  });

  function toggle() {
    const next = dark ? 'light' : 'dark';
    document.documentElement.dataset.theme = next;
    try { localStorage.setItem(KEY, next); } catch { /* ignore */ }
    setDark(!dark);
  }

  return (
    <button type="button" className="btn-ghost" onClick={toggle} aria-label={dark ? 'Switch to light theme' : 'Switch to dark theme'}>
      {dark ? <Sun size={16} aria-hidden="true" /> : <Moon size={16} aria-hidden="true" />}
      <span>{dark ? 'Light' : 'Dark'}</span>
    </button>
  );
}
