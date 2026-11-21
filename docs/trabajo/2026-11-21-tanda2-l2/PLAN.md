# Plan — Tanda 2, lote L2 (garantía y entregas)

- Fecha: 2026-11-21 · Repo: `PasanakuBackend` · Base: `origin/dev` 50cf3be9 · Predecesor: plan de correcciones
  (`docs/trabajo/2026-11-21-plan-correcciones/PLAN.md`, H1 = F1, H2 = F2) y su anexo
  `anexos/ANEXO-L2-garantia-entregas.md`. Decisiones: `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md`.
- Resultado observable: en `entregas` no queda camino para subastar un cupo ni comprar/vender el derecho a cobrar un
  turno (D-L2-1, tanda 7); el número de cuenta bancaria se cifra solo en el servidor; y las correcciones F1/F2 del
  anexo L2 que caben en `servicios/garantia` y `servicios/entregas` quedan en PR hacia `dev`.
- Kill-test: (1) `POST /api/v1/entregas/subastas` con un token válido devuelve algo distinto de 410; (2) un
  `POST /cuentas-bancarias` con `numeroCifrado` en el cuerpo se acepta; (3) `grep -rn 'class SubastaDelCupo' servicios`
  encuentra algo; (4) el piso de ramas de `entregas` (0.71) se baja para que el CI pase.

## Alcance

- IN: `servicios/garantia/**`, `servicios/entregas/**`; `docs/entidades/*.puml`, `contratos/**`, `pactos/**` solo
  de garantía y entregas; esta carpeta; filas de la sección de garantía/entregas de `docs/Restricciones.md`.
- OUT: `plataforma/**`, `apps/**`, `packages/**`, `clientes/**`, `sql/` a mano, `.github/**`, `build.gradle*` raíz,
  contratos y modelos de otros servicios. Lo que haga falta ahí se describe en el `REPORTE.md` y en el PR.
- Ambigüedades registradas:
  - AMB-1. Retiro de operaciones consumidas por otros (regla 98.1.3): la app móvil (L6) y `nucleo-financiero` (L1)
    todavía llaman a operaciones de subasta y mercado. Supuesto: **expandir/contraer**. En este lote las
    operaciones quedan en el contrato marcadas `deprecated` y responden `410` con código estable; el contrato
    no se achica hasta que los consumidores las suelten. Confirmar con Pablo en el PR.
  - AMB-2. Cesiones ya liquidadas en TEST (datos sintéticos, P-14): el fondeo sigue pagándole al cesionario de
    una cesión LIQUIDADA anterior al retiro, para no pagar dos veces el mismo turno. Confirmar con Pablo.

## H1 — Lote L2 (F1 y F2 en garantía y entregas)

