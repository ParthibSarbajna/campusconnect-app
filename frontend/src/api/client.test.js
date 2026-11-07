import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest } from './client.js';
import { getHealth } from './health.js';

function mockFetch(status, body) {
  global.fetch = vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    text: () => Promise.resolve(body === undefined ? '' : JSON.stringify(body)),
  });
}

afterEach(() => vi.restoreAllMocks());

describe('apiRequest', () => {
  it('parses a JSON success response and sends credentials', async () => {
    mockFetch(200, { ok: true });
    await expect(apiRequest('/x')).resolves.toEqual({ ok: true });
    expect(global.fetch).toHaveBeenCalledWith('/api/v1/x', expect.objectContaining({ credentials: 'include', method: 'GET' }));
  });

  it('throws ApiError with status and problem detail on failure', async () => {
    mockFetch(401, { detail: 'Not signed in' });
    await expect(apiRequest('/x')).rejects.toMatchObject({ name: 'ApiError', status: 401, message: 'Not signed in' });
  });

  it('maps network failure to an ApiError with status 0', async () => {
    global.fetch = vi.fn().mockRejectedValue(new TypeError('failed'));
    const err = await apiRequest('/x').catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(0);
  });
});

describe('getHealth', () => {
  it('returns the DOWN payload when the backend answers 503', async () => {
    mockFetch(503, { status: 'DOWN', database: 'DOWN' });
    await expect(getHealth()).resolves.toMatchObject({ status: 'DOWN' });
  });

  it('rethrows other errors', async () => {
    mockFetch(500, { title: 'Internal Server Error' });
    await expect(getHealth()).rejects.toMatchObject({ status: 500 });
  });
});
