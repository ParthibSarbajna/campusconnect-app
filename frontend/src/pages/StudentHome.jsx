import ATSResumeAnalyzer from './ATSResumeAnalyzer.jsx';
import { useEffect, useState } from 'react';
import { changePassword, updateProfile } from '../api/accounts.js';
import {
  getStudentProfile,
  saveStudentProfile,
  uploadStudentResume,
  downloadStudentResume,
  viewStudentResume,
  deleteStudentResume,
  getStudentResumeMetadata,
} from '../api/studentProfile.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { validateName, validatePassword } from '../auth/validators.js';
import Field from '../components/Field.jsx';

const emptyProfile = {
  rollNumber: '',
  phone: '',
  dateOfBirth: '',
  degree: '',
  branch: '',
  graduationYear: '',
  cgpa: '',
  activeBacklogs: '0',
};

function toFormValues(profile) {
  return {
    rollNumber: profile.rollNumber ?? '',
    phone: profile.phone ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    degree: profile.degree ?? '',
    branch: profile.branch ?? '',
    graduationYear: profile.graduationYear?.toString() ?? '',
    cgpa: profile.cgpa?.toString() ?? '',
    activeBacklogs: (profile.activeBacklogs ?? 0).toString(),
  };
}

function validateStudentProfile(form) {
  const year = form.graduationYear.trim();
  const cgpa = form.cgpa.trim();
  const backlogs = form.activeBacklogs.trim();

  if (form.rollNumber.length > 40) return 'Roll number must be 40 characters or fewer.';
  if (form.phone.length > 20) return 'Phone number must be 20 characters or fewer.';
  if (form.degree.length > 100) return 'Degree must be 100 characters or fewer.';
  if (form.branch.length > 120) return 'Branch must be 120 characters or fewer.';

  if (form.dateOfBirth && form.dateOfBirth >= new Date().toISOString().slice(0, 10)) {
    return 'Date of birth must be in the past.';
  }

  if (year && (!/^\d{4}$/.test(year) || Number(year) < 2000 || Number(year) > 2100)) {
    return 'Graduation year must be between 2000 and 2100.';
  }

  if (cgpa && (!/^\d{1,2}(\.\d{1,2})?$/.test(cgpa) || Number(cgpa) < 0 || Number(cgpa) > 10)) {
    return 'CGPA must be between 0 and 10, with up to two decimal places.';
  }

  if (!/^\d+$/.test(backlogs) || Number(backlogs) > 32767) {
    return 'Active backlogs must be a non-negative whole number.';
  }

  return '';
}