**CA:** Dado `dev` con los PR del lote mergeados, cuando se busca la subasta, el mercado de turnos, el
`numeroCifrado` del cliente y las demás vías del anexo L2, entonces no existen o están cerradas con su prueba.
**DoD:** cada PR con CI verde y la salida pegada en `evidencia/`.
**Estado:** A MEDIAS (mergeados en `dev` el 2026-11-21: #419, #421, #422, #424, #429, #459, #462 y #463)

### H1.S1 — Retiro de la cesión de turno (D-L2-1)

**CA:** Dado cualquier usuario, cuando intenta subastar, pujar, publicar o comprar un turno, entonces recibe 410
con código estable y no se escribe nada. **DoD:** `webTest` + `test` de entregas en verde; CI del PR en verde con
los pisos de cobertura sin cambios. **Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| L2.S1.M1 | Retirar subasta (CU-64 por subasta) y mercado de ofertas (CU-130): casos de uso, repositorios, adaptadores, trabajo programado, configuración y pruebas; las operaciones quedan `deprecated` y responden 410 | Las 10 operaciones responden 410 `AP-ENT-RETIRADA`; `grep -rln 'SubastaDelCupo\|GestionDeOfertas' servicios/entregas/src` vacío | `./gradlew :servicios:entregas:test :servicios:entregas:webTest` + grep | HECHO (evidencia/L2.S1.M1-retiro-cesion.txt; `integrationTest` lo corre el CI) |
| L2.S1.M2 | Contracción del contrato y del modelo (borrar las operaciones del OpenAPI y las tablas `subasta_cupo`, `puja_subasta`, `oferta_turno`, `cesion_derecho`) | Las operaciones y tablas no existen | Regeneración DDL + deriva | TODO (precondición: L6 y L1 sueltan los consumidores; ver REPORTE) |

### H1.S2 — Seguridad (F1.L2)

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| F1.L2.M07 | `numeroCifrado` fuera del contrato de entrada; cifrado solo en servidor con clave propia | Un cuerpo con `numeroCifrado` → 400; ida y vuelta del cifrado coincide | `webTest` del rechazo + unitaria de ida y vuelta + arranque sin llave | HECHO (#421 + #463; evidencia/F1.L2.M07-llave-cifrado.txt) |
| F1.L2.M05 | Retirar `POST /desembolsos/{id}/respuesta` (acreditación humana) | 410 y sin `desembolso_acreditado` | `webTest` | HECHO (evidencia/F1.L2.M05-desembolso-sin-humano.txt) |
| F1.L2.M09 | Retirar `POST /entregas` legacy y `EFECTIVO_ORGANIZADOR` | 410; el método no existe en contrato ni modelo ni en bases existentes | `webTest` + psql | HECHO (#424 + #463; evidencia/punto2-check-estrechado.txt) |
| F1.L2.M02 | Retirar la cobertura manual del fondo con datos del cuerpo | 410, sin escritura | `webTest` garantía | A MEDIAS (endpoint 410 y código retirado #472; `SOLICITADA` fuera del CHECK #477, evidencia/F1.L2.M02-check-estrechado.txt; falta `aprobadaPor`/`EntradaCobertura` del contrato y la tabla `politica_cobertura`, que es DECISION_REQUIRED: destructiva sobre datos compartidos, la eleva el coordinador) |
| F1.L2.M03 | Devolución del fondo sin `grupoCerrado`/`deudasVivas` del cuerpo | La API no decide con esos campos | `webTest` | A MEDIAS (endpoint 410; disparo por el sistema en F3.L2.M06) |
| F1.L2.M10 | `saldoRetenido` fuera de la entrada del desembolso | Campo fuera del contrato | `webTest` | TODO (diseño en REPORTE; falta la operación de L1) |
| F1.L2.M04 | Importes de entrada sin signo; disolución y reemplazo con cifras de sus dueños | Un negativo → 400; los totales salen de sus dueños | `webTest` | A MEDIAS (sin signo HECHO; cifras de sus dueños pendiente) |
| F1.L2.M01, M06, M08, M11–M14 | Ver anexo; dependen de plataforma (token de servicio, cliente único) o del rediseño | — | — | TODO |

### H1.S3 — Retiro (F2.L2)

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| P3.M1 | CU-22: la entrega nace solo del fondeo; retiro de `CU22LiquidarEntrega.liquidar/ejecutar`, `LiquidacionDeEntrega` y la ficha CU-22 reescrita | `grep -rn "liquidar(\|LiquidacionDeEntrega" servicios/entregas/src` vacío; los 3 escenarios de CU-22 con su prueba | `test` + `webTest` + verificadores locales; `integrationTest` en el CI del PR | HECHO (#471 mergeado con CI verde, incluidos «Los cinco corredores»; evidencia/punto3-cu22-sin-liquidacion.txt) |
| F2.L2.M09 | Mercado de ofertas | Se resuelve con L2.S1.M1 | — | HECHO (con L2.S1.M1) |
| F2.L2.M01 | Respaldo empresarial en entregas | `grep -rn Respaldo servicios/entregas/src` vacío | suites | A MEDIAS (local verde, evidencia/F2.L2.M01-sin-respaldo-entregas.txt; integración NO COMPROBADO local, la valida el CI del PR) |
| F2.L2.M02 | Respaldo empresarial en garantía | Rutas `/garantia/respaldo/*` 410 | suites | A MEDIAS (local verde, evidencia/F2.L2.M02-sin-respaldo-garantia.txt; integración NO COMPROBADO local; precondición en TEST: evidencia/F2.L2.M02-vigentes-respaldo.sql) |
| F2.L2.M04 | Aval entre participantes | 410 y sin código | suites | A MEDIAS (código retirado y CU-26 marcado RETIRADO, evidencia/F2.L2.M04-cu26-retirado.txt; faltan las tablas) |

### H1.S4 — Entrega automática al cobrarse el turno (F3.L2.M01)

**CA:** Dado un periodo cuyo recaudo `aportes` avisa como listo y un turno conocido para ese periodo, cuando llega el
aviso (una o varias veces, antes o después de los turnos), entonces `entregas` fondea ese turno una sola vez sin que
nadie lo pida a mano. **DoD:** pruebas unitarias en los tres niveles contra dobles (regla 65) + integración con base
real en el CI del PR + DDL regenerado sin deriva. **Estado:** A MEDIAS (las cinco microtareas HECHO contra dobles; falta la integración con los productores reales, que no existen). Diseño:
[evidencia/F3.L2.M01-diseno-entrega-automatica.md](evidencia/F3.L2.M01-diseno-entrega-automatica.md). Los productores
(`aportes.periodo_recaudado`, `grupos.turnos_asignados`) no existen: se trabaja contra dobles de sus pactos.

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| F3.L2.M01.a | Tabla `entregas.turno_del_grupo` en el modelo y DDL regenerado | La tabla existe con `periodo_id` único y sin FK a otro esquema | `generar_ddl.py` + aplicar en psql 5433 + deriva | HECHO (evidencia/F3.L2.M01.a-turno-del-grupo.txt; `verificar_boveda` TODO OK; el README y los planes pasan a 351 tablas porque `verificar_boveda` lo exige) |
| F3.L2.M01.b | Copia de turnos: aplica `grupos.turnos_asignados` una vez por evento y solo con versión mayor | Duplicado y versión vieja no escriben | `test` (unitaria) + `integrationTest` (local contra Postgres real; el CI lo repite con Testcontainers) | HECHO (`AplicarTurnosAsignadosTest` 2/2 y `CU133EntregaAutomaticaTest.versionesDeTurnos`; evidencia/F3.L2.M01-integracion-local.txt y F3.L2.M01.bc-sql-saga-y-copia.txt) |
| F3.L2.M01.c | Fondeo automático al consumir `aportes.periodo_recaudado` (clave `fondeo-auto-<periodoId>`; saga en `entregas.estado_saga`; sin turno conocido o aportes caído → el barrido reintenta) | Correcto, duplicado, fuera de orden y aportes caído dan un solo fondeo | `test` (unitaria, tres niveles) + `integrationTest` (local contra Postgres real; el CI lo repite) | HECHO (`EntregaAutomaticaTest` 11/11, `CU133EntregaAutomaticaTest` 6/6; evidencia/F3.L2.M01-integracion-local.txt) |
| F3.L2.M01.d | Bordes Kafka de los dos eventos, apagados por omisión hasta la decisión del método de desembolso | Un mensaje sin cabecera o campo se rechaza; la lectura del pacto coincide | `test` de los consumidores con los ejemplos de los pactos | HECHO (`PactosDeLaEntregaAutomaticaTest` 5/5; evidencia/F3.L2.M01-suites.txt) |
| F3.L2.M01.e | (no previsto, apareció en la integración) `incidencia_entrega.reportada_por` admite NULL: la incidencia que abre el sistema no tiene persona | Un fondeo automático con faltante abre su incidencia sin violar la FK | `CU133EntregaAutomaticaTest.graciaVencidaYDespuesCompleto` contra Postgres real + DDL regenerado | HECHO (falló con `fk_incidencia_entrega_reportada_por` antes del cambio y pasa después; evidencia/F3.L2.M01-integracion-local.txt y F3.L2.M01-boveda-y-esquema.txt) |
| F3.L2.M01.f | (pedido del coordinador sobre M01.e) La incidencia guarda su `origen` (`PERSONA`\|`SISTEMA`) y R-DES-06 exige autor salvo origen SISTEMA | Persona sin autor, sistema con autor y fila vieja sin autor se rechazan; sistema sin autor y persona con autor se aceptan | psql de los seis casos + `CU22PozoYMercadoRechazosTest.rechazaRDES06` + integración | HECHO (evidencia/F3.L2.M01.f-rdes06-origen.txt; F3.L2.M01-integracion-local.txt) |

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Pruebas con Testcontainers no corren acá (sin Docker) | La cobertura del CI usa `integrationTest` | Se mide en el CI del PR; localmente se corren `test` y `webTest` |
| Retenciones de pujas o compras en curso en TEST | Saldo sintético retenido sin liberar | Consulta previa al despliegue en el PR; liberación por la operación de nucleo |
| Consumidores fuera de alcance (app, nucleo) | Romper su build si se achica el contrato | Expandir/contraer (AMB-1) |
