const API_BASE_URL = 'http://localhost:8080/api';

export interface BookSearchResult {
  title: string;
  author: string | null;
  isbn: string | null;
  coverUrl: string | null;
  publishYear: number | null;
  description: string | null;
}

export interface Book {
  id: number;
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
  const response = await fetch(`${API_BASE_URL}/books/search?q=${encodeURIComponent(query)}`);
  if (!response.ok) {
    throw new Error('Failed to search books');
  }
  return response.json();
};

export const getCollection = async (): Promise<Book[]> => {
  const response = await fetch(`${API_BASE_URL}/collection`);
  if (!response.ok) {
    throw new Error('Failed to fetch collection');
  }
  return response.json();
};

export const addBookToCollection = async (book: AddBookRequest): Promise<Book> => {
  const response = await fetch(`${API_BASE_URL}/collection`, {
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
  const response = await fetch(`${API_BASE_URL}/collection/${bookId}/rating?rating=${rating}`, {
    method: 'PATCH',
  });
  if (!response.ok) {
    throw new Error('Failed to update rating');
  }
  return response.json();
};

export const removeBookFromCollection = async (bookId: number): Promise<void> => {
  const response = await fetch(`${API_BASE_URL}/collection/${bookId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('Failed to remove book');
  }
};

export interface BookCandidate {
  title: string;
  author: string;
  isbn: string | null;
  genre: string;
  description: string;
  whySuggested: string;
}

export interface SuggestionResponse {
  suggestions: BookCandidate[];
  message: string;
}

export const getBookSuggestions = async (prompt: string): Promise<SuggestionResponse> => {
  const response = await fetch(`${API_BASE_URL}/suggestions`, {
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
  const response = await fetch(`${API_BASE_URL}/suggestions/stream`, {
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
