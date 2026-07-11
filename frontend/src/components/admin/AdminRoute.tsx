import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../stores/authStore';

export function AdminRoute() {
  const { user } = useAuth();

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}
