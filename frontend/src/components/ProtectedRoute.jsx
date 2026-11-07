import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

/** UI convenience only. The backend independently enforces every permission. */
export default function ProtectedRoute({ roles, children }) {
  const { status, user } = useAuth();
  const location = useLocation();
  if (status === 'loading') {
    return <p role="status" className="p-8 text-mute">Checking your session…</p>;
  }
  if (status !== 'authenticated') {
    return <Navigate to="/signin" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }
  return children;
}
