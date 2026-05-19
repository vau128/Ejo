import client from './client';

export async function loginAdmin(payload) {
  const requestBody = {
    username: payload.username ?? payload.email ?? '',
    password: payload.password ?? '',
  };

  const { data } = await client.post('/auth/login', requestBody);
  return data;
}
