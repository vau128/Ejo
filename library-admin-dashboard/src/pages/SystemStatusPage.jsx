import { useEffect, useState } from 'react';
import { getSensorLogs, getSystemStatus } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';

export default function SystemStatusPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [system, logs] = await Promise.all([getSystemStatus(), getSensorLogs()]);
        if (cancelled) return;
        setData({ ...system, sensorLogs: logs.logs });
        setError('');
      } catch (err) {
        if (!cancelled) setError(err.response?.data?.message || '시스템 상태를 불러오지 못했습니다.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    const intervalId = window.setInterval(load, 8000);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, []);

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">시스템 상태를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="시스템 상태 모니터링" description="센서 연결 상태, 카메라 상태, 데이터 지연 여부와 센서 로그를 확인합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="센서 연결 상태" value={data.summary.sensorConnected} helper="게이트웨이 기준" accent="emerald" />
        <MetricCard label="카메라 상태" value={data.summary.cameraOnline} helper="온라인 카메라" accent="brand" />
        <MetricCard label="데이터 수집 지연" value={data.summary.dataDelay} helper="최근 10분" accent="rose" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <SectionCard title="장치 상태" subtitle="센서, 카메라, 게이트웨이 장치 상태를 확인합니다.">
          <DataTable
            columns={[
              { key: 'deviceId', label: '장치 ID' },
              { key: 'type', label: '장치 유형' },
              { key: 'zone', label: '구역' },
              { key: 'status', label: '상태', render: (row) => <StatusBadge>{row.status}</StatusBadge> },
              { key: 'lastSeen', label: '최근 수신' },
              { key: 'latencyMs', label: '지연(ms)' },
              { key: 'notes', label: '비고' },
            ]}
            rows={data.devices}
          />
        </SectionCard>

        <SectionCard title="센서 데이터 시간순 조회" subtitle="최근 수집된 센서 이벤트 로그입니다.">
          <div className="grid gap-3">
            {data.sensorLogs.map((log) => (
              <div key={log.id} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-800">{log.eventType}</p>
                    <p className="mt-1 text-sm text-slate-500">{log.timestamp} · {log.deviceId} · {log.seatId}</p>
                  </div>
                  <StatusBadge>{log.status}</StatusBadge>
                </div>
                <p className="mt-3 text-sm text-slate-600">{log.message}</p>
                <p className="mt-1 text-xs text-slate-400">value: {log.value}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
