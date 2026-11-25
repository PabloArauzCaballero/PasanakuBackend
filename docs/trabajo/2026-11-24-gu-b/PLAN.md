# Plan — GU lote B: CU-08 por asignación en `identidad`

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (`servicios/identidad`, `docs/CasosDeUso/CU-08`, ejemplos del simulado)
- Rama: `claude/gu-b-cu08` (desde `dev` 6729629d) · Predecesor: inventario `docs/trabajo/2026-11-24-gobierno-de-usuarios/INVENTARIO.md` (rama `claude/modest-newton-4ckcid`)
- Decisiones de Pablo aplicables: P-2a (por asignación), P-5 (bitácora + correo), P-6 (informe + vencimientos), P-7 (step-up + motivo, nadie sobre sí mismo, nunca sin administrador), P-2b solo en lo que el backend expone.
- Resultado observable: un operador con `ACCESOS_ADMINISTRAR` (o `USUARIOS_INTERNOS_GESTIONAR`) asigna un rol con ámbito, vigencia y justificación, lo revoca individualmente con motivo, da de baja a un empleado y consulta el informe de accesos, todo por `identidad` con step-up; cada acto deja un evento de bitácora y un pedido de aviso en el outbox.
- Kill-test: `POST /usuarios-internos/{id}/asignaciones` sin `evidenciaMfa` válida responde 2xx → no está hecho. O: revocar la última asignación vigente de `ACCESOS_ADMINISTRAR` de un usuario ACTIVO responde 2xx → no está hecho.

## Hechos (descubrimiento)

- `CU08AsignarRol` / `CU08RevocarRol` existen sin controlador (`servicios/identidad/.../aplicacion/CU08*.java`).
- `PUT /usuarios-internos/{id}/roles` → `RotacionDeRolesInternos.rotar` revoca TODO y re-otorga (GU-06) y cierra sesiones siempre.
- `AccesosRepositorio.cuantosConservan` cuenta **asignaciones** (no personas), ignora vigencia y estado de cuenta (GU-11).
- Step-up: `/sesiones/desafios` con `proposito=ADMIN` emite JWT de evidencia (`EmisorDeAcceso.emitirEvidencia`, aud `aportaya-nucleo-financiero` por config). nucleo-financiero la valida con `JwtDecoder` + `jti` de un solo uso (`SegundoFactorStepUp`, `SegundoFactorLocal` en local/test).
- Prefijos ruteados por el gateway para identidad: `/identidad /permisos /roles /sesion /sesiones /usuarios /usuarios-internos` (`plataforma/gateway/.../rutas.yml:110`, `scripts/modelo.py:205`). `/accesos` NO existe.
- `asignacion_rol` no tiene columna de justificación; `uq_asignacion_vigente` impide dos asignaciones vivas iguales (vencidas incluidas mientras `revocada_en` sea NULL).
- No existe consumidor de eventos cruzados en `notificaciones` (solo `POST /notificaciones` → `notificaciones.aviso_solicitado`); el catálogo `evento_notificable` se siembra desde `seeders/minimos/15-eventos-y-plantillas.json`.
- `identidad` no tiene tabla de respuestas idempotentes ni de evidencias consumidas.
- Gradle local no resuelve dependencias (429): Java se compila y prueba en CI.

## Alcance

- IN: `identidad.yaml` (operaciones nuevas con `tags: [Accesos]` + campo opcional `evidenciaMfa` en alta/actualización/desbloqueo), `web/RutasDeAccesos` (nueva, extendida por `UsuariosInternosController`), `CU08AsignarRol`, `CU08RevocarRol`, CU-08 nuevos (baja, vencimiento, informe), `RotacionDeRolesInternos`, `GestionarUsuariosInternos`, `UsuariosInternosRepositorio.actualizar`/`asignarRoles`/`crear`(solo evento), `AccesosRepositorio`, `trabajos/` de identidad, `Aplicacion` (`@EnableScheduling`), puertos/adaptadores de step-up, tests web y CU08 nuevos (archivos nuevos), `docs/CasosDeUso/CU-08*.md`, ejemplos del simulado de las operaciones nuevas, Postman generado.
- OUT (de otros lotes, no se edita): `SegregacionDeFunciones`, semilla de roles, `UsuarioInternoMapeador`, `RolesYPermisosRepositorio`, consultas `listar`/`consultar` de `UsuariosInternosRepositorio` (A); `plataforma/comun-web/seguridad`, `EmisorDeAcceso`, `servicios/auditoria` (E); docs de CU nuevo de participantes (G); backoffice (D). Tests `CU08Test`, `CU08RechazosTest`, `BaseDeCU08` (A los toca por los códigos de segregación): no se cambian constructores ni firmas que usan.
- Ambigüedades registradas:
  1. **Ruta**: el CU dice `/accesos/asignaciones`; el gateway no rutea `/accesos`. Supuesto: `/usuarios-internos/{id}/asignaciones`, `/usuarios-internos/asignaciones/{asignacionId}/revocacion`, `/usuarios-internos/{id}/baja`, `/usuarios-internos/accesos/informe`. Se actualiza el CU. Confirmar con Pablo.
  2. **Revocar con DELETE + cuerpo**: se usa `POST .../revocacion` (cuerpo con motivo; DELETE con cuerpo lo descartan proxies). Confirmar con Pablo.
  3. **Suspender (GU-14)**: supuesto: suspender revoca todas las asignaciones vivas con motivo «SUSPENSION: …» y cierra sesiones; reactivar devuelve la cuenta sin roles: los roles se vuelven a otorgar uno por uno (revisión forzada). Confirmar con Pablo.
  4. **Consumidor del aviso**: ningún lote tiene asignado el consumidor en `notificaciones`. Se emite el pedido de aviso con contrato documentado y se simula en tres niveles (regla 65). Confirmar a quién se asigna.
  5. **Un solo uso de la evidencia step-up dentro de identidad**: sin tabla de evidencias consumidas; supuesto: registro en memoria con vencimiento (igual que los desafíos de `CU04StepUp`), riesgo residual con más de una réplica. Agregar tabla exige cambio de modelo (regla 97): pendiente para decisión.
  6. **Vencimiento**: el trabajo diario marca `revocada_en = vigente_hasta` con motivo `VENCIMIENTO` (libera `uq_asignacion_vigente` y es idempotente por la precondición `revocada_en IS NULL`).

