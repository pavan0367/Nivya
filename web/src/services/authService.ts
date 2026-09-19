import { apiClient } from './api';
import { ApiResponse, AuthResponseData, RoleType, User, Device } from '../types/auth';
let inFlightApprovalRequest: Promise<{
  success: boolean;
  approvalCodeRequired: boolean;
  parentEmailMasked?: string;
  expiresInMinutes: number;
  message: string;
}> | null = null;

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
    parentEmailMasked?: string;
    instructions: string;
    hasPendingApprovalCode?: boolean;
    approvalCodeExpiresInSeconds?: number;
    deliveryStatus?: 'IDLE' | 'DISPATCHING' | 'DELIVERED' | 'FAILED';
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
    if (inFlightApprovalRequest) {
      return inFlightApprovalRequest;
    }

    inFlightApprovalRequest = (async () => {
      try {
        // Request-specific timeout of 45000ms ensures HTTPS transactional email delivery
        // has ample time to complete, while keeping global 15s timeout for other requests.
        const response = await apiClient.post(
          '/account/deletion/request-child-approval',
          {},
          { timeout: 45000 }
        );
        return response.data.data;
      } catch (err: any) {
        // If it was a client timeout or network abort without an HTTP response from the server,
        // safely reconcile the status using deliveryStatus rather than assuming false failure:
        const isTimeout =
          err?.code === 'ECONNABORTED' ||
          err?.message?.toLowerCase().includes('timeout') ||
          (!err?.response && !err?.status);

        if (isTimeout) {
          // Bounded reconciliation: poll getDeletionStatus up to 5 times (max 10s total)
          const maxReconcileAttempts = 5;
          for (let attempt = 0; attempt < maxReconcileAttempts; attempt++) {
            try {
              const status = await authService.getDeletionStatus();
              if (status.deliveryStatus === 'DELIVERED') {
                return {
                  success: true,
                  approvalCodeRequired: true,
                  parentEmailMasked: status.connectedParentEmailMasked || status.parentEmailMasked,
                  expiresInMinutes: Math.max(1, Math.ceil((status.approvalCodeExpiresInSeconds || 900) / 60)),
                  message: 'Approval code sent to your connected parent.',
                };
              }
              if (status.deliveryStatus === 'FAILED') {
                throw new Error('Failed to deliver parent approval code email.');
              }
              if (status.deliveryStatus === 'DISPATCHING') {
                if (attempt < maxReconcileAttempts - 1) {
                  await new Promise((resolve) => setTimeout(resolve, 2000));
                  continue;
                }
              }
            } catch (reconcileErr: any) {
              if (reconcileErr.message === 'Failed to deliver parent approval code email.') {
                throw reconcileErr;
              }
              // If status check request fails, do not invent success
            }
          }
        }
        // Genuine 4xx/5xx server response or failed reconciliation: re-throw actual error
        throw err;
      } finally {
        inFlightApprovalRequest = null;
      }
    })();

    return inFlightApprovalRequest;
  },

  async verifyChildDeletionCode(approvalCode: string): Promise<{
    valid: boolean;
    approved?: boolean;
    message?: string;
  }> {
    const response = await apiClient.post('/account/deletion/verify-child-code', { code: approvalCode });
    const data = response.data?.data;
    const isApproved = data?.approved === true || data?.valid === true;
    return {
      valid: isApproved,
      approved: isApproved,
      message: response.data?.message,
    };
  },

  async deleteAccount(password?: string, approvalCode?: string): Promise<void> {
    await apiClient.post('/account/delete', { password, approvalCode });
    localStorage.clear();
  },
};

