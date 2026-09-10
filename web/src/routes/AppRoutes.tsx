import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '../layouts/AuthLayout';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';

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

      {/* Protected Parent-Only Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<RoleRoute allowedRoles={['PARENT']} />}>
          <Route path="/pairing" element={<PairingScreen />} />
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

      {/* Root redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

export default AppRoutes;
