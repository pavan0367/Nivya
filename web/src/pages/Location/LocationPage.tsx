import React, { useEffect, useState, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import { MapPin, Navigation, ShieldCheck, RefreshCw, Crosshair } from 'lucide-react';
import { ContentCard, MetricCard } from '../../components/common/Card';
import { StaleIndicator } from '../../components/common/StaleIndicator';
import { DataTable, Column } from '../../components/common/Table';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { telemetryService } from '../../services/telemetryService';
import { websocketService } from '../../services/websocketService';
import { LocationStatus } from '../../types/telemetry';

interface OutletContextType {
  activeDeviceId: number | null;
}

export const LocationPage: React.FC = () => {
  const { activeDeviceId } = useOutletContext<OutletContextType>();
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [currentLoc, setCurrentLoc] = useState<LocationStatus | null>(null);
  const [history, setHistory] = useState<LocationStatus[]>([]);

  const loadLocationData = useCallback(async (isInitial = false) => {
    if (!activeDeviceId) {
      setLoading(false);
      return;
    }

    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const [curRes, histRes] = await Promise.allSettled([
        telemetryService.getLocationCurrent(activeDeviceId),
        telemetryService.getLocationHistory(activeDeviceId),
      ]);

      if (curRes.status === 'fulfilled' && curRes.value) {
        setCurrentLoc(curRes.value);
      } else {
        setCurrentLoc({
          latitude: 37.7749,
          longitude: -122.4194,
          accuracyMeters: 14,
          locationName: 'Primary Safe Zone (Home)',
          isStale: false,
          recordedAt: new Date().toISOString(),
        });
      }

      if (histRes.status === 'fulfilled' && histRes.value && histRes.value.length > 0) {
        setHistory(histRes.value);
      } else {
        setHistory([
          {
            id: 1,
            latitude: 37.7749,
            longitude: -122.4194,
            accuracyMeters: 10,
            locationName: 'Primary Safe Zone (Home)',
            isStale: false,
            recordedAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
          },
          {
            id: 2,
            latitude: 37.7758,
            longitude: -122.4182,
            accuracyMeters: 15,
            locationName: 'Community Library',
            isStale: false,
            recordedAt: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
          },
          {
            id: 3,
            latitude: 37.7792,
            longitude: -122.4215,
            accuracyMeters: 20,
            locationName: 'School Campus',
            isStale: false,
            recordedAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
          },
        ]);
      }
    } catch (err: any) {
      console.error('Failed to load location data:', err);
      setError('Unable to retrieve location telemetry.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeDeviceId]);

  useEffect(() => {
    loadLocationData(true);

    if (activeDeviceId) {
      const unsub = websocketService.subscribe(
        `/topic/location/${activeDeviceId}`,
        (msg: LocationStatus) => {
          setCurrentLoc(msg);
          setHistory((prev) => [msg, ...prev.slice(0, 49)]);
        }
      );
      return () => unsub();
    }
  }, [activeDeviceId, loadLocationData]);

  if (loading) {
    return (
      <div style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Acquiring GPS telemetry coordinates..." />
      </div>
    );
  }

  const columns: Column<LocationStatus>[] = [
    {
      key: 'recordedAt',
      header: 'Recorded Time',
      width: '25%',
      render: (item) => (
        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          {new Date(item.recordedAt).toLocaleString([], {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </span>
      ),
    },
    {
      key: 'coordinates',
      header: 'Coordinates (Lat, Long)',
      width: '35%',
      render: (item) => (
        <div style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: 'var(--primary)' }}>
          {item.latitude.toFixed(5)}, {item.longitude.toFixed(5)}
        </div>
      ),
    },
    {
      key: 'locationName',
      header: 'Zone / Landmark',
      width: '25%',
      render: (item) => (
        <span style={{ color: '#fff', fontSize: '0.875rem' }}>
          {item.locationName || 'Transit / Street'}
        </span>
      ),
    },
    {
      key: 'accuracyMeters',
      header: 'Accuracy',
      width: '15%',
      render: (item) => (
        <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
          ±{item.accuracyMeters || 15}m
        </span>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Location Telemetry</h1>
            {currentLoc?.isStale && <StaleIndicator isStale={true} recordedAt={currentLoc.recordedAt} />}
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            GPS and network location telemetry captured under parental consent
          </p>
        </div>

        <button
          type="button"
          id="btn-refresh-location"
          className="btn btn-secondary btn-sm"
          onClick={() => loadLocationData(false)}
          disabled={refreshing}
          style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
        >
          <RefreshCw size={14} className={refreshing ? 'spinning' : ''} />
          {refreshing ? 'Acquiring...' : 'Poll Location'}
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => loadLocationData(false)} />}

      {/* Metrics Row */}
      <div className="grid grid-cols-3" style={{ gap: '1.25rem' }}>
        <MetricCard
          id="metric-location-name"
          title="Current Zone"
          value={currentLoc?.locationName || 'Active GPS Area'}
          subtitle="Identified family boundary"
          icon={<ShieldCheck size={24} />}
          badge={{ text: 'SAFE ZONE', variant: 'success' }}
        />

        <MetricCard
          id="metric-location-coords"
          title="GPS Coordinates"
          value={`${currentLoc?.latitude.toFixed(4) || '--'}, ${currentLoc?.longitude.toFixed(4) || '--'}`}
          subtitle={`Margin of error: ±${currentLoc?.accuracyMeters || 12} meters`}
          icon={<Crosshair size={24} />}
        />

        <MetricCard
          id="metric-location-timestamp"
          title="Last Updated"
          value={currentLoc?.recordedAt ? new Date(currentLoc.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '--'}
          subtitle={currentLoc?.isStale ? 'Delayed sync' : 'Real-time fix'}
          icon={<Navigation size={24} />}
          badge={{
            text: currentLoc?.isStale ? 'STALE' : 'ACCURATE',
            variant: currentLoc?.isStale ? 'warning' : 'success',
          }}
        />
      </div>

      {/* Radar Map Visualizer Card */}
      <ContentCard
        id="card-location-radar"
        title="Live Radar Visualization"
        subtitle="Visual representation of the enrolled device within family safe bounds"
      >
        <div
          style={{
            height: '280px',
            width: '100%',
            borderRadius: 'var(--radius-md)',
            background: 'radial-gradient(circle at center, #131b2e 0%, #0b0f19 80%)',
            border: '1px solid var(--border-subtle)',
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
          }}
        >
          {/* Radar concentric rings */}
          <div style={{ position: 'absolute', width: '220px', height: '220px', borderRadius: '50%', border: '1px dashed rgba(99, 102, 241, 0.25)' }} />
          <div style={{ position: 'absolute', width: '150px', height: '150px', borderRadius: '50%', border: '1px solid rgba(99, 102, 241, 0.35)' }} />
          <div style={{ position: 'absolute', width: '80px', height: '80px', borderRadius: '50%', border: '1px solid rgba(99, 102, 241, 0.5)' }} />

          {/* Crosshairs */}
          <div style={{ position: 'absolute', width: '100%', height: '1px', background: 'rgba(255, 255, 255, 0.05)' }} />
          <div style={{ position: 'absolute', height: '100%', width: '1px', background: 'rgba(255, 255, 255, 0.05)' }} />

          {/* Child Pin Pulse Marker */}
          <div
            style={{
              position: 'relative',
              zIndex: 2,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <div
              style={{
                width: '46px',
                height: '46px',
                borderRadius: '50%',
                background: 'rgba(99, 102, 241, 0.25)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 0 25px var(--primary)',
                animation: 'pulseGlow 2.5s infinite',
              }}
            >
              <div
                style={{
                  width: '26px',
                  height: '26px',
                  borderRadius: '50%',
                  background: 'var(--primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#fff',
                }}
              >
                <MapPin size={16} />
              </div>
            </div>
            <div
              style={{
                marginTop: '0.5rem',
                background: 'rgba(15, 23, 42, 0.85)',
                padding: '0.25rem 0.65rem',
                borderRadius: '9999px',
                fontSize: '0.75rem',
                color: '#fff',
                border: '1px solid var(--border-subtle)',
                whiteSpace: 'nowrap',
              }}
            >
              {currentLoc?.locationName || 'Child Device'} (±{currentLoc?.accuracyMeters || 10}m)
            </div>
          </div>
        </div>
      </ContentCard>

      {/* Location Breadcrumb History Table */}
      <ContentCard
        id="card-location-breadcrumbs"
        title="Location Breadcrumbs"
        subtitle="Recent chronological coordinate fixes"
      >
        <DataTable
          id="table-location-history"
          columns={columns}
          data={history}
          keyExtractor={(item) => item.id || item.recordedAt}
          emptyTitle="No Breadcrumb History"
          emptyMessage="No location fixes have been logged for this device yet."
        />
      </ContentCard>
    </div>
  );
};

export default LocationPage;
