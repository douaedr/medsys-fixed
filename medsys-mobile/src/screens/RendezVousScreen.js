import React, { useState, useEffect } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity, TextInput,
  StyleSheet, ActivityIndicator, Alert, RefreshControl, Modal
} from 'react-native'
import { patientApi } from '../api/api'
import { theme } from '../theme'

const STATUT_STYLE = {
  PLANIFIE:  { bg: '#fef3c7', text: '#78350f' },
  CONFIRME:  { bg: '#d1fae5', text: '#065f46' },
  ANNULE:    { bg: '#fee2e2', text: '#991b1b' },
  COMPLETE:  { bg: '#f3f4f6', text: '#374151' },
  REPORTE:   { bg: '#ede9fe', text: '#5b21b6' },
}

function RdvCard({ rdv, onAnnuler }) {
  const sc = STATUT_STYLE[rdv.statut] || { bg: '#f3f4f6', text: '#374151' }
  const date = rdv.dateHeure ? new Date(rdv.dateHeure) : null

  return (
    <View style={styles.card}>
      <View style={styles.cardRow}>
        {/* Bloc date */}
        <View style={styles.dateBlock}>
          <Text style={styles.dateDay}>{date ? date.getDate().toString().padStart(2, '0') : '—'}</Text>
          <Text style={styles.dateMonth}>
            {date ? date.toLocaleDateString('fr-FR', { month: 'short' }) : ''}
          </Text>
          {date && <Text style={styles.dateTime}>{date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}</Text>}
        </View>

        {/* Infos */}
        <View style={styles.cardInfo}>
          <Text style={styles.cardTitle}>{rdv.motif || 'Consultation'}</Text>
          {rdv.medecinNom && <Text style={styles.subText}>👨‍⚕️ {rdv.medecinNom}</Text>}
          {rdv.service && <Text style={styles.subText}>🏥 {rdv.service}</Text>}
          {rdv.lieu && <Text style={styles.subText}>📍 {rdv.lieu}</Text>}
          {rdv.notes && <Text style={[styles.subText, { fontStyle: 'italic' }]}>{rdv.notes}</Text>}
        </View>

        {/* Statut */}
        <View style={[styles.badge, { backgroundColor: sc.bg }]}>
          <Text style={[styles.badgeText, { color: sc.text }]}>{rdv.statut}</Text>
        </View>
      </View>

      {rdv.statut !== 'ANNULE' && rdv.statut !== 'COMPLETE' && (
        <TouchableOpacity style={styles.annulerBtn} onPress={() => onAnnuler(rdv.id)}>
          <Text style={styles.annulerText}>Annuler ce RDV</Text>
        </TouchableOpacity>
      )}
    </View>
  )
}

