import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import BookDetailModal from './BookDetailModal';
import { BookSearchResult } from '../api/client';

const book = (over: Partial<BookSearchResult> = {}): BookSearchResult => ({
  title: 'The Last Wish',
  author: 'Andrzej Sapkowski',
  isbn: '123',
  coverUrl: null,
  publishYear: 1993,
  description: 'A witcher story.',
  ...over,
});

describe('BookDetailModal', () => {
  it('shows the description', () => {
    render(<BookDetailModal book={book()} onClose={vi.fn()} onAdd={vi.fn()} />);
    expect(screen.getByText('A witcher story.')).toBeInTheDocument();
  });

  it('shows an empty state without a description', () => {
    render(<BookDetailModal book={book({ description: null })} onClose={vi.fn()} onAdd={vi.fn()} />);
    expect(screen.getByText('No description available.')).toBeInTheDocument();
  });

  it('collapses long descriptions behind Show more', () => {
    render(
      <BookDetailModal book={book({ description: 'x'.repeat(500) })} onClose={vi.fn()} onAdd={vi.fn()} />
    );
    fireEvent.click(screen.getByRole('button', { name: 'Show more' }));
    expect(screen.getByRole('button', { name: 'Show less' })).toBeInTheDocument();
  });

  it('adds with the chosen rating', () => {
    const onAdd = vi.fn();
    const b = book();
    render(<BookDetailModal book={b} onClose={vi.fn()} onAdd={onAdd} />);
    fireEvent.click(screen.getByLabelText('Rate 4 out of 5'));
    fireEvent.click(screen.getByRole('button', { name: 'Add to collection' }));
    expect(onAdd).toHaveBeenCalledWith(b, 4);
  });

  it('closes on Escape', () => {
    const onClose = vi.fn();
    render(<BookDetailModal book={book()} onClose={onClose} onAdd={vi.fn()} />);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});
