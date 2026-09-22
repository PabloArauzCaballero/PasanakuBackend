import type { Page } from '@playwright/test'

/**
 * **DOBLE DE PRUEBA, no la sesión real** (PR7 §3, regla 65 del repo — "aislar y simular
 * para no bloquearse", solo cuando de verdad no se puede resolver). El hallazgo de
 * `tablero-y-permisos.e2e.ts` sobre "no hay pantalla de login" está desactualizado: CU-04
 * (`rutas/ingreso/pantalla-de-ingreso.ts`) SÍ existe y funciona, con MFA real de dos
 * pasos. Este helper la maneja de punta a punta — formulario real, dos POST reales a
 * `/sesiones`, `Sesion.abrirConToken()` real, guardas `canMatch` reales — así que el
 * login en sí queda cubierto igual que cualquier otra pantalla.
 *
 * Lo único sintético es el segundo `tokenAcceso`. `identidad` no tiene backend real
 * desplegado en este sandbox (carril F12 ya lo documentó): el ejemplo fijo de Prism para
 * `autenticar` (`packages/simulado/ejemplos/identidad/autenticar.json`) manda
 * `"tokenAcceso": "tokenAcceso"`, un string plano sin los tres segmentos de un JWT. Contra
 * ESE valor, `Sesion.abrirConToken()` → `leerClaims()` no encuentra payload que decodificar
 * y abre la sesión con `permisos: []` — ninguna pantalla protegida quedaría alcanzable, ni
 * siquiera con MFA completo de verdad. Se intercepta —con la API de red de Playwright, no
 * tocando código de producción— solo la SEGUNDA petición a `/sesiones` (la que ya trae
 * `factor`, el código del segundo paso) para servir un `tokenAcceso` con la forma de JWT
 * que un backend real emitiría, con los permisos que la prueba necesita. La primera
 * petición (usuario+contraseña, "hace falta el factor") corre sin tocar, tal como Prism la
 * sirve de verdad.
 */

/** Arma un string con la forma de JWT (`cabecera.payload.firma`) que `leerClaims()` puede decodificar. Sin firma real — es SOLO lo que el cliente lee para mostrar/ocultar menú; el servidor real nunca confía en esto (mismo comentario en `sesion.ts`). */
function tokenDePruebaConPermisos(permisos: readonly string[], rol: string, sujeto: string): string {
  const payload = { permisos, rol, sub: sujeto }
  const json = JSON.stringify(payload)
  const base64url = Buffer.from(json, 'utf8').toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `e2e-cabecera.${base64url}.e2e-firma`
}

/**
 * Ingresa por la pantalla real de CU-04 y termina con una sesión abierta con los
 * `permisos` pedidos. Deja al operador en `/tablero` (redirección real del componente
 * tras el login).
 */
export async function iniciarSesionDePrueba(page: Page, permisos: readonly string[], rol = 'oficial'): Promise<void> {
  // Solo la petición del SEGUNDO paso (la que trae `factor`, el código MFA) se intercepta;
  // la primera pasa intacta hacia Prism.
  await page.route('**/api/v1/sesiones', async (route) => {
    const cuerpo = route.request().postDataJSON() as { factor?: unknown }
    if (!cuerpo.factor) {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        sesionId: 'e2e-sesion',
        expiraEn: new Date(Date.now() + 3_600_000).toISOString(),
        requiereFactorAdicional: false,
        dispositivoConfiable: true,
        tokenAcceso: tokenDePruebaConPermisos(permisos, rol, 'e2e-operador'),
      }),
    })
  })

  await page.goto('/ingreso')
  await page.getByLabel('Teléfono').fill('71234567')
  await page.getByLabel('Contraseña', { exact: true }).fill('contrasena-e2e')
  await page.getByRole('button', { name: 'Continuar' }).click()

  // `ap-campo-otp` dispara `(completado)` sola al llegar a 6 dígitos, que ya llama a
  // `enviar()` — no hace falta (ni conviene) tocar el botón "Entrar" después de esto.
  await page.getByLabel('Código de verificación').fill('123456')

  await page.waitForURL(/\/tablero$/)
}

/**
 * Navega a `ruta` sin recargar la página: `page.goto()` es una navegación real del
 * navegador, y `Sesion.acceso` vive SOLO en memoria (`sesion.ts`, a propósito, por
 * seguridad) — una recarga la borra y cualquier ruta protegida caería en `/ingreso`, no
 * en la redirección de permiso que esta prueba quiere ejercitar. `pushState` + `popstate`
 * es el mismo evento que Angular Router ya escucha para atrás/adelante del navegador; acá
 * se dispara a mano para simular un click sin depender de que exista un link visible (como
 * en la pantalla sin el acceso, donde `@if (sesion.puede(...))` no lo renderiza).
 */
export async function navegarSinRecargar(page: Page, ruta: string): Promise<void> {
  await page.evaluate((r) => {
    history.pushState({}, '', r)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, ruta)
}
