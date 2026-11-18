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

  it('defaults to the reading list and hides the rating', () => {
    const onAdd = vi.fn();
    const b = book();
    render(<BookDetailModal book={b} onClose={vi.fn()} onAdd={onAdd} />);

    expect(screen.getByLabelText('Add to Reading list')).toBeChecked();
    expect(screen.queryByLabelText('Rate 4 out of 5')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Add to Reading list' }));
    expect(onAdd).toHaveBeenCalledWith(b, 'READING_LIST', 0);
  });

  it('shows the rating and adds to the collection with it', () => {
    const onAdd = vi.fn();
    const b = book();
    render(<BookDetailModal book={b} onClose={vi.fn()} onAdd={onAdd} />);

    fireEvent.click(screen.getByLabelText('Add to my Collection'));
    fireEvent.click(screen.getByLabelText('Rate 4 out of 5'));
    fireEvent.click(screen.getByRole('button', { name: 'Add to my Collection' }));

    expect(onAdd).toHaveBeenCalledWith(b, 'COLLECTION', 4);
  });

  it('adds to the collection without a rating when none is chosen', () => {
    const onAdd = vi.fn();
    const b = book();
    render(<BookDetailModal book={b} onClose={vi.fn()} onAdd={onAdd} />);

    fireEvent.click(screen.getByLabelText('Add to my Collection'));
    fireEvent.click(screen.getByRole('button', { name: 'Add to my Collection' }));

    expect(onAdd).toHaveBeenCalledWith(b, 'COLLECTION', 0);
  });

  it('in move mode offers only the collection, with an optional rating', () => {
    const onAdd = vi.fn();
    const b = book();
    render(<BookDetailModal book={b} mode="move" onClose={vi.fn()} onAdd={onAdd} />);

    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('Rate 2 out of 5'));
    fireEvent.click(screen.getByRole('button', { name: 'Add to my Collection' }));

    expect(onAdd).toHaveBeenCalledWith(b, 'COLLECTION', 2);
  });

  it('closes on Escape', () => {
    const onClose = vi.fn();
    render(<BookDetailModal book={book()} onClose={onClose} onAdd={vi.fn()} />);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});
