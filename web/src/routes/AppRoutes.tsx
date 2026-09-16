import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '../layouts/AuthLayout';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { ChildLayout } from '../layouts/ChildLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';
import { authService } from '../services/authService';

// Pages
import { LoginPage } from '../pages/Login/LoginPage';
import { RegisterPage } from '../pages/Register/RegisterPage';
import { VerifyEmailPage } from '../pages/VerifyEmail/VerifyEmailPage';
import { RoleSelectPage } from '../pages/RoleSelection/RoleSelectPage';
import { PairingScreen } from '../pages/Pairing/PairingScreen';
import { AccessDeniedPage } from '../pages/AccessDeniedPage';
import { DashboardPage } from '../pages/Dashboard/DashboardPage';
import { LiveActivityPage } from '../pages/LiveActivity/LiveActivityPage';
import { HistoryPage } from '../pages/History/HistoryPage';
import { LocationPage } from '../pages/Location/LocationPage';
import { UsagePage } from '../pages/Usage/UsagePage';
import { DeviceHealthPage } from '../pages/DeviceHealth/DeviceHealthPage';
import { AlertsPage } from '../pages/Alerts/AlertsPage';
import { ConvocationPage } from '../pages/Convocation/ConvocationPage';
import { SettingsPage } from '../pages/Settings/SettingsPage';

// Child Pages
import { ChildDashboardPage } from '../pages/ChildDashboard/ChildDashboardPage';
import { ChildBatteryPage } from '../pages/ChildDashboard/ChildBatteryPage';
import { ChildScreenTimePage } from '../pages/ChildDashboard/ChildScreenTimePage';
import { ChildNetworkPage } from '../pages/ChildDashboard/ChildNetworkPage';
import { ChildNetworkQualityPage } from '../pages/ChildDashboard/ChildNetworkQualityPage';
import { ChildLocationPage } from '../pages/ChildDashboard/ChildLocationPage';
import { ChildDeviceHealthPage } from '../pages/ChildDashboard/ChildDeviceHealthPage';
import { ChildAlertsPage } from '../pages/ChildDashboard/ChildAlertsPage';
import { ChildConvocationPage } from '../pages/ChildDashboard/ChildConvocationPage';

/**
 * Role-aware landing selector.
 * Guarantees Child accounts land directly on /child,
 * Parent accounts land directly on /dashboard,
 * and unauthenticated sessions land on /login.
 */
export const RoleAwareLanding: React.FC = () => {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  const role = authService.getUserRole();
  if (role === 'CHILD') {
    return <Navigate to={authService.getPostAuthDestination('CHILD', authService.isPaired())} replace />;
  }
  return <Navigate to="/dashboard" replace />;
};

export const AppRoutes: React.FC = () => {
  return (
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
      </Route>

      {/* Role-aware root and fallback redirect */}
      <Route path="/" element={<RoleAwareLanding />} />
      <Route path="*" element={<RoleAwareLanding />} />
    </Routes>
  );
};

export default AppRoutes;
