import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  MessageSquareQuote,
  Send,
  MoreVertical,
  Clock,
  Radio,
  CheckCircle,
  RefreshCw,
} from 'lucide-react';
import { convocationService } from '../../services/convocationService';
import { websocketService, WebSocketConnectionStatus } from '../../services/websocketService';
import { ChildConvocationMessage } from '../../types/convocation';

export const ChildConvocationPage: React.FC = () => {
  const [isTurnedOn, setIsTurnedOn] = useState<boolean>(false);
  const [optionsOpen, setOptionsOpen] = useState<boolean>(false);
  const [messages, setMessages] = useState<ChildConvocationMessage[]>([]);
  const [remainingSeconds, setRemainingSeconds] = useState<number>(0);
  const [inputText, setInputText] = useState<string>('');
  const [sending, setSending] = useState<boolean>(false);
  const [noteSentFeedback, setNoteSentFeedback] = useState<boolean>(false);
  const [wsStatus, setWsStatus] = useState<WebSocketConnectionStatus>(websocketService.getStatus());
  const [loading, setLoading] = useState<boolean>(false);

  const optionsRef = useRef<HTMLDivElement>(null);
  const timerRef = useRef<any>(null);
  const isTurnedOnRef = useRef<boolean>(isTurnedOn);
  const inputRef = useRef<HTMLInputElement>(null);
  const wasSendingRef = useRef<boolean>(false);

  // Focus restoration after sending note completes (success or failure)
  useEffect(() => {
    if (wasSendingRef.current && !sending) {
      inputRef.current?.focus();
    }
    wasSendingRef.current = sending;
  }, [sending]);

  useEffect(() => {
    isTurnedOnRef.current = isTurnedOn;
  }, [isTurnedOn]);

  const normalizeChildMessage = (incoming: any): ChildConvocationMessage => {
    let createdAtStr = incoming.createdAt;
    if (typeof incoming.createdAt === 'number') {
      const ms = incoming.createdAt > 1e11 ? incoming.createdAt : incoming.createdAt * 1000;
      createdAtStr = new Date(ms).toISOString();
    } else if (!incoming.createdAt) {
      createdAtStr = new Date().toISOString();
    }

    return {
      id: Number(incoming.id),
      message: String(incoming.message || ''),
      createdAt: String(createdAtStr),
    };
  };

  // Monitor WebSocket Connection Status & Setup Reconnect Sync
  useEffect(() => {
    websocketService.connect();

    const unsubStatus = websocketService.onStatusChange((status) => {
      setWsStatus(status);
    });

    const unsubReconnect = websocketService.onReconnect(async () => {
      if (isTurnedOn) {
        try {
          const state = await convocationService.childGetVisibilityState();
          if (!state.viewingActive) {
            setIsTurnedOn(false);
            setMessages([]);
            setRemainingSeconds(0);
          } else {
            setRemainingSeconds(state.remainingSeconds);
          }
        } catch {
          // ignore
        }
      }
    });

    return () => {
      unsubStatus();
      unsubReconnect();
    };
  }, [isTurnedOn]);

  // Close options menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (optionsRef.current && !optionsRef.current.contains(e.target as Node)) {
        setOptionsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Countdown timer for active viewing session
  useEffect(() => {
    if (isTurnedOn && remainingSeconds > 0) {
      timerRef.current = setInterval(() => {
        setRemainingSeconds((prev) => {
          if (prev <= 1) {
            // Viewing expired
            setIsTurnedOn(false);
            setMessages([]);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isTurnedOn, remainingSeconds]);

  // Handle Turn On / Turn Off toggle
  const handleToggleVisibility = async () => {
    setOptionsOpen(false);
    if (!isTurnedOn) {
      // Turn ON: Activate 2-minute server viewing session
      setLoading(true);
      try {
        const session = await convocationService.childStartViewing();
        setMessages(session.messages || []);
        setRemainingSeconds(session.remainingSeconds || 120);
        setIsTurnedOn(true);
      } catch (err) {
        console.error('Failed to start child viewing session:', err);
        // If server fails or no unread, enable viewing with empty message list
        setIsTurnedOn(true);
        setMessages([]);
      } finally {
        setLoading(false);
      }
    } else {
      // Turn OFF: Immediately hide all messages, reset state
      setIsTurnedOn(false);
      setMessages([]);
      setRemainingSeconds(0);
    }
  };

  // Real-time synchronization
  useEffect(() => {
    const unsubMsg = websocketService.subscribe('/topic/convocation/messages', (incoming: any) => {
      // Strictly respect privacy: Only add/display message if Child has Turn On active
      if (isTurnedOnRef.current) {
        const isChildOriginated = Boolean(incoming.childOriginated ?? incoming.isChildOriginated);
        if (!isChildOriginated) {
          const newChildMsg = normalizeChildMessage(incoming);
          // Requirement 17: Once a Parent message has been viewed/seen,
          // it leaves the Child's new/unread visible set.
          // New Parent messages are treated as a new unread set.
          // Child's visible set becomes the new unread set [newChildMsg], NOT [oldMsg, newChildMsg].
          setMessages([newChildMsg]);
        }
      }
    });

    const unsubAction = websocketService.subscribe('/topic/convocation/actions', (actionData: any) => {
      if (actionData && actionData.action === 'UNSEND') {
        const targetId = Number(actionData.messageId);
        setMessages((prev) => prev.filter((m) => m.id !== targetId));
      }
    });

    return () => {
      unsubMsg();
      unsubAction();
    };
  }, [isTurnedOn]);

  // Send Child Note to Parent (disappears immediately from Child UI)
  const handleSendNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || sending) return;

    setSending(true);
    try {
      await convocationService.childSendMessage(inputText.trim());
      setInputText('');
      setNoteSentFeedback(true);
      setTimeout(() => setNoteSentFeedback(false), 2500);
    } catch (err) {
      console.error('Failed to send child note:', err);
    } finally {
      setSending(false);
      requestAnimationFrame(() => {
        inputRef.current?.focus();
      });
    }
  };

  const formatTimer = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', height: 'calc(100vh - 120px)' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#fff' }}>Convocation</h1>
            {/* Live Indicator */}
            <span
              id="child-convocation-live-indicator"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
                fontSize: '0.75rem',
                padding: '0.2rem 0.6rem',
                borderRadius: '999px',
                background: wsStatus === 'CONNECTED' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                color: wsStatus === 'CONNECTED' ? '#22c55e' : '#ef4444',
                fontWeight: 600,
              }}
            >
              <Radio size={12} className={wsStatus === 'CONNECTED' ? 'pulse-live' : ''} />
              {wsStatus === 'CONNECTED' ? 'LIVE' : 'OFFLINE'}
            </span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
            Priority family communication and temporary guidance notes
          </p>
        </div>

        {/* Top-Right Options Menu */}
        <div style={{ position: 'relative' }} ref={optionsRef}>
          <button
            type="button"
            id="btn-child-convocation-options"
            className="btn btn-secondary btn-sm"
            onClick={() => setOptionsOpen(!optionsOpen)}
            style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', padding: '0.45rem 0.75rem' }}
            aria-label="Options"
          >
            <span>Options</span>
            <MoreVertical size={15} />
          </button>

          {optionsOpen && (
            <div
              id="child-convocation-options-menu"
              className="glass-panel"
              style={{
                position: 'absolute',
                right: 0,
                top: '110%',
                zIndex: 100,
                minWidth: '160px',
                padding: '0.4rem',
                borderRadius: '10px',
                background: 'rgba(15, 23, 42, 0.95)',
                border: '1px solid var(--border-subtle)',
                boxShadow: '0 8px 24px rgba(0, 0, 0, 0.4)',
              }}
            >
              {/* EXACT REQUIREMENT: Exactly ONE single toggle control. Label changes between Turn on / Turn off */}
              <button
                type="button"
                id="btn-toggle-convocation-visibility"
                onClick={handleToggleVisibility}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.65rem 0.85rem',
                  background: 'transparent',
                  border: 'none',
                  color: isTurnedOn ? '#ef4444' : '#22c55e',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  borderRadius: '6px',
                  textAlign: 'left',
                }}
              >
                <span>{isTurnedOn ? 'Turn off' : 'Turn on'}</span>
                <span
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    border: '2px solid currentColor',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '2px',
                  }}
                >
                  {isTurnedOn ? (
                    <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'currentColor' }} />
                  ) : null}
                </span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Main Viewport */}
      <div
        className="glass-panel"
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          borderRadius: 'var(--radius-lg)',
          overflow: 'hidden',
          border: '1px solid var(--border-subtle)',
        }}
      >
        {/* Active Viewing Session Banner */}
        {isTurnedOn && (
          <div
            id="child-viewing-timer-banner"
            style={{
              padding: '0.65rem 1.25rem',
              background: 'rgba(99, 102, 241, 0.15)',
              borderBottom: '1px solid rgba(99, 102, 241, 0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.85rem',
              color: '#818cf8',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Clock size={15} />
              <span>Viewing session active</span>
            </div>
            <div id="child-viewing-timer" style={{ fontWeight: 700, fontFamily: 'monospace', fontSize: '0.95rem' }}>
              {formatTimer(remainingSeconds)}
            </div>
          </div>
        )}

        {/* Content Area */}
        <div
          id="child-convocation-content"
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '1.5rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          {!isTurnedOn ? (
            /* DEFAULT OFF & EMPTY VIEW */
            <div
              id="child-convocation-empty"
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                color: 'var(--text-dim)',
                padding: '2rem',
              }}
            >
              <div
                style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '50%',
                  background: 'rgba(255, 255, 255, 0.04)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1rem',
                }}
              >
                <MessageSquareQuote size={28} style={{ opacity: 0.5 }} />
              </div>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff', marginBottom: '0.35rem' }}>
                Note Pad
              </h3>
              <p style={{ maxWidth: '380px', fontSize: '0.875rem', lineHeight: 1.5 }}>
                Turn on in Options to view priority parent messages, or send a quick note to family below.
              </p>
            </div>
          ) : messages.length === 0 ? (
            <div
              id="child-convocation-no-messages"
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                color: 'var(--text-dim)',
              }}
            >
              <p style={{ fontSize: '0.9rem' }}>No unread priority messages from family at this moment.</p>
            </div>
          ) : (
            <div id="child-convocation-messages" style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              {messages.map((m) => (
                <div
                  key={m.id}
                  id={`child-msg-${m.id}`}
                  className="child-convocation-bubble"
                  style={{
                    alignSelf: 'flex-start',
                    maxWidth: '80%',
                    background: 'rgba(255, 255, 255, 0.08)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: '16px 16px 16px 4px',
                    padding: '0.85rem 1.15rem',
                    color: '#fff',
                    fontSize: '0.925rem',
                    lineHeight: 1.5,
                  }}
                >
                  <div>{m.message}</div>
                  <div
                    style={{
                      fontSize: '0.7rem',
                      color: 'var(--text-dim)',
                      marginTop: '0.35rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                    }}
                  >
                    <Clock size={11} />
                    <span>{new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Note-Pad Composer Footer */}
        <form
          id="form-child-convocation-note"
          onSubmit={handleSendNote}
          style={{
            padding: '1rem 1.5rem',
            background: 'rgba(15, 23, 42, 0.6)',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            gap: '0.75rem',
            alignItems: 'center',
            position: 'relative',
          }}
        >
          {noteSentFeedback && (
            <div
              id="child-note-sent-banner"
              style={{
                position: 'absolute',
                top: '-32px',
                left: '1.5rem',
                background: '#22c55e',
                color: '#fff',
                fontSize: '0.75rem',
                fontWeight: 600,
                padding: '0.2rem 0.6rem',
                borderRadius: '4px',
                display: 'flex',
                alignItems: 'center',
                gap: '0.3rem',
              }}
            >
              <CheckCircle size={12} />
              <span>Note delivered to family</span>
            </div>
          )}

          <input
            ref={inputRef}
            type="text"
            id="input-child-note"
            className="form-input"
            style={{ flex: 1, padding: '0.7rem 1.15rem' }}
            placeholder="Type a note to parent..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={sending}
          />
          <button
            type="submit"
            id="btn-send-child-note"
            className="btn btn-primary"
            disabled={sending || !inputText.trim()}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', padding: '0.7rem 1.25rem' }}
          >
            <Send size={16} />
            {sending ? 'Sending...' : 'Send Note'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ChildConvocationPage;
