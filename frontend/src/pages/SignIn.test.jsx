import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client.js';
import { AuthContext } from '../auth/AuthContext.jsx';
import SignIn from './SignIn.jsx';

function setup(login) {
  render(
    <AuthContext.Provider value={{ status: 'unauthenticated', user: null, login }}>
      <MemoryRouter initialEntries={['/signin']}>
        <Routes>
          <Route path="/signin" element={<SignIn />} />
          <Route path="/student" element={<div>STUDENT HOME</div>} />
          <Route path="/admin" element={<div>ADMIN HOME</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}
const fill = (email, password) => {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
};

describe('SignIn', () => {
  it('shows field errors and does not call the server for empty input', () => {
    const login = vi.fn();
    setup(login);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(screen.getByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('rejects a malformed email locally', () => {
    const login = vi.fn();
    setup(login);
    fill('not-an-email', 'whatever');
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it('redirects to the area for the signed-in role', async () => {
    const login = vi.fn().mockResolvedValue({ role: 'PLACEMENT_ADMIN' });
    setup(login);
    fill(' admin@example.edu ', 'Passw0rd-Strong1');
    expect(await screen.findByText('ADMIN HOME')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('admin@example.edu', 'Passw0rd-Strong1');
  });

  it('shows a generic message when the credentials are wrong', async () => {
    setup(vi.fn().mockRejectedValue(new ApiError('Invalid email or password.', { status: 401 })));
    fill('a@example.edu', 'Wrong-Passw0rd');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Invalid email or password.'));
  });

  it('shows a helpful message when the server is unreachable', async () => {
    setup(vi.fn().mockRejectedValue(new ApiError('Cannot reach the server. Check that the backend is running.', { status: 0 })));
    fill('a@example.edu', 'Passw0rd-Strong1');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(/cannot reach the server/i));
  });
});
