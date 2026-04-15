import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './features/identity/pages/Login';
import Logout from './features/identity/pages/Logout';
import AppLayout from './components/layout/AppLayout';
import { useAuth } from './hooks/useAuth';
import { RoleConstants } from './constants/RoleConstants';
import GradientSpinner from './components/GradientSpinner';

// Lazy-loaded pages – reduces initial bundle size
const ProfilePage = lazy(() => import('./features/identity/pages/ProfilePage'));
const Dashboard = lazy(() => import('./features/platform/pages/Dashboard'));
const ExpensePage = lazy(() => import('./features/finance/pages/ExpensePage'));
const MemberDetailsPage = lazy(() => import('./features/membership/pages/MemberDetailsPage'));
const MemberPage = lazy(() => import('./features/membership/pages/MemberPage'));
const PaymentPage = lazy(() => import('./features/finance/pages/PaymentPage'));
const QuarterlyChecklistPage = lazy(() => import('./features/finance/pages/QuarterlyChecklistPage'));
const ReferencePage = lazy(() => import('./features/platform/pages/ReferencePage'));
const SystemSettingsPage = lazy(() => import('./features/platform/pages/SystemSettingsPage'));
const UsersPage = lazy(() => import('./features/identity/pages/UsersPage'));
const Register = lazy(() => import('./features/identity/pages/Register'));

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
          {role === RoleConstants.ROLE_ADMIN && <Route path="/register" element={<Register />} />}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>

        <Route path="/logout" element={<Logout />} />
      </Routes>
    </Suspense>
  );
}