import React, { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Radio,
  History,
  MapPin,
  Clock,
  HeartPulse,
  Bell,
  MessageSquareQuote,
  Settings,
  LogOut,
  Smartphone,
  Wifi,
  WifiOff,
} from 'lucide-react';
import { authService } from '../services/authService';
import { alertService } from '../services/alertService';
import { websocketService } from '../services/websocketService';
import { Device, User } from '../types/auth';

interface NavItem {
  path: string;
  name: string;
  icon: React.ReactNode;
}

const parentNavItems: NavItem[] = [
  { path: '/dashboard', name: 'Dashboard', icon: <LayoutDashboard size={19} /> },
  { path: '/live-activity', name: 'Live Activity', icon: <Radio size={19} /> },
  { path: '/history', name: 'History', icon: <History size={19} /> },
  { path: '/location', name: 'Location', icon: <MapPin size={19} /> },
  { path: '/usage', name: 'App Usage', icon: <Clock size={19} /> },
  { path: '/device-health', name: 'Device Health', icon: <HeartPulse size={19} /> },
  { path: '/alerts', name: 'Alerts', icon: <Bell size={19} /> },
  { path: '/convocation', name: 'Convocation', icon: <MessageSquareQuote size={19} /> },
  { path: '/settings', name: 'Settings', icon: <Settings size={19} /> },
];

export const DashboardLayout: React.FC = () => {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDeviceId, setSelectedDeviceId] = useState<number | null>(null);
  const [unreadAlerts, setUnreadAlerts] = useState<number>(0);
  const [isWsConnected, setIsWsConnected] = useState<boolean>(false);

  useEffect(() => {
    const user = authService.getCurrentUser();
    if (!user || user.role !== 'PARENT') {
      navigate('/access-denied');
      return;
    }
    setCurrentUser(user);

    // Fetch family devices
    authService.getFamilyDevices().then((devs) => {
      setDevices(devs);
      if (devs.length > 0) {
        setSelectedDeviceId(devs[0].id);
        localStorage.setItem('nivya_active_device_id', devs[0].id.toString());
      }
    });

    // Fetch initial unread count
    alertService.getUnreadCount().then((res) => {
      setUnreadAlerts(res.unreadCount);
    });

    // Connect WebSocket
    websocketService.connect();
    const unsubConn = websocketService.onConnectionChange((connected) => {
      setIsWsConnected(connected);
    });

    return () => {
      unsubConn();
    };
  }, [navigate]);

  const handleDeviceChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const devId = Number(e.target.value);
    setSelectedDeviceId(devId);
    localStorage.setItem('nivya_active_device_id', devId.toString());
    window.dispatchEvent(new Event('nivya-device-changed'));
  };

  const handleLogout = () => {
    websocketService.disconnect();
    authService.logout();
  };

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        {/* Brand Header */}
        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: 'var(--primary-gradient)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <span style={{ fontWeight: 800, color: '#fff', fontSize: '1.2rem' }}>N</span>
          </div>
          <div>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, letterSpacing: '-0.02em', color: '#fff' }}>Nivya</h2>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Parent Console</small>
          </div>
        </div>

        {/* Navigation Links */}
        <nav style={{ flex: 1, padding: '1.25rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem', overflowY: 'auto' }}>
          {parentNavItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '0.85rem',
                padding: '0.7rem 1rem',
                borderRadius: 'var(--radius-md)',
                color: isActive ? '#fff' : 'var(--text-muted)',
                background: isActive ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
                fontWeight: isActive ? 600 : 500,
                textDecoration: 'none',
                transition: 'all var(--transition-fast)',
                borderLeft: isActive ? '3px solid var(--primary)' : '3px solid transparent',
              })}
            >
              <span style={{ display: 'flex', alignItems: 'center' }}>{item.icon}</span>
              <span className="sidebar-text" style={{ fontSize: '0.9rem' }}>{item.name}</span>
              {item.path === '/alerts' && unreadAlerts > 0 && (
                <span className="badge badge-danger" style={{ marginLeft: 'auto', padding: '0.1rem 0.45rem', fontSize: '0.7rem' }}>
                  {unreadAlerts}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* User Info & Logout Footer */}
        <div style={{ padding: '1.25rem', borderTop: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)' }}>
              {currentUser?.name || 'Parent User'}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>{currentUser?.email}</small>
          </div>
          <button
            type="button"
            id="btn-logout"
            className="btn btn-secondary btn-sm"
            title="Log out"
            style={{ padding: '0.45rem' }}
            onClick={handleLogout}
          >
            <LogOut size={16} />
          </button>
        </div>
      </aside>

      {/* Main Content Viewport */}
      <div className="main-content">
        {/* Topbar */}
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            {/* Child Device Switcher */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Smartphone size={18} color="var(--primary)" />
              <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Target Device:</span>
              <select
                id="select-child-device"
                className="form-select"
                style={{ padding: '0.35rem 0.85rem', fontSize: '0.85rem', width: 'auto', minWidth: '180px' }}
                value={selectedDeviceId || ''}
                onChange={handleDeviceChange}
              >
                {devices.length === 0 && <option value="">No paired device</option>}
                {devices.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.deviceName} ({d.platform})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            {/* Live STOMP Indicator */}
            <span
              className={`badge ${isWsConnected ? 'badge-success' : 'badge-neutral'}`}
              title={isWsConnected ? 'Real-time WebSocket telemetry connected' : 'Connecting to WebSocket...'}
            >
              {isWsConnected ? <Wifi size={13} /> : <WifiOff size={13} />}
              {isWsConnected ? 'Live' : 'Connecting'}
            </span>

            {/* Quick Unread Alert Icon */}
            <NavLink
              to="/alerts"
              style={{
                position: 'relative',
                padding: '0.5rem',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(255, 255, 255, 0.05)',
                color: 'var(--text-muted)',
                display: 'flex',
                alignItems: 'center',
                textDecoration: 'none',
              }}
            >
              <Bell size={18} />
              {unreadAlerts > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: '-3px',
                    right: '-3px',
                    width: '10px',
                    height: '10px',
                    borderRadius: '50%',
                    background: 'var(--danger)',
                    boxShadow: '0 0 8px var(--danger)',
                  }}
                />
              )}
            </NavLink>
          </div>
        </header>

        {/* Body Pages */}
        <main className="page-body">
          <Outlet context={{ activeDeviceId: selectedDeviceId }} />
        </main>
      </div>
    </div>
  );
};
