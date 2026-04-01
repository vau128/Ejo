import { useEffect, useMemo, useState } from 'react';
import { getSeatDetail, getZoneSeats } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SearchField from '../components/SearchField';
import SeatGrid from '../components/SeatGrid';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';

const filters = [
  { value: '', label: '전체' },
  { value: 'OCCUPIED', label: '사용 중' },
  { value: 'AVAILABLE', label: '비어있음' },
  { value: 'OBJECT_ONLY', label: '물품' },
  { value: 'VACANT_LONG', label: '장시간 비움' },
  { value: 'SENSOR_DELAY', label: '센서 지연' },
];

export default function ZoneSeatsPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [data, setData] = useState(null);
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        const response = await getZoneSeats({ status, search });
        if (cancelled) return;
        setData(response);
        setError('');
        if (response.seats.length) {
          const target = detail?.seatId ? response.seats.find((seat) => seat.seatId === detail.seatId) : response.seats[0];
          if (target) {
            const seatDetail = await getSeatDetail(target.seatId);
            if (!cancelled) setDetail(seatDetail);
          } else {
            setDetail(null);
          }
        } else {
          setDetail(null);
        }
      } catch (err) {
        if (!cancelled) setError(err.response?.data?.message || '좌석 데이터를 불러오지 못했습니다.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [search, status]);

  const seatColumns = useMemo(
    () => [
      { key: 'seatId', label: '좌석' },
      { key: 'statusLabel', label: '상태', render: (row) => <StatusBadge>{row.statusLabel}</StatusBadge> },
      { key: 'lastUpdated', label: '최근 변경 시각' },
      { key: 'notes', label: '비고' },
    ],
    []
  );

  const selectSeat = async (seat) => {
    const response = await getSeatDetail(seat.seatId);
    setDetail(response);
  };

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">좌석 현황을 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="3구역 좌석 확인" description="좌석 상태 필터와 검색을 통해 3구역 좌석을 관리합니다." />

      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard label="총 좌석 수" value={data.summary.totalSeats} helper="3구역 기준" />
        <MetricCard label="사용 좌석 수" value={data.summary.occupiedSeats} helper="실시간 점유" accent="emerald" />
        <MetricCard label="비어있는 좌석" value={data.summary.availableSeats} helper="즉시 사용 가능" />
        <MetricCard label="비정상 좌석 수" value={data.summary.abnormalSeats} helper="점검 필요" accent="rose" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.25fr_0.95fr]">
        <SectionCard title="좌석 상태" subtitle="좌석 번호별 상태를 바로 확인할 수 있습니다.">
          <div className="mb-5 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
            <div className="flex flex-wrap gap-2">
              {filters.map((filter) => (
                <button
                  key={filter.value || 'all'}
                  onClick={() => setStatus(filter.value)}
                  className={`${status === filter.value ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-600'} rounded-full px-4 py-2 text-sm font-medium transition`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
            <SearchField value={search} onChange={setSearch} placeholder="좌석 번호를 검색하세요" />
          </div>
          <SeatGrid seats={data.seats} selectedSeatId={detail?.seatId} onSelect={selectSeat} />
          <div className="mt-6">
            <DataTable columns={seatColumns} rows={data.seats} emptyText="검색 결과가 없습니다." />
          </div>
        </SectionCard>

        <SectionCard title="좌석 상세 정보" subtitle="선택한 좌석의 센서와 상태 이력을 보여줍니다.">
          {detail ? (
            <div className="grid gap-5">
              <div className="rounded-2xl bg-slate-50 p-5">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm text-slate-500">선택 좌석</p>
                    <p className="mt-2 text-3xl font-semibold text-slate-800">{detail.seatId}</p>
                  </div>
                  <StatusBadge>{detail.statusLabel}</StatusBadge>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm text-slate-600">
                  <div className="rounded-2xl bg-white p-4">
                    <p className="text-slate-500">최근 변경</p>
                    <p className="mt-1 font-semibold text-slate-800">{detail.lastUpdated}</p>
                  </div>
                  <div className="rounded-2xl bg-white p-4">
                    <p className="text-slate-500">상태 유지 시간</p>
                    <p className="mt-1 font-semibold text-slate-800">{detail.durationMinutes}분</p>
                  </div>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <SensorCard label="압력 값" value={detail.sensor.pressureValue} />
                <SensorCard label="사람 감지" value={detail.sensor.personDetected ? '감지' : '미감지'} />
                <SensorCard label="물체 감지" value={detail.sensor.objectDetected ? '감지' : '미감지'} />
                <SensorCard label="카메라 신뢰도" value={detail.sensor.cameraConfidence} />
              </div>

              <div>
                <p className="mb-3 text-sm font-semibold text-slate-700">최근 상태 이력</p>
                <div className="grid gap-3">
                  {detail.history.map((item) => (
                    <div key={item.time} className="rounded-2xl border border-slate-200 p-4">
                      <div className="flex items-center justify-between gap-4">
                        <p className="font-medium text-slate-800">{item.statusLabel}</p>
                        <span className="text-sm text-slate-500">{item.time}</span>
                      </div>
                      <p className="mt-2 text-sm text-slate-600">{item.reason}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-dashed border-slate-200 p-10 text-center text-sm text-slate-500">
              좌석을 선택하면 상세 정보를 볼 수 있습니다.
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
}

function SensorCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 p-4">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-lg font-semibold text-slate-800">{value}</p>
    </div>
  );
}
