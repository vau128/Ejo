import axios from 'axios';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

const client = axios.create({
  baseURL: apiBaseUrl ? `${apiBaseUrl}/api` : undefined,
  timeout: 8000,
});

client.interceptors.request.use((config) => {
  const token = window.localStorage.getItem('admin_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export default client;
