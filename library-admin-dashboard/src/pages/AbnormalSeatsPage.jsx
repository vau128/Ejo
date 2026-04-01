import { getAbnormalSeats } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import DoughnutChart from '../components/DoughnutChart';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function AbnormalSeatsPage() {
  const { data, loading, error } = useApiData(getAbnormalSeats, []);

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">비정상 좌석 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="비정상 좌석" description="문제 좌석 수와 유형 분포를 별도로 확인합니다." />

      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard label="비정상 좌석 수" value={data.summary.abnormalCount} helper="현재 기준" accent="rose" />
        <MetricCard label="물품 감지" value={data.summary.objectOnly} helper="지속 방치" accent="amber" />
        <MetricCard label="장시간 비움" value={data.summary.vacantLong} helper="시간 초과" accent="brand" />
        <MetricCard label="센서 지연" value={data.summary.sensorDelay} helper="통신 또는 수집 지연" accent="violet" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <SectionCard title="비정상 분포" subtitle="유형별 건수를 시각적으로 확인합니다.">
          <DoughnutChart segments={data.breakdown} />
        </SectionCard>

        <SectionCard title="비정상 좌석 목록" subtitle="현재 조치가 필요한 좌석 리스트입니다.">
          <DataTable
            columns={[
              { key: 'seatId', label: '좌석' },
              { key: 'statusLabel', label: '상태', render: (row) => <StatusBadge>{row.statusLabel}</StatusBadge> },
              { key: 'detectedAt', label: '감지 시각' },
              { key: 'durationMinutes', label: '지속 시간' },
              { key: 'severity', label: '중요도', render: (row) => <StatusBadge>{row.severity}</StatusBadge> },
              { key: 'actionStatus', label: '처리 상태' },
            ]}
            rows={data.rows}
          />
        </SectionCard>
      </div>
    </div>
  );
}
