import { getStatistics } from '../api/dashboardApi';
import MetricCard from '../components/MetricCard';
import MiniLineChart from '../components/MiniLineChart';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import { useApiData } from '../hooks/useApiData';

export default function StatisticsPage() {
  const { data, loading, error } = useApiData(getStatistics, []);

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">통계 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="통계" description="시간대별 이용률, 좌석 회전율, 비정상 발생 빈도를 확인합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="최대 이용률" value={data.summary.peakUsageRate} helper="금일 최고치" accent="brand" />
        <MetricCard label="좌석 회전율" value={data.summary.seatTurnover} helper="평균 사용 회차" accent="emerald" />
        <MetricCard label="비정상 발생 빈도" value={data.summary.abnormalFrequency} helper="최근 7일 기준" accent="rose" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="시간대별 이용률" subtitle="시간 흐름에 따른 점유율 변화입니다.">
          <MiniLineChart labels={data.hourlyUsage.map((item) => item.time)} values={data.hourlyUsage.map((item) => item.value)} />
        </SectionCard>

        <SectionCard title="좌석 회전율" subtitle="시간대별 회전율 추이를 나타냅니다.">
          <MiniLineChart labels={data.turnoverTrend.map((item) => item.time)} values={data.turnoverTrend.map((item) => item.value)} />
        </SectionCard>
      </div>

      <SectionCard title="비정상 발생 분포" subtitle="유형별 발생 건수를 비교합니다." className="mt-6">
        <div className="grid gap-4">
          {data.abnormalBreakdown.map((item) => (
            <div key={item.label}>
              <div className="mb-2 flex items-center justify-between text-sm">
                <span className="text-slate-600">{item.label}</span>
                <span className="font-semibold text-slate-800">{item.value}건</span>
              </div>
              <div className="h-3 rounded-full bg-slate-100">
                <div className="h-3 rounded-full bg-brand-500" style={{ width: `${item.rate}%` }} />
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
