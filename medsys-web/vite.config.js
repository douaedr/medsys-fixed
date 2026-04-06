import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1/patients':   { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/patient':    { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/directeur':  { target: 'http://localhost:8081', changeOrigin: true },
      '/api/appointments':  { target: 'http://localhost:5000', changeOrigin: true },
      '/api/slots':         { target: 'http://localhost:5000', changeOrigin: true },
      '/api/waiting-list':  { target: 'http://localhost:5000', changeOrigin: true },
      '/api/services':      { target: 'http://localhost:5000', changeOrigin: true },
      '/appointmenthub':    { target: 'http://localhost:5000', changeOrigin: true, ws: true },
      '/api': { target: 'http://localhost:8082', changeOrigin: true }
    }
  }
})
