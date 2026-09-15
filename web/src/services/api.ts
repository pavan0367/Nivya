import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

// Resolve API base URL: defaults to local relative '/api/v1' in development,
// or uses VITE_API_BASE_URL (e.g. 'https://nivya-blbf.onrender.com/api/v1') in production.
const rawApiBase = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim();
export const API_BASE = rawApiBase ? rawApiBase.replace(/\/+$/, '') : '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Attach JWT access token to every outgoing request
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('nivya_access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

// Response interceptor: handle 401 token expiration or 403 authorization failures
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Clear token & trigger redirect to login
      localStorage.removeItem('nivya_access_token');
      localStorage.removeItem('nivya_user_role');
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login?expired=true';
      }
    } else if (error.response?.status === 403) {
      console.warn('Access denied: Unauthorized role or restricted parent feature.');
    }
    return Promise.reject(error);
  }
);
