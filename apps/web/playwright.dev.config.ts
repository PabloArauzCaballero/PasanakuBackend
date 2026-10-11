import { defineConfig } from '@playwright/test'

/**
 * E2E del sitio contra el servidor de DESARROLLO (`ng serve`, puerto 4200), sin el build de SSR:
 * sirve para las pruebas que no necesitan datos del backend (p. ej. el enlace de invitación).
 * Los servidores se levantan aparte; esta configuración no lanza ninguno.
 *
 *   yarn workspace @aportaya/web start --port 4200 --host 127.0.0.1
 *   yarn workspace @aportaya/web playwright test -c playwright.dev.config.ts invitacion
 */
export default defineConfig({
  testDir: 'pruebas/e2e',
  testMatch: [/.*\.e2e\.ts/],
  outputDir: '../../.playwright/web-dev',
  reporter: [['list']],
  workers: 1,
  use: { baseURL: 'http://127.0.0.1:4200', colorScheme: 'light' },
  projects: [{ name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1280, height: 900 } } }],
})
