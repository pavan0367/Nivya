import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, LogOut } from 'lucide-react';
import { authService } from '../services/authService';

export const AccessDeniedPage: React.FC = () => {
  const navigate = useNavigate();
  const user = authService.getCurrentUser();

  const handleLogout = () => {
    authService.logout();
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        width: '100vw',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1.5rem',
        background: 'radial-gradient(ellipse at center, #1f1224 0%, #0c0914 60%, #050409 100%)',
      }}
    >
      <div
        className="glass-panel"
        style={{
          maxWidth: '520px',
          width: '100%',
          padding: '2.5rem',
          textAlign: 'center',
          borderRadius: 'var(--radius-lg)',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.5)',
          border: '1px solid rgba(239, 68, 68, 0.25)',
        }}
      >
        <div
          style={{
            width: '64px',
            height: '64px',
            margin: '0 auto 1.5rem',
            borderRadius: '50%',
            background: 'rgba(239, 68, 68, 0.15)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--danger)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
          }}
        >
          <ShieldAlert size={36} />
        </div>

        <h2 style={{ fontSize: '1.6rem', fontWeight: 700, marginBottom: '0.75rem', color: '#fff' }}>
          Access Restricted: Parent Only
        </h2>

        <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '1.5rem' }}>
          The requested section contains sensitive telemetry, real-time activity oversight, and parental supervision controls.
          {user ? (
            <>
              {' '}Your account is currently signed in as a <strong style={{ color: 'var(--warning)' }}>{user.role}</strong> user.
            </>
          ) : (
            ' Please sign in with an authorized Parent account.'
          )}
        </p>

        <div
          style={{
            background: 'rgba(255, 255, 255, 0.03)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            marginBottom: '2rem',
            textAlign: 'left',
            fontSize: '0.85rem',
            color: 'var(--text-dim)',
          }}
        >
          🔒 <strong>Privacy Policy Enforcement:</strong> Nivya strictly enforces role separation at both API and UI boundary layers to guarantee child privacy within family consent agreements.
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate('/role-selection')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <ArrowLeft size={16} />
            Switch Role
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleLogout}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <LogOut size={16} />
            Sign Out
          </button>
        </div>
      </div>
    </div>
  );
};

export default AccessDeniedPage;
