import React, { useState, useRef, useEffect } from 'react';
import { streamBookSuggestions, BookCandidate, BookRef, SuggestionMode, addBookToReadingList } from '../../api/client';
import { ChatMessage } from './ChatMessage';
import { BookSuggestionCard } from './BookSuggestionCard';
import { SendIcon } from '../../components/icons';
import { useToast } from '../../components/ToastProvider';
import './ChatPage.css';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  suggestions?: BookCandidate[];
  /** Which mode this reply was requested in (assistant messages only). */
  mode?: SuggestionMode;
  /** True once the server has finished streaming this reply. */
  done?: boolean;
}

/** Server-side cap on how many earlier suggestions one request may carry. */
const MAX_ALREADY_SUGGESTED = 200;

/** Every book proposed so far in this conversation, most recent last. */
const collectAlreadySuggested = (messages: Message[]): BookRef[] =>
  messages
    .flatMap((m) => m.suggestions ?? [])
    .map(({ title, author, isbn }) => (isbn ? { title, author, isbn } : { title, author }))
    .slice(-MAX_ALREADY_SUGGESTED);

const MODE_COPY: Record<SuggestionMode, { label: string; placeholder: string; welcome: string; examples: string[] }> = {
  DISCOVER: {
    label: 'Find something new',
    placeholder: 'What kind of book are you looking for?',
    welcome:
      "Tell me what you're in the mood to read, and I'll suggest personalized book recommendations based on your collection and preferences.",
    examples: [
      'I want something light and fun to read',
      'Recommend me a thriller that will keep me up at night',
      "I'm looking for a thought-provoking non-fiction book",
    ],
  },
  BACKLOG: {
    label: 'From my Reading List',
    placeholder: 'What are you in the mood for tonight?',
    welcome:
      "Can't decide what to read next? Tell me your mood or how much time you have, and I'll pick the best fit from the books already on your Reading List.",
    examples: [
      'Something short I can finish this weekend',
      "I'm in the mood for something light and funny",
      'A gripping page-turner for a long flight',
    ],
  },
};

