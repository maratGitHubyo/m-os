import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { HomePage } from './pages/HomePage';
import { MapPage } from './pages/MapPage';
import { VictoryPage } from './pages/VictoryPage';

export function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/map" element={<MapPage />} />
          <Route path="/victory" element={<VictoryPage />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
