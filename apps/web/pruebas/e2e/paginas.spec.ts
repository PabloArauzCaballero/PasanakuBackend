import { expect, test } from '@playwright/test'

/** Humo de las páginas públicas: cargan, tienen título y un solo `h1`. */
test.describe('páginas del sitio', () => {
  for (const ruta of ['/', '/plazos']) {
    test(`${ruta} responde con h1 y título`, async ({ page }) => {
      const r = await page.goto(ruta)
      expect(r?.status()).toBe(200)
      await expect(page).toHaveTitle(/AportaYa/)
      await expect(page.locator('h1')).toHaveCount(1)
    })
  }
})
