import client from './client';

export async function testBackendConnection() {
  const { data } = await client.get('/test');
  return data;
}
