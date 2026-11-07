
import { useEffect, useState } from 'react';
import {
  assignApplicationToRound,
  createDriveRound,
  listDriveRounds,
  listRoundParticipants,
  updateRoundParticipantOutcome,
} from '../api/rounds.js';
import { listApplications } from '../api/applications.js';
import { formatDateTime } from '../lib/format.js';

const OUTCOMES = ['PENDING', 'PASSED', 'FAILED', 'ABSENT'];

export default function DriveRounds({ drive }) {
  const [rounds, setRounds] = useState([]);
  const [applications, setApplications] = useState([]);
  const [participants, setParticipants] = useState({});
  const [selectedRound, setSelectedRound] = useState('');
  const [form, setForm] = useState({
    name: '',
    sequenceNumber: '',
    scheduledAt: '',
    venue: '',
    instructions: '',
  });
  const [assignments, setAssignments] = useState({});
  const [feedback, setFeedback] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  async function loadRounds() {
    try {
      const data = await listDriveRounds(drive.id);
      setRounds(data);
      if (data.length && !selectedRound) {
        setSelectedRound(String(data[0].id));
      }
    } catch (e) {
      setError(e.message);
    }
  }

  async function loadApplications() {
    try {
      const data = await listApplications({ driveId: drive.id, size: 100 });
      setApplications(data.items || []);
    } catch (e) {
      setError(e.message);
    }
  }

  async function loadParticipants(roundId) {
    if (!roundId) return;
    try {
      const data = await listRoundParticipants(roundId);
      setParticipants((old) => ({ ...old, [roundId]: data }));
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => {
    loadRounds();
    loadApplications();
  }, [drive.id]);

  useEffect(() => {
    loadParticipants(selectedRound);
  }, [selectedRound]);

  async function createRound(e) {
    e.preventDefault();
    setError('');
    setNotice('');

    if (!form.name.trim() || Number(form.sequenceNumber) <= 0) {
      setError('Enter a round name and a positive sequence number.');
      return;
    }

    setLoading(true);
    try {
      await createDriveRound(drive.id, {
        name: form.name.trim(),
        sequenceNumber: Number(form.sequenceNumber),
        scheduledAt: form.scheduledAt
          ? new Date(form.scheduledAt).toISOString()
          : null,
        venue: form.venue.trim() || null,
        instructions: form.instructions.trim() || null,
      });

      setForm({
        name: '',
        sequenceNumber: '',
        scheduledAt: '',
        venue: '',
        instructions: '',
      });
      setNotice('Recruitment round created.');
      await loadRounds();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function assign(roundId) {
    const applicationId = assignments[roundId];
    if (!applicationId) {
      setError('Choose an application first.');
      return;
    }

    setError('');
    setNotice('');
    try {
      await assignApplicationToRound(roundId, Number(applicationId));
      setNotice('Application assigned to the round.');
      setAssignments((old) => ({ ...old, [roundId]: '' }));
      await loadParticipants(roundId);
    } catch (e) {
      setError(e.message);
    }
  }

  async function saveOutcome(roundId, participant) {
    const outcome = participant.outcome;
    try {
      await updateRoundParticipantOutcome(
        roundId,
        participant.id,
        outcome,
        feedback[participant.id] || null
      );
      setNotice('Round outcome saved.');
      await loadParticipants(roundId);
    } catch (e) {
      setError(e.message);
    }
  }

  const currentParticipants = participants[selectedRound] || [];
  const assignedIds = currentParticipants.map((p) => p.applicationId);

  return (
    <section className="mt-4 rounded-lg border border-line p-4">
      <h3 className="text-lg font-bold">
        Recruitment rounds — {drive.companyName} / {drive.title}
      </h3>

      {notice && <p role="status" className="mt-2 text-sm text-ok">{notice}</p>}
      {error && <p role="alert" className="mt-2 text-sm text-bad">{error}</p>}

      <form onSubmit={createRound} className="mt-3 grid gap-3 sm:grid-cols-2">
        <label className="grid gap-1 text-sm">
          Round name
          <input
            className="rounded-lg border border-line bg-surf px-3 py-2"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="Technical Interview"
            required
          />
        </label>

        <label className="grid gap-1 text-sm">
          Round number
          <input
            type="number"
            min="1"
            className="rounded-lg border border-line bg-surf px-3 py-2"
            value={form.sequenceNumber}
            onChange={(e) => setForm({ ...form, sequenceNumber: e.target.value })}
            required
          />
        </label>

        <label className="grid gap-1 text-sm">
          Scheduled date and time
          <input
            type="datetime-local"
            className="rounded-lg border border-line bg-surf px-3 py-2"
            value={form.scheduledAt}
            onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
          />
        </label>

        <label className="grid gap-1 text-sm">
          Venue
          <input
            className="rounded-lg border border-line bg-surf px-3 py-2"
            value={form.venue}
            onChange={(e) => setForm({ ...form, venue: e.target.value })}
          />
        </label>

        <label className="grid gap-1 text-sm sm:col-span-2">
          Instructions
          <textarea
            className="rounded-lg border border-line bg-surf px-3 py-2"
            value={form.instructions}
            onChange={(e) => setForm({ ...form, instructions: e.target.value })}
          />
        </label>

        <button className="btn sm:col-span-2" type="submit" disabled={loading}>
          {loading ? 'Creating…' : 'Create round'}
        </button>
      </form>

      {rounds.length === 0 && (
        <p className="mt-3 text-sm text-mute">No recruitment rounds created yet.</p>
      )}

      {rounds.length > 0 && (
        <div className="mt-4">
          <label className="mb-1 block text-sm font-bold" htmlFor={`round-${drive.id}`}>
            Select round
          </label>
          <select
            id={`round-${drive.id}`}
            className="w-full rounded-lg border border-line bg-surf px-3 py-2"
            value={selectedRound}
            onChange={(e) => setSelectedRound(e.target.value)}
          >
            {rounds.map((r) => (
              <option key={r.id} value={r.id}>
                Round {r.sequenceNumber}: {r.name}
              </option>
            ))}
          </select>

          {rounds
            .filter((r) => String(r.id) === selectedRound)
            .map((r) => (
              <div key={r.id} className="mt-3 rounded-lg bg-surf p-3">
                <p><strong>{r.name}</strong></p>
                <p className="text-sm">{formatDateTime(r.scheduledAt) || 'Not scheduled'}</p>
                {r.venue && <p className="text-sm">Venue: {r.venue}</p>}
                {r.instructions && <p className="text-sm">{r.instructions}</p>}
                <p className="text-sm">Status: {r.status}</p>
              </div>
            ))}

          <div className="mt-4 grid gap-2 sm:grid-cols-[1fr_auto]">
            <select
              className="rounded-lg border border-line bg-surf px-3 py-2"
              value={assignments[selectedRound] || ''}
              onChange={(e) =>
                setAssignments((old) => ({ ...old, [selectedRound]: e.target.value }))
              }
            >
              <option value="">Choose an application</option>
              {applications
                .filter((a) => !assignedIds.includes(a.id))
                .map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.studentName} — {a.studentEmail} ({a.status})
                  </option>
                ))}
            </select>
            <button type="button" className="btn" onClick={() => assign(selectedRound)}>
              Assign application
            </button>
          </div>

          <h4 className="mt-4 font-bold">Assigned applicants</h4>
          {currentParticipants.length === 0 && (
            <p className="mt-2 text-sm text-mute">No applicants assigned to this round.</p>
          )}

          {currentParticipants.map((p) => {
            const app = applications.find((a) => a.id === p.applicationId);
            return (
              <div key={p.id} className="mt-2 grid gap-2 rounded-lg border border-line p-3 sm:grid-cols-3">
                <div>
                  <p className="font-bold">{app?.studentName || `Application ${p.applicationId}`}</p>
                  <p className="text-xs text-mute">{app?.studentEmail || ''}</p>
                </div>

                <select
                  className="rounded-lg border border-line bg-surf px-3 py-2"
                  value={p.outcome}
                  onChange={(e) =>
                    setParticipants((old) => ({
                      ...old,
                      [selectedRound]: old[selectedRound].map((item) =>
                        item.id === p.id ? { ...item, outcome: e.target.value } : item
                      ),
                    }))
                  }
                >
                  {OUTCOMES.map((o) => <option key={o} value={o}>{o}</option>)}
                </select>

                <button type="button" className="btn" onClick={() => saveOutcome(selectedRound, p)}>
                  Save outcome
                </button>

                <textarea
                  className="rounded-lg border border-line bg-surf px-3 py-2 sm:col-span-3"
                  placeholder="Feedback (optional)"
                  value={feedback[p.id] ?? p.feedback ?? ''}
                  onChange={(e) => setFeedback((old) => ({ ...old, [p.id]: e.target.value }))}
                />
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