## H1 — CU-08 por asignación expuesto, gobernado y auditable

**CA:** Dado un administrador de accesos con step-up, cuando asigna, revoca, da de alta, suspende, reactiva, da de baja o desbloquea a otro operador, entonces la operación ocurre sin reescribir las asignaciones que no cambian, queda un evento de bitácora con quién/qué/a quién/motivo y un pedido de aviso al afectado y al oficial de cumplimiento; sin step-up, sobre sí mismo o dejando el sistema sin administrador vigente y activo, se rechaza.
**DoD:** CI de la PR en verde (spotless, check, testBarrido, compileJava, tests de identidad incl. Testcontainers) · verificadores Python en 0 · `generar_postman.py`/`generar_ddl.py` sin diff · REPORTE.md.
**Estado:** HECHO

### H1.S1 — Contrato aditivo
**CA:** Dado `identidad.yaml`, cuando se valida, entonces es OpenAPI válido, sin romper operaciones existentes, con las 4 operaciones nuevas.
**DoD:** validación del YAML + `inventario_endpoints.py --check` + `generar_postman.py` sin diff.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Operaciones `asignarRolDeOperador`, `revocarAsignacionDeRol`, `darDeBajaUsuarioInterno`, `informeDeAccesos` + esquemas, tag `Accesos` | YAML válido con las 4 operaciones | `python3 -c yaml.safe_load` + `openapi-spec-validator` (si disponible) | HECHO |
| H1.S1.M2 | `evidenciaMfa` opcional en alta, actualización y desbloqueo; `PUT .../roles` marcado `deprecated: true` | diff aditivo | `git diff` del YAML sin campos removidos | HECHO |
| H1.S1.M3 | Ejemplos del simulado + Postman + inventario regenerados | verificadores sin deriva | `generar_postman.py`, `inventario_endpoints.py --check`, `verificar_maqueta.py` → 0 | HECHO |

### H1.S2 — Reglas de gobierno (step-up, sí mismo, último administrador)
**CA:** Dado un acto de gobierno, cuando falta la evidencia, apunta al propio actor o deja 0 titulares vigentes y activos de `ACCESOS_ADMINISTRAR`, entonces 422 con `AP-CU08-08`, `AP-CU08-04` o `AP-CU08-06`.
**DoD:** tests web y CU08 contra Postgres en CI.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Puerto `EvidenciaDeStepUp` + adaptador JWT (`!local & !test`) + doble local/test + `ExigirStepUp` (proposito ADMIN, sub, acr, un solo uso) | evidencia ajena/vencida/reusada/otro propósito → rechazo | `StepUpDeGobiernoTest` (unitario, 3 niveles) en CI | HECHO |
| H1.S2.M2 | `AccesosRepositorio.titularesDeAdministracion` (personas distintas, asignación viva no vencida, usuario ACTIVO, excluyendo asignaciones/usuario) | cuenta personas, no filas | `CU08GobiernoTest` (Testcontainers) en CI | HECHO |
| H1.S2.M3 | Guardas en revocar, baja, suspender (nunca sin administrador) y en toda acción (nadie sobre sí mismo) | rechazos con código | `CU08GobiernoTest` + `AccesosControllerWebTest` | HECHO |

