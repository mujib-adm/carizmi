import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./auth/Login";
import Logout from "./auth/Logout";
import Register from "./auth/Register";
import Dashboard from "./dashboard/Dashboard";
// import MembersList from "./pages/MembersList";
// import PaymentsList from "./pages/PaymentsList";
// import ExpensesList from "./pages/ExpensesList";
// import Reporting from "./pages/Reporting";
// import Settings from "./pages/Settings";
// import Users from "./pages/Users";
// import NotFound from "./pages/NotFound";
// import Unauthorized from "./pages/Unauthorized";
import { useAuth } from "../context/AuthContext";
import MemberPage from "./member/MemberPage";
import PaymentPage from "./payment/PaymentPage";
import SystemSettingsPage from "./settings/SystemSettingsPage";

export default function Router() {
  const { token, isLoading } = useAuth();

if (isLoading) return <div>Loading...</div>;

  if (!token) {
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
      <Route path="/payments" element={<PaymentPage />} /> 
      <Route path="/settings" element={<SystemSettingsPage />} />

      <Route path="/logout" element={<Logout />} />
    </Routes >
  );
}