import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { ShieldCheck, Mail, ArrowRight, AlertCircle, CheckCircle, RefreshCw } from 'lucide-react';
import { authService } from '../../services/authService';

export const VerifyEmailPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const queryEmail = new URLSearchParams(location.search).get('email') || '';
  const stateEmail = (location.state as any)?.email || '';
  const initialEmail = queryEmail || stateEmail || '';

  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(60);
  const [error, setError] = useState<string | null>(null);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  // Cooldown timer for resend code
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessNotice(null);

    const cleanCode = code.trim();
    if (!cleanCode || cleanCode.length < 6) {
      setError('Please enter the complete 6-digit verification code.');
      return;
    }

    if (!email.trim()) {
      setError('Please enter the email address to verify.');
      return;
    }

    setLoading(true);

    try {
      await authService.verifyEmail(email.trim().toLowerCase(), cleanCode);
      setSuccessNotice('Email successfully verified! Redirecting to sign in...');

      setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: {
            registeredEmail: email.trim().toLowerCase(),
            successMessage: 'Your email address has been verified. You may now sign in.',
          },
        });
      }, 1200);
    } catch (err: any) {
      console.error('Email verification error:', err);
      const serverMessage = err.response?.data?.message;
      setError(
        serverMessage || 'Invalid, expired, or exhausted verification code. Please check and try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0 || resending || !email.trim()) return;

    setResending(true);
    setError(null);
    try {
      await authService.sendVerificationCode(email.trim().toLowerCase());
      setSuccessNotice('A new verification code has been dispatched to your email.');
      setResendCooldown(60);
    } catch (err: any) {
      console.error('Failed to resend code:', err);
      setError(err.response?.data?.message || 'Failed to resend verification code. Please try again later.');
    } finally {
      setResending(false);
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
      <div style={{ textAlign: 'center', marginBottom: '1.75rem' }}>
        <div
          style={{
            width: '56px',
            height: '56px',
            borderRadius: '50%',
            background: 'rgba(99, 102, 241, 0.15)',
            border: '1px solid rgba(99, 102, 241, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1rem',
            color: 'var(--primary)',
          }}
        >
          <ShieldCheck size={28} />
        </div>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#fff' }}>Verify Your Email</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.35rem' }}>
          We've sent a 6-digit verification code to
        </p>
        <p style={{ color: 'var(--primary)', fontWeight: 600, fontSize: '0.9rem', marginTop: '0.15rem' }}>
          {email || 'your email address'}
        </p>
      </div>

      {error && (
        <div
          id="verify-error-alert"
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

      {successNotice && (
        <div
          id="verify-success-alert"
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
          <span>{successNotice}</span>
        </div>
      )}

      <form onSubmit={handleVerify} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Email Address (editable if not provided) */}
        {!initialEmail && (
          <div>
            <label
              htmlFor="input-verify-email"
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
                id="input-verify-email"
                type="email"
                className="form-input"
                style={{ paddingLeft: '2.75rem' }}
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={loading || !!successNotice}
              />
            </div>
          </div>
        )}

        {/* 6-Digit Code Input */}
        <div>
          <label
            htmlFor="input-verify-code"
            style={{ display: 'block', fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '0.4rem' }}
          >
            6-Digit Verification Code
          </label>
          <input
            id="input-verify-code"
            type="text"
            className="form-input"
            maxLength={6}
            placeholder="• • • • • •"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            style={{
              textAlign: 'center',
              letterSpacing: '0.45rem',
              fontSize: '1.35rem',
              fontWeight: 700,
              padding: '0.75rem',
            }}
            required
            autoFocus
            disabled={loading || !!successNotice}
          />
        </div>

        {/* Verify Submit Button */}
        <button
          type="submit"
          id="btn-confirm-verification"
          className="btn btn-primary"
          disabled={loading || !!successNotice || code.length < 6}
          style={{ width: '100%', marginTop: '0.35rem', padding: '0.75rem' }}
        >
          {loading ? (
            'Verifying...'
          ) : (
            <>
              Verify & Activate Account
              <ArrowRight size={17} />
            </>
          )}
        </button>

        {/* Resend Code Action */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginTop: '0.5rem', fontSize: '0.85rem' }}>
          <span style={{ color: 'var(--text-muted)' }}>Didn't receive the code?</span>
          <button
            type="button"
            id="btn-resend-code"
            onClick={handleResend}
            disabled={resendCooldown > 0 || resending || !!successNotice}
            style={{
              background: 'none',
              border: 'none',
              color: resendCooldown > 0 ? 'var(--text-dim)' : 'var(--primary)',
              fontWeight: 600,
              cursor: resendCooldown > 0 ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.3rem',
              padding: 0,
            }}
          >
            <RefreshCw size={13} className={resending ? 'animate-spin' : ''} />
            {resendCooldown > 0 ? `Resend code in ${resendCooldown}s` : 'Resend code'}
          </button>
        </div>

        {/* Link back to login */}
        <div style={{ marginTop: '0.75rem', textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          <Link
            to="/login"
            id="link-verify-to-login"
            style={{ color: 'var(--text-muted)', textDecoration: 'none' }}
          >
            ← Return to Sign In
          </Link>
        </div>
      </form>
    </div>
  );
};

export default VerifyEmailPage;
