import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import StatusCard from './StatusCard.jsx';
import { ApiError } from '../api/client.js';

vi.mock('../api/health.js', () => ({ getHealth: vi.fn() }));
import { getHealth } from '../api/health.js';

beforeEach(() => vi.clearAllMocks());

describe('StatusCard', () => {
  it('shows connected state when the backend is UP', async () => {
    getHealth.mockResolvedValue({ status: 'UP', database: 'UP', schemaVersion: '1', version: '0.1.0', timestamp: '2026-10-15T00:00:00Z' });
    render(<StatusCard />);
    expect(screen.getByText(/checking the backend/i)).toBeInTheDocument();
    expect(await screen.findByText(/frontend, backend and database are connected/i)).toBeInTheDocument();
  });

  it('shows an actionable error when the backend is unreachable', async () => {
    getHealth.mockRejectedValue(new ApiError('Cannot reach the server.', { status: 0 }));
    render(<StatusCard />);
    expect(await screen.findByRole('alert')).toHaveTextContent(/backend not reachable/i);
  });
});
