import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client.js';
import AdminCompanies from './AdminCompanies.jsx';

vi.mock('../api/companies.js', () => ({ listCompanies: vi.fn(), createCompany: vi.fn(), setCompanyActive: vi.fn() }));
import * as api from '../api/companies.js';

beforeEach(() => {
  vi.clearAllMocks();
  api.listCompanies.mockResolvedValue([{ id: 3, name: 'Acme Corp', industry: 'IT', active: true }]);
  api.createCompany.mockResolvedValue({});
  api.setCompanyActive.mockResolvedValue({});
});

describe('AdminCompanies', () => {
  it('requires a name and a valid website before calling the server', async () => {
    render(<AdminCompanies />);
    await screen.findAllByText('Acme Corp');
    fireEvent.click(screen.getByRole('button', { name: 'Add company' }));
    expect(screen.getByText('Enter the company name.')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Globex' } });
    fireEvent.change(screen.getByLabelText('Website'), { target: { value: 'globex.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add company' }));
    expect(screen.getByText('Use a full http:// or https:// address.')).toBeInTheDocument();
    expect(api.createCompany).not.toHaveBeenCalled();
  });

  it('creates a company, sending null for blank optional fields', async () => {
    render(<AdminCompanies />);
    await screen.findAllByText('Acme Corp');
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: '  Globex  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add company' }));
    await waitFor(() => expect(api.createCompany).toHaveBeenCalledWith({ name: 'Globex', website: null, industry: null, description: null }));
    expect(await screen.findByText('Company added.')).toBeInTheDocument();
  });

  it('shows a duplicate-name error next to the name field', async () => {
    api.createCompany.mockRejectedValue(new ApiError('A company with this name already exists.', { status: 409 }));
    render(<AdminCompanies />);
    await screen.findAllByText('Acme Corp');
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'acme corp' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add company' }));
    expect(await screen.findByText('A company with this name already exists.')).toBeInTheDocument();
  });

  it('deactivates a company', async () => {
    render(<AdminCompanies />);
    fireEvent.click(await screen.findByRole('button', { name: /Deactivate/ }));
    await waitFor(() => expect(api.setCompanyActive).toHaveBeenCalledWith(3, false));
  });

  it('shows an empty state', async () => {
    api.listCompanies.mockResolvedValue([]);
    render(<AdminCompanies />);
    expect(await screen.findByText(/No companies yet/)).toBeInTheDocument();
  });
});

