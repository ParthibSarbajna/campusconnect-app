import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { ROLE_LABELS } from '../auth/roles.js';
import ThemeToggle from './ThemeToggle.jsx';

const NAV = {
  STUDENT: [
    { to: '/student', label: 'My account', end: true },
    { to: '/student/drives', label: 'Drives' },
    { to: '/student/applications', label: 'Applications' },
  ],
  DEPARTMENT_SPOC: [
    { to: '/spoc', label: 'Department', end: true },
    { to: '/spoc/applications', label: 'Applications' },
  ],
  PLACEMENT_ADMIN: [
    { to: '/admin', label: 'Administration', end: true },
    { to: '/admin/companies', label: 'Companies' },
    { to: '/admin/drives', label: 'Drives' },
    { to: '/admin/applications', label: 'Applications' },
  ],
};

export default function Layout() {
  const { status, user, logout } = useAuth();
  const navigate = useNavigate();

  async function onSignOut() {
    await logout();
    navigate('/', { replace: true });
  }

  return (
    <div style={{ paddingTop: 'env(safe-area-inset-top, 0px)', paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}>
      <header className="sticky top-0 z-10 flex flex-wrap items-center gap-3 border-b border-line bg-surf px-5 py-3">
        <Link to="/" className="font-display text-xl font-extrabold">
          Campus<span className="text-pri">Connect</span>
        </Link>
        <nav aria-label="Main" className="flex items-center gap-3 text-sm font-medium">
          {status === 'authenticated' && (NAV[user.role] || []).map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => (isActive ? 'text-pri' : 'text-mute')}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <span className="flex-1" />
        {status === 'authenticated' ? (
          <>
            <span className="text-sm text-mute">{user.fullName} · {ROLE_LABELS[user.role]}</span>
            <button type="button" className="btn-ghost" onClick={onSignOut}>Sign out</button>
          </>
        ) : status === 'unauthenticated' ? (
          <>
            <Link to="/signin" className="btn-ghost">Sign in</Link>
            <Link to="/register" className="btn-ghost">Register</Link>
          </>
        ) : null}
        <ThemeToggle />
      </header>
      <Outlet />
    </div>
  );
}