export default function StudentHome() {
  const { user, refresh } = useAuth();

  const [name, setName] = useState(user.fullName);
  const [nameError, setNameError] = useState('');
  const [nameMsg, setNameMsg] = useState('');

  const [pw, setPw] = useState({ current: '', next: '' });
  const [pwError, setPwError] = useState('');
  const [pwMsg, setPwMsg] = useState('');

  const [profile, setProfile] = useState(emptyProfile);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState('');
  const [profileMsg, setProfileMsg] = useState('');
  const [profileSaving, setProfileSaving] = useState(false);

  const [resumeFile, setResumeFile] = useState(null);
  const [resumeUploading, setResumeUploading] = useState(false);
  const [resumeError, setResumeError] = useState('');
  const [resumeMsg, setResumeMsg] = useState('');
  const [resumeUploaded, setResumeUploaded] = useState(false);
  const [resumeMeta, setResumeMeta] = useState({ hasResume: false, fileName: '' });
  const [resumeActionBusy, setResumeActionBusy] = useState(false);


  useEffect(() => {
    let active = true;

    getStudentResumeMetadata()
      .then((metadata) => {
        if (active) {
          setResumeMeta(metadata);
          setResumeUploaded(Boolean(metadata.hasResume));
        }
      })
      .catch(() => {
        // Resume metadata is optional; profile loading should remain usable.
      });

    getStudentProfile()
      .then((data) => {
        if (!active) return;
        setProfile(toFormValues(data));
        setProfileError('');
      })
      .catch((error) => {
        if (!active) return;
        if (error.status === 404) {
          setProfile(emptyProfile);
        } else {
          setProfileError(error.message || 'Could not load your student profile.');
        }
      })
      .finally(() => {
        if (active) setProfileLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  async function saveName(e) {
    e.preventDefault();
    setNameMsg('');

    const err = validateName(name);
    setNameError(err);
    if (err) return;

    try {
      await updateProfile(name.trim());
      await refresh();
      setNameMsg('Name updated.');
      setNameError('');
    } catch (ex) {
      setNameError(ex.message);
    }
  }

  async function savePassword(e) {
    e.preventDefault();
    setPwMsg('');

    const err = pw.current ? validatePassword(pw.next) : 'Enter your current password.';
    setPwError(err);
    if (err) return;

    try {
      await changePassword(pw.current, pw.next);
      setPw({ current: '', next: '' });
      setPwMsg('Password changed.');
      setPwError('');
    } catch (ex) {
      setPwError(ex.message);
    }
  }

  function updateField(field, value) {
    setProfile((current) => ({ ...current, [field]: value }));
    setProfileMsg('');
    setProfileError('');
  }

  async function saveStudentDetails(e) {
    e.preventDefault();
    setProfileMsg('');

    const error = validateStudentProfile(profile);
    if (error) {
      setProfileError(error);
      return;
    }

    setProfileSaving(true);
    setProfileError('');

    const payload = {
      rollNumber: profile.rollNumber.trim() || null,
      phone: profile.phone.trim() || null,
      dateOfBirth: profile.dateOfBirth || null,
      degree: profile.degree.trim() || null,
      branch: profile.branch.trim() || null,
      graduationYear: profile.graduationYear ? Number(profile.graduationYear) : null,
      cgpa: profile.cgpa ? Number(profile.cgpa) : null,
      activeBacklogs: Number(profile.activeBacklogs),
    };

    try {
      const saved = await saveStudentProfile(payload);
      setProfile(toFormValues(saved));
      setProfileMsg('Student profile saved successfully.');
    } catch (error) {
      setProfileError(error.message || 'Could not save your student profile.');
    } finally {
      setProfileSaving(false);
    }
  }

  async function handleResumeUpload(e) {
    e.preventDefault();
    setResumeError('');
    setResumeMsg('');

    if (!resumeFile) {
      setResumeError('Please choose a PDF resume first.');
      return;
    }

    if (resumeFile.type !== 'application/pdf') {
      setResumeError('Only PDF files are allowed.');
      return;
    }

    if (resumeFile.size > 5 * 1024 * 1024) {
      setResumeError('Resume must be 5 MB or smaller.');
      return;
    }

    setResumeUploading(true);

    try {
      await uploadStudentResume(resumeFile);
      setResumeUploaded(true);
      setResumeMeta({ hasResume: true, fileName: resumeFile.name });
      setResumeMsg('Resume uploaded successfully.');
      setResumeFile(null);
      e.target.reset();
    } catch (error) {
      setResumeError(error.message || 'Could not upload resume.');
    } finally {
      setResumeUploading(false);
    }
  }

  async function resumeBlobAction(action) {
    setResumeError('');
    setResumeActionBusy(true);
    try {
      const blob = action === 'view' ? await viewStudentResume() : await downloadStudentResume();
      const url = URL.createObjectURL(blob);
      if (action === 'view') {
        window.open(url, '_blank', 'noopener,noreferrer');
        window.setTimeout(() => URL.revokeObjectURL(url), 60000);
      } else {
        const link = document.createElement('a');
        link.href = url;
        link.download = resumeMeta.fileName || 'resume.pdf';
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
    } catch (error) {
      setResumeError(error.message || 'Could not access the uploaded resume.');
    } finally {
      setResumeActionBusy(false);
    }
  }

  async function handleDeleteResume() {
    if (!window.confirm('Delete your uploaded resume?')) return;
    setResumeError('');
    setResumeActionBusy(true);
    try {
      await deleteStudentResume();
      setResumeMeta({ hasResume: false, fileName: '' });
      setResumeUploaded(false);
      setResumeMsg('Resume deleted.');
    } catch (error) {
      setResumeError(error.message || 'Could not delete resume.');
    } finally {
      setResumeActionBusy(false);
    }
  }

  return (
    <main className="mx-auto grid max-w-3xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">My account</h1>

      <section className="card" aria-labelledby="acct">
        <h2 id="acct" className="text-lg">Account details</h2>

        <dl className="mt-2 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          <dt className="text-mute">Email</dt><dd>{user.email}</dd>
          <dt className="text-mute">Role</dt><dd>Student</dd>
        </dl>

        <form onSubmit={saveName} noValidate>
          <Field id="name" label="Full name" value={name} onChange={setName} error={nameError} />
          <button type="submit" className="btn mt-3">Save name</button>
          {nameMsg && <span role="status" className="ml-3 text-sm text-ok">{nameMsg}</span>}
        </form>
      </section>

      <section className="card" aria-labelledby="student-profile">
        <h2 id="student-profile" className="text-lg">Student profile</h2>
        <p className="mt-1 text-sm text-mute">
          Keep your academic details up to date for placement eligibility.
        </p>

        {profileLoading ? (
          <p role="status" className="mt-4 text-sm">Loading student profile…</p>
        ) : (
          <form onSubmit={saveStudentDetails} noValidate className="mt-3 grid gap-1 sm:grid-cols-2 sm:gap-x-4">
            <Field id="rollNumber" label="Roll number" value={profile.rollNumber}
              onChange={(v) => updateField('rollNumber', v)} />

            <Field id="phone" label="Phone" type="tel" value={profile.phone}
              onChange={(v) => updateField('phone', v)} />

            <Field id="dateOfBirth" label="Date of birth" type="date" value={profile.dateOfBirth}
              onChange={(v) => updateField('dateOfBirth', v)} />

            <Field id="degree" label="Degree" value={profile.degree}
              onChange={(v) => updateField('degree', v)} />

            <Field id="branch" label="Branch" value={profile.branch}
              onChange={(v) => updateField('branch', v)} />

            <Field id="graduationYear" label="Graduation year" type="number"
              value={profile.graduationYear}
              onChange={(v) => updateField('graduationYear', v)} error="" />

            <Field id="cgpa" label="CGPA (0–10)" type="number" value={profile.cgpa}
              onChange={(v) => updateField('cgpa', v)} error="" />

            <Field id="activeBacklogs" label="Active backlogs" type="number"
              value={profile.activeBacklogs}
              onChange={(v) => updateField('activeBacklogs', v)} error="" />

            {profileError && (
              <p role="alert" className="sm:col-span-2 mt-2 text-sm text-danger">
                {profileError}
              </p>
            )}

            {profileMsg && (
              <p role="status" className="sm:col-span-2 mt-2 text-sm text-ok">
                {profileMsg}
              </p>
            )}

            <div className="sm:col-span-2">
              <button type="submit" className="btn mt-3" disabled={profileSaving}>
                {profileSaving ? 'Saving…' : 'Save student profile'}
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="card" aria-labelledby="resume">
        <h2 id="resume" className="text-lg">Resume</h2>
        <p className="mt-1 text-sm text-mute">
          Upload your resume as a PDF. Maximum file size: 5 MB.
        </p>

        <form onSubmit={handleResumeUpload} className="mt-3 grid gap-3">
          <label htmlFor="resumePdf" className="text-sm font-medium">
            Choose resume PDF
          </label>

          <input
            id="resumePdf"
            type="file"
            accept=".pdf,application/pdf"
            className="block w-full text-sm file:mr-4 file:cursor-pointer file:rounded-lg file:border-0 file:bg-pri file:px-4 file:py-2 file:font-semibold file:text-onpri hover:file:opacity-90"
            onChange={(e) => {
              setResumeFile(e.target.files?.[0] || null);
              setResumeError('');
              setResumeMsg('');
            }}
          />

          {resumeFile && (
            <p className="text-sm text-mute">
              Selected: {resumeFile.name}
            </p>
          )}

          {resumeError && <p role="alert" className="text-sm text-danger">{resumeError}</p>}
          {resumeMsg && <p role="status" className="text-sm text-ok">{resumeMsg}</p>}

          <button type="submit" className="btn" disabled={resumeUploading}>
            {resumeUploading ? 'Uploading…' : 'Upload resume'}
          </button>

          {resumeMeta.hasResume && (
            <div className="flex flex-wrap items-center gap-2 rounded-lg border border-line p-3">
              <p className="mr-auto text-sm text-ok">
                Resume available: <strong>{resumeMeta.fileName || 'Uploaded PDF'}</strong>
              </p>
              <button type="button" className="btn-ghost" disabled={resumeActionBusy}
                onClick={() => resumeBlobAction('view')}>View Resume</button>
              <button type="button" className="btn-ghost" disabled={resumeActionBusy}
                onClick={() => resumeBlobAction('download')}>Download Resume</button>
              <button type="button" className="btn-ghost" disabled={resumeActionBusy}
                onClick={handleDeleteResume}>Delete Resume</button>
            </div>
          )}
        </form>
      </section>

      <ATSResumeAnalyzer />

      <section className="card" aria-labelledby="pw">
        <h2 id="pw" className="text-lg">Change password</h2>

        <form onSubmit={savePassword} noValidate>
          <Field id="cur" label="Current password" type="password"
            autoComplete="current-password" value={pw.current}
            onChange={(v) => setPw({ ...pw, current: v })} />

          <Field id="new" label="New password" type="password"
            autoComplete="new-password" value={pw.next}
            onChange={(v) => setPw({ ...pw, next: v })} error={pwError} />

          <button type="submit" className="btn mt-3">Change password</button>
          {pwMsg && <span role="status" className="ml-3 text-sm text-ok">{pwMsg}</span>}
        </form>
      </section>
    </main>
  );
}
