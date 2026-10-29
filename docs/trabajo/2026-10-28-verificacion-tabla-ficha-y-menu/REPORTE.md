# Reporte — Verificación de identidad en tabla, ficha completa del expediente y «Plataforma» fija en el menú

> **AVANCE: 9 / 9 — 100 %.** Verificado contra un doble declarado del servicio `identidad` (no hay backend en este sandbox).

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/loving-tesla-bpulmt` (PasanakuFrontend)
- Peldaño de evidencia alcanzado: `VERIFIED` contra el doble (unitarias + a11y + E2E en navegador + capturas miradas en 3 anchos × 2 temas). No es `REGRESSION_VERIFIED` contra el servicio real: falta la integración con `identidad` desplegado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | «Plataforma» es un rótulo fijo con ícono propio (capas), sin chevron ni plegado, que abre la primera sección de Plataforma | `ng test --include=src/app/layout/menu-lateral.spec.ts` · E2E «límite e inválido» | PASS 5/5 · PASS |
| H2.S1.M1 | Presentación pura: estado en palabras, `CI SC` → «Cédula de identidad · Santa Cruz», caras en palabras, «n de 5», impedimentos para aprobar, tiempo en cola | `ng test --include=…/cu02-presentacion.spec.ts` | PASS 4/4 |
| H2.S1.M2 | La cola es una tabla `ap-listado`: Persona (con contacto), Documento (con vencimiento y vigencia), Fotos n de 5, Estado, Iniciada (con «Hace N días»), acción «Revisar»/«Ver»; buscador, filtros de estado/vigencia/fotos, CSV/JSON en palabras, paginación | `ng test --include=…/pantalla-de-expedientes.spec.ts` | PASS 10/10 |
| H3.S1.M1–M3 | Ficha `/cumplimiento/verificaciones/:id`: una tarjeta a lo ancho con pestañas Resumen · Documento y fotos · Decisión · Cómo funciona (en `?vista=`); ningún dato en blanco sin su motivo; decidir desde la ficha con clave de idempotencia; id inexistente y 500 resueltos | `ng test --include=…/ficha-de-expediente*.spec.ts` | PASS 9/9 + a11y 4/4 |
| H4.S1.M1 | Tutorial `cumplimiento-verificaciones` 1.1.0 habla de filas, de la ficha y de AP-CU01-09..11 | `playwright test e2e/tutoriales.e2e.ts` | PASS (en la corrida de 17) |
| H4.S1.M2 | Regresión del backoffice | `yarn typecheck` · `yarn lint` · `yarn test:front` · `yarn test:a11y` | EXIT 0 · TODO OK · 172/1233 · 46/96 |
| H4.S1.M3 | 30 capturas (bandeja + 4 pestañas de la ficha, 3 anchos × 2 temas), miradas | `playwright test e2e/verificacion-de-identidad.e2e.ts` | PASS 8/8, `evidencia/capturas/` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| — Integración con `identidad` real | BLOQUEADO (externo al sandbox) | Correr `e2e/verificacion-de-identidad.e2e.ts` contra el stack local (`start:stack`) o el entorno de prueba; lo que se verificó acá es contra el doble declarado en la propia prueba. |

## Evidencia
- `evidencia/typecheck.txt`, `evidencia/lint.txt`, `evidencia/test-front-completo.txt`, `evidencia/test-a11y.txt`, `evidencia/e2e.txt`.
- `evidencia/capturas/` — `bandeja.*`, `ficha-resumen.*`, `ficha-fotos.*`, `ficha-decision.*`, `ficha-como-funciona.*`, en `movil|tablet|escritorio` × `light|dark`.

```text
yarn typecheck → EXIT 0
yarn lint → All files pass linting. · verificar_frontend.py → TODO OK
yarn test:front → Test Files 172 passed (172) · Tests 1233 passed (1233)
yarn test:a11y → Test Files 46 passed (46) · Tests 96 passed (96)
playwright e2e/verificacion-de-identidad.e2e.ts e2e/shell.e2e.ts e2e/tutoriales.e2e.ts --retries=0 → 17 passed (1.1m), EXIT 0
  consola y red de /identidad sin errores (aserción `problemas == []` en las dos pruebas funcionales)
