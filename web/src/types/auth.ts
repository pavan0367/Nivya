export type RoleType = 'PARENT' | 'CHILD';

export interface User {
  id: number;
  uuid?: string;
  name: string;
  email: string;
  role: RoleType;
  status?: string;
}

export interface AuthResponseData {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
  error?: string;
  status?: number;
}

export interface FamilyMember {
  id: number;
  userId: number;
  name: string;
  email: string;
  role: RoleType;
}

export interface Device {
  id: number;
  deviceUuid: string;
  deviceName: string;
  platform: string;
  pushToken?: string | null;
  status: string;
  lastSeenAt?: string;
  online?: boolean;
}

export type SessionStatus = 'ACTIVE' | 'LOGGED_OUT' | 'REVOKED' | 'EXPIRED';

export interface DeviceSession {
  id: number;
  sessionToken: string;
  deviceId?: string;
  deviceName?: string;
  platform?: string;
  appVersion?: string;
  ipAddress?: string;
  approximateLocation?: string;
  status: SessionStatus;
  createdAt: string;
  lastActiveAt?: string;
  loggedOutAt?: string;
}

export interface EmailPreferences {
  userId?: number;
  email?: string;
  emailVerified?: boolean;
  loginAlertsEnabled: boolean;
  newDeviceAlertsEnabled: boolean;
  appUpdateAlertsEnabled: boolean;
  updatedAt?: string;
}

