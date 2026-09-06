import { defineConfig, type Plugin } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import zlib from 'node:zlib'
import fs from 'node:fs'

function precompressPlugin(): Plugin {
  return {
    name: 'vite-plugin-precompress',
    apply: 'build',
    closeBundle() {
      const distDir = path.resolve(__dirname, 'dist')
      if (!fs.existsSync(distDir)) return

      const compressDir = (dir: string) => {
        const entries = fs.readdirSync(dir, { withFileTypes: true })
        for (const entry of entries) {
          const fullPath = path.join(dir, entry.name)
          if (entry.isDirectory()) {
            compressDir(fullPath)
          } else if (
            /\.(js|css|html|svg|json)$/.test(entry.name) &&
            !entry.name.endsWith('.gz') &&
            !entry.name.endsWith('.br')
          ) {
            const content = fs.readFileSync(fullPath)
            if (content.length < 1024) continue // Skip tiny files (< 1KB)

            // Gzip (.gz) - max level 9
            const gz = zlib.gzipSync(content, { level: 9 })
            fs.writeFileSync(`${fullPath}.gz`, gz)

            // Brotli (.br) - max quality 11
            const br = zlib.brotliCompressSync(content, {
              params: {
                [zlib.constants.BROTLI_PARAM_QUALITY]: 11,
              },
            })
            fs.writeFileSync(`${fullPath}.br`, br)
          }
        }
      }

      compressDir(distDir)
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    react(),
    babel({ presets: [reactCompilerPreset()] }),
    precompressPlugin(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('leaflet') || id.includes('react-leaflet')) {
              return 'vendor-leaflet';
            }
            if (id.includes('react') || id.includes('react-dom') || id.includes('react-router-dom')) {
              return 'vendor-react';
            }
            if (id.includes('@tanstack/react-query') || id.includes('axios')) {
              return 'vendor-query';
            }
            if (id.includes('lucide-react')) {
              return 'vendor-icons';
            }
          }
        },
      },
    },
    chunkSizeWarningLimit: 600,
  },
})

