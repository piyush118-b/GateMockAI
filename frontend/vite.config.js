import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    {
      name: 'remove-crossorigin',
      transformIndexHtml(html) {
        return html.replace(/\s+crossorigin(?:="anonymous"|="use-credentials"|"")?/g, '')
      }
    }
  ],
  server: {
    proxy: {
      // Proxy all API calls and auth endpoints to Spring Boot
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
      '/login': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
      '/logout': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
      '/uploads': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    assetsDir: 'assets',
    rollupOptions: {
      output: {
        entryFileNames: 'assets/[name].js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name].[ext]'
      }
    }
  }
})
