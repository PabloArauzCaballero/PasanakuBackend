import { defineConfig } from '@playwright/test'

/**
 * E2E del backoffice contra el servidor de desarrollo (Vite/`ng serve`) y el mock de
 * Prism (`yarn dev:mock`, puerto 4010) — no hay backend real desplegado en este sandbox
 * (carril F12, informe `planes/informes/carril-F12.md`). `reuseExistingServer: true`
 * porque en CI el runner ya deja levantados `dev:mock` y `dev:backoffice`.
 */
export default defineConfig({
  testDir: 'e2e',
  testMatch: /.*\.e2e\.ts/,
  outputDir: '../../.playwright/backoffice',
  reporter: [['list']],
  workers: 1,
  retries: 1,
  use: { baseURL: 'http://127.0.0.1:4300', colorScheme: 'light' },
  // Sin `webServer`: en este sandbox se levanta a mano con
  // `yarn workspace @aportaya/backoffice start --port 4300` (ver informe del carril).
  // `reuseExistingServer` con `webServer` reintentaba levantar un segundo proceso y
  // chocaba con el puerto ya ocupado.
  projects: [{ name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1280, height: 900 } } }],
})
