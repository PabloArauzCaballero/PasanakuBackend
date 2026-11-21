# Reporte — Tanda 2, lote L2 (garantía y entregas)

> **AVANCE: 12 / 21 — 57,1 %.**

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · PR mergeados en `dev`: #419, #421, #422, #424, #429, #459, #462
  y #463, todos con CI verde, incluidos los cinco corredores. #464 se consolidó en #463 por el tope de 3 PR abiertos.
  #463 todavía no está en `test`: espera a que se cargue `ENTREGAS_LLAVE_CIFRADO_CUENTAS` en Coolify.
- Peldaño de evidencia alcanzado: `TESTED` en L2.S1.M1 (`test` + `webTest` + gates estáticos locales en verde).
  `integrationTest` (Testcontainers) no corre en esta nube: lo corre el CI del PR. Todo lo demás del lote: `UNKNOWN`.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| L2.S1.M1 | La subasta de cupos y el mercado de turnos de `entregas` ya no existen: sus 9 operaciones responden 410 con `AP-CU136-09` / `AP-CU130-10` y sin efecto; `GET /entregas/mercado/ofertas/{id}` responde el 422 `AP-CU130-05` del pacto con nucleo. Se borraron 24 clases de producción y 23 de prueba | `./gradlew :servicios:entregas:test :servicios:entregas:webTest` + grep | PASS — [evidencia](./evidencia/L2.S1.M1-retiro-cesion.txt) |
