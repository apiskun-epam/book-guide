import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Toast, ToastAction, ToastVariant } from './Toast';
import './Toast.css';

export interface ShowToastOptions {
  variant?: ToastVariant;
  /** How long the toast stays visible. Defaults to 3000ms. */
  durationMs?: number;
  /** Optional button (e.g. Undo). Clicking it dismisses the toast. */
  action?: ToastAction;
}

interface ToastItem {
  id: number;
  message: string;
  variant: ToastVariant;
  action?: ToastAction;
  durationMs: number;
}

interface ToastContextValue {
  showToast: (message: string, options?: ShowToastOptions) => void;
}

const DEFAULT_DURATION_MS = 3000;

const ToastContext = createContext<ToastContextValue | null>(null);

/** Single place for all in-app notifications: stacked in the bottom-left corner. */
export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(0);
  const timers = useRef(new Map<number, ReturnType<typeof setTimeout>>());

  const dismiss = useCallback((id: number) => {
    const timer = timers.current.get(id);
    if (timer) clearTimeout(timer);
    timers.current.delete(id);
    setToasts((current) => current.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, { variant = 'success', durationMs = DEFAULT_DURATION_MS, action }: ShowToastOptions = {}) => {
      const id = nextId.current++;
      const wrappedAction = action && {
        label: action.label,
        onClick: () => {
          dismiss(id);
          action.onClick();
        },
      };
      setToasts((current) => [...current, { id, message, variant, action: wrappedAction, durationMs }]);
      timers.current.set(id, setTimeout(() => dismiss(id), durationMs));
    },
    [dismiss],
  );

  useEffect(() => {
    const pending = timers.current;
    return () => pending.forEach(clearTimeout);
  }, []);

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack">
        {toasts.map((t) => (
          <Toast
            key={t.id}
            message={t.message} variant={t.variant}
            action={t.action}
            countdownMs={t.action ? t.durationMs : undefined}
          />
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = (): ToastContextValue => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
};
