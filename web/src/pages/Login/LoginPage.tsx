import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Lock, Mail, ArrowRight, UserCheck, AlertCircle } from 'lucide-react';
import { authService } from '../../services/authService';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('parent@nivya.local');
  const [password, setPassword] = useState('Password123!');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please enter both email and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await authService.login(email, password);
      if (data.user.role === 'PARENT') {
        const origin = (location.state as any)?.from?.pathname || '/dashboard';
        navigate(origin, { replace: true });
      } else {
        // Child user logged in
        navigate('/access-denied', { replace: true });
      }
    } catch (err: any) {
      console.error('Login failed:', err);
      setError(
        err.response?.data?.message ||
        'Authentication failed. Please check your credentials and try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemoParent = () => {
    setEmail('parent@nivya.local');
    setPassword('Password123!');
    setError(null);
  };

  const handleFillDemoChild = () => {
    setEmail('child@nivya.local');
    setPassword('Password123!');
    setError(null);
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
      <div style={{ marginBottom: '1.75rem' }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff' }}>Sign In</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
          Access your family management console
        </p>
      </div>

      {error && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.65rem',
            padding: '0.85rem 1rem',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            color: 'var(--danger)',
            fontSize: '0.85rem',
            marginBottom: '1.5rem',
          }}
        >
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div>
          <label
            htmlFor="input-email"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.45rem' }}
          >
            Email Address
          </label>
          <div style={{ position: 'relative' }}>
            <Mail
              size={18}
              color="var(--text-dim)"
              style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              id="input-email"
              type="email"
              className="form-input"
              style={{ paddingLeft: '2.75rem' }}
              placeholder="parent@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
        </div>

        <div>
          <label
            htmlFor="input-password"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.45rem' }}
          >
            Password
          </label>
          <div style={{ position: 'relative' }}>
            <Lock
              size={18}
              color="var(--text-dim)"
              style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              id="input-password"
              type="password"
              className="form-input"
              style={{ paddingLeft: '2.75rem' }}
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
        </div>

        <button
          type="submit"
          id="btn-login-submit"
          className="btn btn-primary"
          disabled={loading}
          style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem' }}
        >
          {loading ? (
            'Authenticating...'
          ) : (
            <>
              Sign In to Console
              <ArrowRight size={17} />
            </>
          )}
        </button>
      </form>

      {/* Quick Role Fill Presets */}
      <div
        style={{
          marginTop: '1.75rem',
          paddingTop: '1.25rem',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.65rem',
        }}
      >
        <span style={{ fontSize: '0.775rem', color: 'var(--text-dim)', textAlign: 'center', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          Quick Demo Credentials
        </span>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.65rem' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={handleFillDemoParent}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.35rem' }}
          >
            <UserCheck size={14} color="var(--primary)" />
            Parent Demo
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={handleFillDemoChild}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.35rem' }}
          >
            <UserCheck size={14} color="var(--warning)" />
            Child Demo
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
