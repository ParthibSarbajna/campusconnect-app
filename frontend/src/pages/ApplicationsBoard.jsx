import { useCallback, useEffect, useState } from 'react';
import { listApplications, setApplicationStatus } from '../api/applications.js';
import StatusPill from '../components/StatusPill.jsx';
import { formatDateTime } from '../lib/format.js';

const STATUSES = ['APPLIED', 'SHORTLISTED', 'SELECTED', 'REJECTED', 'WITHDRAWN'];
const ACTIONS = {
  APPLIED: [['SHORTLISTED', 'Shortlist'], ['REJECTED', 'Reject']],
  SHORTLISTED: [['SELECTED', 'Select'], ['REJECTED', 'Reject']],
};

/** Shared by the placement admin (canManage) and the department SPOC (read-only, limited to their department by the server). */
export default function ApplicationsBoard({ canManage = false, title = 'Applications' }) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    try { setData(await listApplications({ status, page })); setError(''); }
    catch (e) { setError(e.message); setData({ items: [], page: 0, size: 25, total: 0 }); }
  }, [status, page]);
  useEffect(() => { load(); }, [load]);

  async function act(row, target) {
    setNotice('');
    try {
      await setApplicationStatus(row.id, target);
      setNotice(`${row.studentName}: ${row.companyName} is now ${target}.`);
      load();
    } catch (e) { setError(e.message); }
  }

  const pages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;
  return (
    <main className="mx-auto grid max-w-6xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">{title}</h1>
      <div>
        <label htmlFor="status-filter" className="mr-2 text-sm font-bold">Status</label>
        <select id="status-filter" className="rounded-lg border border-line bg-surf px-2 py-1" value={status}
          onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
          <option value="">All</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>
      {notice && <p role="status" className="rounded-lg bg-okt p-3 text-sm text-ok">{notice}</p>}
      {error && <p role="alert" className="rounded-lg bg-badt p-3 text-sm text-bad">{error}</p>}
      {data === null && <p role="status" className="text-mute">Loading…</p>}
      {data && data.items.length === 0 && !error && <p className="card text-mute">No applications match.</p>}
      {data && data.items.length > 0 && (
        <div className="card overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="text-mute"><th className="py-2">Student</th><th>Branch / batch</th><th>CGPA</th><th>Company and role</th><th>Applied</th><th>Status</th>{canManage && <th />}</tr>
            </thead>
            <tbody>
              {data.items.map((r) => (
                <tr key={r.id} className="border-t border-line align-top">
                  <td className="py-2">{r.studentName}<div className="text-xs text-mute">{r.studentEmail}</div></td>
                  <td>{r.branch || '—'} / {r.graduationYear || '—'}</td>
                  <td>{r.cgpa ?? '—'}</td>
                  <td>{r.companyName}<div className="text-xs text-mute">{r.driveTitle}</div></td>
                  <td>{formatDateTime(r.appliedAt)}</td>
                  <td><StatusPill status={r.status} /></td>
                  {canManage && (
                    <td className="space-x-2 whitespace-nowrap">
                      {(ACTIONS[r.status] || []).map(([target, label]) => (
                        <button key={target} type="button" className="btn-ghost" onClick={() => act(r, target)}>
                          {label}<span className="sr-only"> {r.studentName} at {r.companyName}</span>
                        </button>
                      ))}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.total > data.size && (
        <div className="flex items-center gap-3 text-sm">
          <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>Page {page + 1} of {pages}</span>
          <button type="button" className="btn-ghost" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </main>
  );
}
