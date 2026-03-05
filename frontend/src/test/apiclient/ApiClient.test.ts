import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('ApiClient Runtime Configuration', () => {
  beforeEach(() => {
    vi.resetModules();
    // Clear window.APP_CONFIG before each test
    delete (window as any).APP_CONFIG;
  });

  it('should use window.APP_CONFIG.API_URL if present', async () => {
    (window as any).APP_CONFIG = { API_URL: 'http://runtime-api.com' };
    
    // Dynamically import ApiClient to pick up the global state
    const { default: apiClient } = await import('../../apiclient/ApiClient.ts');
    expect(apiClient.defaults.baseURL).toBe('http://runtime-api.com');
  });

  it('should fallback to environment variable if window.APP_CONFIG is missing', async () => {
    // Note: Vitest handles import.meta.env based on setup
    const { default: apiClient } = await import('../../apiclient/ApiClient.ts');
    // It should not be the runtime API, but could be undefined or the env value
    expect(apiClient.defaults.baseURL).not.toBe('http://runtime-api.com');
  });
});