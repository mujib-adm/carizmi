import { useState, useEffect } from "react";

export function useAuth() {
  const [token, setToken] = useState<string | null>(localStorage.getItem("token"));
  const [role, setRole] = useState<string | null>(localStorage.getItem("role"));
  useEffect(() => { if (token) localStorage.setItem("token", token); }, [token]);
  useEffect(() => { if (role) localStorage.setItem("role", role); }, [role]);
  return { token, role, setToken, setRole };
}