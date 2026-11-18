// Relative, so requests go through the Vite dev proxy and stay same-origin with the session cookie.
const API_BASE_URL = '/api';

let unauthorizedHandler: (() => void) | null = null;

/** Registered by AuthProvider: called whenever the API answers 401 (session missing or expired). */
export const setUnauthorizedHandler = (handler: (() => void) | null): void => {
  unauthorizedHandler = handler;
};

/**
 * Single entry point for API calls. The session lives in an HttpOnly cookie, so the browser sends
 * it automatically; the custom header is the CSRF marker the backend requires on every request.
 */
const apiFetch = async (path: string, init: RequestInit = {}): Promise<Response> => {
  const headers = new Headers(init.headers);
  headers.set('X-Requested-With', 'bookshelf');
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'same-origin',
  });
  if (response.status === 401) {
    unauthorizedHandler?.();
  }
  return response;
};

export interface AuthUser {
  uid: string;
  email: string | null;
  name: string | null;
  picture: string | null;
}

/** Returns the signed-in user, or null when there is no valid session. */
export const getCurrentUser = async (): Promise<AuthUser | null> => {
  const response = await apiFetch('/auth/me');
  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    throw new Error('Failed to load session');
  }
  return response.json();
};

/** Exchanges a Firebase ID token for the server-side session cookie. */
export const createSession = async (idToken: string): Promise<AuthUser> => {
  const response = await apiFetch('/auth/session', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  });
  if (!response.ok) {
    throw new Error('Failed to sign in');
  }
  return response.json();
};

/** Revokes the session on the server and clears the cookie. */
export const logout = async (): Promise<void> => {
  const response = await apiFetch('/auth/logout', { method: 'POST' });
  if (!response.ok) {
    throw new Error('Failed to sign out');
  }
};

export interface BookSearchResult {
  title: string;
  author: string | null;
  isbn: string | null;
  coverUrl: string | null;
  publishYear: number | null;
  description: string | null;
}

export type ListType = 'COLLECTION' | 'READING_LIST';

export interface Book {
  id: number;
  listType: ListType;
  title: string;
  author: string | null;
  isbn: string | null;
  coverUrl: string | null;
  publishYear: number | null;
  description: string | null;
  rating: number | null;
  addedAt: string;
  updatedAt: string;
}

export interface AddBookRequest {
  title: string;
  author?: string;
  isbn?: string;
  coverUrl?: string;
  publishYear?: number;
  description?: string;
  rating?: number;
}

export const searchBooks = async (query: string): Promise<BookSearchResult[]> => {
  const response = await apiFetch(`/books/search?q=${encodeURIComponent(query)}`);
  if (!response.ok) {
    throw new Error('Failed to search books');
  }
  return response.json();
};

export const getCollection = async (): Promise<Book[]> => {
  const response = await apiFetch(`/collection`);
  if (!response.ok) {
    throw new Error('Failed to fetch collection');
  }
  return response.json();
};

export const addBookToCollection = async (book: AddBookRequest): Promise<Book> => {
  const response = await apiFetch(`/collection`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(book),
  });
  if (!response.ok) {
    throw new Error('Failed to add book to collection');
  }
  return response.json();
};

export const updateBookRating = async (bookId: number, rating: number): Promise<Book> => {
  const response = await apiFetch(`/collection/${bookId}/rating?rating=${rating}`, {
    method: 'PATCH',
  });
  if (!response.ok) {
    throw new Error('Failed to update rating');
  }
  return response.json();
};

export const removeBookFromCollection = async (bookId: number): Promise<void> => {
  const response = await apiFetch(`/collection/${bookId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('Failed to remove book');
  }
};

export const undoBookRemoval = async (bookId: number): Promise<Book> => {
  const response = await apiFetch(`/collection/${bookId}/undo-removal`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error('Failed to undo removal');
  }
  return response.json();
};

export const getReadingList = async (): Promise<Book[]> => {
  const response = await apiFetch(`/reading-list`);
  if (!response.ok) {
    throw new Error('Failed to fetch reading list');
  }
  return response.json();
};

export const addBookToReadingList = async (book: AddBookRequest): Promise<Book> => {
  const response = await apiFetch(`/reading-list`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(book),
  });
  if (!response.ok) {
    throw new Error('Failed to add book to reading list');
  }
  return response.json();
};

export const removeBookFromReadingList = async (bookId: number): Promise<void> => {
  const response = await apiFetch(`/reading-list/${bookId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('Failed to remove book');
  }
};

export const undoReadingListRemoval = async (bookId: number): Promise<Book> => {
  const response = await apiFetch(`/reading-list/${bookId}/undo-removal`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error('Failed to undo removal');
  }
  return response.json();
};

/** Moves a reading-list book into the collection, with an optional rating. */
export const moveToCollection = async (bookId: number, rating?: number): Promise<Book> => {
  const response = await apiFetch(`/reading-list/${bookId}/move-to-collection`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ rating }),
  });
  if (!response.ok) {
    throw new Error('Failed to move book to collection');
  }
  return response.json();
};

export interface BookCandidate {
  title: string;
  author: string;
  isbn: string | null;
  genre: string;
  description: string;
  whySuggested: string;
  coverUrl?: string | null;
}

export interface SuggestionResponse {
  suggestions: BookCandidate[];
  message: string;
}

export const getBookSuggestions = async (prompt: string): Promise<SuggestionResponse> => {
  const response = await apiFetch(`/suggestions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ prompt }),
  });
  if (!response.ok) {
    throw new Error('Failed to get book suggestions');
  }
  return response.json();
};

export interface SuggestionStreamHandlers {
  onMessage: (message: string) => void;
  onSuggestion: (book: BookCandidate) => void;
  signal?: AbortSignal;
}

/**
 * Streams suggestions over Server-Sent Events (POST, so fetch is used instead of EventSource).
 * Resolves when the server sends "done"; rejects on an "error" event or network failure.
 */
export const streamBookSuggestions = async (
  prompt: string,
  { onMessage, onSuggestion, signal }: SuggestionStreamHandlers,
): Promise<void> => {
  const response = await apiFetch(`/suggestions/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify({ prompt }),
    signal,
  });
  if (!response.ok || !response.body) {
    throw new Error('Failed to get book suggestions');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  const handleEvent = (raw: string): boolean => {
    let event = 'message';
    const dataLines: string[] = [];
    for (const line of raw.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim();
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''));
    }
    const data = dataLines.join('\n');
    if (event === 'message') onMessage(data);
    else if (event === 'suggestion') onSuggestion(JSON.parse(data) as BookCandidate);
    else if (event === 'error') throw new Error(data || 'Failed to get book suggestions');
    return event === 'done';
  };

  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
    let sep: number;
    while ((sep = buffer.indexOf('\n\n')) !== -1) {
      const raw = buffer.slice(0, sep);
      buffer = buffer.slice(sep + 2);
      if (raw.trim() && handleEvent(raw)) return;
    }
  }
};
