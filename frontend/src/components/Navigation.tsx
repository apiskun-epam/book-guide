import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { LibraryIcon, SparkIcon } from './icons';
import './Navigation.css';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link active' : 'nav-link';

/** Both book lists live under the "Your Collection" entry. */
const BOOK_LIST_PATHS = ['/collection', '/reading-list'];

const Navigation: React.FC = () => {
  const { pathname } = useLocation();
  const onBookList = BOOK_LIST_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));

  return (
    <nav className="navigation" aria-label="Primary">
      <div className="nav-brand">
        <h1>BookShelf</h1>
      </div>
      <div className="nav-links">
        <NavLink to="/collection" className={() => linkClass({ isActive: onBookList })}>
          <LibraryIcon />
          <span>Your Collection</span>
        </NavLink>
        <NavLink to="/discover" className={linkClass}>
          <SparkIcon />
          <span>Find Your Next Book</span>
        </NavLink>
      </div>
    </nav>
  );
};

export default Navigation;
