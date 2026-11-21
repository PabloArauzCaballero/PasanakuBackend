# Reporte — Tanda 2, lote L2 (garantía y entregas)

> **AVANCE: 2 / 13 — 15,4 %.**

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `correcciones/tanda2-l2-retiro-subasta`
- Peldaño de evidencia alcanzado: `TESTED` en L2.S1.M1 (`test` + `webTest` + gates estáticos locales en verde).
  `integrationTest` (Testcontainers) no corre en esta nube: lo corre el CI del PR. Todo lo demás del lote: `UNKNOWN`.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| L2.S1.M1 | La subasta de cupos y el mercado de turnos de `entregas` ya no existen: sus 9 operaciones responden 410 con `AP-CU136-09` / `AP-CU130-10` y sin efecto; `GET /entregas/mercado/ofertas/{id}` responde el 422 `AP-CU130-05` del pacto con nucleo. Se borraron 24 clases de producción y 23 de prueba | `./gradlew :servicios:entregas:test :servicios:entregas:webTest` + grep | PASS — [evidencia](./evidencia/L2.S1.M1-retiro-cesion.txt) |
| F2.L2.M09 | Mercado de ofertas (código muerto) retirado | ídem | PASS (con L2.S1.M1) |

## A medias

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
| F1.L2.M07 (SEG-007 `numeroCifrado`) | TODO | Siguiente PR del lote |
| F1.L2.M05, M09, M02, M03, M10 | TODO | Siguientes PR del lote |
| F1.L2.M01, M04, M06, M08, M11–M14 | TODO | M13/M14 necesitan `plataforma/comun-web` (cliente único y token de servicio): fuera de alcance |
| F2.L2.M01, M02, M04 | TODO | Siguientes PR del lote |
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
- Códigos nuevos: `AP-CU130-10` y `AP-CU136-09` (410). No se reusa ningún código.
