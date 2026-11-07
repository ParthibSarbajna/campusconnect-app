import { apiRequest } from './client.js';
import { queryString } from '../lib/format.js';

// Administrator
export const listAdminDrives = ({
  status,
  companyId,
  graduationYear,
  page = 0,
  size = 25,
} = {}) =>
  apiRequest(
    `/admin/drives${queryString({ status, companyId, graduationYear, page, size })}`
  );

export const createDrive = (body) =>
  apiRequest('/admin/drives', { method: 'POST', body });

export const changeDriveStatus = (id, status) =>
  apiRequest(`/admin/drives/${id}/status`, {
    method: 'POST',
    body: { status },
  });

// Delete a cancelled drive
export const deleteDrive = (id) =>
  apiRequest(`/admin/drives/${id}`, {
    method: 'DELETE',
  });

// Student — authenticated endpoint
export const listOpenDrives = (page = 0, size = 20) =>
  apiRequest(`/drives${queryString({ page, size })}`);

export const applyToDrive = (id) =>
  apiRequest(`/drives/${id}/applications`, { method: 'POST' });

// Public homepage — no student authentication required
export const listPublicDrives = (page = 0, size = 20) =>
  apiRequest(`/public/drives${queryString({ page, size })}`);