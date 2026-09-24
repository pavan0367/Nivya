import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'https://nivya-blbf.onrender.com',
        changeOrigin: true,
        secure: false,
      },
      '/ws': {
        target: 'https://nivya-blbf.onrender.com',
        ws: true,
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
