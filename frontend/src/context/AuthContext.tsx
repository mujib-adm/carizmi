import { createContext, useContext, useEffect, useState } from 'react';
import { onUnauthorized } from '../api/client/ApiClient';
import { authenticationApi } from '../api/generated/authentication/authentication';

type AuthContextType = {
  isAuthenticated: boolean;
  role: string;
  firstName: string;
  isLoading: boolean;
  login: (role: string, firstName: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  role: 'User',
  firstName: '',
  isLoading: true,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('role'));
  const [role, setRole] = useState(localStorage.getItem('role') || 'User');
  const [firstName, setFirstName] = useState(localStorage.getItem('firstName') || '');

  const login = (newRole: string, newFirstName: string) => {
    // Tokens are in httpOnly cookies — only store non-sensitive metadata
    localStorage.setItem('role', newRole);
    localStorage.setItem('firstName', newFirstName);
    setIsAuthenticated(true);
    setRole(newRole);
    setFirstName(newFirstName);
  };

  const logout = () => {
    // Call logout API to clear server-side cookies
    authenticationApi.logout().catch(() => {});
    localStorage.removeItem('role');
    localStorage.removeItem('firstName');
    setIsAuthenticated(false);
    setRole('User');
    setFirstName('');
  };

  // Register unauthorized callback to trigger logout on 401
  useEffect(() => {
    onUnauthorized(() => {
      localStorage.removeItem('role');
      localStorage.removeItem('firstName');
      setIsAuthenticated(false);
      setRole('User');
      setFirstName('');
    });
  }, []);

  // Validate session on mount by calling the profile endpoint
  // Cookies are sent automatically via withCredentials
  useEffect(() => {
    const storedRole = localStorage.getItem('role');
    const storedFirstName = localStorage.getItem('firstName');

    if (storedRole) {
      // Validate session with backend (cookie is sent automatically)
      authenticationApi.getCurrentUser()
        .then((res) => {
          const data = res;
          setIsAuthenticated(true);
          setRole(storedRole);
          if (data.responseData) {
            const fName = data.responseData.firstName || storedFirstName || '';
            setFirstName(fName);
            localStorage.setItem('firstName', fName);
          }
        })
        .catch(() => {
          localStorage.removeItem('role');
          localStorage.removeItem('firstName');
          setIsAuthenticated(false);
          setRole('User');
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, role, firstName, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}