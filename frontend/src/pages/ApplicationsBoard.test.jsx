import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ApplicationsBoard from './ApplicationsBoard.jsx';

vi.mock('../api/applications.js', () => ({ listApplications: vi.fn(), setApplicationStatus: vi.fn() }));
import * as api from '../api/applications.js';

const row = (id, status) => ({
  id, driveId: 5, companyName: 'Acme Corp', driveTitle: 'Engineer', studentUserId: 10, studentName: 'Asha Roy',
  studentEmail: 'asha@example.edu', branch: 'CSE', graduationYear: 2027, cgpa: 8.2, status, appliedAt: '2026-10-01T10:00:00Z',
});
const page = (items) => ({ items, page: 0, size: 25, total: items.length });

beforeEach(() => {
  vi.clearAllMocks();
  api.setApplicationStatus.mockResolvedValue(null);
});

describe('ApplicationsBoard', () => {
  it('lets an administrator shortlist an applied candidate', async () => {
    api.listApplications.mockResolvedValue(page([row(7, 'APPLIED')]));
    render(<ApplicationsBoard canManage />);
    fireEvent.click(await screen.findByRole('button', { name: /Shortlist Asha Roy at Acme Corp/ }));
    await waitFor(() => expect(api.setApplicationStatus).toHaveBeenCalledWith(7, 'SHORTLISTED'));
  });

  it('offers Select and Reject for shortlisted and nothing for final states', async () => {
    api.listApplications.mockResolvedValue(page([row(7, 'SHORTLISTED'), row(8, 'SELECTED'), row(9, 'REJECTED')]));
    render(<ApplicationsBoard canManage />);
    expect(await screen.findAllByRole('button', { name: /Select Asha Roy/ })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: /Reject Asha Roy/ })).toHaveLength(1);
  });

  it('is read-only for a department SPOC', async () => {
    api.listApplications.mockResolvedValue(page([row(7, 'APPLIED')]));
    render(<ApplicationsBoard title="Department applications" />);
    expect(await screen.findByText('Asha Roy')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Shortlist|Reject|Select/ })).not.toBeInTheDocument();
  });

  it('filters by status through the server', async () => {
    api.listApplications.mockResolvedValue(page([row(7, 'APPLIED')]));
    render(<ApplicationsBoard canManage />);
    await screen.findByText('Asha Roy');
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'REJECTED' } });
    await waitFor(() => expect(api.listApplications).toHaveBeenLastCalledWith({ status: 'REJECTED', page: 0 }));
  });

  it('shows the server message when a transition is refused', async () => {
    api.listApplications.mockResolvedValue(page([row(7, 'APPLIED')]));
    api.setApplicationStatus.mockRejectedValue(new Error('An application cannot move from APPLIED to SELECTED.'));
    render(<ApplicationsBoard canManage />);
    fireEvent.click(await screen.findByRole('button', { name: /Shortlist/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('cannot move from APPLIED');
  });
});
