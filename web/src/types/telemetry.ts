export interface BatteryStatus {
  id?: number;
  batteryPct: number;
  isCharging: boolean;
  powerSaveMode: boolean;
  temperatureCelsius?: number;
  drainRatePerHour?: number;
  estimatedRemainingHours?: number;
  healthStatus: 'GOOD' | 'NORMAL' | 'OVERHEAT' | 'DEGRADED' | 'DEAD' | 'UNKNOWN';
  recordedAt: string;
}

export interface NetworkStatus {
  id?: number;
  networkType: 'WIFI' | 'CELLULAR' | 'NONE';
  connectionType?: string;
  isInternetAvailable: boolean;
  signalLevel: number;
  networkQuality: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'DISCONNECTED';
  ssid?: string;
  recordedAt: string;
}

export interface LocationStatus {
  id?: number;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  locationName?: string;
  isStale: boolean;
  recordedAt: string;
}

export interface AppUsage {
  id?: number;
  packageName: string;
  appName: string;
  category: string;
  durationMinutes: number;
  openCount?: number;
}

export interface UsageSummary {
  deviceId: number;
  date: string;
  totalScreenTimeMinutes: number;
  categories: Record<string, number>;
  appUsages: AppUsage[];
}

export interface DeviceHealth {
  deviceId: number;
  totalStorageBytes: number;
  freeStorageBytes: number;
  isLowStorage: boolean;
  isHealthy: boolean;
  permissions: {
    location: boolean;
    usage: boolean;
    notification: boolean;
  };
  recordedAt: string;
}

export interface DeviceTelemetrySnapshot {
  deviceId: number;
  isOnline: boolean;
  status: string;
  battery?: BatteryStatus | null;
  network?: NetworkStatus | null;
  location?: LocationStatus | null;
  activity?: any | null;
  deviceHealth?: DeviceHealth | null;
  lastUpdated?: string;
}

