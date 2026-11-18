import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ReadingListPage from './ReadingListPage';
import { ToastProvider } from '../components/ToastProvider';
import * as api from '../api/client';

vi.mock('../api/client');

const dune: api.Book = {
  id: 1,
  listType: 'READING_LIST',
  title: 'Dune',
  author: 'Frank Herbert',
  isbn: '111',
  coverUrl: null,
  publishYear: 1965,
  description: null,
  rating: null,
  addedAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
};

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <MemoryRouter>
    <ToastProvider>{children}</ToastProvider>
  </MemoryRouter>
);

const setup = () => render(<ReadingListPage />, { wrapper: Wrapper });

describe('ReadingListPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(api.getReadingList).mockResolvedValue([dune]);
  });

  it('shows the empty state when there are no books', async () => {
    vi.mocked(api.getReadingList).mockResolvedValue([]);
    setup();
    expect(await screen.findByText(/Your reading list is empty/)).toBeTruthy();
  });

  it('shows an error state when loading fails', async () => {
    vi.mocked(api.getReadingList).mockRejectedValue(new Error('x'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    setup();
    expect(await screen.findByText('Failed to load reading list')).toBeTruthy();
  });

  it('shows books without a rating', async () => {
    setup();
    expect(await screen.findByText('Dune')).toBeTruthy();
    expect(screen.queryByLabelText('Rate 3 out of 5')).toBeNull();
  });

  it('removes a book and offers Undo', async () => {
    vi.mocked(api.removeBookFromReadingList).mockResolvedValue();
    setup();

    fireEvent.click(await screen.findByRole('button', { name: 'Remove' }));

    await waitFor(() => expect(api.removeBookFromReadingList).toHaveBeenCalledWith(1));
    expect(await screen.findByRole('button', { name: 'Undo' })).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('removed from your reading list');
  });

  it('moves a book to the collection with a rating from the modal', async () => {
    vi.mocked(api.moveToCollection).mockResolvedValue({ ...dune, listType: 'COLLECTION', rating: 4 });
    setup();

    fireEvent.click(await screen.findByRole('button', { name: 'Add to my Collection' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeTruthy();
    fireEvent.click(screen.getByLabelText('Rate 4 out of 5'));
    const buttons = screen.getAllByRole('button', { name: 'Add to my Collection' });
    fireEvent.click(buttons[buttons.length - 1]!);

    await waitFor(() => expect(api.moveToCollection).toHaveBeenCalledWith(1, 4));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(screen.queryByText('Dune')).toBeNull();
  });

  it('keeps the modal open and shows an error toast when the move fails', async () => {
    vi.mocked(api.moveToCollection).mockRejectedValue(new Error('x'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    setup();

    fireEvent.click(await screen.findByRole('button', { name: 'Add to my Collection' }));
    await screen.findByRole('dialog');
    const buttons = screen.getAllByRole('button', { name: 'Add to my Collection' });
    fireEvent.click(buttons[buttons.length - 1]!);

    await waitFor(() => expect(api.moveToCollection).toHaveBeenCalledWith(1, undefined));
    expect(await screen.findByText('Failed to add book to your collection')).toBeTruthy();
    expect(screen.getByRole('dialog')).toBeTruthy();
  });
});
