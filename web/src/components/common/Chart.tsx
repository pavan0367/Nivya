import React from 'react';

interface DataPoint {
  label: string;
  value: number;
}

interface LineChartProps {
  id?: string;
  data: DataPoint[];
  height?: number;
  color?: string;
  unit?: string;
  minValue?: number;
  maxValue?: number;
}

export const LineChart: React.FC<LineChartProps> = ({
  id,
  data,
  height = 180,
  color = 'var(--primary)',
  unit = '',
  minValue = 0,
  maxValue,
}) => {
  if (!data || data.length === 0) {
    return (
      <div style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-dim)' }}>
        No chart data available
      </div>
    );
  }

  const computedMax = maxValue !== undefined ? maxValue : Math.max(...data.map((d) => d.value), 100);
  const range = computedMax - minValue || 1;

  const width = 600;
  const paddingX = 40;
  const paddingY = 25;
  const chartW = width - paddingX * 2;
  const chartH = height - paddingY * 2;

  const points = data.map((d, index) => {
    const x = paddingX + (index / (data.length - 1 || 1)) * chartW;
    const y = height - paddingY - ((d.value - minValue) / range) * chartH;
    return { x, y, label: d.label, value: d.value };
  });

  const pathD = points.reduce((acc, p, i) => {
    return i === 0 ? `M ${p.x} ${p.y}` : `${acc} L ${p.x} ${p.y}`;
  }, '');

  const areaD = points.length > 0
    ? `${pathD} L ${points[points.length - 1].x} ${height - paddingY} L ${points[0].x} ${height - paddingY} Z`
    : '';

  return (
    <div id={id} style={{ width: '100%', overflowX: 'auto' }}>
      <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
        <defs>
          <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.35" />
            <stop offset="100%" stopColor={color} stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {/* Grid lines */}
        <line x1={paddingX} y1={paddingY} x2={width - paddingX} y2={paddingY} stroke="rgba(255,255,255,0.05)" strokeDasharray="3 3" />
        <line x1={paddingX} y1={height / 2} x2={width - paddingX} y2={height / 2} stroke="rgba(255,255,255,0.05)" strokeDasharray="3 3" />
        <line x1={paddingX} y1={height - paddingY} x2={width - paddingX} y2={height - paddingY} stroke="rgba(255,255,255,0.1)" />

        {/* Area */}
        <path d={areaD} fill="url(#chartGradient)" />

        {/* Line */}
        <path d={pathD} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />

        {/* Data points */}
        {points.map((p, idx) => (
          <g key={idx}>
            <circle cx={p.x} cy={p.y} r="4" fill="var(--bg-surface)" stroke={color} strokeWidth="2" />
            <text x={p.x} y={height - 8} textAnchor="middle" fill="var(--text-dim)" fontSize="10" fontFamily="sans-serif">
              {p.label}
            </text>
          </g>
        ))}

        {/* Y Axis Labels */}
        <text x={paddingX - 8} y={paddingY + 4} textAnchor="end" fill="var(--text-dim)" fontSize="10">{computedMax}{unit}</text>
        <text x={paddingX - 8} y={height - paddingY + 4} textAnchor="end" fill="var(--text-dim)" fontSize="10">{minValue}{unit}</text>
      </svg>
    </div>
  );
};

interface BarChartProps {
  id?: string;
  data: DataPoint[];
  height?: number;
  color?: string;
  unit?: string;
}

export const BarChart: React.FC<BarChartProps> = ({
  id,
  data,
  height = 180,
  color = 'var(--primary)',
  unit = '',
}) => {
  if (!data || data.length === 0) {
    return (
      <div style={{ height, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-dim)' }}>
        No chart data available
      </div>
    );
  }

  const maxVal = Math.max(...data.map((d) => d.value), 1);
  const width = 600;
  const paddingX = 30;
  const paddingY = 25;
  const barWidth = Math.min((width - paddingX * 2) / data.length - 12, 36);

  return (
    <div id={id} style={{ width: '100%', overflowX: 'auto' }}>
      <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
        {data.map((item, idx) => {
          const barHeight = (item.value / maxVal) * (height - paddingY * 2);
          const x = paddingX + idx * ((width - paddingX * 2) / data.length) + 8;
          const y = height - paddingY - barHeight;

          return (
            <g key={idx}>
              <rect
                x={x}
                y={y}
                width={barWidth}
                height={barHeight}
                rx="4"
                fill={color}
                opacity="0.85"
                style={{ transition: 'opacity 0.2s' }}
              />
              <text x={x + barWidth / 2} y={y - 6} textAnchor="middle" fill="var(--text-muted)" fontSize="11" fontWeight="600">
                {item.value}{unit}
              </text>
              <text x={x + barWidth / 2} y={height - 8} textAnchor="middle" fill="var(--text-dim)" fontSize="10">
                {item.label}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
};
