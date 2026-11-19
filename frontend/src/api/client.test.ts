import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getCollection, getCurrentUser, setUnauthorizedHandler, streamBookSuggestions } from './client';

const jsonResponse = (status: number, body: unknown = []) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

describe('api client', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    setUnauthorizedHandler(null);
    vi.unstubAllGlobals();
  });

  it('sends same-origin credentials and the CSRF marker header to a relative /api URL', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200));
    await getCollection();

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/collection');
    expect(init.credentials).toBe('same-origin');
    expect(new Headers(init.headers).get('X-Requested-With')).toBe('bookshelf');
  });

  it('notifies the unauthorized handler on 401', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    fetchMock.mockResolvedValue(jsonResponse(401, {}));

    await expect(getCollection()).rejects.toThrow('Failed to fetch collection');
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('returns null from getCurrentUser when there is no session', async () => {
    fetchMock.mockResolvedValue(jsonResponse(401, {}));
    await expect(getCurrentUser()).resolves.toBeNull();
  });

  it('sends books already proposed in the conversation with a suggestion request', async () => {
    fetchMock.mockResolvedValue(
      new Response('event:done\ndata:done\n\n', { status: 200, headers: { 'Content-Type': 'text/event-stream' } }),
    );
    const alreadySuggested = [{ title: 'Dune', author: 'Frank Herbert', isbn: '111' }];

    await streamBookSuggestions('more', { onMessage: vi.fn(), onSuggestion: vi.fn(), alreadySuggested });

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/suggestions/stream');
    expect(JSON.parse(init.body as string)).toEqual({ prompt: 'more', alreadySuggested });
  });
});
