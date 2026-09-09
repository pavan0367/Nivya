import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { authService } from '../services/authService';
import { RoleType } from '../types/auth';

interface RoleRouteProps {
  allowedRoles: RoleType[];
  children?: React.ReactNode;
}

/**
 * Strict role-enforcing route guard.
 * Prevents Child accounts from ever accessing Parent dashboards or telemetry controls.
 */
export const RoleRoute: React.FC<RoleRouteProps> = ({ allowedRoles, children }) => {
  const user = authService.getCurrentUser();
  const role = authService.getUserRole();

  if (!user || !role) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(role)) {
    // Child user attempting to access Parent routes
    return <Navigate to="/access-denied" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
};

export default RoleRoute;
