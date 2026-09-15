import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  BatteryCharging,
  ArrowLeft,
  RefreshCw,
  Zap,
  Activity,
  CheckCircle,
  AlertCircle,
  HelpCircle,
} from 'lucide-react';
import { telemetryService } from '../../services/telemetryService';
import { BatteryStatus } from '../../types/telemetry';
import { ChildOutletContext } from '../../layouts/ChildLayout';

export const ChildBatteryPage: React.FC = () => {
  const navigate = useNavigate();
  const { activeDeviceId, childDevice } = useOutletContext<ChildOutletContext>();
  const [loading, setLoading] = useState(true);
  const [battery, setBattery] = useState<BatteryStatus | null>(null);
  const [history, setHistory] = useState<BatteryStatus[]>([]);
  const [error, setError] = useState<string | null>(null);

  const fetchBatteryData = useCallback(async () => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [curRes, histRes] = await Promise.allSettled([
        telemetryService.getBatteryStatus(activeDeviceId),
        telemetryService.getBatteryHistory(activeDeviceId),
      ]);

      if (curRes.status === 'fulfilled' && curRes.value) {
        setBattery(curRes.value);
      } else {
        setBattery(null);
      }

      if (histRes.status === 'fulfilled' && histRes.value && histRes.value.length > 0) {
        setHistory(histRes.value);
      } else {
        setHistory([]);
      }
    } catch (err: any) {
      console.warn('Could not fetch child battery telemetry:', err);
      setBattery(null);
      setHistory([]);
    } finally {
      setLoading(false);
    }
  }, [activeDeviceId]);

  useEffect(() => {
    fetchBatteryData();
  }, [fetchBatteryData]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header with Back Navigation */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            type="button"
            id="btn-back-battery"
            onClick={() => navigate('/child')}
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <ArrowLeft size={16} />
            <span>Child Home</span>
          </button>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em' }}>
              Battery Details
            </h1>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              1. Battery status & charging diagnostics for {childDevice?.deviceName || 'this device'}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchBatteryData}
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
          <RefreshCw size={28} className="spin" style={{ margin: '0 auto 1rem', color: '#10B981' }} />
          <div>Retrieving real device battery diagnostics...</div>
        </div>
      ) : battery ? (
        <>
          {/* Real Battery Metric Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
            <div className="glass-panel" style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Charge Level</span>
                <BatteryCharging size={20} color="#10B981" />
              </div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#fff' }}>
                {battery.batteryPct}%
              </div>
              <div style={{ fontSize: '0.8rem', color: battery.isCharging ? '#10B981' : 'var(--text-dim)', marginTop: '0.25rem' }}>
                {battery.isCharging ? '⚡ Charging Active' : 'Discharging'}
              </div>
            </div>

            <div className="glass-panel" style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Health Status</span>
                <Activity size={20} color="#6366F1" />
              </div>
              <div style={{ fontSize: '1.3rem', fontWeight: 700, color: '#fff' }}>
                {battery.healthStatus || 'Normal'}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
                {battery.temperatureCelsius ? `Temp: ${battery.temperatureCelsius}°C` : 'Thermal: Normal'}
              </div>
            </div>

            <div className="glass-panel" style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>Power Mode</span>
                <Zap size={20} color="#F59E0B" />
              </div>
              <div style={{ fontSize: '1.3rem', fontWeight: 700, color: '#fff' }}>
                {battery.powerSaveMode ? 'Power Saver ON' : 'Standard Mode'}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
                Last synced: {battery.recordedAt ? new Date(battery.recordedAt).toLocaleTimeString() : 'Recent'}
              </div>
            </div>
          </div>

          {/* Chronological History if returned */}
          {history.length > 0 && (
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#fff', marginBottom: '1rem' }}>
                Recent Battery Readings
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {history.slice(-5).reverse().map((h, idx) => (
                  <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.6rem 0.8rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '8px', fontSize: '0.85rem' }}>
                    <span style={{ fontWeight: 600, color: '#fff' }}>{h.batteryPct}%</span>
                    <span style={{ color: 'var(--text-muted)' }}>{h.isCharging ? 'Charging' : 'Battery'}</span>
                    <span style={{ color: 'var(--text-dim)' }}>{h.recordedAt ? new Date(h.recordedAt).toLocaleTimeString() : ''}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      ) : (
        /* Real Unavailable State — No Invented / Fabricated Data */
        <div className="glass-panel" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: 'rgba(245, 158, 11, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              color: '#F59E0B',
            }}
          >
            <HelpCircle size={30} />
          </div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff', marginBottom: '0.5rem' }}>
            Battery Telemetry Unavailable
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto 1.5rem', lineHeight: 1.5 }}>
            No live battery telemetry has been reported by this device yet. Live battery metrics will appear automatically as soon as the client device transmits its first status broadcast.
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

export default ChildBatteryPage;
