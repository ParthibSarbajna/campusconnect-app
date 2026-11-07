import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import StudentDrives from './StudentDrives.jsx';

vi.mock('../api/drives.js', () => ({ listOpenDrives: vi.fn(), applyToDrive: vi.fn() }));
import * as api from '../api/drives.js';

const drive = (id, over = {}) => ({
  id, companyName: 'Acme Corp', title: 'Engineer', graduationYear: 2027, ctcLpa: 8.5, location: 'Kolkata', description: null,
  minCgpa: 7.5, maxActiveBacklogs: 0, branches: ['CSE'], applicationDeadline: '2030-01-01T00:00:00Z', status: 'OPEN', ...over,
});
const page = (items) => ({ items, page: 0, size: 20, total: items.length });
const eligible = { eligible: true, reasons: [] };
const renderPage = () => render(<MemoryRouter><StudentDrives /></MemoryRouter>);

beforeEach(() => vi.clearAllMocks());

describe('StudentDrives', () => {
  it('lets an eligible student apply and then reloads the list', async () => {
    api.listOpenDrives.mockResolvedValue(page([{ drive: drive(5), eligibility: eligible, applicationId: null, applicationStatus: null }]));
    api.applyToDrive.mockResolvedValue({});
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: /Apply/ }));
    await waitFor(() => expect(api.applyToDrive).toHaveBeenCalledWith(5));
    expect(await screen.findByText('Applied to Acme Corp.')).toBeInTheDocument();
    expect(api.listOpenDrives).toHaveBeenCalledTimes(2);
  });

  it('explains why a student is not eligible and offers no Apply button', async () => {
    api.listOpenDrives.mockResolvedValue(page([{
      drive: drive(5),
      eligibility: { eligible: false, reasons: ['Your CGPA 6.00 is below the required 7.50.', 'Your branch (ME) is not eligible.'] },
      applicationId: null, applicationStatus: null,
    }]));
    renderPage();
    expect(await screen.findByText('You are not eligible yet')).toBeInTheDocument();
    expect(screen.getByText('Your CGPA 6.00 is below the required 7.50.')).toBeInTheDocument();
    expect(screen.getByText('Your branch (ME) is not eligible.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Apply/ })).not.toBeInTheDocument();
  });

  it('shows the application status instead of Apply once applied', async () => {
    api.listOpenDrives.mockResolvedValue(page([{ drive: drive(5), eligibility: eligible, applicationId: 9, applicationStatus: 'SHORTLISTED' }]));
    renderPage();
    expect(await screen.findByText('SHORTLISTED')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Apply/ })).not.toBeInTheDocument();
  });

  it('shows the server message when applying fails', async () => {
    api.listOpenDrives.mockResolvedValue(page([{ drive: drive(5), eligibility: eligible, applicationId: null, applicationStatus: null }]));
    api.applyToDrive.mockRejectedValue(new Error('The application deadline for this drive has passed.'));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: /Apply/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('deadline for this drive has passed');
  });

  it('shows an empty state and a load error', async () => {
    api.listOpenDrives.mockResolvedValueOnce(page([]));
    const view = renderPage();
    expect(await screen.findByText(/No drives are open/)).toBeInTheDocument();
    view.unmount();
    api.listOpenDrives.mockRejectedValueOnce(new Error('Cannot reach the server.'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server.');
  });
});
