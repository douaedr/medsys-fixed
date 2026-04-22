import { patientClient, medicalClient } from './client'

export const patientApi = {
  // CRUD patients
  getAll:     (params) => patientClient.get('', { params }),
  getById:    (id)     => patientClient.get(`/${id}`),
  getByCin:   (cin)    => patientClient.get(`/cin/${cin}`),
  create:     (data)   => patientClient.post('', data),
  update:     (id, data) => patientClient.put(`/${id}`, data),
  delete:     (id)     => patientClient.delete(`/${id}`),
  search:     (q)      => patientClient.get('/search', { params: { q } }),
  stats:      ()       => patientClient.get('/statistiques'),
}

export const medicalRecordApi = {
  // Dossier médical
  getDossier:      (patientId)       => medicalClient.get(`/patient/${patientId}`),

  // Consultations
  addConsultation: (patientId, data) => medicalClient.post(`/patient/${patientId}/consultations`, data),

  // Antécédents
  addAntecedent:   (patientId, data) => medicalClient.post(`/patient/${patientId}/antecedents`, data),

  // Ordonnances
  addOrdonnance:   (patientId, data) => medicalClient.post(`/patient/${patientId}/ordonnances`, data),

  // Analyses
  addAnalyse:      (patientId, data) => medicalClient.post(`/patient/${patientId}/analyses`, data),

  // Radiologies
  addRadiologie:   (patientId, data) => medicalClient.post(`/patient/${patientId}/radiologies`, data),

  // Documents
  getDocuments:    (patientId)        => medicalClient.get(`/patient/${patientId}/documents`),
  uploadDocument:  (patientId, fd)    => medicalClient.post(`/patient/${patientId}/documents`, fd,
                                           { headers: { 'Content-Type': 'multipart/form-data' } }),
  deleteDocument:  (docId)            => medicalClient.delete(`/documents/${docId}`),
  getDocumentUrl:  (docId)            => `/api/v1/medical-records/documents/${docId}/fichier`,

  // Messagerie
  getMessages:     (patientId)        => medicalClient.get(`/patient/${patientId}/messages`),
  sendMessage:     (patientId, data)  => medicalClient.post(`/patient/${patientId}/messages`, data),
  markAsRead:      (messageId)        => medicalClient.put(`/messages/${messageId}/lu`),
}
