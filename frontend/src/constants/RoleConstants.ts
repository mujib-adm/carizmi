export const RoleConstants = {
  ROLE_ADMIN: 'ADMIN',
  ROLE_MANAGER: 'MANAGER',
  ROLE_MEMBER: 'MEMBER',
  ROLE_ANONYMOUS: 'ANONYMOUS',
} as const;

export type Role = (typeof RoleConstants)[keyof typeof RoleConstants];