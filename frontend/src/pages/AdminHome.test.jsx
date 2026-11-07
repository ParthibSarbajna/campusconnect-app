import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../auth/AuthContext.jsx';
import AdminHome from './AdminHome.jsx';

vi.mock('../api/accounts.js', () => ({
  listDepartments: vi.fn(),
  listUsers: vi.fn(),
  createDepartment: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
}));
import * as api from '../api/accounts.js';

const admin = { status: 'authenticated', user: { id: 9, fullName: 'Admin', role: 'PLACEMENT_ADMIN' } };
const student = { id: 5, email: 'kabir@example.edu', fullName: 'Kabir Das', role: 'STUDENT', departmentId: 1, enabled: true };

beforeEach(() => {
  vi.clearAllMocks();
  api.listDepartments.mockResolvedValue([
    { id: 1, code: 'CSE', name: 'Computer Science', active: true },
    { id: 2, code: 'IT', name: 'Information Technology', active: true },
  ]);
  api.listUsers.mockResolvedValue({ items: [student], page: 0, size: 25, total: 1 });
  api.updateUser.mockResolvedValue({});
});

async function renderPage() {
  render(<AuthContext.Provider value={admin}><AdminHome /></AuthContext.Provider>);
  return screen.findByLabelText('Department for kabir@example.edu');
}

describe('AdminHome account department assignment', () => {
  it('sends departmentId: null when the department is set to None', async () => {
    const select = await renderPage();
    fireEvent.change(select, { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(api.updateUser).toHaveBeenCalledWith(5, { role: 'STUDENT', departmentId: null, enabled: true }));
  });

  it('sends the numeric id when another department is chosen', async () => {
    const select = await renderPage();
    fireEvent.change(select, { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(api.updateUser).toHaveBeenCalledWith(5, { role: 'STUDENT', departmentId: 2, enabled: true }));
  });

  it('shows the server error when an update is rejected', async () => {
    api.updateUser.mockRejectedValue(new Error('A department is required for a Department SPOC.'));
    const select = await renderPage();
    fireEvent.change(select, { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('A department is required for a Department SPOC.');
  });
});
