import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// The dev server proxies API calls to the Spring Boot backend on :8080, so the browser sees a
// single origin in development (no CORS needed). In production, set VITE_API_BASE_URL or serve the
// built assets behind the same host as the API.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
})
