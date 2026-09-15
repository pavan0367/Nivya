import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  Wifi,
  WifiOff,
  ArrowLeft,
  RefreshCw,
  Signal,
  HelpCircle,
} from 'lucide-react';
import { telemetryService } from '../../services/telemetryService';
import { NetworkStatus } from '../../types/telemetry';
import { ChildOutletContext } from '../../layouts/ChildLayout';

export const ChildNetworkPage: React.FC = () => {
  const navigate = useNavigate();
  const { activeDeviceId, childDevice } = useOutletContext<ChildOutletContext>();
  const [loading, setLoading] = useState(true);
  const [network, setNetwork] = useState<NetworkStatus | null>(null);

  const fetchNetworkData = useCallback(async () => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await telemetryService.getNetworkStatus(activeDeviceId);
      setNetwork(data || null);
    } catch {
      setNetwork(null);
    } finally {
      setLoading(false);
    }
  }, [activeDeviceId]);

  useEffect(() => {
    fetchNetworkData();
  }, [fetchNetworkData]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            type="button"
            id="btn-back-network"
            onClick={() => navigate('/child')}
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <ArrowLeft size={16} />
            <span>Child Home</span>
          </button>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em' }}>
              Network Details
            </h1>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              5. Wi-Fi & cellular connectivity for {childDevice?.deviceName || 'this device'}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchNetworkData}
          disabled={loading}
          className="btn btn-secondary btn-sm"
          style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
        >
          <RefreshCw size={14} className={loading ? 'spin' : ''} />
          <span>Refresh</span>
        </button>
      </div>

      {loading ? (
        <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <RefreshCw size={28} className="spin" style={{ margin: '0 auto 1rem', color: '#06B6D4' }} />
          <div>Checking connection status...</div>
        </div>
      ) : network ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          <div className="glass-panel" style={{ padding: '1.25rem' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Connection Type</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              {network.networkType === 'WIFI' ? <Wifi size={22} color="#06B6D4" /> : <Signal size={22} color="#06B6D4" />}
              {network.networkType || 'Unavailable'}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
              {network.ssid ? `SSID: ${network.ssid}` : 'Network active'}
            </div>
          </div>

          <div className="glass-panel" style={{ padding: '1.25rem' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Internet Access</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: network.isInternetAvailable ? '#10B981' : '#EF4444' }}>
              {network.isInternetAvailable ? 'Connected' : 'Offline'}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
              Signal Level: {network.signalLevel !== undefined ? `${network.signalLevel}/4` : 'Normal'}
            </div>
          </div>
        </div>
      ) : (
        /* Real Unavailable State */
        <div className="glass-panel" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: 'rgba(6, 182, 212, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              color: '#06B6D4',
            }}
          >
            <HelpCircle size={30} />
          </div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff', marginBottom: '0.5rem' }}>
            Network Status Unavailable
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto 1.5rem', lineHeight: 1.5 }}>
            No network status has been received from this device. Once the device connects to Wi-Fi or cellular and reports telemetry, current network parameters will appear here.
          </p>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem', borderRadius: '20px', background: 'rgba(255, 255, 255, 0.04)', fontSize: '0.8rem', color: 'var(--text-dim)' }}>
            <span className="pulse-dot offline" style={{ width: '6px', height: '6px' }} />
            Waiting for device data
          </div>
        </div>
      )}
    </div>
  );
};

export default ChildNetworkPage;
