import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { authApi } from '../api/auth'

export default function VerifyEmailPage() {
  const [params]  = useSearchParams()
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    const token = params.get('token')
    if (!token) { setStatus('error'); return }
    authApi.verifyEmail(token)
      .then(() => setStatus('success'))
      .catch(() => setStatus('error'))
  }, [])

  return (
    <div className="auth-page">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        {status === 'loading' && <p>Vérification en cours...</p>}
        {status === 'success' && (
          <>
            <div style={{ fontSize: 48 }}>✅</div>
            <h2>Email vérifié !</h2>
            <p>Votre compte est activé. Vous pouvez maintenant vous connecter.</p>
            <Link to="/login" className="btn btn-primary">Se connecter</Link>
          </>
        )}
        {status === 'error' && (
          <>
            <div style={{ fontSize: 48 }}>❌</div>
            <h2>Token invalide</h2>
            <p>Le lien de vérification est invalide ou a expiré.</p>
            <Link to="/" className="btn btn-outline">Retour à l'accueil</Link>
          </>
        )}
      </div>
    </div>
  )
}
