#!/usr/bin/env node
// scripts/humo.mjs
//
// H4.S1.M1 (madre H7.S1.M1), PR13-Ci.Frontend.
//
// Reemplaza el bucle de shell que tenía `package.json` (raíz):
//   "humo": "for c in postman/humo/*.json; do yarn exec postman collection run \"$c\" ... || true; done"
// El `|| true` final hacía que `yarn humo` devolviera SIEMPRE código de salida 0,
// sin importar cuántas colecciones fallaran -- kill-test #1 de este carril (ver
// entregables/PR13-Ci.Frontend/evidencia/H1-S1-M4.txt: hoy ya fallan 1196
// aserciones sin backend real, y el comando igual salía en 0).
//
// Reglas de este script:
//   - Todas las colecciones son OBLIGATORIAS por default. Una colección se vuelve
//     informativa únicamente si aparece en `COLECCIONES_INFORMATIVAS` de acá abajo,
//     con motivo y dueño escritos al lado -- una cuarentena sin esto es abandono,
//     no una excepción (regla de cuarentena con dueño y fecha).
//   - Corre una colección A LA VEZ (regla 70: un runner por vez), nunca en paralelo.
//   - Si CUALQUIER colección obligatoria falla, el proceso termina con código 1 y
//     nombra la colección. Si solo fallan informativas, avisa por stderr/stdout y
//     termina en 0.
//   - `ejecutarHumo` recibe el `runner` (y, para pruebas, `informativas`) por
//     parámetro: así `humo.spec.mjs` prueba los tres escenarios (todo sano, una
//     informativa cae, una obligatoria cae) sin depender del CLI instalado ni de
//     una red real.

import { readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const RAIZ = path.resolve(__dirname, '..');

/**
 * Colecciones que hoy NO bloquean el humo. Vacío por default: a la fecha de este
 * commit ninguna colección de `postman/humo/` tiene una excepción concedida.
 * Cada entrada que se agregue necesita `motivo` y `dueño`.
 */
export const COLECCIONES_INFORMATIVAS = Object.freeze({
  // 'ejemplo.humo': { motivo: '...', dueño: '...', desde: '2026-01-01' },
});

export const INFORMATIVAS = new Set(Object.keys(COLECCIONES_INFORMATIVAS));

export function clasificar(nombreDeArchivo) {
  const nombre = nombreDeArchivo.replace(/\.postman_collection\.json$/, '');
  return INFORMATIVAS.has(nombre) ? 'informativa' : 'obligatoria';
}

export function resultadoFinal(resultados) {
  const rotas = resultados.filter((resultado) => !resultado.ok);
  const obligatoriasRotas = rotas.filter((resultado) => clasificar(resultado.nombreDeArchivo) === 'obligatoria');
  const informativasRotas = rotas.filter((resultado) => clasificar(resultado.nombreDeArchivo) === 'informativa');
  return {
    exitoso: obligatoriasRotas.length === 0,
    obligatoriasRotas: obligatoriasRotas.map((resultado) => resultado.nombreDeArchivo),
    informativasRotas: informativasRotas.map((resultado) => resultado.nombreDeArchivo),
  };
}

/**
 * Corre Postman CLI sobre un archivo de colección. Es el runner por default de
 * `ejecutarHumo`; las pruebas inyectan uno falso y nunca llegan acá.
 * @param {string} coleccionPath
 * @param {string} entornoPath
 * @returns {Promise<{ok: boolean, codigo: number}>}
 */
export async function runnerPostmanReal(coleccionPath, entornoPath) {
  const { spawnSync } = await import('node:child_process');
  const resultado = spawnSync(
    'yarn',
    ['exec', 'postman', 'collection', 'run', coleccionPath, '-e', entornoPath, '--reporters', 'cli', '--report-events=false'],
    { stdio: 'inherit', shell: process.platform === 'win32' },
  );
  return { ok: resultado.status === 0, codigo: resultado.status ?? 1 };
}

/**
 * El corazón testeable del script: sin filesystem, sin Postman CLI, sin red.
 * @param {object} opciones
 * @param {string[]} opciones.colecciones - nombres base (sin extensión) de las colecciones a correr.
 * @param {(nombre: string) => Promise<{ok: boolean, codigo: number}>} opciones.runner
 * @param {Record<string, {motivo: string}>} [opciones.informativas] - default: el registro real de arriba.
 * @returns {Promise<{exitCode: number, obligatoriasCaidas: string[], informativasCaidas: string[]}>}
 */
export async function ejecutarHumo({ colecciones, runner, informativas = COLECCIONES_INFORMATIVAS }) {
  const obligatoriasCaidas = [];
  const informativasCaidas = [];

  // Una colección a la vez -- regla 70, nunca Promise.all ni corridas en paralelo.
  for (const nombre of colecciones) {
    const resultado = await runner(nombre);
    if (!resultado.ok) {
      const esInformativa = Object.prototype.hasOwnProperty.call(informativas, nombre);
      if (esInformativa) {
        informativasCaidas.push(nombre);
        console.warn(
          `[humo] INFORMATIVA caída (no bloquea): ${nombre} — ${informativas[nombre].motivo ?? 'sin motivo registrado'}`,
        );
      } else {
        obligatoriasCaidas.push(nombre);
        console.error(`[humo] OBLIGATORIA caída: ${nombre}`);
      }
    }
  }

  if (obligatoriasCaidas.length > 0) {
    console.error(
      `[humo] ${obligatoriasCaidas.length} colección(es) obligatoria(s) caída(s): ${obligatoriasCaidas.join(', ')}`,
    );
    return { exitCode: 1, obligatoriasCaidas, informativasCaidas };
  }
  if (informativasCaidas.length > 0) {
    console.warn(`[humo] ${informativasCaidas.length} colección(es) informativa(s) caída(s), no bloquean.`);
  } else {
    console.log('[humo] todas las colecciones en verde.');
  }
  return { exitCode: 0, obligatoriasCaidas, informativasCaidas };
}

async function main() {
  const dirColecciones = path.join(RAIZ, 'postman', 'humo');
  const entornoPath = path.join(RAIZ, 'postman', 'entornos', 'local.postman_environment.json');
  const archivos = readdirSync(dirColecciones).filter((f) => f.endsWith('.postman_collection.json'));
  const colecciones = archivos.map((f) => f.replace(/\.postman_collection\.json$/, ''));

  const runner = (nombre) =>
    runnerPostmanReal(path.join(dirColecciones, `${nombre}.postman_collection.json`), entornoPath);

  const { exitCode } = await ejecutarHumo({ colecciones, runner });
  process.exit(exitCode);
}

// Solo ejecuta main() si se invoca directamente (`node scripts/humo.mjs`), no cuando
// `humo.spec.mjs` importa `ejecutarHumo`/`COLECCIONES_INFORMATIVAS`.
if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(__filename)) {
  main();
}
