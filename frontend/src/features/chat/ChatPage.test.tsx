import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
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

const Wrapper = ({ children }: { children: React.ReactNode }) => <ToastProvider>{children}</ToastProvider>;

const ask = async (prompt: string) => {
  fireEvent.change(screen.getByPlaceholderText('What kind of book are you looking for?'), {
    target: { value: prompt },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Send message' }));
  await screen.findByText(/That's everything|No new recommendations/);
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
});
