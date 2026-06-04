import { Link } from 'react-router-dom';
import { getOverview } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SeatGrid from '../components/SeatGrid';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function OverviewPage() {
  const { data, loading, error } = useApiData(getOverview, [], { intervalMs: 10000 });

  if (loading && !data) {
    return <LoadingState />;
  }

  if (error && !data) {
    return <ErrorState message={error} />;
  }

  const summary = data.summary;

  return (
    <div>
      <PageHeader title="Overview" description="전체 현황과 주요 운영 항목을 한 화면에서 확인합니다." />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-6">
        <MetricCard compact label="총 좌석 수" value={summary.totalSeats} helper="운영 기준 4석" />
        <MetricCard compact label="사용 좌석 수" value={summary.occupiedSeats} helper="실시간 점유" accent="emerald" />
        <MetricCard compact label="사용 가능" value={summary.availableSeats} helper="즉시 이용 가능" />
        <MetricCard compact label="비정상 좌석" value={summary.abnormalSeats} helper="확인 필요" accent="rose" />
        <MetricCard compact label="금일 알림" value={summary.alertsToday} helper="발송 기준" accent="amber" />
        <MetricCard compact label="보관 중 분실물" value={summary.openLostItems} helper="관리실 기준" accent="violet" />
      </div>

      <div className="mt-6">
        <SectionCard
          title="좌석 현황"
          subtitle="좌석 상태를 큰 카드로 정리해 한 번에 확인할 수 있습니다."
          action={<Link to="/zone-seats" className="secondary-button">좌석 상세 보기</Link>}
        >
          <div className="mb-5 flex flex-wrap gap-3 text-sm text-slate-500">
            <span className="rounded-full bg-slate-100 px-3 py-1">총 {data.zonePreview.totalSeats}석</span>
            <span className="rounded-full bg-emerald-50 px-3 py-1 text-emerald-700">사용 중 {data.zonePreview.occupiedSeats}</span>
            <span className="rounded-full bg-rose-50 px-3 py-1 text-rose-700">비정상 {data.zonePreview.abnormalSeats}</span>
          </div>
          <SeatGrid seats={data.zonePreview.seats} selectedSeatId={null} onSelect={() => {}} />
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.3fr_1fr]">
        <SectionCard
          title="관리자 조치 기능"
          subtitle="비정상 좌석을 확인하고 즉시 조치를 실행할 수 있습니다."
          action={<Link to="/actions" className="secondary-button">전체 보기</Link>}
        >
          <div className="mb-5 grid gap-3 sm:grid-cols-3">
            <Link to="/actions" className="primary-button py-4">경고 전송</Link>
            <Link to="/actions" className="secondary-button py-4">상태 해제</Link>
            <Link to="/actions" className="secondary-button py-4">처리 완료 체크</Link>
          </div>
          <DataTable
            columns={[
              { key: 'seatId', label: '좌석' },
              { key: 'issueType', label: '문제 유형' },
              { key: 'duration', label: '지속 시간' },
              { key: 'status', label: '상태', render: (row) => <StatusBadge>{row.status}</StatusBadge> },
            ]}
            rows={data.actionQueue}
            emptyText="대기 중인 조치 항목이 없습니다."
          />
        </SectionCard>

        <SectionCard
          title="알림 전송 이력"
          subtitle="최근 발송 이력을 빠르게 확인할 수 있습니다."
          action={<Link to="/alert-history" className="secondary-button">전체 보기</Link>}
        >
          <div className="grid gap-4">
            {data.recentAlertHistory.map((item) => (
              <div key={item.id} className="rounded-2xl border border-slate-200 p-4">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="text-sm font-semibold text-slate-800">{item.seatId} · {item.messageType}</p>
                    <p className="mt-1 text-sm text-slate-500">{item.createdAt}</p>
                  </div>
                  <StatusBadge>{item.status}</StatusBadge>
                </div>
                <p className="mt-3 text-sm text-slate-600">{item.message}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <div className="grid gap-6">
          <SectionCard
            title="분실물 관리보드"
            subtitle="최근 접수된 분실물을 보여줍니다."
            action={<Link to="/lost-items" className="secondary-button">전체 보기</Link>}
          >
            <div className="grid gap-3">
              {data.lostItemsPreview.map((item) => (
                <div key={item.itemId} className="flex items-center justify-between rounded-2xl border border-slate-200 p-4">
                  <div>
                    <p className="font-medium text-slate-800">{item.description}</p>
                    <p className="mt-1 text-sm text-slate-500">{item.zone} · {item.foundAt}</p>
                  </div>
                  <StatusBadge>{item.status}</StatusBadge>
                </div>
              ))}
            </div>
          </SectionCard>
        </div>

        <div className="grid gap-6">
          <SectionCard
            title="시스템 상태 모니터링"
            subtitle="센서, 카메라, 지연 상태를 확인합니다."
            action={<Link to="/system-status" className="secondary-button">시스템 보기</Link>}
          >
            <div className="mb-4 grid gap-3 sm:grid-cols-3">
              <div className="rounded-2xl bg-slate-50 p-4">
                <p className="text-sm text-slate-500">센서 연결</p>
                <p className="mt-2 text-2xl font-semibold text-slate-800">{data.systemPreview.connectedSensors}</p>
              </div>
              <div className="rounded-2xl bg-slate-50 p-4">
                <p className="text-sm text-slate-500">카메라 상태</p>
                <p className="mt-2 text-2xl font-semibold text-slate-800">{data.systemPreview.cameraOnline}</p>
              </div>
              <div className="rounded-2xl bg-slate-50 p-4">
                <p className="text-sm text-slate-500">지연 건수</p>
                <p className="mt-2 text-2xl font-semibold text-slate-800">{data.systemPreview.delayedFeeds}</p>
              </div>
            </div>
            <div className="grid gap-3">
              {data.systemPreview.devices.map((device) => (
                <div key={device.deviceId} className="flex items-center justify-between rounded-2xl border border-slate-200 p-4">
                  <div>
                    <p className="font-medium text-slate-800">{device.deviceId}</p>
                    <p className="mt-1 text-sm text-slate-500">{device.type} · {device.zone}</p>
                  </div>
                  <StatusBadge>{device.status}</StatusBadge>
                </div>
              ))}
            </div>
          </SectionCard>
        </div>
      </div>
    </div>
  );
}

function LoadingState() {
  return <div className="app-card p-10 text-center text-sm text-slate-500">데이터를 불러오는 중입니다.</div>;
}

function ErrorState({ message }) {
  return <div className="app-card p-10 text-center text-sm text-rose-600">{message}</div>;
}
