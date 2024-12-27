import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import dotenv from 'dotenv'

dotenv.config()

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000
  },
  define: {
    'process.env': {
      VITE_MONGODB_URI: JSON.stringify(process.env.VITE_MONGODB_URI),
      VITE_MONGODB_DB_NAME: JSON.stringify(process.env.VITE_MONGODB_DB_NAME)
    }
  },
  resolve: {
    alias: {
      'mongodb-client': 'mongodb/lib/mongodb_client'
    }
  },
  optimizeDeps: {
    include: ['mongodb']
  }
})
