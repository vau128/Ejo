import { useEffect, useState } from 'react';
import SeatGrid from '../components/SeatGrid';
import { fetchCurrentStudent, fetchSeats, toggleSeatSelection } from '../api/studentApi';
import { useAuth } from '../context/AuthContext';

function countByStatus(seats, status) {
  return seats.filter((seat) => seat.status === status).length;
}

export default function SeatsDashboardPage() {
  const { logout, user } = useAuth();
  const [profile, setProfile] = useState(user);
  const [seats, setSeats] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      const [profileData, seatData] = await Promise.all([fetchCurrentStudent(), fetchSeats()]);
      setProfile(profileData.user);
      setSeats(seatData.seats ?? []);
      setError('');
    } catch (err) {
      setError(err.response?.data?.message || '좌석 데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    const timer = window.setInterval(load, 3000);
    return () => window.clearInterval(timer);
  }, []);

  const handleSelect = async (seat) => {
    try {
      const response = await toggleSeatSelection(seat.seatId);
      setMessage(response.message || '좌석 상태를 반영했습니다.');
      await load();
    } catch (err) {
      setError(err.response?.data?.message || '좌석 상태를 변경하지 못했습니다.');
    }
  };

  const selectedSeatId = seats.find((seat) => seat.selectedByCurrentUser)?.seatId ?? null;

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,_#fffdf7_0%,_#eef4ff_100%)] px-4 py-6 text-slate-800">
      <div className="mx-auto max-w-6xl">
        <div className="app-card flex flex-col gap-4 px-6 py-5 md:flex-row md:items-center md:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-slate-500">4 Seat Realtime Dashboard</p>
            <h1 className="mt-2 text-3xl font-semibold">학생 앱 / IoT 공용 4좌석 대시보드</h1>
            <p className="mt-2 text-sm text-slate-500">
              {profile ? `${profile.name} · 경고 ${profile.warningCount}회` : '사용자 정보를 불러오는 중입니다.'}
            </p>
          </div>
          <div className="flex gap-3">
            <button onClick={load} className="secondary-button">새로고침</button>
            <button onClick={logout} className="secondary-button">로그아웃</button>
          </div>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-4">
          <div className="metric-card">
            <div className="text-sm text-slate-500">총 좌석</div>
            <div className="mt-2 text-3xl font-semibold">{seats.length}</div>
          </div>
          <div className="metric-card">
            <div className="text-sm text-slate-500">사용 중</div>
            <div className="mt-2 text-3xl font-semibold">{countByStatus(seats, 'OCCUPIED')}</div>
          </div>
          <div className="metric-card">
            <div className="text-sm text-slate-500">사석화</div>
            <div className="mt-2 text-3xl font-semibold">{countByStatus(seats, 'SQUATTING')}</div>
          </div>
          <div className="metric-card">
            <div className="text-sm text-slate-500">비정상</div>
            <div className="mt-2 text-3xl font-semibold">{countByStatus(seats, 'ABNORMAL')}</div>
          </div>
        </div>

        {message ? <div className="mt-6 rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}
        {error ? <div className="mt-6 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div> : null}

        <div className="mt-6 app-card p-6">
          <div className="flex items-end justify-between gap-4">
            <div>
              <h2 className="text-2xl font-semibold">좌석 상태</h2>
              <p className="mt-1 text-sm text-slate-500">IoT 상태가 seat-1부터 seat-4까지 반영됩니다.</p>
            </div>
            <div className="text-sm text-slate-500">
              {selectedSeatId ? `현재 선택 좌석: ${selectedSeatId}` : '선택된 좌석 없음'}
            </div>
          </div>

          <div className="mt-6">
            {loading ? (
              <div className="text-sm text-slate-500">좌석 데이터를 불러오는 중입니다.</div>
            ) : (
              <SeatGrid
                seats={seats.map((seat) => ({
                  seatId: seat.seatId,
                  status: seat.status,
                  lastUpdated: seat.selectedByCurrentUser ? '내 좌석' : `좌석 번호 ${seat.seatNumber}`,
                }))}
                selectedSeatId={selectedSeatId}
                onSelect={handleSelect}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
