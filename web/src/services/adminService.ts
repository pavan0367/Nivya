import { apiClient } from './api';
import { ApiResponse, RoleType } from '../types/auth';
import {
  AdminStats,
  AdminUserSummary,
  AdminUserDetail,
  AdminAuditLog,
  AdminUserUpdatePayload,
  PaginatedData,
  UserStatus
} from '../types/admin';

export const adminService = {
  async getStats(): Promise<AdminStats> {
    const response = await apiClient.get<ApiResponse<AdminStats>>('/admin/stats');
    return response.data.data!;
  },

  async getUsers(params: {
    page?: number;
    size?: number;
    search?: string;
    role?: RoleType | '';
    status?: UserStatus | '';
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
  } = {}): Promise<PaginatedData<AdminUserSummary>> {
    const response = await apiClient.get<ApiResponse<PaginatedData<AdminUserSummary>>>('/admin/users', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 15,
        search: params.search || undefined,
        role: params.role || undefined,
        status: params.status || undefined,
        sortBy: params.sortBy || 'createdAt',
        sortDir: params.sortDir || 'desc'
      }
    });
    return response.data.data!;
  },

  async getUser(id: number): Promise<AdminUserDetail> {
    const response = await apiClient.get<ApiResponse<AdminUserDetail>>(`/admin/users/${id}`);
    return response.data.data!;
  },

  async updateUser(id: number, payload: AdminUserUpdatePayload): Promise<AdminUserDetail> {
    const response = await apiClient.put<ApiResponse<AdminUserDetail>>(`/admin/users/${id}`, payload);
    return response.data.data!;
  },

  async updateStatus(id: number, status: UserStatus): Promise<AdminUserSummary> {
    const response = await apiClient.patch<ApiResponse<AdminUserSummary>>(`/admin/users/${id}/status`, { status });
    return response.data.data!;
  },

  async updateRole(id: number, role: RoleType): Promise<AdminUserSummary> {
    const response = await apiClient.patch<ApiResponse<AdminUserSummary>>(`/admin/users/${id}/role`, { role });
    return response.data.data!;
  },

  async deleteUser(id: number): Promise<void> {
    await apiClient.delete<ApiResponse<{ message: string }>>(`/admin/users/${id}`);
  },

  async getAuditLogs(params: {
    page?: number;
    size?: number;
    userId?: number;
    action?: string;
  } = {}): Promise<PaginatedData<AdminAuditLog>> {
    const response = await apiClient.get<ApiResponse<PaginatedData<AdminAuditLog>>>('/admin/audit-logs', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        userId: params.userId || undefined,
        action: params.action || undefined
      }
    });
    return response.data.data!;
  }
};
