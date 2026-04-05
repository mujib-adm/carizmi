import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './pages/auth/Login';
import Logout from './pages/auth/Logout';
import AppLayout from './components/layout/AppLayout';
import { useAuth } from './hooks/useAuth';
import { RoleConstants } from './constants/RoleConstants';
import GradientSpinner from './components/GradientSpinner';

// Lazy-loaded pages – reduces initial bundle size
const ProfilePage = lazy(() => import('./pages/auth/ProfilePage'));
const Dashboard = lazy(() => import('./pages/dashboard/Dashboard'));
const ExpensePage = lazy(() => import('./pages/expense/ExpensePage'));
const MemberDetailsPage = lazy(() => import('./pages/member/MemberDetailsPage'));
const MemberPage = lazy(() => import('./pages/member/MemberPage'));
const PaymentPage = lazy(() => import('./pages/payment/PaymentPage'));
const QuarterlyChecklistPage = lazy(() => import('./pages/checklist/QuarterlyChecklistPage'));
const ReferencePage = lazy(() => import('./pages/reference/ReferencePage'));
const SystemSettingsPage = lazy(() => import('./pages/settings/SystemSettingsPage'));
const UsersPage = lazy(() => import('./pages/admin/UsersPage'));
const Register = lazy(() => import('./pages/auth/Register'));

const PageSpinner = () => (
  <div className="page-spinner">
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
        {/* All sidebar pages share AppLayout as a single layout route */}
        <Route element={<AppLayout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/members" element={<MemberPage />} />
          <Route path="/members/:id" element={<MemberDetailsPage />} />
          <Route path="/payments" element={<PaymentPage />} />
          <Route path="/checklist" element={<QuarterlyChecklistPage />} />
          <Route path="/expenses" element={<ExpensePage />} />
          <Route path="/references" element={<ReferencePage />} />
          <Route path="/settings" element={<SystemSettingsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          {role === RoleConstants.ROLE_ADMIN && <Route path="/users" element={<UsersPage />} />}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>

        <Route path="/register" element={<Register />} />
        <Route path="/logout" element={<Logout />} />
      </Routes>
    </Suspense>
  );
}