import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  BatteryCharging,
  Clock,
  Wifi,
  Activity,
  MapPin,
  Cpu,
  Bell,
  ChevronRight,
  AlertTriangle,
  Check,
} from 'lucide-react';
import { convocationService } from '../../services/convocationService';
import { telemetryService } from '../../services/telemetryService';
import { alertService } from '../../services/alertService';
import { BatteryStatus, NetworkStatus, LocationStatus, UsageSummary, DeviceHealth } from '../../types/telemetry';
import { Alert } from '../../types/alerts';
import { ChildOutletContext } from '../../layouts/ChildLayout';

export const ChildDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { activeDeviceId } = useOutletContext<ChildOutletContext>();

  // Delayed Visual States — "Sending..." is exception-only (shown only after grace period)
  const [showCrackSending, setShowCrackSending] = useState(false);
  const [showFreakSending, setShowFreakSending] = useState(false);
  const [showDoneModal, setShowDoneModal] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // In-flight dispatch tracking without UI blocking
  const isCrackInFlight = useRef(false);
  const isFreakInFlight = useRef(false);
  const crackTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const freakTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (crackTimerRef.current) clearTimeout(crackTimerRef.current);
      if (freakTimerRef.current) clearTimeout(freakTimerRef.current);
    };
  }, []);

  // Real Telemetry States — No fake / hardcoded values
  const [battery, setBattery] = useState<BatteryStatus | null>(null);
  const [usage, setUsage] = useState<UsageSummary | null>(null);
  const [network, setNetwork] = useState<NetworkStatus | null>(null);
  const [location, setLocation] = useState<LocationStatus | null>(null);
  const [health, setHealth] = useState<DeviceHealth | null>(null);
  const [alerts, setAlerts] = useState<Alert[]>([]);

  // Load real device data
  const loadRealTelemetry = useCallback(async () => {
    if (!activeDeviceId) return;

    try {
      const [batRes, useRes, netRes, locRes, hlthRes, alrRes] = await Promise.allSettled([
        telemetryService.getBatteryStatus(activeDeviceId),
        telemetryService.getUsageSummary(activeDeviceId),
        telemetryService.getNetworkStatus(activeDeviceId),
        telemetryService.getLocationCurrent(activeDeviceId),
        telemetryService.getDeviceHealth(activeDeviceId),
        alertService.getAlerts(activeDeviceId),
      ]);

      if (batRes.status === 'fulfilled') setBattery(batRes.value || null);
      if (useRes.status === 'fulfilled') setUsage(useRes.value || null);
      if (netRes.status === 'fulfilled') setNetwork(netRes.value || null);
      if (locRes.status === 'fulfilled') setLocation(locRes.value || null);
      if (hlthRes.status === 'fulfilled') setHealth(hlthRes.value || null);
      if (alrRes.status === 'fulfilled') setAlerts(alrRes.value || []);
    } catch {
      // Keep null states — do not fabricate fake data
    }
  }, [activeDeviceId]);

  useEffect(() => {
    loadRealTelemetry();
  }, [loadRealTelemetry]);

  // Requirement 1: Success popup automatically closes after ~2 seconds
  useEffect(() => {
    if (showDoneModal) {
      const timer = setTimeout(() => {
        setShowDoneModal(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [showDoneModal]);

  // CRACK: Immediate dispatch of exact "Mom,here"
  const handleSendCrack = async () => {
    if (isCrackInFlight.current || isFreakInFlight.current) return;
    isCrackInFlight.current = true;
    setErrorMessage(null);

    // Grace period timer: Only show "Sending..." if request remains delayed after 400ms
    crackTimerRef.current = setTimeout(() => {
      if (isCrackInFlight.current) {
        setShowCrackSending(true);
      }
    }, 400);

    try {
      // 1. Start real backend request IMMEDIATELY without delay
      await convocationService.childSendMessage('Mom,here');
      // 2. Only show approved DONE toast on actual backend success
      setShowDoneModal(true);
    } catch (err: any) {
      console.error('Failed to send CRACK note:', err);
      // Requirement 16: Do NOT show fake DONE on failure. Keep screen usable with error message.
      const msg = err.response?.data?.message || 'Failed to send message. Please check connection.';
      setErrorMessage(msg);
    } finally {
      if (crackTimerRef.current) clearTimeout(crackTimerRef.current);
      isCrackInFlight.current = false;
      setShowCrackSending(false);
    }
  };

  // FREAK: Immediate dispatch of exact "Someone's,here"
  const handleSendFreak = async () => {
    if (isCrackInFlight.current || isFreakInFlight.current) return;
    isFreakInFlight.current = true;
    setErrorMessage(null);

    // Grace period timer: Only show "Sending..." if request remains delayed after 400ms
    freakTimerRef.current = setTimeout(() => {
      if (isFreakInFlight.current) {
        setShowFreakSending(true);
      }
    }, 400);

    try {
      // 1. Start real backend request IMMEDIATELY without delay
      await convocationService.childSendMessage("Someone's,here");
      // 2. Only show approved DONE toast on actual backend success
      setShowDoneModal(true);
    } catch (err: any) {
      console.error('Failed to send FREAK note:', err);
      // Requirement 16: Do NOT show fake DONE on failure. Keep screen usable with error message.
      const msg = err.response?.data?.message || 'Failed to send message. Please check connection.';
      setErrorMessage(msg);
    } finally {
      if (freakTimerRef.current) clearTimeout(freakTimerRef.current);
      isFreakInFlight.current = false;
      setShowFreakSending(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', maxWidth: '640px', margin: '0 auto', width: '100%' }}>
      {errorMessage && (
        <div
          style={{
            padding: '0.75rem 1rem',
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: '10px',
            color: '#EF4444',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <AlertTriangle size={16} />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* 1. BATTERY (Click -> Battery details) */}
      <div
        id="child-item-battery"
        className="glass-panel"
        onClick={() => navigate('/child/battery')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(16, 185, 129, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#10B981',
              flexShrink: 0,
            }}
          >
            <BatteryCharging size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>1. Battery</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {battery ? `${battery.batteryPct}% • ${battery.isCharging ? 'Charging' : 'Discharging'}` : 'Unavailable'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* 2. SCREEN TIME (Click -> Screen Time details) */}
      <div
        id="child-item-screentime"
        className="glass-panel"
        onClick={() => navigate('/child/screen-time')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(99, 102, 241, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#6366F1',
              flexShrink: 0,
            }}
          >
            <Clock size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>2. Screen Time</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {usage && usage.totalScreenTimeMinutes !== undefined
                ? `${Math.floor(usage.totalScreenTimeMinutes / 60)}h ${usage.totalScreenTimeMinutes % 60}m today`
                : 'Waiting for device data'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* 3 & 4. CRACK + FREAK — TWO-COLUMN LAYOUT (Requirement 2 & 3) */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          width: '100%',
        }}
      >
        {/* 3. CRACK (Immediate send of "Mom,here") */}
        <button
          type="button"
          id="child-btn-crack"
          onClick={handleSendCrack}
          disabled={showCrackSending || showFreakSending}
          style={{
            width: '100%',
            height: '62px',
            borderRadius: '14px',
            background: '#EF4444',
            color: '#FFFFFF',
            border: 'none',
            fontSize: '1.25rem',
            fontWeight: 800,
            letterSpacing: '0.08em',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 24px rgba(239, 68, 68, 0.35)',
            transition: 'transform 0.15s ease, filter 0.15s ease',
          }}
        >
          {showCrackSending ? 'Sending...' : 'CRACK'}
        </button>

        {/* 4. FREAK (Immediate send of "Someone's,here") */}
        <button
          type="button"
          id="child-btn-freak"
          onClick={handleSendFreak}
          disabled={showCrackSending || showFreakSending}
          style={{
            width: '100%',
            height: '62px',
            borderRadius: '14px',
            background: '#8B5CF6',
            color: '#FFFFFF',
            border: 'none',
            fontSize: '1.25rem',
            fontWeight: 800,
            letterSpacing: '0.08em',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 24px rgba(139, 92, 246, 0.35)',
            transition: 'transform 0.15s ease, filter 0.15s ease',
          }}
        >
          {showFreakSending ? 'Sending...' : 'FREAK'}
        </button>
      </div>

      {/* 5. NETWORK (Click -> Network details) */}
      <div
        id="child-item-network"
        className="glass-panel"
        onClick={() => navigate('/child/network')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(6, 182, 212, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#06B6D4',
              flexShrink: 0,
            }}
          >
            <Wifi size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>5. Network</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {network ? `${network.networkType} • ${network.ssid || (network.isInternetAvailable ? 'Connected' : 'Offline')}` : 'Unavailable'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* 6. NETWORK QUALITY (Click -> Network Quality details) */}
      <div
        id="child-item-network-quality"
        className="glass-panel"
        onClick={() => navigate('/child/network-quality')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(56, 189, 248, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#38BDF8',
              flexShrink: 0,
            }}
          >
            <Activity size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>6. Network Quality</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {network?.networkQuality ? `${network.networkQuality} • Signal ${network.signalLevel !== undefined ? network.signalLevel + '/4' : 'Normal'}` : 'Unavailable'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* 7. LOCATION (Click -> Location details) */}
      <div
        id="child-item-location"
        className="glass-panel"
        onClick={() => navigate('/child/location')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(245, 158, 11, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#F59E0B',
              flexShrink: 0,
            }}
          >
            <MapPin size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>7. Location</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {location && location.latitude !== undefined ? `${location.latitude.toFixed(4)}, ${location.longitude.toFixed(4)}` : 'Waiting for GPS fix'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* 8. DEVICE HEALTH (Click -> Device Health details) */}
      <div
        id="child-item-device-health"
        className="glass-panel"
        onClick={() => navigate('/child/device-health')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(20, 184, 166, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#14B8A6',
              flexShrink: 0,
            }}
          >
            <Cpu size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>8. Device Health</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {health && health.freeStorageBytes
                ? `Storage: ${(health.freeStorageBytes / 1e9).toFixed(1)} GB free`
                : 'Diagnostics unavailable'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* 9. ALERTS (Click -> Alerts details) */}
      <div
        id="child-item-alerts"
        className="glass-panel"
        onClick={() => navigate('/child/alerts')}
        style={{
          padding: '1rem 1.25rem',
          borderRadius: '14px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(30, 41, 59, 0.7)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          cursor: 'pointer',
          transition: 'transform 0.15s ease, border-color 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'rgba(168, 85, 247, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#A855F7',
              flexShrink: 0,
            }}
          >
            <Bell size={22} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.98rem', color: '#fff' }}>9. Alerts</div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              {alerts.length > 0 ? `${alerts.length} active alert${alerts.length > 1 ? 's' : ''}` : 'No active alerts'}
            </div>
          </div>
        </div>
        <ChevronRight size={18} color="#64748B" />
      </div>

      {/* Non-blocking Success Toast: Compact [Check icon] Done! at Top-Center */}
      {showDoneModal && (
        <div
          id="modal-message-sent"
          style={{
            position: 'fixed',
            top: '1.5rem',
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 1000,
            pointerEvents: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            id="toast-done-container"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.45rem',
              padding: '0.5rem 1rem',
              borderRadius: '12px',
              background: 'rgba(15, 23, 42, 0.95)',
              backdropFilter: 'blur(12px)',
              border: '1px solid #10B981',
              color: '#10B981',
              boxShadow: '0 8px 24px rgba(16, 185, 129, 0.25), 0 4px 12px rgba(0, 0, 0, 0.4)',
              fontSize: '0.95rem',
              fontWeight: 700,
              letterSpacing: '0.02em',
              whiteSpace: 'nowrap',
            }}
          >
            <Check size={18} strokeWidth={2.5} color="#10B981" />
            <span>Done!</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChildDashboardPage;
