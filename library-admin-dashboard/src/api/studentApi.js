import client from './client';

export async function loginStudent(payload) {
  const { data } = await client.post('/app/auth/login', payload);
  return data;
}

export async function fetchCurrentStudent() {
  const { data } = await client.get('/app/me');
  return data;
}

export async function fetchSeats() {
  const { data } = await client.get('/app/seats');
  return data;
}

export async function toggleSeatSelection(seatId) {
  const { data } = await client.post(`/app/seats/${seatId}/selection`);
  return data;
}
