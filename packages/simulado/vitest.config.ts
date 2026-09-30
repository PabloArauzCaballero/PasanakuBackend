import { defineConfig } from 'vitest/config'
// `src/**/*.spec.ts` se agrega para `backoffice-sistemas/adaptador.spec.ts` (H2.S1.M3):
// antes solo corrían los specs de `pruebas/` (los que validan ejemplos contra esquema).
export default defineConfig({ test: { globals: true, include: ['pruebas/**/*.spec.ts', 'src/**/*.spec.ts'] } })
