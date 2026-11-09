import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// In development /v1 is proxied to a real backend (the monolith on :8080 by default; MYSMS_API_PROXY overrides it).
// There is no mock backend outside the tests: fakes live only in *.test.* files (ui/05 §13).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/v1': { target: process.env.MYSMS_API_PROXY ?? 'http://localhost:8080', changeOrigin: true } },
  },
  build: {
    sourcemap: true,
    target: 'es2022',
    rollupOptions: {
      output: {
        // framework code changes rarely: its own long-cached chunk; screens are lazy chunks (ui/05 §10)
        manualChunks: (id) =>
          /node_modules\/\.pnpm\/(react|react-dom|scheduler|react-router|@tanstack|zod|react-hook-form|@hookform|intl-messageformat|@formatjs|openapi-fetch|uuid|zustand)@/.test(
            id,
          )
            ? 'vendor'
            : undefined,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
    restoreMocks: true,
    css: false,
    testTimeout: 20_000,
  },
});
