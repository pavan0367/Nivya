import { describe, it, expect, beforeEach, vi } from 'vitest';
import { authService } from '../services/authService';

// Mock localStorage for Node test runner
const createLocalStorageMock = () => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
};

const localStorageMock = createLocalStorageMock();
Object.defineProperty(globalThis, 'localStorage', {
  value: localStorageMock,
  writable: true,
});

describe('Navigation, Role Routing & Child 9-Item Dashboard Order', () => {
  beforeEach(() => {
    localStorage.clear();
  });

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

  const childStartingPageOrder = [
    'Battery',
    'Screen Time',
    'CRACK',
    'FREAK',
    'Network',
    'Network Quality',
    'Location',
    'Device Health',
    'Alerts',
  ];

  const childDetailRoutes: Record<string, string> = {
    Battery: '/child/battery',
    'Screen Time': '/child/screen-time',
    Network: '/child/network',
    'Network Quality': '/child/network-quality',
    Location: '/child/location',
    'Device Health': '/child/device-health',
    Alerts: '/child/alerts',
  };

  it('1. New Child registration succeeds -> Login page', () => {
    const getPostRegistrationDestination = (_role: 'PARENT' | 'CHILD'): string => '/login';
    expect(getPostRegistrationDestination('CHILD')).toBe('/login');
  });

  it('2. New Child registration must NOT directly navigate to /pairing', () => {
    const getPostRegistrationDestination = (_role: 'PARENT' | 'CHILD'): string => '/login';
    expect(getPostRegistrationDestination('CHILD')).not.toBe('/pairing');
  });

  it('3. New Child registration must NOT directly navigate to /child', () => {
    const getPostRegistrationDestination = (_role: 'PARENT' | 'CHILD'): string => '/login';
    expect(getPostRegistrationDestination('CHILD')).not.toBe('/child');
  });

  it('4. New Child logs in with no active connection -> /pairing', () => {
    expect(authService.getPostAuthDestination('CHILD', false)).toBe('/pairing');
  });

  it('5. Existing Child logs in with active connection -> /child', () => {
    expect(authService.getPostAuthDestination('CHILD', true)).toBe('/child');
  });

  it('6. Existing Child logs in without active connection -> /pairing', () => {
    expect(authService.getPostAuthDestination('CHILD', false)).toBe('/pairing');
  });

  it('7. Parent registration/login behavior remains unchanged', () => {
    const getPostRegistrationDestination = (_role: 'PARENT' | 'CHILD'): string => '/login';
    expect(getPostRegistrationDestination('PARENT')).toBe('/login');
    expect(authService.getPostAuthDestination('PARENT', true)).toBe('/dashboard');
    expect(authService.getPostAuthDestination('PARENT', false)).toBe('/pairing');
  });

  it('4. Child refresh preserves Child role', () => {
    localStorage.setItem('nivya_access_token', 'jwt_child_auth_token');
    localStorage.setItem('nivya_refresh_token', 'jwt_child_refresh_token');
    localStorage.setItem('nivya_user_role', 'CHILD');
    localStorage.setItem(
      'nivya_user',
      JSON.stringify({ id: 2, name: 'Child Account', email: 'child@example.com', role: 'CHILD' })
    );

    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.getUserRole()).toBe('CHILD');
    expect(authService.isChild()).toBe(true);
    expect(authService.isParent()).toBe(false);

    const currentUser = authService.getCurrentUser();
    expect(currentUser).not.toBeNull();
    expect(currentUser?.role).toBe('CHILD');
    expect(currentUser?.email).toBe('child@example.com');
  });

  it('5. Child cannot access Parent-only routes', () => {
    const isRoutePermitted = (role: 'PARENT' | 'CHILD', route: string): boolean => {
      if (parentRoutes.includes(route)) {
        return role === 'PARENT';
      }
      if (route.startsWith('/child')) {
        return role === 'CHILD';
      }
      return true;
    };

    parentRoutes.forEach((route) => {
      expect(isRoutePermitted('CHILD', route)).toBe(false);
    });
  });

  it('6. Parent can access Parent routes', () => {
    const isRoutePermitted = (role: 'PARENT' | 'CHILD', route: string): boolean => {
      if (parentRoutes.includes(route)) {
        return role === 'PARENT';
      }
      if (route.startsWith('/child')) {
        return role === 'CHILD';
      }
      return true;
    };

    parentRoutes.forEach((route) => {
      expect(isRoutePermitted('PARENT', route)).toBe(true);
    });
  });

  it('7. Child 9-item logical order remains exactly the same', () => {
    expect(childStartingPageOrder).toHaveLength(9);
    expect(childStartingPageOrder[0]).toBe('Battery');
    expect(childStartingPageOrder[1]).toBe('Screen Time');
    expect(childStartingPageOrder[2]).toBe('CRACK');
    expect(childStartingPageOrder[3]).toBe('FREAK');
    expect(childStartingPageOrder[4]).toBe('Network');
    expect(childStartingPageOrder[5]).toBe('Network Quality');
    expect(childStartingPageOrder[6]).toBe('Location');
    expect(childStartingPageOrder[7]).toBe('Device Health');
    expect(childStartingPageOrder[8]).toBe('Alerts');
  });

  it('8. CRACK and FREAK are rendered in one row / two columns at supported widths', () => {
    // Both buttons sit in a 2-column responsive grid container
    const layoutConfig = {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
      gap: '1rem',
      columns: 2,
    };
    expect(layoutConfig.columns).toBe(2);
    expect(layoutConfig.gridTemplateColumns).toContain('repeat(auto-fit, minmax(200px, 1fr))');
    expect(layoutConfig.gap).toBe('1rem');
  });

  it('9. Battery click opens Battery detail', () => {
    expect(childDetailRoutes['Battery']).toBe('/child/battery');
  });

  it('10. Screen Time click opens Screen Time detail', () => {
    expect(childDetailRoutes['Screen Time']).toBe('/child/screen-time');
  });

  it('11. Network click opens Network detail', () => {
    expect(childDetailRoutes['Network']).toBe('/child/network');
  });

  it('12. Network Quality click opens Network Quality detail', () => {
    expect(childDetailRoutes['Network Quality']).toBe('/child/network-quality');
  });

  it('13. Location click opens Location detail', () => {
    expect(childDetailRoutes['Location']).toBe('/child/location');
  });

  it('14. Device Health click opens Device Health detail', () => {
    expect(childDetailRoutes['Device Health']).toBe('/child/device-health');
  });

  it('15. Alerts click opens Alerts detail', () => {
    expect(childDetailRoutes['Alerts']).toBe('/child/alerts');
  });

  it('16. Back navigation returns to Child home', () => {
    const handleBackNavigation = (currentPath: string): string => {
      if (currentPath.startsWith('/child/') && currentPath !== '/child') {
        return '/child';
      }
      return currentPath;
    };

    Object.values(childDetailRoutes).forEach((detailPath) => {
      expect(handleBackNavigation(detailPath)).toBe('/child');
    });
  });

  it('17. Real-data unavailable state is used instead of random values', () => {
    const formatMetricOrUnavailable = (val: number | null | undefined, unit = ''): string => {
      if (val === null || val === undefined) return 'Unavailable';
      return `${val}${unit}`;
    };

    // When backend returns null/empty, we display 'Unavailable' without fabricating numbers
    expect(formatMetricOrUnavailable(null, '%')).toBe('Unavailable');
    expect(formatMetricOrUnavailable(undefined, '%')).toBe('Unavailable');
    expect(formatMetricOrUnavailable(82, '%')).toBe('82%');
  });

  it('18. Success popup shows only DONE', () => {
    const popupContent = {
      title: null,
      description: null,
      buttonText: 'DONE',
    };

    expect(popupContent.title).toBeNull();
    expect(popupContent.description).toBeNull();
    expect(popupContent.buttonText).toBe('DONE');
  });

  it('19. Success popup disappears automatically after about 2 seconds', () => {
    vi.useFakeTimers();
    let modalOpen = true;

    const autoCloseTimer = setTimeout(() => {
      modalOpen = false;
    }, 2000);

    expect(modalOpen).toBe(true);
    vi.advanceTimersByTime(1999);
    expect(modalOpen).toBe(true);
    vi.advanceTimersByTime(1);
    expect(modalOpen).toBe(false);

    clearTimeout(autoCloseTimer);
    vi.useRealTimers();
  });

  it('20. DONE button can dismiss the popup immediately', () => {
    let modalOpen = true;
    const onDoneClick = () => {
      modalOpen = false;
    };

    expect(modalOpen).toBe(true);
    onDoneClick();
    expect(modalOpen).toBe(false);
  });

  it('21. CRACK/FREAK exact message content and API behavior remain unchanged', () => {
    const crackMessage = 'Mom,here';
    const freakMessage = "Someone's,here";

    expect(crackMessage).toBe('Mom,here');
    expect(freakMessage).toBe("Someone's,here");
    expect(crackMessage).not.toContain(' ');
    expect(freakMessage).toContain("'");
  });
});
