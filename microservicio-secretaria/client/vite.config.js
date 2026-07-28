import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5174, // cualquier puerto libre para el dev server del cliente
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:5176',
    },
  },
  build: {
    outDir: 'dist',
  },
})