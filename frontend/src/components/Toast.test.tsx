import React from 'react';
import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Toast } from './Toast';
import { useToast } from './useToast';

const Harness: React.FC = () => {
  const { toast, showToast } = useToast(3000);
  return (
    <>
      <button onClick={() => showToast('Saved')}>go</button>
      {toast && <Toast message={toast.message} variant={toast.variant} />}
    </>
  );
};

describe('useToast', () => {
  beforeEach(() => { vi.useFakeTimers(); });
  afterEach(() => { vi.useRealTimers(); });

  it('shows the toast and hides it after 3 seconds', () => {
    render(<Harness />);
    act(() => screen.getByText('go').click());
    expect(screen.getByRole('status').textContent).toBe('Saved');

    act(() => vi.advanceTimersByTime(2999));
    expect(screen.queryByRole('status')).not.toBeNull();

    act(() => vi.advanceTimersByTime(1));
    expect(screen.queryByRole('status')).toBeNull();
  });
});
