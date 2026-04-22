import type { ChatMessage } from '../../types';
import ArticleCard from './ArticleCard';
import ResolutionCheck from './ResolutionCheck';
import EscalationNotice from './EscalationNotice';

interface Props {
  message: ChatMessage;
  onResolution?: (helpful: boolean) => void;
}

export default function MessageBubble({ message, onResolution }: Props) {
  const isUser = message.role === 'user';
  const data = message.additional_data ?? {};

  const articles = Array.isArray(data.articles) ? data.articles as { title: string; snippet: string; url: string }[] : [];
  const showResolutionCheck = data.ask_resolution === true;
  const caseNumber = typeof data.case_number === 'string' ? data.case_number : undefined;

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: 8,
      }}
    >
      <div
        style={{
          maxWidth: '80%',
          display: 'flex',
          flexDirection: 'column',
          gap: 6,
        }}
      >
        <div
          style={{
            padding: '10px 14px',
            borderRadius: 'var(--radius)',
            borderBottomRightRadius: isUser ? 0 : 'var(--radius)',
            borderBottomLeftRadius: isUser ? 'var(--radius)' : 0,
            background: isUser ? 'var(--color-bg-secondary)' : 'var(--color-primary)',
            color: isUser ? 'var(--color-text)' : '#fff',
            fontSize: '0.9rem',
            lineHeight: 1.5,
            whiteSpace: 'pre-wrap',
          }}
        >
          {message.text}
        </div>

        {articles.map((article, idx) => (
          <ArticleCard key={idx} title={article.title} snippet={article.snippet} url={article.url} />
        ))}

        {showResolutionCheck && onResolution && (
          <ResolutionCheck onResponse={onResolution} />
        )}

        {caseNumber && (
          <EscalationNotice caseNumber={caseNumber} />
        )}

        <span
          style={{
            fontSize: '0.7rem',
            color: 'var(--color-text-muted)',
            alignSelf: isUser ? 'flex-end' : 'flex-start',
          }}
        >
          {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </span>
      </div>
    </div>
  );
}
