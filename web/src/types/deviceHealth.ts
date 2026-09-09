/**
 * React TypeScript Data Models for Nivya Device Health & Permission Diagnostics
 */

export type HealthStatus = 'EXCELLENT' | 'HEALTHY' | 'WARNING' | 'CRITICAL';
export type SyncState = 'SYNCED' | 'PENDING_SYNC' | 'OFFLINE_QUEUED';
export type PermissionState = 'GRANTED' | 'DENIED' | 'REQUIRED' | 'NOT_REQUESTED';

export interface StorageHealth {
  totalBytes: number;
  usedBytes: number;
  freeBytes: number;
  usedPct: number;
  isLowStorage: boolean;
}

export interface MemoryHealth {
  totalBytes: number;
  usedBytes: number;
  freeBytes: number;
  usedPct: number;
  isLowRam: boolean;
}

export interface PermissionHealth {
  locationPermission: string;
  usagePermission: string;
  notificationPermission: string;
  batteryOptimization: string;
  allHealthy: boolean;
}

/**
 * Detailed Device Health Model for Parent Dashboard
 */
export interface DeviceHealthResponse {
  deviceId: number;
  deviceUuid: string;
  deviceName: string;
  deviceModel: string;
  deviceManufacturer: string;
  osVersion: string;
  sdkVersion?: number;
  storage: StorageHealth;
  memory: MemoryHealth;
  batteryPct?: number;
  chargingState?: string;
  batteryHealth?: string;
  batteryTempCelsius?: number;
  networkType?: string;
  isOnline: boolean;
  syncState: SyncState | string;
  permissionHealth: PermissionHealth;
  healthScore: number;
  healthStatus: HealthStatus;
  conditionSummary?: string;
  storageSummary?: string;
  batterySummary?: string;
  protectionSummary?: string;
  recordedAt: string;
  updatedAt: string;
}

/**
 * Simplified Device Health Model for Child View
 */
export interface ChildDeviceHealthResponse {
  deviceId: number;
  healthScore: number;
  healthStatus: HealthStatus;
  conditionSummary: string;
  storageSummary: string;
  batterySummary: string;
  protectionSummary: string;
  batteryPct?: number;
  chargingState?: string;
  isOnline: boolean;
  freeStorageGb: number;
  recordedAt: string;
}

/**
 * Telemetry Ingest Request Payload
 */
export interface DeviceHealthTelemetryRequest {
  deviceUuid: string;
  deviceModel?: string;
  deviceManufacturer?: string;
  osVersion?: string;
  sdkVersion?: number;
  batteryPct?: number;
  chargingState?: string;
  batteryHealth?: string;
  batteryTempCelsius?: number;
  storageTotalBytes?: number;
  storageUsedBytes?: number;
  storageFreeBytes?: number;
  ramTotalBytes?: number;
  ramUsedBytes?: number;
  ramFreeBytes?: number;
  isLowRam?: boolean;
  networkType?: string;
  isOnline?: boolean;
  locationPermission?: string;
  usagePermission?: string;
  notificationPermission?: string;
  batteryOptimization?: string;
  syncState?: string;
  recordedAt?: string;
}
