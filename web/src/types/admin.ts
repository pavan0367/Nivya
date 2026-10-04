import { RoleType } from './auth';

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'SUSPENDED' | 'PENDING';

export interface AdminStats {
  totalUsers: number;
  totalParents: number;
  totalChildren: number;
  totalAdmins: number;
  activeUsers: number;
  disabledUsers: number;
  totalDevices: number;
  activeDevices: number;
  onlineDevices: number;
  recentRegistrations: AdminUserSummary[];
}

export interface AdminUserSummary {
  id: number;
  uuid: string;
  name: string;
  email: string;
  role: RoleType;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
  deviceCount: number;
  hasActiveDevice: boolean;
}

export interface AdminDevice {
  id: number;
  deviceUuid: string;
  deviceName: string;
  platform: string;
  osVersion: string;
  appVersion: string;
  status: string;
  lastSeenAt: string | null;
  online: boolean;
}

export interface AdminFamily {
  familyId: number;
  familyCode: string;
  familyName: string;
  memberCount: number;
  roleInFamily: string;
}

export interface AdminAuditLog {
  id: number;
  userId: number | null;
  targetUserId: number | null;
  action: string;
  details: string;
  ipAddress: string | null;
  timestamp: string;
}

export interface AdminUserDetail {
  id: number;
  uuid: string;
  name: string;
  email: string;
  phone: string | null;
  role: RoleType;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
  deviceCount: number;
  devices: AdminDevice[];
  family: AdminFamily | null;
  recentAuditLogs: AdminAuditLog[];
}

export interface AdminUserUpdatePayload {
  name: string;
  email: string;
  phone?: string;
  status?: UserStatus;
}

export interface AdminRoleChangePayload {
  role: RoleType;
}

export interface AdminStatusChangePayload {
  status: UserStatus;
}

export interface PaginatedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
