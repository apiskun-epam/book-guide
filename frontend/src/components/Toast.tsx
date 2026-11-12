import React from 'react';
import './Toast.css';

export type ToastVariant = 'success' | 'error';

interface ToastProps {
  message: string;
  variant?: ToastVariant;
}

/** Non-blocking notification pinned to the bottom-left of the screen. */
export const Toast: React.FC<ToastProps> = ({ message, variant = 'success' }) => (
  <div
    className={`toast toast--${variant}`}
    role={variant === 'error' ? 'alert' : 'status'}
    aria-live={variant === 'error' ? 'assertive' : 'polite'}
  >
    {message}
  </div>
);
