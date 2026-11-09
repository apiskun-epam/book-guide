import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import BookDetailSidebar from './BookDetailSidebar';
import { Book } from '../api/client';

const make = (id: number, over: Partial<Book> = {}): Book => ({
  id,
  title: `Book ${id}`,
  author: 'Andrzej Sapkowski',
  isbn: '123',
  coverUrl: null,
  publishYear: 1993,
  description: 'A witcher story.',
  rating: 4,
  addedAt: '2024-01-02T00:00:00',
  updatedAt: '2024-01-02T00:00:00',
  ...over,
});

const setup = (book: Book, books: Book[]) => {
  const onClose = vi.fn();
  const onSelectBook = vi.fn();
  render(
    <BookDetailSidebar book={book} books={books} onClose={onClose} onSelectBook={onSelectBook} />
  );
  return { onClose, onSelectBook };
};

describe('BookDetailSidebar', () => {
  it('shows book data on the first tab', () => {
    const b = make(1);
    setup(b, [b]);
    expect(screen.getByRole('heading', { name: 'Book 1' })).toBeInTheDocument();
    expect(screen.getByText('A witcher story.')).toBeInTheDocument();
    expect(screen.getByText('4 / 5')).toBeInTheDocument();
  });

  it('shows author data and other books on the second tab', () => {
    const a = make(1);
    const b = make(2);
    const c = make(3, { author: 'Someone Else' });
    const { onSelectBook } = setup(a, [a, b, c]);
    fireEvent.click(screen.getByRole('tab', { name: 'Author' }));
    expect(screen.getByRole('heading', { name: 'Andrzej Sapkowski' })).toBeInTheDocument();
    expect(screen.queryByText('Book 3')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Book 2' }));
    expect(onSelectBook).toHaveBeenCalledWith(b);
  });

  it('handles empty states', () => {
    const b = make(1, { author: null, description: null });
    setup(b, [b]);
    expect(screen.getByText('No description available.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Author' }));
    expect(screen.getByText(/No author information/)).toBeInTheDocument();
  });

  it('closes', () => {
    const b = make(1);
    const { onClose } = setup(b, [b]);
    fireEvent.click(screen.getByRole('button', { name: 'Close details' }));
    expect(onClose).toHaveBeenCalled();
  });
});
