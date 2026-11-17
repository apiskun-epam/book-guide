import React from 'react';
import SearchBar from './SearchBar';
import { useToast } from './ToastProvider';
import { useCollectionRefresh } from './CollectionRefresh';
import { useAuth } from '../auth/AuthProvider';
import { LogoutIcon } from './icons';
import './TopBar.css';

/** Shared header on every page: search on the left, user and Log out on the top right. */
const TopBar: React.FC = () => {
  const { user, signOut } = useAuth();
  const { showToast } = useToast();
  const { notifyBookAdded } = useCollectionRefresh();

  const handleLogout = async () => {
    try {
      await signOut();
    } catch (error) {
      console.error('Logout failed:', error);
      showToast('Could not log out. Please try again.', { variant: 'error' });
    }
  };

  return (
    <header className="top-bar">
      <SearchBar onBookAdded={notifyBookAdded} />
      <div className="top-bar__user">
        {user?.picture && (
          <img src={user.picture} alt="" className="top-bar__avatar" referrerPolicy="no-referrer" />
        )}
        {(user?.name || user?.email) && (
          <span className="top-bar__name">{user.name ?? user.email}</span>
        )}
        <button type="button" className="top-bar__logout" onClick={handleLogout}>
          <LogoutIcon />
          Log out
        </button>
      </div>
    </header>
  );
};

export default TopBar;
