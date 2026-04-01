import { useState } from 'react';
import { getLostItems, updateLostItemStatus } from '../api/dashboardApi';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function LostItemsPage() {
  const { data, loading, error, refetch } = useApiData(getLostItems, []);
  const [updatingId, setUpdatingId] = useState('');

  const handleStatus = async (itemId, status) => {
    try {
      setUpdatingId(itemId + status);
      await updateLostItemStatus(itemId, status);
      await refetch();
    } finally {
      setUpdatingId('');
    }
  };

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">분실물 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="분실물 관리보드" description="좌석 주변에서 수거된 분실물을 관리합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="보관 중" value={data.summary.openCount} helper="관리실 인계 전" accent="amber" />
        <MetricCard label="금일 인계 완료" value={data.summary.claimedToday} helper="소유자 전달" accent="emerald" />
        <MetricCard label="보관함 적재" value={data.summary.storageCount} helper="미회수 누적" accent="brand" />
      </div>

      <SectionCard title="분실물 목록" subtitle="분실물 상태를 변경하고 인계 여부를 기록합니다." className="mt-6">
        <div className="grid gap-4 lg:grid-cols-2">
          {data.items.map((item) => (
            <div key={item.itemId} className="rounded-[24px] border border-slate-200 p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-lg font-semibold text-slate-800">{item.description}</p>
                  <p className="mt-1 text-sm text-slate-500">{item.category} · {item.zone} · {item.seatId}</p>
                </div>
                <StatusBadge>{item.status}</StatusBadge>
              </div>
              <div className="mt-4 grid gap-2 text-sm text-slate-600">
                <p>발견 시각: {item.foundAt}</p>
                <p>담당자: {item.custodian}</p>
              </div>
              <div className="mt-5 flex flex-wrap gap-2">
                <button
                  onClick={() => handleStatus(item.itemId, '보관 중')}
                  disabled={updatingId === item.itemId + '보관 중'}
                  className="secondary-button px-3 py-2 text-xs"
                >
                  보관 중
                </button>
                <button
                  onClick={() => handleStatus(item.itemId, '인계 완료')}
                  disabled={updatingId === item.itemId + '인계 완료'}
                  className="primary-button px-3 py-2 text-xs"
                >
                  인계 완료
                </button>
                <button
                  onClick={() => handleStatus(item.itemId, '폐기 예정')}
                  disabled={updatingId === item.itemId + '폐기 예정'}
                  className="secondary-button px-3 py-2 text-xs"
                >
                  폐기 예정
                </button>
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
