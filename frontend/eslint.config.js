import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';

export default [
  {
    ignores: [
      'dist',
      'node_modules',
      'public/*.js',           // Browser globals (config.js, theme-init.js) — no TS project scope
      'clean-orval.mjs',       // Node.js utility script — uses console, not in TS project
      'orval.config.ts',       // Orval config — not included in tsconfig project
      'src/api/generated/**',  // Auto-generated API code — not manually maintained
    ],
  },

  // Base JS recommended rules
  js.configs.recommended,

  // TypeScript recommended configs (spread because it's an array)
  ...tseslint.configs.recommended,

  {
    files: ['**/*.{ts,tsx}'],

    languageOptions: {
      ecmaVersion: 2020,
      parser: tseslint.parser,
      parserOptions: {
        project: true,
      },
    },

    plugins: {
      'react-hooks': reactHooks,
    },

    rules: {
      // React Hooks recommended rules
      ...reactHooks.configs.recommended.rules,

      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',

      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
];