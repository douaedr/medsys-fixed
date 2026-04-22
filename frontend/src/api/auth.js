import { authClient, adminClient } from './client'

export const authApi = {
  login:          (data)  => authClient.post('/login', data),
  register:       (data)  => authClient.post('/register', data),
  forgotPassword: (email) => authClient.post('/forgot-password', { email }),
  resetPassword:  (data)  => authClient.post('/reset-password', data),
  changePassword: (data)  => authClient.post('/change-password', data),
  verifyEmail:    (token) => authClient.get(`/verify?token=${token}`),
  refresh:        (token) => authClient.post(`/refresh?refreshToken=${token}`),
  me:             ()      => authClient.get('/me'),
  verifyToken:    (token) => authClient.get(`/verify-token?token=${token}`),
}

export const adminApi = {
  createPersonnel: (data) => adminClient.post('/personnel', data),
  listUsers:       ()     => adminClient.get('/users'),
  listByRole:      (role) => adminClient.get(`/users/role/${role}`),
  toggleUser:      (id)   => adminClient.put(`/users/${id}/toggle`),
  deleteUser:      (id)   => adminClient.delete(`/users/${id}`),
}
