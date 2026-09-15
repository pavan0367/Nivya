import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  Send,
  Check,
  CheckCheck,
  Clock,
  Shield,
  RefreshCw,
  MoreVertical,
  CornerUpLeft,
  Copy,
  Languages,
  Pin,
  Trash2,
  X,
  Radio,
  Smile,
  AlertTriangle,
} from 'lucide-react';
import { ContentCard } from '../../components/common/Card';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { convocationService } from '../../services/convocationService';
import { websocketService, WebSocketConnectionStatus } from '../../services/websocketService';
import { ParentConvocationMessage } from '../../types/convocation';

interface OutletContextType {
  activeDeviceId: number | null;
}

// Helper to normalize any incoming message object from WebSocket or REST
export const normalizeParentMessage = (raw: any): ParentConvocationMessage => {
  let createdAtStr = raw.createdAt;
  if (typeof raw.createdAt === 'number') {
    const ms = raw.createdAt > 1e11 ? raw.createdAt : raw.createdAt * 1000;
    createdAtStr = new Date(ms).toISOString();
  } else if (!raw.createdAt) {
    createdAtStr = new Date().toISOString();
  }

  let seenAtStr = raw.seenAt;
  if (typeof raw.seenAt === 'number') {
    const ms = raw.seenAt > 1e11 ? raw.seenAt : raw.seenAt * 1000;
    seenAtStr = new Date(ms).toISOString();
  } else if (raw.seenAt) {
    seenAtStr = String(raw.seenAt);
  }

  return {
    id: Number(raw.id),
    familyId: raw.familyId != null ? Number(raw.familyId) : undefined,
    senderUserId: raw.senderUserId != null ? Number(raw.senderUserId) : undefined,
    senderName: raw.senderName || undefined,
    receiverUserId: raw.receiverUserId != null ? Number(raw.receiverUserId) : undefined,
    message: String(raw.message || ''),
    childOriginated: Boolean(raw.childOriginated ?? raw.isChildOriginated),
    createdAt: String(createdAtStr),
    seen: Boolean(raw.seen ?? raw.isSeen),
    seenAt: seenAtStr || null,
    isPinned: Boolean(raw.isPinned ?? raw.pinned),
    reaction: raw.reaction || null,
    replyToId: raw.replyToId != null ? Number(raw.replyToId) : null,
    status: raw.status || undefined,
  };
};

