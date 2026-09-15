import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  Activity,
  ArrowLeft,
  RefreshCw,
  Gauge,
  HelpCircle,
} from 'lucide-react';
import { telemetryService } from '../../services/telemetryService';
import { NetworkStatus } from '../../types/telemetry';
import { ChildOutletContext } from '../../layouts/ChildLayout';

export const ChildNetworkQualityPage: React.FC = () => {
  const navigate = useNavigate();
  const { activeDeviceId, childDevice } = useOutletContext<ChildOutletContext>();
  const [loading, setLoading] = useState(true);
  const [network, setNetwork] = useState<NetworkStatus | null>(null);

  const fetchNetworkQuality = useCallback(async () => {
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
    fetchNetworkQuality();
  }, [fetchNetworkQuality]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            type="button"
            id="btn-back-network-quality"
            onClick={() => navigate('/child')}
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <ArrowLeft size={16} />
            <span>Child Home</span>
          </button>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em' }}>
              Network Quality Details
            </h1>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              6. Latency & connection stability diagnostics for {childDevice?.deviceName || 'this device'}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchNetworkQuality}
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
          <RefreshCw size={28} className="spin" style={{ margin: '0 auto 1rem', color: '#38BDF8' }} />
          <div>Assessing link stability & latency...</div>
        </div>
      ) : network && network.networkQuality ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          <div className="glass-panel" style={{ padding: '1.25rem' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Connection Quality</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#38BDF8', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Gauge size={22} color="#38BDF8" />
              {network.networkQuality}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
              Connection: {network.connectionType || network.ssid || 'Local Network'}
            </div>
          </div>

          <div className="glass-panel" style={{ padding: '1.25rem' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Signal Strength</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff' }}>
              {network.signalLevel !== undefined ? `${network.signalLevel} of 4 bars` : 'Normal'}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
              {network.isInternetAvailable ? 'Internet Accessible' : 'Restricted Connectivity'}
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
              background: 'rgba(56, 189, 248, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              color: '#38BDF8',
            }}
          >
            <HelpCircle size={30} />
          </div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff', marginBottom: '0.5rem' }}>
            Network Quality Diagnostics Unavailable
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto 1.5rem', lineHeight: 1.5 }}>
            No network quality measurements have been transmitted by this device yet. Signal and latency assessments update automatically when device telemetry is submitted.
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

export default ChildNetworkQualityPage;
