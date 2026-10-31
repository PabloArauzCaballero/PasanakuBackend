# Plan — Arenas de reputación, inicio al 75 % y cierre de ingresos

- Fecha: 2026-10-30 · Repos: monorepo Pasanaku (`servicios/grupos`, `servicios/transparencia`, `sql/`, `apps/movil`, `packages/diseno_flutter`) · Predecesor: `2026-10-30-datos-entre-pantallas` (#339/#340, merge BLOQUEADO por el rojo de `test`)
- Rama: `claude/arenas-y-inicio-75-20261030` (desde `origin/test` `cfcb2908`), worktree `/Users/pablo/Documents/GitHub/Pasanaku-wt-datos`
- Pedido de Pablo 2026-10-30: «no se pueda iniciar si no está lleno a un 75 %; una vez iniciado no se puede meter a nadie más; no se puede invitar a personas de rangos inferiores; niveles como rangos de Clash Royale, continuo como arenas, ultra gamificado; orquestación con agentes en paralelo».
- Resultado observable: en la app, cada persona ve su arena y su avance continuo hacia la siguiente; el organizador no puede iniciar el grupo antes del 75 % y, al iniciarlo, el grupo se achica a los cupos ocupados y nadie más entra; nadie por debajo de la arena mínima del grupo puede entrar por ningún camino.
- Kill-test: (1) `POST /grupos/{id}/inicio` con 7/10 ocupados responde 2xx; (2) después de iniciar, un `aceptarInvitacionPorEnlace` o `canjearInvitacion` entra; (3) alguien con puntaje menor a la arena mínima entra por cualquiera de los caminos de ingreso.

## Decisiones de Pablo (AskUserQuestion, 2026-10-30)
| Pregunta | Decisión |
|---|---|
| De dónde salen los «trofeos» | **Del puntaje de reputación existente** (`transparencia.puntaje_reputacion.puntaje`, escala 0–1000, base 500, modelo v1 — `sql/60_semillas/16-reputacion-y-scoring.sql:5`). Una sola fuente de verdad; nadie escribe trofeos a mano (regla 91.4.2) |
| «Rangos inferiores» | **Arena mínima del grupo**: la elige el organizador al crear (por omisión, la suya). Nadie por debajo entra por invitación, enlace, solicitud, postulación ni traspaso |
| Inicio al 75 % | **Lo inicia el organizador**. Antes del 75 % el botón está bloqueado con motivo y conteo. Umbral = `ceil(0,75 × cupos_totales)` cupos `OCUPADO` |
| Cupos libres al iniciar | **El grupo se achica**: los cupos `LIBRE` dejan de existir para el ciclo; `cupos_totales` y `num_periodos` pasan a ser los ocupados (≥ 3 por `ck_grupo_num_periodos`) |
| Paralelismo | Hasta 4 agentes, lotes disjuntos; builds de a uno (`CLAUDE.md`) |
| Cupos RESERVADO al iniciar (2026-10-30) | **No se puede iniciar** mientras haya RESERVADO sin firmar (`AP-CU69-19 FIRMAS_PENDIENTES`) |
| Traspaso después de iniciar | **Permitido**, pero el entrante debe estar en la arena mínima o superior (`AP-CU64-04`); no agrega cupos |
| Ver la arena de otros integrantes | **Sí, solo la arena** (nombre e insignia, nunca el puntaje) dentro del grupo. Requiere `IntegranteDelGrupo.arena` en el contrato |
| Reglamento al achicar (2026-10-30) | **Versión nueva y re-firma**: al iniciar se publica una versión del reglamento con los números reales; cada integrante la acepta antes del sorteo, junto con la cuota 1 |
| Privacidad del puntaje (2026-10-30) | **Puntaje exacto solo propio**; la etapa de otro, solo si comparten grupo (grupos la expone en el detalle); grupos consulta transparencia con token de servicio |
| Desbloqueos financieros por arena | **Sí, con valores a confirmar**: catálogo por arena con cupos máximos y aporte máximo. Los valores se siembran en estado `PROPUESTO` y **solo se aplican los `VIGENTE`** (nada financiero se activa sin confirmación — regla 97.5.4) |

## Hechos (descubrimiento)
- Estados del grupo: `ck_grupo_estado` en `sql/10_tablas/02_grupos_turnos/grupo.sql:37`. Hoy `ABIERTO_A_INSCRIPCION → CONFORMADO` ocurre **solo** al ocupar el 100 % (`CU69FirmarReglamento.java:147-155`). **No existe ninguna transición a `EN_CURSO`** en `servicios/*/src/main`.
- «Iniciar» = transición nueva `ABIERTO_A_INSCRIPCION → CONFORMADO` invocada por el organizador (no un `UPDATE` genérico de estado — skill `rosca-group-lifecycle` §5), con historial en `historial_estado_grupo`.
- Ingreso aceptado hoy en `CONFORMADO` además de `ABIERTO` (`CU69Enlace.java:210`, `CU69EnlaceDeGrupo.java:75,196`) → contradice «una vez iniciado no entra nadie».
- `grupos.grupo.reputacion_minima NUMERIC(6,2)` existe (`grupo.sql:26`); se valida **solo** en `aceptarInvitacionPorEnlace` (`web/InvitacionesWeb.java:149-151`, código `AP-CU68-03`). Grupos ya lee la reputación por API (`afuera.reputacion(usuarioId)`).
- Caminos de ingreso en `grupos.yaml`: `canjearInvitacion`, `invitarAlGrupo`, `aceptarInvitacionPorEnlace`, `solicitarIngresoPorEnlace`, `decidirSolicitudDeIngreso`, `proponerAdmision`, `postulaciones`, `traspasarCupo`.
- Niveles de confianza existentes: `ck_puntaje_reputacion_nivel_confianza` (7 valores). Insignias: `transparencia.insignia_logro`.
- DDL generado por `scripts/generar_ddl.py` («no editar a mano»): la fuente de verdad del esquema es el modelo que lee ese script.

## Catálogo de arenas (interno, PROPUESTO por Claude 2026-10-30, confirmar con Pablo)
Procedencia: diseño de producto propio, sin fuente externa. Nombres originales (no se copian los de ningún juego). Tramos sobre el puntaje v1:

| Orden | Código | Nombre | Desde (incl.) | Hasta (excl.) |
|---|---|---|---|---|
| 0 | `CAMPAMENTO` | Campamento | 0 | 500 |
| 1 | `SEMILLA` | Semilla | 500 | 550 |
| 2 | `BROTE` | Brote | 550 | 600 |
| 3 | `AGUAYO` | Aguayo | 600 | 650 |
| 4 | `FERIA` | Feria | 650 | 700 |
| 5 | `MERCADO` | Mercado | 700 | 760 |
| 6 | `ALTIPLANO` | Altiplano | 760 | 820 |
| 7 | `SALAR` | Salar | 820 | 880 |
| 8 | `ILLIMANI` | Illimani | 880 | 940 |
| 9 | `CONDOR` | Cóndor | 940 | 980 |
| 10 | `INTI` | Inti | 980 | 1000 (incl.) |

Supuesto: quien recién llega (base 500) arranca en **Semilla**; Campamento es la zona de recuperación por debajo de la base.
Desbloqueos por arena (`cupos_max`, `aporte_max` + `moneda`): mecanismo implementado; valores sembrados `PROPUESTO` y **no aplicados** hasta pasar a `VIGENTE`.

## Alcance
- IN: catálogo de arenas y desbloqueos (esquema + semilla + API en transparencia); arena en la respuesta del puntaje; transición «iniciar» con 75 % y achique; cierre de ingresos tras iniciar; arena mínima en todos los caminos de ingreso; desbloqueos validados al crear y al entrar (solo `VIGENTE`); app: arena actual y camino continuo, selector de arena mínima, progreso al 75 % y botón Iniciar, errores de arena accionables.
- OUT: transición a `EN_CURSO` (sorteo/calendario); turnos por reputación; cambio del modelo de scoring; backoffice; valores definitivos de desbloqueos.
- Coordinación (2026-10-30): pasanaku-1f registra la regla de Pablo para CU-60, «sorteo con ≥ 75 % ocupado y primer aporte acreditado por cada ocupante». Con el achique de esta rama, al iniciar el grupo queda al 100 % y CU-60 solo debe sumar la precondición del primer aporte. Se le propuso no relajar AP-CU60-01. `CU60Sortear.java` y `puedeSortear` son de pasanaku-1f; esta rama no los toca. Si su regla implica sortear con cupos libres sin achicar, es una contradicción y la decide Pablo (DECISION_REQUIRED).
- Ambigüedades: nombres y tramos de arenas (propuestos); valores de desbloqueo (Pablo/cumplimiento); si un traspaso de cupo a alguien de arena menor también se bloquea (supuesto: sí, es un ingreso).

## H1 — Arenas visibles y reglas de inicio e ingreso cumplidas en el servidor
**CA:** Dado un grupo de 10 cupos con 7 ocupados, cuando el organizador intenta iniciarlo, entonces recibe 422 con el conteo; con 8 ocupados lo inicia, el grupo queda en 8 cupos/8 periodos y cualquier intento de ingreso posterior responde 422; nadie bajo la arena mínima entra por ningún camino; la app muestra arena y progreso.
**DoD:** tests de integración de grupos y transparencia contra Postgres real; tests de widget de la app; `flutter analyze`; regresión comparada; verificación de deriva de esquema; segunda corrida de semillas idempotente.
**Estado:** TODO

### H1.S0 — Contrato (un agente, antes de los carriles)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S0.M1 | OpenAPI: `iniciarGrupo`, `arenaMinima` en crear/detalle, `GET /reputacion/arenas`, `arena` en el puntaje, códigos de error nuevos | Los contratos validan y el código generado compila | `./gradlew :servicios:grupos:compileJava :servicios:transparencia:compileJava` → BUILD SUCCESSFUL | TODO |

### H1.S1 — Carril A · transparencia (backend)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Esquema `arena` + `desbloqueo_arena` desde la fuente de verdad, semilla idempotente con procedencia | Tablas creadas; segunda corrida no inserta | aplicar + deriva + segunda corrida (salida pegada) | TODO |
| H1.S1.M1b | Corrección del revisor: RLS con `Datos.comoSistema`, tests con el rol `svc_transparencia`, semillas con `actualizar` | 7/17 fallan sin el arreglo; segunda corrida sin cambios | `evidencia/h1-s1-a-*.txt` | HECHO |
| H1.S2.M0 | Corrección del revisor B: autorizar antes de consultar la arena (proponer/resolver/traspaso), reintento idempotente antes de la arena | Tests `ArenaMinima*`, `CU68AdmisionTest`, `CU20ArenaMinimaTest` | `./gradlew :servicios:grupos:test` → 573 tests, 0 fallas | HECHO |
| H1.S2.M2 | `GET /reputacion/arenas` y `arena` en `consultarPuntaje` | Puntaje 549,99 → SEMILLA; 550 → BROTE; 1000 → INTI | `./gradlew :servicios:transparencia:test --tests '*Arena*'` | TODO |

### H1.S2 — Carril B · grupos (backend)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `iniciarGrupo` (HECHO, 573 tests en verde): organizador, ≥75 %, achique, historial, idempotente | 7/10 → 422; 8/10 → 8 cupos/8 periodos; repetir → mismo resultado | `./gradlew :servicios:grupos:test --tests '*Iniciar*'` | TODO |
| H1.S2.M2 | Cierre de ingresos tras iniciar en todos los caminos | Cada camino responde 422 sobre un grupo `CONFORMADO` | `--tests '*IngresoCerrado*'` | TODO |
| H1.S2.M3 | Arena mínima en todos los caminos + desbloqueos `VIGENTE` al crear y entrar | Puntaje bajo la mínima → 422 `AP-CU68-03` en cada camino | `--tests '*ArenaMinima*'` | TODO |

### H1.S3 — Carril C · app, grupos
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Selector de arena mínima en crear grupo | Por omisión la propia; no deja elegir por encima de la propia | `flutter test test/widget/crear_grupo_arena_test.dart` | TODO |
| H1.S3.M2 | Progreso al 75 % y botón Iniciar con confirmación | Bloqueado con «faltan N» antes del umbral | `flutter test test/widget/iniciar_grupo_test.dart` | TODO |

### H1.S4 — Carril D · app, gamificación
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Arena actual + barra continua hacia la siguiente + camino de arenas | Puntaje 575 → Brote, 50 % hacia Aguayo | `flutter test test/widget/camino_de_arenas_test.dart` | TODO |
| H1.S4.M2 | Insignia de arena en la tarjeta de integrante | Se ve el nombre de la arena con nombre accesible | mismo test | TODO — desbloqueado por decisión de Pablo; depende de H1.S4.M3 |
| H1.S4.M3 | Contrato y backend: `IntegranteDelGrupo.arena` (código + nombre, nunca puntaje), solo para quien comparte el grupo | El detalle trae la arena de cada ocupante; un no integrante no la ve | `./gradlew :servicios:grupos:test --tests '*Detalle*'` | TODO — después de que cierre el carril B |

### H1.S5 — Integración (sesión principal)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S5.M1 | Generadores compartidos (errores, postman, clientes) y suites en serie | CI local equivalente en verde salvo preexistentes | salidas en `evidencia/` | TODO |

## Reparto de archivos (regla 70.4.9)
| Carril | Puede escribir | No toca |
|---|---|---|
| S0 contrato | `servicios/{grupos,transparencia}/src/main/resources/openapi/*.yaml` | todo lo demás |
| A | `servicios/transparencia/**` (salvo el yaml), fuente del esquema de transparencia, `sql/**` del módulo 06 | grupos, apps, packages |
| B | `servicios/grupos/**` (salvo el yaml) | transparencia, sql, apps, packages |
| C | `apps/movil/lib/pantallas/pasanaku/**` salvo `tarjeta_de_integrante.dart`; tests nuevos `test/widget/*grupo*` | identidad, diseno_flutter |
| D | `packages/diseno_flutter/**`, `apps/movil/lib/pantallas/reputacion/**` (nuevo), `pantalla_mi_puntaje.dart`, `tarjeta_de_integrante.dart`; tests `test/widget/*arena*` | resto de pasanaku |
| Principal | generadores compartidos, `docs/`, commits | — |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Builds simultáneos de los carriles | Timeouts espurios | Candado por directorio para gradle/flutter (`candado.sh`) |
| `test` en rojo por #343 | CI no verde | Fuera de alcance; se reporta |
| Achicar un grupo con turnos ya asignados | Inconsistencia | Iniciar solo desde `ABIERTO_A_INSCRIPCION`, antes de cualquier sorteo |

## H2 — Gamificación ronda 2 (agregada 2026-10-30, pedido de Pablo con coreyhaines31/marketingskills)
Diseño y filtro ético en [GAMIFICACION.md](./GAMIFICACION.md). Se ejecuta **después** de que cierre H1, para no pisar los archivos de los carriles C y D.
**CA:** Dado alguien recién llegado, cuando abre su arena, entonces ve Semilla con el primer paso hecho, su próxima meta en puntos y una lista de primeros pasos; al subir de arena ve una celebración que respeta movimiento reducido; nada le promete subir por aportar más plata.
**DoD:** tests de widget por mecánica + `flutter analyze` + revisión independiente con el filtro ético de GAMIFICACION.md.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | G1+G2: arranque con progreso y próxima meta | Semilla con paso KYC marcado; «te faltan N puntos» exacto | `flutter test test/widget/arena_meta_test.dart` | TODO |
| H2.S1.M2 | G3: lista de primeros pasos en Inicio, descartable | 5 pasos, progreso, se puede cerrar | `flutter test test/widget/primeros_pasos_test.dart` | TODO |
| H2.S1.M3 | G4: celebración al subir de arena | Aparece al cambiar `orden`; sin animación con movimiento reducido | `flutter test test/widget/subida_de_arena_test.dart` | TODO |
| H2.S1.M4 | G6+G9: aviso preventivo y plan de recuperación | Textos sin culpa; plan en puntos | `flutter test test/widget/arena_recuperacion_test.dart` | TODO |
| H2.S1.M5 | G8+G10: sugerir grupo chico y sello de arena al invitar | Sugiere 4–6 cupos en arena inicial | `flutter test test/widget/empezar_chico_test.dart` | TODO |
| H2.S2.M1 | G5 y G7 (necesitan contrato nuevo) | — | — | TODO — DECISION_REQUIRED: Pablo decide si entra el endpoint agregado por arena y el resumen de cierre (contratos nuevos) |

## H3 — Primera cuota antes del sorteo (agregada 2026-10-30; decisión de Pablo por AskUserQuestion)
Regla de Pablo (registrada en docs por pasanaku-1f: R-GRP-20 y CU-60, error propuesto `AP-CU60-11 PRIMER_APORTE_PENDIENTE`): el sorteo solo se compromete si cada cupo ocupado tiene el primer aporte **acreditado**.
Decisión de Pablo: **la primera cuota nace al iniciar el grupo**. `iniciarGrupo` abre el cobro del periodo 1 para cada cupo del grupo achicado. El sorteo se habilita cuando todas están acreditadas por webhook verificado (regla 91.3.1). Dueño del código: esta sesión, después de cerrar H1. **Desde 2026-10-30 esta sesión también es dueña de `docs/CasosDeUso/CU-60*` (precondición 4, tabla de errores) y de la fila R-GRP-20 de `Restricciones.md`** (traspaso de pasanaku-1f). Si cambia el código `AP-CU60-11` o su nombre, esos dos .md se actualizan en el mismo commit.
Hecho a verificar antes de diseñar: hoy `obligacion_aporte` nace al abrir el periodo (CU-21) y el periodo nace `EN_CURSO`, lo que exige sorteo revelado (según pasanaku-1f; confirmar con archivo:línea).
**CA:** Dado un grupo iniciado con 8 cupos, cuando 7 acreditaron la cuota 1, entonces `comprometerSorteo` responde 422 `AP-CU60-11` con los cupos pendientes; cuando acredita el octavo, se puede sortear; la cuota 1 queda imputada al periodo 1 una vez generado el calendario, sin duplicarse.
**DoD:** saga grupos → aportes con outbox e idempotencia (prueba de duplicado), prueba con aportes apagado (fail-closed), asientos de la cuota 1 pegados y cuadrados (regla 91.6), tests de CU-60, prueba en la app.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Descubrimiento: cómo nacen periodo 1 y obligación hoy (CU-21) y qué contrato tiene aportes | Hechos con archivo:línea en este plan | lectura + `grep` pegado | TODO |
| H3.S1.M2 | Al iniciar se emite el evento (outbox) que abre la obligación de la cuota 1 por cupo | Una obligación por cupo; reenviar el evento no duplica | test de integración de duplicado | TODO |
| H3.S1.M3 | `comprometerSorteo` exige el primer aporte acreditado por cupo ocupado (`AP-CU60-11`) | 7/8 → 422; 8/8 → 2xx | `./gradlew :servicios:grupos:test --tests '*CU60*'` | TODO |
| H3.S1.M4 | Imputar la cuota 1 al periodo 1 cuando se genera el calendario | Sin doble cobro del periodo 1 | test + asientos pegados | TODO |
| H3.S2.M1 | App: estado «esperando la primera cuota de N» y botón de sorteo bloqueado con motivo | Se ve quién falta, sin montos de otros | `flutter test` dirigido | TODO |

## H1.S6 — Renombre «arena» → «etapa» (decisión de Pablo 2026-10-30: «que no suene demasiado fantasioso»)
Se aplica **después** de que cierre la ronda 1b (los carriles A y B escriben los mismos archivos). Como el contrato todavía no está publicado en ninguna rama, se renombra entero, sin alias: API, esquema, semillas, código y textos.
Catálogo nuevo (reemplaza al de arenas; mismos tramos sobre el puntaje v1):

| Orden | Código | Texto en la app | Desde | Hasta |
|---|---|---|---|---|
| 0 | `RECUPERACION` | En recuperación | 0 | 500 |
| 1 | `INICIAL` | Etapa 1 · Inicial | 500 | 550 |
| 2 | `EN_MARCHA` | Etapa 2 · En marcha | 550 | 600 |
| 3 | `CONSTANTE` | Etapa 3 · Constante | 600 | 650 |
| 4 | `PUNTUAL` | Etapa 4 · Puntual | 650 | 700 |
| 5 | `COMPROMETIDO` | Etapa 5 · Comprometido | 700 | 760 |
| 6 | `SOLIDO` | Etapa 6 · Sólido | 760 | 820 |
| 7 | `DESTACADO` | Etapa 7 · Destacado | 820 | 880 |
| 8 | `EXPERTO` | Etapa 8 · Experto | 880 | 940 |
| 9 | `EJEMPLAR` | Etapa 9 · Ejemplar | 940 | 980 |
| 10 | `PILAR` | Etapa 10 · Pilar | 980 | 1000 (incl.) |

Se evitan «Confiable» y «Referente» para no chocar con `nivel_confianza`.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S6.M1 | Renombre en contrato, esquema, semillas, Java y Dart (`arena`→`etapa`, `listarArenas`→`listarEtapas`, `/reputacion/arenas`→`/reputacion/etapas`, `arenaMinima`→`etapaMinima`) | `git grep -i arena` en lo tocado no devuelve nada | `git grep -niw 'arena\|arenas' -- servicios/grupos servicios/transparencia apps/movil/lib packages/diseno_flutter/lib sql seeders` vacío + tests de ambos servicios + `flutter test` dirigidos | TODO |
| H1.S6.M2 | Textos de la app: «Etapa N · Nombre», sin vocabulario de juego | Ningún texto visible dice «arena» | test de widget + captura | TODO |


## H1.S7 — Ronda 3: renombre a etapa + privacidad + re-firma (2026-10-30)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S7.M1 | Contrato: renombre arena→etapa en ambos YAML; `IntegranteDelGrupo.etapa` (código + nombre); `consultarPuntaje` solo propio; lectura de etapa para servicios; re-firma pendiente en el detalle | Compila y los clientes se regeneran | `./gradlew :servicios:grupos:compileJava :servicios:transparencia:compileJava` exit 0 | TODO |
| H1.S7.M2 | Transparencia: renombre + privacidad + permiso de backoffice en el catálogo | Otro participante → 403 sobre el puntaje ajeno; servicio → etapa; backoffice → catálogo | `./gradlew :servicios:transparencia:test` | TODO |
| H1.S7.M3 | Grupos: renombre + token de servicio + etapa por integrante + versión nueva del reglamento al iniciar | Al iniciar existe la versión N+1 con los números reales y nadie la tiene aceptada | `./gradlew :servicios:grupos:test` | TODO |
| H1.S7.M4 | App: renombre a «Etapa N · Nombre», insignia por integrante, re-firma después de iniciar | Ningún texto dice «arena»; se ve la insignia; aparece «Firmá la versión nueva» | `flutter test` dirigidos + `flutter analyze` | TODO |

## H1.S8 — Integración con `test` (descubierto 2026-10-30 con `git merge-tree HEAD origin/test`)
- `origin/test` va 65 commits adelante. Conflictos previstos: `scripts/metadata_enriquecida.json` y `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposDetalleYFirmaWebTest.java`.
- **Camino de ingreso nuevo en `test`: traspaso de cupo por subasta** (#346, `CU64TraspasoPorSubasta.java`, `RutasDeSubastaDelCupo.java`). No existía cuando se diseñó la etapa mínima. Por la decisión de Pablo («el traspaso se permite después de iniciar, respetando la etapa mínima»), quien gana una subasta tiene que cumplir la etapa mínima del grupo.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S8.M1 | Rebase sobre `origin/test` resolviendo los 2 conflictos | Compila y la suite de grupos queda en verde | `./gradlew :servicios:grupos:test` | TODO |
| H1.S8.M2 | Etapa mínima en la subasta del cupo: no se puede pujar ni adjudicar por debajo (`AP-CU64-04`, igual que el traspaso) | Puja de alguien bajo la mínima → 422 antes de retener plata; adjudicación revalida | test dirigido en `CU64SubastaTest` contra Postgres | TODO |

### H3 — contrato acordado con pasanaku-1f (2026-10-30, rama `claude/sorteo-primera-cuota-20261030`)
- **Evento que emite esta rama** por outbox, en la transacción de `CU69IniciarGrupo` y después de que exista el periodo 1: `EventoDominio(tipo="grupos.grupo_iniciado", agregado="grupo", agregadoId=grupoId)`, tema `aportaya.grupos.grupo_iniciado`, clave `grupoId`. Carga: `{grupoId, moneda, cuposTotales, iniciadoEn, periodo{periodoId, numero:1, fechaInicio, fechaLimitePago, fechaFinGracia, fechaEntregaPrevista}, cupos[{cupoId, numero 1..N, participanteId, montoAporte "500.00"}]}`, con solo los cupos OCUPADO renumerados. Si el inicio ya ocurrió, no se reemite: es idempotente por el renglón del historial.
- **Lo que consume `puedeSortear`**: `GET /aportes/grupos/{grupoId}/primera-cuota` → `{grupoId, periodoId|null, cuposPagados[], cuposPendientes[]}` con `@Permiso("BILLETERA_VER")`. Si no hay obligaciones, no se puede sortear.
- Códigos: `AP-CU60-11 PRIMER_APORTE_PENDIENTE` y `AP-CU60-12` (aportes no responde → se niega). El 13 en adelante es de esta rama.
- **Hechos que hay que verificar con archivo:línea antes de implementar:** (a) si `CU69IniciarGrupo` genera hoy el calendario y el periodo 1, porque pasanaku-1f asume que sí, después de `calendario.generar`; (b) que nadie pasa el periodo 1 de PROGRAMADO a ABIERTO, y que `CU21CobrarAporte` puede rechazar el cobro de un periodo PROGRAMADO (`AP-CU21-04`).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M5 | Verificar (a) y (b) | Hechos con archivo:línea en este plan | `git grep` pegado | TODO |
| H3.S1.M6 | Si el cobro choca con PROGRAMADO: abrir el periodo 1 al iniciar, sin pasar el grupo a EN_CURSO | Se cobra la cuota 1 de un grupo CONFORMADO | test de integración de cobro | TODO — si abrir el periodo antes del sorteo cambia una regla de negocio, DECISION_REQUIRED |
- Estado de pasanaku-1f (2026-10-30): grupos 513/513 en su rama. Aportes consume `grupos.grupo_iniciado` con `CU21AbrirPrimeraCuota`, que funciona aunque el periodo 1 esté PROGRAMADO pero **exige el calendario de feriados del año del vencimiento** (`AP-CU21-09`). `CU60Sortear` tiene un constructor nuevo (`PrimerAporteDelCupo`, `AceptacionVigenteRepositorio`), y `AP-CU60-05` se calcula con `reglamento_grupo WHERE vigente_hasta IS NULL`.
- **Para verificar en H1.S7.M3:** la versión N+1 del reglamento que se publica al iniciar tiene que **cerrar la anterior** (`vigente_hasta` con valor). Si no se cierra, la consulta de pasanaku-1f ve dos versiones vigentes y la re-firma no se exige. Hay que demostrarlo con un test.
- pasanaku-1f publicó `origin/claude/sorteo-primera-cuota-20261030 @ 20f874d0` (grupos 513/513, aportes 236/236). **Su rama sola deja todos los sorteos en `AP-CU60-11`** hasta que exista el evento `grupos.grupo_iniciado`, así que **no se mergea antes que la emisión de esta rama**. Los PRs de las dos ramas salen juntos. Al integrar: `CU60Sortear` tiene 7 parámetros y cualquier test que lo construya a mano necesita el doble de `PrimerAporteDelCupo` y `new AceptacionVigenteRepositorio()`.
