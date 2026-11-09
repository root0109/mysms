import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    restoreMocks: true,
    testTimeout: 20_000,
    coverage: {
      provider: 'v8',
      reporter: ['text-summary'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/generated/**', 'src/**/*.test.{ts,tsx}', 'src/index.ts'],
      // ui/05 §12: core keeps 90 % line coverage
      thresholds: { lines: 90, functions: 90, statements: 90, branches: 80 },
    },
  },
});
