import { describe, it, expect } from 'vitest';

// Simple smoke test to ensure the App module can be imported without crashing.
describe('App Smoke Test', () => {
  it('app module can be imported', async () => {
    const appModule = await import('../App.tsx');
    expect(appModule).toBeDefined();
    expect(appModule.default).toBeDefined();
  });
});