import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  Clock,
  ArrowLeft,
  RefreshCw,
  BarChart3,
  Smartphone,
  HelpCircle,
} from 'lucide-react';
import { telemetryService } from '../../services/telemetryService';
import { UsageSummary } from '../../types/telemetry';
import { ChildOutletContext } from '../../layouts/ChildLayout';

export const ChildScreenTimePage: React.FC = () => {
  const navigate = useNavigate();
  const { activeDeviceId, childDevice } = useOutletContext<ChildOutletContext>();
  const [loading, setLoading] = useState(true);
  const [usage, setUsage] = useState<UsageSummary | null>(null);

  const fetchUsageData = useCallback(async () => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await telemetryService.getUsageSummary(activeDeviceId);
      setUsage(data || null);
    } catch {
      setUsage(null);
    } finally {
      setLoading(false);
    }
  }, [activeDeviceId]);

  useEffect(() => {
    fetchUsageData();
  }, [fetchUsageData]);

  const formatMinutes = (mins: number) => {
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    if (h === 0) return `${m}m`;
    return `${h}h ${m}m`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            type="button"
            id="btn-back-screentime"
            onClick={() => navigate('/child')}
            className="btn btn-secondary btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <ArrowLeft size={16} />
            <span>Child Home</span>
          </button>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em' }}>
              Screen Time Details
            </h1>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              2. Daily device usage & wellbeing diagnostics for {childDevice?.deviceName || 'this device'}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchUsageData}
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
          <RefreshCw size={28} className="spin" style={{ margin: '0 auto 1rem', color: '#6366F1' }} />
          <div>Retrieving screen time usage records...</div>
        </div>
      ) : usage && usage.totalScreenTimeMinutes !== undefined && usage.totalScreenTimeMinutes > 0 ? (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
            <div className="glass-panel" style={{ padding: '1.25rem' }}>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Today's Total Screen Time</div>
              <div style={{ fontSize: '2rem', fontWeight: 800, color: '#fff' }}>
                {formatMinutes(usage.totalScreenTimeMinutes)}
              </div>
            </div>

            <div className="glass-panel" style={{ padding: '1.25rem' }}>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>Recorded Date</div>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff' }}>
                {usage.date || 'Today'}
              </div>
            </div>
          </div>

          {/* App Usage Breakdown if available */}
          {usage.appUsages && usage.appUsages.length > 0 && (
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#fff', marginBottom: '1rem' }}>
                Active Applications
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {usage.appUsages.map((app) => (
                  <div key={app.id || app.packageName} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '10px' }}>
                    <div>
                      <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.9rem' }}>{app.appName || app.packageName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>{app.category || 'General'}</div>
                    </div>
                    <span style={{ fontWeight: 700, color: '#6366F1' }}>{formatMinutes(app.durationMinutes)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      ) : (
        /* Real Unavailable State */
        <div className="glass-panel" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: 'rgba(99, 102, 241, 0.12)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              color: '#6366F1',
            }}
          >
            <HelpCircle size={30} />
          </div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff', marginBottom: '0.5rem' }}>
            Screen Time Usage Unavailable
          </h3>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', margin: '0 auto 1.5rem', lineHeight: 1.5 }}>
            No screen time or application usage events have been synced from this device yet. Usage totals update as the client device records foreground application activity.
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

export default ChildScreenTimePage;
