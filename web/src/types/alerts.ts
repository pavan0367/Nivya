export type Severity = 'CRITICAL' | 'WARNING' | 'INFO';

export interface Alert {
  id: number;
  familyId: number;
  deviceId: number;
  alertType: string;
  severity: Severity;
  title: string;
  message: string;
  targetRole: string;
  isRead: boolean;
  resolved: boolean;
  createdAt: string;
  readAt?: string;
  resolvedAt?: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
  familyId?: number;
}
