import { useState, useEffect } from 'react'
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, RefreshControl } from 'react-native'
import { LinearGradient } from 'expo-linear-gradient'
import { useNavigation } from '@react-navigation/native'
import { useAuth } from '../context/AuthContext'
import { patientApi } from '../api/api'
import { colors, radius, shadow } from '../theme'

export default function HomeScreen() {
  const { user } = useAuth()
  const navigation = useNavigation()
  const [profil, setProfil] = useState(null)
  const [notifs, setNotifs] = useState({ total: 0, messagesNonLus: 0, analysesEnAttente: 0 })
  const [refreshing, setRefreshing] = useState(false)

  const load = async () => {
    try {
      const [profilRes, notifRes] = await Promise.allSettled([
        patientApi.me(),
        patientApi.notifications(),
      ])
      if (profilRes.status === 'fulfilled') setProfil(profilRes.value.data)
      if (notifRes.status === 'fulfilled') setNotifs(notifRes.value.data || {})
    } finally {
      setRefreshing(false)
    }
  }

  useEffect(() => { load() }, [])

  const p = profil || user

  const quickCards = [
    { icon: '📁', title: 'Mon dossier', sub: 'Antécédents, consultations', color: colors.primaryLight, screen: 'Dossier' },
    { icon: '📅', title: 'Rendez-vous', sub: 'Mes RDV planifiés', color: colors.successLight, screen: 'RendezVous' },
    { icon: '💬', title: 'Messagerie', sub: `${notifs.messagesNonLus || 0} non lu(s)`, color: '#fce7f3', screen: 'Messagerie', badge: notifs.messagesNonLus },
    { icon: '📂', title: 'Documents', sub: 'Ordonnances, radios…', color: '#ede9fe', screen: 'Documents' },
  ]

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.bg }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load() }} />}
    >
      {/* Banner */}
      <LinearGradient colors={['#0f172a', '#1e3a8a', '#2563eb']} style={styles.banner}>
        <View style={styles.bannerAvatar}>
          <Text style={styles.avatarText}>{p?.prenom?.[0]}{p?.nom?.[0]}</Text>
        </View>
        <Text style={styles.bannerGreeting}>Bonjour, {p?.prenom || 'Patient'} 👋</Text>
        <Text style={styles.bannerSub}>Votre espace santé personnel</Text>
        {p?.numeroDossier && (
          <View style={styles.cinBadge}>
            <Text style={styles.cinText}>📁 Dossier {p.numeroDossier}</Text>
          </View>
        )}
      </LinearGradient>

      <View style={{ padding: 16 }}>

        {/* Alertes */}
        {notifs.analysesEnAttente > 0 && (
          <View style={[styles.alertBox, { backgroundColor: colors.warningLight, borderColor: '#fcd34d' }]}>
            <Text style={{ fontSize: 14, color: '#78350f' }}>
              🧪 {notifs.analysesEnAttente} analyse{notifs.analysesEnAttente > 1 ? 's' : ''} en attente de résultats
            </Text>
          </View>
        )}
        {notifs.messagesNonLus > 0 && (
          <TouchableOpacity
            style={[styles.alertBox, { backgroundColor: colors.primaryLight, borderColor: '#93c5fd' }]}
            onPress={() => navigation.navigate('Messagerie')}
          >
            <Text style={{ fontSize: 14, color: '#1e40af' }}>
              💬 {notifs.messagesNonLus} nouveau{notifs.messagesNonLus > 1 ? 'x' : ''} message{notifs.messagesNonLus > 1 ? 's' : ''} — Appuyez pour lire
            </Text>
          </TouchableOpacity>
        )}

        {/* Accès rapide */}
        <Text style={styles.sectionTitle}>Accès rapide</Text>
        <View style={styles.quickGrid}>
          {quickCards.map((card, i) => (
            <TouchableOpacity
              key={i}
              style={[styles.quickCard, shadow.sm]}
              onPress={() => navigation.navigate(card.screen)}
            >
              <View style={styles.quickIconWrap}>
                <View style={[styles.quickIcon, { backgroundColor: card.color }]}>
                  <Text style={{ fontSize: 22 }}>{card.icon}</Text>
                </View>
                {card.badge > 0 && (
                  <View style={styles.notifDot}>
                    <Text style={styles.notifDotText}>{card.badge}</Text>
                  </View>
                )}
              </View>
              <Text style={styles.quickTitle}>{card.title}</Text>
              <Text style={styles.quickSub}>{card.sub}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Infos patient */}
        <Text style={styles.sectionTitle}>Mes informations</Text>
        <View style={[styles.infoCard, shadow.sm]}>
          {[
            ['Nom complet', `${p?.prenom || ''} ${p?.nom || ''}`.trim() || '—'],
            ['CIN', p?.cin || '—'],
            ['Groupe sanguin', p?.groupeSanguin?.replace('_', ' ') || '—'],
            ['Téléphone', p?.telephone || '—'],
            ['Ville', p?.ville || '—'],
            ['Mutuelle', p?.mutuelle || '—'],
          ].map(([k, v]) => (
            <View key={k} style={styles.infoRow}>
              <Text style={styles.infoLabel}>{k}</Text>
              <Text style={styles.infoValue} numberOfLines={1}>{v}</Text>
            </View>
          ))}
        </View>

      </View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  banner: { padding: 24, paddingTop: 48, alignItems: 'center', paddingBottom: 32 },
  bannerAvatar: {
    width: 64, height: 64, borderRadius: 32,
    backgroundColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 12, borderWidth: 2, borderColor: 'rgba(255,255,255,0.3)',
  },
  avatarText: { fontSize: 22, fontWeight: '800', color: 'white' },
  bannerGreeting: { fontSize: 22, fontWeight: '800', color: 'white', marginBottom: 4 },
  bannerSub: { fontSize: 13, color: 'rgba(255,255,255,0.7)', marginBottom: 14 },
  cinBadge: {
    backgroundColor: 'rgba(255,255,255,0.15)',
    borderRadius: 20, paddingHorizontal: 14, paddingVertical: 6,
    borderWidth: 1, borderColor: 'rgba(255,255,255,0.25)',
  },
  cinText: { color: 'rgba(255,255,255,0.9)', fontSize: 12, fontWeight: '600' },
  alertBox: {
    borderRadius: 10, padding: 12, marginBottom: 12,
    borderWidth: 1,
  },
  sectionTitle: { fontSize: 16, fontWeight: '800', color: colors.dark, marginBottom: 12 },
  quickGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginBottom: 16 },
  quickCard: {
    width: '47%',
    backgroundColor: 'white',
    borderRadius: radius.md,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.border,
  },
  quickIconWrap: { position: 'relative', alignSelf: 'flex-start', marginBottom: 10 },
  quickIcon: { width: 44, height: 44, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  notifDot: {
    position: 'absolute', top: -4, right: -4,
    backgroundColor: colors.danger,
    borderRadius: 10, width: 18, height: 18,
    alignItems: 'center', justifyContent: 'center',
  },
  notifDotText: { color: 'white', fontSize: 9, fontWeight: '700' },
  quickTitle: { fontSize: 13, fontWeight: '700', color: colors.dark, marginBottom: 3 },
  quickSub: { fontSize: 11, color: colors.gray },
  infoCard: {
    backgroundColor: 'white', borderRadius: radius.md,
    borderWidth: 1, borderColor: colors.border, overflow: 'hidden',
    marginBottom: 24,
  },
  infoRow: {
    flexDirection: 'row', justifyContent: 'space-between',
    padding: 14, borderBottomWidth: 1, borderBottomColor: colors.grayLight,
  },
  infoLabel: { fontSize: 13, color: colors.gray },
  infoValue: { fontSize: 13, fontWeight: '700', color: colors.dark, maxWidth: '60%', textAlign: 'right' },
})
