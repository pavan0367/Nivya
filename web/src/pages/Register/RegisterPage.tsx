import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Lock, Mail, User as UserIcon, Shield, ArrowRight, AlertCircle, CheckCircle } from 'lucide-react';
import { authService } from '../../services/authService';
import { RoleType } from '../../types/auth';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState<RoleType>('PARENT');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // 1. Client-side validations
    if (!name.trim() || name.trim().length < 2) {
      setError('Name must be at least 2 characters.');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);

    try {
      await authService.register(name.trim(), email.trim().toLowerCase(), password, role);

      // Clear auto-saved auth tokens so user explicitly logs in on Login page
      localStorage.removeItem('nivya_access_token');
      localStorage.removeItem('nivya_refresh_token');
      localStorage.removeItem('nivya_user_role');
      localStorage.removeItem('nivya_user');

      setSuccessMessage('Account created successfully! Redirecting to sign in...');

      setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: {
            registeredEmail: email.trim().toLowerCase(),
            successMessage: 'Account created successfully! Please sign in with your credentials.',
          },
        });
      }, 1200);
    } catch (err: any) {
      console.error('Registration failed:', err);
      const serverMessage = err.response?.data?.message;
      if (serverMessage) {
        setError(serverMessage);
      } else if (err.response?.status === 409) {
        setError(`An account with email '${email.trim()}' is already registered.`);
      } else {
        setError('Registration failed. Please verify your details and try again.');
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
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff' }}>Create Account</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
          Register for your Nivya family safety console
        </p>
      </div>

      {error && (
        <div
          id="register-error-alert"
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

      {successMessage && (
        <div
          id="register-success-alert"
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
          <span>{successMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.15rem' }}>
        {/* Full Name */}
        <div>
          <label
            htmlFor="input-name"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.4rem' }}
          >
            Full Name
          </label>
          <div style={{ position: 'relative' }}>
            <UserIcon
              size={18}
              color="var(--text-dim)"
              style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              id="input-name"
              type="text"
              className="form-input"
              style={{ paddingLeft: '2.75rem' }}
              placeholder="e.g. Sarah Jenkins"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              disabled={loading || !!successMessage}
            />
          </div>
        </div>

        {/* Email Address */}
        <div>
          <label
            htmlFor="input-email"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.4rem' }}
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
              disabled={loading || !!successMessage}
            />
          </div>
        </div>

        {/* Account Role Selector */}
        <div>
          <label
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.4rem' }}
          >
            Account Role
          </label>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '0.5rem',
              background: 'rgba(15, 23, 42, 0.6)',
              padding: '0.3rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <button
              type="button"
              id="btn-role-parent"
              onClick={() => setRole('PARENT')}
              style={{
                padding: '0.55rem',
                border: 'none',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.85rem',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.4rem',
                background: role === 'PARENT' ? 'var(--primary)' : 'transparent',
                color: role === 'PARENT' ? '#fff' : 'var(--text-muted)',
                transition: 'all var(--transition-fast)',
              }}
            >
              <Shield size={15} />
              Parent
            </button>
            <button
              type="button"
              id="btn-role-child"
              onClick={() => setRole('CHILD')}
              style={{
                padding: '0.55rem',
                border: 'none',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.85rem',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.4rem',
                background: role === 'CHILD' ? 'var(--primary)' : 'transparent',
                color: role === 'CHILD' ? '#fff' : 'var(--text-muted)',
                transition: 'all var(--transition-fast)',
              }}
            >
              Child
            </button>
          </div>
        </div>

        {/* Password */}
        <div>
          <label
            htmlFor="input-password"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.4rem' }}
          >
            Password (min. 8 characters)
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
              minLength={8}
              disabled={loading || !!successMessage}
            />
          </div>
        </div>

        {/* Confirm Password */}
        <div>
          <label
            htmlFor="input-confirm-password"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.4rem' }}
          >
            Confirm Password
          </label>
          <div style={{ position: 'relative' }}>
            <Lock
              size={18}
              color="var(--text-dim)"
              style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }}
            />
            <input
              id="input-confirm-password"
              type="password"
              className="form-input"
              style={{ paddingLeft: '2.75rem' }}
              placeholder="••••••••"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={8}
              disabled={loading || !!successMessage}
            />
          </div>
        </div>

        {/* Submit button */}
        <button
          type="submit"
          id="btn-register-submit"
          className="btn btn-primary"
          disabled={loading || !!successMessage}
          style={{ width: '100%', marginTop: '0.4rem', padding: '0.75rem' }}
        >
          {loading ? (
            'Creating Account...'
          ) : (
            <>
              Register Account
              <ArrowRight size={17} />
            </>
          )}
        </button>

        {/* Link back to Sign In */}
        <div style={{ marginTop: '0.75rem', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link
            to="/login"
            id="link-to-login"
            style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}
          >
            Sign In
          </Link>
        </div>
      </form>
    </div>
  );
};

export default RegisterPage;
