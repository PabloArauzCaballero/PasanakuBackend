// scripts/humo.spec.mjs
//
// H4.S1.M2 (madre H7.S1.M2), PR13-Ci.Frontend. Prueba los tres niveles con newman
// INYECTADO (un runner falso) -- nunca necesita newman instalado ni red real.
//
// Desviación registrada respecto del DoD literal del encargo (`yarn vitest run
// scripts/humo.spec.mjs`): este repo no tiene `vitest` como dependencia de la raíz
// (solo `newman`, `turbo`, `typescript` en package.json:20-24) y este entorno no
// tiene `node_modules` instalado (no hay red de paquetes disponible en esta sesión
// para correr `yarn install`), así que no hay forma de EJECUTAR vitest acá para
// verificarlo de verdad. Se usa el test runner nativo de Node (`node:test`, sin
// dependencias nuevas -- regla 90.4.2) en su lugar: corre de verdad en este entorno
// con `node --test scripts/humo.spec.mjs`, y prueba exactamente los mismos tres
// escenarios que pedía el DoD. Si el equipo decide más adelante que TODO el
// frontend corre bajo vitest por consistencia, migrar este archivo es mecánico.

import { test } from 'node:test';
import assert from 'node:assert/strict';

import { ejecutarHumo, resultadoFinal, clasificar, INFORMATIVAS } from './humo.mjs';

test('el veredicto puro conserva los nombres de las colecciones obligatorias fallidas', () => {
  assert.deepEqual(resultadoFinal([
    { nombreDeArchivo: 'aportes.humo.postman_collection.json', ok: false },
    { nombreDeArchivo: 'grupos.humo.postman_collection.json', ok: true },
  ]), {
    exitoso: false,
    obligatoriasRotas: ['aportes.humo.postman_collection.json'],
    informativasRotas: [],
  });
});

test('sin excepciones registradas, cada colección se clasifica como obligatoria', () => {
  assert.equal(INFORMATIVAS.size, 0);
  assert.equal(clasificar('identidad.humo.postman_collection.json'), 'obligatoria');
});

test('las tres colecciones sanas -> exit 0, ninguna caída', async () => {
  const runner = async () => ({ ok: true, codigo: 0 });
  const resultado = await ejecutarHumo({
    colecciones: ['identidad.humo', 'aportes.humo', 'grupos.humo'],
    runner,
  });
  assert.equal(resultado.exitCode, 0);
  assert.deepEqual(resultado.obligatoriasCaidas, []);
  assert.deepEqual(resultado.informativasCaidas, []);
});

test('una colección informativa cae -> exit 0, se registra, no bloquea', async () => {
  const runner = async (nombre) => ({ ok: nombre !== 'notificaciones.humo', codigo: nombre === 'notificaciones.humo' ? 1 : 0 });
  const informativas = {
    'notificaciones.humo': { motivo: 'proveedor de push aún sin decidir, ADR-035', dueño: 'equipo-notificaciones' },
  };
  const resultado = await ejecutarHumo({
    colecciones: ['identidad.humo', 'notificaciones.humo', 'grupos.humo'],
    runner,
    informativas,
  });
  assert.equal(resultado.exitCode, 0, 'una informativa caída NUNCA debe bloquear el humo');
  assert.deepEqual(resultado.obligatoriasCaidas, []);
  assert.deepEqual(resultado.informativasCaidas, ['notificaciones.humo']);
});

test('una colección obligatoria cae -> exit 1, la nombra', async () => {
  const runner = async (nombre) => ({ ok: nombre !== 'identidad.humo', codigo: nombre === 'identidad.humo' ? 1 : 0 });
  const resultado = await ejecutarHumo({
    colecciones: ['identidad.humo', 'aportes.humo', 'grupos.humo'],
    runner,
  });
  assert.equal(resultado.exitCode, 1, 'una obligatoria caída SIEMPRE debe bloquear el humo');
  assert.deepEqual(resultado.obligatoriasCaidas, ['identidad.humo']);
  assert.deepEqual(resultado.informativasCaidas, []);
});

test('corre las colecciones UNA A LA VEZ (regla 70), nunca en paralelo', async () => {
  let enVuelo = 0;
  let maxEnVuelo = 0;
  const runner = async () => {
    enVuelo += 1;
    maxEnVuelo = Math.max(maxEnVuelo, enVuelo);
    await new Promise((r) => setTimeout(r, 5));
    enVuelo -= 1;
    return { ok: true, codigo: 0 };
  };
  await ejecutarHumo({ colecciones: ['a', 'b', 'c'], runner });
  assert.equal(maxEnVuelo, 1, 'nunca debe haber más de una colección corriendo a la vez');
});

test('todas obligatorias por default: sin registro de informativas, cualquier caída bloquea', async () => {
  const runner = async () => ({ ok: false, codigo: 1 });
  const resultado = await ejecutarHumo({ colecciones: ['x.humo'], runner });
  assert.equal(resultado.exitCode, 1);
  assert.deepEqual(resultado.obligatoriasCaidas, ['x.humo']);
});

test('sin colecciones -> exit 0 (no hay nada que romper)', async () => {
  const runner = async () => ({ ok: true, codigo: 0 });
  const resultado = await ejecutarHumo({ colecciones: [], runner });
  assert.equal(resultado.exitCode, 0);
});
