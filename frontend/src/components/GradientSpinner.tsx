import { useMemo } from 'react';

interface GradientSpinnerProps {
  size?: number;
  strokeWidth?: number;
}

export default function GradientSpinner({ size = 80, strokeWidth = 10 }: GradientSpinnerProps) {
  const outerRadius = (size - strokeWidth) / 2;
  const innerRadius = outerRadius - strokeWidth - 4;

  const outerCircumference = useMemo(() => 2 * Math.PI * outerRadius, [outerRadius]);
  const innerCircumference = useMemo(() => 2 * Math.PI * innerRadius, [innerRadius]);

  return (
    <svg
      className="gradient-spinner-wrapper"
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
    >
      <defs>
        <linearGradient id="spinnerGradientOuter" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="var(--spinner-color-1)" />
          <stop offset="50%" stopColor="var(--spinner-color-2)" />
          <stop offset="100%" stopColor="var(--spinner-color-3)" />
        </linearGradient>
        <linearGradient id="spinnerGradientInner" x1="100%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="var(--spinner-color-3)" />
          <stop offset="100%" stopColor="var(--spinner-color-1)" />
        </linearGradient>
      </defs>

      {/* Outer track */}
      <circle
        cx={size / 2}
        cy={size / 2}
        r={outerRadius}
        stroke="var(--spinner-track)"
        strokeWidth={strokeWidth}
        fill="none"
      />

      {/* Inner track */}
      <circle
        cx={size / 2}
        cy={size / 2}
        r={innerRadius}
        stroke="var(--spinner-track)"
        strokeWidth={strokeWidth}
        fill="none"
      />

      {/* Outer arc — clockwise */}
      <circle
        className="spinner-ring-outer"
        cx={size / 2}
        cy={size / 2}
        r={outerRadius}
        stroke="url(#spinnerGradientOuter)"
        strokeWidth={strokeWidth}
        fill="none"
        strokeLinecap="round"
        strokeDasharray={`${outerCircumference * 0.65} ${outerCircumference * 0.35}`}
      />

      {/* Inner arc — counter-clockwise */}
      <circle
        className="spinner-ring-inner"
        cx={size / 2}
        cy={size / 2}
        r={innerRadius}
        stroke="url(#spinnerGradientInner)"
        strokeWidth={strokeWidth}
        fill="none"
        strokeLinecap="round"
        strokeDasharray={`${innerCircumference * 0.45} ${innerCircumference * 0.55}`}
      />
    </svg>
  );
}