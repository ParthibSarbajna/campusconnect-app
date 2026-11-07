import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../auth/AuthContext.jsx';
import StudentHome from './StudentHome.jsx';

vi.mock('../api/accounts.js', () => ({
  changePassword: vi.fn(),
  updateProfile: vi.fn(),
}));

vi.mock('../api/studentProfile.js', () => ({
  getStudentProfile: vi.fn(),
  saveStudentProfile: vi.fn(),
  uploadStudentResume: vi.fn(),
  analyzeStudentResume: vi.fn(),
  downloadStudentResume: vi.fn(),
  viewStudentResume: vi.fn(),
  deleteStudentResume: vi.fn(),
  getStudentResumeMetadata: vi.fn(),
}));

import * as accountsApi from '../api/accounts.js';
import * as profileApi from '../api/studentProfile.js';

const studentUser = {
  id: 1,
  email: 'student@example.com',
  fullName: 'Test Student',
  role: 'STUDENT',
  departmentId: null,
  enabled: true,
};

function renderPage() {
  const auth = {
    status: 'authenticated',
    user: studentUser,
    refresh: vi.fn().mockResolvedValue(studentUser),
  };

  render(
    <AuthContext.Provider value={auth}>
      <StudentHome />
    </AuthContext.Provider>,
  );

  return auth;
}

const savedProfile = {
  id: 1,
  userId: 1,
  rollNumber: 'CSE001',
  phone: '9876543210',
  dateOfBirth: '2004-05-12',
  degree: 'B.Tech',
  branch: 'CSE',
  graduationYear: 2027,
  cgpa: 8.5,
  activeBacklogs: 0,
  resumeUrl: '',
};

beforeEach(() => {
  vi.clearAllMocks();
  profileApi.getStudentProfile.mockResolvedValue(savedProfile);
  profileApi.saveStudentProfile.mockResolvedValue(savedProfile);
  profileApi.getStudentResumeMetadata.mockResolvedValue({ hasResume: false, fileName: null, uploadedAt: null });
  profileApi.uploadStudentResume.mockResolvedValue({});
  profileApi.analyzeStudentResume.mockResolvedValue({ score: 0, matchedKeywords: [], missingKeywords: [], disclaimer: '' });
  profileApi.downloadStudentResume.mockResolvedValue(new Blob(['pdf']));
  profileApi.viewStudentResume.mockResolvedValue(new Blob(['pdf']));
  profileApi.deleteStudentResume.mockResolvedValue({});
  accountsApi.updateProfile.mockResolvedValue({});
  accountsApi.changePassword.mockResolvedValue({});
});

describe('StudentHome student profile', () => {
  it('loads and displays the saved academic details', async () => {
    renderPage();

    expect(await screen.findByLabelText('Roll number')).toHaveValue('CSE001');
    expect(screen.getByLabelText('Degree')).toHaveValue('B.Tech');
    expect(screen.getByLabelText('Branch')).toHaveValue('CSE');
    expect(screen.getByLabelText('Graduation year')).toHaveValue(2027);
    expect(screen.getByLabelText('CGPA (0–10)')).toHaveValue(8.5);
    expect(profileApi.getStudentProfile).toHaveBeenCalledTimes(1);
  });

  it('shows an empty form when the student has no saved profile', async () => {
    profileApi.getStudentProfile.mockRejectedValue(
      Object.assign(new Error('Student profile not found.'), { status: 404 }),
    );

    renderPage();

    expect(await screen.findByLabelText('Roll number')).toHaveValue('');
    expect(screen.getByLabelText('Degree')).toHaveValue('');
    expect(screen.getByLabelText('Active backlogs')).toHaveValue(0);
  });

  it('saves the edited profile and shows confirmation', async () => {
    renderPage();

    const rollNumber = await screen.findByLabelText('Roll number');
    fireEvent.change(rollNumber, { target: { value: 'CSE002' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save student profile' }));

    await waitFor(() => {
      expect(profileApi.saveStudentProfile).toHaveBeenCalledWith(
        expect.objectContaining({
          rollNumber: 'CSE002',
          degree: 'B.Tech',
          branch: 'CSE',
          graduationYear: 2027,
          cgpa: 8.5,
          activeBacklogs: 0,
        }),
      );
    });

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Student profile saved successfully.',
    );
  });

  it('rejects an invalid CGPA before calling the API', async () => {
    renderPage();

    fireEvent.change(await screen.findByLabelText('CGPA (0–10)'), {
      target: { value: '11' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save student profile' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'CGPA must be between 0 and 10',
    );
    expect(profileApi.saveStudentProfile).not.toHaveBeenCalled();
  });

  it('shows a server error when saving the profile fails', async () => {
    profileApi.saveStudentProfile.mockRejectedValue(
      new Error('That roll number is already in use.'),
    );

    renderPage();

    fireEvent.change(await screen.findByLabelText('Roll number'), {
      target: { value: 'DUPLICATE' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save student profile' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'That roll number is already in use.',
    );
  });
});
