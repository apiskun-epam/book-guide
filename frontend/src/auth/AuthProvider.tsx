import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signInWithPopup, signOut as firebaseSignOut } from 'firebase/auth';
import { AuthUser, createSession, getCurrentUser, logout, setUnauthorizedHandler } from '../api/client';
import { auth, googleProvider } from './firebase';

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

interface AuthContextValue {
  status: AuthStatus;
  user: AuthUser | null;
  /** Opens the Google popup and starts a server session. Rejects on failure. */
  signIn: () => Promise<void>;
  /** Revokes the server session. Rejects (and stays signed in) if the server could not do it. */
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<AuthUser | null>(null);

  const clearSession = useCallback(() => {
    setUser(null);
    setStatus('unauthenticated');
  }, []);

  // Restore the session from the cookie on load, and react to the API reporting it expired.
  useEffect(() => {
    let cancelled = false;
    setUnauthorizedHandler(clearSession);
    getCurrentUser()
      .then((current) => {
        if (cancelled) return;
        if (current) {
          setUser(current);
          setStatus('authenticated');
        } else {
          clearSession();
        }
      })
      .catch(() => {
        if (!cancelled) clearSession();
      });
    return () => {
      cancelled = true;
      setUnauthorizedHandler(null);
    };
  }, [clearSession]);

  const signIn = useCallback(async () => {
    const credential = await signInWithPopup(auth, googleProvider);
    const idToken = await credential.user.getIdToken();
    const current = await createSession(idToken);
    // The server cookie is now the session; drop Firebase's client-side state.
    await firebaseSignOut(auth).catch(() => undefined);
    setUser(current);
    setStatus('authenticated');
    navigate('/collection', { replace: true });
  }, [navigate]);

  const signOut = useCallback(async () => {
    await logout();
    clearSession();
  }, [clearSession]);

  const value = useMemo(() => ({ status, user, signIn, signOut }), [status, user, signIn, signOut]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
};
