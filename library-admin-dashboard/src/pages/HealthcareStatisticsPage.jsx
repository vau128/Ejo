import { getHealthcareStatistics } from '../api/dashboardApi';
import DoughnutChart from '../components/DoughnutChart';
import MetricCard from '../components/MetricCard';
import MiniLineChart from '../components/MiniLineChart';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function HealthcareStatisticsPage() {
  const { data, loading, error } = useApiData(getHealthcareStatistics, [], { intervalMs: 10000 });

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">헬스케어 통계를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  const summary = data.summary;
  const trend = data.pressureTrend ?? [];
  const liveSeatHealth = data.liveSeatHealth ?? [];

  return (
    <div>
      <PageHeader title="헬스 케어 통계" description="압력 센서와 자세 로그를 기준으로 최근 자세 상태와 좌석별 건강 지표를 확인합니다." />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="현재 착석 좌석" value={summary.activeSeatCount} helper="실시간 압력 감지 기준" accent="emerald" />
        <MetricCard label="자세 로그 표본" value={summary.postureSampleCount} helper="최근 수집 샘플" accent="brand" />
        <MetricCard label="비정상 자세 비율" value={`${summary.abnormalPostureRate}%`} helper="최근 자세 로그 기준" accent="rose" />
        <MetricCard label="사석화 위험 좌석" value={summary.vacantRiskSeats} helper="장시간 비움 상태" accent="amber" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="자세 분포" subtitle="최근 자세 로그를 유형별로 집계했습니다.">
          <DoughnutChart segments={data.postureBreakdown} />
        </SectionCard>

        <SectionCard title="압력 합계 추이" subtitle="최근 센서 표본의 압력 총합 변화를 빠르게 확인합니다.">
          <MiniLineChart
            labels={trend.map((item) => item.time)}
            values={trend.map((item) => item.value)}
          />
        </SectionCard>
      </div>

      <SectionCard title="좌석별 현재 헬스 상태" subtitle="체크인 여부, 최신 자세, 센서 합계를 한 번에 봅니다." className="mt-6">
        <div className="grid gap-4 lg:grid-cols-2">
          {liveSeatHealth.map((seat) => (
            <div key={seat.seatId} className="rounded-[24px] border border-slate-200 bg-white p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-lg font-semibold text-slate-800">{seat.seatNumber}번 좌석</p>
                  <p className="mt-1 text-sm text-slate-500">{seat.sensorHint}</p>
                </div>
                <StatusBadge>{seat.statusLabel}</StatusBadge>
              </div>
              <div className="mt-4 grid grid-cols-3 gap-3">
                <InfoTile label="자세" value={seat.posture} />
                <InfoTile label="발권" value={seat.checkedIn ? '예' : '아니오'} />
                <InfoTile label="압력 합계" value={seat.pressureTotal} />
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}

function InfoTile({ label, value }) {
  return (
    <div className="rounded-2xl bg-slate-50 px-4 py-3">
      <p className="text-xs font-medium uppercase tracking-[0.12em] text-slate-400">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-800">{value}</p>
    </div>
  );
}
