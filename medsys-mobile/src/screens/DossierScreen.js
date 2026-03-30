import React, { useState, useEffect } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, ActivityIndicator, RefreshControl
} from 'react-native'
import { patientApi } from '../api/api'
import { theme } from '../theme'

const TABS = [
  { key: 'antecedents', label: '📋 Antécédents' },
  { key: 'consultations', label: '🩺 Consultations' },
  { key: 'ordonnances', label: '💊 Ordonnances' },
  { key: 'analyses', label: '🧪 Analyses' },
  { key: 'radiologies', label: '🩻 Radiologies' },
]

const ANTECEDENT_COLORS = {
  MEDICAL: '#dbeafe',
  CHIRURGICAL: '#fce7f3',
  FAMILIAL: '#d1fae5',
  ALLERGIE: '#fef3c7',
}

function Badge({ label, bg = '#dbeafe', color = '#1e40af' }) {
  return (
    <View style={{ backgroundColor: bg, borderRadius: 6, paddingHorizontal: 8, paddingVertical: 2, alignSelf: 'flex-start' }}>
      <Text style={{ fontSize: 11, fontWeight: '700', color }}>{label}</Text>
    </View>
  )
}

function EmptyTab({ icon, label }) {
  return (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyIcon}>{icon}</Text>
      <Text style={styles.emptyText}>{label}</Text>
    </View>
  )
}

function AntecedentsTab({ items }) {
  if (!items?.length) return <EmptyTab icon="📋" label="Aucun antécédent enregistré" />
  return (
    <View>
      {items.map((a, i) => (
        <View key={i} style={[styles.card, { backgroundColor: ANTECEDENT_COLORS[a.typeAntecedent] || '#f9fafb' }]}>
          <View style={styles.rowBetween}>
            <Badge label={a.typeAntecedent || '—'} />
            {a.dateAntecedent && (
              <Text style={styles.dateText}>{new Date(a.dateAntecedent).toLocaleDateString('fr-FR')}</Text>
            )}
          </View>
          {a.description && (
            <Text style={[styles.cardText, { marginTop: 6 }]}>{a.description}</Text>
          )}
        </View>
      ))}
    </View>
  )
}

function ConsultationsTab({ items }) {
  if (!items?.length) return <EmptyTab icon="🩺" label="Aucune consultation enregistrée" />
  return (
    <View>
      {items.map((c, i) => (
        <View key={i} style={styles.card}>
          <View style={styles.rowBetween}>
            <Text style={styles.cardTitle}>{c.motif || 'Consultation'}</Text>
            {c.dateConsultation && (
              <Text style={styles.dateText}>{new Date(c.dateConsultation).toLocaleDateString('fr-FR')}</Text>
            )}
          </View>
          {c.medecinNom && <Text style={styles.subText}>👨‍⚕️ {c.medecinNom}</Text>}
          {c.diagnostic && <Text style={[styles.cardText, { marginTop: 4 }]}>Diagnostic : {c.diagnostic}</Text>}
          {c.traitement && <Text style={styles.cardText}>Traitement : {c.traitement}</Text>}
          {c.notes && <Text style={[styles.cardText, { fontStyle: 'italic', color: theme.gray }]}>{c.notes}</Text>}
        </View>
      ))}
    </View>
  )
}

function OrdonnancesTab({ items }) {
  if (!items?.length) return <EmptyTab icon="💊" label="Aucune ordonnance enregistrée" />
  return (
    <View>
      {items.map((o, i) => (
        <View key={i} style={styles.card}>
          <View style={styles.rowBetween}>
            <Text style={styles.cardTitle}>{o.typeOrdonnance?.replace('_', ' ') || 'Ordonnance'}</Text>
            {o.dateOrdonnance && (
              <Text style={styles.dateText}>{new Date(o.dateOrdonnance).toLocaleDateString('fr-FR')}</Text>
            )}
          </View>
          {o.medicaments && (
            <Text style={[styles.cardText, { marginTop: 4 }]}>💊 {o.medicaments}</Text>
          )}
          {o.instructions && (
            <Text style={[styles.cardText, { fontStyle: 'italic', color: theme.gray }]}>{o.instructions}</Text>
          )}
        </View>
      ))}
    </View>
  )
}

function AnalysesTab({ items }) {
  if (!items?.length) return <EmptyTab icon="🧪" label="Aucune analyse enregistrée" />
  const statusColors = {
    EN_ATTENTE: { bg: '#fef3c7', text: '#78350f' },
    EN_COURS: { bg: '#dbeafe', text: '#1e40af' },
    TERMINE: { bg: '#d1fae5', text: '#065f46' },
  }
  return (
    <View>
      {items.map((a, i) => {
        const sc = statusColors[a.statut] || { bg: '#f3f4f6', text: '#374151' }
        return (
          <View key={i} style={styles.card}>
            <View style={styles.rowBetween}>
              <Text style={styles.cardTitle}>{a.typeAnalyse || 'Analyse'}</Text>
              <Badge label={a.statut || '—'} bg={sc.bg} color={sc.text} />
            </View>
            {a.dateAnalyse && <Text style={styles.dateText}>{new Date(a.dateAnalyse).toLocaleDateString('fr-FR')}</Text>}
            {a.laboratoire && <Text style={styles.subText}>🏥 {a.laboratoire}</Text>}
            {a.resultats && (
              <View style={{ marginTop: 6, backgroundColor: '#f8fafc', borderRadius: 8, padding: 8 }}>
                <Text style={[styles.cardText, { fontFamily: 'monospace' }]}>{a.resultats}</Text>
              </View>
            )}
          </View>
        )
      })}
    </View>
  )
}

