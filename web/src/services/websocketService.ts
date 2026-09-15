import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Resolve WebSocket base URL: defaults to local relative '/ws' in development,
// or uses VITE_WS_BASE_URL (e.g. 'https://nivya-blbf.onrender.com/ws') in production.
const rawWsBase = (import.meta.env.VITE_WS_BASE_URL as string | undefined)?.trim();
export const WS_BASE = rawWsBase ? rawWsBase.replace(/\/+$/, '') : '/ws';

export type WebSocketConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING';
export type StompMessageCallback = (body: any) => void;

class WebSocketManager {
  private client: Client | null = null;
  private status: WebSocketConnectionStatus = 'DISCONNECTED';
  private listeners: Map<string, Set<StompMessageCallback>> = new Map();
  private activeSubscriptions: Map<string, any> = new Map();
  private connectionListeners: Set<(connected: boolean) => void> = new Set();
  private statusListeners: Set<(status: WebSocketConnectionStatus) => void> = new Set();
  private reconnectListeners: Set<() => void> = new Set();

  private reconnectAttempts = 0;
  private reconnectTimeout: any = null;
  private isManuallyClosed = false;
  private hadConnectedBefore = false;

  private readonly baseDelay = 1000;
  private readonly maxDelay = 30000;

  constructor() {}

  public connect() {
    this.isManuallyClosed = false;
    if (this.status === 'CONNECTED' || this.status === 'CONNECTING') {
      return;
    }
    this.initiateConnection();
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
    this.activeSubscriptions.clear();

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_BASE),
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
        this.activeSubscriptions.clear();
        if (!this.isManuallyClosed) {
          this.scheduleReconnect();
        } else {
          this.setStatus('DISCONNECTED');
        }
      },
      onStompError: (frame) => {
        console.warn('STOMP protocol error:', frame.headers['message']);
        this.activeSubscriptions.clear();
        if (!this.isManuallyClosed) {
          this.scheduleReconnect();
        }
      },
      onWebSocketClose: () => {
        this.activeSubscriptions.clear();
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
    this.activeSubscriptions.clear();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.setStatus('DISCONNECTED');
  }

  subscribe(topic: string, callback: StompMessageCallback): () => void {
    if (!this.listeners.has(topic)) {
      this.listeners.set(topic, new Set());
    }
    this.listeners.get(topic)!.add(callback);

    if (this.status === 'CONNECTED' && !this.activeSubscriptions.has(topic)) {
      this.subscribeInternal(topic);
    }

    return () => {
      const set = this.listeners.get(topic);
      if (set) {
        set.delete(callback);
        if (set.size === 0) {
          this.listeners.delete(topic);
          const activeSub = this.activeSubscriptions.get(topic);
          if (activeSub) {
            try {
              activeSub.unsubscribe();
            } catch (e) {
              // ignore
            }
            this.activeSubscriptions.delete(topic);
          }
        }
      }
    };
  }

  private subscribeInternal(topic: string) {
    if (!this.client || this.status !== 'CONNECTED') return;
    if (this.activeSubscriptions.has(topic)) return;

    try {
      const sub = this.client.subscribe(topic, (message) => {
        try {
          const parsed = typeof message.body === 'string' ? JSON.parse(message.body) : message.body;
          const callbacks = this.listeners.get(topic);
          if (callbacks) {
            callbacks.forEach((cb) => {
              try {
                cb(parsed);
              } catch (cbErr) {
                console.error(`Error in STOMP callback for ${topic}:`, cbErr);
              }
            });
          }
        } catch (e) {
          console.error('Failed to parse STOMP message:', e);
        }
      });
      this.activeSubscriptions.set(topic, sub);
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
