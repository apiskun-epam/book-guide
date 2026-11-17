import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AuthPage from './AuthPage';
import { ToastProvider } from '../components/ToastProvider';
import { useAuth } from './AuthProvider';

vi.mock('./AuthProvider', () => ({ useAuth: vi.fn() }));

const signIn = vi.fn();

const setup = () => render(<AuthPage />, { wrapper: ToastProvider });

describe('AuthPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      status: 'unauthenticated',
      user: null,
      signIn,
      signOut: vi.fn(),
    });
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('shows only a modal with a Google sign-in button', () => {
    setup();
    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(screen.getByRole('button', { name: /continue with google/i })).toBeTruthy();
  });

  it('starts sign-in when the button is clicked', async () => {
    signIn.mockResolvedValue(undefined);
    setup();
    fireEvent.click(screen.getByRole('button', { name: /continue with google/i }));
    await waitFor(() => expect(signIn).toHaveBeenCalledTimes(1));
  });

  it('shows an error toast when sign-in fails', async () => {
    signIn.mockRejectedValue({ code: 'auth/network-request-failed' });
    setup();
    fireEvent.click(screen.getByRole('button', { name: /continue with google/i }));
    expect((await screen.findByRole('alert')).textContent).toContain('Could not sign in');
    expect(screen.getByRole('button', { name: /continue with google/i })).toHaveProperty('disabled', false);
  });

  it('stays quiet when the user closes the popup', async () => {
    signIn.mockRejectedValue({ code: 'auth/popup-closed-by-user' });
    setup();
    fireEvent.click(screen.getByRole('button', { name: /continue with google/i }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /continue with google/i })).toHaveProperty('disabled', false),
    );
    expect(screen.queryByRole('alert')).toBeNull();
  });
});
