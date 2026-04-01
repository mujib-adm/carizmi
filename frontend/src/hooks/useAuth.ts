import { createContext, useContext } from 'react';

export type AuthContextType = {
  isAuthenticated: boolean;
  role: string;
  firstName: string;
  isLoading: boolean;
  login: (role: string, firstName: string) => void;
  logout: () => void;
};

export const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  role: 'User',
  firstName: '',
  isLoading: true,
  login: () => {},
  logout: () => {},
});

export function useAuth() {
  return useContext(AuthContext);
}