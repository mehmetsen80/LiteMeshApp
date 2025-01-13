import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import fs from 'fs';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Load env file based on `mode` in the current working directory.
  const env = loadEnv(mode, process.cwd(), '');

  // Get API Gateway URL from env with fallback
  const apiGatewayUrl = env.VITE_API_GATEWAY_URL || 'https://localhost:7777';
  console.log('API Gateway URL:', apiGatewayUrl);

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
          target: apiGatewayUrl,
          secure: false,
          changeOrigin: true,
          ws: true,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error', err);
            });
          }
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