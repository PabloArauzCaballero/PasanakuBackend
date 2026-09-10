// @ts-check
import eslint from '@eslint/js'
import tseslint from 'typescript-eslint'
import angular from 'angular-eslint'

export default tseslint.config(
  { ignores: ['dist/**', '.angular/**', 'node_modules/**'] },
  {
    files: ['**/*.ts'],
    extends: [eslint.configs.recommended, ...tseslint.configs.recommended, ...angular.configs.tsRecommended],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'ap', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: ['ap', 'app'], style: 'kebab-case' }],
      // Invariante 1: la red vive en nucleo/ y dominio/. Se refuerza con verificar_frontend.py.
      'no-restricted-globals': ['error', { name: 'fetch', message: 'La red pasa por HttpClient en nucleo/ y dominio/.' }],
      'no-console': 'error',
    },
  },
  {
    // El arranque y el servidor de SSR reportan por consola: no hay otro canal antes de que exista la app.
    files: ['src/main.ts', 'src/server.ts', 'src/main.server.ts'],
    rules: { 'no-console': 'off' },
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended, ...angular.configs.templateAccessibility],
    rules: {
      // Accesibilidad como ERROR: una advertencia de accesibilidad es una advertencia que nadie lee.
      '@angular-eslint/template/click-events-have-key-events': 'error',
      '@angular-eslint/template/interactive-supports-focus': 'error',
      '@angular-eslint/template/label-has-associated-control': 'error',
      '@angular-eslint/template/alt-text': 'error',
    },
  },
)
