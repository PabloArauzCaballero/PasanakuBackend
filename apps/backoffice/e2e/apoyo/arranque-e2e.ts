import type { Page } from '@playwright/test'

/**
 * Lo que el servidor de desarrollo NO trae y el arranque del backoffice exige desde que el gateway falla cerrado y la sesión se restaura
 * al cargar (`provideAppInitializer(inicializarSesion())`):
 *  - la etiqueta `<meta name="aportaya-gateway">` en cada documento HTML (sin ella el gateway queda inválido y toda petición va a `/`);
 *  - una respuesta al refresco de sesión: `POST /sesion/refrescar` no existe en el contrato de `identidad` ni en Prism, así que se
 *    responde `401` limpio (como un navegador sin cookie) y el arranque termina en `ANONYMOUS`, no en `/arranque`.
 * Es un DOBLE del contrato que falta (regla 65); no toca código de producción.
 */
export async function prepararArranque(page: Page): Promise<void> {
  await page.route('**/*', async (route) => {
    if (route.request().resourceType() !== 'document') return route.fallback()
    const respuesta = await route.fetch()
    const html = await respuesta.text()
    return route.fulfill({ response: respuesta, body: html.replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
  })
  await page.route('**/sesion/refrescar', (r) => r.fulfill({ status: 401, json: {} }))
}
