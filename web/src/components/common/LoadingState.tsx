import React from 'react';

interface LoadingSpinnerProps {
  text?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ text = 'Loading...', size = 'md' }) => {
  const dim = size === 'sm' ? 20 : size === 'lg' ? 44 : 30;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.75rem', padding: '2rem' }}>
      <svg
        width={dim}
        height={dim}
        viewBox="0 0 24 24"
        fill="none"
        stroke="var(--primary)"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        style={{ animation: 'spin 0.9s linear infinite' }}
      >
        <path d="M21 12a9 9 0 1 1-6.219-8.56" />
      </svg>
      {text && <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>{text}</span>}
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};

export const SkeletonCard: React.FC<{ height?: number }> = ({ height = 140 }) => (
  <div className="skeleton" style={{ height: `${height}px`, width: '100%', borderRadius: 'var(--radius-lg)' }} />
);
