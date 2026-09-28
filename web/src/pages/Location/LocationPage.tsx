import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Navigation, ShieldCheck, RefreshCw, Crosshair, MapPin } from 'lucide-react';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { ContentCard, MetricCard } from '../../components/common/Card';
import { StaleIndicator } from '../../components/common/StaleIndicator';
import { DataTable, Column } from '../../components/common/Table';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { telemetryService } from '../../services/telemetryService';
import { websocketService } from '../../services/websocketService';
import { LocationStatus } from '../../types/telemetry';
import { isValidCoordinate } from '../../utils/locationUtils';

export { isValidCoordinate };

export const GOOGLE_MAPS_API_KEY = (import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string) || '';

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

  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markerRef = useRef<L.Marker | null>(null);
  const accuracyCircleRef = useRef<L.Circle | null>(null);
  const hasCenteredRef = useRef<boolean>(false);

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

      if (curRes.status === 'fulfilled' && curRes.value && isValidCoordinate(curRes.value.latitude, curRes.value.longitude)) {
        setCurrentLoc(curRes.value);
      } else {
        setCurrentLoc(null);
      }

      if (histRes.status === 'fulfilled' && histRes.value && histRes.value.length > 0) {
        setHistory(histRes.value.filter((loc) => isValidCoordinate(loc.latitude, loc.longitude)));
      } else {
        setHistory([]);
      }
    } catch (err: any) {
      console.error('Failed to load location data:', err);
      setError('Unable to retrieve location telemetry.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeDeviceId]);

  // Initial fetch and WebSocket subscription
  useEffect(() => {
    hasCenteredRef.current = false;
    loadLocationData(true);

    if (activeDeviceId) {
      const unsub = websocketService.subscribe(
        `/topic/location/${activeDeviceId}`,
        (msg: LocationStatus) => {
          if (isValidCoordinate(msg.latitude, msg.longitude)) {
            setCurrentLoc(msg);
            setHistory((prev) => [msg, ...prev.slice(0, 49)]);
          }
        }
      );

      const unsubReconnect = websocketService.onReconnect(() => {
        loadLocationData(false);
      });

      return () => {
        unsub();
        unsubReconnect();
      };
    }
  }, [activeDeviceId, loadLocationData]);

  // Initialize and update OpenStreetMap (Leaflet) instance
  useEffect(() => {
    if (!mapContainerRef.current) return;

    const hasValidCoords = currentLoc && isValidCoordinate(currentLoc.latitude, currentLoc.longitude);
    if (!hasValidCoords || !currentLoc) return;

    const { latitude, longitude, accuracyMeters, locationName } = currentLoc;
    const accuracy = Math.max(accuracyMeters || 15, 5);

    // Initialize Leaflet Map with HTTPS OpenStreetMap tiles if not yet created
    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [latitude, longitude],
        zoom: 16,
        zoomControl: true,
        attributionControl: true,
      });

      // Standard HTTPS OpenStreetMap tiles with visible attribution (zero billing, 100% free)
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors',
        maxZoom: 19,
      }).addTo(map);

      mapInstanceRef.current = map;
    }

    const map = mapInstanceRef.current;

    // Custom pulsing circular icon
    const customIcon = L.divIcon({
      className: 'custom-live-marker',
      html: `
        <div class="pulsing-marker-wrapper">
          <div class="pulsing-ring ring-1"></div>
          <div class="pulsing-ring ring-2"></div>
          <div class="marker-core">
            <div class="marker-dot"></div>
          </div>
        </div>
      `,
      iconSize: [44, 44],
      iconAnchor: [22, 22],
      popupAnchor: [0, -22],
    });

    // Create or update marker position from actual telemetry only
    if (!markerRef.current) {
      markerRef.current = L.marker([latitude, longitude], {
        icon: customIcon,
        title: locationName || 'Child Device Position',
      }).addTo(map);
    } else {
      markerRef.current.setLatLng([latitude, longitude]);
    }

    // Bind / update popup
    markerRef.current.bindPopup(`
      <div style="font-family: inherit; font-size: 0.825rem; line-height: 1.5; color: #0f172a; padding: 2px;">
        <strong style="color: #4338ca; font-size: 0.875rem;">${locationName || 'Child Device'}</strong><br/>
        <span><strong>Lat:</strong> ${latitude.toFixed(5)}</span><br/>
        <span><strong>Lng:</strong> ${longitude.toFixed(5)}</span><br/>
        <span><strong>Accuracy:</strong> ±${Math.round(accuracy)}m</span>
      </div>
    `);

    // Create or update accuracy circle
    if (!accuracyCircleRef.current) {
      accuracyCircleRef.current = L.circle([latitude, longitude], {
        radius: accuracy,
        color: '#6366f1',
        weight: 1.5,
        opacity: 0.6,
        fillColor: '#6366f1',
        fillOpacity: 0.15,
      }).addTo(map);
    } else {
      accuracyCircleRef.current.setLatLng([latitude, longitude]);
      accuracyCircleRef.current.setRadius(accuracy);
    }

    // UX Rule: Only center camera on initial valid location fix
    // DO NOT panTo or setView on subsequent WebSocket telemetry updates to prevent hijacking user pan/zoom
    if (!hasCenteredRef.current) {
      map.setView([latitude, longitude], 16);
      hasCenteredRef.current = true;
    }

    setTimeout(() => {
      map.invalidateSize();
    }, 200);
  }, [currentLoc]);

  // Teardown map on device switch or unmount
  useEffect(() => {
    return () => {
      if (markerRef.current) {
        markerRef.current.remove();
        markerRef.current = null;
      }
      if (accuracyCircleRef.current) {
        accuracyCircleRef.current.remove();
        accuracyCircleRef.current = null;
      }
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
      hasCenteredRef.current = false;
    };
  }, [activeDeviceId]);

  const handleRecenter = () => {
    if (mapInstanceRef.current && currentLoc && isValidCoordinate(currentLoc.latitude, currentLoc.longitude)) {
      mapInstanceRef.current.flyTo([currentLoc.latitude, currentLoc.longitude], 16, {
        animate: true,
        duration: 1.0,
      });
    }
  };


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
          ±{Math.round(item.accuracyMeters || 15)}m
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
          value={currentLoc ? (currentLoc.locationName || 'Active GPS Area') : 'Unavailable'}
          subtitle={currentLoc ? 'Identified family boundary' : 'Waiting for device data'}
          icon={<ShieldCheck size={24} />}
          badge={currentLoc ? { text: 'SAFE ZONE', variant: 'success' } : { text: 'NO FIX', variant: 'neutral' }}
        />

        <MetricCard
          id="metric-location-coords"
          title="GPS Coordinates"
          value={currentLoc ? `${currentLoc.latitude.toFixed(4)}, ${currentLoc.longitude.toFixed(4)}` : 'Unavailable'}
          subtitle={currentLoc ? `Margin of error: ±${Math.round(currentLoc.accuracyMeters || 12)} meters` : 'Waiting for GPS fix'}
          icon={<Crosshair size={24} />}
        />

        <MetricCard
          id="metric-location-timestamp"
          title="Last Updated"
          value={currentLoc?.recordedAt ? new Date(currentLoc.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'Unavailable'}
          subtitle={currentLoc ? (currentLoc.isStale ? 'Delayed sync' : 'Real-time fix') : 'Waiting for device data'}
          icon={<Navigation size={24} />}
          badge={currentLoc ? {
            text: currentLoc.isStale ? 'STALE' : 'ACCURATE',
            variant: currentLoc.isStale ? 'warning' : 'success',
          } : {
            text: 'NO DATA',
            variant: 'neutral',
          }}
        />
      </div>

      {/* Real Live Map Visualizer Card */}
      <ContentCard
        id="card-location-radar"
        title="Live Interactive Device Map"
        subtitle="Real-time geographic position of the enrolled child device"
      >
        <div
          style={{
            position: 'relative',
            width: '100%',
            height: '420px',
            borderRadius: 'var(--radius-md)',
            overflow: 'hidden',
            border: '1px solid var(--border-subtle)',
            backgroundColor: '#0b0f19',
          }}
        >
          {currentLoc && isValidCoordinate(currentLoc.latitude, currentLoc.longitude) ? (
            <>
              <div
                id="live-location-map"
                ref={mapContainerRef}
                style={{ width: '100%', height: '100%', zIndex: 1 }}
              />
              <button
                type="button"
                id="btn-recenter-location"
                className="map-recenter-btn"
                onClick={handleRecenter}
                title="Recenter Map on Device"
              >
                <Crosshair size={15} />
                <span>Recenter Device</span>
              </button>
            </>
          ) : (
            <div
              style={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--text-dim)',
                fontSize: '0.875rem',
                gap: '0.75rem',
              }}
            >
              <div
                style={{
                  width: '48px',
                  height: '48px',
                  borderRadius: '50%',
                  background: 'rgba(255, 255, 255, 0.04)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--text-muted)',
                }}
              >
                <MapPin size={24} />
              </div>
              <span>Waiting for real-time device GPS coordinates from mobile...</span>
            </div>
          )}
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
