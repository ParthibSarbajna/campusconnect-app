import { ApiError, apiRequest } from './client.js';

/** Returns the health payload. A 503 with a body still carries a usable DOWN status. */
export async function getHealth(signal) {
  try {
    return await apiRequest('/health', { signal });
  } catch (e) {
    if (e instanceof ApiError && e.status === 503 && e.body && typeof e.body === 'object' && e.body.status) {
      return e.body;
    }
    throw e;
  }
}
