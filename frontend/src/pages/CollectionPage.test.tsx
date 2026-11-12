import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CollectionPage from './CollectionPage';
import { ToastProvider } from '../components/ToastProvider';
import * as api from '../api/client';

vi.mock('../api/client');

const dune: api.Book = {
  id: 1,
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

const setup = () => render(<CollectionPage />, { wrapper: ToastProvider });

describe('CollectionPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(api.getCollection).mockResolvedValue([dune]);
  });

  it('shows the empty state when there are no books', async () => {
    vi.mocked(api.getCollection).mockResolvedValue([]);
    setup();
    expect(await screen.findByText('Your collection is empty')).toBeTruthy();
  });

  it('shows an error state when loading fails', async () => {
    vi.mocked(api.getCollection).mockRejectedValue(new Error('x'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    setup();
    expect(await screen.findByText('Failed to load collection')).toBeTruthy();
  });

  it('removes a book without a browser confirm and offers Undo', async () => {
    const confirm = vi.spyOn(window, 'confirm');
    vi.mocked(api.removeBookFromCollection).mockResolvedValue();
    setup();

    fireEvent.click(await screen.findByRole('button', { name: 'Remove' }));

    expect(confirm).not.toHaveBeenCalled();
    expect(api.removeBookFromCollection).toHaveBeenCalledWith(1);
    expect(await screen.findByText('Your collection is empty')).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('"Dune" removed');
    expect(screen.getByRole('button', { name: 'Undo' })).toBeTruthy();
  });

  it('restores the book when Undo is clicked', async () => {
    vi.mocked(api.removeBookFromCollection).mockResolvedValue();
    vi.mocked(api.undoBookRemoval).mockResolvedValue(dune);
    setup();

    fireEvent.click(await screen.findByRole('button', { name: 'Remove' }));
    await screen.findByText('Your collection is empty');
    vi.mocked(api.getCollection).mockResolvedValue([dune]);
    fireEvent.click(screen.getByRole('button', { name: 'Undo' }));

    expect(api.undoBookRemoval).toHaveBeenCalledWith(1);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Dune' })).toBeTruthy());
  });

  it('shows an error toast and keeps the book when removal fails', async () => {
    vi.mocked(api.removeBookFromCollection).mockRejectedValue(new Error('x'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    setup();

    fireEvent.click(await screen.findByRole('button', { name: 'Remove' }));

    expect((await screen.findByRole('alert')).textContent).toBe('Failed to remove book');
    expect(screen.getByRole('button', { name: 'Dune' })).toBeTruthy();
  });
});
