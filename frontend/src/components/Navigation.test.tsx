import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import Navigation from './Navigation';

const renderAt = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Navigation />
    </MemoryRouter>
  );

const link = (name: string) => screen.getByRole('link', { name });

describe('Navigation', () => {
  it.each(['/collection', '/reading-list'])('highlights Your Collection on %s', (path) => {
    renderAt(path);
    expect(link('Your Collection')).toHaveClass('active');
    expect(link('Find Your Next Book')).not.toHaveClass('active');
  });

  it('highlights Find Your Next Book on /discover only', () => {
    renderAt('/discover');
    expect(link('Find Your Next Book')).toHaveClass('active');
    expect(link('Your Collection')).not.toHaveClass('active');
  });
});
