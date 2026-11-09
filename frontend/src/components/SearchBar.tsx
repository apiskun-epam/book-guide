import React, { useCallback, useEffect, useRef, useState } from 'react';
import { searchBooks, addBookToCollection, BookSearchResult } from '../api/client';
import BookDetailModal from './BookDetailModal';
import './SearchBar.css';

export const MIN_QUERY_LENGTH = 3;
export const DEBOUNCE_MS = 300;

type SearchStatus = 'idle' | 'loading' | 'success' | 'error';

const SearchBar: React.FC<{ onBookAdded: () => void }> = ({ onBookAdded }) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<BookSearchResult[]>([]);
  const [status, setStatus] = useState<SearchStatus>('idle');
  const [showResults, setShowResults] = useState(false);
  const [selectedBook, setSelectedBook] = useState<BookSearchResult | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  // Incremented per request so a slow, older response can never overwrite a newer one.
  const requestId = useRef(0);

  const runSearch = useCallback(async (term: string) => {
    const id = ++requestId.current;
    setStatus('loading');
    setShowResults(true);
    try {
      const books = await searchBooks(term);
      if (id !== requestId.current) return;
      setResults(books);
      setStatus('success');
    } catch (error) {
      if (id !== requestId.current) return;
      console.error('Search failed:', error);
      setResults([]);
      setStatus('error');
    }
  }, []);

  // Typeahead: debounce typing, start at MIN_QUERY_LENGTH characters.
  useEffect(() => {
    const term = query.trim();
    if (term.length < MIN_QUERY_LENGTH) {
      requestId.current++; // discard any in-flight request
      setResults([]);
      setStatus('idle');
      setShowResults(false);
      return;
    }
    const timer = setTimeout(() => runSearch(term), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query, runSearch]);

  // Close the dropdown on outside click or Escape.
  useEffect(() => {
    const onPointerDown = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setShowResults(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, []);

  // Explicit submit searches immediately, without waiting for the debounce.
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const term = query.trim();
    if (!term) return;
    runSearch(term);
  };

  const handleBookClick = (book: BookSearchResult) => {
    setSelectedBook(book);
    setShowResults(false);
  };

  const handleAddBook = async (book: BookSearchResult, rating: number) => {
    try {
      await addBookToCollection({
        title: book.title,
        author: book.author || undefined,
        isbn: book.isbn || undefined,
        coverUrl: book.coverUrl || undefined,
        publishYear: book.publishYear || undefined,
        description: book.description || undefined,
        rating,
      });
      setSelectedBook(null);
      setQuery('');
      onBookAdded();
    } catch (error) {
      console.error('Failed to add book:', error);
      alert('Failed to add book to collection');
    }
  };

  return (
    <>
      <div className="search-bar-container" ref={containerRef}>
        <form onSubmit={handleSearch} className="search-form" role="search">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => status !== 'idle' && setShowResults(true)}
            placeholder="Search for books..."
            aria-label="Search for books"
            autoComplete="off"
            className="search-input"
          />
          <button type="submit" className="search-button" disabled={status === 'loading'}>
            {status === 'loading' ? 'Searching...' : 'Search'}
          </button>
        </form>

        {showResults && status !== 'idle' && (
          <div className="search-results">
            {status === 'loading' && results.length === 0 && (
              <div className="search-message">Searching...</div>
            )}
            {status === 'error' && (
              <div className="search-message search-message-error">
                Search failed. Please try again.
              </div>
            )}
            {status === 'success' && results.length === 0 && (
              <div className="search-message">No books found.</div>
            )}
            {results.map((book, index) => (
              <button
                type="button"
                key={`${book.isbn ?? book.title}-${index}`}
                className="search-result-item"
                onClick={() => handleBookClick(book)}
              >
                {book.coverUrl && (
                  <img src={book.coverUrl} alt="" className="result-cover" />
                )}
                <div className="result-info">
                  <div className="result-title">{book.title}</div>
                  <div className="result-author">{book.author || 'Unknown Author'}</div>
                  {book.publishYear && (
                    <div className="result-year">{book.publishYear}</div>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {selectedBook && (
        <BookDetailModal
          book={selectedBook}
          onClose={() => setSelectedBook(null)}
          onAdd={handleAddBook}
        />
      )}
    </>
  );
};

export default SearchBar;
