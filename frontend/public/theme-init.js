/**
 * Theme Initializer — Flash-of-Wrong-Theme (FOWT) Prevention
 *
 * This script runs SYNCHRONOUSLY before React mounts to set the correct
 * `data-theme` attribute on <html>. Without it, there's a brief flash
 * where CSS variables resolve to the wrong theme on page reload.
 *
 * Priority order (mirrors ThemeContext.tsx):
 *   1. localStorage('carizmi-theme') = 'dark'|'light' — user's explicit choice
 *   2. prefers-color-scheme media query — OS/browser preference (when absent or 'system')
 *   3. 'light' — default fallback
 *
 * IMPORTANT: This file is loaded as a blocking <script> in index.html.
 * It must use vanilla JS (no ES modules, no imports, no TypeScript).
 * The localStorage key MUST match ThemeContext.tsx's STORAGE_KEY.
 */
(function () {
  var STORAGE_KEY = 'carizmi-theme';
  var saved = localStorage.getItem(STORAGE_KEY);
  var theme;

  if (saved === 'dark' || saved === 'light') {
    // Explicit user preference
    theme = saved;
  } else {
    // 'system' mode or no preference — follow OS
    theme = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  }

  document.documentElement.setAttribute('data-theme', theme);
})();