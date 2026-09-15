import React, { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  BatteryCharging,
  Clock,
  Wifi,
  Activity,
  MapPin,
  Cpu,
  Bell,
  MessageSquareQuote,
  Settings,
  LogOut,
  Smartphone,
  ShieldCheck,
  ArrowLeft,
  ExternalLink,
} from 'lucide-react';
import { authService } from '../services/authService';
import { websocketService } from '../services/websocketService';
import { telemetryService } from '../services/telemetryService';
import { Device, User } from '../types/auth';

interface ChildNavItem {
  path: string;
  name: string;
  icon: React.ReactNode;
  exact?: boolean;
}

const childNavItems: ChildNavItem[] = [
  { path: '/child', name: 'Child Home', icon: <LayoutDashboard size={19} />, exact: true },
  { path: '/child/battery', name: 'Battery', icon: <BatteryCharging size={19} /> },
  { path: '/child/screen-time', name: 'Screen Time', icon: <Clock size={19} /> },
  { path: '/child/network', name: 'Network', icon: <Wifi size={19} /> },
  { path: '/child/network-quality', name: 'Network Quality', icon: <Activity size={19} /> },
  { path: '/child/location', name: 'Location', icon: <MapPin size={19} /> },
  { path: '/child/device-health', name: 'Device Health', icon: <Cpu size={19} /> },
  { path: '/child/alerts', name: 'Alerts', icon: <Bell size={19} /> },
  { path: '/child/convocation', name: 'Convocation', icon: <MessageSquareQuote size={19} /> },
  { path: '/child/settings', name: 'Settings', icon: <Settings size={19} /> },
];

export interface ChildOutletContext {
  activeDeviceId: number | null;
  childDevice: Device | null;
  isPaired: boolean;
}

