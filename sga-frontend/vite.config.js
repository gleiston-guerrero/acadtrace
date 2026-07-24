import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    // El SGA Principal es el login único: puerto fijo para que los microservicios
    // (docente/secretaría/soporte) siempre sepan a dónde redirigir el SSO.
    port: 5173,
    strictPort: true,
  },
})