# Reporte — Tanda 2, lote L2 (garantía y entregas)

> **AVANCE: 5 / 14 — 35,7 %.**

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

## A medias

Además de lo que sigue, `CU22LiquidarEntrega.liquidar`/`ejecutar` y `CU23CubrirIncumplimiento` todavía existen, sin
camino HTTP. Se borran con el punto 3 (Pendiente).

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
| F2.L2.M02 | TODO | Después de F2.L2.M01 y del retiro del consumidor de tópicos de respaldo en `nucleo-financiero` (L1) |
| L2.S1.M2 | TODO | Precondición: L6 y L1 sueltan los consumidores (ver A medias). No es un bloqueo de ejecución: es el paso «contraer» y se hace apenas mergeen |
| F1.L2.M10 | TODO | Diseño listo, sin código: puerto `RetencionDelPozo.retener(entregaId, neto, clave) → Concedida(retencionId) / SaldoInsuficiente / NoDisponible`, doble en tres niveles (regla 65) y adaptador que falla cerrado hasta que L1 publique la operación en `nucleo-financiero`. La llamada va FUERA de la transacción de `CU28EmitirDesembolso` (invariante 6), así que la emisión se parte en «leer neto» → «retener» → «escribir orden». Depende además de F2.L2.M06 (la emisión deja de ser humana) |
| F2.L2.M01 | TODO | Corrección: no existe ficha `CU-133`, así que nada lo ata a la bóveda. Lo que falta es reescribir `CU133PozoCompleto*Test` (integración, Docker) para el fondeo sin respaldo: el faltante queda pendiente a la espera del fondo del grupo |
| Punto 3 (bóveda) | TODO | Reescribir CU-22 según ADR-055/056 (pozo completo por fondeo, sin liquidación con deducciones ni cuatro ojos: va junto con F2.L2.M06), sacar de CU-23 la cobertura manual y marcar CU-26 RETIRADO; después borrar `CU22LiquidarEntrega.liquidar`/`ejecutar` y `CU23CubrirIncumplimiento` y reemplazar sus pruebas de integración por las de los criterios nuevos. Conviene hacerlo en una sesión con Docker para correr esas pruebas antes del PR |

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

- En TEST pueden quedar subastas ABIERTAS con pujas retenidas o compras a medias con saldo retenido en la billetera
  (datos sintéticos, P-14). El trabajo que cerraba subastas se retiró. Consulta previa a promover a `test`:
  `SELECT estado, count(*) FROM entregas.subasta_cupo GROUP BY 1; SELECT estado, count(*) FROM entregas.puja_subasta GROUP BY 1; SELECT estado, count(*) FROM entregas.cesion_derecho GROUP BY 1;`
  Si hay retenciones vivas, se liberan con la operación de liberación de `nucleo-financiero` (nunca con `UPDATE`).

## Decisiones y ambigüedades

- AMB-1 (expandir/contraer con 410) y AMB-2 (el fondeo sigue pagándole al cesionario de una cesión LIQUIDADA o con
  título asignado, para no pagar dos veces el turno): supuestos tomados; confirmar con Pablo en el PR.
- Códigos nuevos: `AP-CU130-10`, `AP-CU136-09`, `AP-CU28-08`, `AP-CU22-10`, `AP-CU22-11` (entregas) y
  `AP-CU23-14`, `AP-CU26-08`, `AP-CU29-06` (garantía), todos 410. No se reusa ningún código.
- `OperacionRetirada` y `RetiroDeOperaciones` estaban duplicadas en `entregas` y `garantia`. Con el alcance ampliado
  por el coordinador (punto 5), `OperacionRetirada` pasó a `plataforma/comun-dominio` y el 410 a
  `ManejadorGlobalDeErrores`; las copias se borraron ([evidencia](./evidencia/punto5-410-plataforma.txt)).
- Se borró `CU27ReclamoEnCursoTest.reclamoEnCursoNoEjecutaElAval`: probaba la guarda de reclamos para el aval
  (código 26), que ya no tiene quien la llame. La guarda sigue probada para CU-27.
