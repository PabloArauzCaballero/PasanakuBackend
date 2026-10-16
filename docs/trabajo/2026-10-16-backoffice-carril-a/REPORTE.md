# Reporte — Backoffice financiero · Carril A (dominio, contratos y controles)

> **AVANCE: 36 / 38 — 94,7 %.** Las 2 microtareas no hechas están `BLOQUEADO` por
> `DECISION_REQUIRED` (H7.S4 solicitudes escaladas D-15, H7.S5 registro de accesos). El carril A
> del plan estratégico (A0–A7) **no** está completo: este plan cubre A0, los cortes P0 de A4/A5/A6
> que no esperan decisiones y la ola 2 (llamadas del backoffice sin contrato); el resto queda en
> «Pendiente» con su motivo.

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `codex/backoffice-dominio-20261015`
  (worktree `.claude/worktrees/backoffice-dominio`, integrada con `origin/dev` @ `4b4cb2e5`)
- Peldaño de evidencia alcanzado: **`TESTED`** en `nucleo-financiero`, `cumplimiento` y `erp`
  (integración contra PostgreSQL real con Testcontainers + contrato HTTP con `MockMvc` + gates de la
  bóveda). **No `VERIFIED`**: ningún servicio se ejercitó levantado de punta a punta (ver «No cubierto»).

## Completado

| ID | Qué se logró (observable) | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Matriz de trazabilidad pantalla→CU→servicio→operación→permiso→estado, **generada** desde las fuentes | `python3 .../entregables/matriz.py --verificar` | PASS · `evidencia/H1-01-matrices.txt` |
| H1.S1.M2 | Matriz de autorización: 37 permisos sembrados, roles, CU; 33 no catalogados (= `verificar_seguridad.py`) | idem + `verificar_seguridad.py \| grep S-8` | PASS · H1-01 |
| H1.S1.M3 | Convenciones de contrato citadas con `archivo:línea` | `matriz.py --citas` | PASS 20/20 · H1-02 |
| H1.S2.M1 | Inventario de las 19 tablas `regla_/politica_/decision_/version_` | `diff` contra `ls sql/10_tablas` | PASS · H1-03 |
| H1.S2.M2 | ADR propuesto del núcleo de decisión y gobierno de políticas (cubre §2.1–§2.7) | checklist 7/7 | PASS · H1-03 |
| H2.S1.M1 | Contrato `GET /billetera/retiros` | `compileJava` | PASS |
| H2.S2.M1–M3 | Bandeja de retiros en revisión (keyset, sin destino ni persona, `puedeDecidir`) | `integrationTest --tests '*CU11BandejaTest*'` | PASS 5/5 · H2-01 |
| H2.S2.M2 | Permiso, 401/403/400/422 y forma JSON | `webTest` (BilleteraBandejaWebTest) | PASS 7/7 · H2-04 |
| H2.S2.M4 | **Rojo preexistente** corregido: `ArquitecturaTest` (web leía infraestructura) | `:servicios:nucleo-financiero:test` | PASS 81/0 · H2-02 |
| H2.S2.M5 | **Rojo preexistente** corregido: 44/44 `webTest` sin contexto | `webTest` | PASS 51/0 · H2-04 |
| H2.S2.M6 | `testBarrido` verde (preexistente + propio) | `testBarrido` | PASS · H2-08 |
| H2.S3.M1 | `aprobarRetiro` usa su `Idempotency-Key`: reintento = mismo recibo, 1 evento, 1 métrica | `--tests '*CU11AprobacionIdempotenciaTest*'` | PASS 6/6 (rojo primero: H2-05) · H2-06 |
| H2.S3.M2 | Regresión de retiros | `integrationTest --tests '*CU11*'` | PASS 71/0 · H2-06 |
| H3.S1.M1–M6 | Reclamos: bandeja, ficha, respuesta idempotente por estado, códigos propios | `--tests '*CU52*'` + `webTest` | PASS (7+5+11) · H3-01…H3-04 |
| H4.S1.M1–M3 | Ejercicios y período vigente (zona Bolivia, sin inventar período) | `--tests '*CU100PeriodosTest*'` + `ErpPeriodosWebTest` | PASS (4+6) · H4-01…H4-03 |
| H5.S1.M1–M2 | Bandejas de alertas y casos LFT sin persona/monto/detalle | `--tests '*CU44BandejaTest*'` + `LftBandejaWebTest` | PASS (5+5) · H5-01, H5-02 |
| H5.S2.M1–M3 | Abrir/decidir caso LFT: revisor = sesión, una decisión, reintentos seguros | `--tests '*CU44*'` + `LftAccionesWebTest` | PASS (27+6) · H5-04…H5-06 |
| H6.S1.M1 | Regresión final sobre el árbol integrado con `dev`, sin caché | ver «Evidencia» | PASS 1.136/0 · H6-03 |
| H6.S1.M3 | 11 fichas de contrato para el carril B | `grep -c '^### '` + operationIds en YAML | PASS 11/11 · H6-05 |
| H6.S1.M2 | Reporte del tramo 1 + commits + push | `git log origin/codex/backoffice-dominio-20261015 -1` | PASS · H6-06 |
| H7.S1.M1 | Listados de publicidad que B ya llamaba (anunciantes, campañas, piezas) | `--tests '*CU11[012]ListadoTest'` + `PublicidadListadosWebTest` | PASS (7+5) · H7-02…H7-04 |
| H7.S1.M2 | **Privacidad:** el número de documento ya no va al MDC de los logs (publicidad, erp) | `grep -rn 'marcarCasoDeUso(.*getNumeroDocumento' servicios` | PASS (vacío) · H7-05 |
| H7.S2.M1 | Períodos de un ejercicio con totales **sellados** del cierre | `--tests '*CU100PeriodosTest*'` + `ErpPeriodosWebTest` | PASS · H7-06 |
| H7.S3.M1 | Descarga del estado financiero: se reconstruye, se recalcula el hash y solo se entrega si coincide | `--tests '*CU106DescargaTest*'` + webTest | PASS (3+1) · H7-07, H7-08 |
| H7.S6.M1 | Cruce final: de 33 llamadas de B, quedan 2 sin contrato, ambas `DECISION_REQUIRED` | `entregables/llamadas_sin_contrato.py .` | PASS · H7-10 |

