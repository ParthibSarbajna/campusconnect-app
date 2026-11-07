// Single place where the browser talks to the backend.
 // Authentication is a server-side session in an HttpOnly cookie (never readable from JavaScript).
 // State-changing requests also carry a CSRF token fetched from GET /auth/csrf.

const BASE = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '');
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export class ApiError extends Error {
  constructor(message, { status = 0, body = null } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

let csrf = null;
let csrfPromise = null;
let unauthorizedHandler = null;

/** Called when any request (except a failed login) comes back 401, i.e. the session expired. */
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

export function resetCsrf() {
  csrf = null;
  csrfPromise = null;
}

async function send(path, { method = 'GET', body, signal, headers = {} } = {}) {
  let res;

  // FormData must not be JSON-serialized or assigned a manual Content-Type.
  const isFormData = body instanceof FormData;

  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      credentials: 'include',
      signal,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined && !isFormData
          ? { 'Content-Type': 'application/json' }
          : {}),
        ...headers,
      },
      body: body !== undefined
        ? (isFormData ? body : JSON.stringify(body))
        : undefined,
    });
  } catch (e) {
    if (e && e.name === 'AbortError') {
      throw e;
    }

    throw new ApiError(
      'Cannot reach the server. Check that the backend is running.',
      { status: 0 }
    );
  }

  const text = await res.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const message =
      (data && (data.detail || data.title))
      || `Request failed (${res.status})`;

    throw new ApiError(message, {
      status: res.status,
      body: data,
    });
  }

  return data;
}

async function ensureCsrf() {
  if (csrf) {
    return csrf;
  }

  if (!csrfPromise) {
    csrfPromise = send('/auth/csrf')
      .then((data) => {
        csrf = data;
        return data;
      })
      .finally(() => {
        csrfPromise = null;
      });
  }

  return csrfPromise;
}

async function sendWithCsrf(path, opts, retried = false) {
  const token = await ensureCsrf();

  try {
    return await send(path, {
      ...opts,
      headers: {
        ...opts.headers,
        [token.headerName]: token.token,
      },
    });
  } catch (e) {
    // A 403 can mean the CSRF token went stale (new session).
    // Refresh it and retry exactly once.
    if (e instanceof ApiError && e.status === 403 && !retried) {
      resetCsrf();
      return sendWithCsrf(path, opts, true);
    }

    throw e;
  }
}

export async function apiRequest(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const opts = {
    ...options,
    method,
  };

  try {
    return SAFE_METHODS.has(method)
      ? await send(path, opts)
      : await sendWithCsrf(path, opts);
  } catch (e) {
    if (
      e instanceof ApiError
      && e.status === 401
      && !path.startsWith('/auth/login')
      && unauthorizedHandler
    ) {
      unauthorizedHandler();
    }

    throw e;
  }
}


// Binary/PDF responses must not pass through the JSON/text parser above.
export async function apiBlob(path) {
  let response;
  try {
    response = await fetch(`${BASE}${path}`, {
      method: 'GET',
      credentials: 'include',
      headers: { Accept: 'application/pdf' },
    });
  } catch (e) {
    if (e?.name === 'AbortError') throw e;
    throw new ApiError('Cannot reach the server. Check that the backend is running.', { status:  0 });
  }

  if (!response.ok) {
    let body = null;
    try { body = await response.json(); } catch { /* non-JSON error body */ }
    const err = new ApiError(body?.detail || body?.title || `Request failed (${response.status})`, {
      status: response.status,
      body,
    });
    if (err.status === 401 && !path.startsWith('/auth/login') && unauthorizedHandler) {
      unauthorizedHandler();
    }
    throw err;
  }

  return response.blob();
}
