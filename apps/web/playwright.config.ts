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
  // Dos servidores, en este orden. El segundo es el sitio; el primero es la API
  // simulada, y no es un lujo: `/publico/sorteos/:id` se renderiza en el servidor CON
  // DATOS —es una ruta `Server`, no prerenderizada—, asi que sin backend la pagina
  // responde 200 con el estado de error y la prueba no encuentra el veredicto. Fallaba
  // solo en el CI porque ahi no hay nada escuchando en 4010; en la maquina de quien
  // tuviera el mock levantado, pasaba.
  //
  // `@aportaya/simulado` es prism sobre los catorce contratos con sus ejemplos, que es
  // justamente lo que esta prueba necesita: datos que cumplen el contrato, sin levantar
  // el backend entero.
  webServer: [
    {
      command: 'yarn workspace @aportaya/simulado mock',
      port: 4010,
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      // Las DOS variables, como en el despliegue. `APORTAYA_GATEWAY=/api/v1` es la misma
      // direccion relativa para el servidor y para el navegador, y eso no es cosmetico: la
      // cache que el SSR transfiere esta indexada por URL, asi que con una URL distinta en
      // cada lado el navegador no la encuentra, el bloque vuelve a pedir los datos, el @if
      // se evalua vacio mientras llegan y el DOM deja de coincidir — NG0502 sobre la
      // <section> del veredicto, que es exactamente lo que esta prueba vigila.
      command:
        'PORT=4173 APORTAYA_GATEWAY=/api/v1 APORTAYA_GATEWAY_INTERNO=http://127.0.0.1:4010 node dist/web/server/server.mjs',
      url: 'http://127.0.0.1:4173/',
      reuseExistingServer: true,
      timeout: 30_000,
    },
  ],
  projects: [{ name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1280, height: 900 } } }],
})
