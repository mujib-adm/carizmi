import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './auth/Login';
import Logout from './auth/Logout';
import { useAuth } from '../context/AuthContext';
import { RoleConstants } from '../constants/RoleConstants';
import GradientSpinner from '../context/GradientSpinnerContext';

// Lazy-loaded pages – reduces initial bundle size
const ProfilePage = lazy(() => import('./auth/ProfilePage'));
const Dashboard = lazy(() => import('./dashboard/Dashboard'));
const ExpensePage = lazy(() => import('./expense/ExpensePage'));
const MemberDetailsPage = lazy(() => import('./member/MemberDetailsPage'));
const MemberPage = lazy(() => import('./member/MemberPage'));
const PaymentPage = lazy(() => import('./payment/PaymentPage'));
const ReferencePage = lazy(() => import('./reference/ReferencePage'));
const SystemSettingsPage = lazy(() => import('./settings/SystemSettingsPage'));
const UsersPage = lazy(() => import('./admin/UsersPage'));
const Register = lazy(() => import('./auth/Register'));

const PageSpinner = () => (
  <div style={{ position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
    <GradientSpinner />
  </div>
);

export default function Router() {
  const { isAuthenticated, isLoading, role } = useAuth();

  if (isLoading) return <PageSpinner />;

  if (!isAuthenticated) {
    return (
      <Suspense fallback={<PageSpinner />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
      </Suspense>
    );
  }

  return (
    <Suspense fallback={<PageSpinner />}>
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
    </Suspense>
  );
}