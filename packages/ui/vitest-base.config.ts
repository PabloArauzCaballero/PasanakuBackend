import { defineConfig } from 'vitest/config'

/**
 * H-5 (hallazgo del carril de continuación, ver PR8-carril.md): en esta clase de entorno
 * (Node 24.18.1 + Vitest 4.1.11 en Windows) el pool por defecto de Vitest (`forks`) revienta
 * al arrancar (`Error: Worker exited unexpectedly`), y con eso `@angular/build:unit-test`
 * nunca corre un solo test — no es un problema de ESTOS specs, es el proceso hijo el que
 * muere antes de ejecutar nada. `pool: 'threads'` con `fileParallelism: false` evita spawnear
 * el proceso hijo que revienta. Activado vía `runnerConfig: true` en `angular.json` (`test`).
 */
export default defineConfig({
  test: {
    pool: 'threads',
    fileParallelism: false,
  },
})
