# Plan — Lote F · Gobierno de usuarios: roles editables (GU-16) y cuentas de servicio (GU-18)

- Fecha: 2026-11-25 · Repos afectados: PasanakuBackend (servicios/identidad, apps/backoffice, docs, sql, seeders) · Rama: `claude/gu-f-roles-servicio` (PR contra `dev`) · Predecesor: lotes A (segregación), B (#516 CU-08 + `EvidenciaDeStepUp`), E (consumidor de auditoría), D (menú)
- Resultado observable: un operador con permiso crea, edita y retira roles personalizados desde «Sistemas › Roles internos» (step-up + motivo, bitácora, cuatro ojos si el rol toca permisos de riesgo ALTO/CRÍTICO, sin romper la segregación de funciones) y gobierna cuentas de servicio (alta con credencial de un solo uso, rotación, revocación, vencimiento) desde «Personal y accesos › Cuentas de servicio».
- Kill-test: `POST /roles` con un par incompatible (`ENTREGA_AUTORIZAR`+`ENTREGA_EJECUTAR`) devuelve 4xx y no deja fila; `PATCH` de un rol `es_sistema` devuelve 4xx; el valor de una credencial no aparece en ninguna respuesta posterior ni en logs.

## Hallazgos de descubrimiento (hechos con ruta)
- `servicios/identidad/.../web/RolesController.java`: solo `GET /roles` (`listarRoles`). `RolesYPermisosRepositorio.java`: solo lectura de `rol`/`permiso`/`rol_permiso`.
- Esquema fuente de verdad: `docs/entidades/01_identidad_usuarios.puml` → `scripts/generar_ddl.py` → `sql/` (tabla `rol` sin columnas de autoría ni retiro; `rol_permiso` sin auditoría).
- `SegregacionDeFunciones.conflicto(Set<String>)` y `RolesParaPersonalInterno.admite` ya están en dev (lote A/GU-13). Pares: 6, lista cerrada de P-3.
- Las identidades de servicio son **roles** `SERVICIO_*` (`seeders/minimos/10-roles-y-permisos.json`, `es_sistema=true`) portados por un `usuario` con teléfono, clave y TOTP, aprovisionado **fuera del repo** (`22-cuentas-de-sistema.json`); inician sesión por `POST /sesiones` (`CredencialDeServicioConfigurada`).
- `ROLES_INTERNOS_GESTIONAR` existe en el catálogo (nivel ALTO) y ningún código lo usa (BACKLOG 2026-10-29 #7: «FALTA CONTRATO»).
- Step-up: `CU04StepUp` emite la evidencia (JWT, audiencia nucleo-financiero). El puerto `EvidenciaDeStepUp` y `EvidenciaStepUpConsumidaRepositorio` (un solo uso por `jti`) viven en la rama del lote B (`claude/gu-b-cu08`, #516), **todavía no en dev**.
- Bitácora: eventos por outbox (`CU08AsignarRol` emite `identidad.rol_asignado`); el consumidor de auditoría (lote E) los recoge.
- **`TRASPASO.md` e `INVENTARIO.md` (docs/trabajo/2026-11-24-gobierno-de-usuarios/) no existen en ninguna rama remota.** Las decisiones P-9/P-10 («sí») se toman del encargo.

## Alcance
- IN: backend identidad (casos de uso, repositorios, controlador, openapi solo operaciones nuevas `[Roles]`/`[CuentasDeServicio]`), esquema vía .puml→generar_ddl, seeders idempotentes, backoffice (roles-internos + sección de cuentas de servicio), CU nuevos en `docs/CasosDeUso`.
- OUT: roles `es_sistema` (no se editan ni borran), TOTP de cuentas de servicio por canal distinto al ya existente, cualquier otro servicio, el menú ajeno del lote D, endpoints existentes de B/C.

## Ambigüedades registradas (regla 00)
| # | Pregunta | Supuesto tomado | Confirmar con |
|---|---|---|---|
| A1 | ¿Qué permisos son «ámbito dinero/identidad» (cuatro ojos)? No hay clasificación en el catálogo: solo `nivel_riesgo`. | Cuatro ojos para todo rol que incluya un permiso con `nivel_riesgo` ALTO o CRÍTICO del catálogo real (49 de 74): criterio derivado de datos, más estricto que una lista inventada. | Pablo / Cumplimiento |
| A2 | Vigencia máxima de una cuenta de servicio y de una credencial. | Sin valor por defecto inventado: el vencimiento es obligatorio y lo fija quien da el alta, con tope = parámetro de configuración declarado `aportaya.cuentas-servicio.vigencia-maxima` (sin valor financiero). | Seguridad |
| A3 | Rotación del TOTP de la cuenta de servicio. | Fuera de este lote: la rotación cubre la clave; el factor sigue el enrolamiento existente. | Seguridad |
| A4 | Un rol personalizado, ¿puede ser de ámbito GRUPO/ORGANIZACION? | No: solo `GLOBAL` (los de grupo son de la app, GU-13). | Pablo |

## H1 — Roles: crear, editar, retirar (GU-16)
**CA:** Dado un operador con `ROLES_INTERNOS_GESTIONAR` y step-up vigente, cuando crea o edita un rol personalizado con permisos compatibles, entonces el rol queda, el evento `identidad.rol_*` sale con actor/origen/motivo y, si toca riesgo ALTO/CRÍTICO, queda pendiente de un segundo operador distinto.
**DoD:** `./gradlew :servicios:identidad:test` verde en CI · gates del encargo · capturas.
**Estado:** TODO

### H1.S1 — Dominio y esquema
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `ReglasDeRolPersonalizado` (código `[A-Z0-9_]`, no `SERVICIO_`, sin colisión con sistema, solo GLOBAL, ≥1 permiso, sin par incompatible, `exigeCuatroOjos` por riesgo) | Rechaza cada regla violada y acepta el caso válido | test unitario `ReglasDeRolPersonalizadoTest` (correcto/límite/inválido) | TODO |
| H1.S1.M2 | `.puml` + DDL: `rol.creado_por/creado_en/retirado_en`, tabla `solicitud_cambio_rol` (cuatro ojos), índices con nombre | `generar_ddl.py` sin diff y deriva en verde | `python3 scripts/generar_ddl.py && git diff --exit-code sql` | TODO |
### H1.S2 — Casos de uso y contrato
| H1.S2.M1 | Casos de uso crear/editar/retirar con step-up+motivo+outbox | evento por cada acción | test de integración (CI) | TODO |
| H1.S2.M2 | Aprobación a cuatro ojos (otro operador, no el proponente) | proponente≠aprobador | test CU | TODO |
| H1.S2.M3 | Operaciones nuevas en `identidad.yaml` + controlador + permisos | `openapi validate` y `inventario_endpoints --check` | scripts | TODO |

## H2 — Cuentas de servicio (GU-18)
**CA:** Dado un operador con step-up, cuando da de alta una cuenta de servicio, entonces recibe la credencial una sola vez (en la BD queda solo el hash), con permisos del rol `SERVICIO_*` acotado y vencimiento; rotar y revocar exigen step-up+motivo y dejan bitácora; el valor jamás se loguea ni se devuelve de nuevo.
| H2.S1.M1 | Esquema `cuenta_servicio` + `.puml`/DDL | deriva en verde | generar_ddl | TODO |
| H2.S1.M2 | Alta/rotación/revocación/listado (casos de uso + controlador) | CA arriba | test CI | TODO |
| H2.S1.M3 | Dobles de tres niveles de `EvidenciaDeStepUp` (regla 65) mientras #516 no esté en dev | correcto/límite/inválido | test | TODO |

## H3 — Backoffice
| H3.S1.M1 | Roles internos: crear/editar/retirar (regla 95: 4 estados, formularios, a11y) | capturas 390/834/1440 × claro/oscuro vistas | playwright + inspección | TODO |
| H3.S1.M2 | Cuentas de servicio bajo «Personal y accesos» (solo agrego mi entrada) | idem | idem | TODO |

## H4 — Casos de uso, gates y entrega
| H4.S1.M1 | CU nuevos (primero buscar equivalente) con errores correlativos | `verificar_boveda` | script | TODO |
| H4.S1.M2 | Gates: criterios, seguridad, carriles, contratos_limites, inventario, postman | todos en verde | scripts | TODO |
| H4.S1.M3 | PR a dev, CI verde, aviso a sesión principal | — | CI | TODO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Java no compila en local (Gradle 429) | Errores solo visibles en CI | Código pequeño y simétrico a los patrones vecinos; CI como árbitro |
| #516 no entra a dev | `EvidenciaDeStepUp` ausente | Doble declarado (regla 65); al entrar #516, merge de dev |

## Estado al traspaso (2026-11-25)
H1.S1.M1/M2, H1.S2.M1/M2/M3, H2.S1.M1/M2, H4.S1.M1 (CU-135 y CU-136): **A MEDIAS** — escritos, sin ejecutar en CI (la CI de efa22f39 cayó en spotless y bóveda; corregido en f9e5c579, corrida nueva pendiente).
H2.S1.M3 (doble de step-up): **DESCARTADO** — #516 ya está en dev y trae el puerto `EvidenciaDeStepUp`; se usa el real (las pruebas usan el doble `DOBLE_DE_STEP_UP` de B).
H3.S1.M1/M2 (backoffice): **TODO** — falta además un componente de step-up (abrir desafío + código TOTP → evidencia), que el backoffice no tiene.
H4.S1.M2/M3: **TODO / EN ESPERA de CI**.
Alcance de H2 ajustado: se **registra** una cuenta ya aprovisionada (no se crea el usuario): ambigüedad A5.
