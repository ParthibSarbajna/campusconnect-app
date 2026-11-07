import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { applyToDrive, listOpenDrives } from '../api/drives.js';
import StatusPill from '../components/StatusPill.jsx';
import { formatDateTime } from '../lib/format.js';

export default function StudentDrives() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    try { setData(await listOpenDrives(page)); setError(''); }
    catch (e) { setError(e.message); setData({ items: [], page: 0, size: 20, total: 0 }); }
  }, [page]);
  useEffect(() => { load(); }, [load]);

  async function apply(view) {
    setNotice('');
    setError('');
    setBusyId(view.drive.id);
    try {
      await applyToDrive(view.drive.id);
      setNotice(`Applied to ${view.drive.companyName}.`);
      await load();
    } catch (e) { setError(e.message); }
    finally { setBusyId(null); }
  }

  const pages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;
  return (
    <main className="mx-auto grid max-w-4xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">Open drives</h1>
      <p className="text-sm text-mute">Eligibility is checked against your saved profile. Keep your <Link to="/student" className="font-bold text-pri">profile</Link> up to date.</p>
      {notice && <p role="status" className="rounded-lg bg-okt p-3 text-sm text-ok">{notice}</p>}
      {error && <p role="alert" className="rounded-lg bg-badt p-3 text-sm text-bad">{error}</p>}
      {data === null && <p role="status" className="text-mute">Loading…</p>}
      {data && data.items.length === 0 && !error && <p className="card text-mute">No drives are open for applications right now.</p>}

      {data && data.items.map((v) => (
        <article key={v.drive.id} className="card" aria-label={`${v.drive.companyName} ${v.drive.title}`}>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg">{v.drive.companyName}</h2>
            <span className="text-mute">{v.drive.title}</span>
            <span className="flex-1" />
            {v.drive.ctcLpa != null && <span className="pill bg-prit text-pri">{v.drive.ctcLpa} LPA</span>}
          </div>
          <p className="mt-1 text-sm text-mute">
            Batch {v.drive.graduationYear}
            {v.drive.location ? ` · ${v.drive.location}` : ''} · Apply by {formatDateTime(v.drive.applicationDeadline)}
          </p>
          {v.drive.description && <p className="mt-2 text-sm">{v.drive.description}</p>}
          <p className="mt-2 text-xs text-mute">
            Requires: {v.drive.minCgpa != null ? `CGPA ${v.drive.minCgpa}+` : 'any CGPA'};{' '}
            {v.drive.maxActiveBacklogs != null ? `at most ${v.drive.maxActiveBacklogs} active backlogs` : 'any backlogs'};{' '}
            {v.drive.branches.length ? v.drive.branches.join(', ') : 'all branches'}
          </p>
          <div className="mt-3">
            {v.applicationId ? (
              <span>Your application: <StatusPill status={v.applicationStatus} /></span>
            ) : v.eligibility.eligible ? (
              <button type="button" className="btn" disabled={busyId === v.drive.id} onClick={() => apply(v)}>
                {busyId === v.drive.id ? 'Applying…' : 'Apply'}<span className="sr-only"> to {v.drive.companyName}</span>
              </button>
            ) : (
              <div>
                <p className="text-sm font-bold text-bad">You are not eligible yet</p>
                <ul className="mt-1 list-disc pl-5 text-sm text-mute">
                  {v.eligibility.reasons.map((r) => <li key={r}>{r}</li>)}
                </ul>
              </div>
            )}
          </div>
        </article>
      ))}

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
