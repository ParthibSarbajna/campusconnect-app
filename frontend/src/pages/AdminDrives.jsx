
import { useCallback, useEffect, useState } from 'react';
import { listCompanies } from '../api/companies.js';
import {
  changeDriveStatus,
  createDrive,
  deleteDrive,
  listAdminDrives,
} from '../api/drives.js';
import Field from '../components/Field.jsx';
import StatusPill from '../components/StatusPill.jsx';
import DriveRounds from '../components/DriveRounds.jsx';
import { formatDateTime } from '../lib/format.js';

const EMPTY = {
  companyId: '',
  title: '',
  location: '',
  ctcLpa: '',
  graduationYear: '',
  minCgpa: '',
  maxActiveBacklogs: '',
  branches: '',
  applicationDeadline: '',
  description: '',
};

const SELECT = 'w-full rounded-lg border border-line bg-surf px-3 py-2';

export function validateDrive(f) {
  const errs = {};

  if (!f.companyId) errs.companyId = 'Choose a company.';
  if (!f.title.trim()) errs.title = 'Enter the role title.';

  const year = Number(f.graduationYear);
  if (
    !f.graduationYear ||
    !Number.isInteger(year) ||
    year < 2000 ||
    year > 2100
  ) {
    errs.graduationYear = 'Enter a batch year such as 2027.';
  }

  if (
    f.minCgpa !== '' &&
    (Number(f.minCgpa) < 0 || Number(f.minCgpa) > 10)
  ) {
    errs.minCgpa = 'CGPA must be between 0 and 10.';
  }

  if (
    f.maxActiveBacklogs !== '' &&
    (!Number.isInteger(Number(f.maxActiveBacklogs)) ||
      Number(f.maxActiveBacklogs) < 0)
  ) {
    errs.maxActiveBacklogs = 'Use a whole number, 0 or more.';
  }

  if (f.ctcLpa !== '' && Number(f.ctcLpa) < 0) {
    errs.ctcLpa = 'CTC cannot be negative.';
  }

  const when = new Date(f.applicationDeadline);

  if (!f.applicationDeadline || Number.isNaN(when.getTime())) {
    errs.applicationDeadline = 'Choose the application deadline.';
  } else if (when.getTime() <= Date.now()) {
    errs.applicationDeadline = 'The deadline must be in the future.';
  }

  return errs;
}

