import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Load env file based on `mode` in the current working directory.
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:5000',
          changeOrigin: true,
          secure: false,
        }
      },
      cors: true,
      open: true,
    },
    build: {
      outDir: 'dist',
      sourcemap: true,
      // Prevent bundling of certain imported packages
      commonjsOptions: {
        include: [/node_modules/],
      },
    },
    define: {
      'process.env': env
    },
    optimizeDeps: {
      include: ['react', 'react-dom'],
    },
    css: {
      // CSS preprocessing
      preprocessorOptions: {
        scss: {
          additionalData: `@import "@/assets/styles/variables.scss";`
        }
      }
    },
    // Environment variables that should be available in the app
    envPrefix: 'VITE_',
  };
});