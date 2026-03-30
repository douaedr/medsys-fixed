import React, { useState, useEffect, useRef } from 'react'
import {
  View, Text, ScrollView, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, KeyboardAvoidingView, Platform, RefreshControl
} from 'react-native'
import { useAuth } from '../context/AuthContext'
import { patientApi } from '../api/api'
import { theme } from '../theme'

function MessageBubble({ message, isPatient }) {
  const fmtTime = (d) => {
    if (!d) return ''
    const date = new Date(d)
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <View style={[styles.bubbleWrapper, isPatient ? styles.bubbleRight : styles.bubbleLeft]}>
      {!isPatient && message.medecinNom && (
        <Text style={styles.senderName}>👨‍⚕️ {message.medecinNom}</Text>
      )}
      <View style={[styles.bubble, isPatient ? styles.bubblePatient : styles.bubbleMedecin]}>
        <Text style={[styles.bubbleText, isPatient && { color: 'white' }]}>{message.contenu}</Text>
      </View>
      <Text style={[styles.timeText, isPatient ? { textAlign: 'right' } : {}]}>
        {fmtTime(message.dateEnvoi)}
        {isPatient && (message.lu ? '  · Lu ✓' : '  · Envoyé')}
      </Text>
    </View>
  )
}

export default function MessagerieScreen() {
  const { user } = useAuth()
  const [messages, setMessages] = useState([])
  const [contenu, setContenu] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const scrollRef = useRef(null)

  const load = async () => {
    try {
      const r = await patientApi.getMessages()
      setMessages(r.data || [])
      setError('')
    } catch {
      setError('Impossible de charger les messages.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    load()
    // Polling toutes les 15s
    const interval = setInterval(load, 15000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (messages.length > 0 && scrollRef.current) {
      setTimeout(() => scrollRef.current?.scrollToEnd({ animated: false }), 100)
    }
  }, [messages.length])

  const handleSend = async () => {
    if (!contenu.trim() || sending) return
    const text = contenu.trim()
    setContenu('')
    setSending(true)
    try {
      await patientApi.envoyerMessage({ contenu: text })
      await load()
    } catch {
      setError('Erreur d\'envoi du message.')
      setContenu(text)
    } finally {
      setSending(false)
    }
  }

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={theme.primary} />
      </View>
    )
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={90}
    >
      {/* En-tête */}
      <View style={styles.header}>
        <View style={styles.headerAvatar}>
          <Text style={{ fontSize: 20 }}>👨‍⚕️</Text>
        </View>
        <View>
          <Text style={styles.headerTitle}>Équipe médicale</Text>
          <Text style={styles.headerSub}>Messagerie sécurisée</Text>
        </View>
      </View>

      {error ? (
        <View style={styles.errorBox}><Text style={styles.errorText}>⚠️ {error}</Text></View>
      ) : null}

      {/* Messages */}
      <ScrollView
        ref={scrollRef}
        style={styles.messageList}
        contentContainerStyle={styles.messageListContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load() }} />}
        onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: false })}
      >
        {messages.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={{ fontSize: 40, marginBottom: 10 }}>💬</Text>
            <Text style={styles.emptyText}>Aucun message</Text>
            <Text style={[styles.emptyText, { fontSize: 12 }]}>Envoyez un message à votre équipe médicale</Text>
          </View>
        ) : (
          messages.map((m, i) => (
            <MessageBubble
              key={m.id || i}
              message={m}
              isPatient={m.expediteurRole === 'PATIENT' || m.expediteurId === user?.userId}
            />
          ))
        )}
      </ScrollView>

      {/* Zone de saisie */}
      <View style={styles.inputBar}>
        <TextInput
          style={styles.input}
          value={contenu}
          onChangeText={setContenu}
          placeholder="Écrivez votre message…"
          multiline
          maxLength={1000}
          returnKeyType="send"
          onSubmitEditing={handleSend}
          blurOnSubmit={false}
        />
        <TouchableOpacity
          style={[styles.sendBtn, (!contenu.trim() || sending) && styles.sendBtnDisabled]}
          onPress={handleSend}
          disabled={!contenu.trim() || sending}
        >
          {sending ? (
            <ActivityIndicator size="small" color="white" />
          ) : (
            <Text style={styles.sendIcon}>➤</Text>
          )}
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerAvatar: {
    width: 44, height: 44, borderRadius: 22,
    backgroundColor: '#dbeafe',
    justifyContent: 'center', alignItems: 'center',
  },
  headerTitle: { fontWeight: '700', fontSize: 15, color: '#1e293b' },
  headerSub: { fontSize: 12, color: theme.gray },
  errorBox: { backgroundColor: '#fee2e2', padding: 10, margin: 8, borderRadius: 8 },
  errorText: { color: '#991b1b', fontSize: 13 },
  messageList: { flex: 1 },
  messageListContent: { padding: 16, paddingBottom: 8 },
  emptyContainer: { alignItems: 'center', paddingVertical: 60 },
  emptyText: { fontSize: 14, color: theme.gray, textAlign: 'center', marginTop: 4 },
  bubbleWrapper: { marginBottom: 12, maxWidth: '80%' },
  bubbleLeft: { alignSelf: 'flex-start' },
  bubbleRight: { alignSelf: 'flex-end' },
  senderName: { fontSize: 11, color: '#2563eb', fontWeight: '600', marginBottom: 2, paddingLeft: 4 },
  bubble: {
    borderRadius: 16,
    padding: 10,
    paddingHorizontal: 14,
    ...theme.shadow.sm,
  },
  bubblePatient: {
    backgroundColor: '#2563eb',
    borderBottomRightRadius: 4,
  },
  bubbleMedecin: {
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderBottomLeftRadius: 4,
  },
  bubbleText: { fontSize: 14, color: '#1e293b', lineHeight: 20 },
  timeText: { fontSize: 10, color: theme.gray, marginTop: 3, paddingHorizontal: 4 },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: 10,
    gap: 8,
    backgroundColor: 'white',
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 8,
    fontSize: 14,
    maxHeight: 100,
    backgroundColor: '#f9fafb',
  },
  sendBtn: {
    width: 40, height: 40,
    borderRadius: 20,
    backgroundColor: theme.primary,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendBtnDisabled: { backgroundColor: '#93c5fd' },
  sendIcon: { color: 'white', fontSize: 16 },
})
