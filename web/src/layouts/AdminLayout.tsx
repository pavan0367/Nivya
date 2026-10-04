import React, { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  ShieldAlert,
  Users,
  LayoutDashboard,
  ShieldCheck,
  FileText,
  LogOut,
  Sparkles
} from 'lucide-react';
import { authService } from '../services/authService';
import { User } from '../types/auth';

export const AdminLayout: React.FC = () => {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  useEffect(() => {
    const user = authService.getCurrentUser();
    if (!user || user.role !== 'ADMIN') {
      navigate('/access-denied');
      return;
    }
    setCurrentUser(user);
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        {/* Brand Header */}
        <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <img src="/logo.png" alt="Nivya Logo" style={{ width: '36px', height: '36px', objectFit: 'contain' }} />
          <div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 700, letterSpacing: '-0.02em', color: '#fff' }}>Nivya</h2>
            <small style={{ color: 'var(--primary-light, #818cf8)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
              Admin Console
            </small>
          </div>
        </div>

        {/* Navigation Links */}
        <nav style={{ flex: 1, padding: '1.25rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem', overflowY: 'auto' }}>
          <NavLink
            to="/admin"
            id="nav-admin-dashboard"
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: '0.85rem',
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-md)',
              color: isActive ? '#fff' : 'var(--text-muted)',
              background: isActive ? 'rgba(99, 102, 241, 0.18)' : 'transparent',
              fontWeight: isActive ? 600 : 500,
              textDecoration: 'none',
              transition: 'all var(--transition-fast)',
              borderLeft: isActive ? '3px solid var(--primary)' : '3px solid transparent',
            })}
          >
            <ShieldCheck size={19} />
            <span className="sidebar-text" style={{ fontSize: '0.9rem' }}>Admin Dashboard</span>
          </NavLink>
        </nav>

        {/* User Info & Logout Footer */}
        <div style={{ padding: '1.25rem', borderTop: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
              <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {currentUser?.name || 'Administrator'}
              </span>
              <span className="badge badge-primary" style={{ fontSize: '0.65rem', padding: '0.1rem 0.4rem' }}>
                ADMIN
              </span>
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem', display: 'block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {currentUser?.email}
            </small>
          </div>
          <button
            type="button"
            id="btn-admin-logout"
            className="btn btn-secondary btn-sm"
            title="Log out"
            style={{ padding: '0.45rem', marginLeft: '0.5rem' }}
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <ShieldCheck size={20} color="var(--primary)" />
            <span style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--text-main)' }}>
              Nivya Administrative Management
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span className="badge badge-success" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10b981' }} />
              Secure Admin Session
            </span>
          </div>
        </header>

        {/* Body Pages */}
        <main className="page-body">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
