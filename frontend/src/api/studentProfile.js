import { apiRequest, apiBlob } from './client.js';

export const getStudentProfile = () =>
  apiRequest('/students/me');

export const saveStudentProfile = (profile) =>
  apiRequest('/students/me', {
    method: 'PUT',
    body: profile,
  });

// Upload a PDF resume.
// FormData is handled correctly by client.js.
export const uploadStudentResume = (file) => {
  const formData = new FormData();
  formData.append('file', file);

  return apiRequest('/students/me/resume', {
    method: 'POST',
    body: formData,
  });
};

// Download the currently uploaded resume.
export const downloadStudentResume = () =>
  apiBlob('/students/me/resume');

export const viewStudentResume = () =>
  apiBlob('/students/me/resume/view');

// Delete the uploaded resume.
export const deleteStudentResume = () =>
  apiRequest('/students/me/resume', {
    method: 'DELETE',
  });

// Analyze resume keywords against a job description.
export const analyzeStudentResume = (jobDescription) =>
  apiRequest('/students/me/resume/analyze', {
    method: 'POST',
    body: { jobDescription },
  });

export const getStudentResumeMetadata = () =>
  apiRequest('/students/me/resume/metadata');
