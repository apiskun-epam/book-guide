import React, { useEffect, useMemo, useState } from 'react';
import { Book } from '../api/client';
import './BookDetailSidebar.css';

type Tab = 'book' | 'author';

interface BookDetailSidebarProps {
  book: Book;
  /** Full collection, used to derive author stats. */
  books: Book[];
  onClose: () => void;
  onSelectBook: (book: Book) => void;
}

const formatDate = (iso: string): string => {
  const date = new Date(iso);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
};

const BookDetailSidebar: React.FC<BookDetailSidebarProps> = ({
  book,
  books,
  onClose,
  onSelectBook,
}) => {
  const [tab, setTab] = useState<Tab>('book');

  // Always start on the book tab when a different book is selected.
  useEffect(() => {
    setTab('book');
  }, [book.id]);

  const authorBooks = useMemo(
    () => (book.author ? books.filter((b) => b.author === book.author) : []),
    [book.author, books]
  );
  const otherBooks = authorBooks.filter((b) => b.id !== book.id);
  const rated = authorBooks.filter((b) => b.rating);
  const averageRating = rated.length
    ? (rated.reduce((sum, b) => sum + (b.rating ?? 0), 0) / rated.length).toFixed(1)
    : null;

  const metadata: [string, string][] = [
    ['Author', book.author || 'Unknown Author'],
    ['Published', book.publishYear ? String(book.publishYear) : '—'],
    ['ISBN', book.isbn || '—'],
    ['Rating', book.rating ? `${book.rating} / 5` : 'Not rated'],
    ['Added', formatDate(book.addedAt)],
  ];

  return (
    <aside className="detail-sidebar" aria-label="Book details">
      <div className="detail-tabs" role="tablist">
        <button
          role="tab"
          id="detail-tab-book"
          aria-selected={tab === 'book'}
          aria-controls="detail-panel"
          className={`detail-tab ${tab === 'book' ? 'active' : ''}`}
          onClick={() => setTab('book')}
        >
          Book
        </button>
        <button
          role="tab"
          id="detail-tab-author"
          aria-selected={tab === 'author'}
          aria-controls="detail-panel"
          className={`detail-tab ${tab === 'author' ? 'active' : ''}`}
          onClick={() => setTab('author')}
        >
          Author
        </button>
        <button className="detail-close" onClick={onClose} aria-label="Close details">
          ×
        </button>
      </div>

      <div
        className="detail-body"
        role="tabpanel"
        id="detail-panel"
        aria-labelledby={`detail-tab-${tab}`}
      >
        {tab === 'book' ? (
          <>
            {book.coverUrl ? (
              <img src={book.coverUrl} alt={`Cover of ${book.title}`} className="detail-cover" />
            ) : (
              <div className="detail-cover detail-cover-empty" aria-hidden="true" />
            )}
            <h2 className="detail-title">{book.title}</h2>
            <p className="detail-subtitle">{book.author || 'Unknown Author'}</p>

            <h3 className="detail-section">Summary</h3>
            {book.description ? (
              <p className="detail-text">{book.description}</p>
            ) : (
              <p className="detail-muted">No description available.</p>
            )}

            <h3 className="detail-section">Metadata</h3>
            <dl className="detail-meta">
              {metadata.map(([label, value]) => (
                <React.Fragment key={label}>
                  <dt>{label}</dt>
                  <dd>{value}</dd>
                </React.Fragment>
              ))}
            </dl>
          </>
        ) : book.author ? (
          <>
            <div className="detail-avatar" aria-hidden="true">
              {book.author.charAt(0).toUpperCase()}
            </div>
            <h2 className="detail-title">{book.author}</h2>

            <h3 className="detail-section">In your collection</h3>
            <dl className="detail-meta">
              <dt>Books</dt>
              <dd>{authorBooks.length}</dd>
              <dt>Avg. rating</dt>
              <dd>{averageRating ? `${averageRating} / 5` : 'Not rated'}</dd>
            </dl>

            <h3 className="detail-section">Other books by this author</h3>
            {otherBooks.length === 0 ? (
              <p className="detail-muted">No other books by this author in your collection.</p>
            ) : (
              <ul className="detail-author-books">
                {otherBooks.map((b) => (
                  <li key={b.id}>
                    <button className="detail-author-book" onClick={() => onSelectBook(b)}>
                      {b.coverUrl ? (
                        <img src={b.coverUrl} alt="" />
                      ) : (
                        <div className="detail-thumb-empty" aria-hidden="true" />
                      )}
                      <span>{b.title}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        ) : (
          <p className="detail-muted">No author information available for this book.</p>
        )}
      </div>
    </aside>
  );
};

export default BookDetailSidebar;
