/**
 * Chart SVG Definitions & Label Renderer
 *
 * Recharts-specific JSX components for pie chart gradient fills
 * and custom label badges. Consumes constants from ChartThemeConstants.
 */
import { CHART_GRADIENT_DEFS, LABEL_BADGE } from '../../constants/ChartThemeConstants';

const RADIAN = Math.PI / 180;

/** Renders SVG <defs> with all chart gradient fills — used in dark mode only */
export function ChartGradientDefs() {
  return (
    <defs>
      {CHART_GRADIENT_DEFS.map(({ id, from, to }: { id: string; from: string; to: string }) => (
        <linearGradient key={id} id={id} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={from} />
          <stop offset="100%" stopColor={to} />
        </linearGradient>
      ))}
    </defs>
  );
}

/** Creates a Recharts-compatible custom label renderer for pie chart slices */
export const createLabelRenderer = (isDark: boolean) => {
  const badge = isDark ? LABEL_BADGE.dark : LABEL_BADGE.light;

  return ({ cx, cy, midAngle, outerRadius, payload }: any) => {
    if (
      payload.isGap ||
      payload.value === 0 ||
      payload.name?.includes('Future') ||
      payload.name?.includes('Dues') ||
      payload.name?.includes('Overdues')
    )
      return null;

    const radius = outerRadius + 25;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
      <g>
        <circle
          cx={x}
          cy={y}
          r="20"
          fill={badge.fill}
          stroke={badge.stroke}
          strokeWidth="2"
          style={{ filter: badge.shadow }}
        />
        <text
          x={x}
          y={y}
          fill={badge.textFill}
          textAnchor="middle"
          dominantBaseline="central"
          style={{ fontWeight: 800, fontSize: '13px', fontFamily: 'Outfit, sans-serif' }}
        >
          {`${(payload.rate * 100).toFixed(0)}%`}
        </text>
      </g>
    );
  };
};