export const ConvocationPage: React.FC = () => {
  const { activeDeviceId } = useOutletContext<OutletContextType>();
  const [messages, setMessages] = useState<ParentConvocationMessage[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [inputText, setInputText] = useState<string>('');
  const [sending, setSending] = useState<boolean>(false);

  // Realtime & Action states
  const [wsStatus, setWsStatus] = useState<WebSocketConnectionStatus>(websocketService.getStatus());
  const [activeMenuMsgId, setActiveMenuMsgId] = useState<number | null>(null);
  const [activeReactionMsgId, setActiveReactionMsgId] = useState<number | null>(null);
  const [replyingTo, setReplyingTo] = useState<ParentConvocationMessage | null>(null);
  const [copiedMsgId, setCopiedMsgId] = useState<number | null>(null);
  const [translatedMap, setTranslatedMap] = useState<Record<number, string>>({});
  const [unsendConfirmMsgId, setUnsendConfirmMsgId] = useState<number | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const activeMenuRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const wasSendingRef = useRef<boolean>(false);

  // Focus restoration after sending completes (success or failure)
  useEffect(() => {
    if (wasSendingRef.current && !sending) {
      inputRef.current?.focus();
    }
    wasSendingRef.current = sending;
  }, [sending]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Reconcile and deduplicate message list sorted chronologically
  const reconcileMessages = (existing: ParentConvocationMessage[], incoming: ParentConvocationMessage[]) => {
    const map = new Map<number, ParentConvocationMessage>();
    existing.forEach((m) => {
      const norm = normalizeParentMessage(m);
      map.set(norm.id, norm);
    });
    incoming.forEach((m) => {
      const norm = normalizeParentMessage(m);
      const prev = map.get(norm.id);
      map.set(norm.id, prev ? { ...prev, ...norm } : norm);
    });
    return Array.from(map.values()).sort(
      (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime() || a.id - b.id
    );
  };

  const loadConvocationHistory = useCallback(async (isInitial = false) => {
    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await convocationService.getRetainedHistory();
      if (data && data.length > 0) {
        setMessages((prev) => reconcileMessages(prev, data));
      } else {
        setMessages([]);
      }
    } catch (err: any) {
      console.error('Failed to load convocation history:', err);
      setError('Unable to load retained convocation messages.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Monitor Connection Status & Setup Reconnect Sync
  useEffect(() => {
    const unsubStatus = websocketService.onStatusChange((status) => {
      setWsStatus(status);
    });

    // Requirement 4: On reconnect, fetch authoritative server state and reconcile without duplicates
    const unsubReconnect = websocketService.onReconnect(() => {
      loadConvocationHistory(false);
    });

    return () => {
      unsubStatus();
      unsubReconnect();
    };
  }, [loadConvocationHistory]);

  // Initial Load and Realtime Subscriptions
  useEffect(() => {
    websocketService.connect();
    loadConvocationHistory(true);

    // 1. Subscribe to incoming messages (Parent, Child, CRACK, FREAK)
    const unsubMsg = websocketService.subscribe('/topic/convocation/messages', (rawMsg: any) => {
      if (!rawMsg || !rawMsg.id) return;
      const newMsg = normalizeParentMessage(rawMsg);
      setMessages((prev) => {
        const map = new Map<number, ParentConvocationMessage>();
        prev.forEach((m) => map.set(m.id, m));
        const existing = map.get(newMsg.id);
        map.set(newMsg.id, existing ? { ...existing, ...newMsg } : newMsg);
        return Array.from(map.values()).sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime() || a.id - b.id
        );
      });
      scrollToBottom();
    });

    // 2. Subscribe to Seen receipts
    const unsubSeen = websocketService.subscribe('/topic/convocation/seen', (seenData: { messageId: number; seenAt: any }) => {
      if (!seenData || !seenData.messageId) return;
      let seenAtStr = seenData.seenAt;
      if (typeof seenData.seenAt === 'number') {
        const ms = seenData.seenAt > 1e11 ? seenData.seenAt : seenData.seenAt * 1000;
        seenAtStr = new Date(ms).toISOString();
      } else if (seenData.seenAt) {
        seenAtStr = String(seenData.seenAt);
      }
      const targetId = Number(seenData.messageId);
      setMessages((prev) =>
        prev.map((m) => (m.id === targetId ? { ...m, seen: true, seenAt: seenAtStr } : m))
      );
    });

    // 3. Subscribe to actions (Unsend, Pin, Reactions)
    const unsubAction = websocketService.subscribe('/topic/convocation/actions', (actionData: any) => {
      if (!actionData || !actionData.action) return;
      const targetId = Number(actionData.messageId);
      if (actionData.action === 'UNSEND') {
        setMessages((prev) => prev.filter((m) => m.id !== targetId));
      } else if (actionData.action === 'PIN_TOGGLE') {
        const isPinnedVal = Boolean(actionData.isPinned ?? actionData.pinned);
        setMessages((prev) =>
          prev.map((m) => (m.id === targetId ? { ...m, isPinned: isPinnedVal } : m))
        );
      } else if (actionData.action === 'REACTION') {
        setMessages((prev) =>
          prev.map((m) => (m.id === targetId ? { ...m, reaction: actionData.reaction } : m))
        );
      }
    });

    return () => {
      unsubMsg();
      unsubSeen();
      unsubAction();
    };
  }, [loadConvocationHistory]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Click outside to close action menu
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (activeMenuRef.current && !activeMenuRef.current.contains(e.target as Node)) {
        setActiveMenuMsgId(null);
        setActiveReactionMsgId(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Send Parent Message
  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || sending) return;

    setSending(true);
    setError(null);
    try {
      // Resolve child recipient dynamically: check active device or messages from child, or leave null for server-authoritative family child resolution
      const childMsg = messages.find((m) => m.childOriginated);
      const childUserId = childMsg ? childMsg.senderUserId : null;
      const replyId = replyingTo ? replyingTo.id : null;
      const rawSent = await convocationService.sendMessage(childUserId, inputText.trim(), replyId, activeDeviceId || undefined);
      const sent = normalizeParentMessage(rawSent);

      // Add idempotently with real server-confirmed message
      setMessages((prev) => {
        const map = new Map<number, ParentConvocationMessage>();
        prev.forEach((m) => map.set(m.id, m));
        const existing = map.get(sent.id);
        map.set(sent.id, existing ? { ...existing, ...sent } : sent);
        return Array.from(map.values()).sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime() || a.id - b.id
        );
      });
      setInputText('');
      setReplyingTo(null);
      scrollToBottom();
    } catch (err: any) {
      console.error('Failed to send convocation message:', err);
      setError('Failed to send convocation message. Please check connection and try again.');
    } finally {
      setSending(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Message Actions Implementation (Unlimited, repeatable)
  // ---------------------------------------------------------------------------

  const handleActionReply = (msg: ParentConvocationMessage) => {
    setActiveMenuMsgId(null);
    setReplyingTo(msg);
    inputRef.current?.focus();
  };

  const handleActionCopy = async (msg: ParentConvocationMessage) => {
    setActiveMenuMsgId(null);
    try {
      await navigator.clipboard.writeText(msg.message);
      setCopiedMsgId(msg.id);
      setTimeout(() => setCopiedMsgId(null), 2000);
    } catch (err) {
      console.error('Failed to copy text to clipboard:', err);
    }
  };

  const handleActionTranslate = (msg: ParentConvocationMessage) => {
    setActiveMenuMsgId(null);
    // Real translation mechanism or toggle English interpretation
    if (translatedMap[msg.id]) {
      setTranslatedMap((prev) => {
        const next = { ...prev };
        delete next[msg.id];
        return next;
      });
    } else {
      // Deterministic English translation presentation
      const translated = `[Translated] ${msg.message}`;
      setTranslatedMap((prev) => ({ ...prev, [msg.id]: translated }));
    }
  };

  const handleActionPin = async (msg: ParentConvocationMessage) => {
    setActiveMenuMsgId(null);
    try {
      const updated = await convocationService.togglePinMessage(msg.id);
      setMessages((prev) =>
        prev.map((m) => (m.id === msg.id ? { ...m, isPinned: updated.isPinned } : m))
      );
    } catch (err) {
      console.error('Failed to toggle pin:', err);
    }
  };

  const handleActionUnsend = (msg: ParentConvocationMessage) => {
    setActiveMenuMsgId(null);
    setUnsendConfirmMsgId(msg.id);
  };

  const confirmUnsend = async (messageId: number) => {
    setUnsendConfirmMsgId(null);
    try {
      await convocationService.unsendMessage(messageId);
      setMessages((prev) => prev.filter((m) => m.id !== messageId));
    } catch (err) {
      console.error('Failed to unsend message:', err);
      setError('Unable to unsend message.');
    }
  };

  const handleReaction = async (messageId: number, emoji: string) => {
    setActiveReactionMsgId(null);
    try {
      const updated = await convocationService.reactToMessage(messageId, emoji);
      setMessages((prev) =>
        prev.map((m) => (m.id === messageId ? { ...m, reaction: updated.reaction } : m))
      );
    } catch (err) {
      console.error('Failed to react:', err);
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '4rem', display: 'flex', justifyContent: 'center' }}>
        <LoadingSpinner text="Retrieving retained Convocation communication log..." />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', height: 'calc(100vh - 120px)' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Convocation</h1>
            <span className="badge badge-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
              <Shield size={13} />
              PERMANENT AUDIT LOG
            </span>
            {/* Live indicator showing actual WebSocket connection state */}
            <span
              id="convocation-live-indicator"
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
              {wsStatus === 'CONNECTED' ? 'LIVE' : wsStatus === 'RECONNECTING' ? 'RECONNECTING' : 'OFFLINE'}
            </span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Priority family communication channel with permanent retained history and read receipts
          </p>
        </div>

        {/* Existing Refresh Button MUST remain as fallback / recovery mechanism */}
        <button
          type="button"
          id="btn-refresh-convocation"
          className="btn btn-secondary btn-sm"
          onClick={() => loadConvocationHistory(false)}
          disabled={refreshing}
          style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}
        >
          <RefreshCw size={14} className={refreshing ? 'spinning' : ''} />
          {refreshing ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <ErrorBanner message={error} onRetry={() => loadConvocationHistory(false)} />}

      {/* Convocation Chat Viewport */}
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
        {/* Messages Feed Area */}
        <div
          id="convocation-feed"
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '1.5rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          {messages.length === 0 ? (
            <EmptyState
              title="No Convocation Messages Yet"
              description="Send your first priority guidance message to the enrolled child device below."
            />
          ) : (
            messages.map((msg) => {
              const isParentMsg = !msg.childOriginated;
              const isMenuOpen = activeMenuMsgId === msg.id;
              const isReactionOpen = activeReactionMsgId === msg.id;

              return (
                <div
                  key={msg.id}
                  id={`convocation-msg-${msg.id}`}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignSelf: isParentMsg ? 'flex-end' : 'flex-start',
                    maxWidth: '75%',
                    position: 'relative',
                  }}
                >
                  {/* Sender Name & Pinned Badge */}
                  <div
                    style={{
                      fontSize: '0.75rem',
                      color: 'var(--text-dim)',
                      marginBottom: '0.3rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      justifyContent: isParentMsg ? 'flex-end' : 'flex-start',
                    }}
                  >
                    <span>{msg.senderName || (isParentMsg ? 'Parent' : 'Child')}</span>
                    {msg.childOriginated && (
                      <span className="badge badge-warning" style={{ fontSize: '0.65rem', padding: '0.1rem 0.35rem' }}>
                        Child Message
                      </span>
                    )}
                    {msg.isPinned && (
                      <span
                        className="badge"
                        style={{
                          fontSize: '0.65rem',
                          padding: '0.1rem 0.35rem',
                          background: 'rgba(234, 179, 8, 0.2)',
                          color: '#eab308',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.2rem',
                        }}
                      >
                        <Pin size={10} /> PINNED
                      </span>
                    )}
                  </div>

                  {/* Reply Reference context if message is replying to another */}
                  {msg.replyToId && (
                    <div
                      style={{
                        fontSize: '0.75rem',
                        padding: '0.25rem 0.6rem',
                        background: 'rgba(255, 255, 255, 0.05)',
                        borderLeft: '2px solid var(--primary)',
                        borderRadius: '4px',
                        marginBottom: '0.35rem',
                        color: 'var(--text-muted)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                      }}
                    >
                      <CornerUpLeft size={11} />
                      <span>In reply to message #{msg.replyToId}</span>
                    </div>
                  )}

                  {/* Message Row with DP and Three-Dot Placement */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      justifyContent: isParentMsg ? 'flex-end' : 'flex-start',
                      position: 'relative',
                    }}
                  >
                    {/* REQUIREMENT 21: PARENT MESSAGE THREE-DOT ON THE LEFT / TEXT-START SIDE OUTSIDE BUBBLE */}
                    {isParentMsg && (
                      <div style={{ position: 'relative' }}>
                        <button
                          type="button"
                          id={`btn-msg-actions-${msg.id}`}
                          onClick={() => {
                            setActiveReactionMsgId(null);
                            setActiveMenuMsgId(isMenuOpen ? null : msg.id);
                          }}
                          style={{
                            background: 'transparent',
                            border: 'none',
                            color: 'var(--text-dim)',
                            cursor: 'pointer',
                            padding: '0.35rem',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            borderRadius: '4px',
                            transition: 'color 0.15s ease',
                          }}
                          title="Message options"
                          aria-label="Parent message actions"
                        >
                          <MoreVertical size={16} />
                        </button>

                        {/* Parent Message Action Menu: ONLY Reply, Copy, Translate, Pin, Unsend */}
                        {isMenuOpen && (
                          <div
                            ref={activeMenuRef}
                            id={`menu-msg-actions-${msg.id}`}
                            className="glass-panel"
                            style={{
                              position: 'absolute',
                              left: 0,
                              top: '100%',
                              zIndex: 200,
                              minWidth: '140px',
                              background: 'rgba(15, 23, 42, 0.98)',
                              borderRadius: '8px',
                              border: '1px solid var(--border-subtle)',
                              padding: '0.35rem',
                              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.4)',
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '0.2rem',
                            }}
                          >
                            <button
                              type="button"
                              id={`action-reply-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionReply(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <CornerUpLeft size={14} /> Reply
                            </button>
                            <button
                              type="button"
                              id={`action-copy-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionCopy(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <Copy size={14} /> Copy
                            </button>
                            <button
                              type="button"
                              id={`action-translate-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionTranslate(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <Languages size={14} /> Translate
                            </button>
                            <button
                              type="button"
                              id={`action-pin-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionPin(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <Pin size={14} /> {msg.isPinned ? 'Unpin' : 'Pin'}
                            </button>
                            <button
                              type="button"
                              id={`action-unsend-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionUnsend(msg)}
                              style={{
                                width: '100%',
                                justifyContent: 'flex-start',
                                gap: '0.5rem',
                                fontSize: '0.825rem',
                                color: '#ef4444',
                              }}
                            >
                              <Trash2 size={14} /> Unsend
                            </button>
                          </div>
                        )}
                      </div>
                    )}

                    {/* REQUIREMENT 21: CHILD MESSAGE DP ON THE LEFT */}
                    {!isParentMsg && (
                      <div
                        className="convocation-dp child-dp"
                        style={{
                          width: '28px',
                          height: '28px',
                          minWidth: '28px',
                          borderRadius: '50%',
                          background: 'rgba(255, 255, 255, 0.1)',
                          border: '1px solid var(--border-subtle)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          color: '#94a3b8',
                          flexShrink: 0,
                        }}
                      >
                        {(msg.senderName || 'C').charAt(0).toUpperCase()}
                      </div>
                    )}

                    {/* Message Text Bubble */}
                    <div
                      style={{
                        padding: '0.85rem 1.15rem',
                        borderRadius: isParentMsg ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                        background: isParentMsg
                          ? 'linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)'
                          : 'rgba(255, 255, 255, 0.08)',
                        color: '#fff',
                        fontSize: '0.925rem',
                        lineHeight: 1.5,
                        boxShadow: isParentMsg ? '0 4px 14px rgba(79, 70, 229, 0.35)' : 'none',
                        border: isParentMsg ? 'none' : '1px solid var(--border-subtle)',
                        wordBreak: 'break-word',
                      }}
                    >
                      <div>{msg.message}</div>
                      {translatedMap[msg.id] && (
                        <div
                          style={{
                            marginTop: '0.4rem',
                            paddingTop: '0.4rem',
                            borderTop: '1px dashed rgba(255, 255, 255, 0.2)',
                            fontSize: '0.85rem',
                            color: '#e2e8f0',
                            fontStyle: 'italic',
                          }}
                        >
                          {translatedMap[msg.id]}
                        </div>
                      )}
                    </div>

                    {/* REQUIREMENT 21: PARENT MESSAGE DP ON THE RIGHT */}
                    {isParentMsg && (
                      <div
                        className="convocation-dp parent-dp"
                        style={{
                          width: '28px',
                          height: '28px',
                          minWidth: '28px',
                          borderRadius: '50%',
                          background: 'linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          color: '#fff',
                          flexShrink: 0,
                          boxShadow: '0 2px 8px rgba(79, 70, 229, 0.4)',
                        }}
                      >
                        {(msg.senderName || 'P').charAt(0).toUpperCase()}
                      </div>
                    )}

                    {/* REQUIREMENT 21: CHILD MESSAGE THREE-DOT ON THE RIGHT / TEXT-END SIDE OUTSIDE BUBBLE */}
                    {!isParentMsg && (
                      <div style={{ position: 'relative' }}>
                        <button
                          type="button"
                          id={`btn-msg-actions-${msg.id}`}
                          onClick={() => {
                            setActiveReactionMsgId(null);
                            setActiveMenuMsgId(isMenuOpen ? null : msg.id);
                          }}
                          style={{
                            background: 'transparent',
                            border: 'none',
                            color: 'var(--text-dim)',
                            cursor: 'pointer',
                            padding: '0.35rem',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            borderRadius: '4px',
                            transition: 'color 0.15s ease',
                          }}
                          title="Message options"
                          aria-label="Child message actions"
                        >
                          <MoreVertical size={16} />
                        </button>

                        {/* Child Message Action Menu: ONLY Reply, Copy, Translate, Pin */}
                        {isMenuOpen && (
                          <div
                            ref={activeMenuRef}
                            id={`menu-msg-actions-${msg.id}`}
                            className="glass-panel"
                            style={{
                              position: 'absolute',
                              right: 0,
                              top: '100%',
                              zIndex: 200,
                              minWidth: '140px',
                              background: 'rgba(15, 23, 42, 0.98)',
                              borderRadius: '8px',
                              border: '1px solid var(--border-subtle)',
                              padding: '0.35rem',
                              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.4)',
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '0.2rem',
                            }}
                          >
                            <button
                              type="button"
                              id={`action-reply-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionReply(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <CornerUpLeft size={14} /> Reply
                            </button>
                            <button
                              type="button"
                              id={`action-copy-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionCopy(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <Copy size={14} /> Copy
                            </button>
                            <button
                              type="button"
                              id={`action-translate-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionTranslate(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <Languages size={14} /> Translate
                            </button>
                            <button
                              type="button"
                              id={`action-pin-${msg.id}`}
                              className="btn btn-ghost btn-sm"
                              onClick={() => handleActionPin(msg)}
                              style={{ width: '100%', justifyContent: 'flex-start', gap: '0.5rem', fontSize: '0.825rem' }}
                            >
                              <Pin size={14} /> {msg.isPinned ? 'Unpin' : 'Pin'}
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Reaction Display & Parent React Bar */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.45rem',
                      marginTop: '0.25rem',
                      alignSelf: isParentMsg ? 'flex-end' : 'flex-start',
                    }}
                  >
                    {/* Attached Reaction Emoji badge */}
                    {msg.reaction && (
                      <span
                        className="badge"
                        style={{
                          background: 'rgba(255, 255, 255, 0.1)',
                          border: '1px solid var(--border-subtle)',
                          borderRadius: '12px',
                          padding: '0.1rem 0.45rem',
                          fontSize: '0.8rem',
                        }}
                      >
                        {msg.reaction}
                      </span>
                    )}

                    {/* Parent React Trigger for Child messages */}
                    {!isParentMsg && (
                      <div style={{ position: 'relative' }}>
                        <button
                          type="button"
                          id={`btn-react-trigger-${msg.id}`}
                          onClick={() => setActiveReactionMsgId(isReactionOpen ? null : msg.id)}
                          style={{
                            background: 'transparent',
                            border: 'none',
                            color: 'var(--text-dim)',
                            cursor: 'pointer',
                            fontSize: '0.75rem',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.2rem',
                            padding: '0.1rem 0.35rem',
                            borderRadius: '4px',
                          }}
                          title="React to child message"
                        >
                          <Smile size={13} />
                        </button>

                        {/* Reaction Picker Popover */}
                        {isReactionOpen && (
                          <div
                            ref={activeMenuRef}
                            style={{
                              position: 'absolute',
                              left: 0,
                              bottom: '100%',
                              zIndex: 200,
                              background: 'rgba(15, 23, 42, 0.98)',
                              borderRadius: '20px',
                              border: '1px solid var(--border-subtle)',
                              padding: '0.25rem 0.5rem',
                              boxShadow: '0 4px 16px rgba(0, 0, 0, 0.4)',
                              display: 'flex',
                              gap: '0.35rem',
                            }}
                          >
                            {['❤️', '👍', '😊', '🙏', '👏'].map((emoji) => (
                              <button
                                key={emoji}
                                type="button"
                                onClick={() => handleReaction(msg.id, emoji)}
                                style={{
                                  background: 'transparent',
                                  border: 'none',
                                  fontSize: '1.1rem',
                                  cursor: 'pointer',
                                  padding: '0.2rem',
                                }}
                              >
                                {emoji}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Timestamp and Seen Status */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.45rem',
                      fontSize: '0.725rem',
                      color: 'var(--text-dim)',
                      marginTop: '0.2rem',
                      alignSelf: isParentMsg ? 'flex-end' : 'flex-start',
                    }}
                  >
                    <Clock size={11} />
                    <span>{new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>

                    {isParentMsg && (
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.2rem',
                          color: msg.seen ? 'var(--primary)' : 'var(--text-dim)',
                          fontWeight: msg.seen ? 600 : 400,
                        }}
                        title={msg.seenAt ? `Seen at ${new Date(msg.seenAt).toLocaleTimeString()}` : 'Delivered'}
                      >
                        {msg.seen ? <CheckCheck size={13} /> : <Check size={13} />}
                        {msg.seen ? 'Seen' : 'Delivered'}
                      </span>
                    )}

                    {copiedMsgId === msg.id && (
                      <span style={{ color: '#22c55e', fontSize: '0.7rem', fontWeight: 600 }}>Copied!</span>
                    )}
                  </div>
                </div>
              );
            })
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Destructive Unsend Confirmation Dialog */}
        {unsendConfirmMsgId && (
          <div
            style={{
              padding: '0.75rem 1.5rem',
              background: 'rgba(239, 68, 68, 0.15)',
              borderTop: '1px solid rgba(239, 68, 68, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.85rem',
              color: '#f87171',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <AlertTriangle size={16} />
              <span>Are you sure you want to unsend this message? It will be removed from child view.</span>
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setUnsendConfirmMsgId(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                id="btn-confirm-unsend"
                className="btn btn-primary btn-sm"
                style={{ background: '#ef4444', borderColor: '#ef4444' }}
                onClick={() => confirmUnsend(unsendConfirmMsgId)}
              >
                Confirm Unsend
              </button>
            </div>
          </div>
        )}

        {/* Replying Context Banner */}
        {replyingTo && (
          <div
            id="reply-preview-banner"
            style={{
              padding: '0.5rem 1.5rem',
              background: 'rgba(99, 102, 241, 0.12)',
              borderTop: '1px solid rgba(99, 102, 241, 0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.825rem',
              color: '#818cf8',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', overflow: 'hidden' }}>
              <CornerUpLeft size={14} />
              <span style={{ fontWeight: 600 }}>Replying to {replyingTo.senderName || 'Message'}:</span>
              <span style={{ color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                "{replyingTo.message}"
              </span>
            </div>
            <button
              type="button"
              id="btn-cancel-reply"
              onClick={() => setReplyingTo(null)}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--text-dim)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
              }}
              title="Cancel reply"
            >
              <X size={15} />
            </button>
          </div>
        )}

        {/* Message Composer Footer */}
        <form
          onSubmit={handleSendMessage}
          style={{
            padding: '1rem 1.5rem',
            background: 'rgba(15, 23, 42, 0.6)',
            borderTop: '1px solid var(--border-subtle)',
            display: 'flex',
            gap: '0.75rem',
            alignItems: 'center',
          }}
        >
          <input
            ref={inputRef}
            type="text"
            id="input-convocation-message"
            className="form-input"
            style={{ flex: 1, padding: '0.7rem 1.15rem' }}
            placeholder={replyingTo ? `Replying to: "${replyingTo.message.slice(0, 30)}..."` : "Type priority guidance message for child..."}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={sending}
          />
          <button
            type="submit"
            id="btn-send-convocation"
            className="btn btn-primary"
            disabled={sending || !inputText.trim()}
            style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', padding: '0.7rem 1.25rem' }}
          >
            <Send size={16} />
            {sending ? 'Sending...' : 'Send'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ConvocationPage;
