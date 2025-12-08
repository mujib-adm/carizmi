import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import apiClient from "../../context/ApiClient";
import { GlobalResponse } from "../../constants/types";

export default function Logout() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  useEffect(() => {
    const doLogout = async () => {
      const token = localStorage.getItem("token");

      try {
        const response = await apiClient.post<GlobalResponse>("/auth/logout", {}, { headers: { Authorization: `Bearer ${token}` }, });

      } catch (err) {
        console.error("Logout API call failed", err);
      }

      logout(); // clear local storage/context
      navigate("/login"); // redirect
    };

    doLogout();
  }, [logout, navigate]);

}