import { useState } from 'react'

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
        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px',
          background: colorMap[a.typeAntecedent] || '#f9fafb' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{a.typeAntecedent}</span>
            {a.severite && <span style={{ fontSize: 11, background: 'rgba(0,0,0,0.08)', borderRadius: 6, padding: '2px 8px' }}>{a.severite}</span>}
          </div>
          <div style={{ fontSize: 14 }}>{a.description || '—'}</div>
          {a.dateDiagnostic && <div style={{ fontSize: 12, color: 'var(--gray)', marginTop: 4 }}>
            Diagnostic : {new Date(a.dateDiagnostic).toLocaleDateString('fr-FR')}
          </div>}
        </div>
      ))}
    </div>
  )
}

function ConsultationsTimeline({ items }) {
  if (!items?.length) return <EmptyState icon="🩺" label="Aucune consultation enregistrée" />
  const sorted = [...items].sort((a, b) => new Date(b.dateConsultation) - new Date(a.dateConsultation))
  return (
    <div style={{ position: 'relative', paddingLeft: 28 }}>
      <div style={{ position: 'absolute', left: 10, top: 8, bottom: 8, width: 2, background: '#bfdbfe' }} />
      {sorted.map((c, i) => (
        <div key={i} style={{ position: 'relative', marginBottom: 20 }}>
          <div style={{ position: 'absolute', left: -24, top: 8, width: 14, height: 14, borderRadius: '50%',
            background: '#2563eb', border: '2px solid white', boxShadow: '0 0 0 2px #bfdbfe' }} />
          <div className="card" style={{ padding: '12px 16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
              <span style={{ fontWeight: 700, fontSize: 13, color: '#2563eb' }}>
                {c.dateConsultation ? new Date(c.dateConsultation).toLocaleDateString('fr-FR', { day:'2-digit', month:'long', year:'numeric' }) : '—'}
              </span>
              {c.medecinNomComplet && <span style={{ fontSize: 12, color: 'var(--gray)' }}>👨‍⚕️ {c.medecinNomComplet}</span>}
            </div>
            {c.motif      && <div style={{ fontSize: 13, marginBottom: 4 }}><strong>Motif :</strong> {c.motif}</div>}
            {c.diagnostic && <div style={{ fontSize: 13, marginBottom: 4 }}><strong>Diagnostic :</strong> {c.diagnostic}</div>}
            {c.traitement && <div style={{ fontSize: 13 }}><strong>Traitement :</strong> {c.traitement}</div>}
            {(c.poids || c.temperature || c.tensionSystolique) && (
              <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
                {c.poids && <span className="vitals-badge">⚖️ {c.poids} kg</span>}
                {c.temperature && <span className="vitals-badge">🌡️ {c.temperature}°C</span>}
                {c.tensionSystolique && <span className="vitals-badge">💉 {c.tensionSystolique}/{c.tensionDiastolique} mmHg</span>}
              </div>
            )}
          </div>
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
        <div key={i} className="card" style={{ padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>
              {o.dateOrdonnance ? new Date(o.dateOrdonnance).toLocaleDateString('fr-FR') : '—'}
            </span>
            <span className="badge badge-green">{o.typeOrdonnance}</span>
          </div>
          {o.lignes?.map((l, j) => (
            <div key={j} style={{ fontSize: 13, padding: '4px 0', borderBottom: '1px dashed var(--border)' }}>
              💊 <strong>{l.medicament}</strong>
              {l.dosage && ` — ${l.dosage}`}
              {l.dureeJours && <span style={{ color: 'var(--gray)', marginLeft: 8 }}>{l.dureeJours}j</span>}
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
  const sc = { TERMINE: '#d1fae5', EN_ATTENTE: '#fef3c7', EN_COURS: '#dbeafe', ANORMAL: '#fee2e2' }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((a, i) => (
        <div key={i} className="card" style={{ padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{a.typeAnalyse}</span>
            <span style={{ fontSize: 11, background: sc[a.statut] || '#f3f4f6', borderRadius: 6, padding: '2px 8px' }}>{a.statut}</span>
          </div>
          {a.laboratoire && <div style={{ fontSize: 12, color: 'var(--gray)' }}>🔬 {a.laboratoire}</div>}
          {a.resultats   && <div style={{ fontSize: 13, marginTop: 4 }}>{a.resultats}</div>}
        </div>
      ))}
    </div>
  )
}

function RadiologiesList({ items }) {
  if (!items?.length) return <EmptyState icon="🩻" label="Aucune radiologie enregistrée" />
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {items.map((r, i) => (
        <div key={i} className="card" style={{ padding: '12px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <span style={{ fontWeight: 700, fontSize: 13 }}>{r.typeExamen}</span>
            {r.dateExamen && <span style={{ fontSize: 12, color: 'var(--gray)' }}>
              {new Date(r.dateExamen).toLocaleDateString('fr-FR')}
            </span>}
          </div>
          {r.description && <div style={{ fontSize: 13 }}>{r.description}</div>}
          {r.conclusion  && <div style={{ fontSize: 13, fontStyle: 'italic', color: '#374151', marginTop: 4 }}>
            Conclusion : {r.conclusion}
          </div>}
        </div>
      ))}
    </div>
  )
}

export default function DossierMedical({ dossier }) {
  const [tab, setTab] = useState('antecedents')
  if (!dossier) return <div className="alert alert-warning">Dossier médical non disponible.</div>

  const tabs = [
    { key: 'antecedents',   label: '📋 Antécédents',   count: dossier.antecedents?.length || 0 },
    { key: 'consultations', label: '🩺 Consultations',  count: dossier.consultations?.length || 0 },
    { key: 'ordonnances',   label: '💊 Ordonnances',    count: dossier.ordonnances?.length || 0 },
    { key: 'analyses',      label: '🧪 Analyses',       count: dossier.analyses?.length || 0 },
    { key: 'radiologies',   label: '🩻 Radiologies',    count: dossier.radiologies?.length || 0 },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, padding: '12px 16px', background: '#f0fdf4',
        border: '1px solid #bbf7d0', borderRadius: 10 }}>
        <span style={{ fontWeight: 700, color: '#059669' }}>📁 Dossier n° {dossier.numeroDossier}</span>
        <span style={{ fontSize: 12, color: 'var(--gray)', marginLeft: 12 }}>
          Créé le {dossier.dateCreation ? new Date(dossier.dateCreation).toLocaleDateString('fr-FR') : '—'}
        </span>
      </div>

      <div style={{ display: 'flex', gap: 6, marginBottom: 16, flexWrap: 'wrap' }}>
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)} className={`tab-btn ${tab === t.key ? 'active' : ''}`}>
            {t.label}
            {t.count > 0 && <span className="tab-count">{t.count}</span>}
          </button>
        ))}
      </div>

      <div className="card" style={{ padding: 16 }}>
        {tab === 'antecedents'   && <AntecedentsList    items={dossier.antecedents} />}
        {tab === 'consultations' && <ConsultationsTimeline items={dossier.consultations} />}
        {tab === 'ordonnances'   && <OrdonnancesList    items={dossier.ordonnances} />}
        {tab === 'analyses'      && <AnalysesList       items={dossier.analyses} />}
        {tab === 'radiologies'   && <RadiologiesList    items={dossier.radiologies} />}
      </div>
    </div>
  )
}
