import { defineConfig, type Plugin } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import zlib from 'node:zlib'
import fs from 'node:fs'
import { visualizer } from "rollup-plugin-visualizer";

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

// Configure React Compiler preset to only process src files and skip node_modules in native Rust
const compilerPreset = reactCompilerPreset();
compilerPreset.rolldown.filter = {
  ...compilerPreset.rolldown.filter,
  id: {
    include: [/[\\/]src[\\/].*\.[jt]sx?$/],
    exclude: [/[\\/]node_modules[\\/]/],
  },
};

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    react(),
    babel({ presets: [compilerPreset] }),
    precompressPlugin(),
    visualizer({
      filename: 'stats.html',
      open: false,
      gzipSize: true,
      brotliSize: true,
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          const normalized = id.replace(/\\/g, '/');

          // 1. Group shared API services into a single clean chunk (eliminate <1KB micro-chunks)
          if (normalized.includes('/src/api/')) {
            return 'api-services';
          }

          if (normalized.includes('/node_modules/')) {
            // Extract the real package name from the last '/node_modules/'
            // (Crucial for pnpm virtual stores where peer dependency dirs contain other package names)
            const afterNodeModules = normalized.split('/node_modules/').pop() || '';
            const pkgParts = afterNodeModules.split('/');
            const pkgName = afterNodeModules.startsWith('@')
              ? `${pkgParts[0]}/${pkgParts[1]}`
              : pkgParts[0];

            // 2. Leaflet & GIS libraries (isolated from core React)
            if (
              pkgName === 'leaflet' ||
              pkgName === 'leaflet.markercluster' ||
              pkgName === 'react-leaflet' ||
              pkgName === '@react-leaflet/core'
            ) {
              return 'vendor-leaflet';
            }

            // 3. Lucide icons
            if (pkgName === 'lucide-react') {
              return 'vendor-icons';
            }

            // 4. Data fetching: TanStack Query + Axios
            if (
              pkgName === '@tanstack/react-query' ||
              pkgName === '@tanstack/query-core' ||
              pkgName === 'axios'
            ) {
              return 'vendor-query';
            }

            // 5. Core React & Router runtime
            if (
              pkgName === 'react' ||
              pkgName === 'react-dom' ||
              pkgName === 'react-router' ||
              pkgName === 'react-router-dom' ||
              pkgName === 'scheduler'
            ) {
              return 'vendor-react';
            }
          }
        },
      },
    },
    chunkSizeWarningLimit: 600,
  },
})