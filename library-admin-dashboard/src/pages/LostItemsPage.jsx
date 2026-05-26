import { useState } from 'react';
import { getLostItemsFeed, triggerLostItemScan } from '../api/dashboardApi';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function LostItemsPage() {
  const { data, loading, error, refetch } = useApiData(getLostItemsFeed, []);
  const [message, setMessage] = useState('');
  const [triggering, setTriggering] = useState(false);

  const handleScan = async () => {
    try {
      setTriggering(true);
      const result = await triggerLostItemScan();
      setMessage(result.message || '분실물 스캔 명령을 전송했습니다.');
      await refetch();
    } catch (err) {
      setMessage(err.response?.data?.message || '분실물 스캔 명령 전송에 실패했습니다.');
    } finally {
      setTriggering(false);
    }
  };

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">분실물 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  const openCount = data.filter((item) => item.status === 'FOUND').length;

  return (
    <div>
      <PageHeader
        title="분실물 관리보드"
        description="관리자 수동 스캔으로 등록된 분실물을 확인합니다."
        right={
          <button onClick={handleScan} disabled={triggering} className="primary-button px-4 py-2 text-sm">
            {triggering ? '전송 중' : '분실물 스캔 시작'}
          </button>
        }
      />

      {message ? (
        <div className={`mb-6 rounded-2xl px-4 py-3 text-sm ${message.includes('실패') ? 'bg-rose-50 text-rose-700' : 'bg-emerald-50 text-emerald-700'}`}>
          {message.includes('triggered') ? '분실물 스캔 명령을 전송했습니다.' : message}
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="탐지 건수" value={data.length} helper="전체 등록 수" accent="brand" />
        <MetricCard label="보관 상태" value={openCount} helper="FOUND 상태" accent="amber" />
        <MetricCard label="최근 스캔" value={data[0]?.detected_time ? '완료' : '대기'} helper="관리자 수동 트리거" accent="emerald" />
      </div>

      <SectionCard title="분실물 목록" subtitle="종류, 좌석 번호, 감지 시각을 보여줍니다." className="mt-6">
        <div className="grid gap-4 lg:grid-cols-2">
          {data.map((item) => (
            <div key={item.item_id} className="rounded-[24px] border border-slate-200 p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-lg font-semibold text-slate-800">종류: {item.category}</p>
                  <p className="mt-1 text-sm text-slate-500">좌석 {item.seat_num} · {formatDate(item.detected_time)}</p>
                </div>
                <StatusBadge>{item.status}</StatusBadge>
              </div>
              <div className="mt-4 grid gap-2 text-sm text-slate-600">
                <p>감지 시각: {formatDate(item.detected_time)}</p>
              </div>
              <div className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-slate-50">
                <img src={item.image_url} alt={item.category} className="h-56 w-full object-cover" />
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('ko-KR');
}
