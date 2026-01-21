import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import apiClient from "../../apiclient/ApiClient";
import { GlobalResponse } from "../../constants/types";
import { useAuth } from "../../context/AuthContext";
import { useNotification } from "../../context/NotificationContext";

export default function Logout() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const notify = useNotification();

  useEffect(() => {
    const doLogout = async () => {
      const token = localStorage.getItem("token");

      try {
        const refreshToken = localStorage.getItem("refreshToken");
        await apiClient.post<GlobalResponse>("/auth/logout", { refreshToken }, { headers: { Authorization: `Bearer ${token}` }, });

      } catch (err) {
        notify.warning({ message: "Logout Warning", description: "Server-side logout may have failed, but you have been logged out locally." });
      }

      logout(); // clear local storage/context
      navigate("/login"); // redirect
    };

    doLogout();
  }, [logout, navigate, notify]);

  return <div>Logging out...</div>;
}