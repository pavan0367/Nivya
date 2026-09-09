import { describe, it, expect, vi, beforeEach } from 'vitest';
import { websocketService } from '../services/websocketService';

describe('WebSocketManager', () => {
  beforeEach(() => {
    websocketService.disconnect();
    vi.clearAllMocks();
  });

  it('initializes with DISCONNECTED status', () => {
    expect(websocketService.getStatus()).toBe('DISCONNECTED');
    expect(websocketService.isConnected()).toBe(false);
  });

  it('allows subscription and cleanly unregisters callback', () => {
    const callback = vi.fn();
    const unsub = websocketService.subscribe('/topic/battery/1', callback);

    expect(typeof unsub).toBe('function');
    unsub();
  });

  it('notifies status change listeners', () => {
    const statusHistory: string[] = [];
    const unsub = websocketService.onStatusChange((status) => {
      statusHistory.push(status);
    });

    expect(statusHistory).toContain('DISCONNECTED');
    unsub();
  });

  it('registers and removes reconnect hooks', () => {
    const hook = vi.fn();
    const unsub = websocketService.onReconnect(hook);

    expect(typeof unsub).toBe('function');
    unsub();
  });

  it('disconnect safely sets status to DISCONNECTED', () => {
    websocketService.disconnect();
    expect(websocketService.getStatus()).toBe('DISCONNECTED');
    expect(websocketService.isConnected()).toBe(false);
  });
});
