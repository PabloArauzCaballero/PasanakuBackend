import js from '@eslint/js'
import tseslint from 'typescript-eslint'
export default tseslint.config(
  { ignores: ['generado/**', 'node_modules/**', 'ejemplos/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  { files: ['scripts/**/*.mjs'], languageOptions: { globals: { URL: 'readonly', process: 'readonly', console: 'readonly' } } },
)
