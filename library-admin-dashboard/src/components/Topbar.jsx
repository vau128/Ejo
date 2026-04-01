import { useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const titleMap = {
  '/overview': 'Overview',
  '/actions': '관리자 조치 기능',
  '/alert-history': '알림 전송 이력',
  '/alert-management': '알림 관리',
  '/settings': '설정',
  '/statistics': '통계',
  '/zone-seats': '3구역 좌석 확인',
  '/abnormal-seats': '비정상 좌석',
  '/lost-items': '분실물 관리보드',
  '/system-status': '시스템 상태 모니터링',
};

export default function Topbar() {
  const { pathname } = useLocation();
  const { user, logout } = useAuth();

  return (
    <header className="mb-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div>
        <p className="text-sm font-medium uppercase tracking-[0.25em] text-brand-600">admin dashboard</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-800">{titleMap[pathname] || 'Dashboard'}</h1>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative min-w-[240px]">
          <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">⌕</span>
          <input className="input-base pl-10" placeholder="Search for something" />
        </div>
        <div className="flex items-center gap-3">
          <button className="secondary-button px-3">⟳</button>
          <button className="secondary-button px-3">◷</button>
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
      </div>
    </header>
  );
}
