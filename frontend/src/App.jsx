import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import VerifyEmailPage from './pages/VerifyEmailPage'
import PatientDashboard from './pages/dashboards/PatientDashboard'
import PersonnelDashboard from './pages/dashboards/PersonnelDashboard'
import AdminDashboard from './pages/dashboards/AdminDashboard'
import DirecteurDashboard from './pages/dashboards/DirecteurDashboard'

function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user?.role)) return <Navigate to="/" replace />
  return children
}

function RoleDashboard() {
  const { user } = useAuth()
  switch (user?.role) {
    case 'PATIENT':    return <PatientDashboard />
    case 'MEDECIN':
    case 'SECRETAIRE':
    case 'PERSONNEL':  return <PersonnelDashboard />
    case 'ADMIN':      return <AdminDashboard />
    case 'DIRECTEUR':  return <DirecteurDashboard />
    default:           return <Navigate to="/" replace />
  }
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/"                   element={<LandingPage />} />
          <Route path="/login"              element={<LoginPage />} />
          <Route path="/reset-password"     element={<ResetPasswordPage />} />
          <Route path="/verify"             element={<VerifyEmailPage />} />
          <Route path="/dashboard"          element={
            <ProtectedRoute>
              <RoleDashboard />
            </ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
