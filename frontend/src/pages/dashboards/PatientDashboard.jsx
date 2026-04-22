import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import Navbar from '../../components/Navbar'
import DossierMedical from '../../components/DossierMedical'
import { medicalRecordApi } from '../../api/patients'
import { appointmentApi } from '../../api/appointments'
import { billingApi } from '../../api/appointments'

export default function PatientDashboard() {
  const { user } = useAuth()
  const [tab, setTab]               = useState('dossier')
  const [dossier, setDossier]       = useState(null)
  const [rdvs, setRdvs]             = useState([])
  const [factures, setFactures]     = useState([])
  const [messages, setMessages]     = useState([])
  const [loading, setLoading]       = useState(true)
  const [unreadCount, setUnreadCount] = useState(0)

  const patientId = user?.patientId

  useEffect(() => {
    if (!patientId) return
    const load = async () => {
      setLoading(true)
      try {
        const [d, r, m, f] = await Promise.allSettled([
          medicalRecordApi.getDossier(patientId),
          appointmentApi.getByPatient(patientId),
          medicalRecordApi.getMessages(patientId),
          billingApi.getByPatient(patientId),
        ])
        if (d.status === 'fulfilled') setDossier(d.value.data)
        if (r.status === 'fulfilled') setRdvs(r.value.data)
        if (m.status === 'fulfilled') {
          const msgs = m.value.data
          setMessages(msgs)
          setUnreadCount(msgs.filter(x => !x.lu && x.expediteur !== 'PATIENT').length)
        }
        if (f.status === 'fulfilled') setFactures(f.value.data)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [patientId])

  const handleCancelRdv = async (id) => {
    if (!confirm('Annuler ce rendez-vous ?')) return
    await appointmentApi.cancel(id, 'Annulé par le patient')
    setRdvs(prev => prev.map(r => r.id === id ? { ...r, status: 'CANCELLED' } : r))
  }

  if (!patientId) return (
    <div>
      <Navbar />
      <div style={{ padding: 32 }}>
        <div className="alert alert-warning">
          Votre compte patient n'est pas encore lié. Contactez l'administration.
        </div>
      </div>
    </div>
  )

  const tabs = [
    { key: 'dossier',    label: '📁 Dossier médical' },
    { key: 'rdv',        label: `📅 Rendez-vous (${rdvs.length})` },
    { key: 'messages',   label: `💬 Messages${unreadCount > 0 ? ` (${unreadCount})` : ''}` },
    { key: 'factures',   label: `💶 Factures (${factures.length})` },
  ]

  return (
    <div>
      <Navbar notifCount={unreadCount} />
      <div className="container">
        <div className="page-header">
          <h1>Bonjour, {user.prenom} 👋</h1>
          <p>Votre espace santé personnel</p>
        </div>

        <div className="tabs">
          {tabs.map(t => (
            <button key={t.key} onClick={() => setTab(t.key)}
                    className={`tab-btn ${tab === t.key ? 'active' : ''}`}>
              {t.label}
            </button>
          ))}
        </div>

        {loading ? <div className="loading">Chargement...</div> : (
          <>
            {tab === 'dossier' && <DossierMedical dossier={dossier} />}

            {tab === 'rdv' && (
              <div>
                {!rdvs.length ? (
                  <div className="empty-state">📅 Aucun rendez-vous</div>
                ) : (
                  <div className="list">
                    {rdvs.map(r => (
                      <div key={r.id} className="card list-item">
                        <div className="list-item-main">
                          <span className="list-item-title">
                            Dr {r.medecinPrenom} {r.medecinNom}
                          </span>
                          <span style={{ fontSize: 13, color: 'var(--gray)' }}>
                            {new Date(r.dateHeure).toLocaleString('fr-FR', { dateStyle: 'long', timeStyle: 'short' })}
                          </span>
                          {r.motif && <span style={{ fontSize: 13 }}>{r.motif}</span>}
                        </div>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                          <span className={`badge badge-${statusColor(r.status)}`}>{statusLabel(r.status)}</span>
                          {['PENDING', 'CONFIRMED'].includes(r.status) && (
                            <button className="btn btn-sm btn-danger" onClick={() => handleCancelRdv(r.id)}>
                              Annuler
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {tab === 'messages' && (
              <div>
                {!messages.length ? (
                  <div className="empty-state">💬 Aucun message</div>
                ) : (
                  <div className="list">
                    {messages.map(m => (
                      <div key={m.id} className={`card list-item ${!m.lu && m.expediteur !== 'PATIENT' ? 'unread' : ''}`}>
                        <div>
                          <div style={{ fontWeight: m.lu ? 400 : 700, fontSize: 13 }}>
                            {m.expediteur === 'PATIENT' ? '📤 Vous' : `📥 Dr ${m.medecinNom}`}
                          </div>
                          <div style={{ fontSize: 14, marginTop: 4 }}>{m.contenu}</div>
                          <div style={{ fontSize: 12, color: 'var(--gray)', marginTop: 4 }}>
                            {new Date(m.envoyeAt).toLocaleString('fr-FR')}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {tab === 'factures' && (
              <div>
                {!factures.length ? (
                  <div className="empty-state">💶 Aucune facture</div>
                ) : (
                  <div className="list">
                    {factures.map(f => (
                      <div key={f.id} className="card list-item">
                        <div>
                          <div style={{ fontWeight: 700 }}>{f.numeroFacture}</div>
                          <div style={{ fontSize: 13, color: 'var(--gray)' }}>
                            {f.dateFacture ? new Date(f.dateFacture).toLocaleDateString('fr-FR') : '—'}
                          </div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <div style={{ fontWeight: 700, fontSize: 16 }}>{f.montantTotal} MAD</div>
                          <span className={`badge badge-${invoiceColor(f.status)}`}>{invoiceLabel(f.status)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
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
function invoiceLabel(s) {
  return { PENDING: 'En attente', PAID: 'Payée', PARTIALLY_PAID: 'Part. payée',
           CANCELLED: 'Annulée', DRAFT: 'Brouillon' }[s] || s
}
function invoiceColor(s) {
  return { PENDING: 'yellow', PAID: 'green', PARTIALLY_PAID: 'blue',
           CANCELLED: 'red', DRAFT: 'gray' }[s] || 'gray'
}
