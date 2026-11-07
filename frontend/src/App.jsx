import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import { ROLES } from './auth/roles.js';
import AdminCompanies from './pages/AdminCompanies.jsx';
import AdminDrives from './pages/AdminDrives.jsx';
import AdminHome from './pages/AdminHome.jsx';
import ApplicationsBoard from './pages/ApplicationsBoard.jsx';
import Forbidden from './pages/Forbidden.jsx';
import Home from './pages/Home.jsx';
import NotFound from './pages/NotFound.jsx';
import Register from './pages/Register.jsx';
import SignIn from './pages/SignIn.jsx';
import SpocHome from './pages/SpocHome.jsx';
import StudentApplications from './pages/StudentApplications.jsx';
import StudentDrives from './pages/StudentDrives.jsx';
import StudentHome from './pages/StudentHome.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/signin" element={<SignIn />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forbidden" element={<Forbidden />} />
        <Route path="/student" element={<ProtectedRoute roles={[ROLES.STUDENT]}><StudentHome /></ProtectedRoute>} />
        <Route path="/spoc" element={<ProtectedRoute roles={[ROLES.SPOC]}><SpocHome /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute roles={[ROLES.ADMIN]}><AdminHome /></ProtectedRoute>} />
        <Route path="/student/drives" element={<ProtectedRoute roles={[ROLES.STUDENT]}><StudentDrives /></ProtectedRoute>} />
        <Route path="/student/applications" element={<ProtectedRoute roles={[ROLES.STUDENT]}><StudentApplications /></ProtectedRoute>} />
        <Route path="/spoc/applications" element={<ProtectedRoute roles={[ROLES.SPOC]}><ApplicationsBoard title="Department applications" /></ProtectedRoute>} />
        <Route path="/admin/companies" element={<ProtectedRoute roles={[ROLES.ADMIN]}><AdminCompanies /></ProtectedRoute>} />
        <Route path="/admin/drives" element={<ProtectedRoute roles={[ROLES.ADMIN]}><AdminDrives /></ProtectedRoute>} />
        <Route path="/admin/applications" element={<ProtectedRoute roles={[ROLES.ADMIN]}><ApplicationsBoard canManage title="Applications" /></ProtectedRoute>} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
