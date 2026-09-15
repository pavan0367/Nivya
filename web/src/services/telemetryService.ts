import { apiClient } from './api';
import { ApiResponse } from '../types/auth';
import { BatteryStatus, NetworkStatus, LocationStatus, UsageSummary, DeviceHealth, DeviceTelemetrySnapshot } from '../types/telemetry';

export const telemetryService = {
  async getSnapshot(deviceId: number): Promise<DeviceTelemetrySnapshot | null> {
    try {
      const response = await apiClient.get<ApiResponse<DeviceTelemetrySnapshot>>(`/telemetry/snapshot/${deviceId}`);
      return response.data.data ?? null;
    } catch {
      return null;
    }
  },

  async getBatteryStatus(deviceId: number): Promise<BatteryStatus | null> {
    try {
      const response = await apiClient.get<ApiResponse<BatteryStatus>>(`/battery/current/${deviceId}`);
      return response.data.data ?? null;
    } catch {
      return null;
    }
  },

  async getBatteryHistory(deviceId: number): Promise<BatteryStatus[]> {
    try {
      const response = await apiClient.get<ApiResponse<{ snapshots?: BatteryStatus[]; history?: BatteryStatus[] }>>(`/battery/history/${deviceId}`);
      return (response.data.data?.snapshots || response.data.data?.history || []) as BatteryStatus[];
    } catch {
      return [];
    }
  },

  async getNetworkStatus(deviceId: number): Promise<NetworkStatus | null> {
    try {
      const response = await apiClient.get<ApiResponse<NetworkStatus>>(`/network/current/${deviceId}`);
      return response.data.data ?? null;
    } catch {
      return null;
    }
  },

  async getNetworkHistory(deviceId: number): Promise<NetworkStatus[]> {
    try {
      const response = await apiClient.get<ApiResponse<{ snapshots?: NetworkStatus[]; history?: NetworkStatus[] }>>(`/network/history/${deviceId}`);
      return (response.data.data?.snapshots || response.data.data?.history || []) as NetworkStatus[];
    } catch {
      return [];
    }
  },

  async getLocationCurrent(deviceId: number): Promise<LocationStatus | null> {
    try {
      const response = await apiClient.get<ApiResponse<LocationStatus>>(`/location/current/${deviceId}`);
      return response.data.data ?? null;
    } catch {
      return null;
    }
  },

  async getLocationHistory(deviceId: number): Promise<LocationStatus[]> {
    try {
      const response = await apiClient.get<ApiResponse<{ locations?: LocationStatus[]; breadcrumbs?: LocationStatus[] }>>(`/location/history/${deviceId}`);
      return (response.data.data?.locations || response.data.data?.breadcrumbs || []) as LocationStatus[];
    } catch {
      return [];
    }
  },

  async getUsageSummary(deviceId: number, date?: string): Promise<UsageSummary | null> {
    try {
      const url = date ? `/usage/summary/${deviceId}?date=${date}` : `/usage/summary/${deviceId}`;
      const response = await apiClient.get<ApiResponse<UsageSummary>>(url);
      return response.data.data ?? null;
    } catch {
      return null;
    }
  },

  async getDeviceHealth(deviceId: number): Promise<DeviceHealth | null> {
    try {
      const response = await apiClient.get<ApiResponse<DeviceHealth>>(`/device/health/${deviceId}`);
      return response.data.data ?? null;
    } catch {
      return null;
    }
  },

  async sendHeartbeat(payload?: {
    deviceId?: number;
    deviceUuid?: string;
    batteryPct?: number;
    networkType?: string;
    networkQuality?: string;
    isOnline?: boolean;
  }): Promise<any> {
    try {
      const response = await apiClient.post<ApiResponse<any>>('/devices/heartbeat', payload || {});
      return response.data.data ?? null;
    } catch (err) {
      console.warn('Failed to dispatch device heartbeat:', err);
      return null;
    }
  },
};
