import { useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'

export default function ResetPasswordPage() {
  const [params]  = useSearchParams()
  const navigate  = useNavigate()
  const token     = params.get('token')

  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading]   = useState(false)
  const [message, setMessage]   = useState(null)
  const [error, setError]       = useState(null)

  const handleForgot = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await authApi.forgotPassword(email)
      setMessage('Si cet email existe, un lien de réinitialisation a été envoyé.')
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur')
    } finally {
      setLoading(false)
    }
  }

  const handleReset = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await authApi.resetPassword({ token, newPassword: password })
      setMessage('Mot de passe réinitialisé ! Redirection...')
      setTimeout(() => navigate('/login'), 2000)
    } catch (err) {
      setError(err.response?.data?.message || 'Token invalide ou expiré')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2>{token ? 'Nouveau mot de passe' : 'Mot de passe oublié'}</h2>
        {message && <div className="alert alert-success">{message}</div>}
        {error   && <div className="alert alert-error">{error}</div>}

        {!token ? (
          <form onSubmit={handleForgot}>
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={email}
                     onChange={e => setEmail(e.target.value)} required placeholder="votre@email.com" />
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Envoi...' : 'Envoyer le lien'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleReset}>
            <div className="form-group">
              <label>Nouveau mot de passe</label>
              <input type="password" value={password} minLength={8}
                     onChange={e => setPassword(e.target.value)} required placeholder="Minimum 8 caractères" />
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Enregistrement...' : 'Réinitialiser'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
