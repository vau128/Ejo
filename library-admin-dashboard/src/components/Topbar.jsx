import { useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const sectionMap = {
  '/overview': '대시보드',
  '/actions': '실시간 조치',
  '/alert-history': '알림 이력',
  '/alert-management': '알림 설정',
  '/settings': '설정',
  '/statistics': '통계',
  '/zone-seats': '좌석 모니터링',
  '/abnormal-seats': '예외 좌석',
  '/lost-items': '분실물',
  '/system-status': '시스템 상태',
};

export default function Topbar() {
  const { pathname } = useLocation();
  const { user, logout } = useAuth();
  const currentDate = new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(new Date());

  return (
    <header className="mb-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div>
        <p className="text-sm font-medium uppercase tracking-[0.25em] text-brand-600">admin dashboard</p>
        <p className="mt-2 text-sm text-slate-500">{sectionMap[pathname] || '대시보드'} · {currentDate}</p>
      </div>

      <div className="flex items-center gap-3 self-start sm:self-auto">
        <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">
          자동 갱신 데이터 기준
        </div>
        <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-2">
          <div className="flex h-11 w-11 items-center justify-center rounded-full bg-brand-50 text-brand-700">
            {user?.name?.slice(0, 1) || 'A'}
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-800">{user?.name || '관리자'}</p>
            <button onClick={logout} className="text-xs text-slate-500 hover:text-slate-700">
              로그아웃
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
