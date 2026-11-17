import React, { useState } from 'react';
import { useAuth } from './AuthProvider';
import { useToast } from '../components/ToastProvider';
import { GoogleIcon } from '../components/icons';
import './AuthPage.css';

/** Error codes that mean the user simply closed or abandoned the popup. */
const SILENT_ERROR_CODES = ['auth/popup-closed-by-user', 'auth/cancelled-popup-request'];

/** Shown instead of the whole app while signed out: one centered modal, nothing else. */
const AuthPage: React.FC = () => {
  const { signIn } = useAuth();
  const { showToast } = useToast();
  const [busy, setBusy] = useState(false);

  const handleSignIn = async () => {
    setBusy(true);
    try {
      await signIn();
    } catch (error) {
      const code = (error as { code?: string } | null)?.code;
      if (!code || !SILENT_ERROR_CODES.includes(code)) {
        console.error('Sign-in failed:', code ?? 'unknown error');
        showToast('Could not sign in. Please try again.', { variant: 'error' });
      }
      setBusy(false);
    }
  };

  return (
    <div className="auth-overlay">
      <div className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <h1 id="auth-title" className="auth-title">BookShelf AI</h1>
        <p className="auth-text">Sign in to see your book collection.</p>
        <button type="button" className="auth-google-button" onClick={handleSignIn} disabled={busy}>
          <GoogleIcon size={20} />
          {busy ? 'Signing in...' : 'Continue with Google'}
        </button>
      </div>
    </div>
  );
};

export default AuthPage;
