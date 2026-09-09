export interface LiveActivityEvent {
  id?: number;
  childUserId: number;
  appName: string;
  broadActivity: string;
  startedAt: string;
  durationSeconds?: number;
  ongoing: boolean;
}

export interface HistoryEvent {
  id: number;
  childUserId: number;
  appName: string;
  broadActivity: string;
  label?: string;
  startedAt: string;
  durationMinutes: number;
}
