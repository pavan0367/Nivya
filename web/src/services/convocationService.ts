import { apiClient } from './api';
import { ApiResponse } from '../types/auth';
import {
  ParentConvocationMessage,
  ChildConvocationMessage,
  ChildViewingSessionResponse,
  ChildVisibilityStateResponse,
  SeenStateMap,
} from '../types/convocation';

export const convocationService = {
  async getRetainedHistory(): Promise<ParentConvocationMessage[]> {
    const response = await apiClient.get<ApiResponse<ParentConvocationMessage[]>>('/convocation/parent/history');
    return response.data.data || [];
  },

  async sendMessage(
    receiverUserId?: number | null,
    message: string = '',
    replyToId?: number | null,
    targetDeviceId?: number | null
  ): Promise<ParentConvocationMessage> {
    const payload: { message: string; receiverUserId?: number; replyToId?: number; targetDeviceId?: number } = { message };
    if (receiverUserId) payload.receiverUserId = receiverUserId;
    if (replyToId) payload.replyToId = replyToId;
    if (targetDeviceId) payload.targetDeviceId = targetDeviceId;
    const response = await apiClient.post<ApiResponse<ParentConvocationMessage>>('/convocation/parent/send', payload);
    return response.data.data!;
  },

  async unsendMessage(messageId: number): Promise<void> {
    await apiClient.post<ApiResponse<void>>(`/convocation/parent/message/${messageId}/unsend`);
  },

  async togglePinMessage(messageId: number): Promise<ParentConvocationMessage> {
    const response = await apiClient.post<ApiResponse<ParentConvocationMessage>>(`/convocation/parent/message/${messageId}/pin`);
    return response.data.data!;
  },

  async reactToMessage(messageId: number, reaction: string): Promise<ParentConvocationMessage> {
    const response = await apiClient.post<ApiResponse<ParentConvocationMessage>>(`/convocation/parent/message/${messageId}/react`, {
      reaction,
    });
    return response.data.data!;
  },

  async getSeenState(): Promise<SeenStateMap> {
    const response = await apiClient.get<ApiResponse<SeenStateMap>>('/convocation/parent/seen');
    return response.data.data || {};
  },

  // Child APIs
  async getChildUnreadMessages(): Promise<ChildConvocationMessage[]> {
    const response = await apiClient.get<ApiResponse<ChildConvocationMessage[]>>('/convocation/child/unread');
    return response.data.data || [];
  },

  async childStartViewing(): Promise<ChildViewingSessionResponse> {
    const response = await apiClient.post<ApiResponse<ChildViewingSessionResponse>>('/convocation/child/view/start');
    return response.data.data!;
  },

  async childGetVisibilityState(): Promise<ChildVisibilityStateResponse> {
    const response = await apiClient.get<ApiResponse<ChildVisibilityStateResponse>>('/convocation/child/visibility');
    return response.data.data!;
  },

  async childSendMessage(message: string): Promise<Record<string, any>> {
    const response = await apiClient.post<ApiResponse<Record<string, any>>>('/convocation/child/send', {
      message,
    });
    return response.data.data || {};
  },
};

