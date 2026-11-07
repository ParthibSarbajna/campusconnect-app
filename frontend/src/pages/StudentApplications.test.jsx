import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import StudentApplications from './StudentApplications.jsx';

vi.mock('../api/applications.js', () => ({ myApplications: vi.fn(), withdrawApplication: vi.fn() }));
import * as api from '../api/applications.js';

const app = (id, status, companyName) => ({ id, driveId: 5, companyName, driveTitle: 'Engineer', status, appliedAt: '2026-10-01T10:00:00Z', applicationDeadline: '2030-01-01T00:00:00Z' });

beforeEach(() => {
  vi.clearAllMocks();
  api.myApplications.mockResolvedValue({ items: [app(1, 'APPLIED', 'Acme'), app(2, 'SELECTED', 'Globex'), app(3, 'SHORTLISTED', 'Initech')], page: 0, size: 20, total: 3 });
});
afterEach(() => vi.restoreAllMocks());

describe('StudentApplications', () => {
  it('offers withdrawal only for undecided applications', async () => {
    render(<StudentApplications />);
    expect(await screen.findByRole('button', { name: /Withdraw from Acme/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Withdraw from Initech/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Withdraw from Globex/ })).not.toBeInTheDocument();
  });

  it('withdraws after confirmation and does nothing when cancelled', async () => {
    api.withdrawApplication.mockResolvedValue({});
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    render(<StudentApplications />);
    const button = await screen.findByRole('button', { name: /Withdraw from Acme/ });
    fireEvent.click(button);
    expect(api.withdrawApplication).not.toHaveBeenCalled();
    fireEvent.click(button);
    await waitFor(() => expect(api.withdrawApplication).toHaveBeenCalledWith(1));
    expect(confirm).toHaveBeenCalledTimes(2);
  });

  it('shows an empty state', async () => {
    api.myApplications.mockResolvedValue({ items: [], page: 0, size: 20, total: 0 });
    render(<StudentApplications />);
    expect(await screen.findByText(/have not applied/)).toBeInTheDocument();
  });
});
