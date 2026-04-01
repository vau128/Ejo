import { Outlet } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';

export default function AdminLayout() {
  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffffff,_#eef3fb_60%)] text-slate-800">
      <div className="mx-auto flex min-h-screen max-w-[1600px]">
        <Sidebar />
        <main className="flex-1 px-4 py-4 sm:px-6 lg:px-8 lg:py-7">
          <Topbar />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
