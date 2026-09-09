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
      const response = await apiClient.get<ApiResponse<{ devices: Device[] }>>('/pairing/status');
      return response.data.data?.devices || [];
    } catch {
      return [];
    }
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
};
