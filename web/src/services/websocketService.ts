import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type WebSocketConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING';
export type StompMessageCallback = (body: any) => void;

class WebSocketManager {
  private client: Client | null = null;
  private status: WebSocketConnectionStatus = 'DISCONNECTED';
  private listeners: Map<string, Set<StompMessageCallback>> = new Map();
  private connectionListeners: Set<(connected: boolean) => void> = new Set();
  private statusListeners: Set<(status: WebSocketConnectionStatus) => void> = new Set();
  private reconnectListeners: Set<() => void> = new Set();

  private reconnectAttempts = 0;
  private reconnectTimeout: any = null;
  private isManuallyClosed = false;
  private hadConnectedBefore = false;

  private readonly baseDelay = 1000;
  private readonly maxDelay = 30000;

  connect() {
    this.isManuallyClosed = false;
    if (this.client && (this.client.active || this.status === 'CONNECTED')) {
      return;
    }
    this.initiateConnection(false);
  }

  private setStatus(newStatus: WebSocketConnectionStatus) {
    if (this.status === newStatus) return;
    this.status = newStatus;
    const isConn = newStatus === 'CONNECTED';
    this.connectionListeners.forEach((cb) => cb(isConn));
    this.statusListeners.forEach((cb) => cb(newStatus));
  }

  private initiateConnection(isReconnect = false) {
    if (this.isManuallyClosed) return;

    this.setStatus(isReconnect ? 'RECONNECTING' : 'CONNECTING');

    const token = localStorage.getItem('nivya_access_token');

    // Clean up any old client
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (e) {
        // ignore
      }
      this.client = null;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (_str) => {},
      reconnectDelay: 0, // We handle exponential backoff manually
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        const wasReconnecting = this.hadConnectedBefore || isReconnect;
        this.hadConnectedBefore = true;
        this.reconnectAttempts = 0;
        this.setStatus('CONNECTED');

        // Resubscribe all registered topic listeners
        this.listeners.forEach((_callbacks, topic) => {
          this.subscribeInternal(topic);
        });

        // If we recovered from a prior connection loss or reconnect, trigger snapshot rehydration
        if (wasReconnecting) {
          this.reconnectListeners.forEach((cb) => {
            try {
              cb();
            } catch (err) {
              console.error('Error in reconnect listener:', err);
            }
          });
        }
      },
      onDisconnect: () => {
        if (!this.isManuallyClosed) {
          this.scheduleReconnect();
        } else {
          this.setStatus('DISCONNECTED');
        }
      },
      onStompError: (frame) => {
        console.warn('STOMP protocol error:', frame.headers['message']);
        if (!this.isManuallyClosed) {
          this.scheduleReconnect();
        }
      },
      onWebSocketClose: () => {
        if (!this.isManuallyClosed) {
          this.scheduleReconnect();
        } else {
          this.setStatus('DISCONNECTED');
        }
      },
    });

    this.client.activate();
  }

  private scheduleReconnect() {
    if (this.isManuallyClosed) return;
    this.setStatus('RECONNECTING');

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    // Exponential backoff: min(maxDelay, baseDelay * 1.5^attempts) + random jitter (0-500ms)
    const factor = Math.pow(1.5, Math.min(this.reconnectAttempts, 8));
    const delay = Math.min(this.maxDelay, Math.floor(this.baseDelay * factor) + Math.floor(Math.random() * 500));
    this.reconnectAttempts++;

    this.reconnectTimeout = setTimeout(() => {
      this.initiateConnection(true);
    }, delay);
  }

  disconnect() {
    this.isManuallyClosed = true;
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    this.reconnectAttempts = 0;
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.setStatus('DISCONNECTED');
  }

  subscribe(topic: string, callback: StompMessageCallback): () => void {
    if (!this.listeners.has(topic)) {
      this.listeners.set(topic, new Set());
      if (this.status === 'CONNECTED') {
        this.subscribeInternal(topic);
      }
    }
    this.listeners.get(topic)!.add(callback);

    return () => {
      const set = this.listeners.get(topic);
      if (set) {
        set.delete(callback);
        if (set.size === 0) {
          this.listeners.delete(topic);
        }
      }
    };
  }

  private subscribeInternal(topic: string) {
    if (!this.client || this.status !== 'CONNECTED') return;
    try {
      this.client.subscribe(topic, (message) => {
        try {
          const parsed = JSON.parse(message.body);
          const callbacks = this.listeners.get(topic);
          if (callbacks) {
            callbacks.forEach((cb) => cb(parsed));
          }
        } catch (e) {
          console.error('Failed to parse STOMP message:', e);
        }
      });
    } catch (err) {
      console.warn('STOMP subscribe failure for', topic, err);
    }
  }

  onConnectionChange(callback: (connected: boolean) => void): () => void {
    this.connectionListeners.add(callback);
    callback(this.status === 'CONNECTED');
    return () => {
      this.connectionListeners.delete(callback);
    };
  }

  onStatusChange(callback: (status: WebSocketConnectionStatus) => void): () => void {
    this.statusListeners.add(callback);
    callback(this.status);
    return () => {
      this.statusListeners.delete(callback);
    };
  }

  onReconnect(callback: () => void): () => void {
    this.reconnectListeners.add(callback);
    return () => {
      this.reconnectListeners.delete(callback);
    };
  }

  isConnected(): boolean {
    return this.status === 'CONNECTED';
  }

  getStatus(): WebSocketConnectionStatus {
    return this.status;
  }
}

export const websocketService = new WebSocketManager();
