import { apiRequest } from './client.js';

export const fetchMe = () => apiRequest('/auth/me');
export const login = (email, password) => apiRequest('/auth/login', { method: 'POST', body: { email, password } });
export const register = ({ fullName, email, password }) =>
  apiRequest('/auth/register', { method: 'POST', body: { fullName, email, password } });
export const logout = () => apiRequest('/auth/logout', { method: 'POST' });
