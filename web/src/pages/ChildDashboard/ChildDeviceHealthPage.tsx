import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  Cpu,
  ArrowLeft,
  RefreshCw,
  HardDrive,
  ShieldCheck,
  HelpCircle,
} from 'lucide-react';
import { telemetryService } from '../../services/telemetryService';
import { DeviceHealth } from '../../types/telemetry';
import { ChildOutletContext } from '../../layouts/ChildLayout';

export const ChildDeviceHealthPage: React.FC = () => {
  const navigate = useNavigate();
  const { activeDeviceId, childDevice } = useOutletContext<ChildOutletContext>();
  const [loading, setLoading] = useState(true);
  const [health, setHealth] = useState<DeviceHealth | null>(null);

  const fetchHealthData = useCallback(async () => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await telemetryService.getDeviceHealth(activeDeviceId);
      setHealth(data || null);
    } catch {
      setHealth(null);
    } finally {
      setLoading(false);
    }
  }, [activeDeviceId]);

  useEffect(() => {
    fetchHealthData();
  }, [fetchHealthData]);

  const formatBytes = (bytes?: number) => {
    if (!bytes) return 'N/A';
    const gb = bytes / (1024 * 1024 * 1024);
    return `${gb.toFixed(1)} GB`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            type="button"
            id="btn-back-device-health"
            onClick={() => navigate('/child')}
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <ArrowLeft size={16} />
            <span>Child Home</span>
          </button>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em' }}>
              Device Health Details
            </h1>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              8. System hardware & storage metrics for {childDevice?.deviceName || 'this device'}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchHealthData}
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
          <RefreshCw size={28} className="spin" style={{ margin: '0 auto 1rem', color: '#14B8A6' }} />
          <div>Scanning system health parameters...</div>
        </div>
      ) : health && (health.totalStorageBytes || health.freeStorageBytes) ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          <div className="glass-panel" style={{ padding: '1.25rem' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Storage Capacity</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <HardDrive size={22} color="#14B8A6" />
              {formatBytes(health.freeStorageBytes)} free
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
              Total: {formatBytes(health.totalStorageBytes)}
            </div>
          </div>

          <div className="glass-panel" style={{ padding: '1.25rem' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>System Integrity</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: health.isHealthy ? '#10B981' : '#F59E0B', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <ShieldCheck size={22} />
              {health.isHealthy ? 'Healthy' : 'Needs Attention'}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
              Low storage warning: {health.isLowStorage ? 'YES' : 'NO'}
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
              background: 'rgba(20, 184, 166, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              color: '#14B8A6',
            }}
          >
            <HelpCircle size={30} />
          </div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff', marginBottom: '0.5rem' }}>
            Device Health Diagnostics Unavailable
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto 1.5rem', lineHeight: 1.5 }}>
            No hardware or storage diagnostics have been transmitted by this device yet. Metrics will display as soon as system diagnostics are broadcast from the client.
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

export default ChildDeviceHealthPage;
