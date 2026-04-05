/**
 * Chart Theme Constants
 *
 * Pure data constants for chart colours, gradient definitions, and label badge styling.
 * These values are consumed programmatically by Recharts (SVG) and cannot live in CSS
 * because Recharts requires literal JS values for <Cell fill=""> and <linearGradient>.
 */

// ── Paid-slice palettes (one colour per quarter) ──────────────
export const PAID_COLORS_LIGHT = ['#2D6A4F', '#40916C', '#52B788', '#74C69D'];
export const PAID_COLORS_DARK = ['#a78bfa', '#8b5cf6', '#7c3aed', '#c084fc'];

// ── Unpaid-slice fills (light = solid colours, dark = SVG gradient refs) ──
export const UNPAID_COLORS = {
  light: { dues: 'orange', overdue: 'red', future: '#E0E0E0' },
  dark: { dues: 'url(#duesGrad)', overdue: 'url(#overdueGrad)', future: 'url(#futureGrad)' },
} as const;

// ── Pie-chart label badge theming ──
export const LABEL_BADGE = {
  light: {
    fill: 'white',
    stroke: '#40916C',
    textFill: '#2f744e',
    shadow: 'drop-shadow(0px 4px 8px rgba(0,0,0,0.12))',
  },
  dark: {
    fill: '#111827',
    stroke: '#a78bfa',
    textFill: '#e7e9ea',
    shadow: 'drop-shadow(0px 2px 8px rgba(167, 139, 250, 0.3))',
  },
} as const;

// ── SVG gradient stop-colour definitions (rendered by ChartGradientDefs) ──
export const CHART_GRADIENT_DEFS = [
  // Paid — purple shades matching Revenue card
  { id: 'paidGrad0', from: '#c084fc', to: '#7c3aed' },
  { id: 'paidGrad1', from: '#a78bfa', to: '#6d28d9' },
  { id: 'paidGrad2', from: '#8b5cf6', to: '#5b21b6' },
  { id: 'paidGrad3', from: '#d8b4fe', to: '#7c3aed' },
  // Dues — amber matching Dues card
  { id: 'duesGrad', from: '#f9f0ca', to: '#d97706' },
  // Overdues — crimson matching Overdues card
  { id: 'overdueGrad', from: '#fca5a5', to: '#dc2626' },
  // Future — dark slate
  { id: 'futureGrad', from: '#334155', to: '#1e293b' },
];

// ── Helpers ──

/** Returns the paid-slice fill for a given quarter index */
export const getPaidColor = (isDark: boolean, idx: number): string =>
  isDark
    ? `url(#paidGrad${idx % PAID_COLORS_DARK.length})`
    : PAID_COLORS_LIGHT[idx % PAID_COLORS_LIGHT.length];

/** Returns the unpaid-slice fill for a given status */
export const getUnpaidColor = (isDark: boolean, status: 'dues' | 'overdue' | 'future'): string =>
  isDark ? UNPAID_COLORS.dark[status] : UNPAID_COLORS.light[status];