import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import SearchBar, { DEBOUNCE_MS } from './SearchBar';
import * as client from '../api/client';

vi.mock('../api/client');

const book = (title: string): client.BookSearchResult => ({
  title,
  author: 'Some Author',
  isbn: null,
  coverUrl: null,
  publishYear: 2000,
  description: null,
});

const type = (value: string) =>
  fireEvent.change(screen.getByLabelText('Search for books'), { target: { value } });

const advance = (ms: number) => act(() => vi.advanceTimersByTimeAsync(ms));

describe('SearchBar typeahead', () => {
  const search = vi.mocked(client.searchBooks);

  beforeEach(() => {
    vi.useFakeTimers();
    search.mockReset();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('does not search below 3 characters', async () => {
    render(<SearchBar onBookAdded={() => {}} />);
    type('du');
    await advance(DEBOUNCE_MS * 2);
    expect(search).not.toHaveBeenCalled();
  });

  it('searches once after the debounce when 3 characters are typed', async () => {
    search.mockResolvedValue([book('Dune')]);
    render(<SearchBar onBookAdded={() => {}} />);
    type('dun');
    type('dune');
    await advance(DEBOUNCE_MS - 1);
    expect(search).not.toHaveBeenCalled();
    await advance(1);
    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledWith('dune');
    expect(screen.getByText('Dune')).toBeInTheDocument();
  });

  it('closes results when the query drops below 3 characters', async () => {
    search.mockResolvedValue([book('Dune')]);
    render(<SearchBar onBookAdded={() => {}} />);
    type('dune');
    await advance(DEBOUNCE_MS);
    expect(screen.getByText('Dune')).toBeInTheDocument();
    type('du');
    expect(screen.queryByText('Dune')).not.toBeInTheDocument();
  });

  it('ignores a stale response that resolves after a newer one', async () => {
    let resolveOld: (b: client.BookSearchResult[]) => void = () => {};
    search
      .mockImplementationOnce(() => new Promise((r) => { resolveOld = r; }))
      .mockResolvedValueOnce([book('New Result')]);
    render(<SearchBar onBookAdded={() => {}} />);
    type('old');
    await advance(DEBOUNCE_MS);
    type('new');
    await advance(DEBOUNCE_MS);
    await act(async () => resolveOld([book('Old Result')]));
    expect(screen.getByText('New Result')).toBeInTheDocument();
    expect(screen.queryByText('Old Result')).not.toBeInTheDocument();
  });

  it('shows an empty state', async () => {
    search.mockResolvedValue([]);
    render(<SearchBar onBookAdded={() => {}} />);
    type('zzz');
    await advance(DEBOUNCE_MS);
    expect(screen.getByText('No books found.')).toBeInTheDocument();
  });

  it('shows an error state', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    search.mockRejectedValue(new Error('boom'));
    render(<SearchBar onBookAdded={() => {}} />);
    type('dune');
    await advance(DEBOUNCE_MS);
    expect(screen.getByText('Search failed. Please try again.')).toBeInTheDocument();
  });

  it('submitting searches immediately, even under 3 characters', async () => {
    search.mockResolvedValue([book('Up')]);
    render(<SearchBar onBookAdded={() => {}} />);
    type('up');
    fireEvent.submit(screen.getByRole('search'));
    await advance(0);
    expect(search).toHaveBeenCalledWith('up');
  });
});
