import { useState, useEffect } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, Alert, TextInput, ActivityIndicator, Switch
} from 'react-native'
import { LinearGradient } from 'expo-linear-gradient'
import { useAuth } from '../context/AuthContext'
import { authApi, patientApi } from '../api/api'
import { colors, radius, shadow } from '../theme'

function Section({ title, children }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <View style={[styles.sectionCard, shadow.sm]}>{children}</View>
    </View>
  )
}

function SettingRow({ icon, label, sub, right, onPress, danger }) {
  return (
    <TouchableOpacity style={styles.settingRow} onPress={onPress} disabled={!onPress}>
      <View style={styles.settingLeft}>
        <Text style={styles.settingIcon}>{icon}</Text>
        <View style={{ flex: 1 }}>
          <Text style={[styles.settingLabel, danger && { color: colors.danger }]}>{label}</Text>
          {sub ? <Text style={styles.settingSub}>{sub}</Text> : null}
        </View>
      </View>
      {right}
    </TouchableOpacity>
  )
}

export default function ProfileScreen() {
  const { user, logout } = useAuth()
  const [profil, setProfil] = useState(null)
  const [twoFaEnabled, setTwoFaEnabled] = useState(false)
  const [togglingTwoFa, setTogglingTwoFa] = useState(false)
  const [showPwd, setShowPwd] = useState(false)
  const [pwdForm, setPwdForm] = useState({ ancienMotDePasse: '', nouveauMotDePasse: '', confirmation: '' })
  const [savingPwd, setSavingPwd] = useState(false)
  const [pwdError, setPwdError] = useState('')
  const [pwdSuccess, setPwdSuccess] = useState('')

  useEffect(() => {
    patientApi.me()
      .then(r => { setProfil(r.data); setTwoFaEnabled(r.data?.twoFaEnabled || false) })
      .catch(() => {})
  }, [])

  const p = profil || user

  const handleLogout = () => {
    Alert.alert('Déconnexion', 'Voulez-vous vraiment vous déconnecter ?', [
      { text: 'Annuler', style: 'cancel' },
      { text: 'Se déconnecter', style: 'destructive', onPress: logout },
    ])
  }

  const handleToggle2fa = async () => {
    setTogglingTwoFa(true)
    try {
      const r = await authApi.toggle2fa()
      const newState = r.data?.twoFaEnabled ?? !twoFaEnabled
      setTwoFaEnabled(newState)
      Alert.alert(
        newState ? '2FA activée ✅' : '2FA désactivée',
        newState
          ? 'Un code de vérification vous sera envoyé par email à chaque connexion.'
          : 'La double authentification a été désactivée.'
      )
    } catch {
      Alert.alert('Erreur', 'Impossible de modifier le paramètre 2FA.')
    } finally {
      setTogglingTwoFa(false)
    }
  }

  const handleChangePassword = async () => {
    setPwdError(''); setPwdSuccess('')
    if (pwdForm.nouveauMotDePasse.length < 8) {
      setPwdError('Le nouveau mot de passe doit comporter au moins 8 caractères.')
      return
    }
    if (pwdForm.nouveauMotDePasse !== pwdForm.confirmation) {
      setPwdError('Les mots de passe ne correspondent pas.')
      return
    }
    setSavingPwd(true)
    try {
      await authApi.changePassword({
        ancienMotDePasse: pwdForm.ancienMotDePasse,
        nouveauMotDePasse: pwdForm.nouveauMotDePasse,
      })
      setPwdSuccess('Mot de passe modifié avec succès !')
      setPwdForm({ ancienMotDePasse: '', nouveauMotDePasse: '', confirmation: '' })
      setTimeout(() => setShowPwd(false), 2000)
    } catch (err) {
      setPwdError(err.response?.data?.message || 'Ancien mot de passe incorrect.')
    } finally {
      setSavingPwd(false)
    }
  }

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }}>
      <LinearGradient colors={['#1e3a8a', '#2563eb']} style={styles.header}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{p?.prenom?.[0]}{p?.nom?.[0]}</Text>
        </View>
        <Text style={styles.name}>{p?.prenom} {p?.nom}</Text>
        <Text style={styles.email}>{p?.email || ''}</Text>
        <View style={styles.roleBadge}>
          <Text style={styles.roleText}>PATIENT</Text>
        </View>
      </LinearGradient>

      <View style={{ padding: 16 }}>

        <Section title="👤 Informations">
          {[
            ['CIN', p?.cin || '—'],
            ['Téléphone', p?.telephone || '—'],
            ['Ville', p?.ville || '—'],
            ['Groupe sanguin', p?.groupeSanguin?.replace('_', ' ') || '—'],
            ['Mutuelle', p?.mutuelle || '—'],
            ['N° Dossier', p?.numeroDossier || '—'],
          ].map(([k, v]) => (
            <View key={k} style={styles.infoRow}>
              <Text style={styles.infoLabel}>{k}</Text>
              <Text style={styles.infoValue}>{v}</Text>
            </View>
          ))}
        </Section>

        <Section title="🔒 Sécurité">
          <SettingRow
            icon="📱"
            label="Double authentification (2FA)"
            sub={twoFaEnabled ? 'Activée — code email à chaque connexion' : 'Désactivée'}
            right={
              togglingTwoFa
                ? <ActivityIndicator size="small" color={colors.primary} />
                : <Switch
                    value={twoFaEnabled}
                    onValueChange={handleToggle2fa}
                    trackColor={{ false: colors.grayLight, true: colors.primary }}
                  />
            }
          />
          <SettingRow
            icon="🔑"
            label="Changer le mot de passe"
            sub="Modifier votre mot de passe actuel"
            onPress={() => setShowPwd(s => !s)}
            right={<Text style={{ color: colors.gray, fontSize: 16 }}>{showPwd ? '▲' : '▼'}</Text>}
          />
          {showPwd && (
            <View style={styles.pwdForm}>
              {pwdError ? <Text style={styles.errText}>⚠️ {pwdError}</Text> : null}
              {pwdSuccess ? <Text style={styles.successText}>✅ {pwdSuccess}</Text> : null}
              <TextInput
                style={styles.input}
                placeholder="Mot de passe actuel"
                secureTextEntry
                value={pwdForm.ancienMotDePasse}
                onChangeText={v => setPwdForm(f => ({ ...f, ancienMotDePasse: v }))}
              />
              <TextInput
                style={styles.input}
                placeholder="Nouveau mot de passe (8+ car.)"
                secureTextEntry
                value={pwdForm.nouveauMotDePasse}
                onChangeText={v => setPwdForm(f => ({ ...f, nouveauMotDePasse: v }))}
              />
              <TextInput
                style={styles.input}
                placeholder="Confirmer le nouveau mot de passe"
                secureTextEntry
                value={pwdForm.confirmation}
                onChangeText={v => setPwdForm(f => ({ ...f, confirmation: v }))}
              />
              <TouchableOpacity style={styles.saveBtn} onPress={handleChangePassword} disabled={savingPwd}>
                {savingPwd
                  ? <ActivityIndicator color="white" size="small" />
                  : <Text style={styles.saveBtnText}>💾 Enregistrer</Text>
                }
              </TouchableOpacity>
            </View>
          )}
        </Section>

        <Section title="Compte">
          <SettingRow
            icon="🚪"
            label="Se déconnecter"
            danger
            onPress={handleLogout}
          />
        </Section>

        <View style={{ height: 40 }} />
      </View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  header: { padding: 32, paddingTop: 52, alignItems: 'center' },
  avatar: {
    width: 72, height: 72, borderRadius: 36,
    backgroundColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 10, borderWidth: 2, borderColor: 'rgba(255,255,255,0.3)',
  },
  avatarText: { fontSize: 26, fontWeight: '800', color: 'white' },
  name: { fontSize: 20, fontWeight: '800', color: 'white', marginBottom: 2 },
  email: { fontSize: 13, color: 'rgba(255,255,255,0.7)', marginBottom: 10 },
  roleBadge: { backgroundColor: '#059669', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 3 },
  roleText: { color: 'white', fontSize: 11, fontWeight: '700' },
  section: { marginBottom: 20 },
  sectionTitle: { fontSize: 14, fontWeight: '700', color: colors.dark, marginBottom: 8 },
  sectionCard: {
    backgroundColor: 'white', borderRadius: radius.md,
    borderWidth: 1, borderColor: colors.border, overflow: 'hidden',
  },
  infoRow: {
    flexDirection: 'row', justifyContent: 'space-between',
    padding: 14, borderBottomWidth: 1, borderBottomColor: colors.grayLight,
  },
  infoLabel: { fontSize: 13, color: colors.gray },
  infoValue: { fontSize: 13, fontWeight: '600', color: colors.dark },
  settingRow: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    padding: 14, borderBottomWidth: 1, borderBottomColor: colors.grayLight,
  },
  settingLeft: { flexDirection: 'row', alignItems: 'center', gap: 12, flex: 1 },
  settingIcon: { fontSize: 20 },
  settingLabel: { fontSize: 14, fontWeight: '600', color: colors.dark },
  settingSub: { fontSize: 12, color: colors.gray, marginTop: 1 },
  pwdForm: { padding: 14, borderTopWidth: 1, borderTopColor: colors.grayLight, gap: 10 },
  input: {
    borderWidth: 1, borderColor: colors.border, borderRadius: 10,
    paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: colors.dark,
  },
  saveBtn: {
    backgroundColor: colors.primary, borderRadius: 10,
    paddingVertical: 12, alignItems: 'center', marginTop: 4,
  },
  saveBtnText: { color: 'white', fontWeight: '700', fontSize: 14 },
  errText: { color: colors.danger, fontSize: 13 },
  successText: { color: colors.success, fontSize: 13 },
})
