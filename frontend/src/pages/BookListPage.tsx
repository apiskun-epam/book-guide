import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import {
  getCollection,
  getReadingList,
  updateBookRating,
  removeBookFromCollection,
  removeBookFromReadingList,
  undoBookRemoval,
  undoReadingListRemoval,
  moveToCollection,
  Book,
} from '../api/client';
import { useCollectionRefresh } from '../components/CollectionRefresh';
import BookDetailSidebar from '../components/BookDetailSidebar';
import BookDetailModal from '../components/BookDetailModal';
import { useToast } from '../components/ToastProvider';
import './CollectionPage.css';

/** Matches the backend's undo window (CollectionService.UNDO_WINDOW). */
const UNDO_WINDOW_MS = 5000;

export type ListVariant = 'collection' | 'reading-list';

const VARIANTS = {
  collection: {
    label: 'collection',
    fetch: getCollection,
    remove: removeBookFromCollection,
    undo: undoBookRemoval,
    loadingText: 'Loading your collection...',
    errorText: 'Failed to load collection',
    emptyText:
      'No books in your collection just yet. Ready to find your next read? Use search bar, or a suggestion bot to get started',
  },
  'reading-list': {
    label: 'reading list',
    fetch: getReadingList,
    remove: removeBookFromReadingList,
    undo: undoReadingListRemoval,
    loadingText: 'Loading your reading list...',
    errorText: 'Failed to load reading list',
    emptyText:
      'Your reading list is empty. Use the search bar, or a suggestion bot, to save books you plan to read',
  },
} as const;

