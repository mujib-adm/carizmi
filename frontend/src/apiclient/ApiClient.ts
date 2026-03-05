import axios, { AxiosError, AxiosResponse } from 'axios';
import { ApiEndpoints } from '../constants/endpoints';
import { GlobalResponse } from '../constants/types';
import { setGlobalLoading } from '../context/LoadingContext';

// Prefer runtime configuration injected via window.APP_CONFIG
// Fallback to build-time environment variable or local default (localhost:8080)
const API_BASE = window.APP_CONFIG?.API_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

type UnauthorizedCallback = () => void;
let unauthorizedCallback: UnauthorizedCallback | null = null;

/**
 * Register a callback to be invoked when a 401 Unauthorized error is received.
 */
export const onUnauthorized = (callback: UnauthorizedCallback) => {
  unauthorizedCallback = callback;
};

let isRefreshing = false;
let failedQueue: any[] = [];

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

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  timeout: 15000,
  withCredentials: true, // ← Send httpOnly cookies with every request
});

// Request interceptor → show loader
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

// Response interceptor → hide loader + handle 401 with cookie-based refresh
apiClient.interceptors.response.use(
  (response: AxiosResponse<GlobalResponse>) => {
    setGlobalLoading(false);
    return response;
  },
  async (error: AxiosError<GlobalResponse>) => {
    setGlobalLoading(false);
    const originalRequest = error.config;

    // Check if error is 401
    // Skip refresh logic for login requests
    if (error.response?.status === 401 && originalRequest && !originalRequest.url?.includes(ApiEndpoints.AUTH.LOGIN)) {
      // If already refreshing, queue the request
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

      // If not refreshing and haven't retried yet
      if (!(originalRequest as any)._retry) {
        (originalRequest as any)._retry = true;
        isRefreshing = true;

        try {
          // Call refresh endpoint — cookies are sent automatically via withCredentials
          await axios.post(
            `${API_BASE}${ApiEndpoints.AUTH.REFRESH}`,
            {},
            { withCredentials: true }
          );

          processQueue(null);
          return apiClient(originalRequest);
        } catch (refreshError) {
          processQueue(refreshError);
          // Refresh failed (expired or invalid) -> Logout
        } finally {
          isRefreshing = false;
        }
      }
    }

    // Global 401 handler: redirect to login if unauthorized and/or refresh failed
    // EXCLUDE login endpoint (let the caller handle 401 for invalid credentials)
    if (error.response?.status === 401 && !originalRequest?.url?.includes(ApiEndpoints.AUTH.LOGIN)) {
      if (unauthorizedCallback) {
        unauthorizedCallback();
      } else {
        // Fallback: clear non-sensitive storage and redirect
        localStorage.removeItem('role');
        localStorage.removeItem('firstName');
        window.location.href = '/login';
      }
    }

    return Promise.reject(error); // let caller handle error
  }
);

export default apiClient;