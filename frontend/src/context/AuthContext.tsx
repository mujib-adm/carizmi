import { createContext, useContext, useEffect, useState } from "react";

type AuthContextType = {
  token: string | null;
  role: string;
  isLoading: boolean;
  login: (token: string, role: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType>({
  token: null,
  role: "User",
  isLoading: true,
  login: () => { },
  logout: () => { }
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isLoading, setIsLoading] = useState(true);
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [role, setRole] = useState(localStorage.getItem("role") || "User");

  const login = (newToken: string, newRole: string) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("role", newRole);
    setToken(newToken);
    setRole(newRole);
  };

  const logout = () => {
    localStorage.clear();
    setToken(null);
    setRole("User");
  };

  useEffect(() => {
    setToken(localStorage.getItem("token"));
    setRole(localStorage.getItem("role") || "User");
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem("token");
    const storedRole = localStorage.getItem("role");

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
    <AuthContext.Provider value={{ token, role, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}