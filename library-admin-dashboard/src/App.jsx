import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import AdminLayout from './layouts/AdminLayout';
import LoginPage from './pages/LoginPage';
import OverviewPage from './pages/OverviewPage';
import ActionsPage from './pages/ActionsPage';
import AlertHistoryPage from './pages/AlertHistoryPage';
import AlertManagementPage from './pages/AlertManagementPage';
import SettingsPage from './pages/SettingsPage';
import StatisticsPage from './pages/StatisticsPage';
import ZoneSeatsPage from './pages/ZoneSeatsPage';
import AbnormalSeatsPage from './pages/AbnormalSeatsPage';
import LostItemsPage from './pages/LostItemsPage';
import SystemStatusPage from './pages/SystemStatusPage';

function ProtectedRoute() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="/overview" element={<OverviewPage />} />
          <Route path="/actions" element={<ActionsPage />} />
          <Route path="/alert-history" element={<AlertHistoryPage />} />
          <Route path="/alert-management" element={<AlertManagementPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/statistics" element={<StatisticsPage />} />
          <Route path="/zone-seats" element={<ZoneSeatsPage />} />
          <Route path="/abnormal-seats" element={<AbnormalSeatsPage />} />
          <Route path="/lost-items" element={<LostItemsPage />} />
          <Route path="/system-status" element={<SystemStatusPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
