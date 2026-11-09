import React from 'react';
import { NavLink } from 'react-router-dom';
import { LibraryIcon, SparkIcon } from './icons';
import './Navigation.css';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link active' : 'nav-link';

const Navigation: React.FC = () => {
  return (
    <nav className="navigation" aria-label="Primary">
      <div className="nav-brand">
        <h1>BookShelf</h1>
      </div>
      <div className="nav-links">
        <NavLink to="/collection" className={linkClass}>
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
