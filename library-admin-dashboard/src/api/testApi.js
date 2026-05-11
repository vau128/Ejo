import axios from 'axios';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

export async function testBackendConnection() {
  const { data } = await axios.get(`${apiBaseUrl}/api/test`, {
    timeout: 8000,
  });

  return data;
}
