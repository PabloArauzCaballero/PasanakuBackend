# Auditoría grupos-organizador (lote L3) — solo lectura

> Auditoría de solo lectura sobre `origin/test`. Ningún archivo fuera de esta carpeta se modificó.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | grupos-organizador — `servicios/grupos`, `servicios/organizador`, alta, admisión, inicio, sorteo, postulaciones |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — `git diff --quiet` sin diferencias en `servicios/grupos`, `servicios/organizador`, `sql/10_tablas/02_grupos_turnos`, `sql/10_tablas/07_organizador_automatizacion`, `apps/backoffice/src/app/rutas`, `apps/movil/lib/pantallas/pasanaku`, `contratos`, `clientes/dart/grupos` |
| Historial | El clon era superficial; se aplicó `git fetch --unshallow` → `git rev-parse --is-shallow-repository` = `false` (1878 commits). 119 commits tocan `servicios/grupos` y 28 `servicios/organizador` |
| Archivos / líneas cubiertos | En alcance 472 archivos / 64 255 líneas: 318 / 32 413 leídos completos, 70 / 17 423 parciales, 84 / 14 419 (pruebas de `grupos`) no línea a línea. Detalle en sección 12 |

## 2. Resumen

El módulo grupos-organizador no está listo para operar con dinero de terceros bajo las reglas del 2026-11-20. El circuito del pasanaku depende de personas en cada paso: el organizador propone y backoffice resuelve cada admisión (interruptor encendido por omisión), y el administrador inicia el grupo y dispara las dos fases del sorteo a mano. No existe ningún trabajo programado en `grupos` ni en `organizador`. ADR-054, que ya decidió inicio y sorteo automáticos y admisión por enlace, está aceptado pero no implementado. Además, el alta exige un organizador habilitado, así que el grupo sin organizador de ADR-054 no se puede crear. El código de `grupos` **no** pone dinero de la empresa por nadie: R-NUEVA-2 se cumple en el código del módulo, y la contradicción está en los ADR, el catálogo de flujos y los datos de ejemplo. Los dos riesgos de dinero más serios son de conexión. El alias usado como destino de transferencias es un nombre no único que se resuelve con `LIMIT 1`. El retiro de un participante lo puede pedir el creador del grupo por otro. A esto se suma un ciclo de vida que se corta: el grupo nunca pasa a `EN_CURSO` ni el turno a `COBRADO`, y un grupo que se llena por firmas no publica `grupo_iniciado`, así que nunca abre su primera cuota. Del lado de `organizador`, las sanciones se ejecutan antes del descargo y la apelación no conserva el argumento del apelante.

### Hallazgos por clase y severidad

| Clase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| APROB | 0 | 2 | 0 | 0 | 2 |
| RESPALDO | 0 | 0 | 1 | 0 | 1 |
| FLUJO-QR | 0 | 1 | 0 | 0 | 1 |
| COHERENCIA | 0 | 1 | 1 | 0 | 2 |
| DUP-INT | 0 | 1 | 0 | 0 | 1 |
| DUP-X | 0 | 0 | 1 | 0 | 1 |
| REESCRITURA | 0 | 0 | 1 | 0 | 1 |
| FRAGIL | 0 | 2 | 2 | 0 | 4 |
| DEFECTO | 0 | 3 | 2 | 0 | 5 |
| HUECO | 0 | 5 | 3 | 0 | 8 |
| RESILIENCIA | 0 | 1 | 2 | 0 | 3 |
| DINERO | 1 | 1 | 1 | 0 | 3 |
| DEBIDO-PROCESO | 0 | 2 | 0 | 0 | 2 |
| ARQ | 0 | 0 | 1 | 0 | 1 |
| CLEAN | 0 | 0 | 0 | 1 | 1 |
| ACOPLE | 0 | 1 | 1 | 0 | 2 |
| CONTRATO | 0 | 1 | 1 | 0 | 2 |
| DATOS | 0 | 1 | 1 | 0 | 2 |
| PRUEBAS | 0 | 0 | 2 | 1 | 3 |
| UX-A11Y | 0 | 0 | 0 | 1 | 1 |
| OBSERV | 0 | 1 | 0 | 0 | 1 |
| DOC | 0 | 0 | 2 | 0 | 2 |
| SEG | 1 | 2 | 2 | 0 | 5 |
| TRAZA | 0 | 0 | 1 | 0 | 1 |
| **Total** | **2** | **25** | **25** | **3** | **55** |

Clases sin hallazgo propio (ver sección 8): FLUJO-FALTANTE (cubierta por HUECO-006 y HUECO-007), PISADA (manifestada en DUP-INT-001, REESCRITURA-001, COHERENCIA-001), PARCHE, DDD (incluida en ARQ-001 y FRAGIL-004), RENDIMIENTO, CONFIG (incluida en CLEAN-001 y DINERO-001), REG, OTRO.

### Los diez más graves

1. `grupos-organizador-DINERO-002` (S1) — destino de transferencia por alias = nombre no único con `LIMIT 1`.
2. `grupos-organizador-SEG-001` (S1) — el creador del grupo puede pedir el retiro de otro integrante.
3. `grupos-organizador-APROB-001` (S2) — admisión con aprobación de backoffice encendida por omisión.
4. `grupos-organizador-APROB-002` (S2) — inicio y sorteo solo a mano del administrador; 0 trabajos programados.
5. `grupos-organizador-FLUJO-QR-001` (S2) — grupo lleno por firmas sin `grupo_iniciado`: nunca se abre la primera cuota.
6. `grupos-organizador-HUECO-006` (S2) — el grupo nunca pasa a `EN_CURSO` ni el turno a `COBRADO`; guardas de dinero muertas.
7. `grupos-organizador-COHERENCIA-002` (S2) — el alta exige organizador habilitado; ADR-054 D1 sin implementar.
8. `grupos-organizador-DEBIDO-PROCESO-001` (S2) — sanción ejecutada antes del descargo y de la firmeza.
9. `grupos-organizador-DATOS-001` (S2) — cuatro restricciones de la base que nunca se disparan.
10. `grupos-organizador-DUP-INT-001` (S2) — cuatro implementaciones de «admitir» con efectos distintos.

### Peldaño de evidencia alcanzado (regla 30)

| Área | Peldaño | Por qué no más arriba |
|---|---|---|
| `servicios/grupos` (main) | DISCOVERED | Gradle no corrió (429 de Maven Central); nada se ejecutó |
| `servicios/organizador` | DISCOVERED | ídem |
| Pruebas de ambos servicios | DISCOVERED | no se pudieron correr; se leyeron nombres y una parte del código |
| SQL del módulo | DISCOVERED | sin base de datos; las restricciones se compararon leyendo |
| Frontends (backoffice, app, web) | DISCOVERED | sin `node_modules`, yarn 1 vs 4, sin Flutter |
| Verificadores Python del repo | RUNS | corrieron con salida pegada en `evidencia/` |


## 3. Huella y mapa de conexiones

### Huella

| Capa | Ubicación | Archivos | Líneas |
|---|---|---:|---:|
| Servicio grupos (main) | `servicios/grupos/src/main/**` + README, descriptor, build | 121 | ≈15 600 |
| Servicio grupos (pruebas) | `servicios/grupos/src/test/**` | 110 | 18 119 |
| Servicio organizador (main) | `servicios/organizador/src/main/**` + README, descriptor, build | 43 | ≈5 750 |
| Servicio organizador (pruebas) | `servicios/organizador/src/test/**` | 28 | 5 108 |
| Esquema | `sql/10_tablas/02_grupos_turnos` (27), `sql/10_tablas/07_organizador_automatizacion` (13), claves, índices, rangos de `40_reglas`, `35_append_only`, semillas `17`, dev `03`, volumen `03`, `prueba_humo` | 50 | 7 870 |
| Pactos | `contratos/pactos/{grupos-aportes-grupo-iniciado,identidad-grupos-pertenencia,transparencia-grupos-pertenencia}.json` | 3 | 143 |
| Backoffice | `apps/backoffice/src/app/rutas/{admisiones,grupos,postulaciones,operacion/solicitudes-escaladas,cumplimiento/organizadores}` | 36 | 3 688 |
| App móvil | `apps/movil/lib/pantallas/pasanaku/` (archivos de grupos, admisión, inicio, sorteo, invitación, organizador) | 75 en alcance | 7 698 |
| Web | `apps/web/src/app/paginas/{publico-grupos,publico-sorteos,invitacion}` | 6 | 366 |
| Clientes generados | `clientes/dart/grupos`, `clientes/dart/organizador` (el código bajo `lib/src` se genera en build y no está versionado) | inventariados | — |

Total del anexo: 521 archivos, 68 894 líneas, de los cuales 49 archivos (4 639 líneas) de `apps/movil/lib/pantallas/pasanaku/` se clasificaron fuera de alcance L3 (aportes, subasta, traspaso, permuta, retiro, mora, perfil). En alcance: 472 archivos, 64 255 líneas. Leídos completos: 318 archivos, 32 413 líneas. Parciales: 70 archivos, 17 423 líneas. No leídos línea a línea: 84 clases de prueba de `grupos`, 14 419 líneas.

### Mapa de conexiones

| Dirección | Contraparte | Mecanismo | Punto exacto | Notas |
|---|---|---|---|---|
| grupos → organizador | `GET /organizadores/{id}/habilitacion`, `GET /organizadores/usuarios/{u}/habilitacion` | HTTP, circuito, timeout global 1 s/3 s | `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java:123-146` | alta de grupo (FRAGIL-003) |
| grupos → tarifas | `GET /tarifas/vigentes/{codigo}` | HTTP | `HechosPorHttp.java` | precio congelado |
| grupos → cumplimiento | `GET /licencia/alcance?servicio=` | HTTP | `HechosPorHttp.java` | licencia |
| grupos → aportes | `GET /aportes/participantes/{p}/estado`, `/aportes/grupos/{g}/morosos`, `/aportes/grupos/{g}/primera-cuota` | HTTP (dos circuitos) | `HechosPorHttp.java`, `PrimerAportePorHttp.java` | el sorteo exige cuota 1 acreditada |
| grupos → transparencia | `/reputacion/{u}/puntaje`, `/reputacion/etapas`, `/reputacion/etapas/de-personas` | HTTP, token de servicio | `EtapasPorHttp.java` | SEG-002 |
| grupos → garantia | `GET /cobranza/restricciones/vigentes/{u}` | HTTP | `HechosPorHttp.java:217` | restricción del participante |
| grupos → notificaciones | `POST /notificaciones/supresion/consultas` | HTTP | `HechosPorHttp.java` | |
| grupos → identidad | tokens de invitación, KYC, perfil, búsqueda por teléfono | HTTP | `TokensDeInvitacionPorHttp.java`, `PersonasPorHttp.java` | teléfono en el cuerpo (bien) |
| nucleo-financiero → grupos | `GET /grupos/participantes/alias/{alias}` | HTTP | `servicios/nucleo-financiero/.../ResolverDestino.java:41` | DINERO-002, OBSERV-001 |
| aportes → grupos | `GET /grupos/periodos/{p}/calendario-de-aportes`, `GET /grupos/obligaciones/admisibilidad` | HTTP | `RutasDeSolicitudes.java:52`, `GruposController.java:254` | |
| entregas → grupos | `POST /grupos/{g}/cupos/{c}/traspasos-por-subasta`, `/participaciones` | HTTP | `RutasDeSubastaDelCupo.java:123` | clave de idempotencia ignorada (RESILIENCIA-003) |
| identidad, transparencia → grupos | `GET /grupos/{g}/integrantes/{u}/comparte` | HTTP + pacto | pactos `*-grupos-pertenencia.json` | PRUEBAS-003 |
| identidad → organizador | `habilitacion-basica`, `firma` | HTTP | `OrganizadorPorHttp` (identidad) | |
| organizador → cumplimiento | `GET /reclamos/usuarios/{u}/efecto` | HTTP, circuito | `EfectoDeReclamosPorHttp.java:51` | falla cerrado (bien) |
| grupos → aportes | evento `aportaya.grupos.grupo_iniciado` | outbox → Kafka | `CU69IniciarGrupo.java:200-207` → `ConsumidorDeGrupoIniciadoKafka.java` | FLUJO-QR-001 |
| grupos → nucleo-financiero, tarifas | evento `aportaya.grupos.grupo_creado` | outbox → Kafka | `CU20CrearGrupo` | |
| tarifas → grupos | `aportaya.tarifas.tarifa_congelada` | Kafka | `ConsumidorDePreciosCongeladosKafka.java` | idempotente, sin DLQ |
| entregas → grupos | `aportaya.entregas.derecho_cedido` | Kafka | `ConsumidorDeCesionesKafka.java` | RESILIENCIA-002 |
| organizador → grupos | `aportaya.organizador.habilitacion_revocada` | Kafka, apagado por omisión | `ConsumidorDeRevocacionesKafka.java` | |
| grupos → nadie | 20+ eventos (`sorteo_revelado`, `acuerdo_resuelto`, `admision_resuelta`, …) | outbox | `listar_topicos.py` | sin consumidor |
| organizador → nadie | ≈19 eventos | outbox | `listar_topicos.py` | sin consumidor salvo `habilitacion_revocada` |
| grupos ↔ esquemas ajenos | FK a `identidad`, `aportes`, `garantia`, `organizador`; disparador R-ORG-02 | base compartida | `sql/20_claves/02_grupos_turnos.sql`; `restricciones.sql:2686-2701` | ACOPLE-001 |
| publicidad → organizador | SQL contra esquema `organizador` | base | `servicios/publicidad/.../CreativaRepositorio.java:89` | fuera de alcance (L5) |

**Saltos de una operación de dinero en este módulo (98.8.1).** El módulo no mueve dinero por sí mismo; dispara el cobro. Los saltos son: (1) `grupos` conforma el grupo y publica `grupo_iniciado` por outbox; (2) `aportes` abre la cuota 1 y emite la orden de cobro QR (L1); (3) el webhook de la pasarela confirma el pago en `aportes` (L1); (4) `grupos` consulta `aportes` (`/primera-cuota`) antes de comprometer el sorteo; (5) el sorteo crea los turnos; (6) `entregas` paga el turno (L2) y `grupos` no se entera (HUECO-006). La transferencia entre billeteras por alias (CU-12) pasa por `grupos` solo para resolver el destino (DINERO-002).


## 4. Distancia al flujo nuevo

### 4a. Inventario de aprobaciones

| ID | Dónde | Qué aprueba y quién | Estado · endpoint · permiso · pantalla · evento | Clasificación | Qué se rompe aguas abajo si se quita |
|---|---|---|---|---|---|
| APR-01 | `servicios/grupos/src/main/resources/application.yml:73-78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:141-150`; `.../web/InvitacionesWeb.java:127-133`; `.../web/RutasDeAdmision.java:25-37` | Ingreso de un integrante: el organizador **propone** y backoffice (`ADMIN_PLATAFORMA`) **resuelve** | Solicitud `PENDIENTE` → decisión `PROPUESTA` → `RESOLUCION`; `POST /grupos/solicitudes/{id}/propuestas` (`GRUPO_ADMINISTRAR`), `POST /grupos/solicitudes/{id}/resoluciones` (`ADMIN_PLATAFORMA`); pantallas backoffice `rutas/admisiones`, `rutas/operacion/solicitudes-escaladas`; eventos `aportaya.grupos.admision_propuesta`, `aportaya.grupos.admision_resuelta` | ELIMINABLE desde producto (el propio `application.yml:74` la declara «DECISION PENDIENTE (negocio y cumplimiento)»); queda DECISIÓN REQUERIDA solo si Cumplimiento la exige (A-1) | Las puertas «aceptar por enlace» y «decisión directa del organizador» hoy devuelven 422 (`CodigoError 69-10` y `68-15`) con el interruptor encendido; apagarlo reabre esos caminos, que crean participante sin expediente; la bandeja de backoffice y sus pruebas quedan sin uso |
| APR-02 | `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDelGrupo.java:94-95`; `.../aplicacion/CU69IniciarGrupo.java:89-91`; `.../web/GruposController.java:164-173` | Inicio del grupo y compromiso/revelación del sorteo: los dispara el administrador del grupo | `ABIERTO_A_INSCRIPCION → CONFORMADO`; `POST /grupos/{id}/inicio`, `POST /grupos/{id}/sorteo`, `POST /grupos/{id}/sorteo/revelacion`, todos `GRUPO_ADMINISTRAR`; app `inicio_del_grupo.dart`, `pantalla_sorteo_*`; eventos `grupo_iniciado`, `sorteo_comprometido`, `sorteo_revelado` | ELIMINABLE (ADR-054 D3 ya decidió que sean automáticos) | Sin el disparo manual hoy no hay ningún otro camino (0 `@Scheduled`): quitarlo sin el trabajo programado deja todo grupo detenido |
| APR-03 | `servicios/organizador/src/main/java/bo/aportaya/organizador/web/OrganizadoresController.java:116-129`; `.../web/RutasDeHabilitacion.java:106-129`; backoffice `apps/backoffice/src/app/rutas/postulaciones/`, `apps/backoffice/src/app/rutas/cumplimiento/organizadores/pantalla-de-habilitacion.ts` | Habilitación del organizador «profesional»: backoffice aprueba o rechaza la postulación, la confirma o revoca y habilita tras capacitación | `POST /organizadores/postulaciones/{id}/aprobacion` (vía heredada), `.../resolucion`, `.../revision`, `POST /organizadores/{id}/habilitacion`, todos `ADMIN_PLATAFORMA`; eventos `organizador.postulacion_aprobada`, `habilitacion_resuelta`, `habilitado` | DECISIÓN REQUERIDA (A-1): ADR-054 D6 la deja fuera del camino principal, pero sigue siendo puerta para organizar si no existe el grupo autogestionado (COHERENCIA-002) | Nadie llega a organizador profesional; la vía básica (APR-05) sigue |
| APR-04 | `servicios/organizador/src/main/java/bo/aportaya/organizador/web/AutomatizacionController.java:89-93`; `.../aplicacion/CU96EjecutarTarea.java:30-31,79-80,108-124` | Tarea automatizada «sensible»: nace en `REQUIERE_APROBACION` y la confirma una persona con rol `ORGANIZADOR` | `REQUIERE_APROBACION → PROGRAMADA`; `POST /automatizacion/tareas/{id}/aprobacion`; evento `organizador.tarea_aprobada` | CONSERVAR POR NORMA interna: R-ORG-06 «Las acciones sensibles exigen confirmación humana» (`docs/Restricciones.md:2667`) y regla 91.4 (sanciones). Confirmar con Pablo que R-NUEVA-1 no la alcanza | Una regla configurada que sanciona o mueve dinero se ejecutaría sin confirmación humana |
| APR-05 | `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90HabilitacionBasica.java:28-43`; app `contrato_de_organizador.dart` | Habilitación «Aprendiz»: el propio participante acepta y firma el contrato; **no** aprueba nadie | `POST /organizadores/yo/habilitacion-basica` (`PARTICIPANTE`) | No es aprobación de un tercero; queda como puerta previa al alta mientras exista COHERENCIA-002 | Sin ella, hoy nadie puede crear un grupo (`AP-CU20-02`) |
| APR-06 | `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU65Retirarse.java:80-99` | Retiro de un participante del grupo: la solicitud nace `PENDIENTE` y espera una aprobación | `PENDIENTE → APROBADA`; ningún endpoint, consumidor ni trabajo llama a `aprobar` (HUECO-004) | ELIMINABLE o DECISIÓN REQUERIDA (A-4: qué pasa con lo aportado y la deuda al retirarse) | Hoy nada: la aprobación nunca ocurre |

### 4b. Inventario de respaldo y cobertura

Búsqueda que lo sustenta: `grep -rniE 'respaldo|cobertura|garantia|faltante|pozo completo' servicios/grupos/src/main servicios/organizador/src/main`, más la lectura de las tablas y documentos del módulo. **En el código vivo de `grupos` y `organizador` no hay ningún camino por el que la empresa ponga dinero por quien no pagó.**

