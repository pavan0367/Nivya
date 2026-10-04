import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Lock, Mail, ArrowRight, AlertCircle, CheckCircle, Loader2 } from 'lucide-react';
import { authService } from '../../services/authService';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState((location.state as any)?.registeredEmail || '');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>((location.state as any)?.successMessage || null);
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null);

  useEffect(() => {
    if ((location.state as any)?.registeredEmail) {
      setEmail((location.state as any).registeredEmail);
    }
    if ((location.state as any)?.successMessage) {
      setNotice((location.state as any).successMessage);
    }
  }, [location.state]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (loading) return; // Prevent duplicate concurrent submissions

    const trimmedEmail = email.trim();
    if (!trimmedEmail || !password) {
      setError('Please enter both email and password.');
      return;
    }

    setLoading(true);
    setError(null);
    setUnverifiedEmail(null);

    try {
      const data = await authService.login(trimmedEmail, password);
      const userRole = data.user.role;

      // Update pairing status asynchronously in background without blocking navigation
      authService.getPairingStatus().catch((pairErr) => {
        console.debug('Background pairing status update:', pairErr);
      });

      // Role-specific navigation executes immediately with zero blocking
      if (userRole === 'ADMIN') {
        navigate('/admin', { replace: true });
        return;
      }

      if (userRole === 'CHILD') {
        navigate('/child', { replace: true });
        return;
      }

      // PARENT LOGIN: Enter role-specific dashboard (/dashboard) immediately
      const origin = (location.state as any)?.from?.pathname;
      const target = (origin && origin !== '/access-denied' && !origin.startsWith('/child') && origin !== '/pairing') ? origin : '/dashboard';
      navigate(target, { replace: true });
    } catch (err: any) {
      console.error('Login failed:', err);

      // Explicit error differentiation based on real root cause
      if (err.code === 'ECONNABORTED' || err.message?.toLowerCase().includes('timeout')) {
        setError('Server response timed out. The backend is busy or experiencing high latency. Please retry in a moment.');
      } else if (!err.response && (err.message?.toLowerCase().includes('network') || !navigator.onLine)) {
        setError('Network connection error. Unable to reach Nivya servers. Please verify your internet connection.');
      } else if (err.response?.status === 401) {
        const msg = err.response?.data?.message || 'Invalid email or password. Please check your credentials.';
        setError(msg);
        if (msg.toLowerCase().includes('not verified') || msg.toLowerCase().includes('verify your email')) {
          setUnverifiedEmail(trimmedEmail.toLowerCase());
        }
      } else if (err.response?.status === 429) {
        setError('Too many failed login attempts. Please wait 15 minutes before trying again.');
      } else if (err.response?.status === 502 || err.response?.status === 503 || err.response?.status === 504) {
        setError('Nivya server is temporarily unavailable or warming up. Please retry in a few seconds.');
      } else if (err.response?.status >= 500) {
        setError('A server error occurred while processing your request. Please try again shortly.');
      } else {
        const fallbackMsg = err.response?.data?.message ||
          'Authentication failed. Please check your credentials and try again.';
        setError(fallbackMsg);
        if (fallbackMsg.toLowerCase().includes('not verified') || fallbackMsg.toLowerCase().includes('verify your email')) {
          setUnverifiedEmail(trimmedEmail.toLowerCase());
        }
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
      <div style={{ marginBottom: '1.75rem' }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff' }}>Sign In</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
          Access your family management console
        </p>
      </div>

      {notice && (
        <div
          id="login-notice-alert"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.65rem',
            padding: '0.85rem 1rem',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(16, 185, 129, 0.12)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            color: 'var(--success)',
            fontSize: '0.85rem',
            marginBottom: '1.5rem',
          }}
        >
          <CheckCircle size={18} style={{ flexShrink: 0 }} />
          <span>{notice}</span>
        </div>
      )}

      {error && (
        <div
          id="login-error-alert"
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
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <span>{error}</span>
            {unverifiedEmail && (
              <Link
                to={`/verify-email?email=${encodeURIComponent(unverifiedEmail)}`}
                id="link-verify-unverified"
                style={{ color: '#fff', fontWeight: 600, textDecoration: 'underline', fontSize: '0.82rem' }}
              >
                Click here to verify your email address &rarr;
              </Link>
            )}
          </div>
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
              placeholder="user@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
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
              disabled={loading}
              required
            />
          </div>
        </div>

        <button
          type="submit"
          id="btn-login-submit"
          className="btn btn-primary"
          disabled={loading}
          style={{
            width: '100%',
            marginTop: '0.5rem',
            padding: '0.75rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.5rem',
            opacity: loading ? 0.75 : 1,
            cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          {loading ? (
            <>
              <Loader2 size={17} style={{ animation: 'spin 0.9s linear infinite' }} />
              <span>Authenticating...</span>
            </>
          ) : (
            <>
              <span>Sign In to Console</span>
              <ArrowRight size={17} />
            </>
          )}
        </button>

        {/* Link to Register */}
        <div style={{ marginTop: '0.5rem', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Don't have an account?{' '}
          <Link
            to="/register"
            id="link-to-register"
            style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}
          >
            Create Account
          </Link>
        </div>
      </form>
    </div>
  );
};

export default LoginPage;
