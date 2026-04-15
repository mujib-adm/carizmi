import { createContext, ReactNode, useCallback, useEffect, useMemo, useState } from 'react';

/**
 * Resolved visual theme applied to the UI.
 */
export type ThemeMode = 'light' | 'dark';

/**
 * User's theme preference — includes 'system' for auto-detection.
 *  - 'system': follow OS via matchMedia (default, no localStorage entry)
 *  - 'light' / 'dark': explicit user choice (persisted to localStorage)
 */
export type ThemePreference = 'light' | 'dark' | 'system';

interface ThemeContextValue {
  /** The resolved visual theme currently applied */
  theme: ThemeMode;
  /** The user's explicit preference (system, light, or dark) */
  preference: ThemePreference;
  /** Cycles through: system → dark → light → system */
  cycleTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextValue>({
  theme: 'light',
  preference: 'system',
  cycleTheme: () => {},
});

const STORAGE_KEY = 'carizmi-theme';

/** Cycle order: system → dark → light → system */
const CYCLE_ORDER: ThemePreference[] = ['system', 'dark', 'light'];

function getSystemTheme(): ThemeMode {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function getStoredPreference(): ThemePreference {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved === 'light' || saved === 'dark') return saved;
  return 'system';
}

function resolveTheme(preference: ThemePreference): ThemeMode {
  return preference === 'system' ? getSystemTheme() : preference;
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreference] = useState<ThemePreference>(getStoredPreference);
  const [theme, setTheme] = useState<ThemeMode>(() => resolveTheme(getStoredPreference()));

  // Apply the data-theme attribute to <html> whenever resolved theme changes
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // Listen for OS theme changes — only relevant when preference is 'system'
  useEffect(() => {
    if (preference !== 'system') return;

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = (e: MediaQueryListEvent) => setTheme(e.matches ? 'dark' : 'light');

    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, [preference]);

  // Cycle through: system → dark → light → system
  const cycleTheme = useCallback(() => {
    setPreference((prev) => {
      const currentIndex = CYCLE_ORDER.indexOf(prev);
      const next = CYCLE_ORDER[(currentIndex + 1) % CYCLE_ORDER.length];

      if (next === 'system') {
        localStorage.removeItem(STORAGE_KEY);
        setTheme(getSystemTheme());
      } else {
        localStorage.setItem(STORAGE_KEY, next);
        setTheme(next);
      }

      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ theme, preference, cycleTheme }),
    [theme, preference, cycleTheme],
  );

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}