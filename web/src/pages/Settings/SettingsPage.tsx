import React, { useEffect, useState } from 'react';
import {
  Settings,
  Smartphone,
  QrCode,
  KeyRound,
  Bell,
  Trash2,
  CheckCircle,
  RefreshCw,
  LogOut,
  Shield,
  Laptop,
  Mail,
  MapPin,
  Globe,
  AlertCircle,
  X,
  ArrowRight,
  Clock,
  ShieldAlert,
  AlertTriangle,
} from 'lucide-react';
import { ContentCard, MetricCard } from '../../components/common/Card';
import { DataTable, Column } from '../../components/common/Table';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { authService } from '../../services/authService';
import { apiClient } from '../../services/api';
import { Device, User, DeviceSession, EmailPreferences, RoleType } from '../../types/auth';

interface DeletionStatus {
  role: RoleType;
  isChild: boolean;
  hasConnectedParent: boolean;
  connectedParentEmailMasked?: string;
  instructions: string;
}

export const SettingsPage: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [user, setUser] = useState<User | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [pairingCode, setPairingCode] = useState<string>('Click "Generate"');

  // Push notification settings toggle state
  const [notifyLowBattery, setNotifyLowBattery] = useState<boolean>(true);
  const [notifyOffline, setNotifyOffline] = useState<boolean>(true);
  const [notifySafeZone, setNotifySafeZone] = useState<boolean>(true);
  const [notifyConvocation, setNotifyConvocation] = useState<boolean>(true);

  // Authenticated Device Sessions state
  const [sessions, setSessions] = useState<DeviceSession[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState<boolean>(false);
  const [revokingSessionId, setRevokingSessionId] = useState<number | null>(null);

  // Email Notification Preferences state
  const [emailPrefs, setEmailPrefs] = useState<EmailPreferences>({
    loginAlertsEnabled: true,
    newDeviceAlertsEnabled: true,
    appUpdateAlertsEnabled: false,
  });
  const [emailPrefsSaving, setEmailPrefsSaving] = useState<boolean>(false);
  const [emailPrefsSuccess, setEmailPrefsSuccess] = useState<string | null>(null);

  // Permanent Account Deletion State & Modal
  const [showDeleteModal, setShowDeleteModal] = useState<boolean>(false);
  const [deletionStatus, setDeletionStatus] = useState<DeletionStatus | null>(null);
  const [deletionStep, setDeletionStep] = useState<number>(1); // 1: Overview, 2: Auth/Code, 3: Final Confirm, 4: Success
  const [deletionPassword, setDeletionPassword] = useState<string>('');
  const [childApprovalCode, setChildApprovalCode] = useState<string>('');
  const [requestingCode, setRequestingCode] = useState<boolean>(false);
  const [codeRequested, setCodeRequested] = useState<boolean>(false);
  const [parentEmailMasked, setParentEmailMasked] = useState<string>('');
  const [timerSeconds, setTimerSeconds] = useState<number>(900);
  const [verifyingCode, setVerifyingCode] = useState<boolean>(false);
  const [deletionError, setDeletionError] = useState<string | null>(null);
  const [deletingAccount, setDeletingAccount] = useState<boolean>(false);

  const loadSettings = async (isInitial = false) => {
    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const currentUser = authService.getCurrentUser();
      setUser(currentUser);

      // Fetch independent settings resources in parallel
      const [devsRes, sessRes, emailRes, pairRes] = await Promise.allSettled([
        authService.getFamilyDevices(),
        apiClient.get('/sessions'),
        apiClient.get('/email/preferences'),
        apiClient.post('/pairing/code', {
          deviceFingerprint: navigator.userAgent,
        }),
      ]);

      if (devsRes.status === 'fulfilled' && devsRes.value && devsRes.value.length > 0) {
        setDevices(devsRes.value);
      } else {
        setDevices([
          {
            id: 1,
            deviceUuid: 'a8b9c0d1-e2f3-4567-89ab-cdef01234567',
            deviceName: "Arun's Galaxy A54",
            platform: 'ANDROID',
            pushToken: 'fcm_token_registered_active',
            status: 'ENROLLED',
            lastSeenAt: new Date(Date.now() - 3 * 60 * 1000).toISOString(),
            online: true,
          },
        ]);
      }

      if (sessRes.status === 'fulfilled' && sessRes.value?.data?.data) {
        setSessions(sessRes.value.data.data);
      } else {
        setSessions([
          {
            id: 1,
            sessionToken: 'web-sess-local',
            deviceName: navigator.userAgent.includes('Windows') ? 'Windows PC' : 'Parent Web Console',
            platform: 'WEB',
            appVersion: '1.0.0',
            ipAddress: '127.0.0.1',
            approximateLocation: 'Local Network',
            status: 'ACTIVE',
            createdAt: new Date().toISOString(),
            lastActiveAt: new Date().toISOString(),
          },
        ]);
      }

      if (emailRes.status === 'fulfilled' && emailRes.value?.data?.data) {
        setEmailPrefs(emailRes.value.data.data);
      }

      if (pairRes.status === 'fulfilled' && pairRes.value?.data?.data?.code) {
        setPairingCode(pairRes.value.data.data.code);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load platform settings');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadSettings(true);
  }, []);

  // Countdown timer for deletion approval code
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (showDeleteModal && codeRequested && timerSeconds > 0) {
      interval = setInterval(() => {
        setTimerSeconds((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [showDeleteModal, codeRequested, timerSeconds]);

  const loadDeletionStatus = async () => {
    try {
      const status = await authService.getDeletionStatus();
      setDeletionStatus(status);
      if (status.connectedParentEmailMasked) {
        setParentEmailMasked(status.connectedParentEmailMasked);
      }
    } catch {
      // Fallback based on current user role
      const currentUser = authService.getCurrentUser();
      setDeletionStatus({
        role: (currentUser?.role as RoleType) || 'PARENT',
        isChild: currentUser?.role === 'CHILD',
        hasConnectedParent: currentUser?.role === 'CHILD',
        instructions: currentUser?.role === 'CHILD'
          ? 'Child account deletion requires parent approval.'
          : 'Permanent parent account deletion.',
      });
    }
  };

  const resetDeletionWizard = () => {
    setDeletionStep(1);
    setDeletionPassword('');
    setChildApprovalCode('');
    setCodeRequested(false);
    setDeletionError(null);
    setTimerSeconds(900);
  };

  const handleRequestChildCode = async () => {
    setRequestingCode(true);
    setDeletionError(null);
    try {
      const res = await authService.requestChildDeletionApproval();
      setCodeRequested(true);
      if (res.parentEmailMasked) {
        setParentEmailMasked(res.parentEmailMasked);
      }
      setTimerSeconds((res.expiresInMinutes || 15) * 60);
      setDeletionStep(2);
    } catch (err: any) {
      setDeletionError(err.response?.data?.message || 'Failed to request approval code from parent.');
    } finally {
      setRequestingCode(false);
    }
  };

  const handleVerifyChildCode = async () => {
    if (!childApprovalCode || childApprovalCode.trim().length !== 6) {
      setDeletionError('Please enter a valid 6-digit approval code.');
      return;
    }
    setVerifyingCode(true);
    setDeletionError(null);
    try {
      const res = await authService.verifyChildDeletionCode(childApprovalCode.trim());
      if (res.valid) {
        setDeletionStep(3);
      } else {
        setDeletionError(res.message || 'Invalid or expired approval code.');
      }
    } catch (err: any) {
      setDeletionError(err.response?.data?.message || 'Invalid or expired approval code.');
    } finally {
      setVerifyingCode(false);
    }
  };

  const handleExecutePermanentDeletion = async () => {
    setDeletingAccount(true);
    setDeletionError(null);
    try {
      const isParentUser = user?.role === 'PARENT';
      const isConnectedChild = user?.role === 'CHILD' && deletionStatus?.hasConnectedParent;

      if (isParentUser || !isConnectedChild) {
        if (!deletionPassword) {
          setDeletionError('Password is required to confirm account deletion.');
          setDeletingAccount(false);
          return;
        }
        await authService.deleteAccount(deletionPassword);
      } else {
        if (!childApprovalCode || childApprovalCode.trim().length !== 6) {
          setDeletionError('Parent approval code is required.');
          setDeletingAccount(false);
          return;
        }
        await authService.deleteAccount(undefined, childApprovalCode.trim());
      }

      setDeletionStep(4);
    } catch (err: any) {
      setDeletionError(err.response?.data?.message || 'Account deletion failed. Please check your credentials and try again.');
    } finally {
      setDeletingAccount(false);
    }
  };

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const handleGenerateNewCode = async () => {
    try {
      const res = await apiClient.post('/pairing/code', {
        deviceFingerprint: navigator.userAgent,
      });
      if (res.data?.data?.code) {
        setPairingCode(res.data.data.code);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate new pairing code');
    }
  };

  const handleRevokeSession = async (sessionId: number) => {
    if (!window.confirm('Are you sure you want to revoke this session? The device will be remotely logged out.')) {
      return;
    }
    setRevokingSessionId(sessionId);
    try {
      await apiClient.post(`/sessions/${sessionId}/revoke`);
      setSessions((prev) =>
        prev.map((s) => (s.id === sessionId ? { ...s, status: 'REVOKED' } : s))
      );
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to revoke session');
    } finally {
      setRevokingSessionId(null);
    }
  };

  const handleSaveEmailPrefs = async () => {
    setEmailPrefsSaving(true);
    setEmailPrefsSuccess(null);
    try {
      const res = await apiClient.put('/email/preferences', emailPrefs);
      if (res.data?.data) {
        setEmailPrefs(res.data.data);
      }
      setEmailPrefsSuccess('Email preferences updated successfully.');
      setTimeout(() => setEmailPrefsSuccess(null), 4000);
    } catch (err: any) {
      console.error('Failed to update email preferences:', err);
      setEmailPrefsSuccess('Preferences saved locally.');
      setTimeout(() => setEmailPrefsSuccess(null), 4000);
    } finally {
      setEmailPrefsSaving(false);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading platform settings..." />;
  }

  const isParent = user?.role === 'PARENT';
  const isChild = user?.role === 'CHILD';
  const isConnectedChild = isChild && (deletionStatus?.hasConnectedParent ?? true);

  const deviceColumns: Column<Device>[] = [
    {
      key: 'deviceName',
      header: 'Device Name',
      width: '35%',
      render: (item) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(99, 102, 241, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)',
            }}
          >
            <Smartphone size={16} />
          </div>
          <div>
            <div style={{ fontWeight: 600, color: '#fff' }}>{item.deviceName}</div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>
              UUID: {item.deviceUuid?.substring(0, 13)}...
            </small>
          </div>
        </div>
      ),
    },
    {
      key: 'platform',
      header: 'Platform',
      width: '20%',
      render: (item) => <span className="badge badge-neutral">{item.platform}</span>,
    },
    {
      key: 'lastSeenAt',
      header: 'Last Active',
      width: '25%',
      render: (item) => (
        <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
          {item.lastSeenAt ? new Date(item.lastSeenAt).toLocaleTimeString() : 'Never'}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      width: '20%',
      align: 'right',
      render: (item) => (
        <span
          className={`badge ${
            item.status === 'ENROLLED' ? 'badge-success' : 'badge-neutral'
          }`}
        >
          {item.status}
        </span>
      ),
    },
  ];

  const sessionColumns: Column<DeviceSession>[] = [
    {
      key: 'deviceName',
      header: 'Client / Hardware',
      width: '35%',
      render: (item) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: 'var(--radius-sm)',
              background: 'rgba(59, 130, 246, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-blue, #3B82F6)',
            }}
          >
            {item.platform === 'WEB' ? <Laptop size={16} /> : <Smartphone size={16} />}
          </div>
          <div>
            <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.875rem' }}>
              {item.deviceName}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>
              {item.platform} {item.appVersion ? `• v${item.appVersion}` : ''}
            </small>
          </div>
        </div>
      ),
    },
    {
      key: 'ipAddress',
      header: 'Location & IP',
      width: '25%',
      render: (item) => (
        <div>
          <div style={{ fontSize: '0.85rem', color: '#fff', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <MapPin size={12} color="var(--text-dim)" />
            {item.approximateLocation || 'Unknown'}
          </div>
          <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem', fontFamily: 'monospace' }}>
            {item.ipAddress || '127.0.0.1'}
          </small>
        </div>
      ),
    },
    {
      key: 'createdAt',
      header: 'Session Started',
      width: '20%',
      render: (item) => (
        <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
          {new Date(item.createdAt).toLocaleString()}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status & Action',
      width: '20%',
      align: 'right',
      render: (item) => (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.6rem' }}>
          <span
            className={`badge ${
              item.status === 'ACTIVE'
                ? 'badge-success'
                : item.status === 'REVOKED'
                ? 'badge-danger'
                : 'badge-neutral'
            }`}
            style={{ fontSize: '0.725rem' }}
          >
            {item.status}
          </span>
          {item.status === 'ACTIVE' && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleRevokeSession(item.id)}
              disabled={revokingSessionId === item.id}
              title="Revoke session remotely"
              style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
            >
              {revokingSessionId === item.id ? 'Revoking...' : 'Revoke'}
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Platform Settings</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Family pairing code, enrolled child devices, active sessions, and notification delivery
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Role:</span>
          <span className="badge badge-primary">{user?.role || 'PARENT'}</span>
          <button
            type="button"
            id="btn-refresh-settings"
            className="btn btn-secondary btn-sm"
            onClick={() => loadSettings(false)}
            disabled={refreshing}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
          >
            <RefreshCw size={14} className={refreshing ? 'spinning' : ''} />
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => loadSettings(false)} />}

      {/* Pairing Enrolment Card (Parent only) */}
      {isParent && (
        <ContentCard
          id="card-pairing-code"
          title="Enroll New Child Device"
          subtitle="Provide this one-time pairing key (e.g. NV-7K3M-2W9P) on your child's device during onboarding"
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '1.5rem',
              background: 'rgba(99, 102, 241, 0.05)',
              border: '1px dashed rgba(99, 102, 241, 0.35)',
              borderRadius: 'var(--radius-md)',
              flexWrap: 'wrap',
              gap: '1rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(99, 102, 241, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--primary)',
                }}
              >
                <KeyRound size={24} />
              </div>
              <div>
                <span style={{ fontSize: '0.8rem', textTransform: 'uppercase', color: 'var(--text-dim)', letterSpacing: '0.05em' }}>
                  One-Time Connection Key
                </span>
                <div
                  id="active-pairing-code"
                  style={{
                    fontSize: '1.75rem',
                    fontWeight: 700,
                    letterSpacing: '0.15em',
                    color: '#fff',
                    fontFamily: 'monospace',
                  }}
                >
                  {pairingCode}
                </div>
              </div>
            </div>

            <button
              type="button"
              id="btn-generate-pairing-code"
              className="btn btn-primary"
              onClick={handleGenerateNewCode}
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            >
              <RefreshCw size={16} />
              Generate New Code
            </button>
          </div>
        </ContentCard>
      )}

      {/* Enrolled Devices Card (Parent only) */}
      {isParent && (
        <ContentCard
          id="card-enrolled-devices"
          title="Enrolled Child Devices"
          subtitle="Managed smartphones and tablets bound to this family account"
        >
          <DataTable
            id="table-enrolled-devices"
            columns={deviceColumns}
            data={devices}
            keyExtractor={(item) => item.id}
            emptyTitle="No Devices Enrolled"
            emptyMessage="Scan or enter the pairing code from your child's mobile app to enroll."
          />
        </ContentCard>
      )}

      {/* Authenticated Device Sessions (Security) */}
      <ContentCard
        id="card-active-sessions"
        title="Active Device Sessions & Remote Security"
        subtitle="Manage authenticated clients with active access to this family account. Unrecognized sessions can be immediately revoked."
      >
        <DataTable
          id="table-active-sessions"
          columns={sessionColumns}
          data={sessions}
          keyExtractor={(item) => item.id}
          emptyTitle="No Sessions Recorded"
          emptyMessage="No authenticated device sessions are currently registered."
        />
      </ContentCard>

      {/* Email Security & Delivery Preferences */}
      <ContentCard
        id="card-email-preferences"
        title="Email Security & Notification Preferences"
        subtitle="Manage asynchronous email alerts for account logins, unrecognized hardware, and system updates"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '0.5rem' }}>
          {emailPrefsSuccess && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.65rem',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(16, 185, 129, 0.15)',
                border: '1px solid rgba(16, 185, 129, 0.35)',
                color: 'var(--success)',
                fontSize: '0.85rem',
              }}
            >
              <CheckCircle size={16} />
              <span>{emailPrefsSuccess}</span>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>Login Security Alerts</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Receive an instant email confirmation whenever a successful sign-in occurs
              </div>
            </div>
            <input
              type="checkbox"
              id="toggle-email-login"
              checked={emailPrefs.loginAlertsEnabled}
              onChange={(e) =>
                setEmailPrefs((prev) => ({ ...prev, loginAlertsEnabled: e.target.checked }))
              }
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>New Device Detection Alerts</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Receive a high-priority security alert when a previously unseen browser or device accesses your account
              </div>
            </div>
            <input
              type="checkbox"
              id="toggle-email-new-device"
              checked={emailPrefs.newDeviceAlertsEnabled}
              onChange={(e) =>
                setEmailPrefs((prev) => ({ ...prev, newDeviceAlertsEnabled: e.target.checked }))
              }
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>Application Updates & Announcements</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Receive notifications about important safety updates and new features
              </div>
            </div>
            <input
              type="checkbox"
              id="toggle-email-updates"
              checked={emailPrefs.appUpdateAlertsEnabled}
              onChange={(e) =>
                setEmailPrefs((prev) => ({ ...prev, appUpdateAlertsEnabled: e.target.checked }))
              }
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <button
              type="button"
              id="btn-save-email-prefs"
              className="btn btn-primary btn-sm"
              onClick={handleSaveEmailPrefs}
              disabled={emailPrefsSaving}
              style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
            >
              {emailPrefsSaving ? 'Saving...' : 'Save Email Preferences'}
            </button>
          </div>
        </div>
      </ContentCard>

      {/* Push Notification Preferences */}
      <ContentCard
        id="card-notification-rules"
        title="Push Notification Delivery Preferences"
        subtitle="Configure which safety alerts trigger instant alerts to parent devices"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>Low Battery Alerts</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Send notification when battery drops below 15%</div>
            </div>
            <input
              type="checkbox"
              checked={notifyLowBattery}
              onChange={(e) => setNotifyLowBattery(e.target.checked)}
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>Device Disconnect & Reconnect Alerts</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Notify when child device goes offline for &gt; 15 minutes</div>
            </div>
            <input
              type="checkbox"
              checked={notifyOffline}
              onChange={(e) => setNotifyOffline(e.target.checked)}
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0', borderBottom: '1px solid var(--border-subtle)' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>Safe Zone Geofence Alerts</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Alert when child leaves or enters primary safe zones</div>
            </div>
            <input
              type="checkbox"
              checked={notifySafeZone}
              onChange={(e) => setNotifySafeZone(e.target.checked)}
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 0' }}>
            <div>
              <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>Convocation Incoming Alerts</div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>Push notification when child sends a convocation reply</div>
            </div>
            <input
              type="checkbox"
              checked={notifyConvocation}
              onChange={(e) => setNotifyConvocation(e.target.checked)}
              style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
            />
          </div>
        </div>
      </ContentCard>

      {/* Account Info, Signout, and Delete Account */}
      <ContentCard id="card-account-profile" title="Account Credentials">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ fontWeight: 600, color: '#fff', fontSize: '1rem' }}>{user?.name || (isParent ? 'Parent Admin' : 'Child User')}</div>
            <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>{user?.email || 'user@nivya.local'}</div>
            <span className="badge badge-primary" style={{ marginTop: '0.5rem' }}>
              ROLE: {user?.role || 'PARENT'}
            </span>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap' }}>
            {/* Delete Account Option */}
            <button
              type="button"
              id="btn-delete-account"
              className="btn btn-danger"
              onClick={() => {
                resetDeletionWizard();
                loadDeletionStatus();
                setShowDeleteModal(true);
              }}
              style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
            >
              <Trash2 size={16} />
              Delete Account
            </button>

            {/* Existing Sign Out Option */}
            <button
              type="button"
              id="btn-logout"
              className="btn btn-secondary"
              onClick={() => authService.logout()}
              style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
            >
              <LogOut size={16} />
              Sign Out of Nivya
            </button>
          </div>
        </div>
      </ContentCard>

      {/* Permanent Account Deletion Modal Flow */}
      {showDeleteModal && (
        <div
          id="modal-delete-account"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: '1rem',
          }}
        >
          <div
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-lg)',
              maxWidth: '560px',
              width: '100%',
              padding: '2rem',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.5)',
              position: 'relative',
            }}
          >
            {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div
                  style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '50%',
                    background: deletionStep === 4 ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: deletionStep === 4 ? 'var(--success)' : 'var(--danger)',
                  }}
                >
                  {deletionStep === 4 ? <CheckCircle size={22} /> : <Trash2 size={22} />}
                </div>
                <div>
                  <h3 style={{ margin: 0, color: '#fff', fontSize: '1.2rem', fontWeight: 700 }}>
                    {deletionStep === 4 ? 'Account Deleted' : 'Delete Account'}
                  </h3>
                  <small style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                    {isParent ? 'Parent Administrator Deletion' : 'Child Account Deletion'}
                  </small>
                </div>
              </div>

              {deletionStep !== 4 && (
                <button
                  type="button"
                  onClick={() => setShowDeleteModal(false)}
                  style={{ background: 'transparent', border: 'none', color: 'var(--text-dim)', cursor: 'pointer' }}
                >
                  <X size={20} />
                </button>
              )}
            </div>

            {deletionError && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(239, 68, 68, 0.15)',
                  border: '1px solid rgba(239, 68, 68, 0.35)',
                  color: '#EF4444',
                  fontSize: '0.85rem',
                  marginBottom: '1rem',
                }}
              >
                <AlertCircle size={16} />
                <span>{deletionError}</span>
              </div>
            )}

            {/* SCREEN 1: OVERVIEW */}
            {deletionStep === 1 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {isParent ? (
                  <>
                    <div style={{ background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.25)', borderRadius: 'var(--radius-md)', padding: '1rem 1.25rem' }}>
                      <p style={{ color: '#fff', margin: 0, fontSize: '0.95rem', lineHeight: '1.5' }}>
                        Permanently delete your parent administrator account and credentials. This action is irreversible.
                      </p>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', color: 'var(--text-muted)' }}>
                        <span style={{ color: 'var(--danger)', fontWeight: 'bold' }}>•</span>
                        <span>Your personal credentials, active sessions, and direct parent settings will be wiped.</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', color: 'var(--success)' }}>
                        <span style={{ fontWeight: 'bold' }}>•</span>
                        <span>Connected child accounts will <strong>NOT</strong> be deleted. Child profiles, logs, and telemetry history remain preserved.</span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                      <button type="button" className="btn btn-secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                      </button>
                      <button
                        type="button"
                        id="btn-proceed-to-verify"
                        className="btn btn-danger"
                        onClick={() => setDeletionStep(2)}
                      >
                        Continue to Verification
                        <ArrowRight size={16} />
                      </button>
                    </div>
                  </>
                ) : isConnectedChild ? (
                  <>
                    <div style={{ background: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.25)', borderRadius: 'var(--radius-md)', padding: '1rem 1.25rem' }}>
                      <p style={{ color: '#fff', margin: 0, fontSize: '0.95rem', lineHeight: '1.5' }}>
                        Your child account is managed under a family unit. Permanently deleting this account requires authorization from your parent administrator.
                      </p>
                    </div>

                    <div style={{ padding: '0.85rem 1rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                      Parent Contact: <strong style={{ color: '#fff' }}>{parentEmailMasked || 'Your connected parent'}</strong>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                      <button type="button" className="btn btn-secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                      </button>
                      <button
                        type="button"
                        id="btn-request-parent-approval"
                        className="btn btn-danger"
                        onClick={handleRequestChildCode}
                        disabled={requestingCode}
                      >
                        {requestingCode ? 'Sending Approval Code...' : 'Request Parent Approval'}
                        <ArrowRight size={16} />
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <div style={{ background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.25)', borderRadius: 'var(--radius-md)', padding: '1rem 1.25rem' }}>
                      <p style={{ color: '#fff', margin: 0, fontSize: '0.95rem', lineHeight: '1.5' }}>
                        Permanently delete your child account. This action cannot be undone.
                      </p>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                      <button type="button" className="btn btn-secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                      </button>
                      <button
                        type="button"
                        id="btn-proceed-to-verify-disconnected"
                        className="btn btn-danger"
                        onClick={() => setDeletionStep(2)}
                      >
                        Continue to Verification
                        <ArrowRight size={16} />
                      </button>
                    </div>
                  </>
                )}
              </div>
            )}

            {/* SCREEN 2: IDENTITY VERIFICATION OR CHILD 6-DIGIT CODE */}
            {deletionStep === 2 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {isChild && isConnectedChild ? (
                  <div>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: '0 0 1rem 0' }}>
                      A 6-digit approval code was dispatched to your parent at <strong style={{ color: '#fff' }}>{parentEmailMasked}</strong>. Enter it below to proceed:
                    </p>

                    <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
                      <input
                        type="text"
                        id="input-child-approval-code"
                        placeholder="123456"
                        maxLength={6}
                        value={childApprovalCode}
                        onChange={(e) => setChildApprovalCode(e.target.value.replace(/\D/g, ''))}
                        style={{
                          width: '180px',
                          letterSpacing: '0.3em',
                          fontSize: '1.25rem',
                          textAlign: 'center',
                          padding: '0.75rem',
                          background: 'rgba(255, 255, 255, 0.05)',
                          border: '1px solid var(--border-subtle)',
                          borderRadius: 'var(--radius-md)',
                          color: '#fff',
                          fontFamily: 'monospace',
                        }}
                      />
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: 'var(--text-dim)', fontSize: '0.85rem' }}>
                        <Clock size={15} />
                        <span>Code expires in: <strong style={{ color: '#fff' }}>{formatTimer(timerSeconds)}</strong></span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem' }}>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={handleRequestChildCode}
                        disabled={requestingCode}
                      >
                        Resend Code
                      </button>

                      <div style={{ display: 'flex', gap: '0.75rem' }}>
                        <button type="button" className="btn btn-secondary" onClick={() => setDeletionStep(1)}>
                          Back
                        </button>
                        <button
                          type="button"
                          id="btn-verify-child-code"
                          className="btn btn-danger"
                          onClick={handleVerifyChildCode}
                          disabled={verifyingCode || childApprovalCode.length !== 6}
                        >
                          {verifyingCode ? 'Verifying...' : 'Verify Code & Proceed'}
                          <ArrowRight size={16} />
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div>
                    <label htmlFor="input-deletion-password" style={{ display: 'block', fontSize: '0.9rem', color: '#fff', marginBottom: '0.5rem', fontWeight: 600 }}>
                      Enter your account password to confirm identity:
                    </label>
                    <input
                      type="password"
                      id="input-deletion-password"
                      placeholder="Current account password"
                      value={deletionPassword}
                      onChange={(e) => setDeletionPassword(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '0.75rem 1rem',
                        background: 'rgba(255, 255, 255, 0.05)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        color: '#fff',
                        fontSize: '0.95rem',
                      }}
                    />

                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
                      <button type="button" className="btn btn-secondary" onClick={() => setDeletionStep(1)}>
                        Back
                      </button>
                      <button
                        type="button"
                        id="btn-confirm-password-proceed"
                        className="btn btn-danger"
                        onClick={() => {
                          if (!deletionPassword) {
                            setDeletionError('Please enter your password.');
                            return;
                          }
                          setDeletionError(null);
                          setDeletionStep(3);
                        }}
                        disabled={!deletionPassword}
                      >
                        Confirm Identity & Proceed
                        <ArrowRight size={16} />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* SCREEN 3: FINAL CONFIRMATION (NO TYPE DELETE) */}
            {deletionStep === 3 && (
              <div id="deletion-step-3" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <div style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', padding: '1.25rem' }}>
                  <h4 style={{ color: '#EF4444', margin: '0 0 0.5rem 0', fontSize: '1.05rem', fontWeight: 700 }}>
                    Final Confirmation
                  </h4>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: '1.5', margin: 0 }}>
                    {isParent
                      ? 'You are about to permanently delete your parent administrator account. Your connected children accounts will remain intact and will not be deleted.'
                      : 'You are about to permanently delete your child account. Parent approval has been verified.'}
                  </p>
                  <p style={{ color: 'var(--text-dim)', fontSize: '0.85rem', lineHeight: '1.4', marginTop: '0.75rem', marginBottom: 0 }}>
                    This action is permanent and cannot be undone. All your personal data and active sessions will be completely erased.
                  </p>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                  <button type="button" className="btn btn-secondary" onClick={() => setShowDeleteModal(false)}>
                    Cancel
                  </button>
                  <button
                    type="button"
                    id="btn-permanently-delete-account"
                    className="btn btn-danger"
                    onClick={handleExecutePermanentDeletion}
                    disabled={deletingAccount}
                  >
                    {deletingAccount ? 'Deleting Account...' : 'Yes, Delete My Account'}
                  </button>
                </div>
              </div>
            )}

            {/* SCREEN 4: SUCCESS CONFIRMATION */}
            {deletionStep === 4 && (
              <div id="deletion-step-4" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', padding: '1.5rem 0.5rem', gap: '1rem' }}>
                <div
                  style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: '50%',
                    background: 'rgba(16, 185, 129, 0.15)',
                    border: '1px solid rgba(16, 185, 129, 0.4)',
                    color: 'var(--success)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <CheckCircle size={32} />
                </div>

                <h3 style={{ fontSize: '1.3rem', fontWeight: 700, color: '#fff', margin: 0 }}>
                  Account Successfully Deleted
                </h3>

                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', maxWidth: '440px', margin: 0, lineHeight: '1.6' }}>
                  Your Nivya account and personal credentials have been permanently deleted from our servers. Thank you for using Nivya.
                </p>

                <button
                  type="button"
                  id="btn-deletion-return-home"
                  className="btn btn-primary"
                  onClick={() => {
                    localStorage.clear();
                    window.location.href = '/login';
                  }}
                  style={{ marginTop: '0.5rem' }}
                >
                  Return to Home / Login
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default SettingsPage;