const ChatPage: React.FC = () => {
  const [mode, setMode] = useState<SuggestionMode>('DISCOVER');
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [bookStatus, setBookStatus] = useState<Record<string, 'added'>>({});
  const { showToast } = useToast();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!input.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input.trim(),
    };

    const requestMode = mode;
    const alreadySuggested = requestMode === 'DISCOVER' ? collectAlreadySuggested(messages) : [];

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);
    setError(null);

    const assistantId = (Date.now() + 1).toString();

    const upsertAssistant = (update: (m: Message) => Message) => {
      setMessages((prev) => {
        const exists = prev.some((m) => m.id === assistantId);
        if (!exists) {
          return [...prev, update({ id: assistantId, role: 'assistant', content: '', suggestions: [], mode: requestMode })];
        }
        return prev.map((m) => (m.id === assistantId ? update(m) : m));
      });
    };

    try {
      await streamBookSuggestions(userMessage.content, {
        alreadySuggested,
        mode: requestMode,
        onMessage: (content) => upsertAssistant((m) => ({ ...m, content })),
        onSuggestion: (book) =>
          upsertAssistant((m) => ({ ...m, suggestions: [...(m.suggestions ?? []), book] })),
      });
      upsertAssistant((m) => ({ ...m, done: true }));
    } catch (err) {
      setError('Failed to get suggestions. Please try again.');
      console.error('Error getting suggestions:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const doneStatus = (message: Message): string => {
    const count = message.suggestions?.length ?? 0;
    if (message.mode === 'BACKLOG') {
      return count > 0
        ? `That's my ${count === 1 ? 'pick' : 'picks'} from your Reading List. Tell me a different mood or time for another round.`
        : 'No picks this time.';
    }
    return count > 0
      ? `That's everything — ${count} ${count === 1 ? 'recommendation' : 'recommendations'} in total. Ask for more or refine your request anytime.`
      : 'No new recommendations this time. Try rephrasing or broadening your request.';
  };

  const bookKey = (book: BookCandidate) => book.isbn || `${book.title}|${book.author}`;

  const handleAddToReadingList = async (book: BookCandidate) => {
    const key = bookKey(book);
    try {
      await addBookToReadingList({
        title: book.title,
        author: book.author,
        isbn: book.isbn || undefined,
        coverUrl: book.coverUrl || undefined,
        description: book.description,
      });
      setBookStatus((prev) => ({ ...prev, [key]: 'added' }));
      showToast(`"${book.title}" added to your reading list!`);
    } catch (err) {
      showToast('Failed to add book. It may already be in your lists.', { variant: 'error' });
      console.error('Error adding book:', err);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div className="chat-page">
      <div className="chat-page__container">
        <div className="chat-page__messages">
          {messages.length === 0 && (
            <div className="chat-page__welcome">
              <h2 className="chat-page__welcome-title">Find Your Next Book</h2>
              <p className="chat-page__welcome-text">{MODE_COPY[mode].welcome}</p>
              <div className="chat-page__examples">
                <p className="chat-page__examples-title">Try asking:</p>
                {MODE_COPY[mode].examples.map((example) => (
                  <button
                    key={example}
                    className="chat-page__example-btn"
                    onClick={() => setInput(example)}
                  >
                    "{example}"
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((message) => (
            <ChatMessage key={message.id} role={message.role} content={
              <>
                <p>{message.content}</p>
                {message.suggestions && message.suggestions.length > 0 && (
                  <div className="chat-page__suggestions">
                    {message.suggestions.map((book, index) => (
                      <BookSuggestionCard
                        key={`${bookKey(book)}-${index}`}
                        book={book}
                        added={bookStatus[bookKey(book)] === 'added'}
                        onAddToReadingList={message.mode === 'BACKLOG' ? undefined : handleAddToReadingList}
                        whyLabel={message.mode === 'BACKLOG' ? 'Why now:' : undefined}
                      />
                    ))}
                  </div>
                )}
                {message.role === 'assistant' && (
                  message.done ? (
                    <p className="chat-page__status chat-page__status--done" role="status">
                      {doneStatus(message)}
                    </p>
                  ) : (
                    isLoading && message.id === messages[messages.length - 1]?.id && (
                      <p className="chat-page__status" role="status">
                        {message.mode === 'BACKLOG' ? 'Looking through your Reading List…' : 'Finding more books…'}
                      </p>
                    )
                  )
                )}
              </>
            } />
          ))}

          {isLoading && messages[messages.length - 1]?.role === 'user' && (
            <ChatMessage role="assistant" content={
              <div className="chat-page__loading">
                <span className="chat-page__loading-dot"></span>
                <span className="chat-page__loading-dot"></span>
                <span className="chat-page__loading-dot"></span>
              </div>
            } />
          )}

          {error && (
            <div className="chat-page__error">
              {error}
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="chat-page__input-container">
          <div className="chat-page__modes" role="group" aria-label="Recommendation mode">
            {(Object.keys(MODE_COPY) as SuggestionMode[]).map((m) => (
              <button
                key={m}
                type="button"
                className={`chat-page__mode-btn${mode === m ? ' chat-page__mode-btn--active' : ''}`}
                aria-pressed={mode === m}
                disabled={isLoading}
                onClick={() => setMode(m)}
              >
                {MODE_COPY[m].label}
              </button>
            ))}
          </div>
          <form onSubmit={handleSubmit} className="chat-page__input-form">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={MODE_COPY[mode].placeholder}
              className="chat-page__input"
              rows={1}
              disabled={isLoading}
            />
            <button
              type="submit"
              className="chat-page__send-btn"
              disabled={!input.trim() || isLoading}
              aria-label="Send message"
            >
              <SendIcon size={20} />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ChatPage;
