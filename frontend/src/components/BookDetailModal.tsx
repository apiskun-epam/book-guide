import React, { useEffect, useState } from 'react';
import { BookSearchResult } from '../api/client';
import './BookDetailModal.css';

interface BookDetailModalProps {
  book: BookSearchResult;
  onClose: () => void;
  onAdd: (book: BookSearchResult, rating: number) => void;
}

const CLAMP_THRESHOLD = 400;

const BookDetailModal: React.FC<BookDetailModalProps> = ({ book, onClose, onAdd }) => {
  const [rating, setRating] = useState(0);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  const metadata: [string, string][] = [
    ['Author', book.author || 'Unknown Author'],
    ['Published', book.publishYear ? String(book.publishYear) : '—'],
    ['ISBN', book.isbn || '—'],
  ];
  const isLong = (book.description?.length ?? 0) > CLAMP_THRESHOLD;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        role="dialog"
        aria-modal="true"
        aria-label={book.title}
        onClick={(e) => e.stopPropagation()}
      >
        <button className="modal-close" onClick={onClose} aria-label="Close">
          ×
        </button>

        <div className="modal-body">
          {book.coverUrl ? (
            <img src={book.coverUrl} alt={`Cover of ${book.title}`} className="modal-cover" />
          ) : (
            <div className="modal-cover modal-cover-empty" aria-hidden="true" />
          )}

          <div className="modal-info">
            <header>
              <h2 className="modal-title">{book.title}</h2>
              <p className="modal-author">{book.author || 'Unknown Author'}</p>
            </header>

            <section>
              <h3 className="modal-section">Summary</h3>
              {book.description ? (
                <>
                  <p className={`modal-description ${isLong && !expanded ? 'clamped' : ''}`}>
                    {book.description}
                  </p>
                  {isLong && (
                    <button className="modal-more" onClick={() => setExpanded((v) => !v)}>
                      {expanded ? 'Show less' : 'Show more'}
                    </button>
                  )}
                </>
              ) : (
                <p className="modal-muted">No description available.</p>
              )}
            </section>

            <section>
              <h3 className="modal-section">Metadata</h3>
              <dl className="modal-meta">
                {metadata.map(([label, value]) => (
                  <React.Fragment key={label}>
                    <dt>{label}</dt>
                    <dd>{value}</dd>
                  </React.Fragment>
                ))}
              </dl>
            </section>

            <div className="rating-section">
              <label>Your rating</label>
              <div className="star-rating">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    className={`star ${star <= rating ? 'filled' : ''}`}
                    onClick={() => setRating(star)}
                    aria-label={`Rate ${star} out of 5`}
                  >
                    ★
                  </button>
                ))}
              </div>
              <button
                className="add-button"
                onClick={() => onAdd(book, rating)}
              >
                Add to collection
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookDetailModal;
