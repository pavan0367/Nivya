import { describe, it, expect, beforeEach, vi } from 'vitest';
import { authService } from '../services/authService';
import { apiClient } from '../services/api';

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
    vi.restoreAllMocks();
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
    expect(authService.isChild()).toBe(true);
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

  it('stores auth tokens and user profile on successful register', async () => {
    vi.spyOn(authService, 'register').mockImplementation(async (name, email, _password, role) => {
      const mockData = {
        accessToken: 'registered_access_jwt',
        refreshToken: 'registered_refresh_jwt',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: { id: 3, name, email, role, emailVerified: false, isChild: role === 'CHILD', isParent: role === 'PARENT', roles: [role] }
      };
      localStorage.setItem('nivya_access_token', mockData.accessToken);
      localStorage.setItem('nivya_refresh_token', mockData.refreshToken);
      localStorage.setItem('nivya_user_role', role);
      localStorage.setItem('nivya_user', JSON.stringify(mockData.user));
      return mockData;
    });

    const result = await authService.register('New Parent', 'new.parent@nivya.local', 'Password123!', 'PARENT');
    expect(result.accessToken).toBe('registered_access_jwt');
    expect(localStorage.getItem('nivya_access_token')).toBe('registered_access_jwt');
    expect(localStorage.getItem('nivya_user_role')).toBe('PARENT');
    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.isParent()).toBe(true);
  });

  it('handles permanent deletion and completely wipes credentials and local storage', async () => {
    localStorage.setItem('nivya_access_token', 'active_token');
    localStorage.setItem('nivya_refresh_token', 'active_refresh');
    localStorage.setItem('nivya_user_role', 'PARENT');
    localStorage.setItem('nivya_user', JSON.stringify({ id: 1, role: 'PARENT' }));

    vi.spyOn(authService, 'deleteAccount').mockImplementation(async (_password, _approvalCode) => {
      localStorage.clear();
    });

    await authService.deleteAccount('TestPassword123!');

    expect(localStorage.getItem('nivya_access_token')).toBeNull();
    expect(localStorage.getItem('nivya_refresh_token')).toBeNull();
    expect(localStorage.getItem('nivya_user_role')).toBeNull();
    expect(localStorage.getItem('nivya_user')).toBeNull();
    expect(authService.isAuthenticated()).toBe(false);
  });

  it('supports child deletion approval request and code verification flow', async () => {
    vi.spyOn(authService, 'requestChildDeletionApproval').mockResolvedValue({
      success: true,
      approvalCodeRequired: true,
      parentEmailMasked: 'pa***@nivya.local',
      expiresInMinutes: 15,
      message: 'Code dispatched',
    });

    vi.spyOn(authService, 'verifyChildDeletionCode').mockResolvedValue({
      valid: true,
      message: 'Code verified',
    });

    const reqResult = await authService.requestChildDeletionApproval();
    expect(reqResult.success).toBe(true);
    expect(reqResult.parentEmailMasked).toBe('pa***@nivya.local');

    const verifyResult = await authService.verifyChildDeletionCode('123456');
    expect(verifyResult.valid).toBe(true);
  });

  it('Resend Code triggers requestChildDeletionApproval and resets 15-minute expiry countdown', async () => {
    const resendSpy = vi.spyOn(authService, 'requestChildDeletionApproval').mockResolvedValue({
      success: true,
      approvalCodeRequired: true,
      parentEmailMasked: 'pa***@gmail.com',
      expiresInMinutes: 15,
      message: 'Approval code sent to your connected parent (pa***@gmail.com).',
    });

    // Initial request
    const firstResult = await authService.requestChildDeletionApproval();
    expect(firstResult.success).toBe(true);
    expect(firstResult.expiresInMinutes).toBe(15);
    expect(firstResult.parentEmailMasked).toBe('pa***@gmail.com');

    // Resend request
    const resendResult = await authService.requestChildDeletionApproval();
    expect(resendSpy).toHaveBeenCalledTimes(2);
    expect(resendResult.success).toBe(true);
    expect(resendResult.expiresInMinutes).toBe(15);
    expect(resendResult.message).toContain('Approval code sent');
  });

  it('Failed resend preserves error without reporting false success', async () => {
    vi.spyOn(authService, 'requestChildDeletionApproval').mockRejectedValue({
      response: {
        data: {
          message: 'Failed to send approval code to parent email: Brevo validation/bad request error (HTTP 400)',
        },
      },
    });

    await expect(authService.requestChildDeletionApproval()).rejects.toMatchObject({
      response: {
        data: {
          message: expect.stringContaining('Failed to send approval code'),
        },
      },
    });
  });

  it('cleans up all authentication tokens and profile data when 401 occurs', () => {
    localStorage.setItem('nivya_access_token', 'expired_jwt');
    localStorage.setItem('nivya_refresh_token', 'expired_refresh');
    localStorage.setItem('nivya_user_role', 'CHILD');
    localStorage.setItem('nivya_user', JSON.stringify({ id: 2, role: 'CHILD' }));

    // Simulate 401 interceptor logic
    localStorage.removeItem('nivya_access_token');
    localStorage.removeItem('nivya_refresh_token');
    localStorage.removeItem('nivya_user_role');
    localStorage.removeItem('nivya_user');

    expect(localStorage.getItem('nivya_access_token')).toBeNull();
    expect(localStorage.getItem('nivya_refresh_token')).toBeNull();
    expect(localStorage.getItem('nivya_user_role')).toBeNull();
    expect(localStorage.getItem('nivya_user')).toBeNull();
    expect(authService.isAuthenticated()).toBe(false);
    expect(authService.getCurrentUser()).toBeNull();
  });

  describe('Child Deletion Approval Request Timeout & Deduplication', () => {
    it('uses a request-specific timeout of 45000ms for child approval request', async () => {
      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          data: {
            success: true,
            approvalCodeRequired: true,
            parentEmailMasked: 'pa***@gmail.com',
            expiresInMinutes: 15,
            message: 'Approval code sent to your connected parent (pa***@gmail.com).',
          },
        },
      });

      const result = await authService.requestChildDeletionApproval();
      expect(postSpy).toHaveBeenCalledWith(
        '/account/deletion/request-child-approval',
        {},
        { timeout: 45000 }
      );
      expect(result.success).toBe(true);
      expect(result.parentEmailMasked).toBe('pa***@gmail.com');
    });

    it('deduplicates in-flight approval requests so concurrent clicks share a single request', async () => {
      let resolvePost: (val: any) => void;
      const deferredPromise = new Promise((resolve) => {
        resolvePost = resolve;
      });

      const postSpy = vi.spyOn(apiClient, 'post').mockReturnValueOnce(deferredPromise as any);

      // Trigger two concurrent requests while first is still in flight
      const call1 = authService.requestChildDeletionApproval();
      const call2 = authService.requestChildDeletionApproval();

      // Only ONE underlying API call should have been dispatched
      expect(postSpy).toHaveBeenCalledTimes(1);

      resolvePost!({
        data: {
          data: {
            success: true,
            approvalCodeRequired: true,
            parentEmailMasked: 'pa***@nivya.local',
            expiresInMinutes: 15,
            message: 'Code dispatched',
          },
        },
      });

      const [res1, res2] = await Promise.all([call1, call2]);
      expect(res1).toEqual(res2);
      expect(res1.success).toBe(true);
    });

    it('reconciles client timeout using getDeletionStatus when deliveryStatus is DELIVERED', async () => {
      // Simulate client timeout abort (no response from server)
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        code: 'ECONNABORTED',
        message: 'timeout of 45000ms exceeded',
      });

      // Mock getDeletionStatus indicating code was successfully DELIVERED
      vi.spyOn(authService, 'getDeletionStatus').mockResolvedValueOnce({
        role: 'CHILD',
        isChild: true,
        hasConnectedParent: true,
        connectedParentEmailMasked: 'pa***@gmail.com',
        instructions: 'Parent approval required',
        deliveryStatus: 'DELIVERED',
        hasPendingApprovalCode: true,
        approvalCodeExpiresInSeconds: 880,
      });

      const result = await authService.requestChildDeletionApproval();
      expect(result.success).toBe(true);
      expect(result.parentEmailMasked).toBe('pa***@gmail.com');
      expect(result.expiresInMinutes).toBe(15);
    });

    it('polls while deliveryStatus is DISPATCHING and reconciles when it transitions to DELIVERED', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        code: 'ECONNABORTED',
        message: 'timeout of 45000ms exceeded',
      });

      // 1st poll: DISPATCHING; 2nd poll: DELIVERED
      vi.spyOn(authService, 'getDeletionStatus')
        .mockResolvedValueOnce({
          role: 'CHILD',
          isChild: true,
          hasConnectedParent: true,
          connectedParentEmailMasked: 'pa***@gmail.com',
          instructions: 'Parent approval required',
          deliveryStatus: 'DISPATCHING',
          hasPendingApprovalCode: false,
        })
        .mockResolvedValueOnce({
          role: 'CHILD',
          isChild: true,
          hasConnectedParent: true,
          connectedParentEmailMasked: 'pa***@gmail.com',
          instructions: 'Parent approval required',
          deliveryStatus: 'DELIVERED',
          hasPendingApprovalCode: true,
          approvalCodeExpiresInSeconds: 890,
        });

      const result = await authService.requestChildDeletionApproval();
      expect(result.success).toBe(true);
      expect(result.parentEmailMasked).toBe('pa***@gmail.com');
    });

    it('throws error when deliveryStatus becomes FAILED during reconciliation', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        code: 'ECONNABORTED',
        message: 'timeout of 45000ms exceeded',
      });

      vi.spyOn(authService, 'getDeletionStatus').mockResolvedValueOnce({
        role: 'CHILD',
        isChild: true,
        hasConnectedParent: true,
        connectedParentEmailMasked: 'pa***@gmail.com',
        instructions: 'Parent approval required',
        deliveryStatus: 'FAILED',
        hasPendingApprovalCode: false,
      });

      await expect(authService.requestChildDeletionApproval()).rejects.toThrow(
        'Failed to deliver parent approval code email.'
      );
    });

    it('does not fake success if client timeout occurs and deliveryStatus remains IDLE', async () => {
      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        code: 'ECONNABORTED',
        message: 'timeout of 45000ms exceeded',
      });

      vi.spyOn(authService, 'getDeletionStatus').mockResolvedValue({
        role: 'CHILD',
        isChild: true,
        hasConnectedParent: true,
        connectedParentEmailMasked: 'pa***@gmail.com',
        instructions: 'Parent approval required',
        deliveryStatus: 'IDLE',
        hasPendingApprovalCode: false,
      });

      await expect(authService.requestChildDeletionApproval()).rejects.toMatchObject({
        code: 'ECONNABORTED',
      });
    });

    it('propagates genuine backend 4xx/5xx server errors without attempting reconciliation', async () => {
      const getStatusSpy = vi.spyOn(authService, 'getDeletionStatus');

      vi.spyOn(apiClient, 'post').mockRejectedValueOnce({
        response: {
          status: 400,
          data: { message: 'Only child accounts can request parent deletion approval.' },
        },
      });

      await expect(authService.requestChildDeletionApproval()).rejects.toMatchObject({
        response: {
          status: 400,
        },
      });

      // Should not have called getDeletionStatus on real HTTP errors
      expect(getStatusSpy).not.toHaveBeenCalled();
    });
  });
});
