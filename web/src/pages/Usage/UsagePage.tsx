import React, { useEffect, useState, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Clock, PieChart, Smartphone, RefreshCw, BarChart2 } from 'lucide-react';
import { ContentCard, MetricCard } from '../../components/common/Card';
import { BarChart } from '../../components/common/Chart';
import { DataTable, Column } from '../../components/common/Table';
import { DateSelector, DatePreset } from '../../components/common/DateSelector';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { telemetryService } from '../../services/telemetryService';
import { UsageSummary, AppUsage } from '../../types/telemetry';

interface OutletContextType {
  activeDeviceId: number | null;
}

export const UsagePage: React.FC = () => {
  const { activeDeviceId } = useOutletContext<OutletContextType>();
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [datePreset, setDatePreset] = useState<DatePreset>('TODAY');
  const [customDate, setCustomDate] = useState<string>('');
  const [usage, setUsage] = useState<UsageSummary | null>(null);

  const loadUsage = useCallback(async (isInitial = false) => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }

    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    let dateParam: string | undefined = undefined;
    const now = new Date();
    if (datePreset === 'TODAY') {
      dateParam = now.toISOString().split('T')[0];
    } else if (datePreset === 'YESTERDAY') {
      const yest = new Date(now.getTime() - 24 * 60 * 60 * 1000);
      dateParam = yest.toISOString().split('T')[0];
    } else if (datePreset === 'CUSTOM' && customDate) {
      dateParam = customDate;
    }

    try {
      const data = await telemetryService.getUsageSummary(activeDeviceId, dateParam);
      if (data) {
        setUsage(data);
      } else {
        // Fallback demo data
        setUsage({
          deviceId: activeDeviceId,
          date: dateParam || now.toISOString().split('T')[0],
          totalScreenTimeMinutes: 245,
          categories: {
            Education: 90,
            Entertainment: 75,
            Communication: 50,
            Utilities: 30,
          },
          appUsages: [
            {
              id: 1,
              packageName: 'org.khanacademy.android',
              appName: 'Khan Academy',
              category: 'Education',
              durationMinutes: 90,
              openCount: 4,
            },
            {
              id: 2,
              packageName: 'com.google.android.apps.youtube.kids',
              appName: 'YouTube Kids',
              category: 'Entertainment',
              durationMinutes: 75,
              openCount: 6,
            },
            {
              id: 3,
              packageName: 'com.whatsapp',
              appName: 'WhatsApp',
              category: 'Communication',
              durationMinutes: 50,
              openCount: 12,
            },
            {
              id: 4,
              packageName: 'com.google.android.apps.nbu.files',
              appName: 'Files by Google',
              category: 'Utilities',
              durationMinutes: 30,
              openCount: 5,
            },
          ],
        });
      }
    } catch (err: any) {
      console.error('Failed to load screen time usage:', err);
      setError('Unable to load application screen time summary.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeDeviceId, datePreset, customDate]);

  useEffect(() => {
    loadUsage(true);
  }, [loadUsage]);

  if (loading) {
    return (
      <div style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Analyzing consented application foreground usage..." />
      </div>
    );
  }

  const totalMinutes = usage?.totalScreenTimeMinutes || 0;
  const hours = Math.floor(totalMinutes / 60);
  const mins = totalMinutes % 60;
  const screenTimeText = `${hours}h ${mins}m`;

  const appUsages = usage?.appUsages || [];

  // Chart data for categories
  const categoryChartData = usage?.categories
    ? Object.entries(usage.categories).map(([cat, m]) => ({
        label: cat,
        value: m,
      }))
    : [];

  const columns: Column<AppUsage>[] = [
    {
      key: 'appName',
      header: 'Application',
      width: '30%',
      render: (item) => (
        <div>
          <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.925rem' }}>{item.appName}</div>
          <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem', fontFamily: 'monospace' }}>
            {item.packageName}
          </small>
        </div>
      ),
    },
    {
      key: 'category',
      header: 'Category',
      width: '20%',
      render: (item) => (
        <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
          {item.category}
        </span>
      ),
    },
    {
      key: 'durationMinutes',
      header: 'Foreground Time',
      width: '25%',
      render: (item) => {
        const h = Math.floor(item.durationMinutes / 60);
        const m = item.durationMinutes % 60;
        const timeStr = h > 0 ? `${h}h ${m}m` : `${m}m`;
        const pct = totalMinutes > 0 ? Math.round((item.durationMinutes / totalMinutes) * 100) : 0;
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
              <span style={{ fontWeight: 600, color: '#fff' }}>{timeStr}</span>
              <span style={{ color: 'var(--text-dim)' }}>{pct}%</span>
            </div>
            <div style={{ width: '100%', height: '5px', background: 'rgba(255, 255, 255, 0.08)', borderRadius: '3px', overflow: 'hidden' }}>
              <div style={{ width: `${pct}%`, height: '100%', background: 'var(--primary)', borderRadius: '3px' }} />
            </div>
          </div>
        );
      },
    },
    {
      key: 'openCount',
      header: 'Launches',
      width: '15%',
      align: 'right',
      render: (item) => (
        <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          {item.openCount ? `${item.openCount} opens` : '—'}
        </span>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Screen Time & App Usage</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Daily application screen time and category breakdowns without inspecting private content
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <DateSelector
            id="usage-date-selector"
            selectedPreset={datePreset}
            onPresetChange={(p) => setDatePreset(p)}
            customDate={customDate}
            onCustomDateChange={(d) => setCustomDate(d)}
          />
          <button
            type="button"
            id="btn-refresh-usage"
            className="btn btn-secondary btn-sm"
            onClick={() => loadUsage(false)}
            disabled={refreshing}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
          >
            <RefreshCw size={14} className={refreshing ? 'spinning' : ''} />
            {refreshing ? 'Updating...' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => loadUsage(false)} />}

      {/* Overview Metric Cards */}
      <div className="grid grid-cols-3" style={{ gap: '1.25rem' }}>
        <MetricCard
          id="metric-total-screentime"
          title="Total Screen Time"
          value={screenTimeText}
          subtitle={`Aggregated for ${usage?.date || 'today'}`}
          icon={<Clock size={24} />}
          badge={{ text: totalMinutes > 300 ? 'HIGH USAGE' : 'BALANCED', variant: totalMinutes > 300 ? 'warning' : 'success' }}
        />

        <MetricCard
          id="metric-top-category"
          title="Top Category"
          value={categoryChartData.length > 0 ? categoryChartData[0].label : 'None'}
          subtitle={categoryChartData.length > 0 ? `${categoryChartData[0].value} minutes recorded` : 'No data'}
          icon={<PieChart size={24} />}
          badge={{ text: 'PRIMARY', variant: 'neutral' }}
        />

        <MetricCard
          id="metric-active-apps"
          title="Active Apps Used"
          value={appUsages.length}
          subtitle="Foreground applications launched"
          icon={<Smartphone size={24} />}
        />
      </div>

      {/* Category Chart Card */}
      <ContentCard
        id="card-category-distribution"
        title="Category Breakdown"
        subtitle="Minutes spent in each application category"
      >
        <div style={{ marginTop: '0.5rem' }}>
          <BarChart
            id="chart-usage-categories"
            data={categoryChartData}
            height={200}
            color="var(--accent)"
            unit="m"
          />
        </div>
      </ContentCard>

      {/* Detailed Applications Table */}
      <ContentCard
        id="card-apps-table"
        title="Application Usage Ranking"
        subtitle="Consented foreground time captured via authorized Android usage stats"
      >
        <DataTable
          id="table-app-usage"
          columns={columns}
          data={appUsages}
          keyExtractor={(item) => item.id || item.packageName}
          emptyTitle="No Application Usage Data"
          emptyMessage="No foreground application stats reported for the selected date."
        />
      </ContentCard>
    </div>
  );
};

export default UsagePage;
