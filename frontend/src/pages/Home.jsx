import { Building2, GraduationCap, UserCog } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { homePathFor } from '../auth/roles.js';
import OpenDrivesBoard from '../components/OpenDrivesBoard.jsx';
import StatusCard from '../components/StatusCard.jsx';

const ROLES = [
  { icon: GraduationCap, name: 'Students', text: 'Track every company and round, check eligibility, improve your resume.' },
  { icon: UserCog, name: 'Department SPOCs', text: 'See your department’s students, applications and placement progress.' },
  { icon: Building2, name: 'Placement cell', text: 'Run drives, update round results and view ten years of analytics.' },
];

export default function Home() {
  const { status, user } = useAuth();
  return (
    <main className="mx-auto grid max-w-6xl gap-8 px-4 py-8 md:grid-cols-[1.1fr_1fr] md:px-6 md:py-12">
      <div>
        <h1 className="text-4xl font-extrabold leading-tight md:text-5xl">One place for every placement round.</h1>
        <p className="mt-4 max-w-prose text-lg text-mute">
          Students, department coordinators and the placement cell work from one shared record instead of spreadsheets and email threads.
        </p>
        <div className="mt-6 flex flex-wrap items-center gap-3">
          {status === 'authenticated' ? (
            <Link to={homePathFor(user.role)} className="btn">Open my dashboard</Link>
          ) : (
            <>
              <Link to="/signin" className="btn">Sign in</Link>
              <Link to="/register" className="btn-ghost">Register as a student</Link>
            </>
          )}
        </div>
        <ul className="mt-8 grid gap-3">
          {ROLES.map(({ icon: Icon, name, text }) => (
            <li key={name} className="card flex gap-3">
              <Icon size={22} className="mt-0.5 shrink-0 text-pri" aria-hidden="true" />
              <div>
                <h3 className="text-base">{name} <span className="pill bg-warnt text-warn">Planned</span></h3>
                <p className="text-sm text-mute">{text}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
      <div className="grid content-start gap-4">
        <OpenDrivesBoard />
        <StatusCard />
      </div>
    </main>
  );
}
