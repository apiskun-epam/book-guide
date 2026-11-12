import React from 'react';
import './Toast.css';

export type ToastVariant = 'success' | 'error';

export interface ToastAction {
  label: string;
  onClick: () => void;
}

interface ToastProps {
  message: string;
  variant?: ToastVariant;
  action?: ToastAction;
  /** When set, a thin line along the bottom shrinks to zero over this many ms. */
  countdownMs?: number;
}

/** A single notification. Render it through `ToastProvider`, never directly. */
export const Toast: React.FC<ToastProps> = ({ message, variant = 'success', action, countdownMs }) => (
  <div
    className={`toast toast--${variant}`}
    role={variant === 'error' ? 'alert' : 'status'}
    aria-live={variant === 'error' ? 'assertive' : 'polite'}
  >
    <span className="toast__message">{message}</span>
    {action && (
      <button type="button" className="toast__action" onClick={action.onClick}>
        {action.label}
      </button>
    )}
    {countdownMs !== undefined && (
      <span
        className="toast__countdown"
        data-testid="toast-countdown"
        style={{ animationDuration: `${countdownMs}ms` }}
        aria-hidden="true"
      />
    )}
  </div>
);
