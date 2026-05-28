import client from './client';

export async function getOverview() {
  const { data } = await client.get('/dashboard/overview');
  return data;
}

export async function getActions() {
  const { data } = await client.get('/dashboard/actions');
  return data;
}

export async function sendWarning(seatId) {
  const { data } = await client.post('/dashboard/actions/warning', { seatId });
  return data;
}

export async function releaseSeat(seatId) {
  const { data } = await client.post('/dashboard/actions/release', { seatId });
  return data;
}

export async function forceCheckout(seatId) {
  const { data } = await client.post('/dashboard/actions/force-checkout', { seatId });
  return data;
}

export async function resolveIssue(seatId) {
  const { data } = await client.post('/dashboard/actions/resolve', { seatId });
  return data;
}

export async function getAlertHistory() {
  const { data } = await client.get('/dashboard/alerts/history');
  return data;
}

export async function getAlertManagement() {
  const { data } = await client.get('/dashboard/alerts/management');
  return data;
}

export async function updateAlertRule(ruleId, enabled) {
  const { data } = await client.patch(`/dashboard/alerts/management/${ruleId}`, { enabled });
  return data;
}

export async function getStatistics() {
  const { data } = await client.get('/dashboard/stats');
  return data;
}

export async function getHealthcareStatistics() {
  const { data } = await client.get('/dashboard/healthcare/stats');
  return data;
}

export async function getSettings() {
  const { data } = await client.get('/dashboard/settings');
  return data;
}

export async function updateSettings(settings) {
  const { data } = await client.patch('/dashboard/settings', settings);
  return data;
}

export async function getZoneSeats(params = {}) {
  const { data } = await client.get('/dashboard/seats/zone-3', { params });
  return data;
}

export async function getSeatDetail(seatId) {
  const { data } = await client.get(`/dashboard/seats/zone-3/${seatId}`);
  return data;
}

export async function getAbnormalSeats() {
  const { data } = await client.get('/dashboard/seats/abnormal');
  return data;
}

export async function getLostItems() {
  const { data } = await client.get('/dashboard/lost-items');
  return data;
}

export async function updateLostItemStatus(itemId, status) {
  const { data } = await client.patch(`/dashboard/lost-items/${itemId}`, { status });
  return data;
}

export async function getSystemStatus() {
  const { data } = await client.get('/dashboard/system-status');
  return data;
}

export async function getSensorLogs() {
  const { data } = await client.get('/dashboard/system-status/sensor-logs');
  return data;
}

export async function getSeats() {
  const { data } = await client.get('/seats');
  return data;
}

export async function getSeatWarnings() {
  const { data } = await client.get('/warnings');
  return data;
}

export async function getLostItemsFeed() {
  const { data } = await client.get('/lost-items');
  return data;
}

export async function getSquattingThreshold() {
  const { data } = await client.get('/settings/squatting-threshold');
  return data;
}

export async function updateSquattingThreshold(thresholdMinutes) {
  const requestBody = {
    thresholdMinutes,
  };
  console.log('updateSquattingThreshold requestBody', requestBody);
  const { data } = await client.put('/settings/squatting-threshold', requestBody);
  return data;
}

export async function triggerLostItemScan() {
  const requestBody = { command: 'detect' };
  console.log('triggerLostItemScan requestBody', requestBody);
  const { data } = await client.post('/admin/lost-item-scan', requestBody);
  return data;
}

export async function resetAllSeats() {
  const { data } = await client.post('/testing/reset-demo-state');
  return data;
}
