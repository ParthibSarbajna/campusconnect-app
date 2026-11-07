import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../auth/AuthContext.jsx';
import Layout from './Layout.jsx';

function renderLayout(value) {
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/x']}>
        <Routes><Route element={<Layout />}><Route path="/x" element={<div>BODY</div>} /><Route path="/" element={<div>HOME</div>} /></Route></Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}
const as = (role) => ({ status: 'authenticated', user: { fullName: 'Pat Lee', role }, logout: vi.fn().mockResolvedValue() });

describe('Layout navigation', () => {
  it.each([
    ['STUDENT', 'My account', ['Department', 'Administration']],
    ['DEPARTMENT_SPOC', 'Department', ['My account', 'Administration']],
    ['PLACEMENT_ADMIN', 'Administration', ['My account', 'Department']],
  ])('%s sees only its own link', (role, shown, hidden) => {
    renderLayout(as(role));
    expect(screen.getByRole('link', { name: shown })).toBeInTheDocument();
    hidden.forEach((name) => expect(screen.queryByRole('link', { name })).not.toBeInTheDocument());
  });

  it('shows sign-in and register links when signed out', () => {
    renderLayout({ status: 'unauthenticated', user: null });
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Register' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Sign out' })).not.toBeInTheDocument();
  });

  it('signs out and returns to the landing page', async () => {
    const value = as('STUDENT');
    renderLayout(value);
    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    await waitFor(() => expect(value.logout).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('HOME')).toBeInTheDocument();
  });
});
