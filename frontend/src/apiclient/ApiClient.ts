/**
 * ApiClient.ts — Centralised Axios HTTP Client
 *
 * This module creates and exports a single, pre-configured Axios instance
 * (`apiClient`) used by all API calls in the application. It is consumed in
 * two ways:
 *
 *   1. **Orval-generated clients** — via the custom `apiMutator.ts`, which
 *      delegates every Orval request through this instance.
 *   2. **Manual API calls** — any code that imports `apiClient` directly.
 *
 * Key responsibilities:
 *   - Base URL resolution (runtime config → env variable → localhost fallback)
 *   - Global loading indicator management (request/response interceptors)
 *   - Automatic JWT refresh via httpOnly cookies on 401 responses
 *   - Request queuing during token refresh to prevent duplicate refresh calls
 *   - Global unauthorized (401) handling with pluggable callback
 *
 * Authentication model:
 *   The app uses **httpOnly cookie-based** authentication. Tokens are never
 *   stored in JavaScript — `withCredentials: true` ensures cookies are sent
 *   with every request automatically. On 401, the client attempts a silent
 *   refresh by calling the refresh endpoint (also cookie-based).
 */

import axios, { AxiosError, AxiosResponse } from 'axios';
import { AUTH_LOGIN, AUTH_REFRESH } from '../api/constants/customEndpoints';
import { GlobalResponse } from '../api/generated/types/index';
import { setGlobalLoading } from '../context/LoadingContext';

// ─────────────────────────────────────────────────────────────────────────────
// Base URL Resolution
// ─────────────────────────────────────────────────────────────────────────────

/**
 * API base URL, resolved in priority order:
 *   1. Runtime config injected via `window.APP_CONFIG.API_URL` (for containerised deploys)
 *   2. Build-time Vite env variable `VITE_API_URL`
 *   3. Local development fallback: `http://localhost:8080/api`
 */
const API_BASE = window.APP_CONFIG?.API_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// ─────────────────────────────────────────────────────────────────────────────
// Unauthorized (401) Callback
// ─────────────────────────────────────────────────────────────────────────────

type UnauthorizedCallback = () => void;
let unauthorizedCallback: UnauthorizedCallback | null = null;

/**
 * Registers a callback to be invoked when a 401 Unauthorized response
 * is received and token refresh has failed (or was not applicable).
 *
 * Typically called once from `AuthContext` to wire up the React-level
 * logout flow (clear state, redirect to `/login`).
 *
 * @param callback - Function to execute on unrecoverable 401.
 */
export const onUnauthorized = (callback: UnauthorizedCallback) => {
  unauthorizedCallback = callback;
};

// ─────────────────────────────────────────────────────────────────────────────
// Token Refresh Queue
// ─────────────────────────────────────────────────────────────────────────────

/**
 * When a 401 triggers a token refresh, any additional requests that arrive
 * while the refresh is in-flight are queued here. Once the refresh succeeds
 * (or fails), all queued promises are resolved (retried) or rejected.
 *
 * This prevents a "refresh storm" where multiple concurrent requests each
 * independently attempt to refresh the token.
 */
let isRefreshing = false;
let failedQueue: any[] = [];

/**
 * Resolves or rejects all queued requests after a refresh attempt completes.
 *
 * @param error - `null` if refresh succeeded; the error object if it failed.
 */
const processQueue = (error: any) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });

  failedQueue = [];
};

// ─────────────────────────────────────────────────────────────────────────────
// Axios Instance
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The singleton Axios instance used across the entire application.
 *
 * Configuration:
 *   - `baseURL`         — Resolved API base (see above).
 *   - `Content-Type`    — JSON by default; individual requests can override.
 *   - `timeout`         — 15 seconds to prevent hanging requests.
 *   - `withCredentials` — Sends httpOnly cookies with every request (JWT auth).
 */
const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  timeout: 15000,
  withCredentials: true,
});

// ─────────────────────────────────────────────────────────────────────────────
// Request Interceptor
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shows the global loading indicator before every outgoing request.
 * If the request setup itself fails, the loader is hidden immediately.
 */
apiClient.interceptors.request.use(
  (config) => {
    setGlobalLoading(true);
    return config;
  },
  (error) => {
    setGlobalLoading(false);
    return Promise.reject(error);
  }
);

// ─────────────────────────────────────────────────────────────────────────────
// Response Interceptor — Loading State + 401 Token Refresh
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Response interceptor with two responsibilities:
 *
 *   1. **Loading state** — Hides the global loader on every response/error.
 *
 *   2. **Automatic 401 recovery** — When a 401 is received:
 *      a. If a refresh is already in progress → queue this request.
 *      b. Otherwise → call the refresh endpoint (cookie-based, no token in JS).
 *      c. On success → retry all queued requests (including the original).
 *      d. On failure → invoke the `unauthorizedCallback` to trigger logout.
 *
 *   Login endpoint (`/auth/login`) is excluded from refresh logic so that
 *   invalid-credentials errors are returned directly to the login form.
 */
apiClient.interceptors.response.use(
  (response: AxiosResponse<GlobalResponse>) => {
    setGlobalLoading(false);
    return response;
  },
  async (error: AxiosError<GlobalResponse>) => {
    setGlobalLoading(false);
    const originalRequest = error.config;

    // ── 401 + not a login request → attempt token refresh ───────────────
    if (error.response?.status === 401 && originalRequest && !originalRequest.url?.includes(AUTH_LOGIN)) {

      // If a refresh is already in-flight, queue this request to retry later
      if (isRefreshing) {
        return new Promise(function (resolve, reject) {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            // Cookie was refreshed automatically, just retry original request
            return apiClient(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      // First 401 encountered — initiate the refresh flow
      if (!(originalRequest as any)._retry) {
        (originalRequest as any)._retry = true;
        isRefreshing = true;

        try {
          // Call refresh endpoint — cookies are sent automatically via withCredentials
          await axios.post(
            `${API_BASE}${AUTH_REFRESH}`,
            {},
            { withCredentials: true }
          );

          // Refresh succeeded — retry all queued requests
          processQueue(null);
          return apiClient(originalRequest);
        } catch (refreshError) {
          // Refresh failed — reject all queued requests
          processQueue(refreshError);
        } finally {
          isRefreshing = false;
        }
      }
    }

    // ── Unrecoverable 401 → trigger logout ──────────────────────────────
    // Reached when: refresh failed, or request was already retried.
    // Login endpoint is excluded — let the caller handle invalid credentials.
    if (error.response?.status === 401 && !originalRequest?.url?.includes(AUTH_LOGIN)) {
      if (unauthorizedCallback) {
        unauthorizedCallback();
      } else {
        // Fallback: clear non-sensitive storage and redirect
        localStorage.removeItem('role');
        localStorage.removeItem('firstName');
        window.location.href = '/login';
      }
    }

    return Promise.reject(error); // Let the caller handle all other errors
  }
);

export default apiClient;