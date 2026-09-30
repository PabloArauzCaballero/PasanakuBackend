import { defineConfig } from 'vitest/config'

/** Ver `packages/ui/vitest-base.config.ts` — mismo hallazgo (H-5), mismo remedio. */
export default defineConfig({
  test: {
    pool: 'threads',
    fileParallelism: false,
  },
})