| ID | Dónde | Qué pone la empresa y cuándo | Asiento en el mayor | Clasificación | Qué depende de ello |
|---|---|---|---|---|---|
| RSP-01 | `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md:34,53-54`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:95,110,158,178` | Documento: la empresa aporta el faltante con reserva por ciclo; «pozo completo» | ninguno en este módulo (lo tendría `garantia`) | CONTRADICE R-NUEVA-2 | Pantallas y textos que se escriban desde el ADR; RESPALDO-001 |
| RSP-02 | `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:108-127` | Paso `cubrir-faltante` (`cubrirConRespaldo`) y `recuperar-respaldo` | lo define L2 | CONTRADICE R-NUEVA-2 | `scripts/validar_flujos.py` lo valida como flujo vigente (salida en `evidencia/validar_flujos.txt`) |
| RSP-03 | `sql/10_tablas/02_grupos_turnos/grupo.sql` (`usa_fondo_garantia`, `porcentaje_fondo_garantia`); `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:121-122` | Columnas de fondo de garantía por grupo; el alta las pone en `false`/0 | ninguno | NO APLICA hoy (desactivado); DECISIÓN REQUERIDA (A-3) si se retiran | semilla dev `sql/61_dev/03-grupo-demo.sql:53,82` usa el fondo y la «cobertura del fondo» |
| RSP-04 | `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU92EvaluarDesempeno.java:97`; columna `evaluacion_desempeno.coberturas_consumidas` | Métrica: cuántas coberturas consumió el grupo del organizador | ninguno | DECISIÓN REQUERIDA (A-3): sin cobertura, la métrica no tiene sentido | evaluación de desempeño, contrato `EntradaEvaluacion.coberturasConsumidas` |
| RSP-05 | `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/TipoDeAcuerdo.java:18` (`CONDONACION_MORA`) | El grupo (no la empresa) condona una deuda por votación; sin efecto implementado (HUECO-007) | ninguno | NO APLICA a R-NUEVA-2 (no es la empresa); DECISIÓN REQUERIDA (A-4) | CU-63 |
| RSP-06 | `servicios/grupos/src/test/java/bo/aportaya/grupos/**/CU69Test.java:120-136` (según subagente) | Aval entre participantes (`garantia.aval_participante`) | — | NO APLICA (aval de participantes, A-3) | prueba de grupos |

### 4c. Ambigüedades A-1 a A-4

| ID | Pregunta | Lo que el código muestra hoy | Supuesto de esta auditoría | A quién confirmar |
|---|---|---|---|---|
| A-1 | ¿«Nada de aprobación» incluye KYC, UIF/ASFI, la doble aprobación de retiros y, en este módulo, la habilitación de organizador, R-ORG-06 y las votaciones del grupo? | KYC `BASICO` exigido al entrar (`application.yml:63`, `PersonasPorHttp`); admisión humana (APR-01); habilitación profesional por backoffice (APR-03); R-ORG-06 en tareas (APR-04, restricción muerta en DATOS-001); votaciones para traspasos y acuerdos (CU-63/64) | No se asume que se eliminen KYC ni la habilitación profesional; se clasifican DECISIÓN REQUERIDA. La admisión humana y el inicio/sorteo manual se clasifican ELIMINABLE porque el propio repo los marca como decisión pendiente (APR-01) o ya decidió automatizarlos (ADR-054 D3, APR-02) | Pablo; Cumplimiento para KYC/UIF |
| A-2 | ¿Qué pasa con la entrega del turno cuando falta plata? | `grupos` no tiene rama: no sigue el estado del turno ni del periodo (HUECO-006); la disolución por acuerdo no tiene efecto (HUECO-007); la rama de cobertura vive en `garantia`/`entregas` (L2) | Ninguna rama se presenta como decidida | Pablo |
| A-3 | ¿Qué pasa con lo construido sobre respaldo y con el aval entre participantes? | Columnas de fondo en `grupo` en `false`; métrica `coberturas_consumidas`; aval solo en pruebas | Se inventaría (4b); no se propone borrar | Pablo |
| A-4 | ¿Cómo se trata la mora y el recargo cuando nadie cubre? | `grupos` lee morosos de `aportes`; el retiro calcula siempre «acreedor» porque el grupo nunca arranca (HUECO-006); retiro deudor sin plan obligatorio (DATOS-001); condonación por voto sin efecto | Sin supuesto: DECISIÓN REQUERIDA | Pablo |


## 5. Hallazgos


#### Clase APROB

### grupos-organizador-APROB-001 — La admisión humana de backoffice está encendida por omisión y bloquea todo ingreso sin aprobación
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:73-78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:30-31,141-150`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/InvitacionesWeb.java:55-56,127-133`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeAdmision.java:25-37`
- Evidencia:
  ```yaml
  # DECISION PENDIENTE (negocio y cumplimiento): toda entrada al grupo pasa por admision humana de backoffice?
  # true (produccion): las dos puertas heredadas de TEST ... se cierran con 422 ...
  humana-obligatoria: ${ADMISION_HUMANA_OBLIGATORIA:true}
  ```
  ```java
  if (admisionHumanaObligatoria) {
      throw new ErrorDeNegocio(CodigoError.de(68, 15),
          "Este grupo exige admision humana: tu decision es una propuesta (proponerAdmision) y la resuelve backoffice.");
  ```
  ```java
  @Permiso("ADMIN_PLATAFORMA")
  public ResponseEntity<DecisionIngreso> resolverAdmision(...) { ... admision.decidir(..., true)); }
  ```
- Cómo se comprobó: lectura de las cuatro ubicaciones; `grep -rn 'admisionHumanaObligatoria\|humana-obligatoria' servicios/grupos/src/main` → solo esas dos clases leen el interruptor.
- Impacto: con el valor por omisión, ningún participante entra a un grupo sin que una persona de backoffice resuelva su solicitud. Contradice R-NUEVA-1 de frente: el flujo no avanza solo por pago QR. Para el grupo: inscripción detenida hasta que operación actúe.
- Causa de fondo: la decisión de negocio quedó modelada como interruptor de despliegue en vez de política explícita del producto, y su valor por omisión es el más restrictivo.
- Solución de fondo: que Pablo decida (A-1) si la admisión necesita intervención humana. Si no la necesita, retirar el camino propuesta/resolución y dejar un único camino de admisión automática por reglas (KYC, restricción, reputación, cupo). Si Cumplimiento la exige, documentar la norma y sacarla de un `@Value` de despliegue.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: backoffice (`rutas/admisiones`, `rutas/operacion/solicitudes-escaladas`), app móvil (unirse/pedir cupo), eventos `admision_propuesta`/`admision_resuelta`.
- Estándares y normas: ISO/IEC 25010 adecuación funcional (corrección funcional); reglas 00 §1.7 (ambigüedad resuelta por configuración), 96.4.
- Esfuerzo estimado: medio

### grupos-organizador-APROB-002 — El inicio del grupo y las dos fases del sorteo solo ocurren si el administrador las dispara a mano
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDelGrupo.java:94-95`; `.../web/GruposController.java:164-173`; `.../aplicacion/CU69IniciarGrupo.java:89-91`; `.../web/SorteoDelGrupo.java:88-92`; `.../infraestructura/AdmisionRepositorio.java:40-46`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:77`
- Evidencia:
  ```java
  if (g == null || !admision.esAdministrador(dsl, grupoId, ctx.usuarioId())) {
      throw new ErrorDeNegocio(CodigoError.de(69, 7), "Solo quien organiza el grupo puede iniciarlo.");
  ```
  ```java
  @Permiso("GRUPO_ADMINISTRAR")
  public ResponseEntity<CompromisoDeSorteo> comprometerSorteo(
  ```
  ADR-054 D3 (aceptada 2026-10-31): «**Inicio y sorteo automáticos.** El grupo se conforma al firmarse el último cupo, o en la **fecha límite** …».
- Cómo se comprobó: lectura de las ubicaciones; `grep -rn '@Scheduled' servicios/grupos servicios/organizador` → **0 resultados**. No hay trabajo programado que conforme el grupo ni que comprometa o revele el sorteo. `esAdministrador` exige un participante con `es_organizador`.
- Impacto: un grupo lleno con las primeras cuotas cobradas por QR no avanza si quien lo administra no aprieta «iniciar», «sortear» y «revelar». Es un paso humano en el circuito, contra R-NUEVA-1, y una decisión ya aceptada (ADR-054 D3) sin implementar. Los participantes ven su dinero cobrado sin turno asignado.
- Causa de fondo: ADR-054 se aceptó sin que su carril «`grupos` — inicio y sorteo» (ADR-054 §«Lo que falta en el código», fila 2-3) llegara al código.
- Solución de fondo: trabajo programado en `grupos`, con lock distribuido, que conforme el grupo al completarse o en `fecha_inicio`, y comprometa o revele el sorteo cuando todas las primeras cuotas estén acreditadas. El endpoint manual queda, como mucho, de respaldo operativo.
- Relación con el flujo nuevo: APROB / FLUJO-QR (el pago confirmado no mueve el grupo por sí solo)
- Conexiones y módulos afectados: `aportes` (primera cuota, `PrimerAportePorHttp`), app móvil (`inicio_del_grupo.dart`, `pantalla_sorteo_*`), evento `aportaya.grupos.grupo_iniciado` → `aportes` (pacto `grupos-aportes-grupo-iniciado.json`).
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 00 §6.2; ADR-054 D3.
- Esfuerzo estimado: grande


#### Clase RESPALDO

### grupos-organizador-RESPALDO-001 — Los documentos de decisión y los datos de ejemplo siguen prometiendo que la empresa cubre el faltante; el código de `grupos` no lo hace
- Clase / Severidad / Confianza: RESPALDO / S3 / confirmado
- Ubicaciones: `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md:34,53-54`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:95,110,158,178`; `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:108-127`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:121-122`
- Evidencia: ADR-051 «Recibir mesa es recibir el pozo completo del turno; la empresa aporta el faltante»; ADR-054:178 «El riesgo de mora no cambia de dueño por este ADR: ya era de la reserva empresarial»; catálogo `cubrir-faltante … operacion: cubrirConRespaldo`; en código `.set(GRUPO.USA_FONDO_GARANTIA, false)`.
- Cómo se comprobó: `grep -rniE 'respaldo|cobertura|garantia|faltante|pozo completo' servicios/grupos/src/main servicios/organizador/src/main` → ninguna llamada a respaldo en el módulo; las únicas trazas son las columnas de `grupo` puestas en `false`/0 y la métrica `coberturas_consumidas` de la evaluación de organizador (`CU92EvaluarDesempeno.java:97`).
- Impacto: quien lea los ADR vigentes diseñará (o explicará a un participante) un pozo garantizado por la empresa que R-NUEVA-2 prohíbe. ADR-051 D-03 dice que «sin capacidad se deniega la activación»: `grupos` no lo consulta, lo que coincide con R-NUEVA-2 pero contradice al ADR.
- Causa de fondo: R-NUEVA-2 no está escrita todavía en un ADR.
- Solución de fondo: ADR nuevo que registre R-NUEVA-2 y marque como superadas ADR-051 D-02/D-03/D-15 y las frases de ADR-054; decisión A-2/A-3.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: `garantia`, `entregas` (L2), catálogo de flujos.
- Estándares y normas: ISO/IEC 25010 (adecuación de la documentación); regla 00 §8.
- Esfuerzo estimado: chico


#### Clase FLUJO-QR

