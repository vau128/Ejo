import { useEffect, useRef, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { testBackendConnection } from './api/testApi';
import { useAuth } from './context/AuthContext';
import AdminLayout from './layouts/AdminLayout';
import SeatsDashboardPage from './pages/SeatsDashboardPage';
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
import LoginPage from './pages/LoginPage';

export default function App() {
  const hasCheckedBackend = useRef(false);
  const [backendConnected, setBackendConnected] = useState(false);
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (hasCheckedBackend.current) {
      return;
    }

    hasCheckedBackend.current = true;

    testBackendConnection()
      .then((data) => {
        console.log(data);
        setBackendConnected(true);
      })
      .catch((error) => {
        console.error(error);
        setBackendConnected(false);
      });
  }, []);

  return (
    <>
      <Routes>
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to="/overview" replace /> : <LoginPage />}
        />
        <Route
          element={isAuthenticated ? <AdminLayout /> : <Navigate to="/login" replace />}
        >
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
          <Route path="/seat-test" element={<SeatsDashboardPage />} />
        </Route>
        <Route
          path="*"
          element={<Navigate to={isAuthenticated ? '/' : '/login'} replace />}
        />
      </Routes>
      {backendConnected ? (
        <div className="fixed bottom-4 right-4 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 shadow-sm">
          Backend Connected
        </div>
      ) : null}
    </>
  );
}
