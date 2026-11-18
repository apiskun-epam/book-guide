import React, { useEffect, useState } from 'react';
import { BookSearchResult } from '../api/client';
import './BookDetailModal.css';

export type AddTarget = 'READING_LIST' | 'COLLECTION';

interface BookDetailModalProps {
  book: BookSearchResult;
  onClose: () => void;
  onAdd: (book: BookSearchResult, target: AddTarget, rating: number) => void;
  /**
   * 'search' lets the user choose a list; 'move' (opened from the reading list) can only
   * add to the collection.
   */
  mode?: 'search' | 'move';
}

const CLAMP_THRESHOLD = 400;

const TARGET_OPTIONS: { value: AddTarget; title: string; hint: string; ariaLabel: string }[] = [
  {
    value: 'READING_LIST',
    title: 'Reading list',
    hint: 'Books you plan to read',
    ariaLabel: 'Add to Reading list',
  },
  {
    value: 'COLLECTION',
    title: 'My Collection',
    hint: "Books you've read, with a rating",
    ariaLabel: 'Add to my Collection',
  },
];

const BookDetailModal: React.FC<BookDetailModalProps> = ({ book, onClose, onAdd, mode = 'search' }) => {
  const [rating, setRating] = useState(0);
  const [target, setTarget] = useState<AddTarget>(mode === 'move' ? 'COLLECTION' : 'READING_LIST');
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
              {mode === 'search' && (
                <fieldset className="target-options">
                  <legend className="visually-hidden">Add to</legend>
                  {TARGET_OPTIONS.map((option) => (
                    <label
                      key={option.value}
                      className={`target-card ${target === option.value ? 'selected' : ''}`}
                    >
                      <input
                        type="radio"
                        name="add-target"
                        value={option.value}
                        aria-label={option.ariaLabel}
                        checked={target === option.value}
                        onChange={() => setTarget(option.value)}
                      />
                      <span className="target-dot" aria-hidden="true" />
                      <span className="target-text">
                        <span className="target-title">{option.title}</span>
                        <span className="target-hint">{option.hint}</span>
                      </span>
                    </label>
                  ))}
                </fieldset>
              )}
              {target === 'COLLECTION' && (
                <>
                  <span className="rating-label">Your rating (optional)</span>
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
                </>
              )}
              <button
                className="add-button"
                onClick={() => onAdd(book, target, target === 'COLLECTION' ? rating : 0)}
              >
                {target === 'COLLECTION' ? 'Add to my Collection' : 'Add to Reading list'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookDetailModal;
