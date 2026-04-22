import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import Navbar from '../../components/Navbar'
import { patientApi } from '../../api/patients'
import { appointmentApi, billingApi } from '../../api/appointments'

export default function DirecteurDashboard() {
  const { user } = useAuth()
  const [tab, setTab] = useState('overview')
  const [patientStats, setPatientStats] = useState(null)
  const [apptStats, setApptStats] = useState(null)
  const [billingStats, setBillingStats] = useState(null)
  const [invoices, setInvoices] = useState([])
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [ps, as, bs, inv, pts] = await Promise.allSettled([
          patientApi.stats(),
          appointmentApi.getStats(),
          billingApi.getStats(),
          billingApi.getAll(),
          patientApi.getAll(),
        ])
        if (ps.status === 'fulfilled') setPatientStats(ps.value.data)
        if (as.status === 'fulfilled') setApptStats(as.value.data)
        if (bs.status === 'fulfilled') setBillingStats(bs.value.data)
        if (inv.status === 'fulfilled') setInvoices(inv.value.data)
        if (pts.status === 'fulfilled') setPatients(pts.value.data)
      } finally { setLoading(false) }
    }
    load()
  }, [])

  const fmt = (n) => n != null ? Number(n).toLocaleString('fr-FR') : '—'
  const fmtMad = (n) => n != null ? `${fmt(n)} MAD` : '—'

  const statusColor = (s) => ({
    PAID: 'green', PENDING: 'yellow', PARTIALLY_PAID: 'blue',
    CANCELLED: 'red', DRAFT: 'gray', REFUNDED: 'purple',
  }[s] || 'gray')

  const statusLabel = (s) => ({
    PAID: 'Payée', PENDING: 'En attente', PARTIALLY_PAID: 'Partielle',
    CANCELLED: 'Annulée', DRAFT: 'Brouillon', REFUNDED: 'Remboursée',
  }[s] || s)

  return (
    <div>
      <Navbar />
      <div className="container">
        <div className="page-header">
          <h1>Direction — {user.prenom} {user.nom}</h1>
          <p className="role-badge">{user.role}</p>
        </div>

        {loading ? <div className="loading">Chargement...</div> : (
          <>
            <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
              <div className="stat-card">
                <div className="stat-value">{patientStats?.totalPatients ?? '—'}</div>
                <div className="stat-label">Patients</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{patientStats?.nouveauxCeMois ?? '—'}</div>
                <div className="stat-label">Nouveaux ce mois</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{apptStats?.total ?? '—'}</div>
                <div className="stat-label">Total RDV</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{apptStats?.confirmed ?? '—'}</div>
                <div className="stat-label">RDV confirmés</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{fmtMad(billingStats?.revenuTotal)}</div>
                <div className="stat-label">Revenu total</div>
              </div>
              <div className="stat-card">
                <div className="stat-value">{fmtMad(billingStats?.montantEnAttente)}</div>
                <div className="stat-label">En attente</div>
              </div>
            </div>

            <div className="tabs">
              <button className={`tab-btn ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>
                📊 Vue d'ensemble
              </button>
              <button className={`tab-btn ${tab === 'patients' ? 'active' : ''}`} onClick={() => setTab('patients')}>
                👥 Patients ({patients.length})
              </button>
              <button className={`tab-btn ${tab === 'facturation' ? 'active' : ''}`} onClick={() => setTab('facturation')}>
                💰 Facturation ({invoices.length})
              </button>
            </div>

            {tab === 'overview' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
                <div className="card">
                  <h3 style={{ marginBottom: 16 }}>Rendez-vous</h3>
                  {apptStats ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {[
                        ['En attente', apptStats.pending, 'yellow'],
                        ['Confirmés', apptStats.confirmed, 'blue'],
                        ['Terminés', apptStats.completed, 'green'],
                        ['Annulés', apptStats.cancelled, 'red'],
                        ['Absences', apptStats.noShow, 'gray'],
                      ].map(([label, val, color]) => (
                        <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: 14 }}>{label}</span>
                          <span className={`badge badge-${color}`}>{val ?? 0}</span>
                        </div>
                      ))}
                    </div>
                  ) : <div className="empty-state">Données indisponibles</div>}
                </div>

                <div className="card">
                  <h3 style={{ marginBottom: 16 }}>Facturation</h3>
                  {billingStats ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {[
                        ['Total factures', billingStats.totalFactures, 'blue'],
                        ['Payées', billingStats.payees, 'green'],
                        ['En attente', billingStats.enAttente, 'yellow'],
                      ].map(([label, val, color]) => (
                        <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: 14 }}>{label}</span>
                          <span className={`badge badge-${color}`}>{val ?? 0}</span>
                        </div>
                      ))}
                      <hr style={{ margin: '8px 0', border: 'none', borderTop: '1px solid var(--border)' }} />
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ fontSize: 14, fontWeight: 600 }}>Revenu total</span>
                        <span style={{ fontWeight: 700, color: 'var(--success)' }}>{fmtMad(billingStats.revenuTotal)}</span>
                      </div>
                    </div>
                  ) : <div className="empty-state">Données indisponibles</div>}
                </div>

                {patientStats?.parSexe && (
                  <div className="card">
                    <h3 style={{ marginBottom: 16 }}>Répartition par sexe</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {Object.entries(patientStats.parSexe).map(([sexe, count]) => (
                        <div key={sexe} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: 14 }}>{sexe === 'MASCULIN' ? 'Hommes' : 'Femmes'}</span>
                          <span className="badge badge-blue">{count}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {patientStats?.parGroupeSanguin && (
                  <div className="card">
                    <h3 style={{ marginBottom: 16 }}>Groupes sanguins</h3>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                      {Object.entries(patientStats.parGroupeSanguin).map(([gs, count]) => (
                        <div key={gs} style={{ textAlign: 'center' }}>
                          <span className="badge badge-red">{gs.replace('_', '')}</span>
                          <div style={{ fontSize: 12, marginTop: 4 }}>{count}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {tab === 'patients' && (
              <div className="list">
                {patients.length === 0 && <div className="empty-state">Aucun patient</div>}
                {patients.map(p => (
                  <div key={p.id} className="card list-item">
                    <div className="patient-avatar">{p.prenom?.[0]}{p.nom?.[0]}</div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700 }}>{p.prenom} {p.nom}</div>
                      <div style={{ fontSize: 13, color: 'var(--gray)' }}>
                        CIN: {p.cin} · {p.telephone || '—'} · {p.ville || '—'}
                      </div>
                    </div>
                    {p.groupeSanguin && <span className="badge badge-red">{p.groupeSanguin}</span>}
                  </div>
                ))}
              </div>
            )}

            {tab === 'facturation' && (
              <div className="list">
                {invoices.length === 0 && <div className="empty-state">Aucune facture</div>}
                {invoices.map(inv => (
                  <div key={inv.id} className="card list-item">
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700 }}>{inv.numeroFacture}</div>
                      <div style={{ fontSize: 13, color: 'var(--gray)' }}>
                        {inv.patientPrenom} {inv.patientNom} · {new Date(inv.dateFacture).toLocaleDateString('fr-FR')}
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                      <span style={{ fontWeight: 600 }}>{fmtMad(inv.montantTotal)}</span>
                      <span className={`badge badge-${statusColor(inv.status)}`}>{statusLabel(inv.status)}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
