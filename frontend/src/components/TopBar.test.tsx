import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TopBar from './TopBar';
import { ToastProvider } from './ToastProvider';
import { useAuth } from '../auth/AuthProvider';

vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }));
vi.mock('./SearchBar', () => ({ default: () => <div>search</div> }));

const signOut = vi.fn();

const setup = () => render(<TopBar />, { wrapper: ToastProvider });

describe('TopBar', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      status: 'authenticated',
      user: { uid: 'uid-a', email: 'a@example.com', name: 'Alice', picture: null },
      signIn: vi.fn(),
      signOut,
    });
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('shows the user and a Log out button', () => {
    setup();
    expect(screen.getByText('Alice')).toBeTruthy();
    expect(screen.getByRole('button', { name: /log out/i })).toBeTruthy();
  });

  it('signs out when Log out is clicked', async () => {
    signOut.mockResolvedValue(undefined);
    setup();
    fireEvent.click(screen.getByRole('button', { name: /log out/i }));
    await waitFor(() => expect(signOut).toHaveBeenCalledTimes(1));
  });

  it('shows an error toast when logout fails', async () => {
    signOut.mockRejectedValue(new Error('x'));
    setup();
    fireEvent.click(screen.getByRole('button', { name: /log out/i }));
    expect((await screen.findByRole('alert')).textContent).toContain('Could not log out');
  });
});
