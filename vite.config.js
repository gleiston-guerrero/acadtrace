import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  css: {
    postcss: './postcss.config.js',
  },
  server: {
    // Puerto fijo del portal de Soporte (coincide con la config de handoff del
    // SGA Principal: MICROSERVICIOS.SOPORTE_TECNICO). Se evita el 6000 porque
    // Chrome lo bloquea como puerto inseguro (ERR_UNSAFE_PORT, X11). Proxy de /api al backend.
    port: 5177,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:5178',
    },
  },
  build: {
    outDir: 'dist',
  },
})
