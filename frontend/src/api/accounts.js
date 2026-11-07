import { apiRequest } from './client.js';

export const updateProfile = (fullName) => apiRequest('/users/me', { method: 'PATCH', body: { fullName } });
export const changePassword = (currentPassword, newPassword) =>
  apiRequest('/users/me/password', { method: 'POST', body: { currentPassword, newPassword } });
export const listDepartmentUsers = (departmentId, page = 0, size = 50) =>
  apiRequest(`/departments/${departmentId}/users?page=${page}&size=${size}`);

// Placement Admin only (the backend enforces this; the UI merely hides it from other roles)
export const listUsers = (page = 0, size = 25) => apiRequest(`/admin/users?page=${page}&size=${size}`);
export const createUser = (body) => apiRequest('/admin/users', { method: 'POST', body });
export const updateUser = (id, body) => apiRequest(`/admin/users/${id}`, { method: 'PATCH', body });
export const listDepartments = () => apiRequest('/admin/departments');
export const createDepartment = (code, name) => apiRequest('/admin/departments', { method: 'POST', body: { code, name } });
