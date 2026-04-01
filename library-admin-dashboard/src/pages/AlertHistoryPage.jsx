import { getAlertHistory } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function AlertHistoryPage() {
  const { data, loading, error } = useApiData(getAlertHistory, []);

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">알림 이력을 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="알림 전송 이력" description="경고 및 안내 메시지 전송 이력을 확인합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="총 발송 건수" value={data.summary.warningsSent} helper="금일 누적" accent="brand" />
        <MetricCard label="처리 완료" value={data.summary.resolvedAlerts} helper="이력 기준" accent="emerald" />
        <MetricCard label="전송 성공률" value={data.summary.deliverySuccessRate} helper="최근 24시간" accent="violet" />
      </div>

      <SectionCard title="전송 이력 목록" subtitle="좌석 단위로 전송 내역을 조회할 수 있습니다." className="mt-6">
        <DataTable
          columns={[
            { key: 'id', label: '이력 ID' },
            { key: 'seatId', label: '좌석' },
            { key: 'studentIdMasked', label: '학생 ID' },
            { key: 'messageType', label: '유형' },
            { key: 'channel', label: '채널' },
            { key: 'createdAt', label: '전송 시각' },
            { key: 'status', label: '상태', render: (row) => <StatusBadge>{row.status}</StatusBadge> },
          ]}
          rows={data.history}
        />
      </SectionCard>
    </div>
  );
}
