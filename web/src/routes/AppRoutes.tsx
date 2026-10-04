import React, { Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '../layouts/AuthLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';
import { authService } from '../services/authService';

// Public Auth Pages - imported synchronously for instant first-paint on login/register
import { LoginPage } from '../pages/Login/LoginPage';
import { RegisterPage } from '../pages/Register/RegisterPage';
import { VerifyEmailPage } from '../pages/VerifyEmail/VerifyEmailPage';
import { RoleSelectPage } from '../pages/RoleSelection/RoleSelectPage';
import { AccessDeniedPage } from '../pages/AccessDeniedPage';

// Lazy-loaded Layouts
const DashboardLayout = React.lazy(() => import('../layouts/DashboardLayout').then(m => ({ default: m.DashboardLayout })));
const ChildLayout = React.lazy(() => import('../layouts/ChildLayout').then(m => ({ default: m.ChildLayout })));
const AdminLayout = React.lazy(() => import('../layouts/AdminLayout').then(m => ({ default: m.AdminLayout })));

// Lazy-loaded Pages (Code-split for maximum startup and navigation performance)
const PairingScreen = React.lazy(() => import('../pages/Pairing/PairingScreen').then(m => ({ default: m.PairingScreen })));
const DashboardPage = React.lazy(() => import('../pages/Dashboard/DashboardPage').then(m => ({ default: m.DashboardPage })));
const LiveActivityPage = React.lazy(() => import('../pages/LiveActivity/LiveActivityPage').then(m => ({ default: m.LiveActivityPage })));
const HistoryPage = React.lazy(() => import('../pages/History/HistoryPage').then(m => ({ default: m.HistoryPage })));
const LocationPage = React.lazy(() => import('../pages/Location/LocationPage').then(m => ({ default: m.LocationPage })));
const UsagePage = React.lazy(() => import('../pages/Usage/UsagePage').then(m => ({ default: m.UsagePage })));
const DeviceHealthPage = React.lazy(() => import('../pages/DeviceHealth/DeviceHealthPage').then(m => ({ default: m.DeviceHealthPage })));
const AlertsPage = React.lazy(() => import('../pages/Alerts/AlertsPage').then(m => ({ default: m.AlertsPage })));
const ConvocationPage = React.lazy(() => import('../pages/Convocation/ConvocationPage').then(m => ({ default: m.ConvocationPage })));
const SettingsPage = React.lazy(() => import('../pages/Settings/SettingsPage').then(m => ({ default: m.SettingsPage })));

// Child Pages
const ChildDashboardPage = React.lazy(() => import('../pages/ChildDashboard/ChildDashboardPage').then(m => ({ default: m.ChildDashboardPage })));
const ChildBatteryPage = React.lazy(() => import('../pages/ChildDashboard/ChildBatteryPage').then(m => ({ default: m.ChildBatteryPage })));
const ChildScreenTimePage = React.lazy(() => import('../pages/ChildDashboard/ChildScreenTimePage').then(m => ({ default: m.ChildScreenTimePage })));
const ChildNetworkPage = React.lazy(() => import('../pages/ChildDashboard/ChildNetworkPage').then(m => ({ default: m.ChildNetworkPage })));
const ChildNetworkQualityPage = React.lazy(() => import('../pages/ChildDashboard/ChildNetworkQualityPage').then(m => ({ default: m.ChildNetworkQualityPage })));
const ChildLocationPage = React.lazy(() => import('../pages/ChildDashboard/ChildLocationPage').then(m => ({ default: m.ChildLocationPage })));
const ChildDeviceHealthPage = React.lazy(() => import('../pages/ChildDashboard/ChildDeviceHealthPage').then(m => ({ default: m.ChildDeviceHealthPage })));
const ChildAlertsPage = React.lazy(() => import('../pages/ChildDashboard/ChildAlertsPage').then(m => ({ default: m.ChildAlertsPage })));
const ChildConvocationPage = React.lazy(() => import('../pages/ChildDashboard/ChildConvocationPage').then(m => ({ default: m.ChildConvocationPage })));

// Admin Pages
const AdminDashboardPage = React.lazy(() => import('../pages/Admin/AdminDashboardPage').then(m => ({ default: m.AdminDashboardPage })));

/**
 * Lightweight route transition fallback indicator.
 */
const RouteTransitionFallback: React.FC = () => (
  <div style={{
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '60vh',
    width: '100%'
  }}>
    <div style={{
      width: '28px',
      height: '28px',
      border: '2px solid rgba(99, 102, 241, 0.2)',
      borderTop: '2px solid var(--primary)',
      borderRadius: '50%',
      animation: 'spin 0.7s linear infinite'
    }} />
  </div>
);

/**
 * Role-aware landing selector.
 * Guarantees Child accounts land directly on /child,
 * Parent accounts land directly on /dashboard,
 * Admin accounts land directly on /admin,
 * and unauthenticated sessions land on /login.
 */
export const RoleAwareLanding: React.FC = () => {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  const role = authService.getUserRole();
  if (role === 'ADMIN') {
    return <Navigate to="/admin" replace />;
  }
  if (role === 'CHILD') {
    return <Navigate to="/child" replace />;
  }
  return <Navigate to="/dashboard" replace />;
};

export const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<RouteTransitionFallback />}>
      <Routes>
        {/* Public Auth Routes */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/role-selection" element={<RoleSelectPage />} />
        </Route>

        {/* Role access restriction notice */}
        <Route path="/access-denied" element={<AccessDeniedPage />} />

        {/* Protected Routes */}
        <Route element={<ProtectedRoute />}>
          {/* Pairing is accessible to both Parent and Child */}
          <Route path="/pairing" element={<PairingScreen />} />

          {/* Child Experience & Detail Routes */}
          <Route element={<RoleRoute allowedRoles={['CHILD']} />}>
            <Route element={<ChildLayout />}>
              <Route path="/child" element={<ChildDashboardPage />} />
              <Route path="/child/battery" element={<ChildBatteryPage />} />
              <Route path="/child/screen-time" element={<ChildScreenTimePage />} />
              <Route path="/child/network" element={<ChildNetworkPage />} />
              <Route path="/child/network-quality" element={<ChildNetworkQualityPage />} />
              <Route path="/child/location" element={<ChildLocationPage />} />
              <Route path="/child/device-health" element={<ChildDeviceHealthPage />} />
              <Route path="/child/alerts" element={<ChildAlertsPage />} />
              <Route path="/child/convocation" element={<ChildConvocationPage />} />
              <Route path="/child/settings" element={<SettingsPage />} />
            </Route>
          </Route>

          {/* Parent-Only Routes */}
          <Route element={<RoleRoute allowedRoles={['PARENT']} />}>
            <Route element={<DashboardLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/live-activity" element={<LiveActivityPage />} />
              <Route path="/history" element={<HistoryPage />} />
              <Route path="/location" element={<LocationPage />} />
              <Route path="/usage" element={<UsagePage />} />
              <Route path="/device-health" element={<DeviceHealthPage />} />
              <Route path="/alerts" element={<AlertsPage />} />
              <Route path="/convocation" element={<ConvocationPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>
          </Route>

          {/* Admin-Only Routes */}
          <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin" element={<AdminDashboardPage />} />
            </Route>
          </Route>
        </Route>

        {/* Role-aware root and fallback redirect */}
        <Route path="/" element={<RoleAwareLanding />} />
        <Route path="*" element={<RoleAwareLanding />} />
      </Routes>
    </Suspense>
  );
};

export default AppRoutes;
