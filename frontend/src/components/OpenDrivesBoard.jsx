
import { useEffect, useState } from 'react';
import { listPublicDrives } from '../api/drives.js';

export default function OpenDrivesBoard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    async function loadDrives() {
      try {
        const result = await listPublicDrives();
        setData(result);
      } catch (e) {
        setError(e.message || 'Unable to load placement drives.');
        setData({ items: [] });
      }
    }

    loadDrives();
  }, []);

  return (
    <div
      className="rounded-2xl bg-board p-4 text-amber"
      aria-label="Open placement drives"
    >
      <h3 className="mb-2 text-base text-white">
        Open placement drives
      </h3>

      {data === null && (
        <p role="status" className="py-3 text-sm opacity-70">
          Loading drives...
        </p>
      )}

      {error && (
        <p role="alert" className="py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      {data && data.items.length === 0 && !error && (
        <p className="py-3 text-sm opacity-70">
          No placement drives are currently open.
        </p>
      )}

      {data && data.items.map((drive) => (
        <div
          key={drive.id}
          className="grid grid-cols-[1.2fr_1.4fr_.8fr] gap-2 border-t border-dashed border-[#2c3858] py-2 text-sm"
        >
          <span>{drive.companyName}</span>
          <span>{drive.title}</span>
          <span className="text-right text-emerald-300">
            {drive.ctcLpa != null ? `${drive.ctcLpa} LPA` : 'Not specified'}
          </span>
        </div>
      ))}
    </div>
  );
}