```

Lo que se corrigió **después de mirar las capturas** (no lo detectaba ninguna prueba):
1. A 1440 px la tabla de siete columnas escondía Fotos, Estado e Iniciada detrás del desplazamiento horizontal → cinco columnas, contacto bajo el nombre y vencimiento bajo el documento.
2. «Revisar» salía subrayado → sin subrayado, como las demás acciones de fila.
3. «Iniciada» se cortaba por la hora → fecha sin hora + «Hace N días».
4. En «Decisión» los impedimentos salían dos veces (lista de la ficha + avisos del formulario) → `DecisionDeExpediente` acepta `[conAvisos]="false"`.
5. La tira de fotos repetía qué caras están cargadas → solo el botón y las fotos; el estado por cara lo dicen las cinco tarjetas.
6. En el celular la insignia «Sin fecha de vencimiento» desbordaba la tarjeta → «Sin fecha» en la tabla (filtro y exportación siguen con el texto entero).

## No cubierto
- Integración real con el servicio `identidad` (cola, fotos, decisión): todo contra el doble de la prueba E2E y `HttpTestingController`.
- Lector de pantalla real (sí: axe en las cuatro pestañas y en la tira; teclado de las pestañas lo cubre `ap-pestanas`).
- En el celular, la pestaña elegida puede quedar fuera de la vista en la barra de pestañas (que se desplaza en horizontal): pieza compartida `ap-pestanas`, no se tocó.
- Las cabeceras de la tabla mezclan mayúsculas (las no ordenables) y tipo oración (las ordenables): es de `ap-tabla-de-datos`, compartida por todo el backoffice; no se tocó.

## Desvíos del plan
- `e2e/shell.e2e.ts` afirmaba el rótulo plegable («Plataforma» como botón con `aria-expanded`, plegar oculta «Sistemas»): falló (`element(s) not found`) porque el pedido lo elimina. Clasificado como cambio de requisito, no como debilitamiento: ahora afirma el enlace fijo con `href`, la ausencia de `aria-expanded`, que «Sistemas» sigue a la vista y que pulsar «Plataforma» abre `/sistemas/servicios`.
- Se agregó `ficha-de-expediente.a11y.spec.ts` (4 pruebas) y `e2e/verificacion-de-identidad.e2e.ts`, no listados como archivos en el plan pero dentro de su DoD.
- Se borró `verificaciones/tarjeta-de-expediente.ts`: la tabla y la ficha la reemplazan y no la usa nadie más.
- `DecisionDeExpediente` ganó el input `conAvisos` (por omisión `true`, sin cambio para otros usos).

## Riesgos residuales
- **La rama base no compila con Angular** (`ng build`/`ng test`): el bump de Dependabot `2befaa42` subió `typescript` a 7.0.2 y `@angular/compiler-cli` 22.2.3 exige `>=6.0 <6.1` (TS 7 no tiene API de JavaScript). Para verificar se reemplazó **solo en el `node_modules` local** `typescript` por 6.0.3 y `@jridgewell/sourcemap-codec` 1.5.5 por 1.6.0; **nada de eso está en el commit**. Arreglarlo es fijar `typescript` en `~6.0.3` en los `package.json` del monorepo: decisión aparte, fuera de este alcance. `yarn typecheck` (tsc 7) sí pasa en la base.
- El entorno local además necesitó: Node 24.15 (Angular CLI exige ≥ 22.22.3), los clientes OpenAPI generados con `openapi-generator 7.14.0`, `tokens.css` y `packages/simulado/generado/*.json`. Sin estos últimos, `entrada-estricta.spec.ts` falla con `ENOENT` (clasificado `ENVIRONMENT`, reproducido y resuelto generándolos).
- La ficha pide la cola entera para mostrar un expediente: el contrato no publica «consultar un expediente». Con colas grandes conviene que el backend agregue `GET /identidad/verificaciones/{id}`.
- «Hace N días» usa el reloj de quien mira (referencia para ordenar); las fechas y la vigencia son del servidor.

## Decisiones y ambigüedades
- A1 «que esto abra» (confirmar con Pablo): se interpretó como rótulo «Plataforma» **fijo, con ícono, siempre abierto, y que al pulsarlo abre** su primera sección (Sistemas → Servicios y SLO).
- A2 «muchas cosas están vacías» (confirmar con Pablo): se interpretó como la vista de verificación. En la ficha cada dato ausente dice por qué falta (cuenta sin correo, número que no viaja a propósito, sin resolver, sin documento). Otras pantallas no se revisaron en este trabajo.
- Los nombres de los nueve departamentos se tomaron de la app (`apps/movil/.../departamentos.dart`) y del contrato (`lugarExpedicion`). Un código desconocido se muestra tal cual, nunca se inventa.
- Todos los datos de prueba son sintéticos (`ejemplo.invalid`, «Sintética», «Ficticio»).

## Llevado a `test` (2026-10-29, rama `claude/loving-tesla-bpulmt-a-test`)
`test` estaba 330 commits por delante de `dev` y ya traía un menú lateral fijo (pantallas bajo cada sección, sin «Inicio», sin pestañas de sección), páginas sin ícono de cabecera ni padding propio. Seis conflictos, resueltos con las convenciones de `test`: el rótulo «Plataforma» pasa a ser un encabezado con ícono que abre su primera sección; la tabla y la ficha sin ícono ni padding; `tarjeta-de-expediente.ts` se elimina.

```text
yarn typecheck → EXIT 0 (TypeScript 6.0.3 de test)
yarn lint → All files pass linting · verificar_frontend.py TODO OK
yarn test:front → Test Files 178 passed (178) · Tests 1279 passed (1279)
yarn test:a11y → Test Files 46 passed (46) · Tests 95 passed (95)
playwright (suite completa del backoffice, --retries=0) → 170 passed · 7 failed
```

Las 7 que fallan **fallan igual en `origin/test` sin este cambio** (misma corrida de los tres archivos, 7 failed · 22 passed): `admisiones.e2e.ts` (2), `f2-posiciones-y-roles.e2e.ts` (4, esperan la entrada «Inicio» que `test` quitó) y `sistemas-sin-contrato.e2e.ts` (1). No son de este trabajo y no se tocaron.

Arreglos de base que sí entran, porque tocan archivos de este cambio o rompían `yarn lint` en `test`:
- `shell.e2e.ts` ya fallaba en `test` (línea 21: buscaba el botón plegable «Plataforma»). Alineado con `test`: rótulo fijo con enlace, pantalla actual marcada en el menú, sin pestañas de sección, sin «Inicio». 3/3.
- `nucleo/menu.ts` (202) y `admisiones/pantalla-de-admision.spec.ts` (212) excedían las 200 líneas en `test`: una documentación en una línea y 13 líneas en blanco entre pruebas. Ninguna aserción cambió.

CI de GitHub no corre en el repositorio: «The job was not started because recent account payments have failed or your spending limit needs to be increased». La verificación es la local de arriba.

## Aplicado en `PasanakuBackend` (2026-10-29, rama `claude/loving-tesla-bpulmt`, PR a `dev` y a `test`)
El backoffice de `PasanakuBackend` es donde se desarrolla hoy (`test` de `PasanakuFrontend` es su espejo) e iba 31 archivos por delante. El mismo diff del port a `test` aplica limpio sobre `dev` y `test`, que tienen el backoffice idéntico.

```text
yarn typecheck → EXIT 0 · yarn lint → All files pass linting · TODO OK
yarn test:front → Test Files 182 passed (182) · Tests 1288 passed (1288)
yarn test:a11y → Test Files 45 passed (45) · Tests 94 passed (94)
playwright shell + verificacion-de-identidad + tutoriales --retries=0 → 17 passed
playwright suite completa → 161 passed · 16 failed (antes de corregir shell.e2e: 2 de esas 16)
playwright admisiones + f2-posiciones-y-roles + sistemas-sin-contrato sobre origin/dev SIN este cambio → 14 failed · 15 passed
```

Las 14 restantes fallan igual en `origin/dev` sin este cambio (`admisiones` ×2, `f2-posiciones-y-roles` ×4, `sistemas-sin-contrato` ×8): no son de este trabajo. `shell.e2e.ts` además buscaba «Outbox y descartados», renombrada «Outbox por servicio» en `9a2a3a75`: se actualizó el nombre.
