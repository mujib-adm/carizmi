import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './auth/Login';
import Logout from './auth/Logout';
import ProfilePage from './auth/ProfilePage';
import Register from './auth/Register';
import Dashboard from './dashboard/Dashboard';
// import MembersList from "./pages/MembersList";
// import PaymentsList from "./pages/PaymentsList";
// import ExpensesList from "./pages/ExpensesList";
// import Reporting from "./pages/Reporting";
// import Settings from "./pages/Settings";
// import Users from "./pages/Users";
// import NotFound from "./pages/NotFound";
// import Unauthorized from "./pages/Unauthorized";
import { useAuth } from '../context/AuthContext';
import ExpensePage from './expense/ExpensePage';
import MemberDetailsPage from './member/MemberDetailsPage';
import MemberPage from './member/MemberPage';
import PaymentPage from './payment/PaymentPage';
import ReferencePage from './reference/ReferencePage';
import SystemSettingsPage from './settings/SystemSettingsPage';
import UsersPage from './admin/UsersPage';
import { RoleConstants } from '../constants/RoleConstants';

export default function Router() {
  const { isAuthenticated, isLoading, role } = useAuth();

  if (isLoading) return <div>Loading...</div>;

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<Navigate to="/login" />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/register" element={<Register />} />
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/members" element={<MemberPage />} />
      <Route path="/members/:id" element={<MemberDetailsPage />} />
      <Route path="/payments" element={<PaymentPage />} />
      <Route path="/expenses" element={<ExpensePage />} />
      <Route path="/references" element={<ReferencePage />} />
      <Route path="/settings" element={<SystemSettingsPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      {role === RoleConstants.ROLE_ADMIN && <Route path="/users" element={<UsersPage />} />}

      <Route path="/logout" element={<Logout />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}