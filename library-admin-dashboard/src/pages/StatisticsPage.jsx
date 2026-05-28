import { getSeats } from '../api/dashboardApi';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import { useApiData } from '../hooks/useApiData';

const normalPostures = ['정상', '바른 자세 유지 중'];

export default function StatisticsPage() {
  const { data, loading, error } = useApiData(getSeats, [], { intervalMs: 10000 });

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">통계 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  const seats = data ?? [];
  const postureRows = seats.map((seat) => ({ label: seat.location, posture: seat.posture }));
  const normalCount = seats.filter((seat) => normalPostures.includes(seat.posture)).length;
  const abnormalCount = seats.length - normalCount;
  const normalRatio = seats.length ? Math.round((normalCount / seats.length) * 100) : 0;

  return (
    <div>
      <PageHeader title="통계" description="현재 자세 상태와 사석화 연관 지표를 확인합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="정상 자세 비율" value={`${normalRatio}%`} helper="현재 좌석 기준" accent="emerald" />
        <MetricCard label="비정상 자세 횟수" value={abnormalCount} helper="현재 감지 좌석 수" accent="rose" />
        <MetricCard label="사석화 좌석" value={seats.filter((seat) => seat.status === 'VACANT_LONG' || seat.status === 'OBJECT_ONLY' || seat.status === 'SENSOR_DELAY').length} helper="관리자 확인 필요" accent="amber" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="최근 감지 자세" subtitle="좌석별 최신 헬스케어 상태입니다.">
          <div className="grid gap-3">
            {postureRows.map((item) => (
              <div key={item.label} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-center justify-between gap-4">
                  <p className="font-medium text-slate-800">{item.label}</p>
                  <span className="text-sm text-slate-500">{item.posture}</span>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="자세 분포" subtitle="정상과 비정상 상태를 단순 비율로 표시합니다.">
          <div className="grid gap-4">
            <BarRow label="정상 자세" value={normalCount} rate={normalRatio} tone="bg-emerald-500" />
            <BarRow label="비정상 자세" value={abnormalCount} rate={100 - normalRatio} tone="bg-rose-500" />
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
        <div className={`h-3 rounded-full ${tone}`} style={{ width: `${Math.max(rate, 4)}%` }} />
      </div>
    </div>
  );
}
