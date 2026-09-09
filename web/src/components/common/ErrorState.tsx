import React from 'react';

interface ErrorStateProps {
  id?: string;
  title?: string;
  message?: string;
  onRetry?: () => void;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  id,
  title = 'Failed to load information',
  message = 'An error occurred while connecting to the Nivya backend service.',
  onRetry,
}) => {
  return (
    <div
      id={id}
      style={{
        padding: '1.5rem',
        borderRadius: 'var(--radius-md)',
        background: 'rgba(239, 68, 68, 0.08)',
        border: '1px solid rgba(239, 68, 68, 0.25)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '1rem',
        margin: '1rem 0',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
        <span style={{ fontSize: '1.35rem' }}>⚠️</span>
        <div>
          <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--danger)' }}>{title}</h4>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>{message}</p>
        </div>
      </div>
      {onRetry && (
        <button type="button" className="btn btn-danger btn-sm" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
};

export const ErrorBanner: React.FC<ErrorStateProps> = ErrorState;

