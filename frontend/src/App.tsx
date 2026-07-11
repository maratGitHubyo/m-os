import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { DashboardPage } from './pages/DashboardPage';
import { EventsPage } from './pages/EventsPage';
import { InventoryPage } from './pages/InventoryPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { LoginPage } from './pages/LoginPage';
import { MapPage } from './pages/MapPage';
import { QuestPage } from './pages/QuestPage';
import { VictoryPage } from './pages/VictoryPage';
import { WalletPage } from './pages/WalletPage';
import { isAuthenticated } from './stores/authStore';

function LoginRoute() {
  if (isAuthenticated()) {
    return <Navigate to="/" replace />;
  }
  return <LoginPage />;
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/map" element={<MapPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/wallet" element={<WalletPage />} />
            <Route path="/quests" element={<QuestPage />} />
            <Route path="/score" element={<LeaderboardPage />} />
            <Route path="/victory" element={<VictoryPage />} />
            <Route path="/events" element={<EventsPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