function RadiologiesTab({ items }) {
  if (!items?.length) return <EmptyTab icon="🩻" label="Aucune radiologie enregistrée" />
  return (
    <View>
      {items.map((r, i) => (
        <View key={i} style={styles.card}>
          <View style={styles.rowBetween}>
            <Text style={styles.cardTitle}>{r.typeRadiologie || 'Radiologie'}</Text>
            {r.dateRadiologie && <Text style={styles.dateText}>{new Date(r.dateRadiologie).toLocaleDateString('fr-FR')}</Text>}
          </View>
          {r.description && <Text style={[styles.cardText, { marginTop: 4 }]}>{r.description}</Text>}
          {r.resultat && <Text style={styles.cardText}>Résultat : {r.resultat}</Text>}
        </View>
      ))}
    </View>
  )
}

export default function DossierScreen() {
  const [dossier, setDossier] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [activeTab, setActiveTab] = useState('antecedents')
  const [error, setError] = useState('')

  const load = async () => {
    try {
      const r = await patientApi.myDossier()
      setDossier(r.data)
      setError('')
    } catch {
      setError('Impossible de charger votre dossier médical.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => { load() }, [])

  const onRefresh = () => { setRefreshing(true); load() }

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={theme.primary} />
        <Text style={[styles.subText, { marginTop: 12 }]}>Chargement du dossier…</Text>
      </View>
    )
  }

  if (error) {
    return (
      <View style={styles.centered}>
        <Text style={{ fontSize: 40, marginBottom: 12 }}>⚠️</Text>
        <Text style={[styles.cardText, { color: theme.danger, textAlign: 'center' }]}>{error}</Text>
        <TouchableOpacity style={[styles.btn, { marginTop: 20 }]} onPress={load}>
          <Text style={styles.btnText}>🔄 Réessayer</Text>
        </TouchableOpacity>
      </View>
    )
  }

  const tabData = {
    antecedents: dossier?.antecedents,
    consultations: dossier?.consultations,
    ordonnances: dossier?.ordonnances,
    analyses: dossier?.analyses,
    radiologies: dossier?.radiologies,
  }

  return (
    <View style={styles.container}>
      {/* En-tête dossier */}
      {dossier && (
        <View style={styles.dossierHeader}>
          <Text style={styles.dossierTitle}>📁 Dossier n° {dossier.numeroDossier || '—'}</Text>
          <Text style={styles.dossierSub}>
            Créé le {dossier.dateCreation ? new Date(dossier.dateCreation).toLocaleDateString('fr-FR') : '—'}
          </Text>
        </View>
      )}

      {/* Onglets */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabBar}>
        {TABS.map(t => {
          const count = tabData[t.key]?.length || 0
          return (
            <TouchableOpacity
              key={t.key}
              onPress={() => setActiveTab(t.key)}
              style={[styles.tabBtn, activeTab === t.key && styles.tabBtnActive]}
            >
              <Text style={[styles.tabText, activeTab === t.key && styles.tabTextActive]}>
                {t.label}{count > 0 ? ` (${count})` : ''}
              </Text>
            </TouchableOpacity>
          )
        })}
      </ScrollView>

      {/* Contenu */}
      <ScrollView
        style={{ flex: 1 }}
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      >
        {activeTab === 'antecedents' && <AntecedentsTab items={dossier?.antecedents} />}
        {activeTab === 'consultations' && <ConsultationsTab items={dossier?.consultations} />}
        {activeTab === 'ordonnances' && <OrdonnancesTab items={dossier?.ordonnances} />}
        {activeTab === 'analyses' && <AnalysesTab items={dossier?.analyses} />}
        {activeTab === 'radiologies' && <RadiologiesTab items={dossier?.radiologies} />}
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 },
  dossierHeader: {
    backgroundColor: '#f0fdf4',
    borderBottomWidth: 1,
    borderBottomColor: '#bbf7d0',
    padding: 12,
    paddingHorizontal: 16,
  },
  dossierTitle: { fontWeight: '700', color: '#059669', fontSize: 14 },
  dossierSub: { fontSize: 12, color: theme.gray, marginTop: 2 },
  tabBar: { backgroundColor: 'white', borderBottomWidth: 1, borderBottomColor: '#e5e7eb', flexGrow: 0 },
  tabBtn: {
    paddingHorizontal: 16, paddingVertical: 10,
    borderBottomWidth: 2, borderBottomColor: 'transparent',
  },
  tabBtnActive: { borderBottomColor: theme.primary },
  tabText: { fontSize: 13, color: theme.gray, fontWeight: '500' },
  tabTextActive: { color: theme.primary, fontWeight: '700' },
  scrollContent: { padding: 16, paddingBottom: 40 },
  card: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    ...theme.shadow.sm,
  },
  cardTitle: { fontWeight: '700', fontSize: 14, color: '#1e293b' },
  cardText: { fontSize: 13, color: '#374151', marginTop: 2 },
  subText: { fontSize: 12, color: theme.gray, marginTop: 3 },
  dateText: { fontSize: 12, color: theme.gray },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  emptyContainer: { alignItems: 'center', paddingVertical: 40 },
  emptyIcon: { fontSize: 40, marginBottom: 10 },
  emptyText: { fontSize: 14, color: theme.gray },
  btn: {
    backgroundColor: theme.primary,
    borderRadius: 10,
    paddingVertical: 10,
    paddingHorizontal: 24,
  },
  btnText: { color: 'white', fontWeight: '700' },
})
