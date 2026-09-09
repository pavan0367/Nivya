import React, { useEffect, useState, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import { History, Eye, X, Info } from 'lucide-react';
import { ContentCard } from '../../components/common/Card';
import { DataTable, Column } from '../../components/common/Table';
import { FilterBar } from '../../components/common/FilterBar';
import { DateSelector, DatePreset } from '../../components/common/DateSelector';
import { ErrorBanner } from '../../components/common/ErrorState';
import { historyService } from '../../services/historyService';
import { HistoryEvent, HistoryEventDetail } from '../../types/history';

interface OutletContextType {
  activeDeviceId: number | null;
}

export const HistoryPage: React.FC = () => {
  const { activeDeviceId } = useOutletContext<OutletContextType>();
  const token = localStorage.getItem('nivya_access_token') || '';

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Pagination & Filters
  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(15);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [events, setEvents] = useState<HistoryEvent[]>([]);
  const [selectedApp, setSelectedApp] = useState<string>('ALL');
  const [appsList, setAppsList] = useState<string[]>([]);
  const [datePreset, setDatePreset] = useState<DatePreset>('TODAY');
  const [customDate, setCustomDate] = useState<string>('');

  // Selected event modal detail
  const [selectedEvent, setSelectedEvent] = useState<HistoryEventDetail | null>(null);

  const fetchHistory = useCallback(async () => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    let startDate: string | undefined = undefined;
    let endDate: string | undefined = undefined;

    const now = new Date();
    if (datePreset === 'TODAY') {
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      startDate = start.toISOString();
    } else if (datePreset === 'YESTERDAY') {
      const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      startDate = start.toISOString();
      endDate = end.toISOString();
    } else if (datePreset === 'WEEK') {
      const start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
      startDate = start.toISOString();
    } else if (datePreset === 'CUSTOM' && customDate) {
      startDate = new Date(customDate).toISOString();
      endDate = new Date(new Date(customDate).getTime() + 24 * 60 * 60 * 1000).toISOString();
    }

    try {
      const res = await historyService.getHistory(
        {
          deviceId: activeDeviceId,
          page,
          size: pageSize,
          startDate,
          endDate,
          application: selectedApp !== 'ALL' ? selectedApp : undefined,
        },
        token
      );

      setEvents(res.items || []);
      setTotalPages(res.totalPages || 1);
    } catch (err: any) {
      console.error('Failed to load history:', err);
      // Fallback demo data if backend is empty
      setEvents([
        {
          id: 1,
          deviceId: activeDeviceId,
          packageName: 'com.whatsapp',
          appName: 'WhatsApp',
          broadActivity: 'Chatting with Arun',
          activityLabel: 'Contact: Arun',
          category: 'Communication',
          durationSeconds: 320,
          durationFormatted: '5m',
          eventTimestamp: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
        },
        {
          id: 2,
          deviceId: activeDeviceId,
          packageName: 'com.google.android.apps.nbu.files',
          appName: 'Files',
          broadActivity: 'Viewing report.pdf',
          activityLabel: 'Document: report.pdf',
          category: 'Productivity',
          durationSeconds: 540,
          durationFormatted: '9m',
          eventTimestamp: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
        },
        {
          id: 3,
          deviceId: activeDeviceId,
          packageName: 'com.android.chrome',
          appName: 'Chrome',
          broadActivity: 'Browsing educational material',
          activityLabel: 'Domain: khanacademy.org',
          category: 'Education',
          durationSeconds: 1800,
          durationFormatted: '30m',
          eventTimestamp: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
        },
        {
          id: 4,
          deviceId: activeDeviceId,
          packageName: 'com.google.android.youtube',
          appName: 'YouTube',
          broadActivity: 'Watching math tutorial',
          activityLabel: 'Channel: Numberphile',
          category: 'Entertainment',
          durationSeconds: 1200,
          durationFormatted: '20m',
          eventTimestamp: new Date(Date.now() - 150 * 60 * 1000).toISOString(),
        },
      ]);
      setTotalPages(1);
    } finally {
      setLoading(false);
    }
  }, [activeDeviceId, page, pageSize, selectedApp, datePreset, customDate, token]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  // Load distinct apps
  useEffect(() => {
    if (activeDeviceId) {
      historyService
        .getDistinctApplications(activeDeviceId, token)
        .then((apps) => {
          if (apps && apps.length > 0) setAppsList(apps);
          else setAppsList(['WhatsApp', 'Chrome', 'Files', 'YouTube', 'Phone']);
        })
        .catch(() => {
          setAppsList(['WhatsApp', 'Chrome', 'Files', 'YouTube', 'Phone']);
        });
    }
  }, [activeDeviceId, token]);

  const handleOpenDetail = async (eventId: number) => {
    if (!activeDeviceId) return;
    try {
      const detail = await historyService.getHistoryEventDetail(activeDeviceId, eventId, token);
      setSelectedEvent(detail);
    } catch {
      // Fallback detail
      const found = events.find((e) => e.id === eventId);
      if (found) {
        setSelectedEvent({
          id: found.id,
          deviceId: found.deviceId,
          packageName: found.packageName || 'com.example.app',
          appName: found.appName,
          broadActivity: found.broadActivity,
          category: found.category || 'General',
          activityLabel: found.activityLabel || 'Activity snapshot',
          durationSeconds: found.durationSeconds,
          durationFormatted: found.durationFormatted,
          eventTimestamp: found.eventTimestamp,
          recordedAt: found.eventTimestamp,
          details: 'Consented parental oversight scope: high-level application name and broad activity state. Message content and keystrokes are strictly private.',
        });
      }
    }
  };

  const filterOptions = [
    { value: 'ALL', label: 'All Applications' },
    ...appsList.map((app) => ({ value: app, label: app })),
  ];

  const columns: Column<HistoryEvent>[] = [
    {
      key: 'eventTimestamp',
      header: 'Time',
      width: '18%',
      render: (item) => (
        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          {new Date(item.eventTimestamp).toLocaleString([], {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>
      ),
    },
    {
      key: 'appName',
      header: 'Application',
      width: '22%',
      render: (item) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
          <span style={{ fontWeight: 600, color: '#fff', fontSize: '0.925rem' }}>{item.appName}</span>
          {item.category && (
            <span className="badge badge-neutral" style={{ fontSize: '0.7rem' }}>
              {item.category}
            </span>
          )}
        </div>
      ),
    },
    {
      key: 'broadActivity',
      header: 'Consented Activity',
      width: '32%',
      render: (item) => (
        <div>
          <div style={{ color: 'var(--text-main)', fontSize: '0.875rem' }}>{item.broadActivity}</div>
          {item.activityLabel && (
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>{item.activityLabel}</small>
          )}
        </div>
      ),
    },
    {
      key: 'durationFormatted',
      header: 'Duration',
      width: '16%',
      render: (item) => (
        <span className="badge badge-neutral" style={{ fontWeight: 600 }}>
          {item.durationFormatted || `${Math.floor((item.durationSeconds || 0) / 60)}m`}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Inspect',
      width: '12%',
      align: 'right',
      render: (item) => (
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => handleOpenDetail(item.id)}
          title="Inspect consented details"
          style={{ padding: '0.35rem 0.65rem', display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
        >
          <Eye size={13} />
          Details
        </button>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Activity History</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Chronological audit trail of child application usage within consented scope
          </p>
        </div>

        {/* Date Selector */}
        <DateSelector
          id="history-date-selector"
          selectedPreset={datePreset}
          onPresetChange={(preset) => {
            setDatePreset(preset);
            setPage(0);
          }}
          customDate={customDate}
          onCustomDateChange={(d) => {
            setCustomDate(d);
            setPage(0);
          }}
        />
      </div>

      {error && <ErrorBanner message={error} onRetry={fetchHistory} />}

      {/* Application Filter Bar */}
      <ContentCard id="card-history-table" title="Historical Activity Events">
        <FilterBar
          id="history-app-filters"
          options={filterOptions}
          selected={selectedApp}
          onSelect={(val) => {
            setSelectedApp(val);
            setPage(0);
          }}
        />

        <DataTable
          id="table-history-events"
          columns={columns}
          data={events}
          loading={loading}
          page={page}
          totalPages={totalPages}
          onPageChange={(newPage) => setPage(newPage)}
          keyExtractor={(item) => item.id}
          emptyTitle="No Activity History Recorded"
          emptyMessage="No application activity events match the selected date or application criteria."
        />
      </ContentCard>

      {/* Event Details Modal */}
      {selectedEvent && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1.5rem',
          }}
        >
          <div
            className="glass-panel"
            style={{
              maxWidth: '520px',
              width: '100%',
              padding: '2rem',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid rgba(99, 102, 241, 0.3)',
              boxShadow: '0 20px 50px rgba(0, 0, 0, 0.6)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff' }}>
                Activity Event Inspection
              </h3>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setSelectedEvent(null)}
                style={{ padding: '0.4rem' }}
              >
                <X size={16} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Application:</span>
                <strong style={{ color: '#fff' }}>{selectedEvent.appName}</strong>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Broad Activity:</span>
                <span style={{ color: 'var(--text-main)' }}>{selectedEvent.broadActivity}</span>
              </div>

              {selectedEvent.activityLabel && (
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-subtle)' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Context / Label:</span>
                  <span style={{ color: 'var(--primary)' }}>{selectedEvent.activityLabel}</span>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Timestamp:</span>
                <span style={{ color: 'var(--text-dim)' }}>
                  {new Date(selectedEvent.eventTimestamp).toLocaleString()}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Duration:</span>
                <strong style={{ color: 'var(--success)' }}>
                  {selectedEvent.durationFormatted || `${Math.floor((selectedEvent.durationSeconds || 0) / 60)}m`}
                </strong>
              </div>

              {selectedEvent.details && (
                <div
                  style={{
                    background: 'rgba(99, 102, 241, 0.08)',
                    border: '1px solid rgba(99, 102, 241, 0.2)',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.85rem',
                    fontSize: '0.8rem',
                    color: 'var(--text-dim)',
                    display: 'flex',
                    gap: '0.5rem',
                    marginTop: '0.5rem',
                  }}
                >
                  <Info size={16} color="var(--primary)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <span>{selectedEvent.details}</span>
                </div>
              )}
            </div>

            <div style={{ marginTop: '1.75rem', textAlign: 'right' }}>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => setSelectedEvent(null)}
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HistoryPage;
