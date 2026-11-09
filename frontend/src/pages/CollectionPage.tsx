import React, { useState, useEffect } from 'react';
import { getCollection, updateBookRating, removeBookFromCollection, Book } from '../api/client';
import SearchBar from '../components/SearchBar';
import BookDetailSidebar from '../components/BookDetailSidebar';
import './CollectionPage.css';

const CollectionPage: React.FC = () => {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selectedBook = books.find((b) => b.id === selectedId) ?? null;

  const fetchCollection = async () => {
    try {
      setLoading(true);
      const collection = await getCollection();
      setBooks(collection);
      setError(null);
    } catch (err) {
      setError('Failed to load collection');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCollection();
  }, []);

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
      alert('Failed to update rating');
    }
  };

  const handleRemoveBook = async (bookId: number) => {
    if (!confirm('Are you sure you want to remove this book?')) return;

    try {
      await removeBookFromCollection(bookId);
      setBooks((prevBooks) => prevBooks.filter((book) => book.id !== bookId));
      setSelectedId((current) => (current === bookId ? null : current));
    } catch (err) {
      console.error('Failed to remove book:', err);
      alert('Failed to remove book');
    }
  };

  if (loading) {
    return <div className="collection-loading">Loading your collection...</div>;
  }

  if (error) {
    return <div className="collection-error">{error}</div>;
  }

  return (
    <div className={`collection-layout ${selectedBook ? 'has-detail' : ''}`}>
    <div className="collection-page">
      <header className="collection-header">
        <div className="collection-heading">
          <h2 className="collection-title">Collection</h2>
          <span className="collection-tab">All books</span>
        </div>
        <SearchBar onBookAdded={fetchCollection} />
      </header>

      {books.length === 0 ? (
        <div className="empty-state">
          <p className="empty-title">Your collection is empty</p>
          <p>Search for books above to get started.</p>
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
                </div>
              </div>

              <div className="book-actions" onClick={(e) => e.stopPropagation()}>
                <button
                  className="remove-button"
                  onClick={() => handleRemoveBook(book.id)}
                >
                  Remove
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="count-pill">Count: {books.length}</div>
    </div>
    {selectedBook && (
      <BookDetailSidebar
        book={selectedBook}
        books={books}
        onClose={() => setSelectedId(null)}
        onSelectBook={(b) => setSelectedId(b.id)}
      />
    )}
    </div>
  );
};

export default CollectionPage;
