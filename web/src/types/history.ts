/**
 * TypeScript Data Models for Nivya Parent-Only History
 */

export type DateFilterPreset = 'ALL_TIME' | 'TODAY' | 'LAST_7_DAYS' | 'LAST_30_DAYS';

export interface HistoryEvent {
  id: number;
  deviceId: number;
  packageName: string;
  appName: string;
  broadActivity: string;
  activityLabel?: string | null;
  category?: string | null;
  durationSeconds: number;
  durationFormatted: string;
  eventTimestamp: string;
}

export interface HistoryEventDetail {
  id: number;
  deviceId: number;
  deviceName?: string | null;
  packageName: string;
  appName: string;
  broadActivity: string;
  activityLabel?: string | null;
  category?: string | null;
  durationSeconds: number;
  durationFormatted: string;
  details?: string | null;
  eventTimestamp: string;
  recordedAt: string;
}

export interface HistoryPageResponse {
  items: HistoryEvent[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface HistoryFilterParams {
  deviceId: number;
  page?: number;
  size?: number;
  startDate?: string | null;
  endDate?: string | null;
  application?: string | null;
}
