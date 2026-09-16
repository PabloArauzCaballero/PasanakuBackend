import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

/**
 * Example Express Rest API endpoints can be defined here.
 * Uncomment and define endpoints as necessary.
 *
 * Example:
 * ```ts
 * app.get('/api/{*splat}', (req, res) => {
 *   // Handle API request
 * });
 * ```
 */

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
/**
 * En un entorno desplegado la API sale por el MISMO origen que el sitio.
 *
 * Sin esto el codigo del navegador no tiene a quien llamar: `gatewayPorDefecto()`
 * lee `<meta name="aportaya-gateway">`, y si falta cae a http://localhost:4010 —el
 * Prism de desarrollo—, que en el navegador de un visitante es su propia maquina.
 *
 * Solo se enciende con APORTAYA_GATEWAY_INTERNO (p. ej. http://gateway:8080): en
 * desarrollo no existe y el sitio se comporta como siempre.
 */
const gatewayInterno = process.env['APORTAYA_GATEWAY_INTERNO'];

if (gatewayInterno) {
  const SIN_REENVIO = new Set(['host', 'connection', 'content-length', 'transfer-encoding']);

  app.use('/api', async (req, res, next) => {
    try {
      const cabeceras = new Headers();
      for (const [clave, valor] of Object.entries(req.headers)) {
        if (valor === undefined || SIN_REENVIO.has(clave)) continue;
        cabeceras.set(clave, Array.isArray(valor) ? valor.join(', ') : valor);
      }
      const conCuerpo = req.method !== 'GET' && req.method !== 'HEAD';
      // `fetch` y no HttpClient: esto no es una vista sino el servidor Node reenviando la
      // petición al gateway. La regla existe para que ningún componente haga red.
      // eslint-disable-next-line no-restricted-globals
      const respuesta = await fetch(gatewayInterno + req.originalUrl, {
        method: req.method,
        headers: cabeceras,
        body: conCuerpo ? (req as unknown as ReadableStream) : undefined,
        // Obligatorio en Node al mandar un cuerpo en flujo.
        ...(conCuerpo ? { duplex: 'half' } : {}),
        redirect: 'manual',
      } as RequestInit);
      // fetch ya descomprimio el cuerpo: reenviar content-encoding haria que el
      // navegador intente descomprimir texto plano.
      respuesta.headers.delete('content-encoding');
      respuesta.headers.delete('content-length');
      await writeResponseToNodeResponse(respuesta, res);
    } catch (error) {
      next(error);
    }
  });
}

/**
 * Las etiquetas que el servidor agrega a cada página HTML, según el entorno:
 *   · `aportaya-gateway` — a dónde llama el código del navegador (con APORTAYA_GATEWAY_INTERNO);
 *   · `aportaya-app`     — dónde abrir la app en el navegador (con APORTAYA_URL_APP).
 * Por entorno y no en el código: son direcciones de un despliegue, no del producto.
 */
const urlDeLaApp = process.env['APORTAYA_URL_APP'];
const METAS = [
  gatewayInterno ? '<meta name="aportaya-gateway" content="/api/v1">' : '',
  urlDeLaApp && /^https?:\/\/[\w.-]+(:\d+)?\/?$/.test(urlDeLaApp)
    ? `<meta name="aportaya-app" content="${urlDeLaApp}">`
    : '',
].join('');

app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then(async (response) => {
      if (!response) return next();
      if (!METAS || !response.headers.get('content-type')?.includes('text/html')) {
        return writeResponseToNodeResponse(response, res);
      }
      const html = (await response.text()).replace('</head>', `${METAS}</head>`);
      const cabeceras = new Headers(response.headers);
      cabeceras.delete('content-length');
      return writeResponseToNodeResponse(
        new Response(html, { status: response.status, headers: cabeceras }),
        res,
      );
    })
    .catch(next);
});

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
