import { createContext, useContext, useEffect, useState } from "react";
import { setAuthToken } from "../apiclient/ApiClient";

type AuthContextType = {
  token: string | null;
  role: string;
  firstName: string;
  isLoading: boolean;
  login: (token: string, role: string, firstName: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType>({
  token: null,
  role: "User",
  firstName: "",
  isLoading: true,
  login: () => { },
  logout: () => { }
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isLoading, setIsLoading] = useState(true);
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [role, setRole] = useState(localStorage.getItem("role") || "User");
  const [firstName, setFirstName] = useState(localStorage.getItem("firstName") || "");

  useEffect(() => {
    setAuthToken(token); // keep ApiClient in sync
  }, [token]);

  const login = (newToken: string, newRole: string, newFirstName: string) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("role", newRole);
    localStorage.setItem("firstName", newFirstName);
    setToken(newToken);
    setRole(newRole);
    setFirstName(newFirstName);
  };

  const logout = () => {
    localStorage.clear();
    setToken(null);
    setRole("User");
    setFirstName("");
  };

  useEffect(() => {
    setToken(localStorage.getItem("token"));
    setRole(localStorage.getItem("role") || "User");
    setFirstName(localStorage.getItem("firstName") || "");
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem("token");
    const storedRole = localStorage.getItem("role");
    const storedFirstName = localStorage.getItem("firstName");

    if (storedToken) {
      // validate token with backend
      fetch(`${import.meta.env.VITE_API_URL}/auth/profile`, {
        headers: { Authorization: `Bearer ${storedToken}` }
      })
        .then(res => {
          if (!res.ok) throw new Error("Invalid token");
          return res.json();
        })
        .then(data => {
          setToken(storedToken);
          setRole(storedRole || "User");
          if (data.responseData) {
            const fName = data.responseData.firstName || storedFirstName || "";
            setFirstName(fName);
            localStorage.setItem("firstName", fName);
          }
        })
        .catch(() => {
          localStorage.clear();
          setToken(null);
          setRole("User");
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ token, role, firstName, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}