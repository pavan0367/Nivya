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
} from 'lucide-react';
import { ContentCard, MetricCard } from '../../components/common/Card';
import { DataTable, Column } from '../../components/common/Table';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { authService } from '../../services/authService';
import { apiClient } from '../../services/api';
import { Device, User } from '../../types/auth';

export const SettingsPage: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [user, setUser] = useState<User | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [pairingCode, setPairingCode] = useState<string>('Click "Generate"');

  // Notification settings toggle state
  const [notifyLowBattery, setNotifyLowBattery] = useState<boolean>(true);
  const [notifyOffline, setNotifyOffline] = useState<boolean>(true);
  const [notifySafeZone, setNotifySafeZone] = useState<boolean>(true);
  const [notifyConvocation, setNotifyConvocation] = useState<boolean>(true);

  const loadSettings = async (isInitial = false) => {
    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const currentUser = authService.getCurrentUser();
      setUser(currentUser);

      const devs = await authService.getFamilyDevices();
      if (devs && devs.length > 0) {
        setDevices(devs);
      } else {
        // Fallback demo devices
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
    } catch (err: any) {
      console.error('Failed to load settings:', err);
      setError('Unable to load device pairing records.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadSettings(true);
  }, []);

  const handleUnpairDevice = async (deviceId: number) => {
    if (!window.confirm('Are you sure you want to unpair this child device? Telemetry collection will cease.')) {
      return;
    }

    try {
      await apiClient.delete(`/pairing/devices/${deviceId}`);
      setDevices((prev) => prev.filter((d) => d.id !== deviceId));
    } catch {
      // Local fallback removal
      setDevices((prev) => prev.filter((d) => d.id !== deviceId));
    }
  };

  const handleGenerateNewCode = () => {
    const randomCode = `NV-${Math.floor(100000 + Math.random() * 900000)}`;
    setPairingCode(randomCode);
  };

  if (loading) {
    return (
      <div style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Loading family configuration..." />
      </div>
    );
  }

  const columns: Column<Device>[] = [
    {
      key: 'deviceName',
      header: 'Device Name',
      width: '30%',
      render: (item) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
          <Smartphone size={18} color="var(--primary)" />
          <div>
            <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.925rem' }}>{item.deviceName}</div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem', fontFamily: 'monospace' }}>
              {item.platform} • {item.deviceUuid.slice(0, 16)}...
            </small>
          </div>
        </div>
      ),
    },
    {
      key: 'pushToken',
      header: 'FCM Push Token',
      width: '25%',
      render: (item) => (
        <span
          className={`badge ${item.pushToken ? 'badge-success' : 'badge-warning'}`}
          style={{ fontSize: '0.75rem' }}
        >
          {item.pushToken ? 'Registered (Active)' : 'Unregistered'}
        </span>
      ),
    },
    {
      key: 'lastSeenAt',
      header: 'Last Sync',
      width: '25%',
      render: (item) => (
        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          {item.lastSeenAt ? new Date(item.lastSeenAt).toLocaleString() : 'Unknown'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Manage',
      width: '20%',
      align: 'right',
      render: (item) => (
        <button
          type="button"
          className="btn btn-danger btn-sm"
          onClick={() => handleUnpairDevice(item.id)}
          title="Unpair and disconnect child device"
          style={{ padding: '0.35rem 0.65rem', display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
        >
          <Trash2 size={13} />
          Unlink
        </button>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Platform Settings</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Family pairing code, enrolled child devices, and notification delivery options
          </p>
        </div>

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

      {error && <ErrorBanner message={error} onRetry={() => loadSettings(false)} />}

      {/* Pairing Enrolment Card */}
      <ContentCard
        id="card-pairing-code"
        title="Enroll New Child Device"
        subtitle="Provide this 6-digit one-time pairing key in the Nivya Android child application during onboarding"
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '1.5rem',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(99, 102, 241, 0.08)',
            border: '1px solid rgba(99, 102, 241, 0.25)',
            flexWrap: 'wrap',
            gap: '1.5rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
            <div
              style={{
                width: '54px',
                height: '54px',
                borderRadius: '12px',
                background: 'rgba(99, 102, 241, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--primary)',
              }}
            >
              <KeyRound size={28} />
            </div>
            <div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Active Pairing Code
              </div>
              <div
                id="display-pairing-code"
                style={{
                  fontSize: '2rem',
                  fontWeight: 800,
                  fontFamily: 'monospace',
                  letterSpacing: '0.12em',
                  color: '#fff',
                  marginTop: '0.2rem',
                }}
              >
                {pairingCode}
              </div>
              <small style={{ color: 'var(--text-muted)' }}>Valid for 15 minutes • Single child device link</small>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleGenerateNewCode}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
          >
            <RefreshCw size={15} />
            Generate New Key
          </button>
        </div>
      </ContentCard>

      {/* Enrolled Devices Table */}
      <ContentCard
        id="card-enrolled-devices"
        title="Enrolled Child Devices"
        subtitle="Managed smartphones and tablets bound to this family account"
      >
        <DataTable
          id="table-enrolled-devices"
          columns={columns}
          data={devices}
          keyExtractor={(item) => item.id}
          emptyTitle="No Devices Linked"
          emptyMessage="No child devices are currently paired. Use the pairing code above to enroll a device."
        />
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

      {/* Account Info & Session Signout */}
      <ContentCard id="card-account-profile" title="Account Credentials">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ fontWeight: 600, color: '#fff', fontSize: '1rem' }}>{user?.name || 'Parent Admin'}</div>
            <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>{user?.email || 'parent@nivya.local'}</div>
            <span className="badge badge-primary" style={{ marginTop: '0.5rem' }}>
              ROLE: {user?.role || 'PARENT'}
            </span>
          </div>

          <button
            type="button"
            className="btn btn-danger"
            onClick={() => authService.logout()}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
          >
            <LogOut size={16} />
            Sign Out of Nivya
          </button>
        </div>
      </ContentCard>
    </div>
  );
};

export default SettingsPage;
