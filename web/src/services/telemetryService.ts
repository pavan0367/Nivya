import { apiClient } from './api';
import { ApiResponse } from '../types/auth';
import { BatteryStatus, NetworkStatus, LocationStatus, UsageSummary, DeviceHealth, DeviceTelemetrySnapshot } from '../types/telemetry';

export const telemetryService = {
  async getSnapshot(deviceId: number): Promise<DeviceTelemetrySnapshot> {
    const response = await apiClient.get<ApiResponse<DeviceTelemetrySnapshot>>(`/telemetry/snapshot/${deviceId}`);
    return response.data.data!;
  },

  async getBatteryStatus(deviceId: number): Promise<BatteryStatus> {
    const response = await apiClient.get<ApiResponse<BatteryStatus>>(`/battery/current/${deviceId}`);
    return response.data.data!;
  },

  async getBatteryHistory(deviceId: number): Promise<BatteryStatus[]> {
    const response = await apiClient.get<ApiResponse<{ snapshots?: BatteryStatus[]; history?: BatteryStatus[] }>>(`/battery/history/${deviceId}`);
    return (response.data.data?.snapshots || response.data.data?.history || []) as BatteryStatus[];
  },

  async getNetworkStatus(deviceId: number): Promise<NetworkStatus> {
    const response = await apiClient.get<ApiResponse<NetworkStatus>>(`/network/current/${deviceId}`);
    return response.data.data!;
  },

  async getNetworkHistory(deviceId: number): Promise<NetworkStatus[]> {
    const response = await apiClient.get<ApiResponse<{ snapshots?: NetworkStatus[]; history?: NetworkStatus[] }>>(`/network/history/${deviceId}`);
    return (response.data.data?.snapshots || response.data.data?.history || []) as NetworkStatus[];
  },

  async getLocationCurrent(deviceId: number): Promise<LocationStatus> {
    const response = await apiClient.get<ApiResponse<LocationStatus>>(`/location/current/${deviceId}`);
    return response.data.data!;
  },

  async getLocationHistory(deviceId: number): Promise<LocationStatus[]> {
    const response = await apiClient.get<ApiResponse<{ locations?: LocationStatus[]; breadcrumbs?: LocationStatus[] }>>(`/location/history/${deviceId}`);
    return (response.data.data?.locations || response.data.data?.breadcrumbs || []) as LocationStatus[];
  },

  async getUsageSummary(deviceId: number, date?: string): Promise<UsageSummary> {
    const url = date ? `/usage/summary/${deviceId}?date=${date}` : `/usage/summary/${deviceId}`;
    const response = await apiClient.get<ApiResponse<UsageSummary>>(url);
    return response.data.data!;
  },

  async getDeviceHealth(deviceId: number): Promise<DeviceHealth> {
    const response = await apiClient.get<ApiResponse<DeviceHealth>>(`/device/health/${deviceId}`);
    return response.data.data!;
  },
};
