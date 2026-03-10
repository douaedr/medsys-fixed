import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { patientApi, adminApi } from '../api/api'

// ── Navbar partagée ─────────────────────────────────────────────────────────
function Navbar({ role }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const handleLogout = () => { logout(); navigate('/') }
  return (
    <nav className="navbar">
      <a className="navbar-brand" href="/">
        <div className="navbar-logo-icon">🏥</div>
        <div>
          <div className="navbar-logo-text">MedSys</div>
          <div className="navbar-logo-sub">{role}</div>
        </div>
      </a>
      <div className="navbar-right">
        <div className="user-chip">
          <div className="user-chip-avatar" style={{ background: role === 'Espace Patient' ? '#059669' : '#2563eb' }}>
            {user?.prenom?.[0]}{user?.nom?.[0]}
          </div>
          <span className="user-chip-name">{user?.prenom} {user?.nom}</span>
        </div>
        <button className="btn btn-outline" onClick={handleLogout} style={{ fontSize: 12, padding: '6px 14px' }}>
          🚪 Déconnexion
        </button>
      </div>
    </nav>
  )
}

// ── Composant dossier médical ─────────────────────────────────────────────
function DossierMedical({ dossier }) {
  const [tab, setTab] = useState('antecedents')
  if (!dossier) return <div className="alert alert-warning">Dossier médical non disponible.</div>
  const tabs = [
    { key: 'antecedents', label: '📋 Antécédents', count: dossier.antecedents?.length || 0 },
    { key: 'consultations', label: '🩺 Consultations', count: dossier.consultations?.length || 0 },
    { key: 'ordonnances', label: '💊 Ordonnances', count: dossier.ordonnances?.length || 0 },
    { key: 'analyses', label: '🧪 Analyses', count: dossier.analyses?.length || 0 },
    { key: 'radiologies', label: '📡 Radiologies', count: dossier.radiologies?.length || 0 },
  ]
  return (
    <div>
      <div className="card" style={{ marginBottom: 16, padding: '12px 16px', background: '#f0fdf4', border: '1px solid #bbf7d0' }}>
        <span style={{ fontFamily: 'Syne', fontWeight: 700, color: '#059669' }}>📁 Dossier n° {dossier.numeroDossier}</span>
        <span style={{ fontSize: 12, color: 'var(--gray)', marginLeft: 12 }}>
          Créé le {dossier.dateCreation ? new Date(dossier.dateCreation).toLocaleDateString('fr-FR') : '—'}
        </span>
      </div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 16, flexWrap: 'wrap' }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)} style={{
            padding: '6px 14px', border: 'none', borderRadius: 20, cursor: 'pointer', fontSize: 13,
            background: tab === t.key ? '#2563eb' : '#f3f4f6',
            color: tab === t.key ? 'white' : '#374151', fontWeight: tab === t.key ? 700 : 400
          }}>
            {t.label}{t.count > 0 && <span style={{ background: 'rgba(0,0,0,0.12)', borderRadius: 10, padding: '0 6px', marginLeft: 4, fontSize: 11 }}>{t.count}</span>}
          </button>
        ))}
      </div>
      <div className="card" style={{ padding: 16 }}>
        {tab === 'antecedents' && <AntecedentsList items={dossier.antecedents} />}
        {tab === 'consultations' && <ConsultationsList items={dossier.consultations} />}
        {tab === 'ordonnances' && <OrdonnancesList items={dossier.ordonnances} />}
        {tab === 'analyses' && <AnalysesList items={dossier.analyses} />}
        {tab === 'radiologies' && <RadiologiesList items={dossier.radiologies} />}
      </div>
    </div>
  )
}

function EmptyState({ icon, label }) {
  return (
    <div style={{ textAlign: 'center', padding: '32px 0', color: 'var(--gray)' }}>
      <div style={{ fontSize: 36, marginBottom: 8 }}>{icon}</div>
      <div style={{ fontSize: 14 }}>{label}</div>
    </div>
  )
}

