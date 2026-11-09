// One lint configuration for the whole workspace. Encodes the import rules of ui/05 §1:
//   apps/* and the kits use packages only through @mysms/core hooks (never the SDK directly);
//   core imports SDK values per class, never the root namespaces (React Native bundles stay small);
//   ui-* packages may import only tokens + i18n; nothing imports fixtures outside tests (mocking only in tests).
import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import globals from 'globals';

const sdkThroughCore = {
  name: '@mysms/sdk',
  message: 'Components and apps never call the SDK directly — use the @mysms/core hooks (ui/05 §1).',
};
// Modules only tests may use: the fake backend, test runners and every mocking library.
const TEST_ONLY =
  '/^(@mysms\\/fixtures|vitest|@vitest\\/|@testing-library\\/|msw|nock|sinon|fetch-mock|miragejs|jest-mock|@jest\\/)/';
const sdkPatterns = { group: ['@mysms/sdk/*'], message: sdkThroughCore.message };
const noFixtures = {
  name: '@mysms/fixtures',
  message: 'Fakes and fixtures are for tests only: the apps always talk to a real backend (ui/05 §13).',
};

export default tseslint.config(
  {
    ignores: ['**/dist/**', '**/coverage/**', '**/node_modules/**', '**/src/generated/**', '**/.expo/**'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
    rules: {
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      '@typescript-eslint/consistent-type-imports': 'error',
      'no-console': ['error', { allow: ['warn', 'error'] }],
      eqeqeq: ['error', 'always'],
    },
  },
  {
    files: ['**/*.{js,mjs,cjs}'],
    languageOptions: { globals: { ...globals.node } },
    rules: { 'no-console': ['error', { allow: ['warn', 'error'] }] },
  },
  {
    files: ['**/*.tsx'],
    plugins: { 'react-hooks': reactHooks, 'react-refresh': reactRefresh },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
  {
    files: ['apps/**/*.{ts,tsx}', 'packages/web-kit/**/*.{ts,tsx}', 'packages/native-kit/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}', '**/src/test/**'],
    rules: {
      'no-restricted-imports': ['error', { paths: [sdkThroughCore, noFixtures], patterns: [sdkPatterns] }],
    },
  },
  {
    files: ['packages/ui-web/**/*.{ts,tsx}', 'packages/ui-native/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: '@mysms/core',
              message: 'ui-* may import only @mysms/tokens and @mysms/i18n (ui/05 §1).',
            },
            sdkThroughCore,
            noFixtures,
          ],
          patterns: [sdkPatterns],
        },
      ],
    },
  },
  {
    files: ['packages/core/src/**/*.{ts,tsx}', 'packages/i18n/src/**/*.ts', 'packages/tokens/src/**/*.ts'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: { 'no-restricted-imports': ['error', { paths: [noFixtures] }] },
  },
  {
    // core takes SDK *values* from per-class paths (`@mysms/sdk/master/apis/CountryApi`, `@mysms/sdk/runtime`):
    // Metro does not tree-shake, so a namespace value import would put every module's client in the app bundle.
    files: ['packages/core/src/**/*.{ts,tsx}'],
    ignores: ['**/*.test.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: '@mysms/sdk',
              allowTypeImports: true,
              message: 'Import SDK values from @mysms/sdk/<module>/apis/<Api> or @mysms/sdk/runtime.',
            },
          ],
        },
      ],
    },
  },
  {
    // Mocking only in tests (project rule): shipping code never imports fakes, fixtures, test runners or mocking
    // libraries (statically, dynamically or through require/re-export) and never touches the vi/jest globals.
    files: ['**/*.{ts,tsx,js,mjs,cjs}'],
    ignores: [
      '**/*.test.{ts,tsx}',
      '**/src/test-setup.ts',
      '**/src/test/**',
      'packages/fixtures/**',
      'packages/config/vitest.preset.mjs',
      '**/vite.config.ts',
    ],
    rules: {
      'no-restricted-syntax': [
        'error',
        ...['ImportDeclaration', 'ImportExpression', 'ExportAllDeclaration', 'ExportNamedDeclaration'].map(
          (node) => ({
            selector: `${node}[source.value=${TEST_ONLY}]`,
            message: noFixtures.message,
          }),
        ),
        {
          selector: `CallExpression[callee.name='require'][arguments.0.value=${TEST_ONLY}]`,
          message: noFixtures.message,
        },
        {
          selector: 'MemberExpression[object.name=/^(vi|jest)$/]',
          message: 'Mocks belong in tests only.',
        },
      ],
    },
  },
);
