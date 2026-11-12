import { useCallback, useEffect, useRef, useState } from 'react';
import type { ToastVariant } from './Toast';

export interface ToastState {
  message: string;
  variant: ToastVariant;
}

/** Shows one toast at a time and clears it automatically after `durationMs`. */
export const useToast = (durationMs = 3000) => {
  const [toast, setToast] = useState<ToastState | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearTimer = () => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  };

  const showToast = useCallback(
    (message: string, variant: ToastVariant = 'success') => {
      clearTimer();
      setToast({ message, variant });
      timer.current = setTimeout(() => setToast(null), durationMs);
    },
    [durationMs],
  );

  useEffect(() => clearTimer, []);

  return { toast, showToast };
};