## A medias

Ninguna microtarea quedó a medias. A nivel de **hito**, H2–H5 quedan en `TESTED` y no en
`VERIFIED`:

### H2–H5 — prueba de punta a punta con los servicios levantados
- Qué anda: cada operación nueva está probada contra PostgreSQL real (caso de uso, RLS con rol
  `BACKOFFICE`, restricciones de la base, concurrencia) y por HTTP con la cadena real de seguridad
  (guardia de permisos, deserializador estricto, manejador de errores).
- Qué no anda / no se probó: la petición atravesando gateway → servicio levantado → base, con un
  token emitido por `identidad`.
- Qué falta exactamente: levantar el perfil `todo` y repetir con `curl` los casos de las fichas.
  `./gradlew bd:levantar` falla en esta máquina porque quay.io responde 401 a la imagen de MinIO
  (`ENVIRONMENT`, ver Evidencia); hay que resolver ese acceso o fijar una imagen alternativa.
- Dónde quedó: rama `codex/backoffice-dominio-20261015`, compila, todas las suites tocadas en verde.

## Pendiente

| Bloque del plan estratégico | Estado | Qué lo destraba |
|---|---|---|
| H7.S4 · `GET /grupos/solicitudes-escaladas` (D-15) | `BLOQUEADO` · `DECISION_REQUIRED` | negocio: ¿vencer = expirar o escalar a Operaciones, y quién resuelve? + columna de plazo guardado (invariante 8). Evidencia H7-09 |
| H7.S5 · `POST /extraccion/accesos` | `BLOQUEADO` · `DECISION_REQUIRED` | catálogo de finalidades (A-8); el rastro de lectura va en el servidor |
| A1 · catalogar los 33 permisos S-8 (`entregables/matriz-autorizacion.md`) | `DECISION_REQUIRED` | Seguridad de la información decide recurso/acción/MFA/rol |
| A1 · `aprobado_por` en restablecimiento y política de token por perfil (§9.4–9.5) | `DECISION_REQUIRED` | cambio de modelo con ADR (`boveda-modelo`) |
| A2/A3 · tablas del motor de decisión y del gobierno de políticas | `DECISION_REQUIRED` | aprobar `entregables/ADR-propuesto-motor-de-decisiones.md` (A-2) |
| A4 · conciliación (CU-50, sin operaciones), cierre diario (CU-51, sin implementar), bandeja de desembolsos/reversos (CU-28/CU-14, sin lectura atribuida) | TODO | siguiente ola, mismo patrón que H2 |
| A5 · UIF y requerimientos (CU-43/45), riesgo/incidentes/continuidad (CU-54/55/56), comité (CU-94), denuncia (§9.1) | TODO / `DECISION_REQUIRED` (denuncia) | siguiente ola; denuncia exige modelo |
| A5 · ficha de alerta/caso con datos de la persona | `DECISION_REQUIRED` | catálogo de finalidades para `registro_acceso_datos` (A-8) |
| A6 · listados de ERP que la UI ya llama (presupuestos, OC, facturas, CxC, activos), tarifario, mensajería, reportes, publicidad | TODO | siguiente ola |
| A7 · paneles, alertas con runbook, backups | TODO | siguiente ola |

