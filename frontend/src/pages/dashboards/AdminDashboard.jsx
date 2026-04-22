import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import Navbar from '../../components/Navbar'
import { adminApi } from '../../api/auth'
import { patientApi } from '../../api/patients'
import { appointmentApi, billingApi } from '../../api/appointments'

const ROLES = ['MEDECIN', 'SECRETAIRE', 'INFIRMIER', 'PHARMACIEN', 'LABORANTIN', 'RADIOLOGUE', 'ADMIN', 'DIRECTEUR']

export default function AdminDashboard() {
  const { user } = useAuth()
  const [tab, setTab] = useState('users')
  const [users, setUsers] = useState([])
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [alert, setAlert] = useState(null)
  const [form, setForm] = useState({ nom: '', prenom: '', email: '', role: 'SECRETAIRE', password: '', specialiteId: '' })
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [u, p, a, b] = await Promise.allSettled([
          adminApi.listUsers(),
          patientApi.stats(),
          appointmentApi.getStats(),
          billingApi.getStats(),
        ])
        if (u.status === 'fulfilled') setUsers(u.value.data)
        setStats({
          patients: p.status === 'fulfilled' ? p.value.data : null,
          appointments: a.status === 'fulfilled' ? a.value.data : null,
          billing: b.status === 'fulfilled' ? b.value.data : null,
        })
      } finally { setLoading(false) }
    }
    load()
  }, [])

  const showAlert = (msg, type = 'success') => {
    setAlert({ msg, type })
    setTimeout(() => setAlert(null), 4000)
  }

  const handleToggle = async (id) => {
    try {
      await adminApi.toggleUser(id)
      setUsers(prev => prev.map(u => u.id === id ? { ...u, enabled: !u.enabled } : u))
      showAlert('Compte mis à jour.')
    } catch { showAlert('Erreur lors de la mise à jour.', 'error') }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Supprimer cet utilisateur ?')) return
    try {
      await adminApi.deleteUser(id)
      setUsers(prev => prev.filter(u => u.id !== id))
      showAlert('Utilisateur supprimé.')
    } catch { showAlert('Erreur suppression.', 'error') }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    try {
      await adminApi.createPersonnel(form)
      showAlert(`Compte créé pour ${form.prenom} ${form.nom}.`)
      setForm({ nom: '', prenom: '', email: '', role: 'SECRETAIRE', password: '', specialiteId: '' })
      const res = await adminApi.listUsers()
      setUsers(res.data)
      setTab('users')
    } catch (err) {
      showAlert(err.response?.data?.message || 'Erreur création compte.', 'error')
    } finally { setCreating(false) }
  }

  const roleColor = (role) => ({
    ADMIN: 'red', DIRECTEUR: 'purple', MEDECIN: 'blue', SECRETAIRE: 'green',
    INFIRMIER: 'teal', PATIENT: 'gray', PHARMACIEN: 'orange', LABORANTIN: 'yellow',
  }[role] || 'gray')

  return (
    <div>
      <Navbar />
      <div className="container">
        <div className="page-header">
          <h1>Administration — {user.prenom} {user.nom}</h1>
          <p className="role-badge">{user.role}</p>
        </div>

        {alert && <div className={`alert alert-${alert.type}`}>{alert.msg}</div>}

        {stats && (
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-value">{stats.patients?.totalPatients ?? '—'}</div>
              <div className="stat-label">Patients</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.appointments?.total ?? '—'}</div>
              <div className="stat-label">Rendez-vous</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{users.length}</div>
              <div className="stat-label">Comptes utilisateurs</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.billing?.revenuTotal != null ? `${Number(stats.billing.revenuTotal).toLocaleString('fr-FR')} MAD` : '—'}</div>
              <div className="stat-label">Revenu total</div>
            </div>
          </div>
        )}

        <div className="tabs">
          <button className={`tab-btn ${tab === 'users' ? 'active' : ''}`} onClick={() => setTab('users')}>
            👥 Utilisateurs ({users.length})
          </button>
          <button className={`tab-btn ${tab === 'create' ? 'active' : ''}`} onClick={() => setTab('create')}>
            ➕ Créer un compte
          </button>
        </div>

        {loading ? <div className="loading">Chargement...</div> : (
          <>
            {tab === 'users' && (
              <div className="list">
                {users.length === 0 && <div className="empty-state">Aucun utilisateur</div>}
                {users.map(u => (
                  <div key={u.id} className="card list-item">
                    <div className="patient-avatar">{u.prenom?.[0]}{u.nom?.[0]}</div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700 }}>{u.prenom} {u.nom}</div>
                      <div style={{ fontSize: 13, color: 'var(--gray)' }}>{u.email}</div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <span className={`badge badge-${roleColor(u.role)}`}>{u.role}</span>
                      {!u.enabled && <span className="badge badge-red">Désactivé</span>}
                      <button className={`btn btn-sm ${u.enabled ? 'btn-secondary' : 'btn-primary'}`}
                              onClick={() => handleToggle(u.id)}>
                        {u.enabled ? 'Désactiver' : 'Activer'}
                      </button>
                      {u.role !== 'ADMIN' && (
                        <button className="btn btn-sm btn-danger" onClick={() => handleDelete(u.id)}>
                          Supprimer
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {tab === 'create' && (
              <div className="card" style={{ maxWidth: 540, margin: '0 auto' }}>
                <h2 style={{ marginBottom: 20 }}>Nouveau compte personnel</h2>
                <form onSubmit={handleCreate}>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Prénom</label>
                      <input required value={form.prenom} onChange={e => setForm(f => ({ ...f, prenom: e.target.value }))} />
                    </div>
                    <div className="form-group">
                      <label>Nom</label>
                      <input required value={form.nom} onChange={e => setForm(f => ({ ...f, nom: e.target.value }))} />
                    </div>
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input type="email" required value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label>Rôle</label>
                    <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                      {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Mot de passe</label>
                    <input type="password" required minLength={8} value={form.password}
                           onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
                  </div>
                  {form.role === 'MEDECIN' && (
                    <div className="form-group">
                      <label>ID Spécialité</label>
                      <input type="number" value={form.specialiteId}
                             onChange={e => setForm(f => ({ ...f, specialiteId: e.target.value }))} />
                    </div>
                  )}
                  <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={creating}>
                    {creating ? 'Création...' : 'Créer le compte'}
                  </button>
                </form>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
