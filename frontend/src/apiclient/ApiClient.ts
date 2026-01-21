import axios, { AxiosError, AxiosResponse } from "axios";
import { ApiEndpoints } from "../constants/endpoints";
import { GlobalResponse } from "../constants/types";
import { setGlobalLoading } from "../context/LoadingContext";

// const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080"; // use for real environments
// For Docker builds, this is injected at build-time via Docker ARGs
// For local development (npm run dev), Vite reads it from .env
const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

let authToken: string | null = localStorage.getItem("token"); // initialize from storage
export const setAuthToken = (token: string | null) => { authToken = token; };

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

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json", Accept: "application/json", Authorization: authToken ? `Bearer ${authToken}` : "" },
  timeout: 15000
});

// Request interceptor → show loader
apiClient.interceptors.request.use((config) => {
  setGlobalLoading(true);
  if (authToken) {
    // Axios v1+ headers is AxiosHeaders, so use set()
    config.headers?.set("Authorization", `Bearer ${authToken}`);
  }
  return config;
},
  (error) => {
    setGlobalLoading(false);
    return Promise.reject(error);
  }
);

// Response interceptor → hide loader
apiClient.interceptors.response.use(
  (response: AxiosResponse<GlobalResponse>) => {
    setGlobalLoading(false);
    return response; 
  },
  async (error: AxiosError<GlobalResponse>) => {
    setGlobalLoading(false);
    const originalRequest = error.config;
    
    // Check if error is 401
    if (error.response?.status === 401 && originalRequest) {
        // console.warn("401 Detected. URL:", originalRequest.url, "Retry:", (originalRequest as any)._retry);
        
        // If already refreshing, queue the request
        if (isRefreshing) {
            return new Promise(function(resolve, reject) {
                failedQueue.push({ resolve, reject });
            })
            .then(token => {
                if (originalRequest.headers) {
                    originalRequest.headers.Authorization = `Bearer ${token}`;
                }
                return apiClient(originalRequest);
            })
            .catch(err => {
                // console.error("Queue rejected:", err);
                return Promise.reject(err);
            });
        }

        // If not refreshing and haven't retired yet
        if (!(originalRequest as any)._retry) {
            (originalRequest as any)._retry = true;
            isRefreshing = true;
            
            const refreshToken = localStorage.getItem("refreshToken");

            if (refreshToken) {
                try {
                    // Call refresh endpoint
                    const response = await axios.post(`${API_BASE}${ApiEndpoints.AUTH.REFRESH}`, { refreshToken });

                    const { token, refreshToken: newRefreshToken } = response.data.map;

                    if (!token || !newRefreshToken) {
                        throw new Error("Missing token or refreshToken in response map");
                    }

                    // Update local storage and memory
                    localStorage.setItem("token", token);
                    localStorage.setItem("refreshToken", newRefreshToken);
                    setAuthToken(token);

                    // Update header for original request
                    if (originalRequest.headers) {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                    }
                    
                    processQueue(null, token);
                    return apiClient(originalRequest);
                } catch (refreshError) {
                    processQueue(refreshError, null);
                    // Refresh failed (expired or invalid) -> Logout
                    // console.error("Refresh FAILED:", refreshError);
                } finally {
                    isRefreshing = false;
                }
            } else {
                console.warn("No Refresh Token found in localStorage.");
            }
        }
    }
    
    // Global 401 handler: clear storage and redirect to login if unauthorized (and refresh failed or no token)
    if (error.response?.status === 401) {
        // console.warn("Global 401 Handler triggered. Logging out.");
      if (unauthorizedCallback) {
        unauthorizedCallback();
      } else {
        // Fallback to reload if no callback registered
        localStorage.clear();
        setAuthToken(null);
        window.location.href = "/login";
      }
    }
    
    return Promise.reject(error); // let caller handle error
  }
);

export default apiClient;