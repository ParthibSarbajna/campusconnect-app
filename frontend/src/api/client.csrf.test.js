import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiRequest, resetCsrf, setUnauthorizedHandler } from './client.js';

const res = (status, body) => ({
  ok: status >= 200 && status < 300,
  status,
  text: () => Promise.resolve(body === undefined ? '' : JSON.stringify(body)),
});
const isCsrf = (url) => url.endsWith('/auth/csrf');

beforeEach(() => {
  resetCsrf();
  setUnauthorizedHandler(null);
});

describe('CSRF handling', () => {
  it('fetches a token once and sends it on every state-changing request', async () => {
    global.fetch = vi.fn((url) => Promise.resolve(isCsrf(url) ? res(200, { headerName: 'X-CSRF-TOKEN', token: 'tok1' }) : res(200, { ok: true })));
    await apiRequest('/a', { method: 'POST', body: { x: 1 } });
    await apiRequest('/b', { method: 'PATCH', body: {} });
    expect(global.fetch.mock.calls.filter(([u]) => isCsrf(u))).toHaveLength(1);
    const post = global.fetch.mock.calls.find(([u]) => u.endsWith('/a'));
    expect(post[1].headers['X-CSRF-TOKEN']).toBe('tok1');
    expect(post[1].credentials).toBe('include');
  });

  it('does not request a token for GET', async () => {
    global.fetch = vi.fn(() => Promise.resolve(res(200, { ok: true })));
    await apiRequest('/health');
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  it('refreshes the token and retries exactly once after a 403', async () => {
    let csrfCalls = 0;
    let posts = 0;
    global.fetch = vi.fn((url) => {
      if (isCsrf(url)) { csrfCalls += 1; return Promise.resolve(res(200, { headerName: 'X-CSRF-TOKEN', token: `tok${csrfCalls}` })); }
      posts += 1;
      return Promise.resolve(posts === 1 ? res(403, { title: 'Forbidden' }) : res(200, { ok: true }));
    });
    await expect(apiRequest('/a', { method: 'POST', body: {} })).resolves.toEqual({ ok: true });
    expect(csrfCalls).toBe(2);
    expect(posts).toBe(2);
  });

  it('gives up after one retry so a real 403 surfaces', async () => {
    global.fetch = vi.fn((url) => Promise.resolve(isCsrf(url) ? res(200, { headerName: 'X-CSRF-TOKEN', token: 't' }) : res(403, { title: 'Forbidden' })));
    await expect(apiRequest('/a', { method: 'POST', body: {} })).rejects.toMatchObject({ status: 403 });
  });
});

describe('expired sessions', () => {
  it('notifies the auth layer on 401', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    global.fetch = vi.fn(() => Promise.resolve(res(401, { detail: 'Not signed in.' })));
    await expect(apiRequest('/auth/me')).rejects.toMatchObject({ status: 401 });
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('does not treat a failed login as an expired session', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    global.fetch = vi.fn((url) => Promise.resolve(isCsrf(url) ? res(200, { headerName: 'X-CSRF-TOKEN', token: 't' }) : res(401, { detail: 'Invalid email or password.' })));
    await expect(apiRequest('/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({ status: 401 });
    expect(handler).not.toHaveBeenCalled();
  });
});
