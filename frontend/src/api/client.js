import axios from 'axios'

const BASE_URL = '/api/v1'

function createClient(basePath) {
  const client = axios.create({ baseURL: BASE_URL + basePath })

  client.interceptors.request.use(cfg => {
    const token = sessionStorage.getItem('medsys_token')
    if (token) cfg.headers.Authorization = `Bearer ${token}`
    return cfg
  })

  client.interceptors.response.use(
    res => res,
    err => {
      if (err.response?.status === 401) {
        sessionStorage.clear()
        window.location.href = '/'
      }
      return Promise.reject(err)
    }
  )

  return client
}

export const authClient      = createClient('/auth')
export const patientClient   = createClient('/patients')
export const medicalClient   = createClient('/medical-records')
export const appointmentClient = createClient('/appointments')
export const timeslotClient  = createClient('/time-slots')
export const billingClient   = createClient('/billing')
export const adminClient     = createClient('/admin')
