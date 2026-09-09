import React from 'react';

interface EmptyStateProps {
  id?: string;
  icon?: React.ReactNode;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  id,
  icon,
  title,
  description,
  action,
}) => {
  return (
    <div
      id={id}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '3.5rem 1.5rem',
        background: 'rgba(255, 255, 255, 0.02)',
        borderRadius: 'var(--radius-lg)',
        border: '1px dashed var(--border-subtle)',
      }}
    >
      {icon ? (
        <div style={{ marginBottom: '1rem', color: 'var(--text-dim)' }}>{icon}</div>
      ) : (
        <div
          style={{
            width: '48px',
            height: '48px',
            borderRadius: '50%',
            background: 'rgba(255, 255, 255, 0.05)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: '1rem',
            color: 'var(--text-dim)',
          }}
        >
          📂
        </div>
      )}
      <h4 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>{title}</h4>
      <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', marginBottom: action ? '1.25rem' : 0 }}>
        {description}
      </p>
      {action && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={action.onClick}>
          {action.label}
        </button>
      )}
    </div>
  );
};