function AntecedentsList({ items }) {
  if (!items?.length) return <EmptyState icon="📋" label="Aucun antécédent enregistré" />
  const colorMap = { MEDICAL: '#dbeafe', CHIRURGICAL: '#fce7f3', FAMILIAL: '#d1fae5', ALLERGIE: '#fef3c7' }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((a, i) => (
        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px', background: colorMap[a.typeAntecedent] || '#f9fafb' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{a.typeAntecedent}</span>
            {a.severite && <span style={{ fontSize: 11, background: 'rgba(0,0,0,0.08)', borderRadius: 6, padding: '2px 8px' }}>{a.severite}</span>}
          </div>
          <div style={{ fontSize: 14 }}>{a.description || '—'}</div>
          {a.dateDiagnostic && <div style={{ fontSize: 12, color: 'var(--gray)', marginTop: 4 }}>Diagnostic : {new Date(a.dateDiagnostic).toLocaleDateString('fr-FR')}</div>}
        </div>
      ))}
    </div>
  )
}

function ConsultationsList({ items }) {
  if (!items?.length) return <EmptyState icon="🩺" label="Aucune consultation enregistrée" />
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((c, i) => (
        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{c.dateConsultation ? new Date(c.dateConsultation).toLocaleDateString('fr-FR') : '—'}</span>
            {c.medecinNomComplet && <span style={{ fontSize: 12, color: 'var(--gray)' }}>{c.medecinNomComplet}</span>}
          </div>
          {c.motif && <div style={{ fontSize: 13, marginBottom: 4 }}><strong>Motif :</strong> {c.motif}</div>}
          {c.diagnostic && <div style={{ fontSize: 13, marginBottom: 4 }}><strong>Diagnostic :</strong> {c.diagnostic}</div>}
          {c.traitement && <div style={{ fontSize: 13 }}><strong>Traitement :</strong> {c.traitement}</div>}
          {(c.poids || c.temperature) && (
            <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
              {c.poids && <span style={{ fontSize: 11, background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: 6, padding: '2px 8px' }}>⚖️ {c.poids} kg</span>}
              {c.temperature && <span style={{ fontSize: 11, background: '#fff7ed', border: '1px solid #fed7aa', borderRadius: 6, padding: '2px 8px' }}>🌡️ {c.temperature}°C</span>}
              {c.tensionSystolique && <span style={{ fontSize: 11, background: '#fdf4ff', border: '1px solid #e9d5ff', borderRadius: 6, padding: '2px 8px' }}>💉 {c.tensionSystolique}/{c.tensionDiastolique} mmHg</span>}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

function OrdonnancesList({ items }) {
  if (!items?.length) return <EmptyState icon="💊" label="Aucune ordonnance enregistrée" />
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((o, i) => (
        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{o.dateOrdonnance ? new Date(o.dateOrdonnance).toLocaleDateString('fr-FR') : '—'}</span>
            <span style={{ fontSize: 11, background: '#d1fae5', color: '#065f46', borderRadius: 6, padding: '2px 8px' }}>{o.typeOrdonnance}</span>
          </div>
          {o.lignes?.map((l, j) => (
            <div key={j} style={{ fontSize: 13, padding: '4px 0', borderBottom: '1px dashed var(--border)' }}>
              💊 <strong>{l.medicament}</strong>{l.dosage ? ` — ${l.dosage}` : ''}{l.dureeJours && <span style={{ color: 'var(--gray)', marginLeft: 8 }}>{l.dureeJours}j</span>}
            </div>
          ))}
          {o.instructions && <div style={{ fontSize: 12, color: 'var(--gray)', marginTop: 6 }}>ℹ️ {o.instructions}</div>}
        </div>
      ))}
    </div>
  )
}

function AnalysesList({ items }) {
  if (!items?.length) return <EmptyState icon="🧪" label="Aucune analyse enregistrée" />
  const sc = { TERMINE: '#d1fae5', EN_ATTENTE: '#fef3c7', EN_COURS: '#dbeafe' }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((a, i) => (
        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{a.typeAnalyse}</span>
            <span style={{ fontSize: 11, background: sc[a.statut] || '#f3f4f6', borderRadius: 6, padding: '2px 8px' }}>{a.statut}</span>
          </div>
          {a.laboratoire && <div style={{ fontSize: 12, color: 'var(--gray)' }}>🔬 {a.laboratoire}</div>}
          {a.resultats && <div style={{ fontSize: 13, marginTop: 4 }}>{a.resultats}</div>}
          {a.dateAnalyse && <div style={{ fontSize: 12, color: 'var(--gray)', marginTop: 4 }}>{new Date(a.dateAnalyse).toLocaleDateString('fr-FR')}</div>}
        </div>
      ))}
    </div>
  )
}

function RadiologiesList({ items }) {
  if (!items?.length) return <EmptyState icon="📡" label="Aucune radiologie enregistrée" />
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((r, i) => (
        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{r.typeExamen}</span>
            {r.dateExamen && <span style={{ fontSize: 12, color: 'var(--gray)' }}>{new Date(r.dateExamen).toLocaleDateString('fr-FR')}</span>}
          </div>
          {r.description && <div style={{ fontSize: 13 }}>{r.description}</div>}
          {r.conclusion && <div style={{ fontSize: 13, fontStyle: 'italic', color: '#374151', marginTop: 4 }}>Conclusion : {r.conclusion}</div>}
        </div>
      ))}
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// PATIENT DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════
export function PatientDashboard() {
  const { user } = useAuth()
  const [profil, setProfil] = useState(null)
  const [dossier, setDossier] = useState(null)
  const [view, setView] = useState('accueil')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    patientApi.me()
      .then(r => setProfil(r.data))
      .catch(() => setError('Impossible de charger votre profil.'))
      .finally(() => setLoading(false))
  }, [])

  const loadDossier = async () => {
    if (dossier) { setView('dossier'); return }
    setLoading(true)
    try {
      const r = await patientApi.myDossier()
      setDossier(r.data)
      setView('dossier')
    } catch {
      setError('Impossible de charger le dossier médical.')
    } finally { setLoading(false) }
  }

  return (
    <div>
      <Navbar role="Espace Patient" />
      <div className="container">
        <div style={{ padding: '40px 0 20px' }}>
          <h1 style={{ fontFamily: 'Syne', fontSize: 28, fontWeight: 800 }}>Bonjour, {user?.prenom} 👋</h1>
          <p style={{ color: 'var(--gray)', marginTop: 4 }}>Bienvenue sur votre espace santé personnel</p>
        </div>
        {error && <div className="alert alert-error">⚠️ {error}</div>}
        {loading && <div style={{ textAlign: 'center', padding: 20 }}><span className="spinner" /></div>}
        <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
          {[{ key: 'accueil', label: '🏠 Accueil' }, { key: 'dossier', label: '📁 Mon dossier médical' }].map(t => (
            <button key={t.key} onClick={() => t.key === 'dossier' ? loadDossier() : setView('accueil')}
              style={{ padding: '8px 20px', border: 'none', borderRadius: 20, cursor: 'pointer', fontSize: 14,
                background: view === t.key ? '#2563eb' : '#f3f4f6', color: view === t.key ? 'white' : '#374151',
                fontWeight: view === t.key ? 700 : 400 }}>
              {t.label}
            </button>
          ))}
        </div>
        {view === 'accueil' && (
          <>
            {profil && (
              <div className="card" style={{ padding: 20, marginBottom: 20 }}>
                <div style={{ fontFamily: 'Syne', fontWeight: 700, marginBottom: 12 }}>👤 Mes informations</div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 8 }}>
                  {[
                    ['Nom', profil.nom], ['Prénom', profil.prenom], ['CIN', profil.cin],
                    ['Date de naissance', profil.dateNaissance ? new Date(profil.dateNaissance).toLocaleDateString('fr-FR') : '—'],
                    ['Sexe', profil.sexe], ['Groupe sanguin', profil.groupeSanguin?.replace('_', ' ')],
                    ['Téléphone', profil.telephone || '—'], ['Email', profil.email || '—'],
                    ['Ville', profil.ville || '—'], ['Mutuelle', profil.mutuelle || '—'],
                    ['N° Dossier', profil.numeroDossier || '—'],
                  ].map(([k, v]) => (
                    <div key={k} style={{ padding: '8px 0', borderBottom: '1px solid var(--border)', fontSize: 13 }}>
                      <div style={{ color: 'var(--gray)', fontSize: 11, marginBottom: 2 }}>{k}</div>
                      <div style={{ fontWeight: 600 }}>{v || '—'}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
              {[
                { icon: '📋', title: 'Mon dossier', sub: 'Antécédents, consultations', color: '#dbeafe', action: loadDossier },
                { icon: '🧪', title: 'Mes analyses', sub: 'Résultats de laboratoire', color: '#fef3c7', action: loadDossier },
                { icon: '💊', title: 'Mes ordonnances', sub: 'Traitements prescrits', color: '#d1fae5', action: loadDossier },
              ].map((item, i) => (
                <div key={i} className="card" style={{ padding: 20, cursor: 'pointer' }} onClick={item.action}>
                  <div style={{ width: 48, height: 48, borderRadius: 12, background: item.color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, marginBottom: 12 }}>{item.icon}</div>
                  <div style={{ fontFamily: 'Syne', fontWeight: 700, marginBottom: 4 }}>{item.title}</div>
                  <div style={{ color: 'var(--gray)', fontSize: 13 }}>{item.sub}</div>
                </div>
              ))}
            </div>
          </>
        )}
        {view === 'dossier' && <DossierMedical dossier={dossier} />}
        <div style={{ height: 40 }} />
      </div>
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// PERSONNEL DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════
export function PersonnelDashboard() {
  const { user } = useAuth()
  const [view, setView] = useState('liste')
  const [patients, setPatients] = useState([])
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [total, setTotal] = useState(0)
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [dossier, setDossier] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const loadPatients = async (p = 0, q = '') => {
    setLoading(true); setError('')
    try {
      const res = q.trim() ? await patientApi.search(q, { page: p, size: 10 }) : await patientApi.getAll({ page: p, size: 10 })
      setPatients(res.data.content || [])
      setTotalPages(res.data.totalPages || 0)
      setTotal(res.data.totalElements || 0)
      setPage(p)
    } catch { setError('Impossible de charger la liste des patients.') }
    finally { setLoading(false) }
  }

  useEffect(() => { loadPatients(0, '') }, [])

  const openDossier = async (patient) => {
    setSelectedPatient(patient); setDossier(null); setView('dossier'); setLoading(true)
    try { const r = await patientApi.dossier(patient.id); setDossier(r.data) }
    catch { setError('Impossible de charger le dossier médical.') }
    finally { setLoading(false) }
  }

  return (
    <div>
      <Navbar role="Espace Personnel" />
      <div className="container">
        <div style={{ padding: '40px 0 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <div>
            <h1 style={{ fontFamily: 'Syne', fontSize: 28, fontWeight: 800 }}>
              Bonjour, {user?.role === 'MEDECIN' ? 'Dr.' : ''} {user?.prenom} 👨‍⚕️
            </h1>
            <p style={{ color: 'var(--gray)', marginTop: 4 }}>Tableau de bord — {user?.role}</p>
          </div>
          {view === 'dossier' && (
            <button className="btn btn-outline" onClick={() => { setView('liste'); setSelectedPatient(null) }}>← Retour</button>
          )}
        </div>
        {error && <div className="alert alert-error">⚠️ {error}</div>}
        {view === 'liste' && (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 20 }}>
              {[
                { icon: '👥', label: 'Patients total', value: total, color: '#dbeafe' },
                { icon: '👨‍⚕️', label: 'Votre rôle', value: user?.role, color: '#d1fae5' },
                { icon: '📅', label: 'Date', value: new Date().toLocaleDateString('fr-FR'), color: '#fef3c7' },
              ].map((s, i) => (
                <div key={i} className="card" style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 44, height: 44, borderRadius: 12, background: s.color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>{s.icon}</div>
                  <div>
                    <div style={{ fontSize: 11, color: 'var(--gray)' }}>{s.label}</div>
                    <div style={{ fontFamily: 'Syne', fontWeight: 700 }}>{s.value}</div>
                  </div>
                </div>
              ))}
            </div>
            <form onSubmit={e => { e.preventDefault(); loadPatients(0, search) }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
              <input className="form-input" value={search} onChange={e => setSearch(e.target.value)} placeholder="Rechercher par nom, prénom ou CIN..." style={{ flex: 1 }} />
              <button type="submit" className="btn btn-primary">🔍 Rechercher</button>
              {search && <button type="button" className="btn btn-outline" onClick={() => { setSearch(''); loadPatients(0, '') }}>✕</button>}
            </form>
            {loading ? <div style={{ textAlign: 'center', padding: 40 }}><span className="spinner" /></div>
            : patients.length === 0 ? (
              <div className="card" style={{ padding: 40, textAlign: 'center', color: 'var(--gray)' }}>
                <div style={{ fontSize: 36, marginBottom: 8 }}>👥</div><div>Aucun patient trouvé</div>
              </div>
            ) : (
              <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                  <thead>
                    <tr style={{ background: '#f8fafc', borderBottom: '2px solid var(--border)' }}>
                      {['Nom complet', 'CIN', 'Date naissance', 'Téléphone', 'Ville', 'N° Dossier', ''].map(h => (
                        <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, fontSize: 12, color: 'var(--gray)' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {patients.map((p, i) => (
                      <tr key={p.id} style={{ borderBottom: '1px solid var(--border)', background: i % 2 === 0 ? 'white' : '#fafafa' }}>
                        <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div style={{ width: 32, height: 32, borderRadius: '50%', background: '#dbeafe', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700 }}>
                              {p.prenom?.[0]}{p.nom?.[0]}
                            </div>
                            {p.prenom} {p.nom}
                          </div>
                        </td>
                        <td style={{ padding: '12px 16px' }}>{p.cin}</td>
                        <td style={{ padding: '12px 16px' }}>{p.dateNaissance ? new Date(p.dateNaissance).toLocaleDateString('fr-FR') : '—'}</td>
                        <td style={{ padding: '12px 16px' }}>{p.telephone || '—'}</td>
                        <td style={{ padding: '12px 16px' }}>{p.ville || '—'}</td>
                        <td style={{ padding: '12px 16px' }}>
                          <span style={{ background: '#d1fae5', color: '#065f46', fontSize: 11, borderRadius: 6, padding: '2px 8px', fontWeight: 600 }}>{p.numeroDossier || '—'}</span>
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <button className="btn btn-primary" onClick={() => openDossier(p)} style={{ padding: '4px 12px', fontSize: 12 }}>📁 Dossier</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {totalPages > 1 && (
              <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
                <button className="btn btn-outline" disabled={page === 0} onClick={() => loadPatients(page - 1, search)} style={{ padding: '6px 14px', fontSize: 13 }}>← Préc.</button>
                <span style={{ padding: '6px 14px', fontSize: 13, color: 'var(--gray)' }}>Page {page + 1} / {totalPages}</span>
                <button className="btn btn-outline" disabled={page >= totalPages - 1} onClick={() => loadPatients(page + 1, search)} style={{ padding: '6px 14px', fontSize: 13 }}>Suiv. →</button>
              </div>
            )}
          </>
        )}
        {view === 'dossier' && selectedPatient && (
          <>
            <div className="card" style={{ padding: 20, marginBottom: 20, background: '#f0f9ff', border: '1px solid #bae6fd' }}>
              <div style={{ fontFamily: 'Syne', fontWeight: 700, fontSize: 18, marginBottom: 8 }}>👤 {selectedPatient.prenom} {selectedPatient.nom}</div>
              <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', fontSize: 13 }}>
                {[['CIN', selectedPatient.cin], ['Né(e) le', selectedPatient.dateNaissance ? new Date(selectedPatient.dateNaissance).toLocaleDateString('fr-FR') : '—'],
                  ['Sexe', selectedPatient.sexe], ['Groupe sanguin', selectedPatient.groupeSanguin?.replace('_', ' ')],
                  ['Téléphone', selectedPatient.telephone || '—'], ['Mutuelle', selectedPatient.mutuelle || '—']
                ].map(([k, v]) => (
                  <div key={k}><span style={{ color: 'var(--gray)' }}>{k} : </span><strong>{v}</strong></div>
                ))}
              </div>
            </div>
            {loading ? <div style={{ textAlign: 'center', padding: 40 }}><span className="spinner" /></div> : <DossierMedical dossier={dossier} />}
          </>
        )}
        <div style={{ height: 40 }} />
      </div>
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// ADMIN DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════
export function AdminDashboard() {
  const { user } = useAuth()
  const [view, setView] = useState('users')
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [form, setForm] = useState({ email: '', password: '', nom: '', prenom: '', cin: '', role: 'MEDECIN' })

  const loadUsers = async () => {
    setLoading(true)
    try { const r = await adminApi.listUsers(); setUsers(r.data) }
    catch { setError('Impossible de charger la liste des utilisateurs.') }
    finally { setLoading(false) }
  }

  useEffect(() => { loadUsers() }, [])

  const handleToggle = async (id) => {
    try { await adminApi.toggleUser(id); setSuccess('Statut mis à jour.'); loadUsers(); setTimeout(() => setSuccess(''), 3000) }
    catch { setError('Erreur lors de la mise à jour.') }
  }

  const handleCreate = async (e) => {
    e.preventDefault(); setLoading(true); setError(''); setSuccess('')
    try {
      await adminApi.createPersonnel(form)
      setSuccess(`✅ Compte ${form.role} créé pour ${form.prenom} ${form.nom}`)
      setForm({ email: '', password: '', nom: '', prenom: '', cin: '', role: 'MEDECIN' })
      loadUsers(); setView('users')
    } catch (err) { setError(err.response?.data?.message || 'Erreur lors de la création du compte') }
    finally { setLoading(false) }
  }

  const roleColor = { ADMIN: '#fee2e2', MEDECIN: '#dbeafe', PERSONNEL: '#d1fae5', PATIENT: '#f3f4f6' }
  const roleTextColor = { ADMIN: '#991b1b', MEDECIN: '#1e40af', PERSONNEL: '#065f46', PATIENT: '#374151' }

  return (
    <div>
      <Navbar role="Administration" />
      <div className="container">
        <div style={{ padding: '40px 0 20px' }}>
          <h1 style={{ fontFamily: 'Syne', fontSize: 28, fontWeight: 800 }}>⚙️ Administration</h1>
          <p style={{ color: 'var(--gray)', marginTop: 4 }}>Gestion des comptes utilisateurs</p>
        </div>
        {error && <div className="alert alert-error">⚠️ {error}</div>}
        {success && <div className="alert alert-success">{success}</div>}
        <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
          {[{ key: 'users', label: '👥 Utilisateurs' }, { key: 'creer', label: '➕ Créer un compte' }].map(t => (
            <button key={t.key} onClick={() => { setView(t.key); setError(''); setSuccess('') }}
              style={{ padding: '8px 20px', border: 'none', borderRadius: 20, cursor: 'pointer', fontSize: 14,
                background: view === t.key ? '#2563eb' : '#f3f4f6', color: view === t.key ? 'white' : '#374151',
                fontWeight: view === t.key ? 700 : 400 }}>
              {t.label}
            </button>
          ))}
        </div>
        {view === 'users' && (
          loading ? <div style={{ textAlign: 'center', padding: 40 }}><span className="spinner" /></div> : (
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ background: '#f8fafc', borderBottom: '2px solid var(--border)' }}>
                    {['Nom', 'Email', 'CIN', 'Rôle', 'Statut', 'Actions'].map(h => (
                      <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, fontSize: 12, color: 'var(--gray)' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {users.map((u, i) => (
                    <tr key={u.id} style={{ borderBottom: '1px solid var(--border)', background: i % 2 === 0 ? 'white' : '#fafafa' }}>
                      <td style={{ padding: '12px 16px', fontWeight: 600 }}>{u.prenom} {u.nom}</td>
                      <td style={{ padding: '12px 16px' }}>{u.email}</td>
                      <td style={{ padding: '12px 16px' }}>{u.cin || '—'}</td>
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{ background: roleColor[u.role] || '#f3f4f6', color: roleTextColor[u.role] || '#374151', borderRadius: 6, padding: '2px 10px', fontSize: 11, fontWeight: 700 }}>{u.role}</span>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span style={{ background: u.enabled ? '#d1fae5' : '#fee2e2', color: u.enabled ? '#065f46' : '#991b1b', borderRadius: 6, padding: '2px 8px', fontSize: 11, fontWeight: 600 }}>
                          {u.enabled ? '✓ Actif' : '✗ Inactif'}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        {u.id !== user?.userId && (
                          <button onClick={() => handleToggle(u.id)}
                            style={{ background: 'none', border: '1px solid var(--border)', borderRadius: 6, padding: '4px 10px', cursor: 'pointer', fontSize: 12 }}>
                            {u.enabled ? '🔒 Désactiver' : '🔓 Activer'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
        {view === 'creer' && (
          <div className="card" style={{ maxWidth: 520, padding: 28 }}>
            <h3 style={{ fontFamily: 'Syne', fontWeight: 700, marginBottom: 20 }}>➕ Nouveau compte</h3>
            <form onSubmit={handleCreate}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                <div className="form-group">
                  <label className="form-label">Nom *</label>
                  <input className="form-input" value={form.nom} onChange={e => setForm({...form, nom: e.target.value})} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Prénom *</label>
                  <input className="form-input" value={form.prenom} onChange={e => setForm({...form, prenom: e.target.value})} required />
                </div>
                <div className="form-group">
                  <label className="form-label">CIN</label>
                  <input className="form-input" value={form.cin} onChange={e => setForm({...form, cin: e.target.value.toUpperCase()})} />
                </div>
                <div className="form-group">
                  <label className="form-label">Rôle *</label>
                  <select className="form-select" value={form.role} onChange={e => setForm({...form, role: e.target.value})}>
                    <option value="MEDECIN">Médecin</option>
                    <option value="PERSONNEL">Personnel</option>
                    <option value="ADMIN">Administrateur</option>
                  </select>
                </div>
                <div className="form-group" style={{ gridColumn: '1/-1' }}>
                  <label className="form-label">Email *</label>
                  <input className="form-input" type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} required />
                </div>
                <div className="form-group" style={{ gridColumn: '1/-1' }}>
                  <label className="form-label">Mot de passe temporaire * (min. 8 car.)</label>
                  <input className="form-input" type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required minLength={8} />
                </div>
              </div>
              <div className="alert alert-warning" style={{ marginBottom: 16, fontSize: 12 }}>
                ℹ️ Un email avec les identifiants sera envoyé automatiquement.
              </div>
              <button className="btn btn-primary btn-full" type="submit" disabled={loading}>
                {loading ? <span className="spinner" /> : '✅ Créer le compte'}
              </button>
            </form>
          </div>
        )}
        <div style={{ height: 40 }} />
      </div>
    </div>
  )
}