export const ChildLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [currentUser, setCurrentUser] = useState<User | null>(() => authService.getCurrentUser());
  const [childDevice, setChildDevice] = useState<Device | null>(() => {
    const user = authService.getCurrentUser();
    if (!user || user.role !== 'CHILD') return null;
    const stored = localStorage.getItem('nivya_child_device');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        if (parsed && parsed.userId === user.id && parsed.userRole !== 'PARENT') {
          return parsed;
        }
      } catch {}
    }
    return null;
  });
  const [activeDeviceId, setActiveDeviceId] = useState<number | null>(() => {
    const user = authService.getCurrentUser();
    if (!user || user.role !== 'CHILD') return null;
    const stored = localStorage.getItem('nivya_child_device');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        if (parsed && parsed.userId === user.id && parsed.userRole !== 'PARENT' && parsed.id != null) {
          return parsed.id;
        }
      } catch {}
    }
    return null;
  });
  const [isPaired, setIsPaired] = useState<boolean>(authService.isPaired());

  useEffect(() => {
    const user = authService.getCurrentUser();
    if (!user || user.role !== 'CHILD') {
      navigate('/access-denied', { replace: true });
      return;
    }
    setCurrentUser(user);

    // Fetch live family devices and authoritatively identify the child's own enrolled device
    authService.getFamilyDevices().then((devs) => {
      // 1. Authenticated Child user's enrolled device: matching device.userId === authenticatedUser.id
      let resolved = devs.find((d) => d.userId != null && d.userId === user.id);

      // 2. Existing authoritative pairing-status device whose userRole is CHILD or isChildDevice is true (excluding PARENT)
      if (!resolved) {
        resolved = devs.find((d) => (d.isChildDevice || d.userRole === 'CHILD') && d.userRole !== 'PARENT');
      }

      // 3. Persisted active device ONLY if it still belongs to the authenticated Child user
      const storedChildDeviceId = localStorage.getItem('nivya_child_active_device_id');
      if (storedChildDeviceId) {
        const storedId = Number(storedChildDeviceId);
        const matchedStored = devs.find((d) => d.id === storedId && (d.userId === user.id || (d.isChildDevice && d.userRole !== 'PARENT')));
        if (matchedStored) {
          resolved = matchedStored;
        }
      }

      // Invariant check: Verify the resolved device does NOT belong to a PARENT
      if (resolved && (resolved.userRole === 'PARENT' || (!resolved.isChildDevice && resolved.userId !== user.id))) {
        resolved = undefined;
      }

      if (resolved && resolved.id != null) {
        setChildDevice(resolved);
        setActiveDeviceId(resolved.id);
        localStorage.setItem('nivya_child_active_device_id', String(resolved.id));
        localStorage.setItem('nivya_child_device', JSON.stringify(resolved));
        localStorage.setItem('nivya_active_device_id', String(resolved.id));

        // Dispatch initial heartbeat to immediately mark device online/active
        telemetryService.sendHeartbeat({ deviceId: resolved.id, isOnline: true });
      } else {
        // If no valid Child device exists: show "Device unavailable" / appropriate empty state
        setChildDevice(null);
        setActiveDeviceId(null);
        localStorage.removeItem('nivya_child_active_device_id');
        localStorage.removeItem('nivya_child_device');
        // If nivya_active_device_id pointed to a parent device, clean it up
        const curActive = localStorage.getItem('nivya_active_device_id');
        if (curActive) {
          const isOwn = devs.some((d) => d.id === Number(curActive) && (d.userId === user.id || d.isChildDevice));
          if (!isOwn) {
            localStorage.removeItem('nivya_active_device_id');
          }
        }
      }
    }).catch((err) => {
      console.warn('Could not load child devices:', err);
      setChildDevice(null);
      setActiveDeviceId(null);
    });

    authService.getPairingStatus().then((status) => {
      setIsPaired(status.paired);
    }).catch(() => {
      setIsPaired(authService.isPaired());
    });

    // Establish WebSocket connection for real-time telemetry & Convocation updates
    websocketService.connect();

    // Periodic heartbeat to keep presence fresh during active web session
    const heartbeatInterval = setInterval(() => {
      telemetryService.sendHeartbeat({ isOnline: true });
    }, 60000);

    return () => {
      clearInterval(heartbeatInterval);
    };
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
  };

  const isDetailView = location.pathname !== '/child';

  return (
    <div className="app-container">
      {/* Child Sidebar Navigation */}
      <aside className="sidebar" id="child-sidebar">
        {/* Brand Header */}
        <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 16px rgba(245, 158, 11, 0.35)',
              flexShrink: 0,
            }}
          >
            <ShieldCheck size={20} color="#fff" />
          </div>
          <div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 700, letterSpacing: '-0.02em', color: '#fff' }}>Nivya</h2>
            <small style={{ color: '#F59E0B', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
              Child Console
            </small>
          </div>
        </div>

        {/* Navigation Links — Only Child-appropriate routes */}
        <nav style={{ flex: 1, padding: '1.25rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem', overflowY: 'auto' }}>
          {childNavItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.exact}
              id={`child-nav-${item.name.toLowerCase().replace(/\s+/g, '-')}`}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '0.85rem',
                padding: '0.7rem 1rem',
                borderRadius: 'var(--radius-md)',
                color: isActive ? '#fff' : 'var(--text-muted)',
                background: isActive ? 'rgba(245, 158, 11, 0.15)' : 'transparent',
                fontWeight: isActive ? 600 : 500,
                textDecoration: 'none',
                transition: 'all var(--transition-fast)',
                borderLeft: isActive ? '3px solid #F59E0B' : '3px solid transparent',
              })}
            >
              <span style={{ display: 'flex', alignItems: 'center' }}>{item.icon}</span>
              <span className="sidebar-text" style={{ fontSize: '0.9rem' }}>{item.name}</span>
            </NavLink>
          ))}
        </nav>

        {/* Child User Info & Logout Footer */}
        <div style={{ padding: '1.25rem', borderTop: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ overflow: 'hidden', marginRight: '0.5rem' }}>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {currentUser?.name || 'Child User'}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'block' }}>
              {currentUser?.email}
            </small>
          </div>
          <button
            type="button"
            id="btn-child-logout"
            className="btn btn-secondary btn-sm"
            title="Log out"
            style={{ padding: '0.45rem', flexShrink: 0 }}
            onClick={handleLogout}
          >
            <LogOut size={16} />
          </button>
        </div>
      </aside>

      {/* Main Content Viewport */}
      <div className="main-content" id="child-main-content">
        {/* Topbar */}
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            {isDetailView && (
              <button
                type="button"
                id="btn-back-to-child-home"
                onClick={() => navigate('/child')}
                className="btn btn-secondary btn-sm"
                style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', padding: '0.35rem 0.75rem' }}
              >
                <ArrowLeft size={15} />
                <span>Back to Home</span>
              </button>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Smartphone size={18} color="#F59E0B" />
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Device:</span>
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#fff' }}>
                {childDevice ? `${childDevice.deviceName} (${childDevice.platform})` : 'Device unavailable'}
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {isPaired ? (
              <span
                className="badge badge-success"
                style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}
                title="Family Safety Channel Active"
              >
                <span className="pulse-dot online" style={{ width: '6px', height: '6px' }} />
                Family Linked
              </span>
            ) : (
              <button
                type="button"
                onClick={() => navigate('/pairing')}
                className="badge badge-warning"
                style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.35rem', border: 'none' }}
              >
                Pair with Parent <ExternalLink size={11} />
              </button>
            )}
          </div>
        </header>

        {/* Body Pages */}
        <main className="page-body" style={{ maxWidth: '960px' }}>
          <Outlet context={{ activeDeviceId, childDevice, isPaired } satisfies ChildOutletContext} />
        </main>
      </div>
    </div>
  );
};

export default ChildLayout;
