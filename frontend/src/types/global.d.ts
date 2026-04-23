/** Ambient type declarations for window globals injected at runtime. */
declare global {
  interface Window {
    APP_CONFIG?: {
      API_URL?: string;
    };
  }
}

export {};