export default function AdminDrives() {
  const [companies, setCompanies] = useState([]);
  const [drives, setDrives] = useState(null);
  const [page, setPage] = useState(0);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [roundDriveId, setRoundDriveId] = useState(null);

  const set = (k) => (v) =>
    setForm((f) => ({ ...f, [k]: v }));

  const load = useCallback(async () => {
    try {
      const [c, d] = await Promise.all([
        listCompanies(),
        listAdminDrives({ page }),
      ]);

      setCompanies(c.filter((x) => x.active));
      setDrives(d);
      setError('');
    } catch (e) {
      setError(e.message);
      setDrives({
        items: [],
        page: 0,
        size: 25,
        total: 0,
      });
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  // Close the rounds panel if its drive is cancelled or no longer listed.
  useEffect(() => {
    if (
      roundDriveId !== null &&
      drives?.items &&
      !drives.items.some(
        (d) => d.id === roundDriveId && d.status !== 'CANCELLED'
      )
    ) {
      setRoundDriveId(null);
    }
  }, [drives, roundDriveId]);

  async function submit(e) {
    e.preventDefault();
    setNotice('');

    const errs = validateDrive(form);
    setErrors(errs);

    if (Object.keys(errs).length) return;

    const num = (v) => (v === '' ? null : Number(v));

    try {
      await createDrive({
        companyId: Number(form.companyId),
        title: form.title.trim(),
        description: form.description.trim() || null,
        location: form.location.trim() || null,
        ctcLpa: num(form.ctcLpa),
        graduationYear: Number(form.graduationYear),
        minCgpa: num(form.minCgpa),
        maxActiveBacklogs: num(form.maxActiveBacklogs),
        branches: form.branches
          .split(',')
          .map((b) => b.trim())
          .filter(Boolean),
        applicationDeadline: new Date(
          form.applicationDeadline
        ).toISOString(),
      });

      setForm(EMPTY);
      setNotice(
        'Drive saved as a draft. Publish it when it is ready for students.'
      );
      load();
    } catch (ex) {
      setError(ex.message);
    }
  }

  async function move(d, status, ask) {
    if (ask && !window.confirm(ask)) return;

    setNotice('');

    try {
      await changeDriveStatus(d.id, status);
      setNotice(`${d.title} is now ${status}.`);
      load();
    } catch (ex) {
      setError(ex.message);
    }
  }

  async function removeDrive(d) {
    const confirmed = window.confirm(
      `Remove the cancelled drive "${d.title}"? This action cannot be undone.`
    );
    if (!confirmed) return;

    setNotice('');
    setError('');

    try {
      await deleteDrive(d.id);
      setRoundDriveId(null);
      setNotice(`Drive "${d.title}" was removed.`);
      await load();
    } catch (ex) {
      setError(ex.message);
    }
  }

  const pages = drives
    ? Math.max(1, Math.ceil(drives.total / drives.size))
    : 1;

  const selectedDrive = drives?.items?.find(
    (d) => d.id === roundDriveId
  );

  return (
    <main className="mx-auto grid max-w-6xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">
        Placement drives
      </h1>

      {notice && (
        <p
          role="status"
          className="rounded-lg bg-okt p-3 text-sm text-ok"
        >
          {notice}
        </p>
      )}

      {error && (
        <p
          role="alert"
          className="rounded-lg bg-badt p-3 text-sm text-bad"
        >
          {error}
        </p>
      )}

      <section className="card" aria-labelledby="new-drive">
        <h2 id="new-drive" className="text-lg">
          Create a drive
        </h2>

        {companies.length === 0 && (
          <p className="mt-1 text-sm text-mute">
            There are no active companies yet. Add one on the Companies page first.
          </p>
        )}

        <form
          onSubmit={submit}
          noValidate
          className="grid gap-x-4 sm:grid-cols-2"
        >
          <div className="mt-3">
            <label
              htmlFor="dcompany"
              className="mb-1 block text-sm font-bold"
            >
              Company
            </label>

            <select
              id="dcompany"
              className={SELECT}
              value={form.companyId}
              onChange={(e) => set('companyId')(e.target.value)}
              aria-invalid={errors.companyId ? 'true' : undefined}
            >
              <option value="">Choose…</option>

              {companies.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>

            {errors.companyId && (
              <p role="alert" className="mt-1 text-sm text-bad">
                {errors.companyId}
              </p>
            )}
          </div>

          <Field
            id="dtitle"
            label="Role title"
            value={form.title}
            onChange={set('title')}
            error={errors.title}
          />

          <Field
            id="dyear"
            label="Batch (graduation year)"
            type="number"
            value={form.graduationYear}
            onChange={set('graduationYear')}
            error={errors.graduationYear}
          />

          <Field
            id="ddeadline"
            label="Application deadline"
            type="datetime-local"
            value={form.applicationDeadline}
            onChange={set('applicationDeadline')}
            error={errors.applicationDeadline}
          />

          <Field
            id="dcgpa"
            label="Minimum CGPA (optional)"
            type="number"
            step="0.01"
            value={form.minCgpa}
            onChange={set('minCgpa')}
            error={errors.minCgpa}
          />

          <Field
            id="dback"
            label="Max active backlogs (optional)"
            type="number"
            value={form.maxActiveBacklogs}
            onChange={set('maxActiveBacklogs')}
            error={errors.maxActiveBacklogs}
          />

          <Field
            id="dbranch"
            label="Eligible branches, comma separated (empty = all)"
            value={form.branches}
            onChange={set('branches')}
          />

          <Field
            id="dctc"
            label="CTC in LPA (optional)"
            type="number"
            step="0.01"
            value={form.ctcLpa}
            onChange={set('ctcLpa')}
            error={errors.ctcLpa}
          />

          <Field
            id="dloc"
            label="Location (optional)"
            value={form.location}
            onChange={set('location')}
          />

          <Field
            id="ddesc"
            label="Description (optional)"
            value={form.description}
            onChange={set('description')}
          />

          <div className="sm:col-span-2">
            <button type="submit" className="btn mt-4">
              Save draft
            </button>
          </div>
        </form>
      </section>

      <section className="card" aria-labelledby="drive-list">
        <h2 id="drive-list" className="text-lg">
          All drives{drives ? ` (${drives.total})` : ''}
        </h2>

        {drives === null && (
          <p role="status" className="mt-2 text-mute">
            Loading…
          </p>
        )}

        {drives && drives.items.length === 0 && (
          <p className="mt-2 text-mute">
            No drives yet.
          </p>
        )}

        {drives && drives.items.length > 0 && (
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="text-mute">
                  <th className="py-2">Company and role</th>
                  <th>Batch</th>
                  <th>Criteria</th>
                  <th>Deadline</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {drives.items.map((d) => (
                  <tr
                    key={d.id}
                    className="border-t border-line align-top"
                  >
                    <td className="py-2">
                      {d.companyName}
                      <div className="text-xs text-mute">
                        {d.title}
                      </div>
                    </td>

                    <td>{d.graduationYear}</td>

                    <td className="text-xs text-mute">
                      {d.minCgpa != null
                        ? `CGPA ≥ ${d.minCgpa}`
                        : 'Any CGPA'}
                      ;{' '}
                      {d.maxActiveBacklogs != null
                        ? `≤ ${d.maxActiveBacklogs} backlogs`
                        : 'any backlogs'}
                      ;{' '}
                      {d.branches?.length
                        ? d.branches.join(', ')
                        : 'all branches'}
                    </td>

                    <td>
                      {formatDateTime(d.applicationDeadline)}
                    </td>

                    <td>
                      <StatusPill status={d.status} />
                    </td>

                    <td className="space-x-2 whitespace-nowrap">
                      {d.status === 'DRAFT' && (
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() => move(d, 'OPEN')}
                        >
                          Publish
                          <span className="sr-only"> {d.title}</span>
                        </button>
                      )}

                      {d.status === 'OPEN' && (
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() =>
                            move(
                              d,
                              'CLOSED',
                              'Close this drive to new applications?'
                            )
                          }
                        >
                          Close
                          <span className="sr-only"> {d.title}</span>
                        </button>
                      )}

                      {(d.status === 'DRAFT' ||
                        d.status === 'OPEN') && (
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() =>
                            move(
                              d,
                              'CANCELLED',
                              'Cancel this drive? This cannot be undone.'
                            )
                          }
                        >
                          Cancel
                          <span className="sr-only"> {d.title}</span>
                        </button>
                      )}

                      {d.status !== 'CANCELLED' && (
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() =>
                            setRoundDriveId(
                              roundDriveId === d.id ? null : d.id
                            )
                          }
                        >
                          {roundDriveId === d.id
                            ? 'Hide rounds'
                            : 'Manage rounds'}
                          <span className="sr-only"> for {d.title}</span>
                        </button>
                      )}

                      {d.status === 'CANCELLED' && (
                        <button
                          type="button"
                          className="btn-ghost"
                          onClick={() => removeDrive(d)}
                        >
                          Remove drive
                          <span className="sr-only"> {d.title}</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {selectedDrive && (
          <DriveRounds drive={selectedDrive} />
        )}

        <div className="mt-3 flex items-center gap-3 text-sm">
          <button
            type="button"
            className="btn-ghost"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Previous
          </button>

          <span>
            Page {page + 1} of {pages}
          </span>

          <button
            type="button"
            className="btn-ghost"
            disabled={page + 1 >= pages}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      </section>
    </main>
  );
}
