import { apiClient } from './api';
import { ApiResponse } from '../types/auth';
import { ParentConvocationMessage, SeenStateMap } from '../types/convocation';

export const convocationService = {
  async getRetainedHistory(): Promise<ParentConvocationMessage[]> {
    const response = await apiClient.get<ApiResponse<ParentConvocationMessage[]>>('/convocation/parent/history');
    return response.data.data || [];
  },

  async sendMessage(receiverUserId: number, message: string): Promise<ParentConvocationMessage> {
    const response = await apiClient.post<ApiResponse<ParentConvocationMessage>>('/convocation/parent/send', {
      receiverUserId,
      message,
    });
    return response.data.data!;
  },

  async getSeenState(): Promise<SeenStateMap> {
    const response = await apiClient.get<ApiResponse<SeenStateMap>>('/convocation/parent/seen');
    return response.data.data || {};
  },
};
