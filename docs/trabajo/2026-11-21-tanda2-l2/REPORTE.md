# Reporte — Tanda 2, lote L2 (garantía y entregas)

> **AVANCE: 3 / 13 — 23,1 %.** (F1.L2.M07 en A MEDIAS: no suma)

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `correcciones/tanda2-l2-retiro-subasta` (#419),
  `correcciones/tanda2-l2-seg007-cifrado` (apilada sobre #419)
- Peldaño de evidencia alcanzado: `TESTED` en L2.S1.M1 (`test` + `webTest` + gates estáticos locales en verde).
  `integrationTest` (Testcontainers) no corre en esta nube: lo corre el CI del PR. Todo lo demás del lote: `UNKNOWN`.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| L2.S1.M1 | La subasta de cupos y el mercado de turnos de `entregas` ya no existen: sus 9 operaciones responden 410 con `AP-CU136-09` / `AP-CU130-10` y sin efecto; `GET /entregas/mercado/ofertas/{id}` responde el 422 `AP-CU130-05` del pacto con nucleo. Se borraron 24 clases de producción y 23 de prueba | `./gradlew :servicios:entregas:test :servicios:entregas:webTest` + grep | PASS — [evidencia](./evidencia/L2.S1.M1-retiro-cesion.txt) |
| F2.L2.M09 | Mercado de ofertas (código muerto) retirado | ídem | PASS (con L2.S1.M1) |
| F1.L2.M05 | Ninguna persona anota la respuesta del proveedor: `POST /desembolsos/{id}/respuesta` responde 410 `AP-CU28-08` sin llamar al caso de uso (sin ACREDITADA ni `desembolso_acreditado`). `CU28EmitirDesembolso.anotarRespuesta` se conserva para el webhook firmado de F3.L2.M02 | `./gradlew :servicios:entregas:webTest` | PASS — [evidencia](./evidencia/F1.L2.M05-desembolso-sin-humano.txt) |

## A medias

### F1.L2.M02, F1.L2.M03 y F2.L2.M04 — `garantia`: cobertura manual, devolución a pedido y aval
- Qué anda: `cubrirIncumplimiento` (410 `AP-CU23-14`), `devolverFondo` (410 `AP-CU29-06`) y `ejecutarAval`
  (410 `AP-CU26-08`) quedan `deprecated` (garantia.yaml 1.1.0) y no llaman a ningún caso de uso, así que ya no
  existe camino HTTP donde el cliente afirme la mora, el monto, el aprobador, `grupoCerrado` o `deudasVivas`. El
  aval se retiró de raíz del código: `CU26EjecutarAval`, `TopeDelAval`, la ejecución y la subrogación al avalista
  en `GestionRepositorio`, la configuración `aportaya.aval` y sus pruebas
  ([evidencia](./evidencia/F1.L2.M02-M03-F2.L2.M04-garantia.txt)).
- Qué no anda: `CU23CubrirIncumplimiento` (con su estado `SOLICITADA` y el umbral `politica_cobertura`) y
  `CU29DevolverFondo` siguen en el código sin camino HTTP. Las tablas `aval_participante` y `ejecucion_aval`
  siguen en el modelo.
- Qué falta exactamente:
  1. M02: retirar `CU23CubrirIncumplimiento`, el estado `SOLICITADA`, el tópico `cobertura_requiere_aprobacion` y
     `politica_cobertura` del `.puml`. Requiere ajustar los criterios de `docs/CasosDeUso/CU-23` (fuera de alcance),
     porque `verificar_criterios.py` exige sus pruebas mientras exista una clase `CU23*`.
  2. M03: la devolución la dispara el evento de cierre de ciclo, con el estado del grupo leído de `grupos` y las
     deudas contadas en el propio esquema (F3.L2.M06).
  3. F2.L2.M04: quitar `aval_participante` y `ejecucion_aval` de `docs/entidades` y R-GAR-04 de
     `docs/Restricciones.md` (la cifra de restricciones cambia en `docs/Seguridad.md` y `planes/*`, fuera de
     alcance). Marcar retirado `docs/CasosDeUso/CU-26` (hoy figura «pendiente de implementar»).
     `identidad.referencia_personal.acepta_ser_avalista` es de L4.
- Dónde quedó: rama `correcciones/tanda2-l2-garantia-cuerpo`; compila, `test`/`webTest` en verde.

### F1.L2.M09 — Retirar `POST /entregas` legacy y `EFECTIVO_ORGANIZADOR` (y primera parte de F1.L2.M08)
- Qué anda: `liquidarEntrega` (POST /entregas) responde 410 `AP-CU22-10` y `ejecutarEntrega` (cierre con monto libre,
  F1.L2.M08) 410 `AP-CU22-11`, ambos sin llamar al caso de uso. `EFECTIVO_ORGANIZADOR` salió del enum del contrato
  (400) y del modelo `04_entregas_fondo.puml`: una base creada desde el modelo lo rechaza
  (`ck_entrega_fondo_metodo_desembolso`, [evidencia](./evidencia/F1.L2.M09-entrega-legacy.txt)).
- Qué no anda: en una base que ya existe (TEST), el CHECK viejo sigue admitiendo `EFECTIVO_ORGANIZADOR`, porque el
  DDL de tabla es `CREATE TABLE IF NOT EXISTS`. `CU22LiquidarEntrega.liquidar`/`ejecutar` siguen en el código: el
  CU-22 de la bóveda tiene sus criterios gherkin y `verificar_criterios.py` exige sus pruebas.
- Qué falta exactamente:
  1. Fuera de alcance: registrar `ck_entrega_fondo_metodo_desembolso` como CHECK que evoluciona en
     `scripts/modelo.py` (`CK_AMPLIADOS` o su equivalente para estrechar), para que el generador emita
     `DROP/ADD CONSTRAINT` sobre bases existentes. Antes, consulta en TEST:
     `SELECT count(*) FROM entregas.entrega_fondo WHERE metodo_desembolso = 'EFECTIVO_ORGANIZADOR';` (si hay filas,
     se cierran por su camino, nunca con `UPDATE`). Probé la alternativa de una regla nueva `R-DES-06` en
     `docs/Restricciones.md` y funciona en ambas bases, pero obliga a cambiar la cifra de restricciones en
     `docs/Seguridad.md` y en `planes/*`, y a citarla en `docs/CasosDeUso/CU-22` (fuera de alcance).
  2. Fuera de alcance: actualizar `docs/CasosDeUso/CU-22` para retirar los criterios de la liquidación y la
     ejecución manual; después, en este lote, borrar `CU22LiquidarEntrega.liquidar`/`ejecutar` y sus pruebas.
- Dónde quedó: rama `correcciones/tanda2-l2-entrega-legacy`; compila, `test`/`webTest` en verde.

### F1.L2.M07 — El número de cuenta se cifra solo en el servidor, con clave dedicada (SEG-007)
- Qué anda: `numeroCifrado` ya no existe en `EntradaRegistroCuenta` (OpenAPI 2.0.0); un cuerpo que lo manda recibe 400
  sin llegar al caso de uso. `CU18RegistrarCuentaDestino` cifra siempre desde `numeroEnClaro`, el mismo número del
  que salen el enmascarado y el hash. Prueba unitaria de ida y vuelta en verde
  ([evidencia](./evidencia/F1.L2.M07-numero-cifrado.txt)).
- Qué no anda: la clave de cifrado sigue siendo `aportaya.seguridad.pimienta`, la misma que el pepper del hash.
- Qué falta exactamente:
  1. Fuera de alcance (`scripts/generar_compose.py` y los `docker-compose*.yml` generados): agregar el secreto
     `ENTREGAS_LLAVE_CIFRADO_CUENTAS` a la lista de secretos del generador y regenerar el compose.
  2. En este lote, después de 1: `aportaya.seguridad.llave-cifrado-cuentas: ${ENTREGAS_LLAVE_CIFRADO_CUENTAS}` en
     `application.yml` sin valor por defecto (el servicio no arranca sin ella, con su prueba de arranque), usarla en
     `CU18RegistrarCuentaDestino` y en quien descifra, y recifrar las cuentas existentes (datos sintéticos, P-14)
     descifrando con la pimienta y cifrando con la llave nueva.
- Dónde quedó: rama `correcciones/tanda2-l2-seg007-cifrado`; compila y `test`/`webTest` en verde.

### L2.S1.M2 — Contracción del contrato y del esquema de la cesión
- Qué anda: el código ya no crea ofertas, cesiones, subastas ni pujas; las operaciones son `deprecated` (OpenAPI 1.1.0).
- Qué no anda: las 10 operaciones siguen en el contrato y las tablas `oferta_turno`, `cesion_derecho`, `subasta_cupo`
  y `puja_subasta` siguen en el modelo.
- Qué falta exactamente (fuera de mi alcance, por eso no lo hice):
  1. L6 (app móvil): borrar `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart`, sus pantallas y las
     pruebas `subastar_cupo_test.dart`, `pujar_en_subasta_test.dart`, `subasta_capturas_test.dart`; y
     `packages/simulado/ejemplos/entregas/{publicarOferta,listarOfertas,cancelarOferta,comprarOferta,consultarOfertaParaPago,abrirSubastaDeCupo,listarSubastasDeCupo,consultarSubastaDeCupo,pujarEnSubasta,cancelarSubastaDeCupo}.json`.
  2. L1 (nucleo-financiero): retirar `OfertasDelMercado`/`OfertasPorHttp`, el pago a tercero que lo usa,
     `PactoConEntregasContratoTest` (lee `entregas.yaml` y corta en `/entregas/mercado/ofertas/{ofertaId}/cancelacion:`)
     y el pacto `contratos/pactos/nucleo-financiero-entregas.json`.
  3. Después de 1 y 2 (este lote): borrar las operaciones del OpenAPI (versión 2.0.0), las tablas del
     `docs/entidades/04_entregas_fondo.puml` (más R-DES-04 y R-DES-05 de `docs/Restricciones.md`), regenerar `sql/`,
     quitar `CesionRepositorio` del fondeo y las URL de `grupos`/`nucleo-financiero` de `application.yml` (con el
     compose regenerado). Antes, la consulta de vivas de abajo.
- Dónde quedó: rama `correcciones/tanda2-l2-retiro-subasta`; compila.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| F1.L2.M10 | TODO | Siguiente PR del lote |
| F1.L2.M01, M04, M06, M08, M11–M14 | TODO | M13/M14 necesitan `plataforma/comun-web` (cliente único y token de servicio): fuera de alcance |
| F2.L2.M01, M02 | TODO | Siguientes PR del lote |
| L2.S1.M2 | BLOQUEADO | L6 y L1 (ver A medias) |

## Evidencia

- [`evidencia/L2.S1.M1-retiro-cesion.txt`](./evidencia/L2.S1.M1-retiro-cesion.txt): grep de control, `test`/`webTest`,
  `check`/`testBarrido`/spotless, pacto de nucleo con entregas (6/6), `verificar_criterios`, `verificar_pruebas_web`,
  `verificar_seguridad`, `verificar_boveda`, Postman regenerado.
- [`evidencia/L2.S1.M1-rdes-psql.sql`](./evidencia/L2.S1.M1-rdes-psql.sql) y su [salida](./evidencia/L2.S1.M1-rdes-psql.out):
  el SQL de las pruebas R-DES-04/05 reescritas, corrido contra el esquema real (PostgreSQL 16 local con `sql/aplicar.sql`):
  dispara `uq_oferta_turno_activa`, `ck_cesion_derecho_liquidada`, `uq_cesion_derecho_turno_viva` y
  `ck_cesion_derecho_partes`.

## No cubierto

- `integrationTest`, `contractTest` y `sagaTest` de `entregas` no corrieron acá (sin Docker): `CU22PozoYMercadoRechazosTest`
  reescrita queda `NO COMPROBADO` localmente; su SQL sí se ejercitó con `psql`. Comando:
  `./gradlew :servicios:entregas:integrationTest --tests '*CU22PozoYMercadoRechazosTest*'`.
- Piso de cobertura (`jacocoTestCoverageVerification`, pisos sin cambios 0.89/0.71/0.71): se mide en el CI con los
  cinco corredores.
- Fuera de alcance que todavía llama a `anotarRespuestaDesembolso`: `packages/simulado/ejemplos/entregas/anotarRespuestaDesembolso.json` y `despliegue/ensayos/integracion/h14.py:580` (ahora reciben 410).
- La app móvil sigue llamando a la subasta: hoy recibe 410 y muestra un error genérico (L6).

## Desvíos del plan

- El anexo pedía 404/410 «retirando del OpenAPI». Se eligió 410 manteniendo las operaciones `deprecated` (AMB-1):
  borrarlas rompía el build de la app (cliente Dart generado) y la prueba de pacto de nucleo, ambos fuera de alcance.
- `CU22PozoYMercadoRechazosTest` no se borró: `verificar_criterios.py` exige que CU-22 tenga prueba de rechazo de
  R-DES-04/05, y esas restricciones siguen vivas en la base. Se reescribió con SQL directo en vez de usar el mercado.
- `scripts/generar_catalogo_operaciones.py` cambia 600 líneas de dos catálogos por causas ajenas a este cambio
  (entradas `src-*`); no lo verifica el CI y se revirtió. Queda anotado para el dueño de auditoría.

## Riesgos residuales

- En TEST pueden quedar subastas ABIERTAS con pujas retenidas o compras a medias con saldo retenido en la billetera
  (datos sintéticos, P-14). El trabajo que cerraba subastas se retiró. Consulta previa a promover a `test`:
  `SELECT estado, count(*) FROM entregas.subasta_cupo GROUP BY 1; SELECT estado, count(*) FROM entregas.puja_subasta GROUP BY 1; SELECT estado, count(*) FROM entregas.cesion_derecho GROUP BY 1;`
  Si hay retenciones vivas, se liberan con la operación de liberación de `nucleo-financiero` (nunca con `UPDATE`).

## Decisiones y ambigüedades

- AMB-1 (expandir/contraer con 410) y AMB-2 (el fondeo sigue pagándole al cesionario de una cesión LIQUIDADA o con
  título asignado, para no pagar dos veces el turno): supuestos tomados; confirmar con Pablo en el PR.
- Códigos nuevos: `AP-CU130-10`, `AP-CU136-09`, `AP-CU28-08`, `AP-CU22-10`, `AP-CU22-11` (entregas) y
  `AP-CU23-14`, `AP-CU26-08`, `AP-CU29-06` (garantía), todos 410. No se reusa ningún código.
- `OperacionRetirada` y `RetiroDeOperaciones` quedan duplicadas en `entregas` y `garantia`: su lugar es
  `plataforma/comun-web` (el manejador común no conoce el 410), fuera de alcance. Deuda anotada.
- Se borró `CU27ReclamoEnCursoTest.reclamoEnCursoNoEjecutaElAval`: probaba la guarda de reclamos para el aval
  (código 26), que ya no tiene quien la llame. La guarda sigue probada para CU-27.
