import type { Page } from '@playwright/test'

/**
 * DOBLE DE PRUEBA — no es login real. Documentado explícitamente como tal (regla 65):
 * `nucleo/sesion.ts` guarda el token del operador SOLO en memoria, a propósito («un
 * token en localStorage lo lee cualquier script que llegue a la página» — ver su
 * comentario de cabecera), así que no existe forma de abrir sesión inyectando
 * `storageState`/cookies desde Playwright.
 *
 * SÍ existe una pantalla de login real (`/ingreso`, `PantallaDeIngreso`, CU-04) que
 * llama `POST /sesiones` en dos pasos. El hallazgo heredado del carril F12
 * ("no existe ninguna pantalla de login") describía el estado del código en el
 * commit `c4c7f03`; `2025a43 feat: el backoffice tiene por donde entrar` la agregó
 * después. Ejercitar el login real requeriría conocer las credenciales de ejemplo
 * que Prism devuelve para el contrato de `identidad`, que está fuera de esta pasada;
 * en su lugar, este helper toma la instancia YA MONTADA de `PantallaDeIngreso`
 * (que inyecta `Sesion` de verdad) usando la API de depuración que Angular expone
 * automáticamente en builds no-production (`window.ng`, ver
 * https://angular.dev/guide/testing/debugging via `ng.getComponent`), y llama el
 * método público que `nucleo/sesion.ts` ya expone: `Sesion.abrir(...)`.
 *
 * No se edita `nucleo/sesion.ts`, `nucleo/permisos.ts` ni `app.config.ts` — están
 * fuera de mi reserva de archivos. Esto vive enteramente en `e2e/`, es mío.
 */
export async function abrirSesionDePruebaYNavegar(page: Page, permisos: readonly string[], rol: string, ruta: string): Promise<void> {
  await page.goto('/ingreso')
  await page.waitForSelector('ap-pantalla-de-ingreso')

  await page.evaluate(
    ({ permisos, rol, ruta }) => {
      type Sesion = { abrir: (acceso: string, permisos: readonly string[], rol: string, sujeto?: string | null) => void }
      type Router = { navigateByUrl: (url: string) => void }
      type NgDebug = { getComponent: (el: Element) => (Record<string, unknown> & { sesion?: Sesion; router?: Router }) | null }

      const w = window as unknown as { ng?: NgDebug }
      if (!w.ng) {
        throw new Error('doble de sesión: window.ng no está disponible — ¿se está corriendo contra un build de producción?')
      }
      const el = document.querySelector('ap-pantalla-de-ingreso')
      if (!el) throw new Error('doble de sesión: no se encontró <ap-pantalla-de-ingreso> montado')

      const componente = w.ng.getComponent(el)
      if (!componente?.sesion || !componente.router) {
        throw new Error('doble de sesión: PantallaDeIngreso no expone sesion/router (¿cambió su implementación?)')
      }

      // Token de prueba: la firma nunca se verifica del lado del cliente (ver comentario
      // de `abrirConToken`), así que un valor arbitrario es suficiente para el doble.
      componente.sesion.abrir('token-doble-e2e', permisos, rol, 'e2e-doble')
      componente.router.navigateByUrl(ruta)
    },
    { permisos, rol, ruta },
  )

  await page.waitForURL((url) => url.pathname === ruta || url.pathname.startsWith(`${ruta}/`))
}
