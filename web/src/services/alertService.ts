import { apiClient } from './api';
import { ApiResponse } from '../types/auth';
import { Alert, UnreadCountResponse } from '../types/alerts';

export const alertService = {
  async getFamilyAlerts(familyId: number, unreadOnly = false, severity?: string): Promise<Alert[]> {
    const params = new URLSearchParams();
    if (unreadOnly) params.append('unreadOnly', 'true');
    if (severity && severity !== 'ALL') params.append('severity', severity);

    const response = await apiClient.get<ApiResponse<Alert[]>>(`/alerts/family/${familyId}?${params.toString()}`);
    return response.data.data || [];
  },

  async getUnreadCount(): Promise<UnreadCountResponse> {
    const response = await apiClient.get<ApiResponse<UnreadCountResponse>>('/alerts/unread-count');
    return response.data.data || { unreadCount: 0 };
  },

  async getChildAlerts(): Promise<Alert[]> {
    const response = await apiClient.get<ApiResponse<Alert[]>>('/alerts/my');
    return response.data.data || [];
  },

  async getAlerts(_deviceId?: number): Promise<Alert[]> {
    return this.getChildAlerts();
  },

  async markAsRead(alertId: number): Promise<void> {
    await apiClient.post(`/alerts/${alertId}/read`);
  },

  async resolveAlert(alertId: number): Promise<void> {
    await apiClient.post(`/alerts/${alertId}/resolve`);
  },
};
