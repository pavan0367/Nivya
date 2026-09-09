import { apiClient } from './api';
import { ApiResponse } from '../types/auth';
import { LiveActivityEvent, HistoryEvent } from '../types/activity';

export interface BackendLiveActivityResponse {
  deviceId: number;
  deviceUuid: string;
  deviceName: string;
  online: boolean;
  currentActivity: {
    id: number;
    appName: string;
    broadActivity: string;
    category?: string;
    durationSeconds?: number;
    durationFormatted?: string;
    startedAt: string;
  } | null;
  recentActivities: Array<{
    id: number;
    appName: string;
    broadActivity: string;
    category?: string;
    durationSeconds?: number;
    durationFormatted?: string;
    startedAt: string;
  }>;
  lastUpdatedAt: string;
}

export const activityService = {
  async getLiveActivity(deviceId: number): Promise<BackendLiveActivityResponse | null> {
    try {
      const response = await apiClient.get<ApiResponse<BackendLiveActivityResponse>>(`/activity/live/${deviceId}`);
      return response.data.data || null;
    } catch {
      return null;
    }
  },

  async getRecentTimeline(deviceId: number): Promise<LiveActivityEvent[]> {
    try {
      const res = await this.getLiveActivity(deviceId);
      if (!res || !res.recentActivities) return [];
      return res.recentActivities.map((a) => ({
        id: a.id,
        childUserId: deviceId,
        appName: a.appName,
        broadActivity: a.broadActivity,
        startedAt: a.startedAt,
        durationSeconds: a.durationSeconds,
        ongoing: false,
      }));
    } catch {
      return [];
    }
  },

  async getHistory(childUserId: number, page = 0, size = 20, app?: string, date?: string): Promise<{ content: HistoryEvent[]; totalPages: number }> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    if (app && app !== 'ALL') params.append('application', app);
    if (date) params.append('startDate', date);

    const response = await apiClient.get<ApiResponse<{ content: HistoryEvent[]; totalPages: number }>>(`/history/${childUserId}?${params.toString()}`);
    return response.data.data || { content: [], totalPages: 0 };
  },
};
