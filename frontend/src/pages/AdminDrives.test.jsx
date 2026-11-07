import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AdminDrives, { validateDrive } from './AdminDrives.jsx';

vi.mock('../api/companies.js', () => ({ listCompanies: vi.fn() }));
vi.mock('../api/drives.js', () => ({ listAdminDrives: vi.fn(), createDrive: vi.fn(), changeDriveStatus: vi.fn() }));
import * as companies from '../api/companies.js';
import * as api from '../api/drives.js';

const drive = (id, status) => ({
  id, companyId: 1, companyName: 'Acme Corp', title: `Role ${id}`, graduationYear: 2027, minCgpa: 7.5, maxActiveBacklogs: 0,
  branches: ['CSE'], applicationDeadline: '2030-01-01T00:00:00Z', status,
});
const type = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

beforeEach(() => {
  vi.clearAllMocks();
  companies.listCompanies.mockResolvedValue([{ id: 1, name: 'Acme Corp', active: true }, { id: 2, name: 'Dormant Ltd', active: false }]);
  api.listAdminDrives.mockResolvedValue({ items: [drive(5, 'DRAFT'), drive(6, 'OPEN'), drive(7, 'CLOSED')], page: 0, size: 25, total: 3 });
  api.changeDriveStatus.mockResolvedValue({});
  api.createDrive.mockResolvedValue({});
});
afterEach(() => vi.restoreAllMocks());

describe('validateDrive', () => {
  const ok = { companyId: '1', title: 'Engineer', graduationYear: '2027', minCgpa: '7.5', maxActiveBacklogs: '0', ctcLpa: '', applicationDeadline: '2999-01-01T10:00' };
  it('accepts a valid form', () => expect(validateDrive(ok)).toEqual({}));
  it('flags every bad field', () => {
    const errs = validateDrive({ ...ok, companyId: '', title: ' ', graduationYear: '1999', minCgpa: '11', maxActiveBacklogs: '-1', applicationDeadline: '2000-01-01T10:00' });
    expect(Object.keys(errs).sort()).toEqual(['applicationDeadline', 'companyId', 'graduationYear', 'maxActiveBacklogs', 'minCgpa', 'title']);
  });
});

describe('AdminDrives', () => {
  it('only offers active companies in the form', async () => {
    render(<AdminDrives />);
    await screen.findAllByText('Role 5');
    expect(screen.getByRole('option', { name: 'Acme Corp' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Dormant Ltd' })).not.toBeInTheDocument();
  });

  it('shows field errors and does not call the server for an empty form', async () => {
    render(<AdminDrives />);
    await screen.findAllByText('Role 5');
    fireEvent.click(screen.getByRole('button', { name: 'Save draft' }));
    expect(screen.getByText('Choose a company.')).toBeInTheDocument();
    expect(screen.getByText('Enter the role title.')).toBeInTheDocument();
    expect(api.createDrive).not.toHaveBeenCalled();
  });

  it('submits a clean request body', async () => {
    render(<AdminDrives />);
    await screen.findAllByText('Role 5');
    fireEvent.change(screen.getByLabelText('Company'), { target: { value: '1' } });
    type('Role title', ' Engineer ');
    type('Batch (graduation year)', '2027');
    type('Application deadline', '2999-01-01T10:00');
    type('Minimum CGPA (optional)', '7.5');
    type('Eligible branches, comma separated (empty = all)', 'CSE, IT, ');
    fireEvent.click(screen.getByRole('button', { name: 'Save draft' }));
    await waitFor(() => expect(api.createDrive).toHaveBeenCalledTimes(1));
    expect(api.createDrive).toHaveBeenCalledWith(expect.objectContaining({
      companyId: 1, title: 'Engineer', graduationYear: 2027, minCgpa: 7.5, maxActiveBacklogs: null, ctcLpa: null, branches: ['CSE', 'IT'],
    }));
  });

  it('publishes a draft and shows only the legal actions per status', async () => {
    render(<AdminDrives />);
    fireEvent.click(await screen.findByRole('button', { name: /Publish Role 5/ }));
    await waitFor(() => expect(api.changeDriveStatus).toHaveBeenCalledWith(5, 'OPEN'));
    expect(screen.queryByRole('button', { name: /Publish Role 6/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Close Role 6/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /(Close|Cancel|Publish) Role 7/ })).not.toBeInTheDocument();
  });

  it('asks before cancelling and shows a refusal from the server', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    api.changeDriveStatus.mockRejectedValue(new Error('A drive cannot move from CLOSED to OPEN.'));
    render(<AdminDrives />);
    const cancel = await screen.findByRole('button', { name: /Cancel Role 5/ });
    fireEvent.click(cancel);
    expect(api.changeDriveStatus).not.toHaveBeenCalled();
    fireEvent.click(cancel);
    expect(await screen.findByRole('alert')).toHaveTextContent('cannot move from CLOSED');
    expect(confirm).toHaveBeenCalledTimes(2);
  });
});

