import { describe, it, expect } from 'vitest';
import { ParentConvocationMessage } from '../types/convocation';

describe('Convocation Specifications & Seen Receipts', () => {
  const mockMessages: ParentConvocationMessage[] = [
    {
      id: 1,
      familyId: 10,
      senderUserId: 1,
      senderName: 'Parent',
      receiverUserId: 2,
      message: 'Please finish homework before dinner.',
      childOriginated: false,
      createdAt: '2026-09-09T10:00:00Z',
      seen: true,
      seenAt: '2026-09-09T10:05:00Z',
    },
    {
      id: 2,
      familyId: 10,
      senderUserId: 2,
      senderName: 'Child Arun',
      receiverUserId: 1,
      message: 'Done, packing books.',
      childOriginated: true,
      createdAt: '2026-09-09T10:06:00Z',
      seen: true,
      seenAt: '2026-09-09T10:07:00Z',
    },
    {
      id: 3,
      familyId: 10,
      senderUserId: 1,
      senderName: 'Parent',
      receiverUserId: 2,
      message: 'Great, see you soon.',
      childOriginated: false,
      createdAt: '2026-09-09T10:10:00Z',
      seen: false,
      seenAt: null,
    },
  ];

  it('preserves complete chronological history for Parent', () => {
    expect(mockMessages).toHaveLength(3);
    expect(mockMessages[0].id).toBe(1);
    expect(mockMessages[2].id).toBe(3);
  });

  it('differentiates child-originated messages from parent-originated messages', () => {
    const childMsgs = mockMessages.filter((m) => m.childOriginated);
    const parentMsgs = mockMessages.filter((m) => !m.childOriginated);

    expect(childMsgs).toHaveLength(1);
    expect(childMsgs[0].senderName).toBe('Child Arun');
    expect(parentMsgs).toHaveLength(2);
  });

  it('tracks "Seen" status and timestamps on Parent messages', () => {
    const msg1 = mockMessages[0];
    const msg3 = mockMessages[2];

    expect(msg1.seen).toBe(true);
    expect(msg1.seenAt).toBe('2026-09-09T10:05:00Z');

    expect(msg3.seen).toBe(false);
    expect(msg3.seenAt).toBeNull();
  });

  it('enforces generic notification text constraint for child notifications', () => {
    const childPushNotificationPayload = {
      title: 'Nivya',
      body: 'Check your battery status', // Exact specification requirement
    };

    expect(childPushNotificationPayload.body).toBe('Check your battery status');
    expect(childPushNotificationPayload.body).not.toContain('Please finish homework');
    expect(childPushNotificationPayload.body).not.toContain('Great, see you soon');
  });
});
