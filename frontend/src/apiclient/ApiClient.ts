import axios, { AxiosError, AxiosResponse } from "axios";
import { GlobalResponse } from "../constants/types";
import { setGlobalLoading } from "../context/LoadingContext";

// const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080"; // use for real environments
const API_BASE = (import.meta as any).env?.VITE_API_URL || "http://localhost:8080"; // use for Vite

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

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json", Accept: "application/json", Authorization: authToken ? `Bearer ${authToken}` : "" },
  // timeout: 10000,
});

// Request interceptor → show loader
apiClient.interceptors.request.use((config) => {
  setGlobalLoading(true);
  if (authToken) {
    // Axios v1+ headers is AxiosHeaders, so use set()
    config.headers?.set("Authorization", `Bearer ${authToken}`);
  }
  console.log("Request: ", config.method?.toUpperCase(), config.url); // TODO: remove in production
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
    console.log("Response: ", response); // TODO: remove in production
    return response; // keep full response so you can inspect status + body
  },
  (error: AxiosError<GlobalResponse>) => {
    setGlobalLoading(false);
    console.log("Response - Error: ", error); // TODO: remove in production
    
    // Global 401 handler: clear storage and redirect to login if unauthorized
    if (error.response?.status === 401) {
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