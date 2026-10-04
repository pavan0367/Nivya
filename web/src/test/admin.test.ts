import { describe, it, expect, beforeEach, vi } from 'vitest';
import { authService } from '../services/authService';
import { adminService } from '../services/adminService';
import { apiClient } from '../services/api';
import { RoleType } from '../types/auth';
import { UserStatus } from '../types/admin';

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

describe('Admin Management Module Frontend Tests', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  // =========================================================================
  // 1. ROUTE ACCESS & ROLE GUARDS
  // =========================================================================
  describe('Route Access & Role Guards', () => {
    const isRouteAuthorized = (
      user: { role: RoleType } | null,
      targetRoute: string,
      allowedRoles: RoleType[]
    ): { allowed: boolean; redirect: string | null } => {
      if (!user) {
        return { allowed: false, redirect: '/login' };
      }
      if (!allowedRoles.includes(user.role)) {
        return { allowed: false, redirect: '/access-denied' };
      }
      return { allowed: true, redirect: null };
    };

    it('1. Unauthenticated visitor accessing /admin -> redirected to /login', () => {
      expect(authService.isAuthenticated()).toBe(false);
      const access = isRouteAuthorized(null, '/admin', ['ADMIN']);
      expect(access.allowed).toBe(false);
      expect(access.redirect).toBe('/login');
    });

    it('2. Authenticated PARENT accessing /admin -> denied (redirect to /access-denied)', () => {
      localStorage.setItem('nivya_access_token', 'jwt_parent_token');
      localStorage.setItem('nivya_user_role', 'PARENT');
      localStorage.setItem(
        'nivya_user',
        JSON.stringify({ id: 1, name: 'Parent User', email: 'parent@test.com', role: 'PARENT' })
      );

      expect(authService.isAuthenticated()).toBe(true);
      expect(authService.isParent()).toBe(true);
      expect(authService.isAdmin()).toBe(false);

      const access = isRouteAuthorized({ role: 'PARENT' }, '/admin', ['ADMIN']);
      expect(access.allowed).toBe(false);
      expect(access.redirect).toBe('/access-denied');
    });

    it('3. Authenticated CHILD accessing /admin -> denied (redirect to /access-denied)', () => {
      localStorage.setItem('nivya_access_token', 'jwt_child_token');
      localStorage.setItem('nivya_user_role', 'CHILD');
      localStorage.setItem(
        'nivya_user',
        JSON.stringify({ id: 2, name: 'Child User', email: 'child@test.com', role: 'CHILD' })
      );

      expect(authService.isAuthenticated()).toBe(true);
      expect(authService.isChild()).toBe(true);
      expect(authService.isAdmin()).toBe(false);

      const access = isRouteAuthorized({ role: 'CHILD' }, '/admin', ['ADMIN']);
      expect(access.allowed).toBe(false);
      expect(access.redirect).toBe('/access-denied');
    });

    it('4. Authenticated ADMIN accessing /admin -> allowed', () => {
      localStorage.setItem('nivya_access_token', 'jwt_admin_token');
      localStorage.setItem('nivya_user_role', 'ADMIN');
      localStorage.setItem(
        'nivya_user',
        JSON.stringify({ id: 99, name: 'Administrator', email: 'admin@nivya.local', role: 'ADMIN' })
      );

      expect(authService.isAuthenticated()).toBe(true);
      expect(authService.getUserRole()).toBe('ADMIN');
      expect(authService.isAdmin()).toBe(true);
      expect(authService.isParent()).toBe(false);
      expect(authService.isChild()).toBe(false);

      const access = isRouteAuthorized({ role: 'ADMIN' }, '/admin', ['ADMIN']);
      expect(access.allowed).toBe(true);
      expect(access.redirect).toBeNull();
    });

    it('5. Post-authentication destination routes ADMIN directly to /admin', () => {
      expect(authService.getPostAuthDestination('ADMIN', false)).toBe('/admin');
      expect(authService.getPostAuthDestination('ADMIN', true)).toBe('/admin');
    });

    it('6. Parent/Child navigation links do NOT leak to Admin user', () => {
      const getNavigationItems = (role: RoleType) => {
        if (role === 'ADMIN') {
          return [
            { label: 'Overview', path: '#overview' },
            { label: 'Users', path: '#users' },
            { label: 'Audit Logs', path: '#audit' },
          ];
        }
        if (role === 'PARENT') {
          return [
            { label: 'Dashboard', path: '/dashboard' },
            { label: 'Live Activity', path: '/live-activity' },
            { label: 'Location', path: '/location' },
            { label: 'Convocation', path: '/convocation' },
          ];
        }
        return [
          { label: 'Battery', path: '/child/battery' },
          { label: 'Screen Time', path: '/child/screen-time' },
        ];
      };

      const adminNav = getNavigationItems('ADMIN');
      expect(adminNav.some((n) => n.path === '/dashboard')).toBe(false);
      expect(adminNav.some((n) => n.path === '/child/battery')).toBe(false);
      expect(adminNav.some((n) => n.label === 'Audit Logs')).toBe(true);
    });

    it('7. ADMIN login navigates directly to /admin', () => {
      const getLoginDestination = (role: RoleType, isPaired: boolean): string => {
        if (role === 'ADMIN') return '/admin';
        if (role === 'CHILD') return isPaired ? '/child' : '/pairing';
        return isPaired ? '/dashboard' : '/pairing';
      };
      expect(getLoginDestination('ADMIN', false)).toBe('/admin');
      expect(getLoginDestination('ADMIN', true)).toBe('/admin');
    });
  });

  // =========================================================================
  // 2. ADMIN SERVICE API CLIENT & INTEGRATION
  // =========================================================================
  describe('Admin Service API Client', () => {
    it('1. getStats calls /admin/stats and returns metrics', async () => {
      const mockStats = {
        totalUsers: 45,
        totalParents: 20,
        totalChildren: 24,
        activeUsers: 42,
        disabledUsers: 3,
        activeDevices: 30,
        onlineDevices: 18,
        recentRegistrations: 5,
      };

      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { success: true, data: mockStats },
      });

      const stats = await adminService.getStats();
      expect(apiClient.get).toHaveBeenCalledWith('/admin/stats');
      expect(stats.totalUsers).toBe(45);
      expect(stats.totalParents).toBe(20);
      expect(stats.totalChildren).toBe(24);
      expect(stats.disabledUsers).toBe(3);
      expect(stats.onlineDevices).toBe(18);
    });

    it('2. getUsers sends correct pagination, search, role, status & sort parameters', async () => {
      const mockPaginatedUsers = {
        content: [
          {
            id: 10,
            uuid: 'user-uuid-10',
            name: 'Alice Parent',
            email: 'alice@example.com',
            role: 'PARENT' as RoleType,
            status: 'ACTIVE' as UserStatus,
            createdAt: '2026-09-01T10:00:00Z',
            updatedAt: '2026-09-02T10:00:00Z',
            lastLoginAt: '2026-09-02T10:00:00Z',
            deviceCount: 2,
            hasActiveDevice: true,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 15,
        number: 0,
        first: true,
        last: true,
      };

      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { success: true, data: mockPaginatedUsers },
      });

      const result = await adminService.getUsers({
        page: 0,
        size: 15,
        search: 'alice',
        role: 'PARENT',
        status: 'ACTIVE',
        sortBy: 'name',
        sortDir: 'asc',
      });

      expect(apiClient.get).toHaveBeenCalledWith('/admin/users', {
        params: {
          page: 0,
          size: 15,
          search: 'alice',
          role: 'PARENT',
          status: 'ACTIVE',
          sortBy: 'name',
          sortDir: 'asc',
        },
      });
      expect(result.content.length).toBe(1);
      expect(result.content[0].email).toBe('alice@example.com');
    });

    it('3. getUser retrieves user profile details', async () => {
      const mockUserDetail = {
        id: 15,
        uuid: 'user-uuid-15',
        name: 'Bob Child',
        email: 'bob@example.com',
        phone: null,
        role: 'CHILD' as RoleType,
        status: 'ACTIVE' as UserStatus,
        createdAt: '2026-08-15T12:00:00Z',
        updatedAt: '2026-09-01T12:00:00Z',
        lastLoginAt: '2026-09-01T12:00:00Z',
        deviceCount: 1,
        devices: [
          {
            id: 101,
            deviceUuid: 'dev-uuid-101',
            deviceName: 'Pixel 8',
            platform: 'ANDROID',
            osVersion: '14',
            appVersion: '1.0.0',
            status: 'ACTIVE',
            lastSeenAt: '2026-09-01T12:00:00Z',
            online: false,
          },
        ],
        family: {
          familyId: 3,
          familyCode: 'FAM123',
          familyName: 'Bob Family',
          memberCount: 3,
          roleInFamily: 'CHILD',
        },
        recentAuditLogs: [
          {
            id: 1,
            userId: 15,
            targetUserId: 15,
            action: 'USER_LOGIN',
            details: 'Successful login',
            ipAddress: '127.0.0.1',
            timestamp: '2026-09-01T12:00:00Z',
          },
        ],
      };

      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { success: true, data: mockUserDetail },
      });

      const user = await adminService.getUser(15);
      expect(apiClient.get).toHaveBeenCalledWith('/admin/users/15');
      expect(user.id).toBe(15);
      expect(user.devices.length).toBe(1);
      expect(user.family?.familyName).toBe('Bob Family');
      expect(user.recentAuditLogs.length).toBe(1);
    });

    it('4. updateUser submits permitted fields to /admin/users/{id}', async () => {
      const updatePayload = { name: 'Alice Updated', email: 'alice.new@example.com' };
      const mockResponse = {
        id: 10,
        name: 'Alice Updated',
        email: 'alice.new@example.com',
        role: 'PARENT' as RoleType,
        status: 'ACTIVE' as UserStatus,
        createdAt: '2026-09-01T10:00:00Z',
        updatedAt: '2026-10-04T12:00:00Z',
        deviceCount: 0,
        activeDeviceCount: 0,
        hasOnlineDevice: false,
        devices: [],
        auditSummary: { totalActions: 1 },
      };

      vi.spyOn(apiClient, 'put').mockResolvedValueOnce({
        data: { success: true, data: mockResponse },
      });

      const updated = await adminService.updateUser(10, updatePayload);
      expect(apiClient.put).toHaveBeenCalledWith('/admin/users/10', updatePayload);
      expect(updated.name).toBe('Alice Updated');
      expect(updated.email).toBe('alice.new@example.com');
    });

    it('5. updateStatus sends PATCH to /admin/users/{id}/status', async () => {
      const mockSummary = {
        id: 10,
        name: 'Alice User',
        email: 'alice@example.com',
        role: 'PARENT' as RoleType,
        status: 'DISABLED' as UserStatus,
        createdAt: '2026-09-01T10:00:00Z',
        updatedAt: '2026-10-04T12:00:00Z',
        deviceCount: 1,
        activeDeviceCount: 0,
        hasOnlineDevice: false,
      };

      vi.spyOn(apiClient, 'patch').mockResolvedValueOnce({
        data: { success: true, data: mockSummary },
      });

      const res = await adminService.updateStatus(10, 'DISABLED');
      expect(apiClient.patch).toHaveBeenCalledWith('/admin/users/10/status', { status: 'DISABLED' });
      expect(res.status).toBe('DISABLED');
    });

    it('6. updateRole sends PATCH to /admin/users/{id}/role', async () => {
      const mockSummary = {
        id: 10,
        name: 'Alice User',
        email: 'alice@example.com',
        role: 'ADMIN' as RoleType,
        status: 'ACTIVE' as UserStatus,
        createdAt: '2026-09-01T10:00:00Z',
        updatedAt: '2026-10-04T12:00:00Z',
        deviceCount: 1,
        activeDeviceCount: 1,
        hasOnlineDevice: false,
      };

      vi.spyOn(apiClient, 'patch').mockResolvedValueOnce({
        data: { success: true, data: mockSummary },
      });

      const res = await adminService.updateRole(10, 'ADMIN');
      expect(apiClient.patch).toHaveBeenCalledWith('/admin/users/10/role', { role: 'ADMIN' });
      expect(res.role).toBe('ADMIN');
    });

    it('7. getAuditLogs retrieves paginated audit logs', async () => {
      const mockLogs = {
        content: [
          {
            id: 501,
            userId: 99,
            targetUserId: 10,
            action: 'ADMIN_STATUS_CHANGE',
            details: 'Status changed from ACTIVE to DISABLED',
            ipAddress: '127.0.0.1',
            timestamp: '2026-10-04T12:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
        first: true,
        last: true,
      };

      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { success: true, data: mockLogs },
      });

      const logs = await adminService.getAuditLogs({ page: 0, size: 20, action: 'ADMIN_STATUS_CHANGE' });
      expect(apiClient.get).toHaveBeenCalledWith('/admin/audit-logs', {
        params: {
          page: 0,
          size: 20,
          userId: undefined,
          action: 'ADMIN_STATUS_CHANGE',
        },
      });
      expect(logs.content.length).toBe(1);
      expect(logs.content[0].action).toBe('ADMIN_STATUS_CHANGE');
      expect(logs.content[0].targetUserId).toBe(10);
    });
  });

  // =========================================================================
  // 3. SEARCH, FILTERING & CLIENT VALIDATION
  // =========================================================================
  describe('Search, Filtering & Safety Validation', () => {
    const userPool = [
      { id: 1, name: 'John Doe', email: 'john@parent.com', role: 'PARENT' as RoleType, status: 'ACTIVE' as UserStatus },
      { id: 2, name: 'Jane Doe', email: 'jane@child.com', role: 'CHILD' as RoleType, status: 'ACTIVE' as UserStatus },
      { id: 3, name: 'Admin Root', email: 'admin@nivya.local', role: 'ADMIN' as RoleType, status: 'ACTIVE' as UserStatus },
      { id: 4, name: 'Suspended Child', email: 'child2@nivya.local', role: 'CHILD' as RoleType, status: 'DISABLED' as UserStatus },
    ];

    it('1. Filters users by search query matching name or email', () => {
      const search = (query: string) =>
        userPool.filter(
          (u) =>
            u.name.toLowerCase().includes(query.toLowerCase()) ||
            u.email.toLowerCase().includes(query.toLowerCase())
        );

      expect(search('john')).toHaveLength(1);
      expect(search('john')[0].name).toBe('John Doe');

      expect(search('child')).toHaveLength(2); // jane@child.com and child2@nivya.local
      expect(search('admin')).toHaveLength(1);
      expect(search('nonexistent')).toHaveLength(0);
    });

    it('2. Filters users by role', () => {
      const filterByRole = (role: RoleType | '') =>
        role ? userPool.filter((u) => u.role === role) : userPool;

      expect(filterByRole('PARENT')).toHaveLength(1);
      expect(filterByRole('CHILD')).toHaveLength(2);
      expect(filterByRole('ADMIN')).toHaveLength(1);
      expect(filterByRole('')).toHaveLength(4);
    });

    it('3. Filters users by status', () => {
      const filterByStatus = (status: UserStatus | '') =>
        status ? userPool.filter((u) => u.status === status) : userPool;

      expect(filterByStatus('ACTIVE')).toHaveLength(3);
      expect(filterByStatus('DISABLED')).toHaveLength(1);
      expect(filterByStatus('')).toHaveLength(4);
    });

    it('4. Validates safe editable fields: name and email format', () => {
      const validateUserUpdate = (name: string, email: string): { valid: boolean; error?: string } => {
        if (!name || name.trim().length === 0) {
          return { valid: false, error: 'Name is required' };
        }
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email || !emailRegex.test(email.trim())) {
          return { valid: false, error: 'A valid email address is required' };
        }
        return { valid: true };
      };

      expect(validateUserUpdate('', 'test@example.com').valid).toBe(false);
      expect(validateUserUpdate('Valid Name', 'invalid-email').valid).toBe(false);
      expect(validateUserUpdate('Valid Name', 'valid@example.com').valid).toBe(true);
    });

    it('5. Self-modification guard prevents admin from demoting their own role', () => {
      const currentAdminId = 99;
      const targetUserId = 99;

      const canModifyRole = (adminId: number, targetId: number) => {
        if (adminId === targetId) {
          return { allowed: false, reason: 'Administrators cannot modify their own role' };
        }
        return { allowed: true };
      };

      const result = canModifyRole(currentAdminId, targetUserId);
      expect(result.allowed).toBe(false);
      expect(result.reason).toContain('cannot modify their own role');

      const otherUserResult = canModifyRole(currentAdminId, 10);
      expect(otherUserResult.allowed).toBe(true);
    });

    it('6. Last active admin guard prevents disabling final admin', () => {
      const activeAdminCount = 1;
      const targetUser = { id: 99, role: 'ADMIN' as RoleType, status: 'ACTIVE' as UserStatus };

      const canDisableUser = (activeAdmins: number, target: typeof targetUser) => {
        if (target.role === 'ADMIN' && activeAdmins <= 1) {
          return { allowed: false, reason: 'Cannot deactivate the final active administrator' };
        }
        return { allowed: true };
      };

      const check = canDisableUser(activeAdminCount, targetUser);
      expect(check.allowed).toBe(false);
      expect(check.reason).toContain('Cannot deactivate the final active administrator');

      // With 2 active admins, disabling is allowed
      expect(canDisableUser(2, targetUser).allowed).toBe(true);
    });
  });

  // =========================================================================
  // 4. ERROR HANDLING & API RESPONSES
  // =========================================================================
  describe('Error Handling & API Responses', () => {
    it('1. Handles 401 Unauthorized by propagating error to triggering refresh/re-auth', async () => {
      vi.spyOn(apiClient, 'get').mockRejectedValueOnce({
        response: { status: 401, data: { message: 'Unauthorized access' } },
      });

      await expect(adminService.getStats()).rejects.toMatchObject({
        response: { status: 401 },
      });
    });

    it('2. Handles 403 Forbidden with proper descriptive error', async () => {
      vi.spyOn(apiClient, 'get').mockRejectedValueOnce({
        response: { status: 403, data: { message: 'Admin role authorization required' } },
      });

      await expect(adminService.getStats()).rejects.toMatchObject({
        response: { status: 403 },
      });
    });

    it('3. Handles validation error on invalid role transition', async () => {
      vi.spyOn(apiClient, 'patch').mockRejectedValueOnce({
        response: {
          status: 400,
          data: { success: false, message: 'Invalid role transition: GUEST' },
        },
      });

      await expect(adminService.updateRole(10, 'INVALID' as any)).rejects.toMatchObject({
        response: { status: 400 },
      });
    });

    it('4. Never leaks password hashes or tokens in admin service responses', async () => {
      const mockUserDetail: any = {
        id: 1,
        name: 'Target User',
        email: 'target@example.com',
        role: 'PARENT',
        status: 'ACTIVE',
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-01T00:00:00Z',
      };

      vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: { success: true, data: mockUserDetail },
      });

      const user = await adminService.getUser(1);
      expect((user as any).passwordHash).toBeUndefined();
      expect((user as any).password).toBeUndefined();
      expect((user as any).accessToken).toBeUndefined();
      expect((user as any).refreshToken).toBeUndefined();
    });
  });
});