const BookListPage: React.FC<{ variant: ListVariant }> = ({ variant }) => {
  const config = VARIANTS[variant];
  const showRatings = variant === 'collection';
  const { showToast } = useToast();
  const { version: refreshVersion, notifyBookAdded } = useCollectionRefresh();
  const [moveCandidate, setMoveCandidate] = useState<Book | null>(null);
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selectedBook = books.find((b) => b.id === selectedId) ?? null;

  const fetchCollection = async () => {
    try {
      setLoading(true);
      const collection = await config.fetch();
      setBooks(collection);
      setError(null);
    } catch (err) {
      setError(config.errorText);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Reload on mount and whenever a book is added from the top bar search.
  useEffect(() => {
    fetchCollection();
  }, [refreshVersion, variant]);

  const handleRatingChange = async (bookId: number, newRating: number) => {
    try {
      await updateBookRating(bookId, newRating);
      setBooks((prevBooks) =>
        prevBooks.map((book) =>
          book.id === bookId ? { ...book, rating: newRating } : book
        )
      );
    } catch (err) {
      console.error('Failed to update rating:', err);
      showToast('Failed to update rating', { variant: 'error' });
    }
  };

  // The backend hides the book immediately and deletes it for good after the undo window,
  // so the removal completes even if the page is refreshed in the meantime.
  const handleRemoveBook = async (book: Book) => {
    try {
      await config.remove(book.id);
    } catch (err) {
      console.error('Failed to remove book:', err);
      showToast('Failed to remove book', { variant: 'error' });
      return;
    }
    setBooks((prevBooks) => prevBooks.filter((b) => b.id !== book.id));
    setSelectedId((current) => (current === book.id ? null : current));
    showToast(`"${book.title}" removed from your ${config.label}`, {
      durationMs: UNDO_WINDOW_MS,
      action: { label: 'Undo', onClick: () => handleUndoRemove(book) },
    });
  };

  const handleUndoRemove = async (book: Book) => {
    try {
      await config.undo(book.id);
      await fetchCollection();
      showToast(`"${book.title}" restored`);
    } catch (err) {
      console.error('Failed to undo removal:', err);
      showToast('Could not undo removal', { variant: 'error' });
    }
  };

  const handleMoveToCollection = async (book: Book, rating: number) => {
    try {
      await moveToCollection(book.id, rating > 0 ? rating : undefined);
    } catch (err) {
      console.error('Failed to move book:', err);
      showToast('Failed to add book to your collection', { variant: 'error' });
      return;
    }
    setMoveCandidate(null);
    setBooks((prevBooks) => prevBooks.filter((b) => b.id !== book.id));
    setSelectedId((current) => (current === book.id ? null : current));
    notifyBookAdded();
    showToast(`"${book.title}" added to your collection!`);
  };

  return (
    <div className={`collection-layout ${selectedBook ? 'has-detail' : ''}`}>
    <div className="collection-page">
      <header className="collection-header">
        <div className="collection-heading">
          <h2 className="collection-title">My books</h2>
          <nav className="collection-tabs" aria-label="Book lists">
            <NavLink to="/collection" className={({ isActive }) => `collection-tab ${isActive ? 'active' : ''}`}>
              Collection
            </NavLink>
            <NavLink to="/reading-list" className={({ isActive }) => `collection-tab ${isActive ? 'active' : ''}`}>
              Reading list
            </NavLink>
          </nav>
        </div>
      </header>

      {loading ? (
        <div className="collection-loading">{config.loadingText}</div>
      ) : error ? (
        <div className="collection-error">{error}</div>
      ) : books.length === 0 ? (
        <div className="empty-state">
          <p className="empty-title">{config.emptyText}</p>
        </div>
      ) : (
        <ul className="books-list">
          {books.map((book) => (
            <li
              key={book.id}
              className={`book-row ${book.id === selectedId ? 'selected' : ''}`}
              onClick={() => setSelectedId(book.id)}
            >
              {book.coverUrl ? (
                <img src={book.coverUrl} alt="" className="book-cover" />
              ) : (
                <div className="book-cover book-cover-empty" aria-hidden="true" />
              )}

              <div className="book-info">
                <h3 className="book-title">
                  <button
                    className="book-title-button"
                    onClick={() => setSelectedId(book.id)}
                    aria-pressed={book.id === selectedId}
                  >
                    {book.title}
                  </button>
                </h3>
                {book.description && (
                  <p className="book-description">{book.description}</p>
                )}
                <div className="book-meta">
                  <span>{book.author || 'Unknown Author'}</span>
                  {book.publishYear && (
                    <>
                      <span className="dot">•</span>
                      <span>{book.publishYear}</span>
                    </>
                  )}
                  {showRatings && (
                    <>
                      <span className="dot">•</span>
                      <div className="star-rating" aria-label="Rating" onClick={(e) => e.stopPropagation()}>
                        {[1, 2, 3, 4, 5].map((star) => (
                          <button
                            key={star}
                            className={`star ${star <= (book.rating || 0) ? 'filled' : ''}`}
                            onClick={() => handleRatingChange(book.id, star)}
                            aria-label={`Rate ${star} out of 5`}
                          >
                            ★
                          </button>
                        ))}
                      </div>
                    </>
                  )}
                </div>
              </div>

              <div className="book-actions" onClick={(e) => e.stopPropagation()}>
                {!showRatings && (
                  <button className="add-to-collection-button" onClick={() => setMoveCandidate(book)}>
                    Add to my Collection
                  </button>
                )}
                <button
                  className="remove-button"
                  onClick={() => handleRemoveBook(book)}
                >
                  Remove
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {!loading && !error && <div className="count-pill">Count: {books.length}</div>}
    </div>
    {selectedBook && (
      <BookDetailSidebar
        book={selectedBook}
        books={books}
        onClose={() => setSelectedId(null)}
        onSelectBook={(b) => setSelectedId(b.id)}
      />
    )}
    {moveCandidate && (
      <BookDetailModal
        mode="move"
        book={moveCandidate}
        onClose={() => setMoveCandidate(null)}
        onAdd={(_book, _target, rating) => handleMoveToCollection(moveCandidate, rating)}
      />
    )}
    </div>
  );
};

export default BookListPage;
