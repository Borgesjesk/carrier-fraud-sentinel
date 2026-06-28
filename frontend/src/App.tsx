import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { AlertDetailPage } from './pages/AlertDetailPage';
import { ClientComplaintPage } from './pages/ClientComplaintPage';

function RoleBasedHome() {
  const { user } = useAuth();
  return <Navigate to={user?.role === 'CLIENT' ? '/complaints/new' : '/dashboard'} replace />;
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/alerts/:alertId" element={<ProtectedRoute><AlertDetailPage /></ProtectedRoute>} />
          <Route path="/complaints/new" element={<ProtectedRoute><ClientComplaintPage /></ProtectedRoute>} />
          <Route path="/" element={<ProtectedRoute><RoleBasedHome /></ProtectedRoute>} />
          <Route path="*" element={<ProtectedRoute><RoleBasedHome /></ProtectedRoute>} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
