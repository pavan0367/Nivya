import { apiClient } from './api';
import { ApiResponse, AuthResponseData, RoleType, User, Device } from '../types/auth';

export const authService = {
  async login(email: string, password: string): Promise<AuthResponseData> {
    const response = await apiClient.post<ApiResponse<AuthResponseData>>('/auth/login', { email, password });
    const data = response.data.data!;
    localStorage.setItem('nivya_access_token', data.accessToken);
    localStorage.setItem('nivya_refresh_token', data.refreshToken);
    localStorage.setItem('nivya_user_role', data.user.role);
    localStorage.setItem('nivya_user', JSON.stringify(data.user));
    return data;
  },

  async register(name: string, email: string, password: string, role: RoleType): Promise<AuthResponseData> {
    const response = await apiClient.post<ApiResponse<AuthResponseData>>('/auth/register', { name, email, password, role });
    const data = response.data.data!;
    localStorage.setItem('nivya_access_token', data.accessToken);
    localStorage.setItem('nivya_refresh_token', data.refreshToken);
    localStorage.setItem('nivya_user_role', data.user.role);
    localStorage.setItem('nivya_user', JSON.stringify(data.user));
    return data;
  },

  async selectRole(role: RoleType): Promise<User> {
    const response = await apiClient.post<ApiResponse<AuthResponseData>>('/role/select', { role });
    const user = response.data.data!.user;
    localStorage.setItem('nivya_user_role', user.role);
    localStorage.setItem('nivya_user', JSON.stringify(user));
    return user;
  },

  async getMe(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>('/auth/me');
    const user = response.data.data!;
    localStorage.setItem('nivya_user_role', user.role);
    localStorage.setItem('nivya_user', JSON.stringify(user));
    return user;
  },

  async getFamilyDevices(): Promise<Device[]> {
    try {
      const response = await apiClient.get<ApiResponse<{ devices: any[] }>>('/pairing/status');
      const rawDevices = response.data.data?.devices || [];
      return rawDevices.map((d) => {
        const resolvedId = Number(d.id ?? d.deviceId ?? 0);
        return {
          ...d,
          id: resolvedId,
          deviceId: resolvedId,
          isChildDevice: Boolean(d.isChildDevice || d.userRole === 'CHILD'),
          status: d.status || (d.isOnline ? 'ACTIVE' : 'OFFLINE'),
        };
      });
    } catch {
      return [];
    }
  },

  async getPairingStatus(): Promise<{ paired: boolean; familyId?: number; familyCode?: string; userRole?: RoleType }> {
    const response = await apiClient.get<ApiResponse<{ paired: boolean; familyId?: number; familyCode?: string; userRole?: RoleType }>>('/pairing/status');
    const data = response.data.data!;
    if (data.paired) {
      localStorage.setItem('nivya_is_paired', 'true');
    } else {
      localStorage.setItem('nivya_is_paired', 'false');
    }
    return data;
  },

  async verifyEmail(email: string, code: string, purpose: string = 'EMAIL_VERIFICATION'): Promise<boolean> {
    const response = await apiClient.post<ApiResponse<{ verified: boolean }>>('/email/verify/confirm', {
      email,
      code,
      purpose,
    });
    return !!response.data.data?.verified;
  },

  async sendVerificationCode(email: string, purpose: string = 'EMAIL_VERIFICATION'): Promise<void> {
    await apiClient.post('/email/verify/send', {
      email,
      purpose,
    });
  },

  logout(): void {
    const refreshToken = localStorage.getItem('nivya_refresh_token');
    if (refreshToken) {
      apiClient.post('/auth/logout', { refreshToken }).catch(() => {});
    }
    localStorage.removeItem('nivya_access_token');
    localStorage.removeItem('nivya_refresh_token');
    localStorage.removeItem('nivya_user_role');
    localStorage.removeItem('nivya_user');
    localStorage.removeItem('nivya_active_device_id');
    localStorage.removeItem('nivya_child_active_device_id');
    localStorage.removeItem('nivya_child_device');
    localStorage.removeItem('nivya_parent_active_device_id');
    // Note: Do not remove nivya_is_paired on logout so relogin recognizes already paired device
    window.location.href = '/login';
  },

  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('nivya_user');
    return userStr ? JSON.parse(userStr) : null;
  },

  getUserRole(): RoleType | null {
    return (localStorage.getItem('nivya_user_role') as RoleType) || null;
  },

  isAuthenticated(): boolean {
    return !!localStorage.getItem('nivya_access_token');
  },

  isParent(): boolean {
    return this.getUserRole() === 'PARENT';
  },

  isChild(): boolean {
    return this.getUserRole() === 'CHILD';
  },

  isPaired(): boolean {
    return localStorage.getItem('nivya_is_paired') === 'true';
  },

  setPaired(paired: boolean): void {
    localStorage.setItem('nivya_is_paired', paired ? 'true' : 'false');
  },

  getPostAuthDestination(role: RoleType, paired: boolean): string {
    if (role === 'CHILD') {
      return paired ? '/child' : '/pairing';
    }
    return paired ? '/dashboard' : '/pairing';
  },

  async getDeletionStatus(): Promise<{
    role: RoleType;
    isChild: boolean;
    hasConnectedParent: boolean;
    connectedParentEmailMasked?: string;
    instructions: string;
  }> {
    const response = await apiClient.get('/account/deletion/status');
    return response.data.data;
  },

  async requestChildDeletionApproval(): Promise<{
    success: boolean;
    approvalCodeRequired: boolean;
    parentEmailMasked?: string;
    expiresInMinutes: number;
    message: string;
  }> {
    const response = await apiClient.post('/account/deletion/request-child-approval');
    return response.data.data;
  },

  async verifyChildDeletionCode(approvalCode: string): Promise<{
    valid: boolean;
    message: string;
  }> {
    const response = await apiClient.post('/account/deletion/verify-child-code', { approvalCode });
    return response.data.data;
  },

  async deleteAccount(password?: string, approvalCode?: string): Promise<void> {
    await apiClient.post('/account/delete', { password, approvalCode });
    localStorage.clear();
  },
};

