import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AdminLayout } from './components/admin/AdminLayout';
import { AdminRoute } from './components/admin/AdminRoute';
import { OfflineScreen } from './components/OfflineScreen';
import { SplashScreen } from './components/SplashScreen';
import { ToastContainer } from './components/ui/ToastContainer';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AuditPage } from './pages/admin/AuditPage';
import { AdminAuctionPage } from './pages/admin/AuctionPage';
import { LorePage } from './pages/admin/LorePage';
import { AdminEventsPage } from './pages/admin/EventsPage';
import { ItemsPage } from './pages/admin/ItemsPage';
import { CollectionStatsPage } from './pages/admin/CollectionStatsPage';
import { LocationsPage } from './pages/admin/LocationsPage';
import { NumbersPage } from './pages/admin/NumbersPage';
import { PlayersPage } from './pages/admin/PlayersPage';
import { QrCodesPage } from './pages/admin/QrCodesPage';
import { QuestsPage } from './pages/admin/QuestsPage';
import { SecretsPage } from './pages/admin/SecretsPage';
import { SessionPage } from './pages/admin/SessionPage';
import { AdminWalletPage } from './pages/admin/WalletPage';
import { AuctionPage } from './pages/AuctionPage';
import { DashboardPage } from './pages/DashboardPage';
import { EventsPage } from './pages/EventsPage';
import { InventoryPage } from './pages/InventoryPage';
import { LoginPage } from './pages/LoginPage';
import { MapPage } from './pages/MapPage';
import { QuestPage } from './pages/QuestPage';
import { WalletPage } from './pages/WalletPage';
import { QrScanPage } from './pages/QrScanPage';
import { QrScannerPage } from './pages/QrScannerPage';
import { SecretsPage as PlayerSecretsPage } from './pages/SecretsPage';
import { TradesPage } from './pages/TradesPage';
import { ForbiddenPage } from './pages/ForbiddenPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { isAuthenticated } from './stores/authStore';

function LoginRoute() {
  if (isAuthenticated()) {
    return <Navigate to="/" replace />;
  }
  return <LoginPage />;
}

function useHideMobileBrowserChrome() {
  useEffect(() => {
    const hide = () => {
      if (window.matchMedia('(display-mode: standalone)').matches) {
        return;
      }
      window.scrollTo(0, 1);
    };
    hide();
    window.addEventListener('orientationchange', hide);
    return () => window.removeEventListener('orientationchange', hide);
  }, []);
}

export function App() {
  useHideMobileBrowserChrome();

  return (
    <BrowserRouter>
      <SplashScreen />
      <OfflineScreen />
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/map" element={<MapPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/wallet" element={<WalletPage />} />
            <Route path="/scan" element={<QrScannerPage />} />
            <Route path="/qr/:publicId" element={<QrScannerPage />} />
            <Route path="/qr" element={<QrScanPage />} />
            <Route path="/secrets" element={<PlayerSecretsPage />} />
            <Route path="/trades" element={<TradesPage />} />
            <Route path="/quests" element={<QuestPage />} />
            <Route path="/events" element={<EventsPage />} />
            <Route path="/auction" element={<AuctionPage />} />
          </Route>

          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboardPage />} />
              <Route path="session" element={<SessionPage />} />
              <Route path="players" element={<PlayersPage />} />
              <Route path="wallet" element={<AdminWalletPage />} />
              <Route path="items" element={<ItemsPage />} />
              <Route path="collection" element={<CollectionStatsPage />} />
              <Route path="locations" element={<LocationsPage />} />
              <Route path="qr" element={<QrCodesPage />} />
              <Route path="secrets" element={<SecretsPage />} />
              <Route path="numbers" element={<NumbersPage />} />
              <Route path="quests" element={<QuestsPage />} />
              <Route path="events" element={<AdminEventsPage />} />
              <Route path="auction" element={<AdminAuctionPage />} />
              <Route path="lore" element={<LorePage />} />
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
