import { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  ActivityIndicator, Alert, KeyboardAvoidingView, Platform, ScrollView
} from 'react-native'
import { LinearGradient } from 'expo-linear-gradient'
import { useAuth } from '../context/AuthContext'
import { authApi } from '../api/api'
import { colors, radius, shadow } from '../theme'

export default function LoginScreen({ navigation }) {
  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  // 2FA
  const [twoFaPending, setTwoFaPending] = useState(false)
  const [twoFaEmail, setTwoFaEmail] = useState('')
  const [twoFaCode, setTwoFaCode] = useState('')
  const { login } = useAuth()

  const handleLogin = async () => {
    if (!form.email || !form.password) {
      Alert.alert('Erreur', 'Veuillez remplir tous les champs')
      return
    }
    setLoading(true)
    try {
      const { data } = await authApi.login(form)
      if (data.role !== 'PATIENT') {
        Alert.alert('Accès refusé', 'Cet espace est réservé aux patients.')
        return
      }
      if (data.requiresTwoFa) {
        setTwoFaEmail(data.twoFaSessionId)
        setTwoFaPending(true)
        return
      }
      await login(data, data.token)
    } catch (err) {
      Alert.alert('Erreur', err.response?.data?.message || 'Email ou mot de passe incorrect')
    } finally { setLoading(false) }
  }

  const handleVerify2fa = async () => {
    if (twoFaCode.length < 6) {
      Alert.alert('Erreur', 'Entrez le code à 6 chiffres.')
      return
    }
    setLoading(true)
    try {
      const { data } = await authApi.verify2fa(twoFaEmail, twoFaCode)
      await login(data, data.token)
    } catch (err) {
      Alert.alert('Code invalide', err.response?.data?.message || 'Code incorrect ou expiré. Réessayez.')
      setTwoFaCode('')
    } finally { setLoading(false) }
  }

  if (twoFaPending) {
    return (
      <LinearGradient colors={['#0f172a', '#1e3a8a', '#0891b2']} style={styles.container}>
        <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
          <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
            <View style={styles.logoContainer}>
              <View style={styles.logoIcon}><Text style={{ fontSize: 36 }}>🔐</Text></View>
              <Text style={styles.logoText}>MedSys</Text>
              <Text style={styles.logoSub}>Vérification 2FA</Text>
            </View>
            <View style={styles.card}>
              <Text style={styles.title}>Code de vérification</Text>
              <Text style={styles.subtitle}>
                Un code à 6 chiffres a été envoyé à{'\n'}
                <Text style={{ fontWeight: '700', color: colors.dark }}>{twoFaEmail}</Text>
              </Text>
              <View style={styles.inputGroup}>
                <Text style={styles.label}>Code 2FA</Text>
                <TextInput
                  style={[styles.input, { letterSpacing: 10, fontSize: 24, textAlign: 'center', fontWeight: '800' }]}
                  placeholder="000000"
                  placeholderTextColor={colors.gray}
                  keyboardType="number-pad"
                  maxLength={6}
                  value={twoFaCode}
                  onChangeText={v => setTwoFaCode(v.replace(/\D/g, '').slice(0, 6))}
                  autoFocus
                />
              </View>
              <TouchableOpacity style={styles.loginBtn} onPress={handleVerify2fa} disabled={loading || twoFaCode.length < 6}>
                {loading
                  ? <ActivityIndicator color="white" />
                  : <Text style={styles.loginBtnText}>✅ Valider le code</Text>
                }
              </TouchableOpacity>
              <TouchableOpacity
                style={{ marginTop: 16, alignItems: 'center' }}
                onPress={() => { setTwoFaPending(false); setTwoFaCode('') }}
              >
                <Text style={{ color: colors.gray, fontSize: 13 }}>← Retour à la connexion</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </LinearGradient>
    )
  }

  return (
    <LinearGradient colors={['#0f172a', '#1e3a8a', '#0891b2']} style={styles.container}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.logoContainer}>
            <View style={styles.logoIcon}><Text style={{ fontSize: 36 }}>🏥</Text></View>
            <Text style={styles.logoText}>MedSys</Text>
            <Text style={styles.logoSub}>Espace Patient</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.title}>Connexion</Text>
            <Text style={styles.subtitle}>Accédez à votre dossier médical</Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Email</Text>
              <TextInput
                style={styles.input}
                placeholder="votre@email.com"
                placeholderTextColor={colors.gray}
                keyboardType="email-address"
                autoCapitalize="none"
                value={form.email}
                onChangeText={v => setForm({ ...form, email: v })}
              />
            </View>
            <View style={styles.inputGroup}>
              <Text style={styles.label}>Mot de passe</Text>
              <TextInput
                style={styles.input}
                placeholder="••••••••"
                placeholderTextColor={colors.gray}
                secureTextEntry
                value={form.password}
                onChangeText={v => setForm({ ...form, password: v })}
              />
            </View>

            <TouchableOpacity onPress={handleLogin} style={styles.loginBtn} disabled={loading}>
              {loading
                ? <ActivityIndicator color="white" />
                : <Text style={styles.loginBtnText}>→ Se connecter</Text>
              }
            </TouchableOpacity>

            <TouchableOpacity
              onPress={() => navigation.navigate('ForgotPassword')}
              style={{ marginTop: 16, alignItems: 'center' }}
            >
              <Text style={{ color: colors.primary, fontSize: 13 }}>Mot de passe oublié ?</Text>
            </TouchableOpacity>

            <View style={styles.divider}>
              <View style={styles.dividerLine} />
              <Text style={styles.dividerText}>ou</Text>
              <View style={styles.dividerLine} />
            </View>

            <TouchableOpacity onPress={() => navigation.navigate('Register')} style={styles.registerBtn}>
              <Text style={styles.registerBtnText}>📝 Créer un compte</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </LinearGradient>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flexGrow: 1, justifyContent: 'center', padding: 20 },
  logoContainer: { alignItems: 'center', marginBottom: 32 },
  logoIcon: {
    width: 72, height: 72, borderRadius: 22,
    backgroundColor: 'rgba(255,255,255,0.15)',
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 12, borderWidth: 2, borderColor: 'rgba(255,255,255,0.2)',
  },
  logoText: { fontSize: 36, fontWeight: '800', color: 'white' },
  logoSub: { fontSize: 14, color: 'rgba(255,255,255,0.6)', marginTop: 4 },
  card: { backgroundColor: 'white', borderRadius: radius.xl, padding: 28, ...shadow.md },
  title: { fontSize: 22, fontWeight: '800', color: colors.dark, marginBottom: 4 },
  subtitle: { fontSize: 13, color: colors.gray, marginBottom: 24, lineHeight: 20 },
  inputGroup: { marginBottom: 16 },
  label: { fontSize: 13, fontWeight: '700', color: colors.dark, marginBottom: 6 },
  input: {
    borderWidth: 1.5, borderColor: colors.border, borderRadius: radius.sm,
    padding: 12, fontSize: 14, color: colors.dark, backgroundColor: colors.bg,
  },
  loginBtn: {
    backgroundColor: colors.primary, borderRadius: radius.sm,
    padding: 14, alignItems: 'center', marginTop: 4,
  },
  loginBtnText: { color: 'white', fontWeight: '700', fontSize: 15 },
  divider: { flexDirection: 'row', alignItems: 'center', marginVertical: 20 },
  dividerLine: { flex: 1, height: 1, backgroundColor: colors.border },
  dividerText: { marginHorizontal: 12, color: colors.gray, fontSize: 13 },
  registerBtn: {
    borderWidth: 2, borderColor: colors.primary,
    borderRadius: radius.sm, padding: 13, alignItems: 'center',
  },
  registerBtnText: { color: colors.primary, fontWeight: '700', fontSize: 14 },
})