| F2.L2.M09 | Mercado de ofertas (código muerto) retirado | ídem | PASS (con L2.S1.M1) |
| F1.L2.M07 | El número de cuenta se cifra solo en el servidor (`numeroCifrado` fuera del contrato: 400) y con una llave propia, `ENTREGAS_LLAVE_CIFRADO_CUENTAS`, distinta de la pimienta; sin ella entregas no arranca | `./gradlew :servicios:entregas:test :servicios:entregas:webTest` | PASS — [evidencia](./evidencia/F1.L2.M07-llave-cifrado.txt) (#421, #463) |
| F1.L2.M09 | `POST /entregas` y la ejecución manual responden 410; `EFECTIVO_ORGANIZADOR` no existe en el contrato, en el modelo ni en las bases existentes (`CK_ESTRECHADOS`) | psql contra una base existente y una nueva | PASS — [evidencia](./evidencia/punto2-check-estrechado.txt) (#424, #463) |
| F1.L2.M05 | Ninguna persona anota la respuesta del proveedor: `POST /desembolsos/{id}/respuesta` responde 410 `AP-CU28-08` sin llamar al caso de uso (sin ACREDITADA ni `desembolso_acreditado`). `CU28EmitirDesembolso.anotarRespuesta` se conserva para el webhook firmado de F3.L2.M02 | `./gradlew :servicios:entregas:webTest` | PASS — [evidencia](./evidencia/F1.L2.M05-desembolso-sin-humano.txt) |
| P3.M1 | La entrega nace solo del fondeo: `CU22LiquidarEntrega.liquidar`/`ejecutar` y `LiquidacionDeEntrega` no existen; la ficha CU-22 tiene tres criterios probados | `test` + `webTest` locales; `integrationTest` en el CI | PASS — [evidencia](./evidencia/punto3-cu22-sin-liquidacion.txt) (#471, CI verde con los cinco corredores) |
| F3.L2.M01.a | `entregas.turno_del_grupo` (copia de lectura de los turnos de `grupos`, sin FK a otro esquema, `periodo_id` único) en el modelo, el DDL y la base local | `generar_ddl.py` + psql 5433 + job de bóveda y esquema | PASS — [evidencia](./evidencia/F3.L2.M01.a-turno-del-grupo.txt), [job](./evidencia/F3.L2.M01-boveda-y-esquema.txt) (20/20 pasos en 0) |
| F3.L2.M01.b | La copia de turnos se aplica una vez por evento y solo con versión mayor | `AplicarTurnosAsignadosTest` + `CU133EntregaAutomaticaTest.versionesDeTurnos` contra Postgres real | PASS — [evidencia](./evidencia/F3.L2.M01-integracion-local.txt), [SQL](./evidencia/F3.L2.M01.bc-sql-saga-y-copia.txt) |
| F3.L2.M01.c | `entregas` fondea sola al recibir el aviso de recaudo: una entrega por periodo con clave `fondeo-auto-<periodoId>`; aviso duplicado, aviso antes que los turnos, aportes caído y gracia vencida seguida de completo terminan en UN fondeo | `EntregaAutomaticaTest` (12, tres niveles contra dobles, incluido el del método) + `MetodoDeDesembolsoConfiguradoTest` (3) + `CU133EntregaAutomaticaTest` (6) contra Postgres real | PASS — [evidencia](./evidencia/F3.L2.M01-integracion-local.txt) |
| F3.L2.M01.d | Los bordes Kafka de `aportes.periodo_recaudado` y `grupos.turnos_asignados` leen los ejemplos de los pactos y rechazan sobre o campo faltante; apagados por omisión | `PactosDeLaEntregaAutomaticaTest` (5) | PASS — [evidencia](./evidencia/F3.L2.M01-suites.txt) |
| F3.L2.M01.e | La incidencia por faltante que abre el sistema no lleva persona: `incidencia_entrega.reportada_por` admite NULL | `CU133EntregaAutomaticaTest.graciaVencidaYDespuesCompleto` (rojo con `fk_incidencia_entrega_reportada_por` antes, verde después) | PASS — [evidencia](./evidencia/F3.L2.M01-integracion-local.txt) |
| F3.L2.M01.f | Ninguna incidencia de persona queda sin autor: columna `origen` (`PERSONA`\|`SISTEMA`) y R-DES-06 (`ck_incidencia_entrega_autor`); la fila anterior a la columna cuenta como persona | psql de seis casos (3 aceptados, 3 rechazados) + `rechazaRDES06` + integración local | PASS — [evidencia](./evidencia/F3.L2.M01.f-rdes06-origen.txt) |

## A medias

### H1.S4 / F3.L2.M01 — Entrega automática al cobrarse el turno
- Qué anda: con la propiedad `aportaya.entrega-automatica.habilitada=true` y un método de desembolso configurado, el
  aviso `aportes.periodo_recaudado` fondea el turno del periodo sin pedido manual. El aviso queda en la saga
  `entregas.estado_saga` en la misma transacción que la marca del evento; el barrido (`BarridoDeEntregaAutomatica`,
  ShedLock) retoma lo que esperaba el turno o a `aportes`. Probado contra dobles en los tres niveles y contra Postgres
  real (copia local), ver Completado.
- Qué no anda: los productores no existen. `aportes` (L1) no emite `aportes.periodo_recaudado` y `grupos` no emite
  `grupos.turnos_asignados`. Por eso la función está **apagada por omisión**: en ningún entorno fondea sola todavía.
  El método de desembolso de la entrega que nace sola es DECISION_REQUIRED.
- Qué falta exactamente: (1) L1 emite `aportes.periodo_recaudado` según
  `contratos/pactos/aportes-entregas-periodo-recaudado.json`, con su prueba de productor; (2) el dueño de `grupos`
  emite `grupos.turnos_asignados` según `contratos/pactos/grupos-entregas-turnos-asignados.json`, también con prueba
  de productor; (3) Pablo decide el método de desembolso (ver Decisiones); (4) se enciende en `test` con
  `APORTAYA_ENTREGAAUTOMATICA_HABILITADA=true` y `APORTAYA_ENTREGAAUTOMATICA_METODODESEMBOLSO`, y se prueba con Kafka
  real y con `aportes` apagado de verdad (regla 98.8.4).
- Dónde quedó: rama `correcciones/tanda2-l2-entrega-automatica` (PR #490); compila; `test`, `webTest`, `testBarrido`
  y la integración local en verde.

`CU22LiquidarEntrega.liquidar`/`ejecutar` ya no existen (#471). `CU23CubrirIncumplimiento` se retira en #472.

### F2.L2.M01 — El fondeo ya no le pide el respaldo a `garantia`
- Qué anda: `RespaldoEmpresarial`, `RespaldoPorHttp`, la rama de cobertura del fondeo, la configuración
  `aportaya.servicios.garantia` y sus pruebas (`RespaldoCaidaTest`, las del adaptador, las ramas del doble) se
  retiraron. Con faltante, la entrega queda BLOQUEADA con el pozo como principal y la deuda conservada, a la espera del
  fondo del grupo (F3.L2.M05). `cubiertoEmpresa` sigue en la respuesta y en el evento, siempre en cero, hasta la
  contracción del contrato. `test`, `webTest`, `check`, `testBarrido` y el job de bóveda en verde
  ([evidencia](./evidencia/F2.L2.M01-sin-respaldo-entregas.txt)).
- Qué no anda: nada observado; las pruebas de integración del fondeo reescritas no corrieron acá (sin Docker).
- Qué falta exactamente: el verde de `integrationTest` y de jacoco en el CI del PR; contraer `cubiertoEmpresa`,
  `monto_cubierto_empresa` y `cobertura_respaldo_id` del contrato y del modelo cuando nadie los lea.
- Dónde quedó: rama `correcciones/tanda2-l2-sin-respaldo-entregas`; compila.

### F2.L2.M02 — `garantia` ya no reserva ni cubre: el respaldo empresarial se retira
- Qué anda: las seis rutas `/garantia/respaldo/*` quedan `deprecated` (`garantia.yaml` 1.3.0) y responden 410
  `AP-CU23-15` sin efecto (sin sesión, 401). Los casos de uso del respaldo, la contingencia de caja,
  `AsientoPropuesto` (`CAJA_EMPRESA`/`RESERVA_RESPALDO`) y sus repositorios ya no existen; `garantia` no publica
  ningún tópico `respaldo_*`. CU-23 queda «pendiente de implementar» con los criterios del modelo A. `test`,
  `webTest`, `check`, `testBarrido` y el job de bóveda en verde
  ([evidencia](./evidencia/F2.L2.M02-sin-respaldo-garantia.txt)).
- Qué no anda: nada observado; el `integrationTest` de lo que queda en garantía no corrió acá (sin Docker).
- Qué falta exactamente: el verde del CI; antes de desplegar en TEST, la consulta de vigentes
  (`evidencia/F2.L2.M02-vigentes-respaldo.sql`) y, si da > 0, sus contra-asientos. Las tablas y R-GAR-08 a 13 se
  retiran con F2.L2.M03; las rutas se borran del contrato con la contracción.
- Dónde quedó: rama `correcciones/tanda2-l2-sin-respaldo-garantia`; compila. Desplegar después de #474.

### F1.L2.M04 — Importes de entrada sin signo; disolución y reemplazo con cifras de sus dueños
- Qué anda: `garantia.yaml` 1.2.0 define `DineroEntrada` (monto `^\d+\.\d{2}$`, sin signo) y lo usan los importes que
  manda el cliente en `declararIncumplimiento`, `proponerReemplazo` e `iniciarDisolucion`: un negativo recibe 400 sin
  llegar al caso de uso ([evidencia](./evidencia/F1.L2.M04-dinero-entrada.txt)). `entregas` ya no tiene entradas
  vigentes con importes: todas las que los tenían quedaron retiradas (410).
- Qué no anda: `CU67DisolverGrupo` y `CU66ReemplazarParticipante` siguen tomando del cuerpo los totales, la masa y
  la deuda asumida. D-L2-2 (tanda 7) mantiene reemplazo y disolución, votados.
- Qué falta exactamente: leer el total aportado y la deuda viva de `aportes` y el mayor, y lo entregado de `entregas`,
  con clientes con timeout y circuito (regla 65: dobles en tres niveles), y quitar esos campos de la entrada.
  Depende del cliente HTTP único y del token de servicio en `plataforma` (F1.L2.M13/M14).
- Dónde quedó: mergeado en `dev` (#459) con CI verde; la segunda mitad no se empezó.

### F1.L2.M02, F1.L2.M03 y F2.L2.M04 — `garantia`: cobertura manual, devolución a pedido y aval
- Qué anda: `cubrirIncumplimiento` (410 `AP-CU23-14`), `devolverFondo` (410 `AP-CU29-06`) y `ejecutarAval`
  (410 `AP-CU26-08`) quedan `deprecated` (garantia.yaml 1.1.0) y no llaman a ningún caso de uso, así que ya no
  existe camino HTTP donde el cliente afirme la mora, el monto, el aprobador, `grupoCerrado` o `deudasVivas`. El
  aval se retiró de raíz del código: `CU26EjecutarAval`, `TopeDelAval`, la ejecución y la subrogación al avalista
  en `GestionRepositorio`, la configuración `aportaya.aval` y sus pruebas
  ([evidencia](./evidencia/F1.L2.M02-M03-F2.L2.M04-garantia.txt)).
- Qué anda además (punto 3): `CU23CubrirIncumplimiento`, `CoberturaAplicable`, los métodos de repositorio que solo
  ella usaba, el tópico `cobertura_requiere_aprobacion` y la configuración `aportaya.deuda` se retiraron; la ficha
  CU-23 describe el camino vigente (respaldo, transitorio) y el retiro, con sus tres criterios probados
  (`CU23Test`, `web/CU23CoberturaManualRetiradaWebTest`). Las pruebas de CU-27, CU-66 y tableros arman el deudor
  con `FixturaDeDeudor` ([evidencia](./evidencia/F1.L2.M02-cu23-sin-cobertura-manual.txt)).
- Qué no anda: `CU29DevolverFondo` sigue en el código sin camino HTTP. Nada escribe hoy `cobertura_incumplimiento`
  ni `deuda_participante` hasta la cobertura del modelo A (F3.L2.M05); CU-27 y CU-66 operan sobre deudas existentes. Las tablas `aval_participante` y `ejecucion_aval`
  siguen en el modelo.
- Qué falta exactamente:
  1. M02: `SOLICITADA` ya sale del CHECK (#477, con `CK_ESTRECHADOS`; precondición en TEST: `SELECT count(*) FROM
     garantia.cobertura_incumplimiento WHERE estado = 'SOLICITADA'` = 0). `APROBADA` se queda: el anexo solo nombra
     `SOLICITADA` (confirmado por el coordinador). Falta quitar `aprobadaPor`/`EntradaCobertura` del contrato (la
     operación ya responde 410) y la tabla `politica_cobertura` (ver Pendiente: decisión del dueño).
  2. M03: la devolución la dispara el evento de cierre de ciclo, con el estado del grupo leído de `grupos` y las
     deudas contadas en el propio esquema (F3.L2.M06).
  3. F2.L2.M04: quitar `aval_participante` y `ejecucion_aval` de `docs/entidades` y R-GAR-04 de
     `docs/Restricciones.md` (la cifra de restricciones cambia en `docs/Seguridad.md` y `planes/*`, fuera de
     alcance). `docs/CasosDeUso/CU-26` ya está marcado RETIRADO (`estado: retirado` y aviso en la ficha y en
     el índice); `verificar_criterios.py` lo sigue listando como «pendiente» porque no distingue retirados: es un
     cambio en ese script, fuera del alcance de L2.
     `identidad.referencia_personal.acepta_ser_avalista` es de L4.
- Dónde quedó: rama `correcciones/tanda2-l2-garantia-cuerpo`; compila, `test`/`webTest` en verde.

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
| Contracción `politica_cobertura` | DECISION_REQUIRED | Borrar la tabla es destructivo sobre datos compartidos: lo eleva el coordinador a Pablo junto con la migración. Hoy la referencia `garantia.fondo_garantia.politica_cobertura_id` (`fk_fondo_garantia_politica_cobertura_id`, ON DELETE RESTRICT), y `FondoRepositorio.Fondo.politicaId` la sigue leyendo. Consulta de uso en TEST: `SELECT count(*) FROM garantia.politica_cobertura;` · `SELECT count(*) FROM garantia.fondo_garantia WHERE politica_cobertura_id IS NOT NULL;` · `SELECT conrelid::regclass, conname FROM pg_constraint WHERE confrelid = 'garantia.politica_cobertura'::regclass;`. Con la tabla se van la columna `requirio_aprobacion_manual` de `cobertura_incumplimiento` y `fondo_garantia.politica_cobertura_id` (expandir/contraer: primero el código que deja de leerla) |
| Contracción de las tablas del aval (F2.L2.M04) | DECISION_REQUIRED | Borrar `aval_participante` y `ejecucion_aval` es destructivo sobre datos compartidos: decide Pablo, junto con `politica_cobertura`. Nada las escribe (el aval se retiró del código). Única FK: `ejecucion_aval.fk_ejecucion_aval_aval_id → aval_participante`; columnas ligadas en otros servicios: `identidad.referencia_personal.acepta_ser_avalista` (L4) y `grupos.configuracion_grupo.requiere_avalista`. Consulta de uso para TEST: [`evidencia/F2.L2.M04-uso-tablas-aval.sql`](./evidencia/F2.L2.M04-uso-tablas-aval.sql) (filas y montos por estado, grupos y referencias que las usan, FK) |
| F3.S3 / F3.L2.M04–M06 (modelo A) | DECISION_REQUIRED | ADR-056 y el plan ponen la opinión de Cumplimiento (F3.S3.M7: L-01, L-02, L-03) antes de implementar. El diseño quedó como propuesta, sin aplicar: [`evidencia/F3.S3-propuesta-modelo-A.md`](./evidencia/F3.S3-propuesta-modelo-A.md) (límites entre servicios, entidades a reusar y contraer, flujos, asientos lógicos sin códigos de cuenta, restricciones, parámetros y orden). Se aplica cuando Cumplimiento responda o si Pablo decide avanzar asumiendo L-01 |
| L2.S1.M2 | TODO | Precondición: L6 y L1 sueltan los consumidores (ver A medias). No es un bloqueo de ejecución: es el paso «contraer» y se hace apenas mergeen |
| F1.L2.M10 | TODO | Diseño listo, sin código: puerto `RetencionDelPozo.retener(entregaId, neto, clave) → Concedida(retencionId) / SaldoInsuficiente / NoDisponible`, doble en tres niveles (regla 65) y adaptador que falla cerrado hasta que L1 publique la operación en `nucleo-financiero`. La llamada va FUERA de la transacción de `CU28EmitirDesembolso` (invariante 6), así que la emisión se parte en «leer neto» → «retener» → «escribir orden». Depende además de F2.L2.M06 (la emisión deja de ser humana) |

## Evidencia

- F3.L2.M01: [`F3.L2.M01-diseno-entrega-automatica.md`](./evidencia/F3.L2.M01-diseno-entrega-automatica.md) (diseño),
  [`F3.L2.M01.a-turno-del-grupo.txt`](./evidencia/F3.L2.M01.a-turno-del-grupo.txt) (tabla aplicada en psql),
  [`F3.L2.M01.bc-sql-saga-y-copia.txt`](./evidencia/F3.L2.M01.bc-sql-saga-y-copia.txt) (SQL de la copia y de la saga
  con `ROLLBACK`: alta 1, repetido 0, versión mayor 1; la saga solo se reabre desde `EN_CURSO`),
  [`F3.L2.M01-integracion-local.txt`](./evidencia/F3.L2.M01-integracion-local.txt) (54 pruebas de integración de
  entregas más las 18 unitarias nuevas, todas en verde),
  [`F3.L2.M01-suites.txt`](./evidencia/F3.L2.M01-suites.txt) (spotless + `test` 44 + `webTest` 65 + `testBarrido` 2) y
  [`F3.L2.M01-boveda-y-esquema.txt`](./evidencia/F3.L2.M01-boveda-y-esquema.txt) (los 20 pasos del job con su código de
  salida real).

- [`evidencia/L2.S1.M1-retiro-cesion.txt`](./evidencia/L2.S1.M1-retiro-cesion.txt): grep de control, `test`/`webTest`,
  `check`/`testBarrido`/spotless, pacto de nucleo con entregas (6/6), `verificar_criterios`, `verificar_pruebas_web`,
  `verificar_seguridad`, `verificar_boveda`, Postman regenerado.
- [`evidencia/L2.S1.M1-rdes-psql.sql`](./evidencia/L2.S1.M1-rdes-psql.sql) y su [salida](./evidencia/L2.S1.M1-rdes-psql.out):
  el SQL de las pruebas R-DES-04/05 reescritas, corrido contra el esquema real (PostgreSQL 16 local con `sql/aplicar.sql`):
  dispara `uq_oferta_turno_activa`, `ck_cesion_derecho_liquidada`, `uq_cesion_derecho_turno_viva` y
  `ck_cesion_derecho_partes`.

## No cubierto

- F3.L2.M01: la integración de entregas corrió contra una **copia local** de Postgres (sin Docker no hay
  Testcontainers), con la URL cambiada solo en el disco y restaurada después. Falta la corrida del CI. No se
  ejercitó: el broker Kafka real ni el contenedor de listeners; la caída real del contenedor de `aportes` (se simuló
  con el doble del puerto `RecaudoDelPozo`); el arranque del contexto Spring con la función encendida; los productores
  reales (no existen); el reintento entre dos réplicas a la vez (el fondeo ya es único por turno y está probado con
  ocho pedidos simultáneos en `CU133PozoCompletoConcurrenciaTest`, pero no con dos barridos).

- `integrationTest`, `contractTest` y `sagaTest` de `entregas` no corrieron acá (sin Docker): `CU22PozoYMercadoRechazosTest`
  reescrita queda `NO COMPROBADO` localmente; su SQL sí se ejercitó con `psql`. Comando:
  `./gradlew :servicios:entregas:integrationTest --tests '*CU22PozoYMercadoRechazosTest*'`.
- Piso de cobertura (`jacocoTestCoverageVerification`, pisos sin cambios 0.89/0.71/0.71): se mide en el CI con los
  cinco corredores.
- Fuera de alcance que todavía llama a `anotarRespuestaDesembolso`: `packages/simulado/ejemplos/entregas/anotarRespuestaDesembolso.json` y `despliegue/ensayos/integracion/h14.py:580` (ahora reciben 410).
- La app móvil sigue llamando a la subasta: hoy recibe 410 y muestra un error genérico (L6).

## Desvíos del plan

- F3.L2.M01: el diseño decía «reintento con backoff y dead-letter» de Kafka si el aviso llega antes que los turnos.
  Se cambió por una saga persistida en `entregas.estado_saga` (ADR-028) con barrido: el contenedor de Spring Kafka
  descarta el mensaje tras unos pocos reintentos seguidos, y eso perdería el aviso. La marca del evento y la saga van
  en la misma transacción; el aviso no se pierde aunque los turnos lleguen horas después.
- F3.L2.M01: hubo que tocar `README.md` y seis archivos de `planes/` (350 → 351 tablas), porque `verificar_boveda`
  falla si las cifras citadas no coinciden. Regenerar la bóveda Obsidian (`generar_boveda.py`) destapó cinco
  entidades de otros lotes sin caso de uso (`desbloqueo_etapa`, `disponibilidad_servicio_dia`, `etapa`,
  `puja_subasta`, `subasta_cupo`) y una cifra desfasada en el plan maestro. Se revirtió: no lo verifica el CI y es de
  otros dueños. Por eso `docs/Modelos/` todavía no tiene la nota de `turno_del_grupo`.
- F3.L2.M01.e no estaba en el plan: apareció en la integración y se agregó como microtarea. F3.L2.M01.f la completa
  a pedido del coordinador. Las cifras de `planes/` y `docs/Seguridad.md` pasan a 184 restricciones.
- `ck_incidencia_entrega_origen` (los valores de la columna) nace solo con la tabla nueva: el generador no agrega
  CHECK de columnas en bases existentes. R-DES-06 sí llega a las existentes, y ya limita los valores posibles.

- **Alcance ampliado por el coordinador** (2026-11-21): `plataforma/comun-web` (410 común, #462),
  `scripts/generar_compose.py` (#463), `scripts/modelo.py` y `generar_ddl.py` (#464). La prueba de #464 destapó que
  un `DROP`/`ADD` en dos sentencias deja la tabla sin CHECK si el `ADD` falla; los estrechados se emiten en un único
  `ALTER`. El punto 3 (fichas CU-22, CU-23 y CU-26) quedó sin empezar: ver Pendiente. No hay ficha `CU-133` en la
  bóveda, así que F2.L2.M01 no depende de ninguna.

- **CI de #419, job Frontend en rojo** (clasificación `PRODUCT_BUG` de este lote, regla 80.4): con `deprecated: true` en
  las operaciones de subasta, el cliente Dart generado marcó los métodos y `apps/movil` (que todavía los llama) falló
  `dart analyze --fatal-infos` con 6 `deprecated_member_use`. Corregido en #419 (commit «las operaciones de subasta y
  mercado retiradas no llevan deprecated»): sin el flag, con el retiro declarado en el summary, la versión y el 410.
  Verificado que el cliente Dart regenerado ya no anota esos métodos (Flutter no está en esta nube). Propagado a toda
  la pila. Las operaciones retiradas en #422, #424 y #429 sí llevan `deprecated`, porque ningún cliente compilado
  del monorepo las llama (solo `packages/simulado` y un ensayo en Python).
- **CI de #424, «Los cinco corredores» en rojo** (clasificación `TEST_BUG`, regla 80.4): `CifradoDelNumeroDeCuentaTest`
  excedía el límite de 5 s por prueba del corredor `test` («idaYVuelta() timed out after 5 seconds»; localmente
  4,948 s). El costo era Mockito simulando clases `final` (`Datos`) y `DSLContext`, no el cifrado. Corregido en #421
  sin tocar el límite: la invariante pasó al átomo `NumeroDeCuentaProtegido` (enmascarado, hash y cifrado del mismo
  número por construcción), que usa CU-18; su prueba pura corre en 0,247 s. Propagado a la pila.

- El anexo pedía 404/410 «retirando del OpenAPI». Se eligió 410 manteniendo las operaciones `deprecated` (AMB-1):
  borrarlas rompía el build de la app (cliente Dart generado) y la prueba de pacto de nucleo, ambos fuera de alcance.
- `CU22PozoYMercadoRechazosTest` no se borró: `verificar_criterios.py` exige que CU-22 tenga prueba de rechazo de
  R-DES-04/05, y esas restricciones siguen vivas en la base. Se reescribió con SQL directo en vez de usar el mercado.
- `scripts/generar_catalogo_operaciones.py` cambia 600 líneas de dos catálogos por causas ajenas a este cambio
  (entradas `src-*`); no lo verifica el CI y se revirtió. Queda anotado para el dueño de auditoría.

## Riesgos residuales

- F3.L2.M01: la FK `incidencia_entrega.reportada_por` pasó a `ON DELETE SET NULL`, que es la convención del generador
  para una FK que admite nulos. Borrar un usuario dejaría sin autor las incidencias que reportó. Hoy los usuarios no
  se borran físicamente; si eso cambia, hay que revisarlo.
- F3.L2.M01: `turno_del_grupo` es una réplica. Si `grupos` cambia un titular y el evento se atrasa, el fondeo usa el
  titular viejo. `RegistroDelFondeo` sigue dándole prioridad al título de una cesión. El desfase máximo tolerable
  (98.4.3) y su alerta quedan para cuando exista el productor.

- F2.L2.M01: desde el despliegue, un turno con faltante queda BLOQUEADO hasta que lleguen los aportes que faltan; nadie
  lo cubre hasta F3.L2.M05 (fondo del grupo). Es lo que pide el plan, pero en `test` cambia lo que ve quien pruebe un
  pozo incompleto: antes lo cubría la reserva. `garantia` sigue exponiendo el respaldo hasta F2.L2.M02.

- En TEST pueden quedar subastas ABIERTAS con pujas retenidas o compras a medias con saldo retenido en la billetera
  (datos sintéticos, P-14). El trabajo que cerraba subastas se retiró. Consulta previa a promover a `test`:
  `SELECT estado, count(*) FROM entregas.subasta_cupo GROUP BY 1; SELECT estado, count(*) FROM entregas.puja_subasta GROUP BY 1; SELECT estado, count(*) FROM entregas.cesion_derecho GROUP BY 1;`
  Si hay retenciones vivas, se liberan con la operación de liberación de `nucleo-financiero` (nunca con `UPDATE`).

## Decisiones y ambigüedades

- **DECISION_REQUIRED (F3.L2.M01), para Pablo** (el coordinador la elevó el 2026-11-21): qué método de desembolso
  lleva una entrega que nace sola. Por indicación del coordinador quedó detrás del puerto `MetodoDeDesembolso`.
  - En las pruebas lo cumple un doble con la propuesta: el método sale de la cuenta VERIFICADA del beneficiario y, sin
    cuenta verificada, la saga espera sin fondear.
  - En producción, un adaptador provisorio (`MetodoDeDesembolsoConfigurado`) usa un solo método por configuración; si
    falta, el servicio no levanta.
  - No se cambió la nulabilidad de `entrega_fondo.metodo_desembolso`. Publicado en el PR #490.
- F3.L2.M01: la incidencia que abre el sistema queda con `reportada_por` NULL y `origen = SISTEMA`; no se siembra un
  usuario de sistema (pedido del coordinador: la tabla de usuarios no va en seeders). R-DES-06 impide una incidencia
  de persona sin autor.
- Hallazgo al probar R-DES-06: la primera versión del CHECK aceptaba una fila con `origen` NULL y sin autor, porque un
  CHECK que da NULL se acepta. Se corrigió con `COALESCE(origen, 'PERSONA')` y el caso quedó en la prueba psql.

- AMB-1 (expandir/contraer con 410) y AMB-2 (el fondeo sigue pagándole al cesionario de una cesión LIQUIDADA o con
  título asignado, para no pagar dos veces el turno): supuestos tomados; confirmar con Pablo en el PR.
- Códigos nuevos: `AP-CU130-10`, `AP-CU136-09`, `AP-CU28-08`, `AP-CU22-10`, `AP-CU22-11` (entregas) y
  `AP-CU23-14`, `AP-CU26-08`, `AP-CU29-06` (garantía), todos 410. No se reusa ningún código.
- `OperacionRetirada` y `RetiroDeOperaciones` estaban duplicadas en `entregas` y `garantia`. Con el alcance ampliado
  por el coordinador (punto 5), `OperacionRetirada` pasó a `plataforma/comun-dominio` y el 410 a
  `ManejadorGlobalDeErrores`; las copias se borraron ([evidencia](./evidencia/punto5-410-plataforma.txt)).
- Se borró `CU27ReclamoEnCursoTest.reclamoEnCursoNoEjecutaElAval`: probaba la guarda de reclamos para el aval
  (código 26), que ya no tiene quien la llame. La guarda sigue probada para CU-27.
