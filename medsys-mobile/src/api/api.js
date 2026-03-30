import axios from 'axios'
import AsyncStorage from '@react-native-async-storage/async-storage'

// Configurer dans .env :
// EXPO_PUBLIC_AUTH_URL=http://192.168.1.X:8082/api/v1
// EXPO_PUBLIC_PATIENT_URL=http://192.168.1.X:8081/api/v1
const AUTH_BASE = process.env.EXPO_PUBLIC_AUTH_URL || 'http://10.0.2.2:8082/api/v1'
const PATIENT_BASE = process.env.EXPO_PUBLIC_PATIENT_URL || 'http://10.0.2.2:8081/api/v1'

const authAxios = axios.create({ baseURL: AUTH_BASE })
const patientAxios = axios.create({ baseURL: PATIENT_BASE })

const addAuthInterceptor = (instance) => {
  instance.interceptors.request.use(async (config) => {
    const token = await AsyncStorage.getItem('medsys_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  })
}
addAuthInterceptor(authAxios)
addAuthInterceptor(patientAxios)

export const authApi = {
  login: (data) => authAxios.post('/auth/login', data),
  register: (data) => authAxios.post('/auth/register', data),
  forgotPassword: (email) => authAxios.post('/auth/forgot-password', { email }),
  resetPassword: (data) => authAxios.post('/auth/reset-password', data),
  changePassword: (data) => authAxios.post('/auth/change-password', data),
  me: () => authAxios.get('/auth/me'),
  verify2fa: (email, code) => authAxios.post('/auth/2fa/verify', { email, code }),
  toggle2fa: () => authAxios.post('/auth/2fa/toggle'),
}

export const patientApi = {
  // Profil
  me: () => patientAxios.get('/patient/me'),
  updateMe: (data) => patientAxios.patch('/patient/me', data),

  // Dossier médical
  myDossier: () => patientAxios.get('/patient/me/dossier'),

  // Notifications
  notifications: () => patientAxios.get('/patient/me/notifications'),

  // Rendez-vous
  getRdv: () => patientAxios.get('/patient/me/rdv'),
  prendreRdv: (data) => patientAxios.post('/patient/me/rdv', data),
  annulerRdv: (id) => patientAxios.put(`/patient/me/rdv/${id}/annuler`),

  // Documents
  getDocuments: () => patientAxios.get('/patient/me/documents'),
  deleteDocument: (id) => patientAxios.delete(`/patient/me/documents/${id}`),
  getDocumentUrl: (id) => `${PATIENT_BASE}/patient/me/documents/${id}/fichier`,

  // Messagerie
  getMessages: () => patientAxios.get('/patient/me/messages'),
  envoyerMessage: (data) => patientAxios.post('/patient/me/messages', data),
  marquerLu: (id) => patientAxios.put(`/patient/me/messages/${id}/lu`),

  // Export
  exportPdf: () => patientAxios.get('/patient/me/dossier/pdf', { responseType: 'blob' }),
  exportRgpd: () => patientAxios.get('/patient/me/export-rgpd', { responseType: 'blob' }),
}
