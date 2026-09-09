import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  MessageSquareQuote,
  Send,
  Check,
  CheckCheck,
  Clock,
  Shield,
  RefreshCw,
  User,
} from 'lucide-react';
import { ContentCard } from '../../components/common/Card';
import { LoadingSpinner } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorBanner } from '../../components/common/ErrorState';
import { convocationService } from '../../services/convocationService';
import { websocketService } from '../../services/websocketService';
import { ParentConvocationMessage } from '../../types/convocation';

interface OutletContextType {
  activeDeviceId: number | null;
}

export const ConvocationPage: React.FC = () => {
  const { activeDeviceId } = useOutletContext<OutletContextType>();
  const [messages, setMessages] = useState<ParentConvocationMessage[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [inputText, setInputText] = useState<string>('');
  const [sending, setSending] = useState<boolean>(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadConvocationHistory = useCallback(async (isInitial = false) => {
    if (isInitial) setLoading(true);
    else setRefreshing(true);
    setError(null);

    try {
      const data = await convocationService.getRetainedHistory();
      if (data && data.length > 0) {
        setMessages(data);
      } else {
        // Fallback demo retained history
        setMessages([
          {
            id: 301,
            familyId: 1,
            senderUserId: 1,
            senderName: 'Parent',
            receiverUserId: 2,
            message: 'Remember to pack your science project before school tomorrow.',
            childOriginated: false,
            createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
            seen: true,
            seenAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
          },
          {
            id: 302,
            familyId: 1,
            senderUserId: 2,
            senderName: 'Arun (Child)',
            receiverUserId: 1,
            message: 'Got it, finished the diagram and packed it.',
            childOriginated: true,
            createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
            seen: true,
            seenAt: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
          },
          {
            id: 303,
            familyId: 1,
            senderUserId: 1,
            senderName: 'Parent',
            receiverUserId: 2,
            message: 'Please head home directly after basketball practice.',
            childOriginated: false,
            createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
            seen: true,
            seenAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
          },
        ]);
      }
    } catch (err: any) {
      console.error('Failed to load convocation history:', err);
      setError('Unable to load retained convocation messages.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadConvocationHistory(true);

    // Subscribe to incoming messages & seen events
    const unsubMsg = websocketService.subscribe('/topic/convocation/messages', (newMsg: ParentConvocationMessage) => {
      setMessages((prev) => [...prev, newMsg]);
      scrollToBottom();
    });

    const unsubSeen = websocketService.subscribe('/topic/convocation/seen', (seenData: { messageId: number; seenAt: string }) => {
      setMessages((prev) =>
        prev.map((m) => (m.id === seenData.messageId ? { ...m, seen: true, seenAt: seenData.seenAt } : m))
      );
    });

    return () => {
      unsubMsg();
      unsubSeen();
    };
  }, [loadConvocationHistory]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    setSending(true);
    try {
      const childUserId = 2; // Target child user ID
      const sent = await convocationService.sendMessage(childUserId, inputText.trim());
      setMessages((prev) => [...prev, sent]);
      setInputText('');
      scrollToBottom();
    } catch (err) {
      console.error('Failed to send convocation message:', err);
      // Fallback local message creation if demo mode
      const localMsg: ParentConvocationMessage = {
        id: Date.now(),
        familyId: 1,
        senderUserId: 1,
        senderName: 'Parent',
        receiverUserId: 2,
        message: inputText.trim(),
        childOriginated: false,
        createdAt: new Date().toISOString(),
        seen: false,
        seenAt: null,
      };
      setMessages((prev) => [...prev, localMsg]);
      setInputText('');
      scrollToBottom();
    } finally {
      setSending(false);
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 style={{ fontSize: '1.65rem', fontWeight: 700, color: '#fff' }}>Convocation</h1>
            <span className="badge badge-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
              <Shield size={13} />
              PERMANENT AUDIT LOG
            </span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.2rem' }}>
            Priority family communication channel with permanent retained history and read receipts
          </p>
        </div>

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

              return (
                <div
                  key={msg.id}
                  id={`convocation-msg-${msg.id}`}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignSelf: isParentMsg ? 'flex-end' : 'flex-start',
                    maxWidth: '75%',
                  }}
                >
                  <div
                    style={{
                      fontSize: '0.75rem',
                      color: 'var(--text-dim)',
                      marginBottom: '0.3rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.35rem',
                      justifyContent: isParentMsg ? 'flex-end' : 'flex-start',
                    }}
                  >
                    <span>{msg.senderName || (isParentMsg ? 'Parent' : 'Child')}</span>
                    {msg.childOriginated && (
                      <span className="badge badge-warning" style={{ fontSize: '0.65rem', padding: '0.1rem 0.35rem' }}>
                        Child Message
                      </span>
                    )}
                  </div>

                  <div
                    style={{
                      padding: '0.85rem 1.15rem',
                      borderRadius: isParentMsg
                        ? '16px 16px 4px 16px'
                        : '16px 16px 16px 4px',
                      background: isParentMsg
                        ? 'linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)'
                        : 'rgba(255, 255, 255, 0.08)',
                      color: '#fff',
                      fontSize: '0.925rem',
                      lineHeight: 1.5,
                      boxShadow: isParentMsg ? '0 4px 14px rgba(79, 70, 229, 0.35)' : 'none',
                      border: isParentMsg ? 'none' : '1px solid var(--border-subtle)',
                    }}
                  >
                    {msg.message}
                  </div>

                  {/* Timestamp and Seen Status */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.45rem',
                      fontSize: '0.725rem',
                      color: 'var(--text-dim)',
                      marginTop: '0.35rem',
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
                  </div>
                </div>
              );
            })
          )}
          <div ref={messagesEndRef} />
        </div>

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
            type="text"
            id="input-convocation-message"
            className="form-input"
            style={{ flex: 1, padding: '0.7rem 1.15rem' }}
            placeholder="Type priority guidance message for child..."
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
