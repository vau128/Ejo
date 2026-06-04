import { useMemo, useState } from 'react';
import { getSeats } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SearchField from '../components/SearchField';
import SeatGrid from '../components/SeatGrid';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

const filters = [
  { value: '', label: '전체' },
  { value: 'OCCUPIED', label: '사용 중' },
  { value: 'AVAILABLE', label: '비어있음' },
  { value: 'VACANT_LONG', label: '장시간 비움' },
];

export default function ZoneSeatsPage() {
  const { data, loading, error } = useApiData(getSeats, [], { intervalMs: 8000 });
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [selectedSeatNum, setSelectedSeatNum] = useState(null);

  const seats = useMemo(() => data ?? [], [data]);
  const filteredSeats = useMemo(
    () =>
      seats.filter((seat) => {
        const statusMatched = !status || seat.status === status;
        const keyword = search.trim().toLowerCase();
        const searchMatched =
          !keyword ||
          String(seat.seat_num).includes(keyword) ||
          String(seat.seat_code ?? '').toLowerCase().includes(keyword) ||
          String(seat.location ?? '').toLowerCase().includes(keyword) ||
          statusLabel(seat.status).toLowerCase().includes(keyword);
        return statusMatched && searchMatched;
      }),
    [search, seats, status]
  );

  const selectedSeat = filteredSeats.find((seat) => seat.seat_num === selectedSeatNum) ?? filteredSeats[0] ?? null;

  const seatColumns = useMemo(
    () => [
      { key: 'seat_num', label: '좌석' },
      { key: 'status', label: '상태', render: (row) => <StatusBadge>{statusLabel(row.status)}</StatusBadge> },
      { key: 'checked_in', label: '발권', render: (row) => (row.checked_in ? '발권 중' : '미발권') },
      { key: 'location', label: '위치' },
    ],
    []
  );

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">좌석 현황을 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="좌석 현황" description="4개 좌석의 발권 상태와 이용 현황만 확인합니다." />

      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard label="총 좌석 수" value={seats.length} helper="조회 API 기준" />
        <MetricCard label="사용 좌석 수" value={seats.filter((seat) => seat.status === 'OCCUPIED').length} helper="현재 점유" accent="emerald" />
        <MetricCard label="발권 좌석" value={seats.filter((seat) => seat.checked_in).length} helper="체크인 기준" accent="brand" />
        <MetricCard label="장시간 비움" value={seats.filter((seat) => seat.status === 'VACANT_LONG').length} helper="관리 필요" accent="rose" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.25fr_0.95fr]">
        <SectionCard title="좌석 상태" subtitle="상태 필터와 검색으로 필요한 좌석만 좁혀 볼 수 있습니다.">
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
            <SearchField value={search} onChange={setSearch} placeholder="좌석 번호 또는 위치 검색" />
          </div>
          <SeatGrid
            seats={filteredSeats.map((seat) => ({
              seatId: `seat-${seat.seat_num}`,
              status: seat.status,
              location: seat.location,
              lastUpdated: `${statusLabel(seat.status)} · ${seat.checked_in ? '발권 중' : '미발권'}`,
            }))}
            selectedSeatId={selectedSeat ? `seat-${selectedSeat.seat_num}` : null}
            onSelect={(seat) => setSelectedSeatNum(Number(String(seat.seatId).replace('seat-', '')))}
            variant="detailed"
          />
          <div className="mt-6">
            <DataTable columns={seatColumns} rows={filteredSeats} emptyText="검색 결과가 없습니다." />
          </div>
        </SectionCard>

        <SectionCard title="좌석 상세 정보" subtitle="좌석 상태와 발권 여부만 확인합니다.">
          {selectedSeat ? (
            <div className="grid gap-5">
              <div className="rounded-2xl bg-slate-50 p-5">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm text-slate-500">선택 좌석</p>
                    <p className="mt-2 text-3xl font-semibold text-slate-800">{selectedSeat.location}</p>
                  </div>
                  <StatusBadge>{statusLabel(selectedSeat.status)}</StatusBadge>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm text-slate-600">
                  <div className="rounded-2xl bg-white p-4">
                    <p className="text-slate-500">발권 상태</p>
                    <p className="mt-1 font-semibold text-slate-800">{selectedSeat.checked_in ? '체크인 중' : '미체크인'}</p>
                  </div>
                  <div className="rounded-2xl bg-white p-4">
                    <p className="text-slate-500">마지막 갱신</p>
                    <p className="mt-1 font-semibold text-slate-800">{formatDate(selectedSeat.updated_at)}</p>
                  </div>
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

function statusLabel(status) {
  switch (status) {
    case 'OCCUPIED':
      return '사용 중';
    case 'VACANT_LONG':
      return '장시간 비움';
    default:
      return '비어있음';
  }
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('ko-KR');
}
