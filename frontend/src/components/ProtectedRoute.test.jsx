import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext } from '../auth/AuthContext.jsx';
import ProtectedRoute from './ProtectedRoute.jsx';

function renderAt(value) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/secret']}>
        <Routes>
          <Route path="/signin" element={<div>SIGNIN PAGE</div>} />
          <Route path="/forbidden" element={<div>FORBIDDEN PAGE</div>} />
          <Route path="/secret" element={<ProtectedRoute roles={['PLACEMENT_ADMIN']}><div>ADMIN SECRET</div></ProtectedRoute>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('ProtectedRoute', () => {
  it('shows a loading state while the session is being checked', () => {
    renderAt({ status: 'loading', user: null });
    expect(screen.getByRole('status')).toHaveTextContent(/checking your session/i);
    expect(screen.queryByText('ADMIN SECRET')).not.toBeInTheDocument();
  });

  it('redirects unauthenticated visitors to sign in', () => {
    renderAt({ status: 'unauthenticated', user: null });
    expect(screen.getByText('SIGNIN PAGE')).toBeInTheDocument();
  });

  it('sends a signed-in user with the wrong role to the access-denied page', () => {
    renderAt({ status: 'authenticated', user: { role: 'STUDENT' } });
    expect(screen.getByText('FORBIDDEN PAGE')).toBeInTheDocument();
    expect(screen.queryByText('ADMIN SECRET')).not.toBeInTheDocument();
  });

  it('renders the page for an allowed role', () => {
    renderAt({ status: 'authenticated', user: { role: 'PLACEMENT_ADMIN' } });
    expect(screen.getByText('ADMIN SECRET')).toBeInTheDocument();
  });
});
