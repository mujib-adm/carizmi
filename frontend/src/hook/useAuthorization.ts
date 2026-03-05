import { RoleConstants } from '../constants/RoleConstants';
import { useAuth } from '../context/AuthContext';

export function useAuthorization() {
  const { role } = useAuth();

  // ADMIN and MANAGER can add/edit/delete/update records.
  // MEMBER can only view.
  const canWrite = role === RoleConstants.ROLE_ADMIN || role === RoleConstants.ROLE_MANAGER;

  // Only ADMIN can manage users.
  const canManageUsers = role === RoleConstants.ROLE_ADMIN;

  return {
    role,
    canWrite,
    canManageUsers,
  };
}