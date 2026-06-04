import { NavLink } from 'react-router-dom';

const menu = [
  { to: '/overview', label: 'Dashboard', icon: '⌂' },
  { to: '/zone-seats', label: '좌석 현황', icon: '▦' },
  { to: '/actions', label: '관리자 조치 기능', icon: '▣' },
  { to: '/abnormal-seats', label: '비정상 좌석', icon: '!' },
  { to: '/lost-items', label: '분실물 관리보드', icon: '◈' },
  { to: '/alert-management', label: '알림 관리', icon: '⚙' },
  { to: '/alert-history', label: '알림 전송 이력', icon: '⎘' },
  { to: '/statistics', label: '통계', icon: '◔' },
  { to: '/system-status', label: '시스템 상태 모니터링', icon: '◎' },
  { to: '/settings', label: '설정', icon: '☰' },
];

export default function Sidebar() {
  return (
    <aside className="hidden w-[280px] shrink-0 border-r border-slate-200/70 bg-white/80 px-5 py-7 backdrop-blur lg:block">
      <div className="mb-8 flex items-center gap-3 px-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-xl text-brand-700">▣</div>
        <div>
          <p className="text-xl font-semibold text-slate-800">Library Admin</p>
          <p className="text-sm text-slate-500">AIoT Seat Control</p>
        </div>
      </div>

      <nav className="grid gap-2">
        {menu.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar-link ${isActive ? 'sidebar-link-active' : ''}`}
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-50 text-sm text-slate-500">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
