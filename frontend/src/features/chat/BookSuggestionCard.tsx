import React from 'react';
import { BookCandidate } from '../../api/client';
import './BookSuggestionCard.css';

interface BookSuggestionCardProps {
  book: BookCandidate;
  onAddToCollection?: (book: BookCandidate) => void;
  added?: boolean;
}

export const BookSuggestionCard: React.FC<BookSuggestionCardProps> = ({ 
  book, 
  onAddToCollection,
  added,
}) => {
  return (
    <div className="book-suggestion-card">
      <div className="book-suggestion-card__header">
        <h3 className="book-suggestion-card__title">{book.title}</h3>
        <p className="book-suggestion-card__author">by {book.author}</p>
        <span className="book-suggestion-card__genre">{book.genre}</span>
      </div>
      
      <div className="book-suggestion-card__body">
        <p className="book-suggestion-card__description">{book.description}</p>
        
        <div className="book-suggestion-card__why">
          <strong>Why this book:</strong>
          <p>{book.whySuggested}</p>
        </div>
        
        {book.isbn && (
          <p className="book-suggestion-card__isbn">ISBN: {book.isbn}</p>
        )}
      </div>
      
      {onAddToCollection && (
        <div className="book-suggestion-card__footer">
          <button
            className="book-suggestion-card__add-btn"
            onClick={() => onAddToCollection(book)}
            disabled={added}
            aria-label={`Add ${book.title} to collection`}
          >
            {added ? 'Added ✓' : 'Add to Collection'}
          </button>
        </div>
      )}
    </div>
  );
};
