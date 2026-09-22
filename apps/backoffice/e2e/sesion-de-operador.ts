import { expect, type Page } from '@playwright/test'

/**
 * Autentica contra el simulado (Prism) por la pantalla REAL de ingreso, en vez de
 * inyectar el token a mano (H5.S1.M2, madre H8.S1.M2, PR13-Ci.Frontend). Antes de
 * esto, la única prueba que abría sesión (`tutoriales.e2e.ts`) tenía su propio
 * `entrar()` repetido a mano; esto lo saca a un lugar común para que cualquier
 * escenario nuevo del backoffice lo reutilice.
 *
 * **Por qué la cabecera `Prefer: example=vacio`.** El escenario por omisión de
 * `packages/simulado/ejemplos/identidad/autenticar.json` (`ok`) devuelve siempre
 * `requiereFactorAdicional: true`, también cuando la petición ya trae el factor:
 * contra ese escenario la pantalla de ingreso pide el código en bucle y nunca abre
 * sesión. Prism elige el escenario con esa cabecera, y `vacio` es el que responde
 * con la sesión ya abierta. No se cambia el ejemplo `ok` porque lo usan las pruebas
 * de contrato de la app (`apps/movil/test/identidad/autenticar_test.dart`).
 *
 * **Por qué no queda sesión con permisos de sección.** El token de ese escenario no
 * trae claims, así que quien entra por acá ve exactamente lo que un operador sin
 * rol asignado vería. Es el mismo límite que ya documentaba `tutoriales.e2e.ts`:
 * alcanza para lo que hoy se puede probar sin una pantalla de login por rol
 * (bloqueado, ver `tablero-y-permisos.e2e.ts`).
 */
export async function sesionDeOperador(
  page: Page,
  credenciales: { telefono?: string; clave?: string } = {},
): Promise<void> {
  await page.setExtraHTTPHeaders({ Prefer: 'example=vacio' })
  await page.goto('/ingreso')
  await page.getByRole('textbox', { name: 'Teléfono' }).fill(credenciales.telefono ?? '71234567')
  await page
    .getByRole('textbox', { name: 'Contraseña' })
    .fill(credenciales.clave ?? 'una-contrasena-larga')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page).toHaveURL(/\/tablero$/)
}
