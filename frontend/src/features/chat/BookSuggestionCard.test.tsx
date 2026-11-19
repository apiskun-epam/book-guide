import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { BookSuggestionCard } from './BookSuggestionCard';
import { BookCandidate } from '../../api/client';

const candidate: BookCandidate = {
  title: 'Dune',
  author: 'Frank Herbert',
  isbn: '111',
  genre: 'Sci-fi',
  description: 'Spice.',
  whySuggested: 'Epic.',
  coverUrl: null,
};

describe('BookSuggestionCard', () => {
  it('offers to add to the reading list', () => {
    const onAdd = vi.fn();
    render(<BookSuggestionCard book={candidate} onAddToReadingList={onAdd} />);

    fireEvent.click(screen.getByRole('button', { name: 'Add Dune to reading list' }));

    expect(screen.getByText('Add to Reading List')).toBeInTheDocument();
    expect(onAdd).toHaveBeenCalledWith(candidate);
  });

  it('shows Added once added', () => {
    render(<BookSuggestionCard book={candidate} onAddToReadingList={vi.fn()} added />);
    expect(screen.getByRole('button', { name: 'Add Dune to reading list' })).toBeDisabled();
    expect(screen.getByText('Added ✓')).toBeInTheDocument();
  });

  it('omits the genre chip and the add button for a Reading List pick', () => {
    render(<BookSuggestionCard book={{ ...candidate, genre: null }} whyLabel="Why now:" />);

    expect(screen.getByText('Why now:')).toBeInTheDocument();
    expect(screen.queryByText('Sci-fi')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
