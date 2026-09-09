import { DeviceHealthResponse, ChildDeviceHealthResponse, DeviceHealthTelemetryRequest } from '../types/deviceHealth';

const BASE_URL = '/api/v1/device/health';

/**
 * Service providing API client calls for Nivya Device Health
 */
export const deviceHealthService = {
  /**
   * Retrieves full device health diagnostics for parents
   */
  async getDeviceHealth(deviceId: number, token: string): Promise<DeviceHealthResponse> {
    const res = await fetch(`${BASE_URL}/${deviceId}`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });
    if (!res.ok) {
      throw new Error(`Failed to fetch device health: ${res.statusText}`);
    }
    const json = await res.json();
    return json.data;
  },

  /**
   * Retrieves simplified device health for current child device
   */
  async getMyDeviceHealth(token: string): Promise<ChildDeviceHealthResponse> {
    const res = await fetch(`${BASE_URL}/my`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });
    if (!res.ok) {
      throw new Error(`Failed to fetch own device health: ${res.statusText}`);
    }
    const json = await res.json();
    return json.data;
  },

  /**
   * Records device health telemetry
   */
  async recordTelemetry(request: DeviceHealthTelemetryRequest, token: string): Promise<DeviceHealthResponse> {
    const res = await fetch(`${BASE_URL}/telemetry`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!res.ok) {
      throw new Error(`Failed to record device health: ${res.statusText}`);
    }
    const json = await res.json();
    return json.data;
  },
};
