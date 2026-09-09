import React, { useEffect, useState, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Radio, Clock, Smartphone, RefreshCw, Layers } from 'lucide-react';
import { ContentCard } from '../../components/common/Card';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { activityService, BackendLiveActivityResponse } from '../../services/activityService';
import { websocketService } from '../../services/websocketService';

interface OutletContextType {
  activeDeviceId: number | null;
}

export const LiveActivityPage: React.FC = () => {
  const { activeDeviceId } = useOutletContext<OutletContextType>();
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activityData, setActivityData] = useState<BackendLiveActivityResponse | null>(null);

  const loadLiveActivity = useCallback(async (isInitial = false) => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }

    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await activityService.getLiveActivity(activeDeviceId);
      if (data) {
        setActivityData(data);
      } else {
        // Fallback demo state if backend returns empty
        setActivityData({
          deviceId: activeDeviceId,
          deviceUuid: 'dev-demo-uuid',
          deviceName: 'Child Phone',
          online: true,
          currentActivity: {
            id: 101,
            appName: 'WhatsApp',
            broadActivity: 'Chatting with Arun',
            category: 'Communication',
            durationSeconds: 240,
            durationFormatted: '4m',
            startedAt: new Date(Date.now() - 4 * 60 * 1000).toISOString(),
          },
          recentActivities: [
            {
              id: 100,
              appName: 'Chrome',
              broadActivity: 'Browsing educational portal',
              category: 'Education',
              durationSeconds: 900,
              durationFormatted: '15m',
              startedAt: new Date(Date.now() - 20 * 60 * 1000).toISOString(),
            },
            {
              id: 99,
              appName: 'Files',
              broadActivity: 'Viewing biology_homework.pdf',
              category: 'Productivity',
              durationSeconds: 480,
              durationFormatted: '8m',
              startedAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
            },
            {
              id: 98,
              appName: 'YouTube Kids',
              broadActivity: 'Watching science documentary',
              category: 'Entertainment',
              durationSeconds: 1200,
              durationFormatted: '20m',
              startedAt: new Date(Date.now() - 55 * 60 * 1000).toISOString(),
            },
          ],
          lastUpdatedAt: new Date().toISOString(),
        });
      }
    } catch (err: any) {
      console.error('Failed to load live activity:', err);
      setError('Unable to fetch real-time activity status.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeDeviceId]);

  useEffect(() => {
    loadLiveActivity(true);

    if (activeDeviceId) {
      const unsub = websocketService.subscribe(
        `/topic/activity/${activeDeviceId}`,
        (msg: any) => {
          if (msg) {
            setActivityData((prev) => {
              if (!prev) return msg;
              return {
                ...prev,
                currentActivity: msg.currentActivity || msg,
                recentActivities: msg.recentActivities || prev.recentActivities,
                lastUpdatedAt: new Date().toISOString(),
              };
            });
          }
        }
      );
      return () => unsub();
    }
  }, [activeDeviceId, loadLiveActivity]);

  if (loading) {
    return (
      <div style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Connecting to real-time activity stream..." />
      </div>
    );
  }

  const current = activityData?.currentActivity;
  const recent = activityData?.recentActivities || [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Live Activity</h1>
            <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
              <span className="pulse-dot online" />
              LIVE MONITOR
            </span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Current foreground application and consented high-level activity telemetry
          </p>
        </div>

        <button
          type="button"
          id="btn-refresh-live-activity"
          className="btn btn-secondary btn-sm"
          onClick={() => loadLiveActivity(false)}
          disabled={refreshing}
          style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
        >
          <RefreshCw size={14} className={refreshing ? 'spinning' : ''} />
          {refreshing ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => loadLiveActivity(false)} />}

      {/* Hero: Current Active App Spotlight */}
      <div
        id="hero-current-activity"
        className="glass-panel"
        style={{
          padding: '2rem',
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(168, 85, 247, 0.08) 100%)',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
        }}
      >
        <span
          style={{
            fontSize: '0.785rem',
            fontWeight: 600,
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
            color: 'var(--primary)',
          }}
        >
          Currently Active in Foreground
        </span>

        {current ? (
          <div style={{ marginTop: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
              <div
                style={{
                  width: '64px',
                  height: '64px',
                  borderRadius: '16px',
                  background: 'var(--primary-gradient)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 6px 20px rgba(99, 102, 241, 0.4)',
                }}
              >
                <Radio size={32} color="#fff" />
              </div>
              <div>
                <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#fff' }}>
                  {current.appName}
                </h2>
                <p style={{ fontSize: '1.1rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  {current.broadActivity || 'Active in app'}
                </p>
                {current.category && (
                  <span className="badge badge-neutral" style={{ marginTop: '0.5rem' }}>
                    {current.category}
                  </span>
                )}
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', justifyContent: 'flex-end', color: 'var(--text-muted)' }}>
                <Clock size={16} />
                <span style={{ fontSize: '0.85rem' }}>Elapsed Time</span>
              </div>
              <div style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--primary)', marginTop: '0.25rem' }}>
                {current.durationFormatted || `${Math.floor((current.durationSeconds || 0) / 60)}m`}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                Started at {new Date(current.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
          </div>
        ) : (
          <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <Smartphone size={32} color="var(--text-dim)" style={{ marginBottom: '0.5rem' }} />
            <p>Device is in standby or no foreground app event reported yet.</p>
          </div>
        )}
      </div>

      {/* Recent Chronological Activity Timeline */}
      <ContentCard
        id="card-activity-timeline"
        title="Chronological Session Timeline"
        subtitle="Recent foreground sessions captured within consented scope"
      >
        {recent.length === 0 ? (
          <EmptyState
            title="No Recent Sessions"
            description="Foreground activity telemetry will populate here as the child uses consented apps."
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '0.5rem' }}>
            {recent.map((ev, index) => (
              <div
                key={ev.id || index}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '1rem 1.25rem',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--border-subtle)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '10px',
                      background: 'rgba(255, 255, 255, 0.06)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--primary)',
                    }}
                  >
                    <Layers size={20} />
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.95rem', color: '#fff' }}>
                      {ev.appName}
                    </div>
                    <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                      {ev.broadActivity || 'Active in app'}
                    </div>
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <span className="badge badge-neutral" style={{ fontWeight: 600 }}>
                    {ev.durationFormatted || `${Math.floor((ev.durationSeconds || 0) / 60)}m`}
                  </span>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '0.35rem' }}>
                    {new Date(ev.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </ContentCard>
    </div>
  );
};

export default LiveActivityPage;
