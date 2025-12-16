import axios, { AxiosError, AxiosResponse } from "axios";
import { setGlobalLoading } from "../context/LoadingContext";
import { GlobalResponse } from "../constants/types";

// const API_BASE = process.env.REACT_APP_API_URL || "http://localhost:8080"; // use for real environments
const API_BASE = (import.meta as any).env?.VITE_API_URL || "http://localhost:8080"; // use for Vite

let authToken: string | null = localStorage.getItem("token"); // initialize from storage
export const setAuthToken = (token: string | null) => { authToken = token; };

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
    return Promise.reject(error); // let caller handle error
  }
);

export default apiClient;