import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, User, ArrowRight, ShieldAlert, Check } from 'lucide-react';
import { authService } from '../../services/authService';
import { RoleType } from '../../types/auth';

export const RoleSelectPage: React.FC = () => {
  const navigate = useNavigate();
  const [selectedRole, setSelectedRole] = useState<RoleType>('PARENT');
  const [loading, setLoading] = useState(false);

  const handleConfirmRole = async () => {
    setLoading(true);
    try {
      if (authService.isAuthenticated()) {
        try {
          await authService.selectRole(selectedRole);
        } catch {
          // In demo or test mode if endpoint is unavailable, store locally
          localStorage.setItem('nivya_user_role', selectedRole);
        }
      } else {
        localStorage.setItem('nivya_user_role', selectedRole);
      }

      if (selectedRole === 'PARENT') {
        navigate('/pairing');
      } else {
        navigate('/access-denied');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="glass-panel"
      style={{
        width: '100%',
        padding: '2.25rem',
        borderRadius: 'var(--radius-lg)',
        boxShadow: '0 16px 40px rgba(0, 0, 0, 0.4)',
      }}
    >
      <div style={{ marginBottom: '1.75rem', textAlign: 'center' }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff' }}>Select Account Role</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
          Choose your interface access tier
        </p>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '2rem' }}>
        {/* Parent Option Card */}
        <div
          id="card-role-parent"
          onClick={() => setSelectedRole('PARENT')}
          style={{
            padding: '1.25rem',
            borderRadius: 'var(--radius-md)',
            border: selectedRole === 'PARENT' ? '2px solid var(--primary)' : '1px solid var(--border-subtle)',
            background: selectedRole === 'PARENT' ? 'rgba(99, 102, 241, 0.12)' : 'rgba(255, 255, 255, 0.03)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            transition: 'all 0.2s ease',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                background: 'rgba(99, 102, 241, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary)',
              }}
            >
              <Users size={22} />
            </div>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '1rem' }}>Parent</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Full telemetry, live activity oversight, history & alerts
              </div>
            </div>
          </div>
          {selectedRole === 'PARENT' && (
            <div
              style={{
                width: '24px',
                height: '24px',
                borderRadius: '50%',
                background: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
              }}
            >
              <Check size={14} />
            </div>
          )}
        </div>

        {/* Child Option Card */}
        <div
          id="card-role-child"
          onClick={() => setSelectedRole('CHILD')}
          style={{
            padding: '1.25rem',
            borderRadius: 'var(--radius-md)',
            border: selectedRole === 'CHILD' ? '2px solid var(--warning)' : '1px solid var(--border-subtle)',
            background: selectedRole === 'CHILD' ? 'rgba(245, 158, 11, 0.1)' : 'rgba(255, 255, 255, 0.03)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            transition: 'all 0.2s ease',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '10px',
                background: 'rgba(245, 158, 11, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--warning)',
              }}
            >
              <User size={22} />
            </div>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '1rem' }}>Child</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Android client focused; dashboard routes strictly restricted
              </div>
            </div>
          </div>
          {selectedRole === 'CHILD' && (
            <div
              style={{
                width: '24px',
                height: '24px',
                borderRadius: '50%',
                background: 'var(--warning)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
              }}
            >
              <Check size={14} />
            </div>
          )}
        </div>
      </div>

      {selectedRole === 'CHILD' && (
        <div
          style={{
            display: 'flex',
            gap: '0.65rem',
            padding: '0.85rem',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(245, 158, 11, 0.1)',
            border: '1px solid rgba(245, 158, 11, 0.25)',
            color: 'var(--warning)',
            fontSize: '0.825rem',
            marginBottom: '1.5rem',
          }}
        >
          <ShieldAlert size={18} style={{ flexShrink: 0 }} />
          <span>
            Notice: Choosing the Child role will block access to the web dashboard and redirect to the privacy enforcement notice.
          </span>
        </div>
      )}

      <button
        type="button"
        id="btn-confirm-role"
        className="btn btn-primary"
        disabled={loading}
        onClick={handleConfirmRole}
        style={{ width: '100%', padding: '0.75rem' }}
      >
        {loading ? (
          'Setting Role...'
        ) : (
          <>
            Proceed as {selectedRole === 'PARENT' ? 'Parent' : 'Child'}
            <ArrowRight size={17} />
          </>
        )}
      </button>
    </div>
  );
};

export default RoleSelectPage;
