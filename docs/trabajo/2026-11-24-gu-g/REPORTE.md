# Reporte — Lote G · CU de gobierno de participantes (borrador para aprobación)

> **AVANCE: ver `plan_status.py` (H1, H2 y H3.S1 hechos).** H3 (implementación) en curso: modelo, esquema, semillas y contrato escritos y verificados en local; backend BLOQUEADO por Maven Central 429. Antes: H1 cerrado (PR #514 y #518 en `dev`). H2: CU-134 pasa a APROBADO (decisión de Pablo del 2026-11-24); falta la CI del PR. D-3 (5/5 días hábiles, 12 meses, 90 días) y D-11 (retener la entrega) resueltas por Pablo el mismo día; abiertas A-1 y A-2.

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-g-cu-participantes` → PR a `dev`
- Peldaño de evidencia alcanzado: `RUNS` sobre la bóveda, en local y en la CI del PR #514 (los verificadores corren en exit 0 con el CU nuevo). Es un documento: no hay comportamiento que ejercitar; el CU queda **BORRADOR** hasta que Pablo lo apruebe (decisión P-11).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Relevamiento de lo existente con rutas (tabla «Hallazgos») | `grep -n "ck_usuario_estado\|ck_bloqueo_cuenta_motivo\|ck_restriccion_usuario_tipo" sql/10_tablas/01_identidad_usuarios/*.sql` | PASS — [evidencia/H1.S1.M1-checks.txt](./evidencia/H1.S1.M1-checks.txt) |
| H1.S2.M1 | `docs/CasosDeUso/CU-134 Directorio y ficha del participante, y suspensión o bloqueo de su cuenta por seguridad con debido proceso.md`, con las 13 secciones, 11 códigos `AP-CU134-01..11`, 17 alternativos, 12 escenarios gherkin, sección de datos sensibles y 15 decisiones requeridas | `python3 scripts/verificar_boveda.py` | PASS `TODO OK` — [evidencia/verificar_boveda.txt](./evidencia/verificar_boveda.txt) |
| H1.S2.M2 | Fila en `_CasosDeUso.md` (Identidad), `total_casos: 113`, nota del rango; «Ver también» recíproco en CU-07, 17, 27, 44, 52, 55, 93 | `python3 scripts/verificar_boveda.py` | PASS (`"Ver también" recíproco` OK) |
| H1.S2.M3 | Cifras «112 casos de uso» → 113 en 6 archivos de `planes/` | `python3 scripts/verificar_boveda.py` | PASS (`ninguna cifra desfasada`) |
| H1.S3.M1 | Verificadores de los workflows en local | ver «Evidencia» | PASS: seguridad, bóveda, carriles, criterios, inventario de endpoints, contratos y límites, maqueta, flujos, pruebas web, catálogo de datos → exit 0 |
| H1.S3.M2 | Este reporte con las preguntas para Pablo | `python3 .claude/hooks/report_gate.py --self-test` | `14 PASS, 0 FAIL` |
| H1.S3.M3 | PR #514 a `dev` con la CI en verde; mergeado el 2026-11-24 | checks del head `af1db1d` (runs 45459193470 y 45459192974) | PASS — ver «CI del PR #514» |

## A medias
### H2.S1.M3 — PR de aprobación a `dev`
- Qué anda: CU-134 en APROBADO con «Decisiones aprobadas»; `verificar_boveda.py` → `TODO OK`; catálogo de datos sin diferencias.
- Qué no anda: la CI del PR todavía no corrió.
- Qué falta exactamente: abrir el PR y ver los checks en verde.
- Dónde quedó: rama `claude/gu-g-cu-participantes`, actualizada con `dev`.

### H3 — Implementación de CU-134 (en curso) — ESTADO PARA RETOMAR
- **Qué anda (verificado en local):**
  - H3.S1, modelo y esquema: 5 tablas nuevas, CHECK ampliados, R-SEG-13..15, permisos `PARTICIPANTES_*` y la semilla de `politica_medida_cuenta` (5/5 días hábiles, 12 meses, 90 días). Aplicado dos veces sobre Postgres 16; restricciones probadas en tres niveles; semillas idempotentes (`evidencia/H3.S1-*.txt`).
  - Prueba de humo: 166 OK, 0 FALLA (`evidencia/H3.S1-prueba-humo.txt`).
  - Verificadores y deriva en 0 tras integrar `dev` con #516 (`evidencia/H3-verificadores-tras-merge.txt`).
  - H3.S3, átomos puros: `CausalDeMedida`, `PlazosDeMedida` y `Enmascarado`. Compilan con `javac -Xlint:all -Werror` y su ejercicio en tres niveles pasa (`evidencia/H3.S3-atomos.txt`). Están formateados según Spotless.
- **Escrito, sin compilar en local (lo verifica la CI):** contrato aplicado; `ParticipantesRepositorio`, `MedidasDeCuentaRepositorio`, `CU134ConsultarParticipante`, `CU134GestionarMedida` (incluido el trabajo diario `vencer`), `ParticipantesController` y `CU134Test` (12 criterios y 3 rechazos). `verificar_criterios.py`, `inventario_endpoints.py --check` y `verificar_contratos_limites.py` dan 0.
- **Qué no existe todavía:** el disparador programado del trabajo `vencer`, el consumidor de acuses de `notificaciones` (hoy registra la notificación quien tiene `PARTICIPANTES_MEDIDA_PROPONER`), la retención de la entrega en `entregas` (H3.S4), el modo restringido del suspendido (H3.S5) y el backoffice (H3.S6). La ficha no muestra `gruposActivos` ni el bloqueo de autoridad, que viven en otros servicios (regla 98.1). RECLAMO_EN_CURSO (AP-CU134-11) no se verifica: los reclamos son de `cumplimiento`.
- **Qué falta exactamente (próximo paso concreto):**
  1. Aplicar el contrato: `git apply docs/trabajo/2026-11-24-gu-g/evidencia/H3.S2-contrato-participantes.patch`. Las rutas son `/identidad/participantes/...` y `/identidad/medidas-cuenta/...`; los errores `AP-CU134-01..11` ya están en comentarios.
  2. Escribir en `servicios/identidad` los repositorios `ParticipantesRepositorio` y `MedidasDeCuentaRepositorio`, con tablas jOOQ `Tables.EXPEDIENTE_MEDIDA_CUENTA` etc. y `catalogo.dia_no_habil` para el calendario (patrón `aportes/infraestructura/FeriadosRepositorio`). La escritura en `comun.registro_acceso_datos` sigue el patrón de `aportes/infraestructura/AuditoriaRepositorio`.
  3. Escribir los organismos `CU134ConsultarParticipante`, `CU134ProponerMedida`, `CU134DecidirMedida` y `CU134ResolverApelacion`, más notificación, descargo y apelación, con el patrón de `CU08AsignarRol` (`datos.conContexto`, `ErrorDeNegocio(CodigoError.de(134, n))`, `outbox.emitir`).
  4. `ParticipantesController implements ParticipantesApi` con `@Permiso("PARTICIPANTES_*")`.
  5. Escribir `CU134Test`: un `@DisplayName` por escenario gherkin del CU y la cita de cada R-XXX del CU (lo exige `verificar_criterios.py`). Base con `BaseDePrueba.contenedor()`, como `BaseDeCU08`.
  6. La segunda instancia y el segundo factor reciente: verificar la evidencia de step-up de `CU04StepUp` (hoy solo la verifica `nucleo-financiero`, con `SegundoFactorLocal`).
- **PR #525 (borrador), CI en `abef59c92`:** los verificadores Python de seguridad y pruebas web quedaron en verde tras dos arreglos. Falla «Formato, reglas propias y compilación» porque `spotlessCheck` encuentra formato Palantir distinto en `CU134ConsultarParticipante`, `CU134GestionarMedida`, `MedidasDeCuentaRepositorio`, `ParticipantesRepositorio`, `ParticipantesController`, `CU134Test` y `ParticipantesControllerWebTest`. Destraba esto correr `./gradlew :servicios:identidad:spotlessApply` en una máquina con Gradle funcionando y pushear el resultado. Desde esta sesión: el espejo de Maven y el formateador nativo de Palantir (repo1.maven.org) fueron denegados por el control de permisos. La CI todavía no llegó a compilar ni a correr `CU134Test`.
- **Bloqueo:** `./gradlew` no resuelve dependencias en este entorno (Maven Central 429; espejo denegado por permisos, `evidencia/H3.S3-bloqueo-maven-429.txt`). La CI sí compila: verificar con la CI.
- **Dónde quedó:** rama `claude/gu-g-cu-participantes`, todo pusheado, PR #519.

## Continuación del 2026-11-25

En `codex/cu134-medidas-participantes`, desde #525 (`6f42f97ce`), están las decisiones A-1 y A-3 de Pablo. La semilla conserva la versión anterior y agrega la versión con diez días hábiles para resolver una apelación. Identidad persiste el plazo de CU-59 en la recepción y devuelve la fecha al titular en la vista del expediente. El trabajo diario toma lotes con bloqueo de filas y acepta por vencimiento, revierte y emite el evento en la misma transacción. La consulta interna de retención distingue cautelar vigente y medida firme vigente de una suspensión todavía apelable que no se aplicó.

Continúan pendientes H3.S4–S6: custodia de punta a punta en entregas, aplicación y conciliación de la liberación con otras restricciones y cuenta verificada, modo restringido y backoffice. La rama conserva #525 como base y se debe integrar revisando sus últimos cambios. La autorización de un desembolso requiere validación del servicio de entregas. La consulta de identidad por sí sola no acredita fondos. El cierre de una medida que no se aplicó deja intacto el estado de la cuenta. Para un bloqueo cautelar aplicado, falta vincular su fila de bloqueo al expediente para revertirla sin alcanzar bloqueos independientes por la misma causal. Antes de usar la consulta desde entregas, conceder al servicio permiso dedicado a ese veredicto en lugar de lectura del directorio.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Implementación de CU-134 (Ola 3) | `TODO` | Aprobada por la sesión principal; arranca cuando #516 (lote B) esté en `dev` o contra un doble declarado de su contrato |
| A-1 destino de la entrega retenida si la medida queda firme | Decidido | Custodia al titular durante la vigencia. Falta integrar en entregas. |
| A-2 prescripción en meses y duración máxima en días | Decidido | Columnas propias en la política. |
| A-3 plazo de resolución de la apelación interna | Decidido | Diez días hábiles, con aceptación por vencimiento. |

## Hallazgos (relevamiento)
| Hecho | Ruta |
|---|---|
| `usuario.estado` ya admite `SUSPENDIDO` y `BLOQUEADO_SEGURIDAD`; CU-04 rechaza el login de ambos con AP-CU04-02 y revoca el refresh | `sql/10_tablas/01_identidad_usuarios/usuario.sql:25` · `docs/CasosDeUso/CU-04 …md:305-325` |
| `bloqueo_cuenta.motivo` ∈ `FRAUDE, INTENTOS_FALLIDOS, ORDEN_ADMIN, SOLICITUD_USUARIO` (no hay motivo de cuenta comprometida) | `sql/10_tablas/01_identidad_usuarios/bloqueo_cuenta.sql:14` |
| `restriccion_usuario` es la vía de la mora (CU-27), con tipos `LIMITE_MONTO, NO_CREAR_GRUPO, NO_SER_ORGANIZADOR, NO_UNIRSE` | `restriccion_usuario.sql:17-18` |
| `sancion` y `apelacion_sancion` exigen `registro_id` de incumplimiento: no sirven para una medida de seguridad sin modificar el modelo | `docs/Modelos/Entidades/08 …/sancion.md` |
| `politica_sancion.grupo_id` es nulable: podría ser la política de plataforma con plazos | `docs/Modelos/Entidades/08 …/politica_sancion.md` |
| `registro_acceso_datos` exige justificación ≥ 10 caracteres, pero `tipo_dato` no tiene valor para ficha ni correo | `docs/Restricciones.md:1599-1600` · `sql/10_tablas/09*/registro_acceso_datos.sql:19-20` |
| El permiso `DATOS_SENSIBLES_LEER` y los roles `FRAUD_ANALYST`, `RESPONSABLE_SEGURIDAD`, `SOPORTE` ya existen en la semilla; no hay permiso para leer participantes | `sql/60_semillas/10-roles-y-permisos.sql` |
| CU-44 impone deber de reserva (no avisar al titular) y dice que la cuenta pasa a `CONGELADA`, que es estado de `cuenta_billetera`, no de `usuario` | `docs/CasosDeUso/CU-44 …md:38,56` · `sql/10_tablas/10*/cuenta_billetera.sql:25` |
| Bolivia no tiene ley integral de protección de datos; el modelo sigue principios tipo RGPD | `docs/Cumplimiento.md:389-396` |
| Ningún CU especificaba directorio/ficha del participante ni suspensión de su cuenta (P-11) | `INVENTARIO.md` de gobierno de usuarios |

## Preguntas para Pablo
Están completas, con opciones y recomendación, en la sección «Decisiones requeridas» del CU-134. Resumen:

| ID | Pregunta |
|---|---|
| D-1 | ¿Catálogo cerrado de causales: `CUENTA_COMPROMETIDA`, `FRAUDE_CON_EVIDENCIA`, `ABUSO_CONTRA_PARTICIPANTES`, `CUENTA_DUPLICADA_O_DE_TERCERO`? |
| D-2 | ¿Medidas: bloqueo cautelar + suspensión temporal? ¿Existe inhabilitación definitiva? |
| D-3 | Números de plazos (descargo, apelación, prescripción, duración máxima) y dónde viven (¿`politica_sancion` de plataforma?) |
| D-4 | ¿Expediente en tablas nuevas de `identidad` (recomendado) o generalizar `sancion`? |
| D-5 | ¿Quién propone, decide y resuelve la apelación? ¿Permisos nuevos? |
| D-6 | Con `SUSPENDIDO`, CU-04 cierra todo login: ¿modo restringido para defenderse, pagar y reclamar? |
| D-7 | Sin cautelar, ¿la suspensión rige al decidir o al quedar firme? |
| D-8 | ¿Qué medidas firmes afectan la reputación? |
| D-9 | Confirmar la base legal declarada en la ficha |
| D-10 | ¿Siguen exigibles los aportes del suspendido? |
| D-11 | **Dinero:** ¿qué pasa con su entrega si su turno cae durante la medida? |
| D-12 | ¿Se lo reemplaza en el grupo? |
| D-13 | Inhabilitación definitiva con saldo o deuda |
| D-14 | Ampliar `registro_acceso_datos.tipo_dato` con `FICHA_PARTICIPANTE` y `CORREO` |
| D-15 | Motivo `CUENTA_COMPROMETIDA` en `bloqueo_cuenta` |
| — | Confirmar el número **CU-134** (CU-133 y CU-136 ya se usan como prefijo de pruebas en `servicios/entregas`) |

## Evidencia
```text
$ python3 scripts/verificar_boveda.py | tail -4
          cifras vigentes: 113 casos de uso · 354 tablas · 334 entidades · 188 restricciones · 56 ADR

TODO OK
exit=0

$ python3 scripts/verificar_criterios.py | tail -2
pendientes de implementar (6): CU-05 (identidad), CU-134 (identidad), CU-23 (garantia), CU-26 (garantia), CU-51 (aportes), CU-57 (nucleo-financiero)
Sin divergencias entre la boveda y el codigo.          (exit=0)

$ python3 scripts/verificar_seguridad.py | tail -1
TODO OK · 6 aviso(s)                                     (exit=0; los mismos 6 avisos en dev sin este cambio)

$ python3 scripts/verificar_carriles.py | tail -1
TODO OK (con avisos)                                     (exit=0; aviso previo: inversiones sin ficha de carril)

$ python3 scripts/inventario_endpoints.py --check       → exit=0, hallazgos: 0
$ python3 scripts/verificar_contratos_limites.py        → exit=0
$ python3 scripts/verificar_maqueta.py                  → exit=0, TODO OK
$ python3 scripts/validar_flujos.py                     → exit=0, Sin fallas.
$ python3 scripts/verificar_pruebas_web.py              → exit=0
$ python3 -I scripts/test_catalogo_de_datos.py          → OK
$ python3 -I scripts/catalogo_de_datos.py --escribir    → 404 tablas; regenera docs/catalogo-de-datos.md y catalogo-datos.json (cambio commiteado: CU-134 nombra tablas)
```
### CI del PR #514 (head `af1db1d`)
```text
success  Bóveda, seguridad, carriles y despliegue coherentes   (run 45459192974)
success  Esquema, semillas y prueba de humo sobre PostgreSQL real (run 45459192974)
success  Detectar impacto del cambio · La boveda y el esquema no divergen · Base efimera, semillas y permisos
success  Formato, reglas propias y compilacion · Dependencias, secretos e imagen
success  bootJar y docker build, multietapa y sin root · Los cinco corredores · Trivy
skipped  Flutter/Angular, goldens, iOS, OSV, contratos generados, punta a punta (no aplican a un cambio de docs)
```
Historia: en el primer head (`9aef3a2`) «Los cinco corredores» falló porque Maven Central devolvió
`429 Too Many Requests` al bajar `org.jacoco.ant:0.8.14` (clase `EXTERNAL`). La corrida del head
`af1db1d` fue cancelada por la concurrencia `ci-<ref>` con `cancel-in-progress` (un relanzamiento
sobre la misma rama la pisó); se relanzó una vez y terminó en verde.

Archivos completos: [evidencia/](./evidencia/).

## No cubierto
- El CU no se implementó ni se ejecutó contra nada: es especificación. Ninguna regla de él está verificada en runtime.
- No se dieron de alta en `docs/Restricciones.md` las restricciones nuevas ni en el modelo las entidades `expediente_medida_cuenta` / `apelacion_medida_cuenta`, los permisos `PARTICIPANTES_*` ni los valores de CHECK nuevos: quedan propuestos dentro del CU.
- Los jobs de Flutter, iOS y punta a punta se saltaron en la CI por no aplicar a un cambio de documentación.

## Desvíos del plan
- El PR #514 se mergeó antes de que este reporte registrara la CI verde; este estado final entra por un PR aparte, desde la misma rama.
- Se agregaron al alcance (anotado en el PLAN): la línea «Ver también» recíproca en 7 CU vecinos y el catálogo de datos regenerado, porque los verificadores de la CI los exigen.
- `planes/17 …` tenía además «los 112 CU» en la línea 577; también se actualizó.

## Riesgos residuales
- Choque menor de merge si otro lote edita el «Ver también» de CU-07, 17, 27, 44, 52, 55 o 93, o regenera el catálogo de datos (lote E toca `servicios/auditoria/consumo`, no el JSON del catálogo).
- Hasta que se decida D-6, la especificación de «el dinero propio no se toca» choca con el rechazo total de login de CU-04 para `SUSPENDIDO`.
- CU-44 menciona un estado `CONGELADA` de cuenta que no existe en `usuario.estado` (sí en `cuenta_billetera`): inconsistencia previa, anotada, no corregida (fuera de alcance).

## Decisiones y ambigüedades
- 2026-11-24: Pablo aprobó todo el lote G («Aprobado todo lo de G ya mismo»), comunicado por la sesión principal. Se aplicó la opción recomendada en cada D-n. D-13 no aplica porque D-2 no incluye inhabilitación definitiva.
- 2026-11-24: Pablo fijó D-3 (descargo 5 días hábiles, apelación 5 días hábiles, prescripción 12 meses, suspensión máxima 90 días; valor de plataforma sin fuente normativa) y D-11 (retener la entrega en custodia hasta resolver), comunicado por la sesión principal.
- Número **CU-134** (no CU-133) — supuesto; confirmar con Pablo.
- Servicio dueño `identidad` — supuesto; confirmar con Pablo.
- Título del CU: el pedido literal del lote G.
- Todo lo marcado «PROPUESTA — requiere aprobación de Pablo» en el CU es un supuesto sin fuente, no una regla.
