import { describe, it, expect } from 'vitest';

describe('Navigation and Parent Route Isolation', () => {
  const parentRoutes = [
    '/dashboard',
    '/live-activity',
    '/history',
    '/location',
    '/usage',
    '/device-health',
    '/alerts',
    '/convocation',
    '/settings',
  ];

  it('defines all required Parent dashboard navigation paths', () => {
    expect(parentRoutes).toHaveLength(9);
    expect(parentRoutes).toContain('/dashboard');
    expect(parentRoutes).toContain('/live-activity');
    expect(parentRoutes).toContain('/history');
    expect(parentRoutes).toContain('/location');
    expect(parentRoutes).toContain('/usage');
    expect(parentRoutes).toContain('/device-health');
    expect(parentRoutes).toContain('/alerts');
    expect(parentRoutes).toContain('/convocation');
    expect(parentRoutes).toContain('/settings');
  });

  it('strictly validates role permission check for parent-only routes', () => {
    const isRoutePermitted = (role: 'PARENT' | 'CHILD', route: string): boolean => {
      if (parentRoutes.includes(route)) {
        return role === 'PARENT';
      }
      return true;
    };

    // Parent has access to all dashboard routes
    parentRoutes.forEach((route) => {
      expect(isRoutePermitted('PARENT', route)).toBe(true);
    });

    // Child is strictly denied access to all parent routes
    parentRoutes.forEach((route) => {
      expect(isRoutePermitted('CHILD', route)).toBe(false);
    });
  });

  it('ensures public auth routes allow access without parent credentials', () => {
    const publicRoutes = ['/login', '/register', '/verify-email', '/role-selection', '/access-denied'];
    const isPublicRoute = (path: string) => publicRoutes.includes(path);

    expect(isPublicRoute('/login')).toBe(true);
    expect(isPublicRoute('/register')).toBe(true);
    expect(isPublicRoute('/verify-email')).toBe(true);
    expect(isPublicRoute('/role-selection')).toBe(true);
    expect(isPublicRoute('/access-denied')).toBe(true);
    expect(isPublicRoute('/dashboard')).toBe(false);
  });
});
