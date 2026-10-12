import { expect, test } from '@playwright/test'
import path from 'node:path'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { todas } from './apoyo/capturas-f2'

/**
 * H17 · pago a proveedor sin «QR» (carril R, ADR-052 DD-6): la base solo admite CHEQUE, EFECTIVO y TRANSFERENCIA
 * (`ck_pago_a_proveedor_forma_pago`), así que el formulario no ofrece una forma que el servidor rechazaría.
 *
 * `VERIFIED_FUNCTIONAL_ONLY` contra Prism (el mock de `@aportaya/simulado`): la sesión es un token de prueba y la lista de
 * facturas es el ejemplo del contrato. Capturas por viewport y tema en `evidencia/carril-H17/capturas-backoffice/`.
 */
test.describe('H17 · pago a proveedor', () => {
  test('el formulario ofrece solo las formas que la base admite, en 3 viewports y 2 temas', async ({ page }) => {
    const errores: string[] = []
    page.on('pageerror', (e) => errores.push(e.message))
    page.on('console', (m) => m.type() === 'error' && errores.push(m.text()))

    // El listado `GET /erp/facturas-de-proveedor` todavia no esta en el contrato (hueco declarado en `contrato-erp.ts`): doble.
    const bs = (monto: string) => ({ monto, moneda: 'BOB' })
    const factura = (n: string, estado: string, saldo: string, pagado: string) => ({
      facturaProveedorId: `a0000000-0000-4000-8000-00000000000${n}`, numeroFactura: `F-2026-00${n}`, terceroRazonSocial: 'Proveedor sintetico',
      ordenCompraId: null, fechaEmision: '2026-10-01', fechaVencimiento: '2026-10-31', estado, monto: bs('1500.00'), montoPagado: bs(pagado),
      saldoPendiente: bs(saldo), aprobadaPor: null, aprobadaPorLaSesion: false, asientoContableId: null,
    })
    await page.route('**/erp/facturas-de-proveedor', (r) =>
      r.request().method() === 'GET'
        ? r.fulfill({ status: 200, json: [factura('1', 'APROBADA', '1500.00', '0.00'), factura('2', 'PAGADA', '0.00', '1500.00')] })
        : r.fallback(),
    )
    await page.route('**/erp/ordenes-de-compra', (r) => (r.request().method() === 'GET' ? r.fulfill({ status: 200, json: [] }) : r.fallback()))
    await iniciarSesionDePrueba(page, ['CONTABILIDAD_ERP_COMPRAS', 'CONTABILIDAD_ERP_PAGAR'], 'tesoreria')
    await navegarSinRecargar(page, '/contabilidad/compras')
    await page.getByRole('tab', { name: 'Facturas de proveedor' }).click()
    await page.getByRole('button', { name: 'Registrar el pago' }).first().click()

    const dialogo = page.getByRole('dialog')
    await expect(dialogo).toBeVisible()
    const formas = dialogo.getByRole('radio')
    await expect(formas).toHaveCount(3)
    await expect(dialogo.getByRole('radio', { name: /transferencia/i })).toBeChecked()
    await expect(dialogo.getByRole('radio', { name: /^QR$/i })).toHaveCount(0)
    await expect(dialogo.getByText(/QR/)).toHaveCount(0)

    await todas(page, 'bo', 'pago-a-proveedor')
    expect(errores, 'consola sin errores').toEqual([])
  })
})
