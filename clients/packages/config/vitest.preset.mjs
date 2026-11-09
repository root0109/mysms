// Shared Vitest settings: jsdom for React code, coverage via v8, deterministic timers off by default.
export const vitestPreset = {
  test: {
    environment: 'jsdom',
    globals: false,
    restoreMocks: true,
    coverage: { provider: 'v8', reporter: ['text-summary', 'lcov'] },
  },
};
