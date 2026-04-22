import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const ROLE_LABELS = {
  PATIENT:    'Espace Patient',
  MEDECIN:    'Espace Médecin',
  SECRETAIRE: 'Espace Secrétariat',
  ADMIN:      'Administration',
  DIRECTEUR:  'Direction',
  PERSONNEL:  'Personnel',
}

const ROLE_COLORS = {
  PATIENT:    '#059669',
  MEDECIN:    '#2563eb',
  SECRETAIRE: '#7c3aed',
  ADMIN:      '#dc2626',
  DIRECTEUR:  '#d97706',
  PERSONNEL:  '#0891b2',
}

export default function Navbar({ notifCount = 0 }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  const color = ROLE_COLORS[user?.role] || '#2563eb'
  const label = ROLE_LABELS[user?.role] || user?.role

  return (
    <nav className="navbar">
      <a className="navbar-brand" href="/dashboard">
        <div className="navbar-logo-icon">🏥</div>
        <div>
          <div className="navbar-logo-text">MedSys</div>
          <div className="navbar-logo-sub">{label}</div>
        </div>
      </a>

      <div className="navbar-right">
        {notifCount > 0 && (
          <div style={{ position: 'relative', marginRight: 8 }}>
            <span style={{ fontSize: 20 }}>🔔</span>
            <span className="badge-count">{notifCount}</span>
          </div>
        )}

        <div className="user-chip">
          <div className="user-chip-avatar" style={{ background: color }}>
            {user?.prenom?.[0]}{user?.nom?.[0]}
          </div>
          <span className="user-chip-name">{user?.prenom} {user?.nom}</span>
        </div>

        <button className="btn btn-outline btn-sm" onClick={handleLogout}>
          Déconnexion
        </button>
      </div>
    </nav>
  )
}