## Evidencia

Índice de `evidencia/` (salidas literales; cada archivo empieza con el comando):

| Archivo | Qué prueba |
|---|---|
| `H1-01-matrices.txt` · `H1-02-citas-convenciones.txt` · `H1-03-adr-inventario.txt` | entregables A0 regenerables y citas válidas |
| `H2-01…H2-08` | bandeja y aprobación de retiros; rojo primero (H2-05); regresión `nucleo-financiero` |
| `H3-00-linea-base-cumplimiento.txt` · `H3-01…H3-04` | línea base verde antes de tocar; reclamos |
| `H4-00-linea-base-erp.txt` · `H4-01…H4-03` | línea base; ejercicios y período vigente |
| `H5-01…H5-06` | bandejas LFT; decisión y apertura de casos |
| `H6-01-gates-repo.txt` | `verificar_criterios` (sin divergencias), `verificar_boveda` (TODO OK), Postman sin divergencia, matrices al día |
| `H6-02-compilacion-completa.txt` | todo el backend compila sin caché |
| `H6-03-regresion-final.txt` | regresión final de los tres servicios |
| `H6-04-contractTest-clasificacion.txt` | 9 fallas de contrato clasificadas con reproducción |
| `H6-05-fichas.txt` · `H6-06-push.txt` | fichas de contrato ↔ operationIds; push del tramo 1 |
| `H7-00-llamadas-sin-contrato.txt` · `H7-10-cruce-final.txt` | cruce de las 33 llamadas de B contra los OpenAPI: 7 → 2 |
| `H7-01-linea-base-publicidad.txt` · `H7-02…H7-04` | línea base y listados de publicidad |
| `H7-05-sin-documento-en-trazas.txt` | ningún `marcarCasoDeUso` con número de documento |
| `H7-06…H7-08` | períodos del ejercicio, documento sellado, regresión `erp` |
| `H7-09-d15-sin-modelo.txt` | por qué D-15 es `DECISION_REQUIRED` |

```text
$ ./gradlew :servicios:<s>:spotlessCheck test webTest testBarrido integrationTest contractTest --continue --no-build-cache
== nucleo-financiero   test 81 · webTest 51 · testBarrido 2 · integrationTest 455 · contractTest 10   — 0 fallas
== cumplimiento        test 14 · webTest 70 · testBarrido 2 · integrationTest 306                     — 0 fallas
== erp                 test 5  · webTest 43 · testBarrido 2 · integrationTest 95                      — 0 fallas
TOTAL 1136 pruebas · 0 fallas

# ola 2 (después del tramo 1)
== publicidad          test 5  · webTest 21 · testBarrido 2 · integrationTest 59                      — 0 fallas
== erp                 test 5  · webTest 45 · testBarrido 2 · integrationTest 100                     — 0 fallas
```

