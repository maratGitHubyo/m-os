import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AdminLayout } from './components/admin/AdminLayout';
import { AdminRoute } from './components/admin/AdminRoute';
import { ToastContainer } from './components/ui/ToastContainer';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AuditPage } from './pages/admin/AuditPage';
import { AdminEventsPage } from './pages/admin/EventsPage';
import { ItemsPage } from './pages/admin/ItemsPage';
import { LocationsPage } from './pages/admin/LocationsPage';
import { NumbersPage } from './pages/admin/NumbersPage';
import { PlayersPage } from './pages/admin/PlayersPage';
import { QrCodesPage } from './pages/admin/QrCodesPage';
import { QuestsPage } from './pages/admin/QuestsPage';
import { AdminScorePage } from './pages/admin/ScorePage';
import { SecretsPage } from './pages/admin/SecretsPage';
import { SessionPage } from './pages/admin/SessionPage';
import { AdminVictoryPage } from './pages/admin/VictoryPage';
import { AdminWalletPage } from './pages/admin/WalletPage';
import { DashboardPage } from './pages/DashboardPage';
import { EventsPage } from './pages/EventsPage';
import { InventoryPage } from './pages/InventoryPage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { LoginPage } from './pages/LoginPage';
import { MapPage } from './pages/MapPage';
import { QuestPage } from './pages/QuestPage';
import { VictoryPage } from './pages/VictoryPage';
import { WalletPage } from './pages/WalletPage';
import { ForbiddenPage } from './pages/ForbiddenPage';
import { NotFoundPage } from './pages/NotFoundPage';
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

          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboardPage />} />
              <Route path="session" element={<SessionPage />} />
              <Route path="players" element={<PlayersPage />} />
              <Route path="wallet" element={<AdminWalletPage />} />
              <Route path="score" element={<AdminScorePage />} />
              <Route path="items" element={<ItemsPage />} />
              <Route path="locations" element={<LocationsPage />} />
              <Route path="qr" element={<QrCodesPage />} />
              <Route path="secrets" element={<SecretsPage />} />
              <Route path="numbers" element={<NumbersPage />} />
              <Route path="quests" element={<QuestsPage />} />
              <Route path="victory" element={<AdminVictoryPage />} />
              <Route path="events" element={<AdminEventsPage />} />
              <Route path="audit" element={<AuditPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      <ToastContainer />
    </BrowserRouter>
  );
}
