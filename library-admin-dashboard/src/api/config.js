const rawApiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').trim().replace(/\/+$/, '');

export const apiBaseUrl = rawApiBaseUrl
  ? rawApiBaseUrl.endsWith('/api')
    ? rawApiBaseUrl
    : `${rawApiBaseUrl}/api`
  : '/api';
