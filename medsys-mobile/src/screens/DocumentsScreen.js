import React, { useState, useEffect } from 'react'
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert, RefreshControl, Linking
} from 'react-native'
import { patientApi } from '../api/api'
import { theme } from '../theme'

const EXT_ICONS = {
  pdf: '📄', jpg: '🖼️', jpeg: '🖼️', png: '🖼️',
  doc: '📝', docx: '📝', default: '📁',
}

function getExtIcon(filename) {
  if (!filename) return EXT_ICONS.default
  const ext = filename.split('.').pop()?.toLowerCase()
  return EXT_ICONS[ext] || EXT_ICONS.default
}

function formatBytes(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} o`
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / 1048576).toFixed(1)} Mo`
}

function DocumentCard({ doc, onDelete, onOpen }) {
  return (
    <View style={styles.card}>
      <View style={styles.cardRow}>
        <View style={styles.iconBox}>
          <Text style={styles.fileIcon}>{getExtIcon(doc.nomFichier)}</Text>
        </View>
        <View style={styles.cardInfo}>
          <Text style={styles.fileName} numberOfLines={2}>{doc.nomFichier || 'Document'}</Text>
          {doc.description && (
            <Text style={styles.subText} numberOfLines={1}>{doc.description}</Text>
          )}
          <View style={styles.metaRow}>
            {doc.dateUpload && (
              <Text style={styles.metaText}>
                {new Date(doc.dateUpload).toLocaleDateString('fr-FR')}
              </Text>
            )}
            {doc.taille && <Text style={styles.metaText}>{formatBytes(doc.taille)}</Text>}
          </View>
        </View>
        <View style={styles.actions}>
          <TouchableOpacity style={styles.actionBtn} onPress={() => onOpen(doc)}>
            <Text style={{ fontSize: 20 }}>👁️</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.actionBtn, { marginTop: 4 }]} onPress={() => onDelete(doc.id)}>
            <Text style={{ fontSize: 20 }}>🗑️</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  )
}

export default function DocumentsScreen() {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const load = async () => {
    try {
      const r = await patientApi.getDocuments()
      setDocuments(r.data || [])
      setError('')
    } catch {
      setError('Impossible de charger vos documents.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleDelete = (id) => {
    Alert.alert(
      'Supprimer le document',
      'Cette action est irréversible. Confirmer ?',
      [
        { text: 'Annuler', style: 'cancel' },
        {
          text: 'Supprimer', style: 'destructive',
          onPress: async () => {
            try {
              await patientApi.deleteDocument(id)
              setDocuments(d => d.filter(doc => doc.id !== id))
            } catch {
              Alert.alert('Erreur', 'Impossible de supprimer le document.')
            }
          }
        }
      ]
    )
  }

  const handleOpen = async (doc) => {
    try {
      const url = patientApi.getDocumentUrl(doc.id)
      const supported = await Linking.canOpenURL(url)
      if (supported) {
        await Linking.openURL(url)
      } else {
        Alert.alert('Non supporté', 'Impossible d\'ouvrir ce type de fichier sur cet appareil.')
      }
    } catch {
      Alert.alert('Erreur', 'Impossible d\'ouvrir le document.')
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
    <View style={styles.container}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load() }} />}
      >
        {/* Info upload */}
        <View style={styles.infoBox}>
          <Text style={styles.infoText}>
            📋 Pour uploader un document, utilisez l'application web MedSys depuis un navigateur.
          </Text>
        </View>

        {error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>⚠️ {error}</Text>
          </View>
        )}

        {documents.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={{ fontSize: 48, marginBottom: 12 }}>📂</Text>
            <Text style={styles.emptyTitle}>Aucun document</Text>
            <Text style={styles.subText}>Vos documents médicaux apparaîtront ici.</Text>
          </View>
        ) : (
          <>
            <Text style={styles.sectionLabel}>{documents.length} document{documents.length > 1 ? 's' : ''}</Text>
            {documents.map(doc => (
              <DocumentCard
                key={doc.id}
                doc={doc}
                onDelete={handleDelete}
                onOpen={handleOpen}
              />
            ))}
          </>
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
  infoBox: {
    backgroundColor: '#eff6ff',
    borderRadius: 10,
    padding: 12,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#bfdbfe',
  },
  infoText: { fontSize: 13, color: '#1e40af', lineHeight: 18 },
  errorBox: { backgroundColor: '#fee2e2', borderRadius: 10, padding: 12, marginBottom: 12 },
  errorText: { color: '#991b1b', fontSize: 13 },
  sectionLabel: { fontSize: 12, color: theme.gray, marginBottom: 10, fontWeight: '600' },
  card: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    ...theme.shadow.sm,
  },
  cardRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 12 },
  iconBox: {
    width: 48, height: 48, borderRadius: 10,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center', alignItems: 'center',
    flexShrink: 0,
  },
  fileIcon: { fontSize: 24 },
  cardInfo: { flex: 1 },
  fileName: { fontWeight: '600', fontSize: 14, color: '#1e293b' },
  subText: { fontSize: 12, color: theme.gray, marginTop: 2 },
  metaRow: { flexDirection: 'row', gap: 12, marginTop: 4 },
  metaText: { fontSize: 11, color: theme.gray },
  actions: { alignItems: 'center' },
  actionBtn: {
    width: 36, height: 36, borderRadius: 8,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center', alignItems: 'center',
  },
  emptyContainer: { alignItems: 'center', paddingVertical: 60 },
  emptyTitle: { fontWeight: '700', fontSize: 16, color: '#1e293b', marginBottom: 4 },
})
