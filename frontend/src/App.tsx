import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { MfaSetupPage } from './pages/MfaSetupPage';
import { SimulatePage } from './pages/SimulatePage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { ProfilePage } from './pages/ProfilePage';
import { DashboardPage } from './pages/DashboardPage';
import { AlertDetailPage } from './pages/AlertDetailPage';
import { ClientComplaintPage } from './pages/ClientComplaintPage';
import { MyComplaintsPage } from './pages/MyComplaintsPage';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { SessionsPage } from './pages/SessionsPage';
import { AuditLogPage } from './pages/AuditLogPage';

function RoleBasedHome() {
  const { user, status } = useAuth();

  if (status === 'unknown') return null;
  if (!user) return <Navigate to='/login' replace />;
  return <Navigate to={user.role === 'CLIENT' ? '/complaints/mine' : '/dashboard'} replace />;
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
          <Route path="/alerts/:alertId" element={<ProtectedRoute><AlertDetailPage /></ProtectedRoute>} />
          <Route path="/complaints/new" element={<ProtectedRoute><ClientComplaintPage /></ProtectedRoute>} />
          <Route path="/complaints/mine" element={<ProtectedRoute><MyComplaintsPage /></ProtectedRoute>} />
          <Route path="/settings/mfa" element={<ProtectedRoute><MfaSetupPage /></ProtectedRoute>} />
          <Route path="/simulate" element={<ProtectedRoute><SimulatePage /></ProtectedRoute>} />
          <Route path="/analytics" element={<ProtectedRoute><AnalyticsPage /></ProtectedRoute>} />
          <Route path="/settings/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
          <Route path="/settings/sessions" element={<ProtectedRoute><SessionsPage /></ProtectedRoute>} />
          <Route path="/admin/users" element={<ProtectedRoute><AdminUsersPage /></ProtectedRoute>} />
          <Route path="/admin/audit" element={<ProtectedRoute><AuditLogPage /></ProtectedRoute>} />
          <Route path="/" element={<ProtectedRoute><RoleBasedHome /></ProtectedRoute>} />
          <Route path="*" element={<ProtectedRoute><RoleBasedHome /></ProtectedRoute>} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
