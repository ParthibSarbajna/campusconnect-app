import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest } from '../api/client.js';
import { ApiError } from '../api/client.js';
import { AuthProvider, useAuth } from './AuthContext.jsx';

vi.mock('../api/auth.js', () => ({ fetchMe: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() }));
import * as authApi from '../api/auth.js';

const student = { id: 1, email: 'asha@example.edu', fullName: 'Asha Roy', role: 'STUDENT', departmentId: null, enabled: true };

function Probe() {
  const a = useAuth();
  return (
    <div>
      <span data-testid="status">{a.status}</span>
      <span data-testid="name">{a.user ? a.user.fullName : ''}</span>
      <button onClick={() => a.login('asha@example.edu', 'pw').catch(() => {})}>in</button>
      <button onClick={() => a.logout()}>out</button>
    </div>
  );
}
const renderProbe = () => render(<AuthProvider><Probe /></AuthProvider>);

beforeEach(() => vi.clearAllMocks());

describe('AuthProvider', () => {
  it('starts loading, then unauthenticated when there is no session', async () => {
    authApi.fetchMe.mockRejectedValue(new ApiError('Not signed in.', { status: 401 }));
    renderProbe();
    expect(screen.getByTestId('status')).toHaveTextContent('loading');
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'));
  });

  it('becomes authenticated when the server reports a session', async () => {
    authApi.fetchMe.mockResolvedValue(student);
    renderProbe();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));
    expect(screen.getByTestId('name')).toHaveTextContent('Asha Roy');
  });

  it('logs in, then logs out and clears the user', async () => {
    authApi.fetchMe.mockRejectedValue(new ApiError('Not signed in.', { status: 401 }));
    authApi.login.mockResolvedValue(student);
    authApi.logout.mockResolvedValue(null);
    renderProbe();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'));
    fireEvent.click(screen.getByText('in'));
    await waitFor(() => expect(screen.getByTestId('name')).toHaveTextContent('Asha Roy'));
    fireEvent.click(screen.getByText('out'));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'));
    expect(screen.getByTestId('name')).toHaveTextContent('');
    expect(authApi.logout).toHaveBeenCalledTimes(1);
  });

  it('drops to unauthenticated when a later request reports an expired session', async () => {
    authApi.fetchMe.mockResolvedValue(student);
    renderProbe();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));
    global.fetch = vi.fn(() => Promise.resolve({ ok: false, status: 401, text: () => Promise.resolve('{"detail":"Not signed in."}') }));
    await act(async () => { await apiRequest('/anything').catch(() => {}); });
    expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated');
  });
});