**Asientos / dinero (regla 91.6):** ningún cambio de este trabajo crea, modifica ni reversa asientos
ni movimientos. La aprobación de retiros reutiliza la transición existente (ADR-049); lo nuevo es el
recibo idempotente, probado con: misma clave dos veces → 1 transición, 1 evento
`retiro_autorizado`, +1 en `withdrawal_approved_total` (H2-06), y concurrencia (2 envíos → 1).

**Seguridad (regla 90.6):**

| Amenaza | Control | Prueba |
|---|---|---|
| Suplantar al revisor de un caso LFT mandando `revisadoPor` | revisor = sesión; el contrato rechaza el campo | `LftAccionesWebTest` «400: el cuerpo no puede declarar revisadoPor…» |
| Revisarse a sí mismo | analista leído del caso **guardado** | `CU44DecisionTest` «rechaza que el analista…» |
| Doble decisión / doble aprobación | `UPDATE` condicional verificado + reintento por estado/clave | `CU44DecisionTest` concurrencia; `CU11AprobacionIdempotenciaTest` concurrencia |
| Fuga de datos de la persona en colas | respuesta mínima sin persona/monto/detalle | `LftBandejaWebTest`, `ReclamosBandejaWebTest`, `BilleteraBandejaWebTest` (`doesNotContain`) |
| Leer colas sin permiso | `@Permiso` por operación; 401/403 | matriz por clase web + `SeguridadWebTest` (sábana de todas las rutas) |

Riesgo residual de seguridad: ver «Riesgos residuales».

## No cubierto

- Peldaño `VERIFIED`: ninguna operación se ejercitó con el servicio levantado y token real (ver «A medias»).
- Accesibilidad / UI: fuera del carril (B).
- `contractTest` de `plataforma:comun-pruebas` en rojo por una ruta de `identidad` que trajo `dev`
  (H6-04): no se tocó; el resto de `contractTest` pasa (inversiones pasa 23/23 con `python`→`python3`).
- Volumen: las bandejas usan índices existentes (`ix_orden_retiro_estado`, `ix_reclamo_cliente_*`,
  `ix_alerta_monitoreo_lft_*`); el orden por `COALESCE(plazo_prorrogado_hasta, plazo_respuesta)` y por
  `caso_investigacion_lft.plazo_limite` no tiene índice propio. Sin prueba de carga.
- Clientes generados (`clientes/angular`, `clientes/dart`): los genera el CI; no se verificó el
  cliente Angular resultante.

## Desvíos del plan

1. **Rojos preexistentes corregidos** (no estaban en el plan; se agregaron como H2.S2.M4–M6): en
   `e3e11457` y en `dev` ya fallaban `ArquitecturaTest`, las 44 `webTest` y `testBarrido` de
   `nucleo-financiero`. Se corrigieron porque bloqueaban la verificación del módulo tocado.
2. **Bóveda modificada** (decisión D-1): criterios y códigos en CU-11, CU-44, CU-52 y CU-100.
   `verificar_criterios.py` rechaza una prueba sin su criterio en la bóveda.
3. **Pruebas existentes ajustadas** (con el mismo o mayor rigor):
   - `CU52Test.reintento`: su nombre ya decía "devuelve la misma respuesta"; su cuerpo exigía un
     error. Ahora verifica mismo recibo + 1 cierre + 1 evento.
   - `CU52Test.concurrencia`: conserva su aserción (1 error, 1 evento) con dos respuestas distintas.
   - Pruebas web de aprobación de retiros: la firma nueva lleva la clave; ahora además verifican que
     clave y motivo lleguen al caso de uso.
   - `ArranqueTest` de `erp`: 15 → 17 rutas del contrato.
   - `CU44Test` pasa **sin cambios**.
4. **Defectos de CU-44 corregidos** al preparar su exposición HTTP (H5.S2): `decidir` confiaba en el
   cuerpo y duplicaba efectos; `abrirCaso` creaba un caso vacío al reintentar; códigos reusados.
5. **Fichas de contrato** (H6.S1.M3) agregadas: COORDINACION.md las exige antes de integrar con B.
6. **Integración de `origin/dev`** (merge `70502103`, sin conflictos en archivos del carril).
7. **Ola 2 (H7) agregada al plan** tras cerrar el tramo 1: cruce de llamadas reales del backoffice.
8. **Privacidad en trazas** (H7.S1.M2): descubierto al tocar `publicidad`; mismo defecto en `erp`.
9. **Pruebas partidas por CU** en `publicidad`: `integrationTest` solo incluye `CU*Test`; una clase
   `ListadosDePublicidadTest` no habría corrido nunca en el CI.
