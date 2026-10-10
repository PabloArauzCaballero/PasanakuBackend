import { defineConfig } from '@playwright/test'
import path from 'node:path'
import { BASE_WEB, EVIDENCIA, MODO } from '../../../../backoffice/e2e/aceptacion/apoyo/entorno'

/**
 * Lado A (participante) en el sitio web: estados de la página de invitación, higiene del secreto, viewports y temas, axe.
 * Los servidores los levanta `apps/backoffice/e2e/aceptacion/scripts/aceptacion.sh`. Un worker, sin reintentos.
 *
 *   BASE_WEB=http://127.0.0.1:5448 playwright test -c pruebas/e2e/aceptacion/playwright.aceptacion.config.ts
 */
export default defineConfig({
  testDir: '.',
  testMatch: /.*\.aceptacion\.e2e\.ts/,
  outputDir: path.join(EVIDENCIA, 'trazas', `web-${MODO}`),
  reporter: [['list'], ['json', { outputFile: path.join(EVIDENCIA, `resultado-web-${MODO}.json`) }]],
  workers: 1,
  fullyParallel: false,
  retries: 0,
  use: { baseURL: BASE_WEB, trace: 'retain-on-failure', screenshot: 'only-on-failure', colorScheme: 'light', browserName: 'chromium' },
})
