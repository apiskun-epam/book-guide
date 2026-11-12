import React, { useState, useRef, useEffect } from 'react';
import { streamBookSuggestions, BookCandidate, addBookToCollection } from '../../api/client';
import { ChatMessage } from './ChatMessage';
import { BookSuggestionCard } from './BookSuggestionCard';
import { SendIcon } from '../../components/icons';
import { Toast } from '../../components/Toast';
import { useToast } from '../../components/useToast';
import './ChatPage.css';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  suggestions?: BookCandidate[];
}

const ChatPage: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [bookStatus, setBookStatus] = useState<Record<string, 'added'>>({});
  const { toast, showToast } = useToast(3000);
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

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);
    setError(null);

    const assistantId = (Date.now() + 1).toString();

    const upsertAssistant = (update: (m: Message) => Message) => {
      setMessages((prev) => {
        const exists = prev.some((m) => m.id === assistantId);
        if (!exists) {
          return [...prev, update({ id: assistantId, role: 'assistant', content: '', suggestions: [] })];
        }
        return prev.map((m) => (m.id === assistantId ? update(m) : m));
      });
    };

    try {
      await streamBookSuggestions(userMessage.content, {
        onMessage: (content) => upsertAssistant((m) => ({ ...m, content })),
        onSuggestion: (book) =>
          upsertAssistant((m) => ({ ...m, suggestions: [...(m.suggestions ?? []), book] })),
      });
    } catch (err) {
      setError('Failed to get suggestions. Please try again.');
      console.error('Error getting suggestions:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const bookKey = (book: BookCandidate) => book.isbn || `${book.title}|${book.author}`;

  const handleAddToCollection = async (book: BookCandidate) => {
    const key = bookKey(book);
    try {
      await addBookToCollection({
        title: book.title,
        author: book.author,
        isbn: book.isbn || undefined,
        description: book.description,
      });
      setBookStatus((prev) => ({ ...prev, [key]: 'added' }));
      showToast(`"${book.title}" added to your collection!`);
    } catch (err) {
      showToast('Failed to add book. It may already be in your collection.', 'error');
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
              <p className="chat-page__welcome-text">
                Tell me what you're in the mood to read, and I'll suggest personalized book recommendations based on your collection and preferences.
              </p>
              <div className="chat-page__examples">
                <p className="chat-page__examples-title">Try asking:</p>
                <button 
                  className="chat-page__example-btn"
                  onClick={() => setInput("I want something light and fun to read")}
                >
                  "I want something light and fun to read"
                </button>
                <button 
                  className="chat-page__example-btn"
                  onClick={() => setInput("Recommend me a thriller that will keep me up at night")}
                >
                  "Recommend me a thriller that will keep me up at night"
                </button>
                <button 
                  className="chat-page__example-btn"
                  onClick={() => setInput("I'm looking for a thought-provoking non-fiction book")}
                >
                  "I'm looking for a thought-provoking non-fiction book"
                </button>
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
                        onAddToCollection={handleAddToCollection}
                      />
                    ))}
                  </div>
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
          <form onSubmit={handleSubmit} className="chat-page__input-form">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="What kind of book are you looking for?"
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
      {toast && <Toast message={toast.message} variant={toast.variant} />}
    </div>
  );
};

export default ChatPage;
