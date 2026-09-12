import { defineConfig } from '@playwright/test'

/**
 * E2E del sitio contra el build de SSR ya hecho (`yarn build` primero). Sirve para las
 * capturas del catálogo y para el humo de las páginas prerenderizadas.
 */
export default defineConfig({
  testDir: 'pruebas/e2e',
  testMatch: [/.*\.spec\.ts/, /.*\.e2e\.ts/],
  outputDir: '../../.playwright/web',
  reporter: [['list']],
  use: { baseURL: 'http://127.0.0.1:4173', colorScheme: 'light' },
  webServer: { command: 'PORT=4173 node dist/web/server/server.mjs', url: 'http://127.0.0.1:4173/', reuseExistingServer: true, timeout: 30_000 },
  projects: [{ name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1280, height: 900 } } }],
})
