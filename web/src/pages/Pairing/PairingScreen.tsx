import React, { useState, useEffect } from 'react';
import './PairingScreen.css';

export interface DeviceStatusItem {
  deviceId: number;
  deviceUuid: string;
  deviceName: string;
  platform: string;
  online: boolean;
  batteryPct?: number;
  networkType?: string;
  networkQuality?: string;
  lastSyncAt?: string;
  lastSeenAt?: string;
  stale: boolean;
}

export interface LinkedMember {
  userId: number;
  name: string;
  email: string;
  role: 'PARENT' | 'CHILD';
  joinedAt: string;
}

export interface PairingStatusData {
  paired: boolean;
  familyId?: number;
  familyCode?: string;
  familyName?: string;
  userRole?: 'PARENT' | 'CHILD';
  members?: LinkedMember[];
  devices?: DeviceStatusItem[];
}

interface PairingScreenProps {
  role?: 'PARENT' | 'CHILD';
  onPairingComplete?: () => void;
}

export const PairingScreen: React.FC<PairingScreenProps> = ({
  role,
  onPairingComplete
}) => {
  const currentRole = role || (localStorage.getItem('userRole') as 'PARENT' | 'CHILD') || 'PARENT';
  const oppositeRole = currentRole === 'PARENT' ? 'Child' : 'Parent';

  const [myCode, setMyCode] = useState<string>('');
  const [ttlSeconds, setTtlSeconds] = useState<number>(600);
  const [oppositeCode, setOppositeCode] = useState<string>('');
  const [isLoadingCode, setIsLoadingCode] = useState<boolean>(true);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [copied, setCopied] = useState<boolean>(false);
  const [pairingStatus, setPairingStatus] = useState<PairingStatusData | null>(null);

  // Fetch initial code and status
  useEffect(() => {
    fetchPairingCode();
    checkPairingStatus();
  }, []);

  // Countdown timer for 10-minute code TTL
  useEffect(() => {
    if (ttlSeconds <= 0) return;
    const timer = setInterval(() => {
      setTtlSeconds(prev => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [ttlSeconds]);

  const getAuthHeader = () => {
    const token = localStorage.getItem('accessToken');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  };

  const fetchPairingCode = async () => {
    setIsLoadingCode(true);
    setErrorMessage(null);
    try {
      const res = await fetch('/api/v1/pairing/code', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader()
        },
        body: JSON.stringify({ deviceFingerprint: navigator.userAgent })
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => null);
        throw new Error(errorData?.message || 'Failed to generate pairing code');
      }

      const data = await res.json();
      if (data.data?.code) {
        setMyCode(data.data.code);
        setTtlSeconds(data.data.ttlSeconds || 600);
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Error generating connection code');
    } finally {
      setIsLoadingCode(false);
    }
  };

  const checkPairingStatus = async () => {
    try {
      const res = await fetch('/api/v1/pairing/status', {
        headers: { ...getAuthHeader() }
      });
      if (res.ok) {
        const json = await res.json();
        if (json.data?.paired) {
          setPairingStatus(json.data);
        }
      }
    } catch {
      // Ignored for unauthenticated or first-time setup
    }
  };

  const handleCopyCode = () => {
    if (!myCode) return;
    navigator.clipboard.writeText(myCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!oppositeCode.trim()) {
      setErrorMessage("Please enter the opposite device's code");
      return;
    }

    setIsConnecting(true);
    setErrorMessage(null);

    const deviceUuid = localStorage.getItem('deviceUuid') || 'web-' + Math.random().toString(36).substring(2, 10);
    localStorage.setItem('deviceUuid', deviceUuid);

    try {
      const res = await fetch('/api/v1/pairing/connect', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeader()
        },
        body: JSON.stringify({
          code: oppositeCode.trim().toUpperCase(),
          deviceInfo: {
            deviceUuid,
            deviceName: `${currentRole} Web Browser`,
            platform: 'WEB',
            osVersion: navigator.platform,
            appVersion: '1.0.0'
          }
        })
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Pairing failed. Please check the code and try again.');
      }

      setPairingStatus(data.data);
      if (onPairingComplete) {
        onPairingComplete();
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Connection error. Check code and network.');
    } finally {
      setIsConnecting(false);
    }
  };

  const formatTtl = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div className="pairing-container">
      <div className="pairing-card">
        <header className="pairing-header">
          <h1 className="pairing-logo">Nivya</h1>
          <h2 className="pairing-title">Device Connection &amp; Pairing</h2>
          <p className="pairing-subtitle">
            Link your {currentRole.toLowerCase()} device with your {oppositeRole.toLowerCase()} to establish a persistent family safety channel.
          </p>
        </header>

        {errorMessage && (
          <div className="error-banner">
            <span>⚠️</span>
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Existing Pairing Status Banner */}
        {pairingStatus?.paired && (
          <div className="connected-banner">
            <span className="connected-badge">ACTIVE FAMILY CONNECTION</span>
            <h3 style={{ margin: '4px 0 8px', fontSize: '18px', color: '#10b981' }}>
              {pairingStatus.familyName} ({pairingStatus.familyCode})
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '13px', color: '#94a3b8' }}>
              Synchronized members: {pairingStatus.members?.map(m => m.name).join(', ')}
            </p>

            {/* Linked Devices & Offline Indicators */}
            {pairingStatus.devices && pairingStatus.devices.length > 0 && (
              <div className="device-list">
                {pairingStatus.devices.map(dev => (
                  <div key={dev.deviceId} className="device-item">
                    <div className="device-info">
                      <div className="device-name">{dev.deviceName}</div>
                      <div className="device-meta">
                        {dev.platform} • Battery: {dev.batteryPct !== undefined ? `${dev.batteryPct}%` : 'N/A'}
                        {dev.lastSeenAt && ` • Last seen: ${new Date(dev.lastSeenAt).toLocaleTimeString()}`}
                      </div>
                    </div>
                    <div>
                      {dev.online && !dev.stale ? (
                        <span className="status-badge status-online">● Online</span>
                      ) : (
                        <span className="status-badge status-offline">
                          ○ Offline {dev.stale ? '(Stale)' : ''}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Section 1: This Device's Code */}
        <section className="code-display-panel">
          <div className="code-label">YOUR ONE-TIME PAIRING CODE</div>
          <div className="code-box">
            <span className="code-text">
              {isLoadingCode ? 'GENERATING...' : (myCode || '--------')}
            </span>
          </div>
          <div className="code-actions">
            <button
              className="code-action-btn"
              onClick={handleCopyCode}
              disabled={isLoadingCode || !myCode}
            >
              {copied ? '✓ Copied to Clipboard' : '📋 Copy Code'}
            </button>
            <button
              className="code-action-btn"
              onClick={fetchPairingCode}
              disabled={isLoadingCode}
              title="Generate new code"
            >
              🔄 Refresh
            </button>
          </div>
          <p className="code-expiry-text">
            {ttlSeconds > 0 ? (
              <>Expires in <strong style={{ color: '#38bdf8' }}>{formatTtl(ttlSeconds)}</strong> (Single-use)</>
            ) : (
              <span style={{ color: '#ef4444' }}>Code Expired — click Refresh</span>
            )}
          </p>
        </section>

        {/* Divider */}
        <div className="divider-container">
          <div className="divider-line" />
          <span className="divider-text">OR ENTER OPPOSITE DEVICE CODE</span>
          <div className="divider-line" />
        </div>

        {/* Section 2: Enter Opposite Code */}
        <form className="input-panel" onSubmit={handleConnect}>
          <label htmlFor="opposite-code" className="input-label">
            Enter {oppositeRole}&apos;s Pairing Code
          </label>
          <p className="input-desc">
            Type the 8-character connection code displayed on your {oppositeRole.toLowerCase()}&apos;s device screen.
          </p>
          <input
            id="opposite-code"
            type="text"
            className="code-input-field"
            placeholder="e.g. NV-7K3M-2W9P"
            value={oppositeCode}
            onChange={(e) => setOppositeCode(e.target.value.toUpperCase())}
            maxLength={14}
            disabled={isConnecting}
            autoComplete="off"
            spellCheck="false"
          />
          <button
            type="submit"
            className="connect-btn"
            disabled={!oppositeCode.trim() || isConnecting}
          >
            {isConnecting ? 'Validating & Pairing...' : 'Connect Devices'}
          </button>
        </form>

        {/* Consent and Security Transparency Notice */}
        <div className="consent-notice">
          <div className="consent-icon">🛡️</div>
          <div>
            <strong>Consent &amp; Security Notice:</strong> Pairing binds both devices to an authenticated Family Safety unit. Connection state persists across reboots and network loss. When child device is disconnected, parent dashboard reflects last-known device telemetry with an offline/stale status indicator.
          </div>
        </div>
      </div>
    </div>
  );
};

export default PairingScreen;
