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

describe('authService & Role Isolation', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('correctly identifies unauthenticated sessions', () => {
    expect(authService.isAuthenticated()).toBe(false);
    expect(authService.getCurrentUser()).toBeNull();
    expect(authService.getUserRole()).toBeNull();
    expect(authService.isParent()).toBe(false);
  });

  it('correctly identifies Parent role and authorizes parent access', () => {
    localStorage.setItem('nivya_access_token', 'mock_jwt_parent_token');
    localStorage.setItem('nivya_user_role', 'PARENT');
    localStorage.setItem(
      'nivya_user',
      JSON.stringify({ id: 1, name: 'Parent User', email: 'parent@nivya.local', role: 'PARENT' })
    );

    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.getUserRole()).toBe('PARENT');
    expect(authService.isParent()).toBe(true);

    const user = authService.getCurrentUser();
    expect(user).not.toBeNull();
    expect(user?.role).toBe('PARENT');
  });

  it('correctly identifies Child role and blocks parent authorization', () => {
    localStorage.setItem('nivya_access_token', 'mock_jwt_child_token');
    localStorage.setItem('nivya_user_role', 'CHILD');
    localStorage.setItem(
      'nivya_user',
      JSON.stringify({ id: 2, name: 'Child User', email: 'child@nivya.local', role: 'CHILD' })
    );

    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.getUserRole()).toBe('CHILD');
    expect(authService.isParent()).toBe(false);
  });

  it('clears all session and role state on logout', () => {
    localStorage.setItem('nivya_access_token', 'mock_jwt');
    localStorage.setItem('nivya_refresh_token', 'mock_refresh');
    localStorage.setItem('nivya_user_role', 'PARENT');
    localStorage.setItem('nivya_user', JSON.stringify({ id: 1, role: 'PARENT' }));

    // Mock window.location.href
    delete (globalThis as any).window;
    (globalThis as any).window = { location: { href: '' } };

    authService.logout();

    expect(localStorage.getItem('nivya_access_token')).toBeNull();
    expect(localStorage.getItem('nivya_refresh_token')).toBeNull();
    expect(localStorage.getItem('nivya_user_role')).toBeNull();
    expect(localStorage.getItem('nivya_user')).toBeNull();
    expect((globalThis as any).window.location.href).toBe('/login');
  });
});