### H1.S3 — Asignar / revocar por asignación (GU-05, GU-06)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | `AccesosController` (asignar, revocar) con matriz negativa 401/403/rol insuficiente | 201/200 y negativos | `AccesosControllerWebTest` en CI | HECHO |
| H1.S3.M2 | Asignar idempotente (misma asignación viva → mismo id, sin evento nuevo) y `requiereMfa` | segunda llamada mismo id | `CU08AsignacionIdempotenteTest` en CI | HECHO |
| H1.S3.M3 | Revocar idempotente (ya revocada → mismo resultado, sin evento) | sin segundo evento | ídem | HECHO |
| H1.S3.M4 | `PUT .../roles` (deprecado) por diferencia: conserva `otorgada_por/en` de los roles que siguen y cierra sesiones solo si quita alguno | asignaciones intactas | `RotacionPorDiferenciaTest` (Testcontainers) en CI | HECHO |

### H1.S4 — Bitácora y aviso (GU-07 emisión)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | `BitacoraDeAccesos`: evento `identidad.acceso_gobernado` (forma v1) + `identidad.aviso_de_acceso_solicitado` con destinatarios (afectado + titulares de `OFICIAL_CUMPLIMIENTO`) en la misma transacción | dos filas en `evento_dominio` por acto | `CU08BitacoraTest` (Testcontainers) en CI | HECHO |
| H1.S4.M2 | Emisión desde alta, suspensión, reactivación, baja, desbloqueo, asignar, revocar, vencer | cada acto emite | ídem | HECHO |
| H1.S4.M3 | Forma del evento documentada en CU-08 (§ Contrato de eventos) | sección presente | `verificar_boveda.py` → 0 | HECHO |

### H1.S5 — Baja y suspensión (GU-14)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S5.M1 | `CU08DarDeBaja`: revoca todas, cierra sesiones, estado `BAJA_VOLUNTARIA` (ambigüedad 7), conserva histórico | 0 vivas, filas intactas | `CU08BajaTest` en CI | HECHO |
| H1.S5.M2 | Suspender revoca asignaciones vivas; reactivar no devuelve roles | roles vacíos tras reactivar | `CU08BajaTest` en CI | HECHO |

### H1.S6 — Vencimientos e informe (GU-08)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S6.M1 | `CU08VencerAccesos` + `trabajos/VencimientoDeAccesos` (`@Scheduled`, SKIP LOCKED, idempotente) | segunda corrida no hace nada | `CU08VencimientoTest` en CI | HECHO |
| H1.S6.M2 | `CU08InformeDeAccesos` + endpoint paginado (vivos, por vencer 7 días, sin TOTP confirmado) | página determinista | `CU08InformeTest` + web | HECHO |

### H1.S7 — Cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S7.M1 | CU-08 actualizado (rutas, errores AP-CU08-08/09, baja/suspensión, eventos, gherkin de los tests nuevos) | criterios trazados | `verificar_criterios.py` → 0 | HECHO |
| H1.S7.M2 | Verificadores locales en 0 | salida 0 | scripts listados en el encargo | HECHO |
| H1.S7.M3 | Push + PR a `dev` + CI verde | checks verdes | `pull_request_read` checks | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Gradle local: errores de compilación solo en CI | iteraciones lentas | copiar patrones vecinos; revisar imports a mano |
| Step-up obligatorio en alta/suspensión/desbloqueo rompe el backoffice actual (no manda evidencia) | backoffice en `test` falla esos botones hasta el lote D | declarado; el campo es aditivo; orden de despliegue B→D en el reporte |
| Lote A cambia `CU08*Test`/`BaseDeCU08` | conflicto | no se tocan firmas públicas usadas por esos tests |
| Evidencia de un solo uso en memoria | reuso entre réplicas en ≤5 min | riesgo residual declarado + ambigüedad 5 |

Ambigüedad 7: la baja usa estado `BAJA_VOLUNTARIA`? El enum de `usuario.estado` no tiene `BAJA` de empleado; opciones `BAJA_VOLUNTARIA` o `ELIMINADO`. Supuesto: `SUSPENDIDO` NO; se usa `BAJA_VOLUNTARIA` (no borra ni anonimiza). Confirmar con Pablo.

## Desvíos registrados durante la ejecución
- Códigos: el lote A usa `AP-CU08-08` (ROL_NO_ASIGNABLE); B se corrió a `09` STEP_UP_REQUERIDO, `10` DESTINATARIO_NO_HABILITADO, `11` MOTIVO_REQUERIDO.
- El generador Spring ignora `tags`: las rutas nuevas caen en `UsuariosInternosApi`. Se sirven desde `RutasDeAccesos` (abstracta) que `UsuariosInternosController` extiende, patrón `RutasDeContactoEnGrupo`/`InvitacionesController`.
- `RolesParaPersonalInterno.admite` (lote A) no está en `dev`: se engancha cuando entre (acordado con A).
- Estado de microtareas: `A MEDIAS` = código escrito y pruebas escritas, pendiente de la CI (Gradle local bloqueado por 429).

## Cierre (2026-11-25)
- 20/20 microtareas HECHO. Evidencia: CI run 45513084333 (53710c43): «Los cinco corredores» y «Formato, reglas propias y compilacion» success; PR #516 mergeado en dev (90774d90).
