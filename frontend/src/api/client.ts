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
