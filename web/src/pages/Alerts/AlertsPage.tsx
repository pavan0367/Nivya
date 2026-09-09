import React, { useEffect, useState, useCallback } from 'react';
import {
  Bell,
  AlertTriangle,
  CheckCircle,
  CheckCheck,
  ShieldCheck,
  RefreshCw,
  Clock,
} from 'lucide-react';
import { ContentCard, MetricCard } from '../../components/common/Card';
import { FilterBar } from '../../components/common/FilterBar';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { alertService } from '../../services/alertService';
import { websocketService } from '../../services/websocketService';
import { Alert, Severity } from '../../types/alerts';

export const AlertsPage: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [unreadOnly, setUnreadOnly] = useState<boolean>(false);

  const familyId = 1; // Default family context

  const loadAlerts = useCallback(async (isInitial = false) => {
    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await alertService.getFamilyAlerts(
        familyId,
        unreadOnly,
        severityFilter !== 'ALL' ? severityFilter : undefined
      );

      if (data && data.length > 0) {
        setAlerts(data);
      } else {
        // Fallback demo alerts if backend returns empty
        setAlerts([
          {
            id: 201,
            familyId: 1,
            deviceId: 1,
            alertType: 'BATTERY_LOW',
            severity: 'WARNING',
            title: 'Low Battery Warning',
            message: 'Target device battery dropped below 15% (currently at 12%).',
            targetRole: 'PARENT',
            isRead: false,
            resolved: false,
            createdAt: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
          },
          {
            id: 202,
            familyId: 1,
            deviceId: 1,
            alertType: 'NETWORK_DISCONNECTED',
            severity: 'CRITICAL',
            title: 'Device Offline',
            message: 'Target device disconnected from cellular/Wi-Fi for over 15 minutes.',
            targetRole: 'PARENT',
            isRead: true,
            resolved: false,
            createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
            readAt: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
          },
          {
            id: 203,
            familyId: 1,
            deviceId: 1,
            alertType: 'SAFE_ZONE_ENTRY',
            severity: 'INFO',
            title: 'Safe Zone Reached',
            message: 'Child device entered Primary Safe Zone (Home).',
            targetRole: 'PARENT',
            isRead: true,
            resolved: true,
            createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
            readAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
            resolvedAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
          },
        ]);
      }
    } catch (err: any) {
      console.error('Failed to load alerts:', err);
      setError('Unable to fetch active alerts.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [familyId, unreadOnly, severityFilter]);

  useEffect(() => {
    loadAlerts(true);

    // Subscribe to STOMP WebSocket for incoming real-time alerts
    const unsub = websocketService.subscribe(`/topic/alerts/${familyId}`, (newAlert: Alert) => {
      setAlerts((prev) => [newAlert, ...prev]);
    });

    return () => unsub();
  }, [familyId, loadAlerts]);

  const handleMarkAsRead = async (alertId: number) => {
    try {
      await alertService.markAsRead(alertId);
      setAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, isRead: true, readAt: new Date().toISOString() } : a))
      );
    } catch (err) {
      console.error('Failed to mark alert read:', err);
    }
  };

  const handleResolveAlert = async (alertId: number) => {
    try {
      await alertService.resolveAlert(alertId);
      setAlerts((prev) =>
        prev.map((a) =>
          a.id === alertId
            ? { ...a, resolved: true, isRead: true, resolvedAt: new Date().toISOString() }
            : a
        )
      );
    } catch (err) {
      console.error('Failed to resolve alert:', err);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Loading family safety alerts..." />
      </div>
    );
  }

  // Count summaries
  const unreadCount = alerts.filter((a) => !a.isRead).length;
  const criticalCount = alerts.filter((a) => a.severity === 'CRITICAL' && !a.resolved).length;
  const activeCount = alerts.filter((a) => !a.resolved).length;

  const severityOptions = [
    { value: 'ALL', label: 'All Alerts' },
    { value: 'CRITICAL', label: 'Critical', badge: alerts.filter((a) => a.severity === 'CRITICAL').length },
    { value: 'WARNING', label: 'Warning', badge: alerts.filter((a) => a.severity === 'WARNING').length },
    { value: 'INFO', label: 'Informational', badge: alerts.filter((a) => a.severity === 'INFO').length },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Safety Alerts</h1>
            {unreadCount > 0 && (
              <span className="badge badge-danger">
                {unreadCount} UNREAD
              </span>
            )}
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Telemetry warnings, battery thresholds, and connectivity incidents
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button
            type="button"
            className={`btn btn-sm ${unreadOnly ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setUnreadOnly(!unreadOnly)}
          >
            {unreadOnly ? 'Showing Unread Only' : 'Show All Alerts'}
          </button>
          <button
            type="button"
            id="btn-refresh-alerts"
            className="btn btn-secondary btn-sm"
            onClick={() => loadAlerts(false)}
            disabled={refreshing}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
          >
            <RefreshCw size={14} className={refreshing ? 'spinning' : ''} />
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => loadAlerts(false)} />}

      {/* Metrics Row */}
      <div className="grid grid-cols-3" style={{ gap: '1.25rem' }}>
        <MetricCard
          id="metric-critical-alerts"
          title="Active Critical"
          value={criticalCount}
          subtitle={criticalCount > 0 ? 'Urgent attention required' : 'No critical threats'}
          icon={<AlertTriangle size={24} />}
          badge={{
            text: criticalCount > 0 ? 'CRITICAL' : 'ALL CLEAR',
            variant: criticalCount > 0 ? 'danger' : 'success',
          }}
        />

        <MetricCard
          id="metric-unresolved-alerts"
          title="Total Unresolved"
          value={activeCount}
          subtitle="Open incidents pending resolution"
          icon={<Bell size={24} />}
          badge={{
            text: activeCount > 0 ? 'ACTION NEEDED' : 'RESOLVED',
            variant: activeCount > 0 ? 'warning' : 'neutral',
          }}
        />

        <MetricCard
          id="metric-unread-alerts"
          title="Unread Notifications"
          value={unreadCount}
          subtitle="New messages awaiting review"
          icon={<CheckCheck size={24} />}
          badge={{
            text: unreadCount > 0 ? 'NEW' : 'REVIEWED',
            variant: unreadCount > 0 ? 'info' : 'success',
          }}
        />
      </div>

      {/* Filter and Alerts List */}
      <ContentCard id="card-alerts-feed" title="Incident Feed">
        <FilterBar
          id="filterbar-alerts-severity"
          options={severityOptions}
          selected={severityFilter}
          onSelect={(val) => setSeverityFilter(val)}
        />

        {alerts.length === 0 ? (
          <EmptyState
            title="No Alerts Found"
            description="There are no safety alerts matching the selected filter criteria."
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            {alerts.map((item) => (
              <div
                key={item.id}
                id={`alert-item-${item.id}`}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  padding: '1.25rem',
                  borderRadius: 'var(--radius-md)',
                  background: item.isRead ? 'rgba(255, 255, 255, 0.02)' : 'rgba(99, 102, 241, 0.08)',
                  border: item.isRead ? '1px solid var(--border-subtle)' : '1px solid rgba(99, 102, 241, 0.3)',
                  borderLeft: `4px solid ${
                    item.severity === 'CRITICAL'
                      ? 'var(--danger)'
                      : item.severity === 'WARNING'
                      ? 'var(--warning)'
                      : 'var(--info)'
                  }`,
                  gap: '1rem',
                  transition: 'all 0.2s',
                }}
              >
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                  <div
                    style={{
                      padding: '0.5rem',
                      borderRadius: '8px',
                      background:
                        item.severity === 'CRITICAL'
                          ? 'rgba(239, 68, 68, 0.15)'
                          : item.severity === 'WARNING'
                          ? 'rgba(245, 158, 11, 0.15)'
                          : 'rgba(59, 130, 246, 0.15)',
                      color:
                        item.severity === 'CRITICAL'
                          ? 'var(--danger)'
                          : item.severity === 'WARNING'
                          ? 'var(--warning)'
                          : 'var(--info)',
                    }}
                  >
                    <AlertTriangle size={20} />
                  </div>

                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                      <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#fff' }}>{item.title}</h4>
                      <span
                        className={`badge badge-${
                          item.severity === 'CRITICAL'
                            ? 'danger'
                            : item.severity === 'WARNING'
                            ? 'warning'
                            : 'neutral'
                        }`}
                        style={{ fontSize: '0.7rem' }}
                      >
                        {item.severity}
                      </span>
                      {item.resolved && (
                        <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>
                          Resolved
                        </span>
                      )}
                      {!item.isRead && (
                        <span className="badge badge-info" style={{ fontSize: '0.7rem' }}>
                          Unread
                        </span>
                      )}
                    </div>

                    <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.35rem' }}>
                      {item.message}
                    </p>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', marginTop: '0.5rem', color: 'var(--text-dim)', fontSize: '0.75rem' }}>
                      <Clock size={13} />
                      <span>{new Date(item.createdAt).toLocaleString()}</span>
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexShrink: 0 }}>
                  {!item.isRead && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleMarkAsRead(item.id)}
                      title="Mark as read"
                      style={{ padding: '0.35rem 0.65rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
                    >
                      <CheckCheck size={14} />
                      Mark Read
                    </button>
                  )}

                  {!item.resolved && (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => handleResolveAlert(item.id)}
                      title="Resolve incident"
                      style={{ padding: '0.35rem 0.65rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
                    >
                      <CheckCircle size={14} />
                      Resolve
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </ContentCard>
    </div>
  );
};

export default AlertsPage;
