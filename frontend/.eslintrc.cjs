/* ESLint 8 (legacy config). Run with `npm run lint`. */
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  plugins: ['react-refresh'],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', 'node_modules', '.eslintrc.cjs'],
  overrides: [
    {
      // By convention (CLAUDE.md) a provider and its hook (useAuth, useToast, ...) live in the same file.
      files: ['src/auth/AuthProvider.tsx', 'src/components/ToastProvider.tsx', 'src/components/CollectionRefresh.tsx'],
      rules: { 'react-refresh/only-export-components': 'off' },
    },
  ],
  rules: {
    // Vite fast refresh: files should export only components.
    'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    // CLAUDE.md: no `any` unless a comment explains why.
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    // CLAUDE.md: notifications are toasts, never browser dialogs.
    'no-alert': 'error',
  },
};
