import React from 'react';

export type RoleType = 'PARENT' | 'CHILD';

interface RoleRouteGuardProps {
  allowedRoles: RoleType[];
  currentRole: RoleType | null;
  children: React.ReactNode;
  fallbackPath?: string;
}

/**
 * Route guard component strictly enforcing role separation.
 * Guarantees Child sessions cannot render Parent modules or controls.
 */
export const RoleRouteGuard: React.FC<RoleRouteGuardProps> = ({
  allowedRoles,
  currentRole,
  children,
  fallbackPath = '/role-selection'
}) => {
  if (!currentRole) {
    if (typeof window !== 'undefined') {
      window.location.href = fallbackPath;
    }
    return null;
  }

  const isPermitted = allowedRoles.includes(currentRole);

  if (!isPermitted) {
    // Prohibited role attempting access: redirect to their own role dashboard
    const targetDashboard = currentRole === 'PARENT' ? '/parent/dashboard' : '/child/dashboard';
    if (typeof window !== 'undefined') {
      window.location.href = targetDashboard;
    }
    return null;
  }

  return <>{children}</>;
};

export default RoleRouteGuard;
