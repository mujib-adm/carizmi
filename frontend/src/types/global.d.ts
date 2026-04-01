/**
 * Global Type Declarations
 *
 * This file contains ambient type declarations for the global 'window' object.
 * It is used to define properties that are injected into the global scope
 * (e.g., via external scripts or runtime configuration) which are not part
 * of the standard DOM library.
 *
 * Add any other global external variables or configuration schemas here
 * to ensure type safety throughout the TypeScript application.
 */
declare global {
  interface Window {
    APP_CONFIG?: {
      API_URL?: string;
    };
  }
}

export {};