import axios from 'axios'
import AsyncStorage from '@react-native-async-storage/async-storage'
import { AUTH_BASE_URL, PATIENT_BASE_URL } from '../config'

// ─────────────────────────────────────────────────────────────────────────────
//  Pour configurer l'IP du serveur, créer un fichier .env à la racine :
//    EXPO_PUBLIC_API_HOST=192.168.1.42
//
//  Voir src/config.js pour les détails complets.
// ─────────────────────────────────────────────────────────────────────────────

const authAxios   = axios.create({ baseURL: AUTH_BASE_URL })
const patientAxios = axios.create({ baseURL: PATIENT_BASE_URL })

// Intercepteur : ajoute le token JWT à chaque requête
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
  login:          (data)  => authAxios.post('/auth/login', data),
  register:       (data)  => authAxios.post('/auth/register', data),
  forgotPassword: (email) => authAxios.post('/auth/forgot-password', { email }),
  resetPassword:  (data)  => authAxios.post('/auth/reset-password', data),
  me:             ()      => authAxios.get('/auth/me'),
}

export const patientApi = {
  me: () => patientAxios.get('/patient/me'),
}
