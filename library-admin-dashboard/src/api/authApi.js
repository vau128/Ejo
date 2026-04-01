import client from './client';

export async function loginAdmin(payload) {
  const { data } = await client.post('/auth/login', payload);
  return data;
}
