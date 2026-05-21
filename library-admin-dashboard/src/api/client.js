import axios from 'axios';
import { apiBaseUrl } from './config';

const client = axios.create({
  baseURL: apiBaseUrl || undefined,
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
