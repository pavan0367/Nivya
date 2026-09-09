import React from 'react';

interface MetricCardProps {
  id?: string;
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ReactNode;
  badge?: {
    text: string;
    variant: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  };
  trend?: {
    value: string;
    isPositive: boolean;
  };
  className?: string;
  onClick?: () => void;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  id,
  title,
  value,
  subtitle,
  icon,
  badge,
  trend,
  className = '',
  onClick,
}) => {
  return (
    <div
      id={id}
      className={`glass-panel ${className}`}
      style={{
        padding: '1.5rem',
        cursor: onClick ? 'pointer' : 'default',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
      onClick={onClick}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
        <div>
          <span style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            {title}
          </span>
          <div style={{ fontSize: '1.85rem', fontWeight: 700, marginTop: '0.35rem', color: 'var(--text-main)' }}>
            {value}
          </div>
        </div>
        {icon && (
          <div
            style={{
              padding: '0.65rem',
              borderRadius: 'var(--radius-md)',
              background: 'rgba(99, 102, 241, 0.12)',
              color: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {icon}
          </div>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto', gap: '0.5rem' }}>
        {subtitle && <span style={{ fontSize: '0.825rem', color: 'var(--text-dim)' }}>{subtitle}</span>}
        {badge && <span className={`badge badge-${badge.variant}`}>{badge.text}</span>}
        {trend && (
          <span style={{ fontSize: '0.825rem', color: trend.isPositive ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}>
            {trend.isPositive ? '↑ ' : '↓ '}
            {trend.value}
          </span>
        )}
      </div>
    </div>
  );
};

interface ContentCardProps {
  id?: string;
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

export const ContentCard: React.FC<ContentCardProps> = ({
  id,
  title,
  subtitle,
  action,
  children,
  className = '',
}) => {
  return (
    <div id={id} className={`glass-panel ${className}`} style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
        <div>
          <h3 style={{ fontSize: '1.15rem', fontWeight: 600 }}>{title}</h3>
          {subtitle && <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>{subtitle}</p>}
        </div>
        {action && <div>{action}</div>}
      </div>
      <div>{children}</div>
    </div>
  );
};