function NouveauRdvModal({ visible, onClose, onSuccess }) {
  const [form, setForm] = useState({
    motif: '', service: '', lieu: '', notes: '',
    dateHeure: new Date(Date.now() + 86400000).toISOString().slice(0, 16),
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async () => {
    if (!form.motif.trim()) { setError('Le motif est obligatoire.'); return }
    setSaving(true); setError('')
    try {
      await patientApi.prendreRdv({ ...form, dateHeure: form.dateHeure + ':00' })
      onSuccess()
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la prise de RDV.')
    } finally { setSaving(false) }
  }

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View style={styles.modalContainer}>
        <View style={styles.modalHeader}>
          <Text style={styles.modalTitle}>📅 Nouvelle demande de RDV</Text>
          <TouchableOpacity onPress={onClose}>
            <Text style={{ fontSize: 22, color: theme.gray }}>✕</Text>
          </TouchableOpacity>
        </View>

        <ScrollView style={styles.modalBody} keyboardShouldPersistTaps="handled">
          {error ? (
            <View style={styles.errorBox}><Text style={styles.errorText}>⚠️ {error}</Text></View>
          ) : null}

          <Text style={styles.label}>Date et heure *</Text>
          <TextInput
            style={styles.input}
            value={form.dateHeure}
            onChangeText={v => setForm(f => ({ ...f, dateHeure: v }))}
            placeholder="YYYY-MM-DDTHH:MM"
          />

          <Text style={styles.label}>Motif *</Text>
          <TextInput
            style={styles.input}
            value={form.motif}
            onChangeText={v => setForm(f => ({ ...f, motif: v }))}
            placeholder="Consultation, suivi, urgence…"
          />

          <Text style={styles.label}>Service</Text>
          <TextInput
            style={styles.input}
            value={form.service}
            onChangeText={v => setForm(f => ({ ...f, service: v }))}
            placeholder="Cardiologie, Généraliste…"
          />

          <Text style={styles.label}>Lieu</Text>
          <TextInput
            style={styles.input}
            value={form.lieu}
            onChangeText={v => setForm(f => ({ ...f, lieu: v }))}
            placeholder="Cabinet, Bloc B, Salle 12…"
          />

          <Text style={styles.label}>Notes</Text>
          <TextInput
            style={[styles.input, { height: 80, textAlignVertical: 'top' }]}
            value={form.notes}
            onChangeText={v => setForm(f => ({ ...f, notes: v }))}
            placeholder="Informations supplémentaires…"
            multiline
          />

          <TouchableOpacity style={[styles.btn, { marginTop: 16 }]} onPress={handleSubmit} disabled={saving}>
            {saving ? <ActivityIndicator color="white" /> : <Text style={styles.btnText}>✅ Envoyer la demande</Text>}
          </TouchableOpacity>
          <View style={{ height: 40 }} />
        </ScrollView>
      </View>
    </Modal>
  )
}

export default function RendezVousScreen() {
  const [rdvList, setRdvList] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)

  const load = async () => {
    try {
      const r = await patientApi.getRdv()
      setRdvList(r.data || [])
      setError('')
    } catch {
      setError('Service rendez-vous indisponible pour l\'instant.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleAnnuler = (id) => {
    Alert.alert(
      'Confirmer l\'annulation',
      'Voulez-vous vraiment annuler ce rendez-vous ?',
      [
        { text: 'Non', style: 'cancel' },
        {
          text: 'Oui, annuler', style: 'destructive',
          onPress: async () => {
            try {
              await patientApi.annulerRdv(id)
              setRdvList(l => l.map(r => r.id === id ? { ...r, statut: 'ANNULE' } : r))
            } catch {
              Alert.alert('Erreur', 'Impossible d\'annuler. Contactez la clinique.')
            }
          }
        }
      ]
    )
  }

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={theme.primary} />
      </View>
    )
  }

  return (
    <View style={styles.container}>
      <NouveauRdvModal
        visible={showModal}
        onClose={() => setShowModal(false)}
        onSuccess={() => { setShowModal(false); load() }}
      />

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load() }} />}
      >
        {/* Bouton prendre RDV */}
        <TouchableOpacity style={styles.btn} onPress={() => setShowModal(true)}>
          <Text style={styles.btnText}>➕ Demander un rendez-vous</Text>
        </TouchableOpacity>

        {error && (
          <View style={[styles.errorBox, { marginTop: 12 }]}>
            <Text style={styles.errorText}>⚠️ {error}</Text>
          </View>
        )}

        {rdvList.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={{ fontSize: 48, marginBottom: 12 }}>📅</Text>
            <Text style={styles.emptyTitle}>Aucun rendez-vous</Text>
            <Text style={styles.subText}>Vous n'avez pas de rendez-vous planifié.</Text>
          </View>
        ) : (
          rdvList.map(rdv => (
            <RdvCard key={rdv.id} rdv={rdv} onAnnuler={handleAnnuler} />
          ))
        )}
        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  scrollContent: { padding: 16 },
  card: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    ...theme.shadow.sm,
  },
  cardRow: { flexDirection: 'row', gap: 12 },
  dateBlock: {
    backgroundColor: '#dbeafe',
    borderRadius: 10,
    padding: 8,
    alignItems: 'center',
    minWidth: 52,
    justifyContent: 'center',
  },
  dateDay: { fontSize: 20, fontWeight: '800', color: '#2563eb', lineHeight: 22 },
  dateMonth: { fontSize: 10, color: '#3b82f6', textTransform: 'uppercase' },
  dateTime: { fontSize: 10, color: '#3b82f6', marginTop: 2 },
  cardInfo: { flex: 1 },
  cardTitle: { fontWeight: '700', fontSize: 14, color: '#1e293b', marginBottom: 4 },
  subText: { fontSize: 12, color: theme.gray, marginTop: 1 },
  badge: {
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
    alignSelf: 'flex-start',
  },
  badgeText: { fontSize: 10, fontWeight: '700' },
  annulerBtn: {
    marginTop: 10,
    borderWidth: 1,
    borderColor: '#fca5a5',
    borderRadius: 8,
    paddingVertical: 6,
    alignItems: 'center',
  },
  annulerText: { color: '#ef4444', fontSize: 12, fontWeight: '600' },
  btn: {
    backgroundColor: theme.primary,
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 16,
  },
  btnText: { color: 'white', fontWeight: '700', fontSize: 15 },
  emptyContainer: { alignItems: 'center', paddingVertical: 60 },
  emptyTitle: { fontWeight: '700', fontSize: 16, color: '#1e293b', marginBottom: 4 },
  errorBox: { backgroundColor: '#fee2e2', borderRadius: 10, padding: 12, marginBottom: 12 },
  errorText: { color: '#991b1b', fontSize: 13 },
  // Modal
  modalContainer: { flex: 1, backgroundColor: 'white' },
  modalHeader: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    padding: 20, borderBottomWidth: 1, borderBottomColor: '#e5e7eb',
  },
  modalTitle: { fontWeight: '800', fontSize: 18, color: '#1e293b' },
  modalBody: { padding: 20 },
  label: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 4, marginTop: 12 },
  input: {
    borderWidth: 1, borderColor: '#d1d5db', borderRadius: 10,
    paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: '#1e293b',
    backgroundColor: 'white',
  },
})
