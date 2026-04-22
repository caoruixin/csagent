import { useState, useRef, useEffect, useCallback } from 'react';
import type { ChatMessage, ChatPhase, CreateSessionRequest } from '../../types';
import { createSession, sendMessage } from '../../api/client';
import PreChatForm from './PreChatForm';
import MessageBubble from './MessageBubble';
import ChatInput from './ChatInput';
import TypingIndicator from './TypingIndicator';

let msgId = 0;
function nextId() {
  return `msg-${++msgId}`;
}

export default function ChatWidget() {
  const [phase, setPhase] = useState<ChatPhase>('CLOSED');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading, scrollToBottom]);

  const handlePreChat = async (data: CreateSessionRequest) => {
    setLoading(true);
    setError(null);
    try {
      const res = await createSession(data);
      setSessionId(res.session_id);
      setMessages([
        {
          id: nextId(),
          role: 'bot',
          text: res.reply_text,
          timestamp: new Date(),
        },
      ]);
      setPhase('CHATTING');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to start session';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSend = async (text: string) => {
    if (!sessionId) return;

    const userMsg: ChatMessage = {
      id: nextId(),
      role: 'user',
      text,
      timestamp: new Date(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);
    setError(null);

    try {
      const res = await sendMessage(sessionId, { message: text });
      const botMsg: ChatMessage = {
        id: nextId(),
        role: 'bot',
        text: res.reply_text,
        timestamp: new Date(),
        additional_data: res.additional_data,
        intent: res.intent,
        should_end_chat: res.should_end_chat,
      };
      setMessages((prev) => [...prev, botMsg]);

      if (res.should_end_chat) {
        setPhase('ENDED');
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to send message';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleResolution = async (helpful: boolean) => {
    if (!sessionId) return;
    const text = helpful ? 'Yes, that helped!' : 'No, I still need help.';
    await handleSend(text);
  };

  const handleToggle = () => {
    setPhase((prev) => (prev === 'CLOSED' ? 'PRE_CHAT' : 'CLOSED'));
  };

  const handleRestart = () => {
    setPhase('PRE_CHAT');
    setSessionId(null);
    setMessages([]);
    setError(null);
  };

  return (
    <>
      {/* Floating button */}
      <button
        onClick={handleToggle}
        aria-label={phase === 'CLOSED' ? 'Open chat' : 'Close chat'}
        style={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          width: 56,
          height: 56,
          borderRadius: '50%',
          background: 'var(--color-cta)',
          color: '#000',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--shadow)',
          zIndex: 1000,
          transition: 'transform 0.2s',
          fontSize: '1.4rem',
        }}
        onMouseEnter={(e) => { (e.currentTarget as HTMLElement).style.transform = 'scale(1.08)'; }}
        onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.transform = 'scale(1)'; }}
      >
        {phase === 'CLOSED' ? (
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        ) : (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        )}
      </button>

      {/* Chat panel */}
      {phase !== 'CLOSED' && (
        <div
          style={{
            position: 'fixed',
            bottom: 96,
            right: 24,
            width: 400,
            maxHeight: 'calc(100vh - 160px)',
            background: '#fff',
            borderRadius: 'var(--radius)',
            boxShadow: 'var(--shadow)',
            display: 'flex',
            flexDirection: 'column',
            zIndex: 999,
            overflow: 'hidden',
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: '14px 16px',
              background: 'var(--color-primary)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexShrink: 0,
            }}
          >
            <div>
              <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>Gumtree Support</div>
              <div style={{ fontSize: '0.75rem', opacity: 0.8 }}>We typically reply instantly</div>
            </div>
            {sessionId && (
              <span
                style={{
                  fontSize: '0.65rem',
                  opacity: 0.6,
                  fontFamily: 'monospace',
                }}
              >
                {sessionId.slice(0, 8)}
              </span>
            )}
          </div>

          {/* Body */}
          {phase === 'PRE_CHAT' && (
            <PreChatForm onSubmit={handlePreChat} loading={loading} error={error} />
          )}

          {(phase === 'CHATTING' || phase === 'ENDED') && (
            <>
              <div
                style={{
                  flex: 1,
                  overflowY: 'auto',
                  padding: '16px 12px',
                  display: 'flex',
                  flexDirection: 'column',
                  minHeight: 300,
                }}
              >
                {messages.map((msg) => (
                  <MessageBubble
                    key={msg.id}
                    message={msg}
                    onResolution={handleResolution}
                  />
                ))}
                {loading && <TypingIndicator />}

                {error && (
                  <div
                    style={{
                      background: '#FEF2F2',
                      color: '#B91C1C',
                      padding: '8px 12px',
                      borderRadius: 'var(--radius)',
                      fontSize: '0.8rem',
                      marginTop: 8,
                    }}
                  >
                    {error}
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {phase === 'CHATTING' ? (
                <ChatInput onSend={handleSend} disabled={loading} />
              ) : (
                <div
                  style={{
                    padding: '16px',
                    borderTop: '1px solid var(--color-border)',
                    textAlign: 'center',
                  }}
                >
                  <p style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: 12 }}>
                    Chat session ended.
                  </p>
                  <button className="btn btn-cta" onClick={handleRestart} style={{ padding: '10px 24px' }}>
                    Start New Chat
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </>
  );
}
