import { appointmentClient, timeslotClient, billingClient } from './client'
import axios from 'axios'

const waitlistClient = axios.create({ baseURL: '/api/v1/waiting-list' })
waitlistClient.interceptors.request.use(cfg => {
  const token = sessionStorage.getItem('medsys_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

export const appointmentApi = {
  create:        (data)  => appointmentClient.post('', data),
  getById:       (id)    => appointmentClient.get(`/${id}`),
  getByPatient:  (id)    => appointmentClient.get(`/patient/${id}`),
  getByMedecin:  (id)    => appointmentClient.get(`/medecin/${id}`),
  confirm:       (id)    => appointmentClient.put(`/${id}/confirm`),
  cancel:        (id, r) => appointmentClient.put(`/${id}/cancel`, null, { params: { reason: r } }),
  complete:      (id)    => appointmentClient.put(`/${id}/complete`),
  markNoShow:    (id)    => appointmentClient.put(`/${id}/no-show`),
  stats:         ()      => appointmentClient.get('/stats'),
}

export const timeslotApi = {
  create:            (data)      => timeslotClient.post('', data),
  getAllAvailable:    ()          => timeslotClient.get('/available'),
  getByMedecin:      (medecinId) => timeslotClient.get(`/medecin/${medecinId}/available`),
  getById:           (id)        => timeslotClient.get(`/${id}`),
  delete:            (id)        => timeslotClient.delete(`/${id}`),
}

export const waitingListApi = {
  add:            (data) => waitlistClient.post('', data),
  getByPatient:   (id)   => waitlistClient.get(`/patient/${id}`),
  getPending:     ()     => waitlistClient.get('/pending'),
}

export const billingApi = {
  createInvoice:  (data)        => billingClient.post('/invoices', data),
  getAll:         ()            => billingClient.get('/invoices'),
  getById:        (id)          => billingClient.get(`/invoices/${id}`),
  getByPatient:   (patientId)   => billingClient.get(`/invoices/patient/${patientId}`),
  getByStatus:    (status)      => billingClient.get(`/invoices/status/${status}`),
  addPayment:     (id, data)    => billingClient.post(`/invoices/${id}/payments`, data),
  cancel:         (id)          => billingClient.put(`/invoices/${id}/cancel`),
  stats:          ()            => billingClient.get('/stats'),
}
