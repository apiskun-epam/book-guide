import React from 'react';
import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider, useToast } from './ToastProvider';

const onUndo = vi.fn();

const Harness: React.FC = () => {
  const { showToast } = useToast();
  return (
    <>
      <button onClick={() => showToast('Saved')}>plain</button>
      <button onClick={() => showToast('Boom', { variant: 'error' })}>error</button>
      <button
        onClick={() => showToast('Removed', { durationMs: 5000, action: { label: 'Undo', onClick: onUndo } })}
      >
        action
      </button>
    </>
  );
};

const setup = () => render(<Harness />, { wrapper: ToastProvider });

describe('ToastProvider', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    onUndo.mockClear();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows a toast and hides it after 3 seconds by default', () => {
    setup();
    act(() => screen.getByText('plain').click());
    expect(screen.getByRole('status').textContent).toBe('Saved');

    act(() => vi.advanceTimersByTime(2999));
    expect(screen.queryByRole('status')).not.toBeNull();

    act(() => vi.advanceTimersByTime(1));
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('announces errors with the alert role', () => {
    setup();
    act(() => screen.getByText('error').click());
    expect(screen.getByRole('alert').textContent).toBe('Boom');
  });

  it('stacks multiple toasts independently', () => {
    setup();
    act(() => screen.getByText('plain').click());
    act(() => screen.getByText('action').click());
    expect(screen.getAllByRole('status')).toHaveLength(2);

    act(() => vi.advanceTimersByTime(3000)); // the 3s toast expires, the 5s one remains
    expect(screen.getAllByRole('status')).toHaveLength(1);
    expect(screen.getByRole('status').textContent).toContain('Removed');
  });

  it('runs the action and dismisses the toast when its button is clicked', () => {
    setup();
    act(() => screen.getByText('action').click());
    act(() => screen.getByRole('button', { name: 'Undo' }).click());

    expect(onUndo).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('shows a countdown line matching the duration only on action toasts', () => {
    setup();
    act(() => screen.getByText('plain').click());
    expect(screen.queryByTestId('toast-countdown')).toBeNull();

    act(() => screen.getByText('action').click());
    expect(screen.getByTestId('toast-countdown').style.animationDuration).toBe('5000ms');
  });

  it('keeps an action toast for its custom duration', () => {
    setup();
    act(() => screen.getByText('action').click());
    act(() => vi.advanceTimersByTime(4999));
    expect(screen.queryByRole('status')).not.toBeNull();
    act(() => vi.advanceTimersByTime(1));
    expect(screen.queryByRole('status')).toBeNull();
  });
});
