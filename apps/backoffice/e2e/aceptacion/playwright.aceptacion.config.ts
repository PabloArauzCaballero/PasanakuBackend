import { defineConfig } from '@playwright/test'
import path from 'node:path'
import { EVIDENCIA, MODO } from './apoyo/entorno'

/**
 * Suite de aceptación y rechazo con DOS usuarios (A = participante en el sitio web, B = operador de backoffice).
 * Un solo worker, un solo Chromium, dos `browser.newContext` aislados. Sin reintentos (regla 80.5.3).
 * No levanta servidores de aplicación: los levanta `scripts/aceptacion.sh` (uno por vez, bajo el guardián de RAM).
 *
 *   ACEPTACION_MODO=mock|real  BASE_API=... BASE_WEB=... BASE_BO=...  playwright test -c e2e/aceptacion/playwright.aceptacion.config.ts
 */
export default defineConfig({
  testDir: '.',
  testMatch: /.*\.aceptacion\.e2e\.ts/,
  outputDir: path.join(EVIDENCIA, 'trazas', MODO),
  reporter: [['list'], ['json', { outputFile: path.join(EVIDENCIA, `resultado-${MODO}.json`) }]],
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  globalSetup: './apoyo/global-setup.ts',
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    colorScheme: 'light',
    browserName: 'chromium',
    // Chromium reciente bloquea el WebSocket de HMR de Vite entre 127.0.0.1 y 127.0.0.1: es del entorno de prueba, no del producto.
    launchOptions: { args: ['--disable-features=LocalNetworkAccessChecks'] },
  },
})
