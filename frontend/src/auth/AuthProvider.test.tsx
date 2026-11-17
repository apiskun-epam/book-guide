import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { signInWithPopup, signOut as firebaseSignOut } from 'firebase/auth';
import { AuthProvider, useAuth } from './AuthProvider';
import * as api from '../api/client';

vi.mock('../api/client');
vi.mock('./firebase', () => ({ auth: {}, googleProvider: {} }));
vi.mock('firebase/auth');

const alice: api.AuthUser = { uid: 'uid-a', email: 'a@example.com', name: 'Alice', picture: null };

const Probe = () => {
  const { status, user, signIn, signOut } = useAuth();
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="user">{user?.uid ?? 'none'}</span>
      <button onClick={() => signIn().catch(() => undefined)}>sign in</button>
      <button onClick={() => signOut().catch(() => undefined)}>sign out</button>
    </div>
  );
};

const setup = () =>
  render(
    <MemoryRouter>
      <AuthProvider>
        <Probe />
      </AuthProvider>
    </MemoryRouter>,
  );

const status = () => screen.getByTestId('status').textContent;

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(signInWithPopup).mockResolvedValue({
      user: { getIdToken: () => Promise.resolve('id-token') },
    } as never);
    vi.mocked(firebaseSignOut).mockResolvedValue(undefined);
  });

  it('restores a signed-in user from the session cookie', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue(alice);
    setup();
    await waitFor(() => expect(status()).toBe('authenticated'));
    expect(screen.getByTestId('user').textContent).toBe('uid-a');
  });

  it('is unauthenticated when there is no session', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue(null);
    setup();
    await waitFor(() => expect(status()).toBe('unauthenticated'));
  });

  it('exchanges the Google ID token for a server session on sign-in', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue(null);
    vi.mocked(api.createSession).mockResolvedValue(alice);
    setup();
    await waitFor(() => expect(status()).toBe('unauthenticated'));

    fireEvent.click(screen.getByText('sign in'));

    await waitFor(() => expect(status()).toBe('authenticated'));
    expect(api.createSession).toHaveBeenCalledWith('id-token');
  });

  it('logs out through the server and returns to unauthenticated', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue(alice);
    vi.mocked(api.logout).mockResolvedValue();
    setup();
    await waitFor(() => expect(status()).toBe('authenticated'));

    fireEvent.click(screen.getByText('sign out'));

    await waitFor(() => expect(status()).toBe('unauthenticated'));
    expect(api.logout).toHaveBeenCalledTimes(1);
  });

  it('stays signed in when the server cannot revoke the session', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue(alice);
    vi.mocked(api.logout).mockRejectedValue(new Error('x'));
    setup();
    await waitFor(() => expect(status()).toBe('authenticated'));

    fireEvent.click(screen.getByText('sign out'));

    await waitFor(() => expect(api.logout).toHaveBeenCalled());
    expect(status()).toBe('authenticated');
  });

  it('drops to unauthenticated when the API reports an expired session', async () => {
    vi.mocked(api.getCurrentUser).mockResolvedValue(alice);
    setup();
    await waitFor(() => expect(status()).toBe('authenticated'));

    const handler = vi.mocked(api.setUnauthorizedHandler).mock.calls[0]?.[0];
    act(() => handler?.());

    expect(status()).toBe('unauthenticated');
  });
});
