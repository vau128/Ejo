import { getSeats } from '../api/dashboardApi';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import { useApiData } from '../hooks/useApiData';

export default function StatisticsPage() {
  const { data, loading, error } = useApiData(getSeats, [], { intervalMs: 10000 });

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">통계 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  const seats = data ?? [];
  const occupiedCount = seats.filter((seat) => seat.status === 'OCCUPIED').length;
  const checkedInCount = seats.filter((seat) => seat.checked_in).length;
  const vacantLongCount = seats.filter((seat) => seat.status === 'VACANT_LONG').length;
  const availableCount = seats.filter((seat) => seat.status === 'AVAILABLE').length;

  return (
    <div>
      <PageHeader title="통계" description="발권과 좌석 이용 상태만 확인합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="발권 좌석" value={checkedInCount} helper="체크인 기준" accent="brand" />
        <MetricCard label="사용 좌석" value={occupiedCount} helper="착석 감지" accent="emerald" />
        <MetricCard label="장시간 비움" value={vacantLongCount} helper="관리자 확인 필요" accent="amber" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="좌석 상태 분포" subtitle="현재 좌석 상태를 간단히 요약합니다.">
          <div className="grid gap-3">
            {[
              { label: '사용 중', value: occupiedCount },
              { label: '장시간 비움', value: vacantLongCount },
              { label: '비어있음', value: availableCount },
            ].map((item) => (
              <div key={item.label} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-center justify-between gap-4">
                  <p className="font-medium text-slate-800">{item.label}</p>
                  <span className="text-sm text-slate-500">{item.value}석</span>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="발권 대비 착석" subtitle="발권 후 실제 착석 여부를 비교합니다.">
          <div className="grid gap-4">
            <BarRow label="현재 착석" value={occupiedCount} rate={checkedInCount === 0 ? 0 : Math.round((occupiedCount / checkedInCount) * 100)} tone="bg-emerald-500" />
            <BarRow label="미착석/장시간 비움" value={Math.max(checkedInCount - occupiedCount, 0)} rate={checkedInCount === 0 ? 0 : Math.round((Math.max(checkedInCount - occupiedCount, 0) / checkedInCount) * 100)} tone="bg-amber-500" />
          </div>
        </SectionCard>
      </div>
    </div>
  );
}

function BarRow({ label, value, rate, tone }) {
  return (
    <div>
      <div className="mb-2 flex items-center justify-between text-sm">
        <span className="text-slate-600">{label}</span>
        <span className="font-semibold text-slate-800">{value}석</span>
      </div>
      <div className="h-3 rounded-full bg-slate-100">
        <div className={`h-3 rounded-full ${tone}`} style={{ width: `${Math.max(rate, value > 0 ? 4 : 0)}%` }} />
      </div>
    </div>
  );
}
