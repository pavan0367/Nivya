import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type StompMessageCallback = (body: any) => void;

class WebSocketManager {
  private client: Client | null = null;
  private connected = false;
  private listeners: Map<string, Set<StompMessageCallback>> = new Map();
  private connectionListeners: Set<(connected: boolean) => void> = new Set();

  connect() {
    if (this.client && (this.client.active || this.connected)) {
      return;
    }

    const token = localStorage.getItem('nivya_access_token');

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => {
        // console.log('[STOMP Debug]', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        this.notifyConnectionState(true);
        // Resubscribe all active topics
        this.listeners.forEach((callbacks, topic) => {
          this.subscribeInternal(topic);
        });
      },
      onDisconnect: () => {
        this.connected = false;
        this.notifyConnectionState(false);
      },
      onStompError: (frame) => {
        console.warn('STOMP protocol error:', frame.headers['message']);
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected = false;
      this.notifyConnectionState(false);
    }
  }

  subscribe(topic: string, callback: StompMessageCallback): () => void {
    if (!this.listeners.has(topic)) {
      this.listeners.set(topic, new Set());
      if (this.connected) {
        this.subscribeInternal(topic);
      }
    }
    this.listeners.get(topic)!.add(callback);

    // Return unsubscription lambda
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
    if (!this.client || !this.connected) return;
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
  }

  onConnectionChange(callback: (connected: boolean) => void): () => void {
    this.connectionListeners.add(callback);
    callback(this.connected);
    return () => {
      this.connectionListeners.delete(callback);
    };
  }

  private notifyConnectionState(state: boolean) {
    this.connectionListeners.forEach((cb) => cb(state));
  }

  isConnected(): boolean {
    return this.connected;
  }
}

export const websocketService = new WebSocketManager();
