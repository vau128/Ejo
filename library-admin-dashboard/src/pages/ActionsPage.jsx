import { useMemo, useState } from 'react';
import { forceCheckout, getActions, releaseSeat, resolveIssue, sendWarning } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function ActionsPage() {
  const { data, loading, error, refetch } = useApiData(getActions, []);
  const [submittingSeatId, setSubmittingSeatId] = useState('');

  const handleAction = async (type, seatId) => {
    try {
      setSubmittingSeatId(seatId + type);
      if (type === 'warning') await sendWarning(seatId);
      if (type === 'release') await releaseSeat(seatId);
      if (type === 'force-checkout') await forceCheckout(seatId);
      if (type === 'resolve') await resolveIssue(seatId);
      await refetch();
    } finally {
      setSubmittingSeatId('');
    }
  };

  const columns = useMemo(
    () => [
      { key: 'seatId', label: '좌석' },
      { key: 'issueType', label: '문제 유형' },
      { key: 'detectedAt', label: '감지 시각' },
      { key: 'duration', label: '지속 시간' },
      { key: 'sensorHint', label: '센서 힌트' },
      { key: 'status', label: '상태', render: (row) => <StatusBadge>{row.status}</StatusBadge> },
      {
        key: 'actions',
        label: '조치',
        render: (row) => (
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => handleAction('warning', row.seatId)}
              disabled={submittingSeatId === row.seatId + 'warning'}
              className="primary-button px-3 py-2 text-xs"
            >
              경고 전송
            </button>
            <button
              onClick={() => handleAction('release', row.seatId)}
              disabled={submittingSeatId === row.seatId + 'release'}
              className="secondary-button px-3 py-2 text-xs"
            >
              상태 해제
            </button>
            <button
              onClick={() => handleAction('resolve', row.seatId)}
              disabled={submittingSeatId === row.seatId + 'resolve'}
              className="secondary-button px-3 py-2 text-xs"
            >
              처리 완료
            </button>
          </div>
        ),
      },
    ],
    [submittingSeatId]
  );

  const activeCheckInColumns = useMemo(
    () => [
      { key: 'seatId', label: '좌석' },
      { key: 'statusLabel', label: '현재 상태', render: (row) => <StatusBadge>{row.statusLabel}</StatusBadge> },
      { key: 'lastUpdated', label: '최종 갱신' },
      {
        key: 'actions',
        label: '관리자 조치',
        render: (row) => (
          <button
            onClick={() => handleAction('force-checkout', row.seatId)}
            disabled={submittingSeatId === row.seatId + 'force-checkout'}
            className="primary-button px-3 py-2 text-xs"
          >
            강제 퇴실
          </button>
        ),
      },
    ],
    [submittingSeatId]
  );

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">조치 항목을 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="관리자 조치 기능" description="문제 좌석을 확인하고 경고, 상태 해제, 처리 완료를 수행합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="경고 대기" value={data.summary.pendingWarnings} helper="즉시 조치 권장" accent="amber" />
        <MetricCard label="상태 해제 필요" value={data.summary.pendingReleases} helper="현장 확인 권장" accent="rose" />
        <MetricCard label="현재 발권 좌석" value={data.summary.activeCheckIns} helper="강제 퇴실 가능" accent="emerald" />
      </div>

      <SectionCard title="조치 대기 목록" subtitle="비정상 좌석을 기준으로 즉시 액션을 실행할 수 있습니다." className="mt-6">
        <DataTable columns={columns} rows={data.queue} emptyText="현재 조치가 필요한 좌석이 없습니다." />
      </SectionCard>

      <SectionCard title="학생 발권 현황" subtitle="앱에서 체크아웃하지 못한 학생을 관리자가 강제 퇴실 처리할 수 있습니다." className="mt-6">
        <DataTable columns={activeCheckInColumns} rows={data.activeCheckIns} emptyText="현재 발권 중인 학생이 없습니다." />
      </SectionCard>
    </div>
  );
}
