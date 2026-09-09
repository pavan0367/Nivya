export interface ParentConvocationMessage {
  id: number;
  familyId: number;
  senderUserId: number;
  senderName: string;
  receiverUserId: number;
  message: string;
  childOriginated: boolean;
  createdAt: string;
  seen: boolean;
  seenAt?: string | null;
}

export type SeenStateMap = Record<number, boolean>;