### grupos-organizador-FLUJO-QR-001 — Un grupo que se llena por firmas queda `CONFORMADO` sin publicar `grupo_iniciado`, así que nunca se abre la primera cuota
- Clase / Severidad / Confianza: FLUJO-QR / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69FirmarReglamento.java:193-216`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69IniciarGrupo.java:101,140,200-207`; `servicios/aportes/src/main/java/bo/aportaya/aportes/consumo/ConsumidorDeGrupoIniciadoKafka.java:25,44-50`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/RequisitosDelSorteo.java:41-53`
- Evidencia:
  ```java
  "UPDATE grupos.grupo SET estado = 'CONFORMADO' WHERE id = ? AND estado = 'ABIERTO_A_INSCRIPCION'"
  ... calendario.generar(dsl, grupoId, ahora.toLocalDate());
  return true;            // sin outbox: no hay grupo_iniciado en este camino
  ```
- Cómo se comprobó: `grep -n 'grupo_iniciado\|outbox' CU69FirmarReglamento.java CU69IniciarGrupo.java` → solo `CU69IniciarGrupo` emite. En `aportes`, `CU21AbrirPrimeraCuota` solo se invoca desde `ConsumidorDeGrupoIniciadoKafka`. `CU69IniciarGrupo` exige estado `ABIERTO` (`:101`), así que un grupo ya `CONFORMADO` no se puede «iniciar» para recuperar el evento.
- Impacto: el camino más natural (todos firman) deja el grupo sin primera cuota, sin QR que pagar y sin sorteo, porque el sorteo exige la cuota 1 acreditada. El grupo queda detenido para siempre sin error visible.
- Causa de fondo: dos caminos de conformación (DUP-INT divergente) y solo uno publica el hecho que el resto del flujo necesita.
- Solución de fondo: un único método de conformación en el dominio del grupo que siempre publique `grupo_iniciado` por outbox, y un trabajo de reparación para los grupos ya conformados sin evento.
- Relación con el flujo nuevo: FLUJO-QR (el cobro por QR no puede empezar)
- Conexiones y módulos afectados: `aportes` (primera cuota), app (pantalla de aportar).
- Estándares y normas: ISO/IEC 25010 corrección funcional; reglas 98.3.3, 96.7.2.
- Esfuerzo estimado: chico


#### Clase COHERENCIA

### grupos-organizador-COHERENCIA-002 — El alta de un grupo exige un organizador habilitado; ADR-054 D1 admite grupos sin organizador
- Clase / Severidad / Confianza: COHERENCIA / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU20CrearGrupo.java:86-88`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java:115`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:116`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:75`; `apps/movil/lib/pantallas/pasanaku/pantalla_crear_grupo.dart:42-44`
- Evidencia:
  ```java
  if (entrada.organizador().isEmpty() || !entrada.organizadorHabilitado()) {
      throw new ErrorDeNegocio(CodigoError.de(20, 2), "Ese organizador no esta habilitado.");
  ```
  ```java
  .set(GRUPO.ES_AUTOGESTIONADO, organizador.isEmpty())   // nunca true: la línea 86 ya rechazó el vacío
  ```
  ADR-054 D1: «Existen grupos sin organizador humano … el organizador humano habilitado es una opción, no un requisito».
- Cómo se comprobó: lectura de las ubicaciones. `CreacionRepositorio.java:116` solo se alcanza después de `CU20CrearGrupo.java:86`, así que `es_autogestionado` siempre queda `false`. La app comenta lo contrario («Nulo … lo administra la plataforma (RN-18)», informado por el subagente de frontend y abierto por mí).
- Impacto: toda persona que quiere crear un pasanaku tiene que hacerse organizador primero. Hoy pasa por un autoservicio (`CU90HabilitacionBasica`: KYC + contrato firmado, sin aprobación humana; ver APR-05). El grupo autogestionado de ADR-054 no se puede crear. Además, la creación queda acoplada a `organizador` por HTTP en el camino crítico.
- Causa de fondo: ADR-054 aceptado sin su carril de código; quedaron rama muerta (`ES_AUTOGESTIONADO`) y comentario que miente.
- Solución de fondo: decidir si R-NUEVA-1 alcanza a la habilitación del creador (A-1). Si no la alcanza, implementar ADR-054 D1: sin organizador, `es_autogestionado = true`, creador como anfitrión.
- Relación con el flujo nuevo: APROB (puerta previa al circuito)
- Conexiones y módulos afectados: `organizador` (`GET /organizadores/usuarios/{id}/habilitacion`), app móvil (`contrato_de_organizador.dart`, `pantalla_crear_grupo.dart`).
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 00 §8; ADR-054 D1.
- Esfuerzo estimado: medio

### grupos-organizador-COHERENCIA-001 — El interruptor de admisión humana es global; ADR-054 lo quiere por tipo de conformación y sin admisión en el grupo de confianza
- Clase / Severidad / Confianza: COHERENCIA / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:30-31`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:58-59,76,79,93`
- Evidencia: ADR-054 D2 «El enlace es la admisión … Nadie aprueba a nadie»; D5 «El interruptor pasa de global a **por `tipo_conformacion`**». En el código, `@Value("${aportaya.grupos.admision.humana-obligatoria}") private boolean admisionHumanaObligatoria;` es un único booleano.
- Cómo se comprobó: `grep -rn 'tipo_conformacion' servicios/grupos/src/main` → 0 usos en lógica de admisión.
- Impacto: tres fuentes dicen tres cosas sobre la admisión: ADR-054 (sin aprobación en el grupo de confianza), `application.yml` (aprobación humana en todos) y R-NUEVA-1 (sin aprobación en ninguno). Manda el código, que es la más restrictiva.
- Causa de fondo: decisiones de producto aceptadas sin el cambio de código que las materializa.
- Solución de fondo: Pablo confirma R-NUEVA-1 sobre ADR-054 D5 (A-1). Después se actualiza el ADR y el interruptor desaparece.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: ninguno adicional a APROB-001.
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 00 §8 (prioridad de evidencia).
- Esfuerzo estimado: chico (si se decide quitar)


#### Clase DUP-INT

### grupos-organizador-DUP-INT-001 — Admitir a una persona tiene cuatro implementaciones con efectos distintos
- Clase / Severidad / Confianza: DUP-INT / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/OrganizadorDecideIngreso.java:105-149`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AdmisionRepositorio.java:97-143`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69Enlace.java:126-149`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/SubastaDeCupoRepositorio.java:49-60`
- Evidencia: el camino del organizador reserva 1 cupo e ignora `cupos_solicitados`; el de backoffice reserva N y mira `max_cupos_por_persona`; aceptar por enlace crea al participante `ACTIVO` con cupo `OCUPADO` sin pasar por `RESERVADO`; la subasta crea `ACTIVO` con reputación 0. Cada uno emite un evento distinto (`ingreso_aceptado`, `admision_resuelta`, `invitacion_aceptada`).
- Cómo se comprobó: lectura de los cuatro bloques (los inserts de participante y cupo); el subagente de `grupos` los listó y se abrieron.
- Impacto: la misma regla de negocio (quién entra y con cuántos cupos) responde distinto según la puerta; un consumidor que escuche un solo evento pierde ingresos. Con R-NUEVA-1 sobrevive una sola puerta y no está claro cuál.
- Causa de fondo: reescrituras sucesivas sin retirar las anteriores (agentes en paralelo; ver REESCRITURA-001).
- Solución de fondo: una sola operación de dominio «admitir» con un solo evento; las puertas solo cambian el disparador.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `aportes` (cupos que cobran), app, backoffice.
- Estándares y normas: ISO/IEC 25010 mantenibilidad, corrección funcional; regla 96.1.
- Esfuerzo estimado: grande


#### Clase DUP-X

### grupos-organizador-DUP-X-001 — El cliente HTTP con circuito está copiado en cinco servicios con contenidos distintos
- Clase / Severidad / Confianza: DUP-X / S3 / confirmado
- Ubicaciones: `servicios/{grupos,organizador,aportes,entregas,garantia}/src/main/java/bo/aportaya/*/infraestructura/clientes/ClienteConCircuito.java`
- Evidencia: `md5sum` → cinco sumas distintas (`621e2d01…` grupos, `52135020…` organizador, `a1c51408…` aportes, `b91c5601…` entregas, `7e3d990c…` garantia).
- Cómo se comprobó: comando citado. Además `grupos` tiene una segunda instancia del circuito hacia `aportes` en `PrimerAportePorHttp`.
- Impacto: un arreglo de resiliencia se aplica en una copia y no en las otras; el comportamiento ante caída difiere por servicio.
- Causa de fondo: agentes que copiaron en lugar de extraer a `plataforma/comun-web`.
- Solución de fondo: una sola implementación en `plataforma/comun-web`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L1, L2, L5.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (reusabilidad); regla 96.1.
- Esfuerzo estimado: medio


#### Clase REESCRITURA

### grupos-organizador-REESCRITURA-001 — La habilitación de organizador tiene un camino heredado de aprobación que convive con el nuevo, y la «habilitación tras capacitación» nunca puede tener éxito
- Clase / Severidad / Confianza: REESCRITURA / S3 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90PostularOrganizador.java:116-185,203-206`; `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90ResolverHabilitacion.java:68-134`; `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/OrganizadorRepositorio.java:222-233`
- Evidencia: `aprobar(EntradaAprobacion, ctx)` (heredado: sin `Backoffice.exigir`, sin clave de idempotencia, sin revisión esperada) frente a `CU90ResolverHabilitacion` (con las tres). `capacitacionesAprobadas` cuenta filas de `organizador.capacitacion_organizador`.
- Cómo se comprobó: `grep -rn 'Backoffice\.' CU90PostularOrganizador.java` → 0; `grep -rn 'INSERT INTO organizador.capacitacion' servicios` → solo `servicios/organizador/src/test/java/bo/aportaya/organizador/FixturaDeOrganizador.java:135`.
- Impacto: dos puertas para lo mismo con controles distintos; el backoffice llama `POST /organizadores/{id}/habilitacion` y esa operación nunca encuentra capacitaciones en producción: el organizador aprobado por bandeja queda en `CAPACITACION_PENDIENTE`.
- Causa de fondo: el flujo nuevo se agregó sin retirar el viejo.
- Solución de fondo: retirar `aprobar` heredado (y su ruta) con versión del contrato; decidir si la capacitación existe (A-1) y, si existe, quién la registra.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: backoffice `postulaciones/`, `cumplimiento/organizadores`.
- Estándares y normas: ISO/IEC 25010 mantenibilidad; reglas 96.1, 98.1.3.
- Esfuerzo estimado: medio


#### Clase FRAGIL

### grupos-organizador-FRAGIL-001 — El mismo interruptor tiene valor por omisión distinto en TEST y en producción
- Clase / Severidad / Confianza: FRAGIL / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:78`; `docker-compose.coolify.yml:377`
- Evidencia: `humana-obligatoria: ${ADMISION_HUMANA_OBLIGATORIA:true}` frente a `ADMISION_HUMANA_OBLIGATORIA: ${ADMISION_HUMANA_OBLIGATORIA:-false}`.
- Cómo se comprobó: `grep -rn 'ADMISION_HUMANA_OBLIGATORIA' --include=*.yml --include=*.yaml .` → solo esas dos definiciones.
- Impacto: todo lo que se prueba en TEST (E2E, pruebas manuales, app) ejercita las puertas que producción cierra. Si cambia una variable de entorno en el despliegue, se rompe la admisión en la app (CONTRATO-001) y ninguna prueba de TEST lo detecta.
- Causa de fondo: una decisión de negocio pendiente (`application.yml:74`) resuelta por configuración, distinta por entorno.
- Solución de fondo: la de APROB-001. Mientras exista el interruptor, el mismo valor en todos los entornos de prueba que el de producción.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: despliegue (L5), app móvil.
- Estándares y normas: ISO/IEC 25010 fiabilidad (madurez); regla 90.3.4.
- Esfuerzo estimado: chico

### grupos-organizador-FRAGIL-003 — La vía básica deja al organizador `HABILITADO` antes de firmar, y `grupos` lo cree habilitado hasta que un disparador de la base lo rechaza
- Clase / Severidad / Confianza: FRAGIL / S2 / confirmado (organizador); probable (el 500 en `grupos`)
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90HabilitacionBasica.java:133-141`; `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/ConsultarHabilitacion.java:40-46,69-70`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java:123-146`; `sql/40_reglas/restricciones.sql:2686-2701`
- Evidencia:
  ```java
  organizadorId = organizadores.crear(dsl, usuarioId, ESTADO_HABILITADO, NivelDeOrganizador.APRENDIZ.name(), ...);
  ...
  "HABILITADO".equals(fila.get("estado", String.class)));   // ConsultarHabilitacion: no mira el contrato
  ```
  ```sql
  RAISE EXCEPTION 'R-ORG-02: el organizador % no tiene contrato firmado y vigente', NEW.organizador_id;
  ```
- Cómo se comprobó: lectura de las cuatro ubicaciones.
- Impacto: quien aceptó el contrato y no lo firmó pasa las validaciones de `CU20CrearGrupo` y el alta termina en una excepción de la base, probablemente un 500 sin código de negocio. La regla «habilitado = estado + contrato firmado vigente» está escrita dos veces de forma distinta (`CU90HabilitacionBasica.estadoDe:240` sí mira el contrato).
- Causa de fondo: regla de negocio duplicada con dos respuestas (DUP-INT divergente).
- Solución de fondo: una sola definición de «habilitado» en `organizador`, que exija el contrato vigente, y la usen las dos consultas.
- Relación con el flujo nuevo: ninguna (puerta previa al alta)
- Conexiones y módulos afectados: `grupos` (alta), app (`contrato_de_organizador.dart`).
- Estándares y normas: ISO/IEC 25010 fiabilidad; reglas 96.4.4, 98.1.
- Esfuerzo estimado: chico

### grupos-organizador-FRAGIL-002 — El estado aceptado de una solicitud tiene dos nombres (`APROBADA`/`ACEPTADA`) en el mismo contrato
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/openapi/grupos.yaml:1703,1720`; `sql/10_tablas/02_grupos_turnos/solicitud_ingreso.sql:21`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/BandejaDeAdmisiones.java:35,61`; `apps/backoffice/src/app/rutas/admisiones/pantalla-de-admision.ts:52`; `apps/backoffice/src/app/rutas/admisiones/entrada-de-admision.ts:152`
- Evidencia:
  ```yaml
  SolicitudDeIngreso:  estado: { enum: [PENDIENTE, APROBADA, RECHAZADA, EXPIRADA] }
  SolicitudDeAdmision: estado: { enum: [PENDIENTE, ACEPTADA, RECHAZADA, EXPIRADA] }
  ```
  ```java
  String enBase = estado == null ? null : "ACEPTADA".equals(estado) ? "APROBADA" : estado;
  ```
- Cómo se comprobó: lectura; además el filtro del backoffice ofrece `'ESCALADA'` (`entrada-de-admision.ts:152`), que no está en ningún enum del contrato ni en el `CHECK` de la tabla: ese filtro siempre devuelve vacío.
- Impacto: si alguien agrega un estado o cambia la traducción de `BandejaDeAdmisiones`, la bandeja o la pantalla de admisión muestran estados falsos. El backoffice ya compara contra los dos nombres. Ninguna prueba de contrato lo detecta (no hay pacto para estas operaciones).
- Causa de fondo: lenguaje ubicuo roto entre esquema y contrato; la traducción vive en un caso de uso.
- Solución de fondo: un solo nombre de estado en esquema, contrato y clientes, con versión del contrato si cambia.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: backoffice.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (modificabilidad); reglas 96.4.1, 98.1.3.
- Esfuerzo estimado: chico

### grupos-organizador-FRAGIL-004 — Estados y roles como cadenas sueltas repetidas en muchas clases, con listas distintas
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/Consultas.java:208-211`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/ConsultasRepositorio.java:55`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/EmparejamientoRepositorio.java:56`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/ConsultarMisLimites.java:43`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU20CrearGrupo.java:174`
- Evidencia: `return "BACKOFFICE".equals(ctx.rol()) || …` (omite `ADMIN_PLATAFORMA`, que el resto del servicio usa); listas de estados terminales `('CERRADO','CANCELADO','DISUELTO')`, `('FINALIZADO','CANCELADO','DISUELTO_ANTICIPADAMENTE')`.
- Cómo se comprobó: `grep` de cada lista; comparación con `grupo.sql:37`.
- Impacto: si cambia un estado o un rol, unas consultas lo reconocen y otras no (DEFECTO-002 es un caso ya roto). Ninguna prueba compara las listas.
- Causa de fondo: falta un enumerado de dominio para estados y roles.
- Solución de fondo: enumerados de dominio generados del modelo y usados en el SQL por parámetro.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (modificabilidad).
- Esfuerzo estimado: medio


#### Clase DEFECTO

### grupos-organizador-DEFECTO-001 — Aceptar una apelación rehabilita al organizador aunque tenga otra sanción vigente
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU93SancionarOrganizador.java:216-223`
- Evidencia:
  ```java
  if (entrada.aceptada()) {
      var organizador = organizadores.bloquear(dsl, sancion.organizadorId()).orElseThrow();
      organizadores.cambiarEstado(dsl, organizador.id(), "HABILITADO", organizador.version(), ahora);
  ```
- Cómo se comprobó: lectura; no se consulta si hay otras sanciones `VIGENTE` del mismo organizador, y el resultado de `cambiarEstado` no se mira.
- Impacto: un organizador con una inhabilitación firme y una advertencia apelada vuelve a `HABILITADO` al ganar la apelación menor.
- Causa de fondo: el estado del organizador se escribe como campo en vez de derivarse de las sanciones vigentes (regla 91.4.2: la reputación es consecuencia del expediente).
- Solución de fondo: derivar el estado de las sanciones vigentes al cerrar cualquier apelación.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `grupos` (`organizadorHabilitado` lee ese estado).
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 91.4.2.
- Esfuerzo estimado: chico

### grupos-organizador-DEFECTO-002 — «En cuántos grupos vivos participa» usa estados que no existen, y decide si una persona puede cerrar su billetera
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/ConsultasRepositorio.java:46-58`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RespuestasAOtrosServicios.java:50-53`; `sql/10_tablas/02_grupos_turnos/grupo.sql:37`
- Evidencia: `AND g.estado NOT IN ('CERRADO', 'CANCELADO', 'DISUELTO')` frente al `CHECK` `('ABIERTO_A_INSCRIPCION','ACTIVO','BORRADOR','CANCELADO','CONFORMADO','DISUELTO_ANTICIPADAMENTE','EN_CURSO','FINALIZADO','SUSPENDIDO')`; comentario del consumidor: «Cero significa que puede cerrar su billetera».
- Cómo se comprobó: comparación de la lista con el `CHECK`; además solo cuenta `p.estado = 'ACTIVO'`, así que un participante `EN_MORA` o `SUSPENDIDO` cuenta cero.
- Impacto: una persona en mora en un grupo vivo aparece con «0 grupos activos» y puede cerrar su billetera debiendo al grupo. A la vez, grupos `FINALIZADO` cuentan como vivos.
- Causa de fondo: cadenas mágicas de estado repetidas en varias consultas, cada una con su propia lista (ver FRAGIL-004).
- Solución de fondo: un único conjunto «estados terminales» en el dominio, usado por todas las consultas; contar participantes no retirados.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `nucleo-financiero`/`identidad` (cierre de billetera, consumidor de `consultarActividad`).
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 91.
- Esfuerzo estimado: chico

### grupos-organizador-DEFECTO-003 — Resolver un acuerdo escribe el estado sin precondición: dos votos simultáneos lo resuelven dos veces
- Clase / Severidad / Confianza: DEFECTO / S2 / probable (falta la prueba concurrente)
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AcuerdoRepositorio.java:133-141`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU63Acordar.java:114-150`
- Evidencia:
  ```java
  dsl.update(ACUERDO).set(ACUERDO.ESTADO, estado) ... .where(ACUERDO.ID.eq(acuerdoId)).execute();   // sin AND estado='ABIERTO'
  ```
- Cómo se comprobó: lectura; `resolver` lee los votos, decide y escribe sin `FOR UPDATE` ni condición de estado; además devuelve `"EN_VOTACION"`, que no es un estado del `CHECK` (`ABIERTO`).
- Impacto: dos `acuerdo_resuelto` para el mismo acuerdo, o un acuerdo `EJECUTADO` que vuelve a `APROBADO` y se reutiliza para otro traspaso de cupo.
- Causa de fondo: precondición en un `if` en lugar de en la escritura (regla 96.3.2).
- Solución de fondo: `UPDATE … WHERE id = ? AND estado = 'ABIERTO'` y tratar 0 filas como «ya resuelto».
- Relación con el flujo nuevo: APROB (votación del grupo)
- Conexiones y módulos afectados: CU-64 traspaso.
- Estándares y normas: ISO/IEC 25010 fiabilidad; CWE-362; reglas 96.3.1, 96.3.2.
- Esfuerzo estimado: chico

### grupos-organizador-DEFECTO-004 — Fechas del calendario: el día de hoy se toma en UTC y la fecha de fin ignora la periodicidad
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69IniciarGrupo.java:132`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69FirmarReglamento.java:214`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:110`; `CU69IniciarGrupo.java:230`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CanjearInvitacion.java:53`
- Evidencia: `calendario.generar(dsl, grupoId, ahora.toLocalDate());` con `ahora` en `ZoneOffset.UTC`; `.set(GRUPO.FECHA_FIN_ESTIMADA, datos.fechaDeInicio().plusMonths(datos.cupos()))`; `.isAfter(java.time.OffsetDateTime.now())`.
- Cómo se comprobó: lectura; `application.yml` declara `zona-horaria: America/La_Paz`.
- Impacto: un grupo iniciado entre las 20:00 y las 24:00 de La Paz arma su periodo 1 un día después. Un grupo semanal de 10 cupos muestra fin estimado a 10 meses. `CanjearInvitacion` usa el reloj del sistema y no el inyectado (las pruebas no lo controlan).
- Causa de fondo: conversión de zona no centralizada.
- Solución de fondo: `Reloj.hoyEnLaPaz()` único y fecha fin calculada con la misma función que el calendario.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `aportes` (vencimientos).
- Estándares y normas: ISO 8601; ISO/IEC 25010 corrección funcional.
- Esfuerzo estimado: chico

### grupos-organizador-DEFECTO-005 — La pantalla de invitar por número comparte su estado entre todos los grupos
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado (código; no ejecutado)
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/cu69_invitar_al_grupo.dart:50-53`
- Evidencia: `final invitarAlGrupoProvider = AsyncNotifierProvider<InvitarAlGrupo, SalidaInvitacion?>(InvitarAlGrupo.new);` (sin `.family` ni `autoDispose`).
- Cómo se comprobó: lectura.
- Impacto: tras invitar a alguien al grupo A, abrir «invitar» del grupo B muestra el resultado de A.
- Causa de fondo: proveedor global para un estado que es por grupo.
- Solución de fondo: proveedor `family` por `grupoId` y `autoDispose`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: ISO/IEC 25010 corrección funcional.
- Esfuerzo estimado: chico


#### Clase HUECO

### grupos-organizador-HUECO-001 — No existe el escalamiento de solicitudes vencidas que la pantalla de backoffice promete
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.ts:14-22`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:58,141,163,211,1095` (rutas de solicitudes publicadas); `scripts/verificar_rutas_frontend.py:27`
- Evidencia:
  ```ts
  * D-15 · solicitudes de ingreso que el organizador dejó vencer (48 horas).
  * **Sin contrato (2026-10-26, franja B).** Ni `grupos.yaml` ni el simulado ... publican una cola de solicitudes escaladas
  ```
- Cómo se comprobó: `grep -rniE 'escalad|PT48H|48 ?h|vencid' servicios/grupos/src/main` → ningún trabajo programado ni estado que venza una solicitud `PENDIENTE`; las únicas coincidencias son de invitaciones, feriados y traspasos. `grupos.yaml` no publica `/grupos/solicitudes-escaladas`.
- Impacto: una solicitud que el organizador nunca propone queda `PENDIENTE` sin plazo; con APROB-001 encendido, el participante no entra nunca y nadie se entera. Para el grupo: el inicio al 75 % (CU-69) puede no alcanzarse.
- Causa de fondo: la regla de 48 h se diseñó en la pantalla y en el texto, nunca en el servicio dueño.
- Solución de fondo: depende de A-1. Si la admisión deja de ser humana, el hueco desaparece con APROB-001. Si se conserva, el vencimiento va en `grupos` como trabajo programado con estado propio y evento.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: backoffice `operacion/solicitudes-escaladas`; menú `apps/backoffice/src/app/nucleo/menu.ts:91`.
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 00 §5.4 (implementación sustituida por texto), 95.2.
- Esfuerzo estimado: medio

### grupos-organizador-HUECO-002 — No existe la cancelación con devolución de un grupo que no llega al 75 % (ADR-054 D4)
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:78`; ausencia en `servicios/grupos/src/main/java/**`
- Evidencia: ADR-054 D4: «Si no llega al 75 % en la fecha límite, se cancela y devuelve solo: `estado = CANCELADO` … evento `grupo.cancelado_sin_inicio`».
- Cómo se comprobó: `grep -rln 'cancelado_sin_inicio' --include=*.java --include=*.yaml --include=*.sql --include=*.json .` → 0 archivos; `grep -rn '@Scheduled' servicios/grupos` → 0. Las menciones de `CANCELADO` en `grupos` son solo filtros de lectura (`ConsultasRepositorio.java:55`, `ConsultarMisLimites.java:43`, `CU20CrearGrupo.java:174`).
- Impacto: un grupo que nunca alcanza el 75 % queda `ABIERTO_A_INSCRIPCION` para siempre. Si alguno ya pagó su primera cuota por QR (la regla de inicio la exige), ese dinero no tiene devolución automática.
- Causa de fondo: la misma de APROB-002, el carril de ADR-054 sin implementar.
- Solución de fondo: transición `ABIERTO_A_INSCRIPCION → CANCELADO` en `grupos`, con evento por outbox, y saga de devolución en el servicio dueño del dinero (L1). No requiere decisión nueva: D4 ya la tomó.
- Relación con el flujo nuevo: FLUJO-QR (dinero cobrado sin destino)
- Conexiones y módulos afectados: `aportes`/`nucleo-financiero` (devolución), notificaciones.
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 91.3, 98.3.2.
- Esfuerzo estimado: grande

### grupos-organizador-HUECO-004 — La solicitud de retiro nace `PENDIENTE` y nada la resuelve: `CU65Retirarse.aprobar` no tiene quien la llame
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU65Retirarse.java:80-99`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/RetiroRepositorio.java:36,45-50`
- Evidencia: `.set(SOLICITUD_RETIRO.ESTADO, "PENDIENTE")` al pedir; `public void aprobar(...)` que pasa a `APROBADA`.
- Cómo se comprobó: `grep -rn 'cu65\.aprobar\|\.aprobar(' servicios/grupos/src/main` → solo la llamada interna `retiros.aprobar` dentro del propio método; ningún controlador, consumidor ni trabajo llama a `CU65Retirarse.aprobar`.
- Impacto: un participante que pide retirarse queda `PENDIENTE` sin plazo; su cupo y sus obligaciones siguen. Hay además una aprobación diseñada (APR-06) que contradice R-NUEVA-1.
- Causa de fondo: caso de uso de dos pasos con solo el primero cableado.
- Solución de fondo: decidir (A-1/A-4) si el retiro necesita aprobación. Si no la necesita, el retiro se ejecuta en el mismo caso de uso con su liquidación. Si la necesita, se cablea quién la da y con qué plazo.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: app (`pantalla_retiro`, fuera de este lote), `garantia` (plan de regularización).
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 00 §5.4.
- Esfuerzo estimado: medio

### grupos-organizador-HUECO-006 — Ningún servicio avanza el grupo a `EN_CURSO`/`FINALIZADO` ni el turno a `COBRADO`: las guardas que dependen de eso están muertas
- Clase / Severidad / Confianza: HUECO / S2 / confirmado (código); probable (efecto en dinero, depende de L2)
- Ubicaciones: `sql/10_tablas/02_grupos_turnos/grupo.sql:37`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU65Retirarse.java:58`; `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/PosicionAlRetirarse.java:29-31`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/TraspasoRepositorio.java:27-28`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/SorteoRepositorio.java:155`
- Evidencia:
  ```java
  boolean arranco = "EN_CURSO".equals(retiros.estadoDelGrupoDe(dsl, entrada.participanteId()));
  if (!elGrupoArranco) { return new PosicionAlRetirarse(Tipo.ACREEDORA, totalAportado, Momento.INMEDIATO); }
  boolean cobrado = dsl.fetchExists(... TURNO.ESTADO.eq("COBRADO"));
  ```
- Cómo se comprobó: `grep -rnE "UPDATE grupos\.(grupo|turno|periodo)" servicios plataforma sql/40_reglas` fuera de `servicios/grupos` → 0. Dentro de `grupos`, las únicas escrituras de estado del grupo son `BORRADOR`, `ABIERTO_A_INSCRIPCION` y `CONFORMADO`; el turno solo nace `PROGRAMADO` (`SorteoRepositorio.java:155`).
- Impacto: todo retiro se calcula como «acreedor, devolver íntegro, inmediato» aunque el ciclo esté corriendo. La guarda «cupo con turno ya cobrado no se traspasa» nunca bloquea: quien ya cobró el pozo puede traspasar o subastar su cupo y dejar al comprador con los aportes restantes sin turno que cobrar. Los indicadores de turnos dan cero.
- Causa de fondo: el ciclo de vida del grupo después del sorteo no tiene dueño en el código; `entregas` no puede escribir este esquema (regla 98.1) y no hay evento que `grupos` consuma.
- Solución de fondo: `grupos` consume los hechos de cobro del turno (`entregas`) y de cierre de periodo, y mueve sus estados con precondición en el `UPDATE`.
- Relación con el flujo nuevo: FLUJO-FALTANTE (con el grupo sin estado, la rama de cierre de turno no existe en `grupos`)
- Conexiones y módulos afectados: `entregas` (L2), `aportes` (L1).
- Estándares y normas: ISO/IEC 25010 corrección funcional; reglas 96.3.2, 98.3.
- Esfuerzo estimado: grande

### grupos-organizador-HUECO-007 — Los acuerdos del grupo se votan pero no tienen efecto: condonar, expulsar, disolver o cambiar el reglamento no hace nada
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU63Acordar.java:29-32,114-150`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:466-470`
- Evidencia: `resolver` solo hace `acuerdos.resolver(...)` y `outbox.emitir("grupos.acuerdo_resuelto", …)`; no hay rama por tipo.
- Cómo se comprobó: lectura del método; `grep -n 'EXPULSION\|CONDONACION\|DISOLUCION' CU63Acordar.java` → 0. Según el subagente, nadie consume `acuerdo_resuelto` (búsqueda de `@KafkaListener` en `servicios/`).
- Impacto: el grupo cree que decidió (expulsar a un moroso, condonar una deuda, disolverse) y nada cambia. Con R-NUEVA-2, la disolución por mora es justamente una salida al faltante (A-2), y no existe.
- Causa de fondo: caso de uso que promete ejecutar «en la misma transacción» y solo registra.
- Solución de fondo: efecto por tipo en el servicio dueño, o retirar del contrato los tipos sin efecto. Depende de A-1 (¿siguen existiendo votaciones?) y A-2.
- Relación con el flujo nuevo: APROB / FLUJO-FALTANTE
- Conexiones y módulos afectados: `aportes`/`garantia` (condonación), `entregas` (disolución).
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 00 §6.2.
- Esfuerzo estimado: grande

### grupos-organizador-HUECO-003 — El catálogo de errores de la app no tiene los códigos de admisión ni de sorteo
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `apps/movil/lib/dominio/errores.dart:54-78`; consumidores `apps/movil/lib/pantallas/pasanaku/pantalla_pedir_cupo.dart:136`, `pantalla_sorteo.dart:127-131`
- Evidencia: `grep -nE "AP-CU68|AP-CU60" apps/movil/lib/dominio/errores.dart` → sin resultados; el archivo solo tiene `'AP-CU69-10'` del módulo.
- Cómo se comprobó: búsqueda citada; las pantallas consumidoras las listó el subagente de frontend y se abrió `errores.dart`.
- Impacto: rechazos de negocio de admisión y sorteo (`AP-CU68-*`, `AP-CU60-*`) se muestran con texto genérico; la persona no sabe qué hacer (regla 95.2.3).
- Causa de fondo: el catálogo de errores del cliente se escribe a mano y no se genera del contrato (que sí lista los códigos, p. ej. `grupos.yaml:886,1167`).
- Solución de fondo: generar el catálogo desde la tabla de errores del contrato.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: app móvil.
- Estándares y normas: ISO/IEC 25010 usabilidad (protección frente a errores); RFC 9457; regla 95.2.3.
- Esfuerzo estimado: chico

### grupos-organizador-HUECO-005 — Una suspensión con plazo nunca vence: `vigente_hasta` se escribe y nadie lo lee
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/DesempenoRepositorio.java:100`; `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU93SancionarOrganizador.java:77`
- Evidencia: `OffsetDateTime hasta = entrada.duracion().map(ahora::plus).orElse(null);` → `.set(DSL.field("vigente_hasta", ...), hasta)`.
- Cómo se comprobó: `grep -rn 'vigente_hasta' servicios/organizador/src/main` → los otros usos son de `capacitacion_organizador` y `contrato_organizador`, no de `sancion_organizador`; `grep -rn '@Scheduled' servicios/organizador` → 0; `servicios/organizador/src/main/java/bo/aportaya/organizador/trabajos/` solo tiene `.gitkeep`.
- Impacto: una suspensión temporal se vuelve permanente hasta que alguien la apele y gane.
- Causa de fondo: falta el trabajo programado que levante sanciones vencidas.
- Solución de fondo: trabajo con lock que pase a `CUMPLIDA` las sanciones vencidas y recalcule el estado del organizador.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `grupos`.
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 91.4.
- Esfuerzo estimado: chico

### grupos-organizador-HUECO-008 — La ruta de la app para postular a un grupo (`pedir-cupo`) existe pero nada navega a ella
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas.dart:47-52`
- Evidencia: `path: '/pasanaku/grupos/:grupoId/pedir-cupo', name: 'pasanaku.pedirCupo'`.
- Cómo se comprobó: `grep -rn 'pedir-cupo\|pedirCupo' apps/movil/lib` → solo la definición, la lista de acceso y el texto del botón dentro de la propia pantalla.
- Impacto: CU-68 «postular» no es alcanzable desde la app.
- Causa de fondo: pantalla construida sin punto de entrada.
- Solución de fondo: decidir si la postulación existe con R-NUEVA-1; si existe, enlazarla; si no, retirarla.
- Relación con el flujo nuevo: APROB (la postulación termina en decisión humana)
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: ISO/IEC 25010 completitud funcional.
- Esfuerzo estimado: chico


#### Clase RESILIENCIA

### grupos-organizador-RESILIENCIA-002 — Los consumidores de Kafka de `grupos` no tienen cola de muertos: una cesión ya pagada se puede descartar
- Clase / Severidad / Confianza: RESILIENCIA / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/consumo/ConsumidorDeCesionesKafka.java:25-31`; `servicios/grupos/src/main/java/bo/aportaya/grupos/consumo/ConsumidorDePreciosCongeladosKafka.java`; `servicios/grupos/src/main/java/bo/aportaya/grupos/consumo/ConsumidorDeRevocacionesKafka.java`
- Evidencia: «El manejo de errores es el del contenedor de Spring Kafka (reintentos y descarte tras agotarlos); una cola de mensajes muertos con alerta es la decisión pendiente de operaciones.»
- Cómo se comprobó: lectura del comentario y de los tres consumidores (sin `DeadLetterPublishingRecoverer` ni manejador propio).
- Impacto: si `entregas` ya cobró la venta de un turno y `grupos` rechaza la cesión (comprador no activo, orden de llegada), el evento se descarta tras los reintentos: el dinero se movió y el cupo sigue con el vendedor, sin alerta.
- Causa de fondo: decisión operativa pendiente, sin degradación declarada (regla 98.2.4).
- Solución de fondo: DLQ con alerta y reproceso, más compensación en `entregas` si la cesión es definitivamente inválida.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `entregas` (L2), `tarifas`, `organizador`.
- Estándares y normas: ISO/IEC 25010 fiabilidad (recuperabilidad); reglas 96.7.3, 98.3.4.
- Esfuerzo estimado: medio

### grupos-organizador-RESILIENCIA-001 — Sancionar, apelar, evaluar, habilitar, emitir contrato y las operaciones de tareas no tienen clave de idempotencia
- Clase / Severidad / Confianza: RESILIENCIA / S3 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU93SancionarOrganizador.java:59-60` (firma sin clave); `servicios/organizador/src/main/java/bo/aportaya/organizador/web/OrganizadoresController.java:127-264`; `servicios/organizador/src/main/java/bo/aportaya/organizador/web/AutomatizacionController.java:104-117`
- Evidencia: `public SalidaSancion sancionar(EntradaSancion entrada, ContextoSesion ctx)`, sin `Idempotency-Key` ni deduplicación.
- Cómo se comprobó: lectura de las firmas; el subagente de organizador listó las operaciones sin clave y se abrieron `sancionar` y `anotarEjecucion`.
- Impacto: un reintento del cliente crea una segunda sanción `VIGENTE` y un segundo evento; `anotarEjecucion` suma intentos de más.
- Causa de fondo: la idempotencia se aplicó caso a caso (sí en `CU90ResolverHabilitacion`), no como regla del controlador.
- Solución de fondo: `Idempotency-Key` obligatoria en toda mutación del contrato y deduplicación por índice `UNIQUE`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: backoffice (formularios de sanción y habilitación).
- Estándares y normas: ISO/IEC 25010 fiabilidad; reglas 96.3.3, 90.1.10.
- Esfuerzo estimado: medio

### grupos-organizador-RESILIENCIA-003 — La `Idempotency-Key` que exige el contrato se ignora en sorteo, retiro, permuta, acuerdos y traspaso por subasta
- Clase / Severidad / Confianza: RESILIENCIA / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java:165-177,181-190`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSubastaDelCupo.java:123`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/TurnosController.java:43`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/AcuerdosController.java:49,103`
- Evidencia: `comprometerSorteo(UUID grupoId, UUID idempotencyKey, EntradaCompromiso cuerpo)` y el cuerpo nunca usa `idempotencyKey`.
- Cómo se comprobó: lectura de las firmas y de las llamadas al caso de uso.
- Impacto: un reintento del cliente devuelve un error de negocio («ya tiene su sorteo») en vez de la respuesta original, o inserta un segundo `PENDIENTE` (retiro, permuta) que choca con un índice parcial y sale como 500.
- Causa de fondo: el contrato declara la cabecera pero el patrón de idempotencia no es transversal.
- Solución de fondo: guardar la respuesta por clave en una tabla con `UNIQUE` y devolverla en el reintento, como ya hace CU-20.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: app (reintentos).
- Estándares y normas: ISO/IEC 25010 fiabilidad; reglas 96.3.3, 91.1.4.
- Esfuerzo estimado: medio


#### Clase DINERO

### grupos-organizador-DINERO-002 — El destino de una transferencia por «alias» se resuelve con un nombre visible no único y `LIMIT 1` sin orden
- Clase / Severidad / Confianza: DINERO / S1 / probable (falta comprobar si CU-12 muestra y hace confirmar el destinatario antes de mover el dinero)
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/ConsultasRepositorio.java:30-45`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:44-57`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/OrganizadorDecideIngreso.java:126-130`; `sql/10_tablas/02_grupos_turnos/participante.sql:9`; consumidor `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ResolverDestino.java:12-15,41`
- Evidencia:
  ```java
  SELECT p.usuario_id AS usuario FROM grupos.participante p
   WHERE p.alias = ? AND p.estado = 'ACTIVO'
   LIMIT 1
  ```
  ```java
  VALUES (?, ?, ?, (SELECT nombre_visible FROM grupos.solicitud_ingreso WHERE id = ?), ...   // alias = nombre visible
  ```
  ```java
  return afuera.usuarioDelAlias(valor).flatMap(usuario -> cuentas.deUsuario(usuario, ctx));  // nucleo-financiero
  ```
- Cómo se comprobó: lectura de las ubicaciones; `grep -rn alias sql/30_indices/02_grupos_turnos.sql sql/40_reglas/restricciones.sql` → ningún índice único sobre `participante.alias`. La consulta corre con rol de sistema (`Datos.comoSistema`), sobre todos los grupos.
- Impacto: dos participantes con el mismo nombre visible en cualquier grupo hacen que una transferencia de billetera (CU-12) vaya a cualquiera de los dos, según el plan de ejecución de PostgreSQL. El dinero de un participante puede acreditarse a otra persona.
- Causa de fondo: se usa un dato de presentación (nombre) como identificador de destino de dinero.
- Solución de fondo: alias único y explícito (elegido por su titular, con índice `UNIQUE`), o resolver el destino por identificador. Si hay más de una coincidencia, rechazar.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `nucleo-financiero` (CU-12, lote L1).
- Estándares y normas: ISO/IEC 25010 corrección funcional, integridad; CWE-706 (uso de nombre resuelto incorrectamente); regla 91.
- Esfuerzo estimado: medio

### grupos-organizador-DINERO-001 — La evaluación de desempeño fija el límite de dinero que administra un organizador con números que manda el cliente
- Clase / Severidad / Confianza: DINERO / S2 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU92EvaluarDesempeno.java:106-117`; `servicios/organizador/src/main/java/bo/aportaya/organizador/web/MapeoDeOrganizador.java:41-60`; `servicios/organizador/src/main/resources/application.yml:35-40`
- Evidencia:
  ```java
  seAplico = organizadores.cambiarNivel(dsl, organizador.id(), nivelSugerido.name(),
          entrada.limiteDeGruposDelNivel(), entrada.limiteDeMontoDelNivel(), organizador.version());
  ```
  ```yaml
  limite-inicial-de-monto: 30000.00     # sin moneda ni procedencia
  umbral-de-ascenso: 85.00
  ```
- Cómo se comprobó: lectura; los límites del nivel y las métricas llegan en el cuerpo de `POST /organizadores/{id}/evaluaciones` (`ADMIN_PLATAFORMA`), no de un catálogo. `limiteDeMonto` no lleva moneda en el contrato. El subagente informó, y se confirmó con `grep`, que `monto_administrado_actual` nunca se actualiza: el límite de monto no se hace cumplir en ningún lado.
- Impacto: el tope de dinero de terceros que administra una persona depende de lo que tipee un operador; además es un parámetro financiero sin moneda ni fuente (regla 97.5.4) y no se aplica. El descenso de nivel se aplica solo, sin expediente (91.4).
- Causa de fondo: parámetros financieros como entrada libre y configuración literal.
- Solución de fondo: catálogo de niveles con procedencia y moneda; la evaluación calcula métricas desde datos propios; el límite se aplica en el alta.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `grupos` (solo lee `limiteDeGrupos`).
- Estándares y normas: ISO/IEC 25010 corrección funcional; ISO 4217; reglas 91.1.2, 97.5.4, 91.4.
- Esfuerzo estimado: medio

### grupos-organizador-DINERO-003 — Importes del módulo sin moneda al lado, sin `CHECK >= 0`, y uno guardado como texto
- Clase / Severidad / Confianza: DINERO / S3 / confirmado
- Ubicaciones: `sql/10_tablas/02_grupos_turnos/{periodo,turno,solicitud_retiro,traspaso_cupo,solicitud_permuta,postulacion_emparejamiento,propuesta_grupo,alta_grupo}.sql`; `sql/10_tablas/07_organizador_automatizacion/{organizador,requisito_habilitacion}.sql`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:1368-1374,2002-2008`
- Evidencia: `monto_recaudado NUMERIC(14,2) DEFAULT 0 NOT NULL` (sin `moneda`); `fondo_por_periodo VARCHAR(60)`; contrato `pattern: '^-?\d+\.\d{2}$'` para un monto de salida.
- Cómo se comprobó: lectura del DDL (subagente de datos, tabla de inventario; se abrieron `periodo.sql`, `alta_grupo.sql` y el patrón del contrato).
- Impacto: la moneda se infiere del grupo; el día que un grupo opere en USD (el contrato lo admite) los importes derivados quedan ambiguos. `periodo.monto_recaudado` se inserta en 0 y nunca se actualiza (`CalendarioDelGrupoRepositorio.java:43-47`): segunda fuente para un número de dinero que no es el mayor (regla 91.1.5).
- Causa de fondo: modelo de datos sin objeto de valor Dinero.
- Solución de fondo: `moneda CHAR(3)` junto a cada importe, `CHECK >= 0`, retirar `monto_recaudado` y leerlo del mayor.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `nucleo-financiero` (mayor).
- Estándares y normas: ISO 4217; reglas 91.1.2, 91.1.5, 91.2.2.
- Esfuerzo estimado: medio


#### Clase DEBIDO-PROCESO

### grupos-organizador-DEBIDO-PROCESO-001 — La sanción al organizador se ejecuta en el acto; el descargo y la apelación llegan después
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S2 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU93SancionarOrganizador.java:59-118`
- Evidencia:
  ```java
  UUID sancionId = desempenos.sancionar(dsl, organizador.id(), ...);
  String estadoNuevo = switch (entrada.tipo()) {
      case "SUSPENSION" -> "SUSPENDIDO"; case "INHABILITACION" -> "DESHABILITADO"; ...
  if (estadoNuevo != null) { organizadores.cambiarEstado(dsl, organizador.id(), estadoNuevo, ...); }
  var proceso = new DebidoProceso(ahora, plazoParaApelar);
  ```
- Cómo se comprobó: lectura completa del método; no hay estado `PROPUESTA` ni `FIRME`: el cambio de estado del organizador ocurre en la misma transacción que crea la sanción, y el plazo de apelación empieza a correr después.
- Impacto: una persona queda suspendida o inhabilitada sin haber podido defenderse. Regla 91.4.1: «Estado, evidencia, plazo de descargo y estado `FIRME` **antes** de ejecutar».
- Causa de fondo: máquina de estados de la sanción sin fase previa a la firmeza.
- Solución de fondo: `PROPUESTA → (descargo) → FIRME → ejecutada`, con la ejecución en un paso separado y verificable; la apelación solo después de `FIRME`.
- Relación con el flujo nuevo: ninguna (R-NUEVA-1 no elimina el debido proceso)
- Conexiones y módulos afectados: `grupos` (sustitución del administrador consume `habilitacion_revocada`, no `sancionado`: ver HUECO-006).
- Estándares y normas: ISO/IEC 25010 adecuación funcional; regla 91.4.1 y 91.4.4.
- Esfuerzo estimado: medio

### grupos-organizador-DEBIDO-PROCESO-002 — La apelación guarda el argumento que escribe quien resuelve, no el del apelante
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S2 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU93SancionarOrganizador.java:129-176,204-212`; `servicios/organizador/src/main/java/bo/aportaya/organizador/web/OrganizadoresController.java:255-264`
- Evidencia:
  ```java
  // apelar(): el argumento viaja solo en el evento
  "argumento", entrada.argumento(),
  // resolver(): la fila se escribe con lo que manda el resolutor
  UUID apelacionId = desempenos.registrarApelacionResuelta(dsl, sancion.id(),
          entrada.argumento(), entrada.evidenciasJson(), estadoDeLaApelacion, ctx.usuarioId(), ...);
  ```
- Cómo se comprobó: lectura de ambos métodos y del controlador: `resolverApelacion` arma `EntradaResolucion` con `cuerpo.getArgumento()` del cuerpo de la petición del resolutor.
- Impacto: el expediente de apelación no conserva lo que dijo el sancionado; quien resuelve puede escribir cualquier argumento a su nombre. El rastro de debido proceso es falsificable.
- Causa de fondo: la fila de apelación se escribe entera al resolver (`DesempenoRepositorio`), por una restricción de la base que impide la fila «a medias» (comentario de `:129-133`).
- Solución de fondo: persistir la apelación al presentarla (con su estado `ABIERTA`) y que la resolución sea otra fila o un cambio de estado que no reescriba el argumento.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 seguridad (no repudio, responsabilidad); reglas 91.4.3, 90.2.7.
- Esfuerzo estimado: medio


#### Clase ARQ

### grupos-organizador-ARQ-001 — 13 de los 35 casos de uso de `grupos` escriben SQL directo con jOOQ desde la capa de aplicación
- Clase / Severidad / Confianza: ARQ / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/{CU60Sortear,CU69Enlace,CU69EnlaceDeGrupo,CU69FirmarReglamento,CU69IniciarGrupo,ConsultarDetalleDelGrupo,GrupoAbierto,IndicadoresComerciales,IndicadoresDeTurnos,LecturasDelDetalle,OrganizadorDecideIngreso,PrecondicionesDePostulacion,ReglamentoAlIniciar}.java`; ejemplo `CU69IniciarGrupo.java:22-24,83,122-128,219-224`
- Evidencia:
  ```java
  import org.jooq.DSLContext;
  ...
  UPDATE grupos.grupo SET estado = 'CONFORMADO', cupos_totales = ?, ...
   WHERE id = ? AND estado = 'ABIERTO_A_INSCRIPCION'
  ```
- Cómo se comprobó: `grep -l 'import org.jooq' servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/*.java | wc -l` → 13 sobre 35 archivos.
- Impacto: la regla de transición de estado del grupo vive en un `UPDATE` dentro del caso de uso. Cualquier cambio de esquema rompe la capa de aplicación, y las pruebas unitarias del caso de uso no se pueden hacer sin base. Encarece cada cambio.
- Causa de fondo: los puertos de persistencia (`infraestructura/*Repositorio`) existen pero no se usan de forma uniforme; conviven dos estilos en la misma capa.
- Solución de fondo: mover el SQL a repositorios de `infraestructura` detrás de puertos de `dominio/puertos`, como ya hacen los otros 22 casos de uso.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno fuera de `grupos`.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (modularidad, modificabilidad); ADR-033 (puertos y adaptadores), ADR-016 (jOOQ en infraestructura); regla 96.1.3.
- Esfuerzo estimado: grande


#### Clase CLEAN

### grupos-organizador-CLEAN-001 — Código muerto y duplicado en `grupos` y en la app
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado (muestra)
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/EstadoDeSorteo.java`; `.../infraestructura/clientes/HechosPorHttp.java:123-128` (`organizadorHabilitado`, superado por `organizadorHabilitadoDelUsuario`); `.../aplicacion/CU68Postular.java:112-152` (`responder`, solo pruebas); `apps/movil/lib/pantallas/pasanaku/dominio/cu60_sortear_turnos.dart:11-91` (`ComprometerSorteo`, `RevelarSorteo` sin uso); `apps/backoffice/src/app/rutas/admisiones/solicitudes-del-grupo.ts`; `servicios/grupos/src/main/resources/application.yml:107` (`factor-fijo` «SOLO TEST» en la configuración principal)
- Evidencia: `grep -rn 'ComprometerSorteo\|RevelarSorteo\b' apps/movil/lib apps/movil/test` fuera de su archivo → 0.
- Cómo se comprobó: búsqueda de referencias (muestra); el resto lo listó el subagente de `grupos` con su conteo de referencias.
- Impacto: ruido que confunde sobre qué camino está vivo.
- Causa de fondo: reescrituras sin limpieza.
- Solución de fondo: retirar lo que no tiene referencias, con su prueba si la hay.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: ISO/IEC 25010 mantenibilidad.
- Esfuerzo estimado: chico


#### Clase ACOPLE

### grupos-organizador-ACOPLE-001 — Las tablas de `grupos` y `organizador` tienen claves foráneas y un disparador hacia los esquemas de otros servicios
- Clase / Severidad / Confianza: ACOPLE / S2 / confirmado
- Ubicaciones: `sql/20_claves/02_grupos_turnos.sql:25,35,45,50,70,80,85,95,105,120` (hacia `identidad`, `aportes`, `garantia`, `organizador`); `sql/20_claves/07_organizador_automatizacion.sql:87-90` (`tarea_automatizada → grupos.grupo`); `sql/40_reglas/restricciones.sql:2686-2701` (`grupos.fn_org_validar_contrato_grupo` lee `organizador.contrato_organizador`, `SECURITY DEFINER`)
- Evidencia:
  ```sql
  FOREIGN KEY (politica_mora_id) REFERENCES aportes.politica_mora (id) ...
  FOREIGN KEY (organizador_id) REFERENCES organizador.organizador (id) ...
  SELECT 1 FROM organizador.contrato_organizador c WHERE c.organizador_id = NEW.organizador_id ...
  ```
- Cómo se comprobó: `grep -nE 'REFERENCES (identidad|aportes|garantia|organizador|nucleo_financiero)\.' sql/20_claves/02_grupos_turnos.sql sql/20_claves/07_organizador_automatizacion.sql`. El código Java no consulta esquemas ajenos (bien); el acople está en la base.
- Impacto: ningún servicio puede tener su propia base; un cambio de esquema en `organizador` o `aportes` rompe inserciones de `grupos` (alta de grupo vía disparador). La regla R-ORG-02 se aplica en un esquema ajeno a su dueño.
- Causa de fondo: modelo único generado para todos los esquemas (base compartida).
- Solución de fondo: validar contra el servicio dueño por contrato (ya existe `GET /organizadores/{id}/habilitacion`) y retirar FK y disparador entre esquemas, en un plan de expansión y contracción.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `identidad`, `aportes`, `garantia`, `organizador`.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (modularidad); reglas 98.1.1, 98.1.2; ADR-017.
- Esfuerzo estimado: grande

### grupos-organizador-ACOPLE-002 — La app fija en el cliente parámetros que el contrato deja elegir (10 cupos, día de cobro 5, BOB)
- Clase / Severidad / Confianza: ACOPLE / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/pantalla_crear_grupo.dart:20,60-61`; `apps/movil/lib/pantallas/pasanaku/organismos_crear_grupo.dart:154-158,170-177`; `apps/movil/lib/pantallas/pasanaku/dominio/cu20_crear_grupo.dart:50`
- Evidencia: `const cuposPorDefecto = 10;`, `final int _diaCobro = 5;`, `moneda: DineroMonedaEnum.BOB`.
- Cómo se comprobó: lectura de las líneas citadas por el subagente de frontend.
- Impacto: la persona no puede crear un grupo de otro tamaño, otra fecha de cobro ni en USD, aunque el servicio lo admite.
- Causa de fondo: decisiones de producto escritas como constantes de pantalla.
- Solución de fondo: formulario con los campos del contrato, o regla de producto documentada que el servicio aplique.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: regla 95.1; ISO/IEC 25010 adecuación funcional.
- Esfuerzo estimado: chico


#### Clase CONTRATO

### grupos-organizador-CONTRATO-001 — La app móvil usa dos puertas de admisión que el contrato cierra en producción y no tiene las que las reemplazan
- Clase / Severidad / Confianza: CONTRATO / S2 / confirmado (código); probable (el valor real de producción no se pudo ver)
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/solicitudes_del_grupo.dart:24-47`; `apps/movil/lib/pantallas/pasanaku/dominio/cu69_unirse_por_enlace.dart:38-56`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:144-150`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/InvitacionesWeb.java:127-133`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:886`; `docker-compose.coolify.yml:377`; `apps/movil/lib/dominio/errores.dart:76`
- Evidencia:
  ```dart
  await DefaultApi(ref.read(dioProvider)).decidirSolicitudDeIngreso(
    ... motivo: aceptar ? null : 'El organizador no aceptó la solicitud.',
  ```
  ```dart
  final r = await dio.post<Map<String, dynamic>>('/grupos/invitaciones/enlace/aceptar', data: {...});
  ```
  ```yaml
  ADMISION_HUMANA_OBLIGATORIA: ${ADMISION_HUMANA_OBLIGATORIA:-false}   # docker-compose.coolify.yml:377 (TEST)
  humana-obligatoria: ${ADMISION_HUMANA_OBLIGATORIA:true}              # application.yml:78 (por omisión)
  ```
- Cómo se comprobó: `grep -rnE 'proponerAdmision|/propuestas|canjearInvitacion|/canje' apps/movil/lib` → 0 resultados. `grep -nE "AP-CU68|AP-CU60" apps/movil/lib/dominio/errores.dart` → 0; solo existe `'AP-CU69-10'` en la línea 76.
- Impacto: en TEST el interruptor está en `false` y la app funciona. Donde rija el valor por omisión `true`, la aceptación del organizador responde 422 `AP-CU68-15` (sin texto propio: genérico) y la invitación por número responde 422 `AP-CU69-10`. El participante invitado no puede entrar desde la app. Se considera discrepancia latente (S2) y no S1 porque en el entorno vivo auditado el camino funciona.
- Causa de fondo: el contrato cambió de comportamiento por un interruptor de entorno sin versión ni cliente actualizado (regla 98.1.3); ver también FRAGIL-001.
- Solución de fondo: resolver A-1. Si la admisión queda sin aprobación, retirar el interruptor y conservar las puertas que la app usa. Si no, la app implementa `proponerAdmision` y `canjearInvitacion` y su catálogo de errores.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: app móvil, `grupos`.
- Estándares y normas: ISO/IEC 25010 compatibilidad (interoperabilidad); RFC 9110 (422); reglas 96.4.1, 98.1.3, 95.2.3.
- Esfuerzo estimado: medio

### grupos-organizador-CONTRATO-002 — El contrato acepta tipos de acuerdo que el servicio no sabe leer y campos que ignora
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/openapi/grupos.yaml:443,1409`; `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/TipoDeAcuerdo.java:13-21,32-39`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:113`
- Evidencia: el contrato enumera `REPETIR_SORTEO`; el enum Java no lo tiene y `deContrato` cae en `valueOf(valor)` → `IllegalArgumentException`. `EntradaGrupo.modalidadTurnos` es obligatorio y el alta lo fija en `SORTEO_ALEATORIO`.
- Cómo se comprobó: lectura de ambos archivos.
- Impacto: proponer un acuerdo `REPETIR_SORTEO` responde con error interno en lugar de un 422 del contrato; el cliente cree que elige la modalidad de turnos y no.
- Causa de fondo: el contrato y el modelo crecieron por separado.
- Solución de fondo: alinear el enum del contrato con el modelo (versión nueva si se quita un valor).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: app, backoffice.
- Estándares y normas: RFC 9110; reglas 96.2.4, 96.4.1.
- Esfuerzo estimado: chico


#### Clase DATOS

### grupos-organizador-DATOS-001 — Cuatro restricciones de la base comparan contra estados que su propia tabla no admite: nunca se disparan (y una bloquea las apelaciones)
- Clase / Severidad / Confianza: DATOS / S2 / confirmado
- Ubicaciones: `sql/40_reglas/restricciones.sql:1940-1943` (`uq_acuerdo_abierto … WHERE estado = 'EN_VOTACION'`) vs `sql/10_tablas/02_grupos_turnos/acuerdo.sql:24` (`'ABIERTO'`); `restricciones.sql:1945-1951` (`estado <> 'APROBADO'`) vs `solicitud_retiro.sql:16` (`'APROBADA'`); `restricciones.sql:2741-2746` (`estado = 'PENDIENTE' OR (resuelta_en IS NOT NULL …)`) vs `apelacion_sancion_org.sql:15` (`'ACEPTADA','DESISTIDA','EN_REVISION','PRESENTADA','RECHAZADA'`); `restricciones.sql:2772-2778` (`accion NOT IN ('PROPONER_COBRO','PROPONER_ENTREGA',…)`) vs `regla_automatizacion.sql:18` (`'APLICAR_MORA','EJECUTAR_ENTREGA',…`)
- Evidencia: fragmentos citados en Ubicaciones.
- Cómo se comprobó: comparación de cada condición con el `CHECK` de dominio de la tabla. Además `sql/50_verificacion/prueba_humo.sql:720-800` solo verifica que el nombre de la restricción exista (informado por el subagente de datos, abierto el rango).
- Impacto: (1) R-GRP-09 no impide dos votaciones abiertas iguales (expulsión, condonación) y el código hace `SELECT` y luego `INSERT` (`AcuerdoRepositorio.java:24-30`): carrera. (2) R-GRP-12 no exige plan a un retiro deudor. (3) Una apelación abierta (`PRESENTADA`, sin `resuelta_en`) viola el `CHECK`: por eso `CU93` solo escribe la apelación al resolverla (causa de DEBIDO-PROCESO-002). (4) R-ORG-06 no obliga a confirmar acciones de dinero. Las cuatro reglas están declaradas como invariantes de la base y ninguna está en vigor.
- Causa de fondo: el DDL de dominio se genera del modelo y las reglas de `40_reglas` se escriben a mano con otro vocabulario; ninguna prueba ejercita la restricción.
- Solución de fondo: corregir las reglas en su fuente (`docs/Restricciones.md` → `40_reglas`) y agregar a la verificación una prueba que intente violar cada una. Para R-ORG-06, antes decidir A-1.
- Relación con el flujo nuevo: APROB (R-ORG-06, apelación)
- Conexiones y módulos afectados: ninguna externa.
- Estándares y normas: ISO/IEC 25010 integridad; reglas 91.2, 97.2, 91.4.3.
- Esfuerzo estimado: chico

### grupos-organizador-DATOS-002 — El historial de estados del grupo se puede modificar
- Clase / Severidad / Confianza: DATOS / S3 / confirmado
- Ubicaciones: `sql/35_append_only/append_only.sql` (sin `historial_estado_grupo`); `sql/00_base/03_permisos.sql` (`GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA grupos TO svc_grupos`)
- Evidencia: `grep -n 'historial_estado_grupo' sql/35_append_only/append_only.sql` → 0.
- Cómo se comprobó: búsqueda citada; `alta_grupo`, `decision_ingreso`, `snapshot_sorteo` y `sustitucion_administrador` sí están protegidas (bien).
- Impacto: el rastro de quién inició, conformó o canceló un grupo se puede reescribir con el rol de la aplicación.
- Causa de fondo: tabla de auditoría no incluida en la lista append-only.
- Solución de fondo: agregarla a `35_append_only` (disparador + `REVOKE UPDATE, DELETE`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: auditoría (L5).
- Estándares y normas: reglas 91.1.3, 90.2.7.
- Esfuerzo estimado: chico


#### Clase PRUEBAS

### grupos-organizador-PRUEBAS-002 — Las pruebas validan el flujo viejo (aprobación humana, organizador obligatorio) y ninguna cubre el grupo sin organizador
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/test/java/bo/aportaya/grupos/**/{GruposAdmisionHumanaWebTest,InvitacionesAdmisionHumanaWebTest,CU69IniciarGrupoTest,CU69EventoDeInicioTest}.java` y las listadas en el anexo; `servicios/organizador/src/test/java/bo/aportaya/organizador/` (CU96Test.criterio2, CU96RechazosTest)
- Evidencia: nombres de clase citados (verificados con `ls -R`); según el subagente de pruebas, 591 pruebas en 90 clases de `grupos` y ninguna con `organizadorId` vacío ni con inicio o sorteo automáticos. No hay `@Disabled`, `skip` ni pruebas comentadas.
- Cómo se comprobó: listado de archivos; la lectura línea a línea de cada clase no se hizo (ver Límites).
- Impacto: cualquier cambio hacia R-NUEVA-1 rompe decenas de pruebas que defienden la aprobación, y ninguna prueba defiende el comportamiento nuevo.
- Causa de fondo: el producto cambió de decisión antes de que el código cambiara.
- Solución de fondo: después de A-1, reescribir las pruebas que afirman la aprobación como pruebas del flujo nuevo, sin borrar ninguna sin registro (regla 80.5.5).
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: regla 80.
- Esfuerzo estimado: grande

### grupos-organizador-PRUEBAS-003 — No hay prueba de contrato del lado productor para los pactos que `grupos` cumple
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `contratos/pactos/identidad-grupos-pertenencia.json`; `contratos/pactos/transparencia-grupos-pertenencia.json`; `contratos/pactos/grupos-aportes-grupo-iniciado.json`
- Evidencia: el propio pacto lo dice: «NO existe todavia una prueba contra el proceso real de grupos» (citado por el subagente de datos; verificado con `grep -n 'NO existe todavia' contratos/pactos/*pertenencia.json`).
- Cómo se comprobó: búsqueda de los nombres de pacto en `servicios/grupos/src/test` (subagente) y lectura del texto del pacto.
- Impacto: un cambio de `consultarSiCompartenGrupo` o del payload de `grupo_iniciado` rompe a `identidad`, `transparencia` o `aportes` sin aviso (regla 98.1.4).
- Causa de fondo: verificación de pactos solo del lado consumidor.
- Solución de fondo: verificación del pacto contra el proveedor real en CI.
- Relación con el flujo nuevo: FLUJO-QR (`grupo_iniciado` abre el cobro)
- Conexiones y módulos afectados: `identidad`, `transparencia`, `aportes`.
- Estándares y normas: regla 98.1.4.
- Esfuerzo estimado: medio

### grupos-organizador-PRUEBAS-001 — El verificador de rutas del frontend falla por una ruta de grupos marcada como conocida
- Clase / Severidad / Confianza: PRUEBAS / S4 / confirmado
- Ubicaciones: `scripts/verificar_rutas_frontend.py:27`
- Evidencia (salida literal en `evidencia/verificar_rutas_frontend.txt`):
  ```text
  FALLA · la lista de conocidas se pudrió: estas rutas YA tienen contrato (sacalas de CONOCIDAS_SIN_CONTRATO):
     - GET /grupos/solicitudes-escaladas
  [exit 1]
  ```
- Cómo se comprobó: `python3 scripts/verificar_rutas_frontend.py` → exit 1. Pero `grep -n 'solicitudes-escaladas' servicios/grupos/src/main/resources/openapi/grupos.yaml` no devuelve nada: la ruta **no** tiene contrato. El script decide «tiene contrato» por otra regla, probablemente el prefijo `/grupos/solicitudes`, y se equivoca.
- Impacto: un gate de CI en rojo por un falso positivo acostumbra a ignorar el rojo.
- Causa de fondo: el emparejamiento de rutas del script es más laxo que el contrato.
- Solución de fondo: emparejar por plantilla de ruta completa, sin prefijo. Al resolverse HUECO-001, la entrada se retira de la lista.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: herramienta transversal `scripts/`.
- Estándares y normas: ISO/IEC 25010 fiabilidad (madurez del proceso de verificación); regla 80.5.
- Esfuerzo estimado: chico


#### Clase UX-A11Y

### grupos-organizador-UX-A11Y-001 — Pantallas del módulo con estados vacíos mudos, textos y tipografía fuera del sistema, y la marca escrita de dos formas
- Clase / Severidad / Confianza: UX-A11Y / S4 / confirmado (muestra)
- Ubicaciones: vacíos `mensajeVacio: ''` en `apps/movil/lib/pantallas/pasanaku/{seccion_del_grupo.dart:36,pantalla_sorteo.dart:150,pantalla_unirse_al_grupo.dart:79,pantalla_enlace_del_grupo.dart:122}`; tipografía literal `font-size` en `apps/backoffice/src/app/rutas/admisiones/{entrada-de-admision.ts:87-91,historial-de-admision.ts:55-56,tabla-de-admisiones.ts:54}`; marca «Aporteo» en `apps/movil/lib/pantallas/pasanaku/textos_enlace_del_grupo.dart:216,220,228,310,313` y `dominio/enlace_del_grupo.dart:27`
- Evidencia: `grep -rn 'Aporteo' apps/movil/lib | wc -l` → 8.
- Cómo se comprobó: muestra verificada (marca); el resto lo informó el subagente de frontend con ruta y línea. `scripts/verificar_frontend.py backoffice` da «sin literal de diseño» (no detecta `font-size` en rem dentro de `styles`).
- Impacto: estados vacíos que no orientan (regla 95.2.2), marca inconsistente frente al participante.
- Causa de fondo: pantallas escritas por agentes distintos sin catálogo de textos común.
- Solución de fondo: textos al catálogo, tokens tipográficos, vacíos con explicación.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: WCAG 2.2 (información y relaciones); reglas 95.1.5, 95.2.2.
- Esfuerzo estimado: chico


#### Clase OBSERV

### grupos-organizador-OBSERV-001 — El alias (nombre de una persona) viaja en la URL y entra a todas las líneas de log de la petición
- Clase / Severidad / Confianza: OBSERV / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasAOtrosServicios.java:60-65`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:1212-1214` (`/grupos/participantes/alias/{alias}`)
- Evidencia: `Traza.marcarCasoDeUso("CU-12", alias);`
- Cómo se comprobó: lectura; el alias es el nombre visible (DINERO-002). `marcarCasoDeUso` pone el segundo argumento en el contexto de traza del log (informado por el subagente en `plataforma/.../Traza.java:66-68`; no se abrió el enmascarador de PII, ver Límites).
- Impacto: nombres de personas en logs de acceso del gateway, de cada salto y del servicio, ligados a una transferencia.
- Causa de fondo: identificador de dinero que es dato personal, pasado en el path.
- Solución de fondo: la resolución va en el cuerpo de un `POST` y la traza registra solo un identificador opaco.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: gateway, `nucleo-financiero`.
- Estándares y normas: ISO/IEC 25010 seguridad (confidencialidad); CWE-532; reglas 90.2.1, 90.2.2, 98.5.2.
- Esfuerzo estimado: chico


#### Clase DOC

### grupos-organizador-DOC-001 — Tres superficies dicen tres cosas distintas sobre quién decide la admisión, y ninguna coincide con R-NUEVA-1
- Clase / Severidad / Confianza: DOC / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/textos.dart:35-39`; `apps/web/src/app/paginas/invitacion/invitacion.ts:53`; `apps/backoffice/src/app/rutas/admisiones/textos.ts:4-5`
- Evidencia: app «Decide el organizador»; web «Una persona del equipo de AportaYa revisa tu solicitud y te avisa.»; backoffice «El administrador propone y el algoritmo solo recomienda: la decisión final es de backoffice.»
- Cómo se comprobó: `grep` de cada texto en su archivo.
- Impacto: el participante recibe promesas contradictorias sobre el mismo trámite; con R-NUEVA-1 las tres quedan obsoletas.
- Causa de fondo: la decisión de admisión (A-1) nunca se tomó; cada superficie escribió su supuesto.
- Solución de fondo: después de A-1, un solo texto de producto para el trámite, en el catálogo de textos compartido.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: app, web, backoffice.
- Estándares y normas: ISO/IEC 25010 usabilidad (reconocibilidad de la adecuación); regla 95.2.
- Esfuerzo estimado: chico

### grupos-organizador-DOC-002 — El comentario del esquema `organizador` dice que liquida comisiones de los organizadores (RN-18 lo prohíbe)
- Clase / Severidad / Confianza: DOC / S3 / confirmado
- Ubicaciones: `sql/60_semillas/24-metadata-comentarios.sql:25`
- Evidencia: «Habilitación, capacitación, evaluación de reputación, liquidación de comisiones y control de conducta de los organizadores comunitarios».
- Cómo se comprobó: lectura; `grep -rniE 'comision' servicios/organizador/src/main` no muestra cálculo de comisión (no hay hallazgo DINERO por RN-18 en el código).
- Impacto: el catálogo de metadatos describe una función prohibida por la regla 91.5.1.
- Causa de fondo: texto heredado de un modelo anterior.
- Solución de fondo: corregir el comentario en su fuente.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: regla 91.5.
- Esfuerzo estimado: chico


#### Clase SEG

### grupos-organizador-SEG-001 — Pedir el retiro de un grupo no comprueba que el participante sea quien llama; el creador del grupo puede retirar a otro
- Clase / Severidad / Confianza: SEG / S1 / probable (falta ejecutar la petición con la sesión del creador contra una base con RLS)
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java:179-196`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU65Retirarse.java:48-78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/RetiroRepositorio.java:17-43`; `sql/40_reglas/restricciones.sql:1399,1409-1418,1423-1425`; `sql/10_tablas/02_grupos_turnos/solicitud_retiro.sql:7`
- Evidencia:
  ```java
  @Permiso("PARTICIPANTE")
  public ResponseEntity<SalidaRetiro> solicitarRetiro(UUID grupoId, UUID idempotencyKey, EntradaRetiro cuerpo) {
      ... cu65.solicitar(MapeoDeGrupos.entradaDeRetiro(cuerpo,
              afuera.estadoDePagos(cuerpo.getParticipanteId()), ...
  ```
  ```sql
  IF r.esq = 'grupos' AND r.t = 'participante' THEN
    cond := 'usuario_id = fn_seg_usuario_actual() OR ... OR EXISTS (SELECT 1 FROM grupos.alta_grupo ag
             WHERE ag.grupo_id=participante.grupo_id AND ag.creador_id=fn_seg_usuario_actual())';
  ```
- Cómo se comprobó: lectura. `participanteId` viene del cuerpo; ni el controlador ni `CU65Retirarse.solicitar` lo comparan con `ctx.usuarioId()`; `grupoId` e `idempotencyKey` no llegan al caso de uso. **Refutación por RLS**: la política de `grupos.participante` deja leer la fila al propio titular **y al creador del grupo**; `solicitud_retiro` no tiene `usuario_id` ni `cuenta_billetera_id`, así que el bucle de `fn_seg_aplicar_rls` (`restricciones.sql:1409-1418`) no le pone política. Un participante cualquiera recibe `AP-CU65-01` (RLS lo frena); el **creador** del grupo pasa la lectura y puede insertar el retiro de otro. El subagente de pruebas informó que `BaseDeCU65:56-81` retira con un actor ajeno y pasa.
- Impacto: el creador del grupo puede dejar una solicitud de retiro a nombre de otro integrante y conocer su posición (acreedora/deudora), que sale de su estado de pagos. Si alguien aprueba esa solicitud, saca al participante de su cupo. Para el participante: pérdida de su lugar sin haberlo pedido.
- Causa de fondo: autorización por objeto confiada a RLS, y una tabla sin columna de titular queda fuera del recorrido de RLS.
- Solución de fondo: el caso de uso deriva el participante del `ctx.usuarioId()` y del `grupoId` de la ruta (nunca del cuerpo), y usa la `Idempotency-Key`. Prueba web negativa «otro usuario» y «creador del grupo sobre otro integrante».
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `aportes`/`garantia` vía `afuera.estadoDePagos`.
- Estándares y normas: ISO/IEC 25010 seguridad (confidencialidad, autenticidad); OWASP API Security Top 10 API1 (BOLA); CWE-639; reglas 90.1.3, 96.5.2, 80.2.3.
- Esfuerzo estimado: chico

### grupos-organizador-SEG-002 — El organizador recibe el puntaje de reputación exacto del postulante
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/MotorDeRecomendacion.java:46-53`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU68Postular.java:81-90`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AdmisionRepositorio.java:66-74`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU68AceptarIngreso.java:164`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/AdmisionDelGrupo.java:102`
- Evidencia:
  ```java
  return Map.of("reputacion", Integer.toString(e.reputacion()), ...);      // entradas del motor
  Map.entry("entradas", MotorDeRecomendacion.entradas(entrada)),            // payload de ingreso_solicitado
  salida.setEvidenciaAlgoritmo(decision.evidenciaAlgoritmo());              // respuesta de proponer/historial
  ```
- Cómo se comprobó: lectura de la cadena completa. `MapeoDePostulacion.java:44,56` toma `reputacion.puntaje()` de la sesión del postulante; `AdmisionRepositorio.evidencia` lee el payload del outbox y lo devuelve.
- Impacto: el puntaje exacto, que la casa decidió que solo ve su titular (regla 90.2.8; `application.yml` de grupos, comentario de `transparencia.credencial-servicio`), le llega al organizador y viaja en un evento de Kafka.
- Causa de fondo: la evidencia del algoritmo se guarda cruda en el evento y se reutiliza como respuesta.
- Solución de fondo: guardar en la evidencia la etapa, no el puntaje, y una vista de respuesta mínima por rol.
- Relación con el flujo nuevo: APROB (la evidencia existe para que un humano decida)
- Conexiones y módulos afectados: `transparencia` (dueño del puntaje), backoffice.
- Estándares y normas: ISO/IEC 25010 seguridad (confidencialidad); OWASP API Security API3 (exposición excesiva de propiedades); reglas 90.1.8, 90.2.8.
- Esfuerzo estimado: chico

### grupos-organizador-SEG-003 — Cualquier persona con rol `ORGANIZADOR` puede programar, aprobar o dar por ejecutada la tarea automatizada de cualquier grupo
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `servicios/organizador/src/main/java/bo/aportaya/organizador/web/AutomatizacionController.java:74-117`; `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU96EjecutarTarea.java:108-131`
- Evidencia:
  ```java
  @Permiso("ORGANIZADOR")
  public ResponseEntity<SalidaProgramacion> aprobarTarea(UUID tareaId) { ... cu96.aprobar(tareaId, sesion.actual()) }
  // aprobar(): bloquea la tarea y cambia REQUIERE_APROBACION → PROGRAMADA; no mira grupo ni actor
  ```
- Cómo se comprobó: lectura; `tarea_automatizada` no tiene `usuario_id` ni `cuenta_billetera_id`, así que queda fuera de RLS (`restricciones.sql:1409-1418`). No hay control «quien aprueba ≠ quien programa».
- Impacto: hoy bajo, porque ningún planificador ejecuta tareas ni hay consumidor de `tarea_aprobada` (por eso S2 y no S1). El día que se cablee CU-96, un organizador podría liberar `EJECUTAR_ENTREGA` o `APLICAR_MORA` en el grupo de otro.
- Causa de fondo: autorización solo por rol (BFLA), sin propiedad del recurso.
- Solución de fondo: comprobar que quien opera administra el `grupoId` de la tarea, y separar funciones entre quien programa y quien aprueba. Antes, decidir A-1 sobre R-ORG-06.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `grupos` (administración del grupo).
- Estándares y normas: OWASP API Security API1 y API5; CWE-639; reglas 90.1.2, 90.1.3, 90.1.4.
- Esfuerzo estimado: chico

### grupos-organizador-SEG-004 — En el traspaso de cupo (CU-64) el nivel de KYC del entrante se reemplaza por el mínimo del grupo
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java:234-240`
- Evidencia: `politica.kycMinimo(), politica.kycMinimo(),` — el mismo valor como «KYC del entrante» y como «mínimo».
- Cómo se comprobó: lectura; el comentario dice que lo decide cumplimiento al aceptar la postulación, pero el traspaso no pasa por postulación.
- Impacto: la comprobación de debida diligencia del que entra siempre aprueba.
- Causa de fondo: se dejó un marcador en lugar de la consulta a `identidad`/`cumplimiento`.
- Solución de fondo: consultar el nivel real del entrante (ya existe `PersonasPorHttp` con `GET /usuarios/{u}/kyc`).
- Relación con el flujo nuevo: ninguna (A-1 si KYC deja de exigirse)
- Conexiones y módulos afectados: `identidad`, `cumplimiento` (L4).
- Estándares y normas: OWASP ASVS (lógica de negocio); regla 90.1.
- Esfuerzo estimado: chico

### grupos-organizador-SEG-005 — El código secreto del enlace del grupo va en el path de la URL y el token de invitación termina en un parámetro de login
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/enlace_del_grupo.dart:15-21`; `apps/movil/lib/pantallas/pasanaku/vistas_de_unirse.dart:62,69`
- Evidencia: `return '$base/g/$codigo';` y `context.go('/ingreso?volver=$retorno')`.
- Cómo se comprobó: lectura; el contrato marca el código como «no registrar» (`grupos.yaml:1546-1552`, informado por el subagente). La página web de invitación sí usa el fragmento y `replaceState` (bien).
- Impacto: el secreto queda en logs de servidor web, proxies e historial; quien lo vea puede pedir ingreso al grupo.
- Causa de fondo: el enlace web reutiliza el path para el secreto.
- Solución de fondo: secreto en el fragmento (`#`), como ya hace `apps/web`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `apps/web`.
- Estándares y normas: CWE-598; regla 90.2.2.
- Esfuerzo estimado: chico

#### Clase TRAZA

### grupos-organizador-TRAZA-001 — Las lecturas de grupos y solicitudes desde backoffice no dejan rastro de quién leyó qué
- Clase / Severidad / Confianza: TRAZA / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/ConsultarGruposDeBackoffice.java:16-20`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/BandejaDeAdmisiones.java:33-64`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeAdmision.java:57-60`
- Evidencia: «Los grupos vistos desde el backoffice … Solo lectura.»; la bandeja lee solicitudes con nombre visible, mensaje y puntaje de compatibilidad con rol de sistema.
- Cómo se comprobó: `grep -rn 'registro_acceso\|AuditoriaDeLectura\|registrarAcceso' servicios/grupos/src servicios/organizador/src` → 0 resultados.
- Impacto: un operador puede recorrer datos de participantes (nombre, cupos, estado, mensaje de la solicitud, alias) sin rastro auditable de lectura (regla 90.2.7).
- Causa de fondo: la bitácora de lectura no es transversal; cada servicio la implementa o no.
- Solución de fondo: registrar la lectura en la bitácora separada de lectura (servicio `auditoria`, L5) desde las rutas de backoffice.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `auditoria` (L5), backoffice.
- Estándares y normas: ISO/IEC 25010 seguridad (responsabilidad); regla 90.2.7.
- Esfuerzo estimado: chico

## 6. Matriz de contratos

Fuentes: `servicios/grupos/src/main/resources/openapi/grupos.yaml` (44 operaciones), `servicios/organizador/src/main/resources/openapi/organizador.yaml` (25), controladores, pactos, `clientes/dart/{grupos,organizador}` (barrel: 68 modelos = 68 esquemas; `lib/src` generado y no versionado), `packages/simulado/ejemplos/{grupos,organizador}`, `postman/` (44 = 44) y SQL. `python3 scripts/inventario_endpoints.py --check` → «endpoints relevados: 350 · hallazgos: 0». Las filas siguientes son las operaciones con discrepancia o con conexión entre servicios; el resto está alineado según ese verificador.

| Operación / evento | OpenAPI | Implementación | Pacto | Cliente / modelo de la app | Simulado | SQL | Discrepancias |
|---|---|---|---|---|---|---|---|
| `crearGrupo` POST /grupos | `grupos.yaml:1173`; `organizadorId` nulo admitido | `GruposController.java:115`; `MapeoDeAltaDeGrupo.java:50-56` exige organizador | — | Dart generado; app fija 10 cupos, día 5, BOB (ACOPLE-002) | presente | `alta_grupo`, `grupo` | `organizadorId: null` inalcanzable (COHERENCIA-002); `modalidadTurnos` ignorado (CONTRATO-002); `fondoPorPeriodo` texto y patrón con negativo (DINERO-003); AP-CU20-02 reutilizado para clave faltante (`CU20CrearGrupo.java:78`) |
| `iniciarGrupo` POST /grupos/{g}/inicio | `:321` (200/422) | `RutasDelGrupo.java:94-95`, `GRUPO_ADMINISTRAR` | evento abajo | `iniciar_grupo.dart` | agrega 202 y 503 que el contrato no tiene | `grupo`, `historial_estado_grupo` | disparo humano (APROB-002) |
| `comprometerSorteo` / `revelarSorteo` | `:364`, `:396` | `GruposController.java:164-177` | — | `detalle_del_grupo.dart:48-78` (y duplicado muerto en `cu60_sortear_turnos.dart`) | presente | `sorteo_turnos`, `snapshot_sorteo` | `Idempotency-Key` ignorada (RESILIENCIA-003); AP-CU60-03 nunca se aplica por HTTP (`SorteoDelGrupo.java:42`); app sin textos `AP-CU60-*` (HUECO-003) |
| `decidirSolicitudDeIngreso` POST …/decision | `:1097` | `RutasDeSolicitudes.java:141-150`; 422 `AP-CU68-15` si interruptor | — | app `solicitudes_del_grupo.dart:24-47`; backoffice `cu68-admision.ts:156-163` | `APROBADA` | `decision_ingreso`, `solicitud_ingreso` | cerrada en producción y usada por la app (CONTRATO-001) |
| `proponerAdmision` / `resolverAdmision` | `:143`, `:165` | `RutasDeAdmision.java:25-37` | — | backoffice sí; app no | presente | ídem | tipos a mano en backoffice sin `versionMotor`, `recomendacionAlgoritmo`, `apartamiento` (informe del lote 4) |
| `listarBandejaDeAdmisiones` GET /grupos/solicitudes | `:60` | `RutasDeSolicitudes.java:106` | — | backoffice filtra `ESCALADA` | — | `solicitud_ingreso` (`APROBADA`) | `ACEPTADA` vs `APROBADA` (FRAGIL-002); `ESCALADA` inexistente |
| `aceptarInvitacionPorEnlace` / `canjearInvitacion` | `:864`, `:22` | `InvitacionesWeb.java:127-133`; `RutasDeInvitacion.java:95` | — | app usa `aceptar` (Dio crudo, `Map`); no usa `canje` | presente | `invitacion`, `participante` | CONTRATO-001 |
| `solicitarRetiro` POST /grupos/{g}/retiros | `:549` | `GruposController.java:179-196` | — | fuera de lote | presente | `solicitud_retiro` (sin RLS) | `participanteId` del cuerpo (SEG-001); `grupoId` y clave ignorados; `aceptaPlanDePago` ignorado; `AP-CU65-03` sin código |
| `proponerAcuerdo` POST /acuerdos | `:464`, enum `:1409` | `AcuerdosController.java:49`; `TipoDeAcuerdo.java:32-39` | — | — | presente | `acuerdo` (`ABIERTO`) | `REPETIR_SORTEO` → excepción (CONTRATO-002) |
| `resolverAlias` GET /grupos/participantes/alias/{alias} | `:1214` | `RutasAOtrosServicios.java:60-65` | — | consumidor `nucleo-financiero` `HechosPorHttp:50` | — | `participante.alias` sin `UNIQUE` | DINERO-002; dato personal en path (OBSERV-001) |
| `consultarActividad` GET /grupos/participantes/{u}/actividad | `:1238` | `RutasAOtrosServicios.java:68-72`; `ConsultasRepositorio.java:46-58` | — | consumidor billetera | — | — | estados inexistentes (DEFECTO-002); sin comprobación de que `u` sea quien llama (probable IDOR o 0 por RLS, no ejecutado) |
| `consultarSiCompartenGrupo` | `:1033` (solo 200) | `RutasDelGrupo.java:81-87` | `identidad-grupos-pertenencia.json`, `transparencia-grupos-pertenencia.json` | presente | `adverso` = `ok` | `participante` | sin prueba del lado productor (PRUEBAS-003); 403/404 no documentados |
| evento `aportaya.grupos.grupo_iniciado` | — | `CU69IniciarGrupo.java:160-207`, outbox | `grupos-aportes-grupo-iniciado.json` | consumidor `aportes` idempotente | — | `periodo`, `cupo` | no se emite al conformar por firmas (FLUJO-QR-001); `montoAporte` ignora `cupo.fraccion` (hoy siempre 1); pacto dice «PAGADO», R-GRP-20 y ADR-054 dicen «ACREDITADO» |
| evento `aportaya.entregas.derecho_cedido` (consumido) | — | `ConsumidorDeCesionesKafka.java` | — | — | — | `cupo` | sin DLQ (RESILIENCIA-002) |
| evento `aportaya.organizador.habilitacion_revocada` (consumido) | — | `ConsumidorDeRevocacionesKafka.java`, apagado | — | — | — | `sustitucion_administrador` | consumidor apagado por omisión; la sustitución automática llama a `organizador` sin token desde el hilo de Kafka (informe del lote 1, probable) |
| `aprobarPostulacion` (heredada) / `resolverHabilitacion` | `organizador.yaml:266`, `:217` | `OrganizadoresController.java:116`; `RutasDeHabilitacion.java:112` | — | backoffice `cu90-postulaciones.ts:58-71` | presente | `decision_habilitacion` | dos caminos (REESCRITURA-001) |
| `sancionarOrganizador`, `apelarSancion`, `resolverApelacion` | `organizador.yaml` | `OrganizadoresController.java:213-264` | — | — | presente | `sancion_organizador`, `apelacion_sancion_org` | sin `Idempotency-Key` (RESILIENCIA-001); códigos 200/201 distintos del contrato en `evaluarDesempeno`, `apelarSancion`, `anotarEjecucion`, `emitirContrato`, `postular` (informe del lote 2; `AutomatizacionControllerWebTest:227` fija 201) |
| `evaluarDesempeno` | `organizador.yaml` | `CU92EvaluarDesempeno.java:106-117` | — | — | — | `organizador` | `limiteDeMonto` sin moneda; esquema `Dinero` definido y no usado (DINERO-001) |
| operaciones `/automatizacion/tareas/*` | `organizador.yaml` | `AutomatizacionController.java:74-117` | — | sin pantalla | presente | `tarea_automatizada` (sin RLS) | sin propiedad (SEG-003); nadie llama en producción |


## 7. Evidencia obligatoria del módulo

| Evidencia (regla) | Aplica | Estado | Detalle |
|---|---|---|---|
| Asientos por operación de dinero (91.6.1) | Parcial: el módulo no escribe en el mayor | NO COMPROBADO | `grupos` y `organizador` no generan asientos; CU-12 por alias (DINERO-002) los genera en `nucleo-financiero` (L1) |
| Idempotencia ejercitada (91.6.2) | Sí (CU-20, CU-69, decisiones) | NO COMPROBADO | Gradle no corrió; la existencia de `UNIQUE` sobre `clave_idempotencia` se leyó en `alta_grupo`, `decision_ingreso`, `sustitucion_administrador`, `tarea_automatizada` |
| Cuadre del mayor (91.6.3) | No (el módulo no tiene mayor) | SKIP | el KPI de volumen comprometido es un `SUM` ad-hoc (`IndicadoresComercialesRepositorio.java:104-118`), anotado en DINERO-003 |
| Caso de reversa (91.6.4) | Sí (cancelación con devolución, ADR-054 D4) | FAIL por ausencia | no existe (HUECO-002) |
| Redondeo declarado (91.6.5) | Sí (inicio al 75 % redondeado hacia arriba) | NO COMPROBADO | `application.yml` declara `ceil(75 × 10 / 100)`; `CU69IniciarGrupo` usa `RoundingMode` (leído, no ejecutado) |
| Prueba de contrato productor/consumidor (98.8.2) | Sí | FAIL por ausencia del lado productor | PRUEBAS-003 |
| Prueba de duplicado de mensaje (98.8.3) | Sí (3 consumidores) | NO COMPROBADO | consumidores con marca `Consumidos` en la misma transacción (leído) |
| Servicio dependido apagado (98.8.4) | Sí | NO COMPROBADO | circuito y negación por omisión leídos en `ClienteConCircuito`; no se apagó nada |
| Traza con `correlationId` (98.8.5) | Sí | NO COMPROBADO | `Traza.marcarCasoDeUso` en cada ruta; sin ejecución |
| Matriz de autorización negativa (80.2.3 / 90.6.3) | Sí | NO COMPROBADO | `scripts/verificar_pruebas_web.py` → «controladores: 42 · con matriz propia: 42» (existencia, no corrida); faltan negativos de propiedad en retiro y tareas (SEG-001, SEG-003) |
| Verificación de deriva (97.2) | Sí | NO COMPROBADO | sin base ni generación jOOQ |


## 8. Cobertura por clase

| Clase | Qué se revisó y cómo | Hallazgos |
|---|---|---:|
| APROB | búsqueda `aprob|PENDIENTE_|habilit|confirmar|postulaci|decision` en código, SQL, front; lectura de admisión, inicio, sorteo, habilitación, tareas, retiro | 2 (+ inventario de 6 en 4a) |
| RESPALDO | búsqueda `respaldo|cobertura|garantia|faltante|pozo completo`; lectura de ADR-051/054 y catálogo | 1 (+ 6 en 4b) |
| FLUJO-QR | camino conformación → `grupo_iniciado` → cuota 1 → sorteo; consumidores en `aportes` | 1 (+ APROB-002, HUECO-002) |
| FLUJO-FALTANTE | búsqueda de ramas de faltante en `grupos`; estados de turno y periodo | 0 propios; cubierta por HUECO-006 y HUECO-007 (el módulo no tiene rama) |
| COHERENCIA | ADR-054 vs código, `application.yml` vs compose, textos de las tres apps | 2 (+ DOC-001) |
| DUP-INT | comparación de los cuatro caminos de admisión y de conformación | 1 (+ FLUJO-QR-001, FRAGIL-003) |
| DUP-X | `md5sum` de `ClienteConCircuito` en 5 servicios | 1 |
| REESCRITURA | caminos heredados en `organizador`; código sin referencias | 1 (+ CLEAN-001) |
| PISADA | `git log` de `servicios/grupos` (119 commits; merge `92f0ce4b` «donde chocamos gana el mejor arreglo»); dos soluciones conviviendo | 0 propios; manifestaciones en DUP-INT-001, REESCRITURA-001, COHERENCIA-001 |
| PARCHE | búsqueda de casos por id, `catch` vacíos, umbrales literales | 0: no se hallaron `catch` que traguen ni casos por id; los literales se reportan en DINERO-001 y FRAGIL-004 |
| FRAGIL | cadenas de estado y rol repetidas; interruptor por entorno | 4 |
| DEFECTO | zonas horarias, listas de estados, apelación, carreras | 5 |
| HUECO | casos de uso sin segundo paso, estados que nadie escribe, pantallas sin entrada | 8 |
| RESILIENCIA | timeouts (global en `comun-web`), circuitos, idempotencia, DLQ | 3 |
| DINERO | float/double (0 hallados: `BigDecimal` y cadenas), moneda, mayor, RN-18 (sin comisión en código) | 3 |
| DEBIDO-PROCESO | máquina de estados de sanción y apelación | 2 |
| DDD | lenguaje ubicuo (`APROBADA`/`ACEPTADA`, `EN_VOTACION`/`ABIERTO`), objeto Dinero | 0 propios; incluidos en FRAGIL-002, FRAGIL-004, DINERO-003 |
| ARQ | SQL en capa de aplicación (`grep import org.jooq`), tamaño de archivos (0 > 300 en main) | 1 |
| CLEAN | código muerto, `TODO`/`FIXME` (0 hallados) | 1 |
| ACOPLE | FK y disparador entre esquemas; constantes de negocio en la app | 2 |
| CONTRATO | `inventario_endpoints.py --check`, comparación de enums y códigos | 2 (+ CONTRATO-001 en S2) |
| DATOS | restricciones vs dominio, append-only, moneda | 2 |
| RENDIMIENTO | listados (`LIMIT 200`, `LIMIT ?`, `LIMIT 50` presentes), consultas en bucle | 0: todos los listados leídos tienen tope |
| PRUEBAS | nombres de 90 clases, ausencia de `@Disabled`, pactos | 3 |
| UX-A11Y | estados, textos, tipografía, movimiento reducido (respetado en el sorteo) | 1 |
| OBSERV | datos personales en traza y path | 1 |
| CONFIG | `application.yml` de ambos servicios; secretos (0 valores versionados; `BD_CLAVE` y similares del entorno) | 0 propios; `factor-fijo` en CLEAN-001 y parámetros sin procedencia en DINERO-001 |
| DOC | ADR, comentarios, README | 2 (+ RESPALDO-001) |
| SEG | propiedad del recurso por endpoint, exposición de datos, secretos en URL | 5 |
| TRAZA | bitácora de lectura en rutas de backoffice | 1 |
| REG | KYC del entrante (SEG-004), habilitación y sanción (DEBIDO-PROCESO-*); dossier H15 | 0 propios: riesgo regulatorio anotado en SEG-004 y DEBIDO-PROCESO-001; «norma a identificar por Cumplimiento» |
| OTRO | — | 0 |


## 9. Lo que está bien

- Dinero como `BigDecimal` y como cadena decimal en la API; ningún `float`/`double` en el módulo (lotes 1, 2 y 4).
- El sorteo exige la primera cuota **acreditada** por `aportes` y falla cerrado (`RequisitosDelSorteo.java:41-53`): es exactamente el principio de R-NUEVA-1.
- Sorteo commit-reveal con semilla sellada, snapshot congelado e inmutable (`snapshot_sorteo` append-only, disparador de inmutabilidad del compromiso).
- Outbox en todos los eventos; consumidores con marca de consumo en la misma transacción.
- Idempotencia de CU-20 con lock consultivo, `UNIQUE` y huella; `UNIQUE` sobre `clave_idempotencia` en `alta_grupo`, `decision_ingreso`, `sustitucion_administrador`, `tarea_automatizada`.
- Precondición en el `UPDATE` en abrir, iniciar y firmar (`CU69IniciarGrupo.java:219-224`).
- El código Java no consulta esquemas ajenos: todo pasa por HTTP con circuito y timeout global.
- Teléfonos en el cuerpo, no en la URL (`PersonasPorHttp`); secreto del enlace guardado solo como SHA-256 con comparación en tiempo constante.
- Separación de funciones en `organizador`: nadie se aprueba ni se sanciona a sí mismo, ni resuelve su propia sanción; la consulta a `cumplimiento` falla cerrada y tiene pruebas en tres niveles.
- Todos los índices y restricciones con nombre explícito; semillas con `ON CONFLICT`; datos de volumen rotulados como sintéticos.
- En el front: protección de doble envío, claves de idempotencia que sobreviven a la falla de red, movimiento reducido respetado en el sorteo, región viva para lectores de pantalla, token de invitación web en el fragmento con `replaceState`.
- El código de `grupos` no consulta la capacidad de respaldo antes de activar un grupo: coincide con R-NUEVA-2.


## 10. Observado fuera de alcance

- `scripts/auditar_backend.py` informa «`grupos`: `canjearInvitacion`, `revocarInvitacionDelGrupo`, `listarBandejaDeAdmisiones`, `listarGruposDeBackoffice`, `consultarGrupoDeBackoffice`, `proponerAdmision`, `resolverAdmision` declarada sin implementar» (`evidencia/auditar_backend.txt`). Es un falso positivo: las siete están implementadas en `servicios/grupos/src/main/java/bo/aportaya/grupos/web/{RutasDeInvitacion,RutasDeSolicitudes,RutasDelGrupo,RutasDeAdmision}.java`, y `scripts/inventario_endpoints.py --check` dice «0 hallazgos».
- `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/CreativaRepositorio.java:89` y `.../aplicacion/CU110AltaDeAnunciante.java:158` consultan el esquema `organizador`, una «divergencia declarada» de `auditar_backend.py`. Viola la regla 98.1.1 desde publicidad (lote L5); en este lote se registra como conexión entrante de organizador (sección 3).

- `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ResolverDestino.java:41` — confía en `grupos` para resolver el alias; ver DINERO-002 (L1 debe revisar la confirmación del destinatario en CU-12).
- `servicios/aportes/src/main/java/bo/aportaya/aportes/consumo/ConsumidorDeGrupoIniciadoKafka.java` — única vía de apertura de la cuota 1 (L1).
- `docs/auditoria-produccion/endpoints.md:5` — documento generado el 2026-10-21 con 320 endpoints; el verificador hoy releva 350 y faltan filas de `grupos` (inicio, comparte, backoffice, subasta).
- `servicios/grupos/README.md:17-33` — marca CU-60 y otros como «sin implementar» cuando existen.
- `sql/50_verificacion/prueba_humo.sql:720-800` — solo comprueba que existan los nombres de las restricciones (transversal, L5).

## 11. Límites de esta auditoría

- Gradle no corrió: `./gradlew :servicios:grupos:test` → «Received status code 429 from server: Too Many Requests» (Maven Central) y `BUILD FAILED` (`evidencia/gradle_grupos_test.txt`). Todo lo que exige compilar o ejecutar pruebas Java queda NO COMPROBADO, incluidos el barrido de tamaño, el de umbrales y el de secretos de Gradle. El tamaño de archivos se midió a mano: ningún `.java` de main supera 300 líneas (`evidencia/barrido_tamano_manual.txt`).
- `yarn typecheck`/`yarn lint` no corrieron: el repo pide `yarn@4.18.0`, el entorno tiene 1.22.22 y no hay `node_modules` (`evidencia/yarn_typecheck.txt`). Flutter no está instalado.
- No se usó base de datos ni Docker: nada se ejecutó contra TEST, producción ni entornos compartidos.
- Cobertura de lectura: en alcance 472 archivos y 64 255 líneas. Leídos completos 318 archivos (32 413 líneas); parciales 70 (17 423); 84 clases de prueba de `grupos` (14 419 líneas) no se leyeron línea a línea, solo nombres, títulos de aserción y búsquedas dirigidas. `grupos.yaml` se leyó en un 65 % aproximadamente; `sql/40_reglas/restricciones.sql` (3 526 líneas, transversal) solo en los rangos del módulo.
- La lectura se repartió en cuatro subagentes de solo lectura con lotes disjuntos (código `grupos`; `organizador` y pruebas de `grupos`; SQL, pactos y documentos; frontends). Todo hallazgo S1 y S2 se verificó abriendo el código en esta sesión. En los S3/S4 se verificó una muestra por hallazgo; los marcados «(muestra)» o «informado por el subagente» citan ubicaciones que no se reabrieron una por una.
- No se abrió el enmascarador de PII de la configuración de logs (`plataforma`); OBSERV-001 no afirma si enmascara el alias.
- Valores reales de `ADMISION_HUMANA_OBLIGATORIA` en producción: no hay acceso; se usó el valor por omisión del código y el de `docker-compose.coolify.yml`.
- `clientes/dart/*/lib/src` y `clientes/angular/{grupos,organizador}` se generan en build y no están versionados: los modelos generados no se compararon campo a campo.
- Supuesto de frontera: retiro, acuerdos, permutas y traspaso de cupo son parte de `servicios/grupos` y se auditaron en su lado de servidor; sus pantallas de la app (aportar, subasta, traspaso, permuta, retiro) quedaron para el lote que audite esos flujos, porque el alcance L3 nombra alta, admisión, inicio, sorteo y postulaciones.
- `scripts/cobertura.py` no tiene informes (requiere correr Gradle). `scripts/verificar_rutas_frontend.py` terminó con exit 1 por la lista de conocidas (PRUEBAS-001).
- Procesos en segundo plano: ninguno queda corriendo; el demonio de Gradle se detuvo con `./gradlew --stop`.

### Refutación (Fase 7)

| Hallazgo | Explicación que podría desmentirlo | Resultado |
|---|---|---|
| SEG-001 | RLS sobre `grupos.participante` | Resiste en parte: RLS frena a un participante cualquiera, no al creador del grupo; `solicitud_retiro` no tiene política. Confianza bajada a «probable» |
| DINERO-002 | Unicidad del alias en `40_reglas` o `30_indices` | Resiste: no hay índice único; la consulta corre como sistema |
| APROB-001 | El valor de TEST es `false` | Resiste como APROB; el desfase entre entornos pasó a FRAGIL-001 y bajó CONTRATO-001 de S1 a S2 |
| APROB-002 | Un trabajo programado en otro servicio | Resiste: 0 `@Scheduled` en ambos servicios; ningún consumidor de `aportes` hacia `grupos` |
| HUECO-006 | Otro servicio escribe el estado del grupo | Resiste: `grep` de `UPDATE grupos.(grupo|turno|periodo)` fuera de `grupos` → 0 |
| FLUJO-QR-001 | Otro emisor de `grupo_iniciado` | Resiste: solo `CU69IniciarGrupo` lo emite |
| DEBIDO-PROCESO-001 | Una fase previa en la base | Resiste: el `CHECK` de sanción no tiene `PROPUESTA`; el cambio de estado ocurre en la misma transacción |
| DATOS-001 | Los valores se traducen antes de escribir | Resiste: el código escribe `ABIERTO`, `APROBADA`, `PRESENTADA` según el dominio |
| COHERENCIA-002 | Otra ruta de alta sin organizador | Resiste: una sola ruta (`crearGrupo`) |
| SEG-002 | El puntaje se filtra antes de responder | Resiste: `AdmisionDelGrupo.java:102` copia la evidencia tal cual |
| CONTRATO-001 (antes S1) | El interruptor está en `false` en el entorno vivo | Aceptada: bajado a S2 (latente) |
| Revisión inversa | Clases sin hallazgo | PARCHE, RENDIMIENTO, CONFIG, REG y OTRO: se miraron (sección 8) y no dieron hallazgo propio |

## 12. Anexo de cobertura de archivos

| Ruta | Líneas | Leído completo | Nota |
|---|---:|---|---|
| `servicios/grupos/src/main/java/bo/aportaya/grupos/Aplicacion.java` | 21 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/ConfiguracionDeGrupos.java` | 28 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/AbrirInscripcionDelGrupo.java` | 100 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/AplicarCesionDeDerecho.java` | 158 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/BandejaDeAdmisiones.java` | 66 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU20CrearGrupo.java` | 262 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU59CalcularPlazo.java` | 174 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU60Sortear.java` | 289 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU62Permutar.java` | 127 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU63Acordar.java` | 198 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU64TraspasarCupo.java` | 236 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU64TraspasoPorSubasta.java` | 195 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU65Retirarse.java` | 126 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU68AceptarIngreso.java` | 281 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU68Postular.java` | 279 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69Enlace.java` | 250 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69EnlaceDeGrupo.java` | 293 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69FirmarReglamento.java` | 223 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69IniciarGrupo.java` | 268 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69Invitar.java` | 252 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CanjearInvitacion.java` | 110 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/ConsultarDetalleDelGrupo.java` | 285 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/ConsultarGruposDeBackoffice.java` | 126 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/ConsultarMisLimites.java` | 57 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/Consultas.java` | 216 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/GrupoAbierto.java` | 39 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/IndicadoresComerciales.java` | 80 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/IndicadoresDeTurnos.java` | 45 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/LecturasDelDetalle.java` | 91 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/MotorDeRecomendacion.java` | 55 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/OrganizadorDecideIngreso.java` | 232 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/PrecondicionesDePostulacion.java` | 70 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/ReglamentoAlIniciar.java` | 73 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/RequisitosDelSorteo.java` | 55 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/SustitucionAutomatica.java` | 164 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/SustituirAdministrador.java` | 140 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/TextoDelReglamento.java` | 51 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/consumo/ConsumidorDeCesionesKafka.java` | 96 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/consumo/ConsumidorDePreciosCongeladosKafka.java` | 85 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/consumo/ConsumidorDeRevocacionesKafka.java` | 72 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/AlcanceDeCalendario.java` | 21 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/CalendarioDeAportes.java` | 25 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/CalendarioVacio.java` | 23 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/ComputoDeVotacion.java` | 69 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/CondicionesDeIngreso.java` | 23 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/CriterioDeEmparejamiento.java` | 61 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/DecisionDeIngreso.java` | 32 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/EstadoDeSorteo.java` | 12 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/Etapas.java` | 88 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/FeriadoConProcedencia.java` | 73 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/GrupoNuevo.java` | 41 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/InicioDelGrupo.java` | 30 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/InvitacionAdmisible.java` | 78 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/InvitacionRegistrada.java` | 8 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/MensajeDeInvitacion.java` | 25 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/MotivoDelPuntaje.java` | 61 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/PaqueteDeSorteo.java` | 26 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/PermutaPosible.java` | 62 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/PoliticaDelGrupo.java` | 14 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/PosicionAlRetirarse.java` | 66 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/ResumenDeAcuerdo.java` | 15 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/SnapshotDeSorteo.java` | 95 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/SustitucionDeAdministrador.java` | 21 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/TipoDeAcuerdo.java` | 64 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/TraspasoAdmisible.java` | 102 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/VencimientoLocal.java` | 100 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/puertos/HechosDeOtrosServicios.java` | 164 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/puertos/PrimerAporteDelCupo.java` | 20 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AceptacionVigenteRepositorio.java` | 37 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AcuerdoRepositorio.java` | 197 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AdmisionRepositorio.java` | 201 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CalendarioDeAportesRepositorio.java` | 67 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CalendarioDelGrupoRepositorio.java` | 68 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CalendarioRepositorio.java` | 61 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/ConsultasRepositorio.java` | 262 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java` | 170 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/EmparejamientoRepositorio.java` | 175 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/IndicadoresComercialesRepositorio.java` | 119 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/IndicadoresDeTurnosRepositorio.java` | 38 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/InvitacionRepositorio.java` | 146 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/ListasDeSorteo.java` | 34 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/PermutaRepositorio.java` | 111 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/RetiroRepositorio.java` | 80 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/SorteoRepositorio.java` | 280 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/SubastaDeCupoRepositorio.java` | 73 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/SustitucionRepositorio.java` | 111 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/TraspasoRepositorio.java` | 145 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/ClienteConCircuito.java` | 141 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/ConfiguracionDeCircuito.java` | 28 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/ConfiguracionDeCredencialDeServicio.java` | 32 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/EtapasPorHttp.java` | 158 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java` | 294 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/PersonasPorHttp.java` | 63 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/PrimerAportePorHttp.java` | 48 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/TokensDeInvitacionPorHttp.java` | 144 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/AcuerdosController.java` | 155 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/AdmisionDelGrupo.java` | 113 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/EtapaDelIngreso.java` | 126 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java` | 283 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposDeBackoffice.java` | 58 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/IntegrantesDelGrupo.java` | 173 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/InvitacionesWeb.java` | 172 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/MapeoDeAltaDeGrupo.java` | 128 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/MapeoDeCalendario.java` | 37 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/MapeoDeGrupos.java` | 102 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/MapeoDePostulacion.java` | 75 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RespuestasAOtrosServicios.java` | 73 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasAOtrosServicios.java` | 84 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeAdmision.java` | 62 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeEnlaceDeGrupo.java` | 104 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeInvitacion.java` | 138 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java` | 183 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSubastaDelCupo.java` | 140 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDelGrupo.java` | 130 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/SorteoDelGrupo.java` | 97 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/java/bo/aportaya/grupos/web/TurnosController.java` | 77 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/resources/application.yml` | 130 | sí | sí — lote 1 (subagente) + verificación propia de las ubicaciones citadas |
| `servicios/grupos/src/main/resources/openapi/grupos.yaml` | 2024 | parcial | parcial (~65 % por lote 1, rangos de contrato por lote 3): se leyeron índice de rutas y esquemas usados |
| `servicios/grupos/README.md` | 102 | sí | sí |
| `servicios/grupos/build.gradle.kts` | 45 | sí | sí |
| `servicios/grupos/descriptor.yml` | 30 | sí | sí |
| `servicios/organizador/README.md` | 63 | sí | sí — lote 2 |
| `servicios/organizador/build.gradle.kts` | 44 | sí | sí — lote 2 |
| `servicios/organizador/descriptor.yml` | 30 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/Aplicacion.java` | 21 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/Backoffice.java` | 15 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90ConsultarBandeja.java` | 84 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90HabilitacionBasica.java` | 283 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90PostularOrganizador.java` | 239 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90ResolverHabilitacion.java` | 277 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU91FirmarContrato.java` | 213 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU92EvaluarDesempeno.java` | 188 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU93SancionarOrganizador.java` | 255 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU95DefinirAutomatizacion.java` | 163 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU96EjecutarTarea.java` | 215 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/ConsultarHabilitacion.java` | 80 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/ReclamosEnCurso.java` | 49 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/UsuarioDelOrganizador.java` | 32 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/AccionSensible.java` | 43 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/ClaveDeTarea.java` | 28 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/DebidoProceso.java` | 37 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/DecisionDeHabilitacion.java` | 25 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/EfectoDeReclamos.java` | 32 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/ExpedienteDeHabilitacion.java` | 17 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/NivelDeOrganizador.java` | 52 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/PuntajeDeDesempeno.java` | 77 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/dominio/RequisitosDeHabilitacion.java` | 66 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/AutomatizacionRepositorio.java` | 197 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/BitacoraDeHabilitacion.java` | 42 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/ClienteConCircuito.java` | 128 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/ConfiguracionDeCircuito.java` | 28 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/ContratoRepositorio.java` | 147 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/DesempenoRepositorio.java` | 179 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/EfectoDeReclamosPorHttp.java` | 81 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/HabilitacionRepositorio.java` | 149 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/infraestructura/OrganizadorRepositorio.java` | 282 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/web/AutomatizacionController.java` | 128 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/web/HabilitacionBasicaController.java` | 85 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/web/MapeoDeHabilitacion.java` | 58 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/web/MapeoDeOrganizador.java` | 62 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/web/OrganizadoresController.java` | 274 | sí | sí — lote 2 |
| `servicios/organizador/src/main/java/bo/aportaya/organizador/web/RutasDeHabilitacion.java` | 151 | sí | sí — lote 2 |
| `servicios/organizador/src/main/resources/application.yml` | 54 | sí | sí — lote 2 |
| `servicios/organizador/src/main/resources/openapi/organizador.yaml` | 1079 | sí | sí — lote 2 |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU93ReclamoEnCursoTest.java` | 167 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU90HabilitacionBasicaTest.java` | 193 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU91Test.java` | 238 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/BarridoTest.java` | 28 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU96RechazosTest.java` | 181 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU90Test.java` | 282 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU95Test.java` | 232 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/FixturaDeOrganizador.java` | 215 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU93Test.java` | 259 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU92Test.java` | 275 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/ArquitecturaTest.java` | 60 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/ArranqueTest.java` | 64 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU95RechazosTest.java` | 134 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/BaseDeOrganizador.java` | 131 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU93RechazosTest.java` | 165 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU90ResolucionTest.java` | 211 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/BaseDeResolucion.java` | 86 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU90RevisionYBandejaTest.java` | 176 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU91RechazosTest.java` | 149 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU96Test.java` | 262 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU90RechazosTest.java` | 174 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/CU92RechazosTest.java` | 175 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/infraestructura/EfectoDeReclamosPorHttpTest.java` | 186 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/web/SeguridadWebTest.java` | 64 | sí | sí |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/web/OrganizadorSancionesWebTest.java` | 228 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/web/OrganizadoresControllerWebTest.java` | 295 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/web/HabilitacionBandejaWebTest.java` | 221 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/organizador/src/test/java/bo/aportaya/organizador/web/AutomatizacionControllerWebTest.java` | 257 | parcial | parcial — nombres, aserciones y secciones clave |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69IniciarGrupoConcurrenciaTest.java` | 67 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69AceptacionDelReglamentoTest.java` | 111 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68AceptarIngresoTest.java` | 261 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69LimiteDeParticipacionesTest.java` | 65 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/GruposDeBackofficeRepositorioTest.java` | 119 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU62.java` | 136 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/DoblesDeTarifasYNucleo.java` | 111 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69CanjeTest.java` | 242 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU64.java` | 172 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU63Test.java` | 219 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeInicio.java` | 105 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BarridoTest.java` | 28 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU20.java` | 189 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BandejaDeAdmisionesRepositorioTest.java` | 159 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/ArranqueCalendarioDeAportesTest.java` | 179 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69ReglamentoAlIniciarTest.java` | 126 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU20Test.java` | 293 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU60SnapshotTest.java` | 275 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/FixturaDeGrupos.java` | 249 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CUIndicadoresDeTurnosTest.java` | 110 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CUIndicadoresComercialesTest.java` | 139 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU68.java` | 232 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/TipoDeAcuerdoTest.java` | 36 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CUCesionDeDerechoTest.java` | 260 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU59.java` | 121 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69FirmaSinCupoTest.java` | 187 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68SustitucionTest.java` | 201 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU69.java` | 171 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69EtapaContraTransparenciaTest.java` | 152 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU60.java` | 153 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU64TitularYAcuerdoTest.java` | 244 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU20EtapaMinimaTest.java` | 189 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69AceptacionIdempotenteTest.java` | 115 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69RevocacionTest.java` | 199 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69RolRealTest.java` | 54 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69EnlaceDeGrupoTest.java` | 239 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68AdmisionConcurrenteTest.java` | 117 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69ReciboDeIdentidadTest.java` | 52 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69IniciarGrupoTest.java` | 182 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/ArquitecturaTest.java` | 60 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68AdmisionTest.java` | 268 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/ArranqueTest.java` | 72 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69Test.java` | 283 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69FirmaYSorteoTest.java` | 178 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69EventoDeInicioTest.java` | 116 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU62Test.java` | 214 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68SustitucionAutomaticaTest.java` | 239 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CalendarioDeAportesRepositorioTest.java` | 130 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU63.java` | 127 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU64Test.java` | 177 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU59VencimientoTest.java` | 169 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68MotorTest.java` | 240 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU60Test.java` | 258 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU20AperturaTest.java` | 145 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU60RequisitosTest.java` | 133 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU59Test.java` | 191 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU64SubastaTest.java` | 247 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU65Test.java` | 154 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU68Test.java` | 261 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU65.java` | 133 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/CU69IngresoCerradoTest.java` | 293 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/PertenenciaCompartidaRepositorioTest.java` | 100 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeReglamentoAlIniciar.java` | 137 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/dominio/EtapasYInicioTest.java` | 123 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/dominio/VencimientoLocalTest.java` | 132 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/dominio/FeriadoConProcedenciaTest.java` | 140 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/HechosSupresionPorCuerpoTest.java` | 106 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/HechosConsumoDeInvitacionTest.java` | 89 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/HechosEmisionDeInvitacionTest.java` | 178 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/PrimerAportePorHttpTest.java` | 140 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/HechosCaidaTest.java` | 246 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttpTest.java` | 195 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/HechosDeEnsayo.java` | 139 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/EtapasPorHttpInvalidoTest.java` | 90 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/EtapasPorHttpTest.java` | 111 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/infraestructura/clientes/BaseDeEtapasPorHttp.java` | 152 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposCalendarioDeAportesWebTest.java` | 212 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/BaseDeEtapaMinimaWeb.java` | 125 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/EtapaMinimaDecisionYTraspasoWebTest.java` | 184 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposControllerWebTest.java` | 294 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/EtapaMinimaWebTest.java` | 195 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposInvitacionesWebTest.java` | 240 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/SorteoDelGrupoTest.java` | 183 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/EtapaMinimaAdmisionTest.java` | 181 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/CU69CanjeOrdenWebTest.java` | 196 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposCalendarioWebTest.java` | 217 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposSolicitudesWebTest.java` | 238 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposEnlaceDelGrupoWebTest.java` | 176 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/SeguridadWebTest.java` | 93 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposPostulacionWebTest.java` | 176 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/AcuerdosControllerWebTest.java` | 222 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposSubastaWebTest.java` | 232 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/BaseDeDetalleYFirmaWeb.java` | 63 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposBandejaWebTest.java` | 209 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/AcuerdosListadoWebTest.java` | 109 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposDeBackofficeWebTest.java` | 233 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/InvitacionesWebTest.java` | 163 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/AcuerdoDeTraspasoWebTest.java` | 136 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposDetalleYFirmaWebTest.java` | 222 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/TurnosControllerWebTest.java` | 200 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposHistorialAdmisionWebTest.java` | 160 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/AdmisionDelGrupoSustitucionTest.java` | 97 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposInicioYEtapaWebTest.java` | 269 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/EtapasDePrueba.java` | 47 | parcial | parcial — helper leído total o parcialmente |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposAdmisionHumanaWebTest.java` | 171 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/InvitacionesAdmisionHumanaWebTest.java` | 96 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposFirmaYPerfilWebTest.java` | 161 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/consumo/ConsumidorDePreciosCongeladosKafkaTest.java` | 76 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/java/bo/aportaya/grupos/consumo/ConsumidorDeCesionesKafkaTest.java` | 97 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `servicios/grupos/src/test/resources/fixtures/calendario-de-aportes.json` | 21 | no | no línea a línea — nombres de prueba, títulos de aserción y búsquedas dirigidas |
| `sql/10_tablas/02_grupos_turnos/aceptacion_reglamento.sql` | 20 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/acuerdo.sql` | 35 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/alta_grupo.sql` | 20 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/configuracion_grupo.sql` | 29 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/criterio_emparejamiento.sql` | 19 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/cupo.sql` | 27 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/decision_ingreso.sql` | 31 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/dia_no_habil.sql` | 19 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/enlace_grupo.sql` | 24 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/grupo.sql` | 56 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/historial_estado_grupo.sql` | 19 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/invitacion.sql` | 40 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/participante.sql` | 31 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/periodo.sql` | 26 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/postulacion_emparejamiento.sql` | 27 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/propuesta_grupo.sql` | 24 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/propuesta_postulacion.sql` | 15 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/reglamento_grupo.sql` | 25 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/snapshot_sorteo.sql` | 22 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/solicitud_ingreso.sql` | 36 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/solicitud_permuta.sql` | 29 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/solicitud_retiro.sql` | 25 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/sorteo_turnos.sql` | 32 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/sustitucion_administrador.sql` | 22 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/traspaso_cupo.sql` | 27 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/turno.sql` | 29 | sí | sí — lote 3 |
| `sql/10_tablas/02_grupos_turnos/voto_participante.sql` | 22 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/apelacion_sancion_org.sql` | 24 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/capacitacion_organizador.sql` | 19 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/contrato_organizador.sql` | 29 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/decision_habilitacion.sql` | 26 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/ejecucion_tarea.sql` | 23 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/evaluacion_desempeno.sql` | 32 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/metrica_organizador.sql` | 18 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/organizador.sql` | 32 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/regla_automatizacion.sql` | 25 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/requisito_habilitacion.sql` | 23 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/sancion_organizador.sql` | 28 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/solicitud_organizador.sql` | 28 | sí | sí — lote 3 |
| `sql/10_tablas/07_organizador_automatizacion/tarea_automatizada.sql` | 24 | sí | sí — lote 3 |
| `sql/20_claves/02_grupos_turnos.sql` | 270 | sí | sí — lote 3 |
| `sql/20_claves/07_organizador_automatizacion.sql` | 95 | sí | sí — lote 3 |
| `sql/30_indices/02_grupos_turnos.sql` | 128 | sí | sí — lote 3 |
| `sql/30_indices/07_organizador_automatizacion.sql` | 59 | sí | sí — lote 3 |
| `sql/60_semillas/17-organizador-y-emparejamiento.sql` | 41 | sí | sí — lote 3 |
| `sql/61_dev/03-grupo-demo.sql` | 208 | sí | sí — lote 3 |
| `sql/40_reglas/restricciones.sql` | 3526 | parcial | parcial — rangos 1340-1505, 1840-2070, 2620-2810 + búsquedas (archivo transversal) |
| `sql/35_append_only/append_only.sql` | 244 | parcial | parcial — solo lo que toca tablas del módulo |
| `sql/50_verificacion/prueba_humo.sql` | 824 | parcial | parcial — solo lo que toca tablas del módulo |
| `sql/62_volumen/volumen-03.sql` | 1413 | parcial | parcial — solo lo que toca tablas del módulo |
| `contratos/pactos/grupos-aportes-grupo-iniciado.json` | 51 | sí | sí — lote 3 |
| `contratos/pactos/identidad-grupos-pertenencia.json` | 46 | sí | sí — lote 3 |
| `contratos/pactos/transparencia-grupos-pertenencia.json` | 46 | sí | sí — lote 3 |
| `apps/backoffice/src/app/rutas/admisiones/admisiones.routes.ts` | 21 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/dominio/cu68-admision.ts` | 167 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/entrada-de-admision.spec.ts` | 174 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/entrada-de-admision.ts` | 176 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/formulario-de-decision-directa.ts` | 134 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/formulario-de-resolucion-vista.spec.ts` | 71 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/formulario-de-resolucion-vista.ts` | 114 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/formulario-de-resolucion.spec.ts` | 194 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/formulario-de-resolucion.ts` | 149 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/historial-de-admision.ts` | 83 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/pantalla-de-admision.a11y.spec.ts` | 55 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/pantalla-de-admision.spec.ts` | 199 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/pantalla-de-admision.ts` | 126 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/solicitudes-del-grupo.ts` | 96 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/tabla-de-admisiones.ts` | 108 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/admisiones/textos.ts` | 97 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/grupos/detalle-de-grupo.ts` | 90 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/grupos/dominio/consulta-de-grupos.ts` | 44 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/grupos/grupos.routes.ts` | 10 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/grupos/grupos.spec.ts` | 154 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/grupos/listado-de-grupos.ts` | 149 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/grupos/textos.ts` | 52 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/bandeja-de-postulaciones.spec.ts` | 106 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/bandeja-de-postulaciones.ts` | 144 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/dominio/cu90-postulaciones.ts` | 97 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/formulario-de-habilitacion.ts` | 128 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/pantalla-de-postulacion.spec.ts` | 158 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/pantalla-de-postulacion.ts` | 128 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/postulaciones.routes.ts` | 19 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/postulaciones/textos.ts` | 58 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.a11y.spec.ts` | 23 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.spec.ts` | 52 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.ts` | 74 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/cumplimiento/organizadores/pantalla-de-habilitacion.a11y.spec.ts` | 38 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/cumplimiento/organizadores/pantalla-de-habilitacion.spec.ts` | 104 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/backoffice/src/app/rutas/cumplimiento/organizadores/pantalla-de-habilitacion.ts` | 96 | sí | sí — lote 4 (y propia para solicitudes-escaladas) |
| `apps/web/src/app/paginas/publico-grupos/grupo-transparencia.ts` | 38 | sí | sí — lote 4 |
| `apps/web/src/app/paginas/publico-sorteos/sorteo-verificacion.ts` | 38 | sí | sí — lote 4 |
| `apps/web/src/app/paginas/invitacion/invitacion.spec.ts` | 71 | sí | sí — lote 4 |
| `apps/web/src/app/paginas/invitacion/invitacion.ts` | 111 | sí | sí — lote 4 |
| `apps/web/src/app/paginas/invitacion/token-de-invitacion.spec.ts` | 47 | sí | sí — lote 4 |
| `apps/web/src/app/paginas/invitacion/token-de-invitacion.ts` | 61 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/selector_de_etapa_minima.dart` | 112 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_verificar_sorteo.dart` | 117 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pintor_de_ruleta.dart` | 157 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/organismos_crear_grupo.dart` | 183 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_detalle_miembro.dart` | 178 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/secciones_de_mis_grupos.dart` | 84 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_crear_grupo.dart` | 200 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_mi_puntaje.dart` | 160 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_unirse_a_un_grupo.dart` | 176 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart` | 109 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_transparencia.dart` | 109 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_escanear.dart` | 192 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/botones_de_compartir.dart` | 80 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/acceso_al_traspaso.dart` | 63 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/rechazo_al_crear.dart` | 29 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/compartir_invitacion.dart` | 43 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_sorteo_vista.dart` | 115 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/tarjeta_de_aporte.dart` | 150 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/vistas_de_unirse.dart` | 151 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_pedir_cupo.dart` | 152 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/abrir_mi_grupo.dart` | 29 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_del_grupo.dart` | 47 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/inicio_del_grupo.dart` | 162 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_subasta.dart` | 77 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_traspaso.dart` | 176 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/textos_de_conversion.dart` | 44 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_traspaso_envio.dart` | 69 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/grupo_creado.dart` | 101 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_unirse.dart` | 191 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/seccion_del_grupo.dart` | 170 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_subastas.dart` | 67 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/tarjeta_del_monto_del_periodo.dart` | 45 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_aportar.dart` | 170 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/seccion_de_confianza.dart` | 145 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/marco_de_escaneo.dart` | 149 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_permuta.dart` | 134 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/rutas.dart` | 153 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/contrato_de_organizador.dart` | 146 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/textos_perfil_del_integrante.dart` | 53 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/errores_de_etapa_e_inicio.dart` | 40 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/textos_detalle_del_grupo.dart` | 63 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/acceso_a_la_subasta.dart` | 63 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/formulario_de_subasta.dart` | 135 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/textos_de_tramites_del_grupo.dart` | 27 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/etapa_del_integrante.dart` | 63 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_sorteo_orden.dart` | 29 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/opciones_de_grupo.dart` | 25 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_mis_grupos.dart` | 62 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/errores_de_la_subasta.dart` | 37 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/avatar_del_integrante.dart` | 31 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/orden_de_cobro.dart` | 81 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/acciones_de_grupo.dart` | 84 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/seccion_de_contacto.dart` | 107 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/aviso_en_linea.dart` | 40 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/tarjetas_de_unirse.dart` | 99 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/textos_de_refirma_y_etapa.dart` | 22 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/invitacion_creada.dart` | 182 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/textos.dart` | 187 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/tarjeta_de_integrante.dart` | 154 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_unirse_al_grupo.dart` | 180 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_firmar_reglamento.dart` | 154 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_mi_estado.dart` | 138 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/errores_del_traspaso.dart` | 71 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_solicitudes_del_grupo.dart` | 199 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/condiciones_del_traspaso.dart` | 168 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/grilla_de_opciones.dart` | 57 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/comprobante_del_traspaso.dart` | 125 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/formulario_de_puja.dart` | 111 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/vista_enlace_del_grupo.dart` | 169 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_reclamo_pendiente.dart` | 62 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/aviso_de_revision_en_grupos.dart` | 72 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pedido_enviado.dart` | 72 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_subastar_cupo.dart` | 102 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/accion_del_traspaso.dart` | 101 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_enlace_del_grupo.dart` | 181 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/anuncio_del_sorteo.dart` | 95 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/tarjeta_de_subasta.dart` | 105 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/textos_de_la_subasta.dart` | 69 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/no_habilitado.dart` | 40 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/marco_de_ruleta.dart` | 133 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/textos_de_etapa_e_inicio.dart` | 55 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/tarjeta_de_cesion.dart` | 167 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/pantalla_retiro.dart` | 155 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/textos_del_traspaso.dart` | 103 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/integrantes_del_grupo.dart` | 39 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/orquesta_del_sorteo.dart` | 170 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_turno.dart` | 116 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/aporte_registrado.dart` | 47 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/ruleta_del_sorteo.dart` | 181 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/tarjeta_de_mi_grupo.dart` | 126 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pasos_del_traspaso.dart` | 162 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/textos_de_verificacion_del_grupo.dart` | 37 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/pantalla_sorteo.dart` | 160 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/pantalla_invitar.dart` | 155 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/aviso_de_refirma.dart` | 46 | parcial | parcial — rangos o búsqueda de patrones (lote 4) |
| `apps/movil/lib/pantallas/pasanaku/textos_invitacion.dart` | 76 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/textos_enlace_del_grupo.dart` | 115 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/errores_de_firma.dart` | 32 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/votacion_del_traspaso.dart` | 108 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu61_verificar_sorteo.dart` | 21 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/estado_de_mora.dart` | 78 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu20_crear_grupo.dart` | 98 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/enlace_del_grupo.dart` | 157 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/solicitudes_del_grupo.dart` | 48 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/iniciar_grupo.dart` | 41 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu60_sortear_turnos.dart` | 107 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu69_unirse_por_enlace.dart` | 67 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/retirar_subasta.dart` | 37 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/mis_limites.dart` | 33 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/habilitacion_basica.dart` | 109 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart` | 146 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu64_traspasar_cupo.dart` | 67 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu68_postular_al_grupo.dart` | 61 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu73_verificar_transparencia.dart` | 21 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/detalle_del_grupo.dart` | 78 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/perfil_del_integrante.dart` | 54 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/consultar_puntaje.dart` | 20 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/etapa_minima.dart` | 70 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/mis_grupos.dart` | 60 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu65_solicitar_retiro.dart` | 51 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu62_solicitar_permuta.dart` | 50 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu69_invitar_al_grupo.dart` | 53 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/enlace_para_compartir.dart` | 82 | sí | sí — lote 4 |
| `apps/movil/lib/pantallas/pasanaku/dominio/cu21_cobrar_aporte.dart` | 55 | no | fuera de alcance L3 (aportes/subasta/traspaso/permuta/retiro/mora/perfil): no leído |
