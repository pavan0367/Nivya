import React, { useState, useEffect, useCallback } from 'react';
import { RoleRouteGuard, RoleType } from '../../routes/RoleRouteGuard';
import {
  HistoryEvent,
  HistoryEventDetail,
  DateFilterPreset,
  HistoryPageResponse,
} from '../../types/history';
import { historyService } from '../../services/historyService';

interface ParentHistoryPageProps {
  currentRole: RoleType;
  deviceId?: number;
  token?: string;
}

export const ParentHistoryPage: React.FC<ParentHistoryPageProps> = ({
  currentRole,
  deviceId = 1,
  token = '',
}) => {
  const [items, setItems] = useState<HistoryEvent[]>([]);
  const [availableApps, setAvailableApps] = useState<string[]>([]);
  const [selectedApp, setSelectedApp] = useState<string | null>(null);
  const [datePreset, setDatePreset] = useState<DateFilterPreset>('ALL_TIME');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [hasNext, setHasNext] = useState<boolean>(false);
  const [hasPrevious, setHasPrevious] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isStale, setIsStale] = useState<boolean>(false);
  const [selectedDetail, setSelectedDetail] = useState<HistoryEventDetail | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);

  const computeDateRange = (preset: DateFilterPreset): { startDate?: string; endDate?: string } => {
    const now = new Date();
    if (preset === 'TODAY') {
      const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      return { startDate: startOfDay.toISOString(), endDate: now.toISOString() };
    }
    if (preset === 'LAST_7_DAYS') {
      const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      return { startDate: sevenDaysAgo.toISOString(), endDate: now.toISOString() };
    }
    if (preset === 'LAST_30_DAYS') {
      const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      return { startDate: thirtyDaysAgo.toISOString(), endDate: now.toISOString() };
    }
    return {};
  };

  const loadApplications = useCallback(async () => {
    try {
      if (token) {
        const apps = await historyService.getDistinctApplications(deviceId, token);
        setAvailableApps(apps);
      } else {
        setAvailableApps(['WhatsApp', 'Chrome', 'YouTube', 'Files', 'Phone']);
      }
    } catch {
      setAvailableApps(['WhatsApp', 'Chrome', 'YouTube', 'Files', 'Phone']);
    }
  }, [deviceId, token]);

  const loadHistory = useCallback(
    async (page: number) => {
      setIsLoading(true);
      setErrorMessage(null);
      const { startDate, endDate } = computeDateRange(datePreset);

      try {
        if (token) {
          const response: HistoryPageResponse = await historyService.getHistory(
            {
              deviceId,
              page,
              size: 15,
              startDate,
              endDate,
              application: selectedApp,
            },
            token
          );
          setItems(response.items);
          setCurrentPage(response.currentPage);
          setTotalPages(response.totalPages);
          setTotalElements(response.totalElements);
          setHasNext(response.hasNext);
          setHasPrevious(response.hasPrevious);
          setIsStale(false);
        } else {
          // Offline demonstration fallback
          const sample = getDemoEvents();
          const filtered = sample.filter(
            (e) => !selectedApp || e.appName.toLowerCase() === selectedApp.toLowerCase()
          );
          setItems(filtered);
          setCurrentPage(0);
          setTotalPages(1);
          setTotalElements(filtered.length);
          setHasNext(false);
          setHasPrevious(false);
          setIsStale(true);
        }
      } catch (err: any) {
        setErrorMessage(err.message || 'Failed to load activity history');
      } finally {
        setIsLoading(false);
      }
    },
    [deviceId, token, datePreset, selectedApp]
  );

  useEffect(() => {
    loadApplications();
  }, [loadApplications]);

  useEffect(() => {
    loadHistory(0);
  }, [loadHistory]);

  const handleSelectEvent = async (event: HistoryEvent) => {
    // Immediate preview
    setSelectedDetail({
      id: event.id,
      deviceId: event.deviceId,
      deviceName: "Alex's Galaxy A54",
      packageName: event.packageName,
      appName: event.appName,
      broadActivity: event.broadActivity,
      activityLabel: event.activityLabel,
      category: event.category,
      durationSeconds: event.durationSeconds,
      durationFormatted: event.durationFormatted,
      details: 'Consented activity session recorded within authorized family safety scope.',
      eventTimestamp: event.eventTimestamp,
      recordedAt: event.eventTimestamp,
    });

    if (token) {
      try {
        setIsLoadingDetail(true);
        const detail = await historyService.getHistoryEventDetail(deviceId, event.id, token);
        setSelectedDetail(detail);
      } catch {
        // Keep local preview
      } finally {
        setIsLoadingDetail(false);
      }
    }
  };

  const getDemoEvents = (): HistoryEvent[] => {
    const now = new Date();
    return [
      {
        id: 1,
        deviceId: 1,
        packageName: 'com.whatsapp',
        appName: 'WhatsApp',
        broadActivity: 'Chatting with Arun',
        activityLabel: 'Arun',
        category: 'COMMUNICATION',
        durationSeconds: 840,
        durationFormatted: '14m',
        eventTimestamp: new Date(now.getTime() - 20 * 60 * 1000).toISOString(),
      },
      {
        id: 2,
        deviceId: 1,
        packageName: 'com.android.chrome',
        appName: 'Chrome',
        broadActivity: 'Browsing',
        activityLabel: null,
        category: 'BROWSING',
        durationSeconds: 1200,
        durationFormatted: '20m',
        eventTimestamp: new Date(now.getTime() - 55 * 60 * 1000).toISOString(),
      },
      {
        id: 3,
        deviceId: 1,
        packageName: 'com.google.android.youtube',
        appName: 'YouTube',
        broadActivity: 'Watching',
        activityLabel: 'Science Documentary',
        category: 'ENTERTAINMENT',
        durationSeconds: 1800,
        durationFormatted: '30m',
        eventTimestamp: new Date(now.getTime() - 130 * 60 * 1000).toISOString(),
      },
      {
        id: 4,
        deviceId: 1,
        packageName: 'com.google.android.apps.docs',
        appName: 'Files',
        broadActivity: 'Viewing report.pdf',
        activityLabel: 'report.pdf',
        category: 'PRODUCTIVITY',
        durationSeconds: 300,
        durationFormatted: '5m',
        eventTimestamp: new Date(now.getTime() - 200 * 60 * 1000).toISOString(),
      },
      {
        id: 5,
        deviceId: 1,
        packageName: 'com.google.android.dialer',
        appName: 'Phone',
        broadActivity: 'In call with Mom',
        activityLabel: 'Mom',
        category: 'COMMUNICATION',
        durationSeconds: 480,
        durationFormatted: '8m',
        eventTimestamp: new Date(now.getTime() - 320 * 60 * 1000).toISOString(),
      },
    ];
  };

  const getAppBadgeColor = (appName: string): string => {
    const lower = appName.toLowerCase();
    if (lower.includes('whatsapp')) return '#25D366';
    if (lower.includes('chrome')) return '#4285F4';
    if (lower.includes('youtube')) return '#FF0000';
    if (lower.includes('file') || lower.includes('docs')) return '#FBBC05';
    if (lower.includes('phone')) return '#34A853';
    return '#2563EB';
  };

  return (
    <RoleRouteGuard allowedRoles={['PARENT']} currentRole={currentRole}>
      <div style={styles.container}>
        {/* Header */}
        <div style={styles.header}>
          <div>
            <h1 style={styles.title}>Activity History</h1>
            <p style={styles.subtitle}>
              Chronological Audit Log • Alex's Galaxy A54
            </p>
          </div>
          <button
            style={styles.refreshButton}
            onClick={() => loadHistory(currentPage)}
            title="Refresh History"
          >
            🔄 Refresh
          </button>
        </div>

        {/* Privacy Shield Banner */}
        <div style={styles.privacyBanner}>
          <div style={styles.privacyIcon}>🛡️</div>
          <div>
            <div style={styles.privacyTitle}>Parent-Only Audit Log</div>
            <div style={styles.privacyDesc}>
              Chronological records of broad activity and durations within consented scope.
              Private chat messages, passwords, and microphone/call audio are strictly protected and never recorded.
            </div>
          </div>
        </div>

        {/* Stale Data Indicator */}
        {isStale && (
          <div style={styles.staleBanner}>
            ⚠️ Displaying locally cached or offline activity history.
          </div>
        )}

        {/* Filter Toolbar */}
        <div style={styles.filterCard}>
          {/* Date Filter */}
          <div style={styles.filterGroup}>
            <span style={styles.filterLabel}>Time Range:</span>
            <div style={styles.chipRow}>
              {(
                [
                  { id: 'ALL_TIME', label: 'All Time' },
                  { id: 'TODAY', label: 'Today' },
                  { id: 'LAST_7_DAYS', label: 'Last 7 Days' },
                  { id: 'LAST_30_DAYS', label: 'Last 30 Days' },
                ] as const
              ).map((preset) => (
                <button
                  key={preset.id}
                  style={
                    datePreset === preset.id
                      ? { ...styles.chip, ...styles.chipActive }
                      : styles.chip
                  }
                  onClick={() => {
                    setDatePreset(preset.id);
                    setCurrentPage(0);
                  }}
                >
                  {preset.label}
                </button>
              ))}
            </div>
          </div>

          {/* Application Filter */}
          <div style={styles.filterGroup}>
            <span style={styles.filterLabel}>Application:</span>
            <div style={styles.chipRow}>
              <button
                style={
                  selectedApp === null
                    ? { ...styles.chip, ...styles.chipPurpleActive }
                    : styles.chip
                }
                onClick={() => {
                  setSelectedApp(null);
                  setCurrentPage(0);
                }}
              >
                All Apps
              </button>
              {availableApps.map((app) => (
                <button
                  key={app}
                  style={
                    selectedApp === app
                      ? { ...styles.chip, ...styles.chipPurpleActive }
                      : styles.chip
                  }
                  onClick={() => {
                    setSelectedApp(selectedApp === app ? null : app);
                    setCurrentPage(0);
                  }}
                >
                  {app}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Main Content Area */}
        {isLoading ? (
          <div style={styles.centerMessage}>
            <div style={styles.spinner} />
            <p>Loading activity history...</p>
          </div>
        ) : errorMessage ? (
          <div style={styles.errorMessage}>
            <p>⚠️ {errorMessage}</p>
            <button
              style={styles.retryButton}
              onClick={() => loadHistory(currentPage)}
            >
              Try Again
            </button>
          </div>
        ) : items.length === 0 ? (
          <div style={styles.centerMessage}>
            <span style={{ fontSize: '3rem' }}>📜</span>
            <h3>No History Records Found</h3>
            <p>No activity recorded for this period or selected application.</p>
          </div>
        ) : (
          <div>
            <div style={styles.timelineList}>
              {items.map((event) => {
                const badgeColor = getAppBadgeColor(event.appName);
                return (
                  <div
                    key={event.id}
                    style={styles.eventCard}
                    onClick={() => handleSelectEvent(event)}
                  >
                    <div
                      style={{
                        ...styles.appIconBadge,
                        backgroundColor: `${badgeColor}22`,
                        color: badgeColor,
                        borderColor: `${badgeColor}55`,
                      }}
                    >
                      {event.appName.slice(0, 2).toUpperCase()}
                    </div>

                    <div style={styles.eventBody}>
                      <div style={styles.eventHeader}>
                        <div style={styles.appName}>{event.appName}</div>
                        <div style={styles.durationPill}>
                          ⏱ {event.durationFormatted}
                        </div>
                      </div>

                      <div style={styles.broadActivity}>
                        {event.broadActivity}
                      </div>

                      <div style={styles.eventFooter}>
                        {event.activityLabel ? (
                          <span style={styles.labelTag}>
                            🏷 {event.activityLabel}
                          </span>
                        ) : (
                          <span style={styles.categoryTag}>
                            {event.category || 'ACTIVITY'}
                          </span>
                        )}
                        <span style={styles.timestamp}>
                          {new Date(event.eventTimestamp).toLocaleString()}
                        </span>
                      </div>
                    </div>

                    <div style={styles.chevron}>›</div>
                  </div>
                );
              })}
            </div>

            {/* Pagination Controls */}
            <div style={styles.paginationRow}>
              <button
                style={
                  hasPrevious
                    ? styles.pageButton
                    : { ...styles.pageButton, ...styles.pageButtonDisabled }
                }
                disabled={!hasPrevious}
                onClick={() => hasPrevious && loadHistory(currentPage - 1)}
              >
                ‹ Previous
              </button>

              <span style={styles.pageInfo}>
                Page {currentPage + 1} of {Math.max(1, totalPages)} ({totalElements} events)
              </span>

              <button
                style={
                  hasNext
                    ? styles.pageButton
                    : { ...styles.pageButton, ...styles.pageButtonDisabled }
                }
                disabled={!hasNext}
                onClick={() => hasNext && loadHistory(currentPage + 1)}
              >
                Next ›
              </button>
            </div>
          </div>
        )}

        {/* Event Detail Modal */}
        {selectedDetail && (
          <div style={styles.modalBackdrop} onClick={() => setSelectedDetail(null)}>
            <div
              style={styles.modalCard}
              onClick={(e) => e.stopPropagation()}
            >
              <div style={styles.modalHeader}>
                <div style={styles.modalTitle}>
                  <div
                    style={{
                      ...styles.appIconBadge,
                      backgroundColor: `${getAppBadgeColor(selectedDetail.appName)}22`,
                      color: getAppBadgeColor(selectedDetail.appName),
                      borderColor: `${getAppBadgeColor(selectedDetail.appName)}55`,
                      width: 40,
                      height: 40,
                      fontSize: '1rem',
                    }}
                  >
                    {selectedDetail.appName.slice(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <h3 style={{ margin: 0, color: '#F8FAFC' }}>
                      {selectedDetail.appName}
                    </h3>
                    <span style={{ fontSize: '0.8rem', color: '#94A3B8' }}>
                      {selectedDetail.packageName}
                    </span>
                  </div>
                </div>
                <button
                  style={styles.closeButton}
                  onClick={() => setSelectedDetail(null)}
                >
                  ✕
                </button>
              </div>

              <div style={styles.modalContent}>
                <div style={styles.detailItem}>
                  <span style={styles.detailLabel}>Broad Activity</span>
                  <span style={styles.detailValue}>
                    {selectedDetail.broadActivity}
                  </span>
                </div>

                {selectedDetail.activityLabel && (
                  <div style={styles.detailItem}>
                    <span style={styles.detailLabel}>Activity Label / Contact</span>
                    <span style={styles.detailValue}>
                      {selectedDetail.activityLabel}
                    </span>
                  </div>
                )}

                <div style={styles.detailItem}>
                  <span style={styles.detailLabel}>Duration</span>
                  <span style={styles.detailValue}>
                    {selectedDetail.durationFormatted} ({selectedDetail.durationSeconds} seconds)
                  </span>
                </div>

                <div style={styles.detailItem}>
                  <span style={styles.detailLabel}>Category</span>
                  <span style={styles.detailValue}>
                    {selectedDetail.category || 'GENERAL'}
                  </span>
                </div>

                <div style={styles.detailItem}>
                  <span style={styles.detailLabel}>Event Timestamp</span>
                  <span style={styles.detailValue}>
                    {new Date(selectedDetail.eventTimestamp).toLocaleString()}
                  </span>
                </div>

                {selectedDetail.details && (
                  <div style={styles.consentedScopeBox}>
                    <div style={styles.consentedScopeTitle}>Consented Scope</div>
                    <div style={styles.consentedScopeText}>
                      {selectedDetail.details}
                    </div>
                  </div>
                )}
              </div>

              <div style={styles.modalFooter}>
                <button
                  style={styles.modalCloseBtn}
                  onClick={() => setSelectedDetail(null)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </RoleRouteGuard>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    maxWidth: '960px',
    margin: '0 auto',
    padding: '24px 16px',
    color: '#F8FAFC',
    fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '20px',
  },
  title: {
    margin: 0,
    fontSize: '1.875rem',
    fontWeight: 700,
    color: '#F8FAFC',
  },
  subtitle: {
    margin: '4px 0 0 0',
    fontSize: '0.95rem',
    color: '#94A3B8',
  },
  refreshButton: {
    background: '#1E293B',
    color: '#60A5FA',
    border: '1px solid #334155',
    borderRadius: '8px',
    padding: '8px 16px',
    cursor: 'pointer',
    fontWeight: 600,
    fontSize: '0.9rem',
  },
  privacyBanner: {
    display: 'flex',
    alignItems: 'center',
    gap: '14px',
    backgroundColor: 'rgba(37, 99, 235, 0.12)',
    border: '1px solid rgba(37, 99, 235, 0.3)',
    borderRadius: '12px',
    padding: '14px 18px',
    marginBottom: '20px',
  },
  privacyIcon: {
    fontSize: '1.5rem',
  },
  privacyTitle: {
    fontWeight: 700,
    color: '#60A5FA',
    fontSize: '0.95rem',
    marginBottom: '2px',
  },
  privacyDesc: {
    fontSize: '0.85rem',
    color: '#94A3B8',
    lineHeight: 1.4,
  },
  staleBanner: {
    backgroundColor: 'rgba(245, 158, 11, 0.15)',
    border: '1px solid rgba(245, 158, 11, 0.4)',
    color: '#F59E0B',
    padding: '10px 14px',
    borderRadius: '8px',
    fontSize: '0.85rem',
    marginBottom: '16px',
  },
  filterCard: {
    backgroundColor: '#0F172A',
    border: '1px solid #334155',
    borderRadius: '12px',
    padding: '16px',
    marginBottom: '20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '14px',
  },
  filterGroup: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    flexWrap: 'wrap',
  },
  filterLabel: {
    fontSize: '0.85rem',
    fontWeight: 600,
    color: '#94A3B8',
    minWidth: '95px',
  },
  chipRow: {
    display: 'flex',
    gap: '8px',
    flexWrap: 'wrap',
  },
  chip: {
    backgroundColor: '#1E293B',
    color: '#94A3B8',
    border: '1px solid #334155',
    borderRadius: '20px',
    padding: '6px 14px',
    fontSize: '0.85rem',
    cursor: 'pointer',
    transition: 'all 0.15s ease',
  },
  chipActive: {
    backgroundColor: '#2563EB',
    color: '#FFFFFF',
    borderColor: '#2563EB',
    fontWeight: 600,
  },
  chipPurpleActive: {
    backgroundColor: '#7C3AED',
    color: '#FFFFFF',
    borderColor: '#7C3AED',
    fontWeight: 600,
  },
  timelineList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  eventCard: {
    display: 'flex',
    alignItems: 'center',
    backgroundColor: '#0F172A',
    border: '1px solid #1E293B',
    borderRadius: '12px',
    padding: '14px 18px',
    cursor: 'pointer',
    transition: 'border-color 0.15s, transform 0.15s',
  },
  appIconBadge: {
    width: '46px',
    height: '46px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
    fontSize: '0.9rem',
    border: '1px solid',
    marginRight: '14px',
    flexShrink: 0,
  },
  eventBody: {
    flex: 1,
  },
  eventHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '4px',
  },
  appName: {
    fontWeight: 700,
    fontSize: '0.95rem',
    color: '#F8FAFC',
  },
  durationPill: {
    backgroundColor: '#1E293B',
    color: '#60A5FA',
    fontSize: '0.8rem',
    fontWeight: 600,
    padding: '2px 8px',
    borderRadius: '6px',
    border: '1px solid #334155',
  },
  broadActivity: {
    fontSize: '0.95rem',
    fontWeight: 600,
    color: '#F1F5F9',
    marginBottom: '6px',
  },
  eventFooter: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    fontSize: '0.8rem',
  },
  labelTag: {
    backgroundColor: 'rgba(37, 99, 235, 0.15)',
    color: '#60A5FA',
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '0.75rem',
    fontWeight: 600,
  },
  categoryTag: {
    color: '#64748B',
    fontSize: '0.75rem',
    textTransform: 'uppercase',
  },
  timestamp: {
    color: '#64748B',
    fontSize: '0.8rem',
  },
  chevron: {
    fontSize: '1.4rem',
    color: '#64748B',
    marginLeft: '12px',
  },
  paginationRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: '20px',
    padding: '12px 16px',
    backgroundColor: '#0F172A',
    borderRadius: '10px',
    border: '1px solid #1E293B',
  },
  pageButton: {
    backgroundColor: '#1E293B',
    color: '#60A5FA',
    border: '1px solid #334155',
    borderRadius: '6px',
    padding: '6px 14px',
    fontSize: '0.85rem',
    cursor: 'pointer',
    fontWeight: 600,
  },
  pageButtonDisabled: {
    opacity: 0.4,
    cursor: 'not-allowed',
    color: '#64748B',
  },
  pageInfo: {
    fontSize: '0.85rem',
    color: '#94A3B8',
  },
  centerMessage: {
    textAlign: 'center',
    padding: '48px 16px',
    color: '#94A3B8',
  },
  errorMessage: {
    textAlign: 'center',
    padding: '32px 16px',
    color: '#EF4444',
  },
  retryButton: {
    backgroundColor: '#2563EB',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '6px',
    padding: '8px 16px',
    cursor: 'pointer',
    marginTop: '12px',
  },
  spinner: {
    width: '32px',
    height: '32px',
    border: '3px solid #334155',
    borderTopColor: '#2563EB',
    borderRadius: '50%',
    margin: '0 auto 12px auto',
    animation: 'spin 1s linear infinite',
  },
  modalBackdrop: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '16px',
  },
  modalCard: {
    backgroundColor: '#0F172A',
    border: '1px solid #334155',
    borderRadius: '16px',
    width: '100%',
    maxWidth: '520px',
    padding: '24px',
    boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5)',
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '18px',
    borderBottom: '1px solid #1E293B',
    paddingBottom: '14px',
  },
  modalTitle: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
  },
  closeButton: {
    background: 'none',
    border: 'none',
    color: '#94A3B8',
    fontSize: '1.2rem',
    cursor: 'pointer',
  },
  modalContent: {
    display: 'flex',
    flexDirection: 'column',
    gap: '14px',
  },
  detailItem: {
    display: 'flex',
    flexDirection: 'column',
    gap: '3px',
  },
  detailLabel: {
    fontSize: '0.75rem',
    textTransform: 'uppercase',
    color: '#64748B',
    fontWeight: 600,
  },
  detailValue: {
    fontSize: '0.95rem',
    color: '#F8FAFC',
    fontWeight: 500,
  },
  consentedScopeBox: {
    backgroundColor: '#1E293B',
    borderRadius: '8px',
    padding: '12px',
    border: '1px solid #334155',
    marginTop: '6px',
  },
  consentedScopeTitle: {
    fontSize: '0.8rem',
    fontWeight: 700,
    color: '#60A5FA',
    marginBottom: '4px',
  },
  consentedScopeText: {
    fontSize: '0.85rem',
    color: '#94A3B8',
    lineHeight: 1.4,
  },
  modalFooter: {
    marginTop: '20px',
    display: 'flex',
    justifyContent: 'flex-end',
  },
  modalCloseBtn: {
    backgroundColor: '#2563EB',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '8px',
    padding: '8px 20px',
    cursor: 'pointer',
    fontWeight: 600,
  },
};

export default ParentHistoryPage;
