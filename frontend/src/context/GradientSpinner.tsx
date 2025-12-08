import React from "react";

interface GradientSpinnerProps {
    size?: number;        // diameter in px
    strokeWidth?: number; // thickness of the ring
    colors?: string[];    // gradient colors
    speed?: number;       // animation duration in seconds
}

const GradientSpinner: React.FC<GradientSpinnerProps> = ({
    size = 80,
    strokeWidth = 18,
    colors = ["#FFFFFF", "#AEDF88", "#1E5631"],
    speed = 1.2,
}) => {
    const radius = (size - strokeWidth) / 2;
    const circumference = 2 * Math.PI * radius;

    return (
        <svg
            className="gradient-spinner"
            width={size}
            height={size}
            viewBox={`0 0 ${size} ${size}`}
            style={{
                animation: `spin ${speed}s linear infinite`,
            }}
        >
            <defs>
                <linearGradient id="spinnerGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                    {colors.map((color, i) => (
                        <stop
                            key={i}
                            offset={`${(i / (colors.length - 1)) * 100}%`}
                            stopColor={color}
                        />
                    ))}
                </linearGradient>
            </defs>
            <circle
                cx={size / 2}
                cy={size / 2}
                r={radius}
                stroke="url(#spinnerGradient)"
                strokeWidth={strokeWidth}
                fill="none"
                strokeLinecap="round"
                strokeDasharray={circumference}
            />
        </svg>
    );
};

export default GradientSpinner;

// CSS (global or module)
const styles = `
@keyframes spin {
  to { transform: rotate(360deg); }
}
.gradient-spinner {
  display: block;
}
`;
document.head.insertAdjacentHTML("beforeend", `<style>${styles}</style>`);
