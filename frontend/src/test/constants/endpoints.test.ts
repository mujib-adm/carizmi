import { describe, it, expect } from 'vitest';
import { ApiEndpoints } from '../../constants/endpoints.ts';

describe('ApiEndpoints', () => {
  describe('MEMBERS', () => {
    it('should have correct static endpoints', () => {
      expect(ApiEndpoints.MEMBERS.ADD).toBe('/members/add');
      expect(ApiEndpoints.MEMBERS.UPDATE).toBe('/members/update');
      expect(ApiEndpoints.MEMBERS.SEARCH).toBe('/members/search');
      expect(ApiEndpoints.MEMBERS.LOOKUP).toBe('/members/lookup');
    });

    it('should generate correct GET URL with memberID', () => {
      expect(ApiEndpoints.MEMBERS.GET(1)).toBe('/members/get/1');
      expect(ApiEndpoints.MEMBERS.GET(42)).toBe('/members/get/42');
    });

    it('should generate correct DELETE URL with memberID', () => {
      expect(ApiEndpoints.MEMBERS.DELETE(5)).toBe('/members/delete/5');
    });

    it('should generate correct SUMMARY URL with memberID', () => {
      expect(ApiEndpoints.MEMBERS.SUMMARY(10)).toBe('/members/10/summary');
    });
  });

  describe('PAYMENTS', () => {
    it('should have correct static endpoints', () => {
      expect(ApiEndpoints.PAYMENTS.ADD).toBe('/payments/add');
      expect(ApiEndpoints.PAYMENTS.UPDATE).toBe('/payments/update');
      expect(ApiEndpoints.PAYMENTS.SEARCH).toBe('/payments/search');
      expect(ApiEndpoints.PAYMENTS.LATEST).toBe('/payments/latest');
    });

    it('should generate correct GET URL with paymentID', () => {
      expect(ApiEndpoints.PAYMENTS.GET(3)).toBe('/payments/get/3');
    });

    it('should generate correct DELETE URL with paymentID', () => {
      expect(ApiEndpoints.PAYMENTS.DELETE(7)).toBe('/payments/delete/7');
    });
  });

  describe('EXPENSES', () => {
    it('should have correct static endpoints', () => {
      expect(ApiEndpoints.EXPENSES.ADD).toBe('/expenses/add');
      expect(ApiEndpoints.EXPENSES.UPDATE).toBe('/expenses/update');
      expect(ApiEndpoints.EXPENSES.SEARCH).toBe('/expenses/search');
    });

    it('should generate correct GET URL with expenseID', () => {
      expect(ApiEndpoints.EXPENSES.GET(2)).toBe('/expenses/get/2');
    });

    it('should generate correct DELETE URL with expenseID', () => {
      expect(ApiEndpoints.EXPENSES.DELETE(9)).toBe('/expenses/delete/9');
    });
  });

  describe('DASHBOARD', () => {
    it('should have correct metrics endpoint', () => {
      expect(ApiEndpoints.DASHBOARD.METRICS).toBe('/dashboard/metrics');
    });
  });

  describe('SETTINGS', () => {
    it('should have correct static endpoints', () => {
      expect(ApiEndpoints.SETTINGS.ADD).toBe('/system-settings/add');
      expect(ApiEndpoints.SETTINGS.UPDATE).toBe('/system-settings/update');
      expect(ApiEndpoints.SETTINGS.SEARCH).toBe('/system-settings/search');
    });

    it('should generate correct GET URL with settingID', () => {
      expect(ApiEndpoints.SETTINGS.GET(4)).toBe('/system-settings/get/4');
    });

    it('should generate correct GETBYKEY URL with key', () => {
      expect(ApiEndpoints.SETTINGS.GETBYKEY('appName')).toBe('/system-settings/by-key/appName');
    });

    it('should generate correct DELETE URL with settingID', () => {
      expect(ApiEndpoints.SETTINGS.DELETE(6)).toBe('/system-settings/delete/6');
    });
  });

  describe('REFERENCE', () => {
    it('should have correct SEARCH endpoint', () => {
      expect(ApiEndpoints.REFERENCE.SEARCH).toBe('/reference/search');
    });

    it('should generate correct GET_BY_NAME URL', () => {
      expect(ApiEndpoints.REFERENCE.GET_BY_NAME('feeType')).toBe('/reference/list/feeType');
    });
  });

  describe('AUTH', () => {
    it('should have correct static endpoints', () => {
      expect(ApiEndpoints.AUTH.LOGIN).toBe('/auth/login');
      expect(ApiEndpoints.AUTH.REGISTER).toBe('/auth/register');
      expect(ApiEndpoints.AUTH.REFRESH).toBe('/auth/refresh');
      expect(ApiEndpoints.AUTH.LOGOUT).toBe('/auth/logout');
      expect(ApiEndpoints.AUTH.PROFILE).toBe('/auth/profile');
      expect(ApiEndpoints.AUTH.PASSWORD_UPDATE).toBe('/auth/password-update');
    });
  });

  describe('USERS', () => {
    it('should have correct LIST endpoint', () => {
      expect(ApiEndpoints.USERS.LIST).toBe('/users');
    });

    it('should generate correct UPDATE_ROLE URL with userId', () => {
      expect(ApiEndpoints.USERS.UPDATE_ROLE(1)).toBe('/users/1/role');
    });

    it('should generate correct UPDATE_STATUS URL with userId', () => {
      expect(ApiEndpoints.USERS.UPDATE_STATUS(2)).toBe('/users/2/status');
    });
  });
});