export interface ParentConvocationMessage {
  id: number;
  familyId?: number;
  senderUserId?: number;
  senderName?: string;
  receiverUserId?: number;
  message: string;
  childOriginated: boolean;
  createdAt: string;
  seen: boolean;
  seenAt?: string | null;
  isPinned?: boolean;
  reaction?: string | null;
  replyToId?: number | null;
  status?: string;
}

export interface ChildConvocationMessage {
  id: number;
  message: string;
  createdAt: string;
}

export interface ChildViewingSessionResponse {
  sessionId: string;
  startedAt: string;
  expiresAt: string;
  remainingSeconds: number;
  messages: ChildConvocationMessage[];
}

export interface ChildVisibilityStateResponse {
  viewingActive: boolean;
  remainingSeconds: number;
  unreadCount: number;
}

export type SeenStateMap = Record<number, boolean>;

