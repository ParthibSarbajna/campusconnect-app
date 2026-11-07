import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client.js';
import { AuthContext } from '../auth/AuthContext.jsx';
import Register from './Register.jsx';

function setup(register) {
  render(
    <AuthContext.Provider value={{ status: 'unauthenticated', user: null, register }}>
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<Register />} />
          <Route path="/signin" element={<div>SIGNIN PAGE</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}
const type = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
const submit = () => fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
function fillValid() {
  type('Full name', '  Asha Roy ');
  type('Email', 'asha@example.edu');
  type('Password', 'Passw0rd-Strong1');
  type('Confirm password', 'Passw0rd-Strong1');
}

describe('Register', () => {
  it('validates every field before calling the server', () => {
    const register = vi.fn();
    setup(register);
    submit();
    expect(screen.getByText('Enter your full name.')).toBeInTheDocument();
    expect(screen.getByText('Enter your email address.')).toBeInTheDocument();
    expect(screen.getByText('Enter a password.')).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it('enforces the password rule and the confirmation match', () => {
    const register = vi.fn();
    setup(register);
    type('Full name', 'Asha Roy');
    type('Email', 'asha@example.edu');
    type('Password', 'onlyletterslong');
    type('Confirm password', 'different');
    submit();
    expect(screen.getByText('Include at least one letter and one digit.')).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it('submits trimmed values and moves to sign-in on success', async () => {
    const register = vi.fn().mockResolvedValue({});
    setup(register);
    fillValid();
    submit();
    expect(await screen.findByText('SIGNIN PAGE')).toBeInTheDocument();
    expect(register).toHaveBeenCalledWith({ fullName: 'Asha Roy', email: 'asha@example.edu', password: 'Passw0rd-Strong1' });
  });

  it('shows a field error when the email is already registered', async () => {
    setup(vi.fn().mockRejectedValue(new ApiError('An account with this email already exists.', { status: 409 })));
    fillValid();
    submit();
    await waitFor(() => expect(screen.getByText('An account with this email already exists.')).toBeInTheDocument());
  });
});
