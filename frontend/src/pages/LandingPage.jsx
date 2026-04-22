import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useEffect } from 'react'

export default function LandingPage() {
  const { user } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (user) navigate('/dashboard', { replace: true })
  }, [user, navigate])

  return (
    <div className="landing">
      <div className="landing-hero">
        <div className="landing-logo">
          <span className="logo-icon">+</span>
          <span className="logo-text">MedSys</span>
        </div>
        <h1 className="landing-title">Système de Gestion Médicale</h1>
        <p className="landing-subtitle">
          Plateforme intégrée pour la gestion des patients, rendez-vous, dossiers médicaux et facturation.
        </p>
        <button className="btn btn-primary btn-lg" onClick={() => navigate('/login')}>
          Connexion
        </button>
      </div>

      <div className="landing-features">
        {[
          { icon: '👥', title: 'Gestion des patients', desc: 'Dossiers médicaux complets, historique, ordonnances et analyses.' },
          { icon: '📅', title: 'Rendez-vous', desc: 'Planification en ligne, rappels automatiques, liste d\'attente.' },
          { icon: '📋', title: 'Dossier médical', desc: 'Consultations, antécédents, documents et messagerie sécurisée.' },
          { icon: '💰', title: 'Facturation', desc: 'Génération de factures, suivi des paiements et statistiques.' },
        ].map(f => (
          <div key={f.title} className="feature-card">
            <div className="feature-icon">{f.icon}</div>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </div>
        ))}
      </div>

      <footer className="landing-footer">
        <p>MedSys — Solution de santé numérique</p>
      </footer>
    </div>
  )
}
