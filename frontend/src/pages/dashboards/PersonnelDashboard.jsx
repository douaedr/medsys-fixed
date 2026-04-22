import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import Navbar from '../../components/Navbar'
import DossierMedical from '../../components/DossierMedical'
import { patientApi, medicalRecordApi } from '../../api/patients'
import { appointmentApi, billingApi } from '../../api/appointments'

export default function PersonnelDashboard() {
  const { user } = useAuth()
  const [tab, setTab]               = useState('patients')
  const [patients, setPatients]     = useState([])
  const [search, setSearch]         = useState('')
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [dossier, setDossier]       = useState(null)
  const [rdvs, setRdvs]             = useState([])
  const [loading, setLoading]       = useState(true)
  const [stats, setStats]           = useState(null)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [p, s, r] = await Promise.allSettled([
          patientApi.getAll(),
          patientApi.stats(),
          user.personnelId ? appointmentApi.getByMedecin(user.personnelId) : Promise.resolve({ data: [] }),
        ])
        if (p.status === 'fulfilled') setPatients(p.value.data)
        if (s.status === 'fulfilled') setStats(s.value.data)
        if (r.status === 'fulfilled') setRdvs(r.value.data)
      } finally { setLoading(false) }
    }
    load()
  }, [user.personnelId])

  const handleSearch = async () => {
    if (!search.trim()) {
      const res = await patientApi.getAll()
      setPatients(res.data)
    } else {
      const res = await patientApi.search(search)
      setPatients(res.data)
    }
  }

  const selectPatient = async (patient) => {
    setSelectedPatient(patient)
    setTab('dossier')
    try {
      const res = await medicalRecordApi.getDossier(patient.id)
      setDossier(res.data)
    } catch { setDossier(null) }
  }

  const todayRdvs = rdvs.filter(r => {
    const d = new Date(r.dateHeure)
    const t = new Date()
    return d.toDateString() === t.toDateString()
  })

  return (
    <div>
      <Navbar />
      <div className="container">
        <div className="page-header">
          <h1>Tableau de bord — {user.prenom} {user.nom}</h1>
          <p className="role-badge">{user.role}</p>
        </div>

        {stats && (
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-value">{stats.totalPatients}</div>
              <div className="stat-label">Patients</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.nouveauxCeMois}</div>
              <div className="stat-label">Nouveaux ce mois</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{todayRdvs.length}</div>
              <div className="stat-label">RDV aujourd'hui</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{rdvs.filter(r => r.status === 'PENDING').length}</div>
              <div className="stat-label">RDV en attente</div>
            </div>
          </div>
        )}

        <div className="tabs">
          <button className={`tab-btn ${tab === 'patients' ? 'active' : ''}`} onClick={() => setTab('patients')}>
            👥 Patients
          </button>
          <button className={`tab-btn ${tab === 'rdv' ? 'active' : ''}`} onClick={() => setTab('rdv')}>
            📅 Rendez-vous ({rdvs.length})
          </button>
          {selectedPatient && (
            <button className={`tab-btn ${tab === 'dossier' ? 'active' : ''}`} onClick={() => setTab('dossier')}>
              📁 Dossier — {selectedPatient.prenom} {selectedPatient.nom}
            </button>
          )}
        </div>

        {loading ? <div className="loading">Chargement...</div> : (
          <>
            {tab === 'patients' && (
              <div>
                <div className="search-bar">
                  <input type="text" placeholder="Rechercher par nom, prénom, CIN..."
                         value={search} onChange={e => setSearch(e.target.value)}
                         onKeyDown={e => e.key === 'Enter' && handleSearch()} />
                  <button className="btn btn-primary" onClick={handleSearch}>Rechercher</button>
                </div>
                <div className="list">
                  {patients.map(p => (
                    <div key={p.id} className="card list-item clickable" onClick={() => selectPatient(p)}>
                      <div className="patient-avatar">{p.prenom?.[0]}{p.nom?.[0]}</div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700 }}>{p.prenom} {p.nom}</div>
                        <div style={{ fontSize: 13, color: 'var(--gray)' }}>
                          CIN: {p.cin} · {p.telephone || '—'} · {p.ville || '—'}
                        </div>
                      </div>
                      <div>
                        {p.groupeSanguin && <span className="badge badge-red">{p.groupeSanguin}</span>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {tab === 'rdv' && (
              <div>
                {!rdvs.length ? <div className="empty-state">Aucun rendez-vous</div> : (
                  <div className="list">
                    {rdvs.map(r => (
                      <div key={r.id} className="card list-item">
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 700 }}>{r.patientPrenom} {r.patientNom}</div>
                          <div style={{ fontSize: 13, color: 'var(--gray)' }}>
                            {new Date(r.dateHeure).toLocaleString('fr-FR')}
                          </div>
                          {r.motif && <div style={{ fontSize: 13 }}>{r.motif}</div>}
                        </div>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <span className={`badge badge-${statusColor(r.status)}`}>{statusLabel(r.status)}</span>
                          {r.status === 'PENDING' && (
                            <button className="btn btn-sm btn-primary"
                                    onClick={() => appointmentApi.confirm(r.id).then(() =>
                                      setRdvs(prev => prev.map(x => x.id === r.id ? { ...x, status: 'CONFIRMED' } : x)))}>
                              Confirmer
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {tab === 'dossier' && selectedPatient && (
              <DossierMedical dossier={dossier} />
            )}
          </>
        )}
      </div>
    </div>
  )
}

function statusLabel(s) {
  return { PENDING: 'En attente', CONFIRMED: 'Confirmé', COMPLETED: 'Terminé',
           CANCELLED: 'Annulé', NO_SHOW: 'Absent' }[s] || s
}
function statusColor(s) {
  return { PENDING: 'yellow', CONFIRMED: 'blue', COMPLETED: 'green',
           CANCELLED: 'red', NO_SHOW: 'gray' }[s] || 'gray'
}