10. **`serializar` de CU-106 extraído** a `dominio/DocumentoDeEstadoFinanciero` para que generación y
    descarga usen una sola definición (comportamiento idéntico: suites de CU-106 en verde).

## Riesgos residuales

| Riesgo | Impacto | Qué hacer |
|---|---|---|
| A-5: `aprobarRetiro` promete 409/404 y emite 422; "orden inexistente" comparte `AP-CU11-01` con `SALDO_INSUFICIENTE` | B puede mostrar "saldo insuficiente" para una orden que no existe | asignar `AP-CU11-15 ORDEN_INEXISTENTE` y decidir 409 vs 422 con el dueño de `errores-api` |
| A-7: importes sin moneda en `reclamo_cliente` y `alerta_monitoreo_lft` | viola 91.1.2; la UI no puede rotular la moneda | cambio de modelo (columna `moneda`) |
| `CU48CalibrarReglas.activar` recibe `aprobadaPor` en la entrada | si se expone por HTTP tal cual, se puede declarar otro aprobador | aplicar el patrón de `CU44DecidirComoRevisor` antes de exponerlo |
| Ruta de `identidad` `PUT /usuarios/{id}/documento/fecha-expiracion` no declarada como abierta | gate de autorización en rojo en `dev` | declararla en la política de acceso con su control (dueño: identidad) |
| Caché de Gradle local cruza salidas del generador OpenAPI entre servicios | compilaciones falsas en rojo (visto en `entregas`) | usar `--no-build-cache` como el CI, o arreglar las entradas de la tarea de generación |
| `identidad/web/SesionesController.java:96` pone el **teléfono** en el MDC en cada login | dato personal en logs (regla 90.2.1) | mismo arreglo que en publicidad/erp, por su dueño |
| `crearCampana`: el YAML dice `PUBLICIDAD_APROBAR_CAMPANA`, el código exige `PUBLICIDAD_CAMPANA_GESTIONAR` | documentación engañosa del permiso | corregir el YAML (dueño de publicidad) |
| `Traza.marcarCasoDeUso(cu, usuarioId)` se usa con ids de recurso | logs con "usuario" que no es un usuario | renombrar el parámetro (plataforma, micro-PR) |
| Respuestas idempotentes por estado (reclamos, LFT) comparan el texto completo | un reintento con un espacio de diferencia es "otra respuesta" (`AP-…-06/07`) | la UI debe reenviar exactamente el mismo cuerpo |

## Decisiones y ambigüedades

| # | Decisión / supuesto | Confirmar con |
|---|---|---|
| A-1 | Entregables de A0 en `docs/trabajo/…/entregables/`, estado *propuesto* | Pablo Arauz (dueño del repo) |
| A-2 | El carril no edita `sql/` ni `.puml`: motor y gobierno quedan como ADR propuesto | Pablo Arauz + Riesgos + Seguridad |
| A-3 | Bandeja de retiros con `RETIRO_APROBAR`; reclamos con `RECLAMO_VER`/`RECLAMO_ATENDER`; Contabilidad con cualquiera de sus 8 permisos | Seguridad de la información |
| A-4 | Filas de bandeja sin datos personales | Cumplimiento |
| A-5 | Se mantiene 422 por convención de la casa aunque el YAML diga 409/404 | dueño de `errores-api` |
| A-6 | Códigos nuevos `AP-CU52-05/06/07`, `AP-CU44-05…08`, `AP-CU100-04`, `AP-CU11-14` | dueños de CU-52, CU-44, CU-100, CU-11 |
| A-7 | Importe sin moneda publicado como cadena, sin suponer BOB | dueño del modelo M12 |
| A-8 | Fichas LFT con datos de la persona esperan el catálogo de finalidades | Oficial de Cumplimiento / Seguridad |
| D-1 | Criterios nuevos escritos en la bóveda de cada CU | Pablo Arauz |
