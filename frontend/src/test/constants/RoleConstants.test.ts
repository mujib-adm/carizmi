import { describe, it, expect } from 'vitest';
import { RoleConstants, Role } from '../../constants/RoleConstants.ts';

describe('RoleConstants', () => {
  it('should define ROLE_ADMIN as "ADMIN"', () => {
    expect(RoleConstants.ROLE_ADMIN).toBe('ADMIN');
  });

  it('should define ROLE_MANAGER as "MANAGER"', () => {
    expect(RoleConstants.ROLE_MANAGER).toBe('MANAGER');
  });

  it('should define ROLE_MEMBER as "MEMBER"', () => {
    expect(RoleConstants.ROLE_MEMBER).toBe('MEMBER');
  });

  it('should define ROLE_ANONYMOUS as "ANONYMOUS"', () => {
    expect(RoleConstants.ROLE_ANONYMOUS).toBe('ANONYMOUS');
  });

  it('should have exactly 4 roles', () => {
    expect(Object.keys(RoleConstants)).toHaveLength(4);
  });

  it('should allow Role type assignment from constants', () => {
    const role: Role = RoleConstants.ROLE_ADMIN;
    expect(role).toBe('ADMIN');
  });
});
