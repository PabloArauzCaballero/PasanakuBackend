import { defineConfig } from '@playwright/test'

/**
 * E2E del backoffice contra el servidor de desarrollo (`ng serve`) y el mock de
 * Prism (`@aportaya/simulado`, puerto 4010) — no hay backend real desplegado en este
 * sandbox (carril F12, informe `planes/informes/carril-F12.md`).
 *
 * H5.S1.M1 (PR13-Ci.Frontend, kill-test #2 del carril): esto ANTES decía "se levanta
 * a mano" y no traía `webServer` — el CI, que no tiene manos, no podía correr esto
 * nunca, y por eso el paso ni existía en `.github/workflows/ci.yml`. Ahora levanta
 * los DOS procesos por configuración, igual que ya hacía
 * `apps/web/playwright.config.ts` para el sitio — el mismo patrón, no uno nuevo.
 *
 * `reuseExistingServer: !process.env.CI`: en CI NUNCA reutiliza un proceso que
 * hubiera quedado de una corrida anterior (regla del H5.S1.M1 — "en CI nunca
 * reutiliza"); en una máquina de desarrollo, sí, para no relanzar `ng serve` en cada
 * corrida de la suite mientras se itera.
 */
export default defineConfig({
  testDir: 'e2e',
  testMatch: /.*\.e2e\.ts/,
  outputDir: '../../.playwright/backoffice',
  reporter: [['list']],
  workers: 1,
  retries: 1,
  use: { baseURL: 'http://127.0.0.1:4300', colorScheme: 'light' },
  webServer: [
    {
      command: 'yarn workspace @aportaya/simulado mock',
      port: 4010,
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
    {
      command: 'yarn workspace @aportaya/backoffice start --port 4300 --host 127.0.0.1 --proxy-config proxy.e2e.json',
      url: 'http://127.0.0.1:4300/tablero',
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
  ],
  projects: [{ name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1280, height: 900 } } }],
})
