import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import ChatPage from './ChatPage';
import { ToastProvider } from '../../components/ToastProvider';
import * as api from '../../api/client';

vi.mock('../../api/client');

const book = (title: string, author: string, isbn = ''): api.BookCandidate => ({
  title,
  author,
  isbn,
  genre: 'Fantasy',
  description: 'A story.',
  whySuggested: 'You will like it.',
  coverUrl: null,
});

/** The alreadySuggested list sent with the n-th suggestion request. */
const sentWith = (call: number) => vi.mocked(api.streamBookSuggestions).mock.calls[call]?.[1].alreadySuggested;

/** Options passed to the n-th suggestion request. */
const optionsOf = (call: number) => vi.mocked(api.streamBookSuggestions).mock.calls[call]?.[1];

const Wrapper = ({ children }: { children: React.ReactNode }) => <ToastProvider>{children}</ToastProvider>;

const DONE_STATUS = /That's everything|No new recommendations|from your Reading List\. Tell me|No picks this time/;

const ask = async (prompt: string, placeholder = 'What kind of book are you looking for?') => {
  const doneBefore = screen.queryAllByText(DONE_STATUS).length;
  fireEvent.change(screen.getByPlaceholderText(placeholder), {
    target: { value: prompt },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }));
  // Status lines from earlier turns stay on screen, so wait for a new one instead of matching any.
  await waitFor(() => expect(screen.queryAllByText(DONE_STATUS).length).toBeGreaterThan(doneBefore));
};

describe('ChatPage', () => {
  beforeAll(() => {
    // jsdom does not implement scrolling.
    Element.prototype.scrollIntoView = vi.fn();
  });

  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('sends every book proposed earlier in the conversation with a follow-up request', async () => {
    vi.mocked(api.streamBookSuggestions)
      .mockImplementationOnce(async (_prompt, { onMessage, onSuggestion }) => {
        onMessage('Here you go:');
        onSuggestion(book('The Lightning Thief', 'Rick Riordan', '9780786838653'));
        onSuggestion(book('The Hobbit', 'J.R.R. Tolkien'));
      })
      .mockImplementationOnce(async (_prompt, { onMessage, onSuggestion }) => {
        onMessage('More:');
        onSuggestion(book('Howl’s Moving Castle', 'Diana Wynne Jones'));
      })
      .mockImplementationOnce(async (_prompt, { onMessage }) => {
        onMessage('Even more:');
      });

    render(<ChatPage />, { wrapper: Wrapper });

    await ask('something light and fun');
    expect(sentWith(0)).toEqual([]);

    await ask('3 more please');
    expect(sentWith(1)).toEqual([
      { title: 'The Lightning Thief', author: 'Rick Riordan', isbn: '9780786838653' },
      { title: 'The Hobbit', author: 'J.R.R. Tolkien' },
    ]);

    await ask('and more');
    expect(sentWith(2)).toEqual([
      { title: 'The Lightning Thief', author: 'Rick Riordan', isbn: '9780786838653' },
      { title: 'The Hobbit', author: 'J.R.R. Tolkien' },
      { title: 'Howl’s Moving Castle', author: 'Diana Wynne Jones' },
    ]);
  });

  describe('Reading List mode', () => {
    const pick = (title: string): api.BookCandidate => ({ ...book(title, 'Jane Austen', '222'), genre: null });

    it('sends the backlog mode and shows picks without add-to-list actions', async () => {
      vi.mocked(api.streamBookSuggestions).mockImplementationOnce(async (_prompt, { onMessage, onSuggestion }) => {
        onMessage('Here is the best pick from your Reading List:');
        onSuggestion(pick('Emma'));
      });
      render(<ChatPage />, { wrapper: Wrapper });

      fireEvent.click(screen.getByRole('button', { name: 'From my Reading List' }));
      await ask('something light', 'What are you in the mood for tonight?');

      expect(optionsOf(0)?.mode).toBe('BACKLOG');
      expect(optionsOf(0)?.alreadySuggested).toEqual([]);
      expect(screen.getByText('Emma')).toBeInTheDocument();
      expect(screen.getByText('Why now:')).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /to reading list/i })).not.toBeInTheDocument();
    });

    it('defaults to discovering new books', async () => {
      vi.mocked(api.streamBookSuggestions).mockImplementationOnce(async (_prompt, { onMessage }) => {
        onMessage('Nothing new.');
      });
      render(<ChatPage />, { wrapper: Wrapper });

      await ask('something light');

      expect(optionsOf(0)?.mode).toBe('DISCOVER');
    });

    it('shows the empty state returned for an empty Reading List', async () => {
      vi.mocked(api.streamBookSuggestions).mockImplementationOnce(async (_prompt, { onMessage }) => {
        onMessage('Your Reading List is empty. Add a few books to it first.');
      });
      render(<ChatPage />, { wrapper: Wrapper });

      fireEvent.click(screen.getByRole('button', { name: 'From my Reading List' }));
      await ask('anything', 'What are you in the mood for tonight?');

      expect(screen.getByText(/Your Reading List is empty/)).toBeInTheDocument();
      expect(screen.getByText('No picks this time.')).toBeInTheDocument();
    });

    it('shows an error when the request fails', async () => {
      vi.mocked(api.streamBookSuggestions).mockRejectedValueOnce(new Error('boom'));
      vi.spyOn(console, 'error').mockImplementation(() => undefined);
      render(<ChatPage />, { wrapper: Wrapper });

      fireEvent.click(screen.getByRole('button', { name: 'From my Reading List' }));
      fireEvent.change(screen.getByPlaceholderText('What are you in the mood for tonight?'), {
        target: { value: 'anything' },
      });
      fireEvent.click(screen.getByRole('button', { name: 'Send message' }));

      expect(await screen.findByText('Failed to get suggestions. Please try again.')).toBeInTheDocument();
    });
  });
});
