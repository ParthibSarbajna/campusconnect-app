import { apiRequest } from './client.js';

export const listCompanies = () => apiRequest('/admin/companies');
export const createCompany = (body) => apiRequest('/admin/companies', { method: 'POST', body });
export const setCompanyActive = (id, active) =>
  apiRequest(`/admin/companies/${id}/active`, { method: 'PATCH', body: { active } });
