# Plan — GU-A · Segregación de funciones y correcciones de lectura (identidad)

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (`servicios/identidad`, `seeders/minimos`, `sql/60_semillas`,
  `apps/backoffice` solo la matriz de roles, tests de `nucleo-financiero`) · Rama: `claude/gu-a-segregacion` (desde `dev` 6729629d)
- Predecesor: inventario `docs/trabajo/2026-11-24-gobierno-de-usuarios/INVENTARIO.md` (rama `claude/modest-newton-4ckcid`),
  brechas GU-01, GU-10, GU-13, GU-17 y decisión P-3 de Pablo.
- Resultado observable: (1) asignar a un operador permisos que P-3 declara incompatibles con los códigos REALES de la
  semilla se rechaza con `INCOMPATIBILIDAD_DE_FUNCIONES`; (2) existe el rol `SUPERVISOR_CUMPLIMIENTO`, que levanta
  bloqueos por autoridad, y `OFICIAL_CUMPLIMIENTO` ya no los levanta; (3) la ficha y el listado de usuarios internos
  muestran el bloqueo real de `bloqueo_cuenta`; (4) el catálogo de roles puede pedirse filtrado a los asignables a
  personal interno y la asignación rechaza los de participante/servicio/anunciante; (5) `tieneSegundoFactor` solo
  cuenta factores activos y confirmados y el listado está paginado con orden determinista.
- Kill-test: `SegregacionDeFunciones.viola(Set.of("ENTREGA_AUTORIZAR","ENTREGA_EJECUTAR"))` devuelve `false` → no está hecho.

## Alcance
- IN: `dominio/SegregacionDeFunciones`, `dominio/RolesParaPersonalInterno` (nuevo), `infraestructura/UsuarioInternoMapeador`,
  `UsuariosInternosRepositorio.{listar,consultar,asignarRoles}`, `RolesYPermisosRepositorio`, clase nueva de lectura
  paginada, `UsuariosInternosController.listarUsuariosInternos`, `RolesController.listarRoles`, `identidad.yaml`
  (solo `GET /usuarios-internos`, `UsuarioInternoResumen`, `GET /roles`; cambios aditivos), semilla
  `seeders/minimos/10-roles-y-permisos.json` → `sql/60_semillas/10-roles-y-permisos.sql` (generado), tests CU08,
  tests de repositorio/web tocados, `apps/backoffice/src/app/nucleo/matriz-de-roles.spec.ts`, etiquetas de rol en tests
  de `nucleo-financiero` que levantan bloqueos, CU-08 (fila de error nueva).
- OUT (de otros lotes, no se tocan): `crear`/`actualizar` de usuarios internos, `CU08*` de aplicación,
  `RotacionDeRolesInternos`, `GestionarUsuariosInternos`, `AccesosController`, `trabajos/` (lote B); credenciales y
  fecha de nacimiento inventada (lote C); pantallas del backoffice (lote D); `plataforma/comun-web` y auditoría (lote E).
- Ambigüedades registradas:
  - **AMB-1 — RESUELTA por Pablo (2026-11-24, recibida por la sesión principal `session_01ATKb2QAkFb98LJ18k7D6w4`):**
    la consulta fue que P-3 juntaba en un mismo rol pares declarados incompatibles (`TESORERIA`, `OPERADOR_PUBLICIDAD`).
    Decisión: (1) el par contable correcto es «aprueba ≠ paga», `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR`↔`CONTABILIDAD_ERP_PAGAR`
    (reemplaza al planteo anterior; `CONTABILIDAD_ERP_PAGAR` NO se separa de `TESORERIA_OPERAR`/`ENTREGA_EJECUTAR`; la
    semilla ya lo cumple); (2) publicidad se parte: `OPERADOR_PUBLICIDAD` queda con `PUBLICIDAD_ANUNCIANTES` y
    `PUBLICIDAD_LIQUIDAR`, rol nuevo `APROBADOR_PUBLICIDAD` con `PUBLICIDAD_APROBAR_CAMPANA`; pares
    ANUNCIANTES↔APROBAR_CAMPANA↔MODERAR (tres personas).
  - **AMB-2:** P-3 da a `SUPERVISOR_CUMPLIMIENTO` «lectura de cumplimiento»; no existe un permiso de solo lectura de
    cumplimiento. Supuesto: `BLOQUEO_AUTORIDAD_LEVANTAR` (que ya abre la bandeja de bloqueos y su listado) +
    `AUDITORIA_LEER`. Confirmar con Pablo.
  - **AMB-3:** «paginado según el patrón del repo» sin romper el contrato: se agregan `limite/antesDe/antesDeId`
    opcionales y la respuesta sigue siendo un array (patrón de cursor `(creadoEn, id)` de erp/nucleo-financiero).
    Con el default de 50 un backoffice que no pagina ve solo los 50 más recientes: el lote D agrega la paginación.

## H1 — La segregación de funciones usa los códigos reales (GU-01, P-3)
**CA:** Dado un operador con `ENTREGA_AUTORIZAR`, cuando se le asigna un rol con `ENTREGA_EJECUTAR`, entonces se
rechaza con `INCOMPATIBILIDAD_DE_FUNCIONES`; ídem para cada par decidido y no conflictivo.
**DoD:** unitarios de segregación en verde en CI; CU08 con códigos reales; `verificar_criterios.py` en 0.
**Estado:** HECHO localmente (evidencia/H1-segregacion.md, H6-gates.md); CI pendiente al abrir el PR

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Reescribir los tests CU08 (criterio 2 y R-SEG-04) con códigos reales | fallan con el código viejo (los pares inventados no matchean) | CI `:servicios:identidad:test` rojo esperado en el commit de test, o demostración por lectura del Map viejo + test unitario puro | HECHO |
| H1.S1.M2 | Test unitario puro `SegregacionDeFuncionesTest`: cada par en ambos sentidos, sin falsos positivos, todos los códigos existen en la semilla, ningún rol de la semilla viola por sí solo | pasa solo con la tabla nueva | CI `:servicios:identidad:test --tests *Segregacion*` | HECHO |
| H1.S1.M3 | `SegregacionDeFunciones` con la lista cerrada de P-3 (con la corrección de AMB-1) | los tests de M1/M2 pasan | CI `:servicios:identidad:test` verde | HECHO |

## H2 — Rol `SUPERVISOR_CUMPLIMIENTO` (P-3 bloqueo)
**CA:** Dada la semilla aplicada, entonces `SUPERVISOR_CUMPLIMIENTO` tiene `BLOQUEO_AUTORIDAD_LEVANTAR` y
`OFICIAL_CUMPLIMIENTO` no lo tiene, también en una base ya sembrada; la segunda corrida no cambia nada.
**DoD:** `generar_semillas.py` sin errores; psql dos veces sobre base efímera (si hay PostgreSQL) o CI `base`; spec de
matriz de roles en verde.
**Estado:** HECHO localmente (evidencia en `evidencia/`); CI pendiente al abrir el PR

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Semilla JSON: rol nuevo + sus permisos; quitar `LEVANTAR` a `OFICIAL_CUMPLIMIENTO` (con bloque re-ejecutable que borra la fila vieja en bases ya sembradas); regenerar SQL | el SQL generado contiene el rol y el DELETE acotado | `python3 scripts/generar_semillas.py` exit 0 + `git diff --stat` | HECHO |
| H2.S1.M2 | Matriz de roles del backoffice: 33 roles y lo que ve el supervisor | spec verde | `yarn workspace @aportaya/backoffice vitest run src/app/nucleo/matriz-de-roles.spec.ts` | HECHO |
| H2.S1.M3 | Etiquetas de rol de los tests de nucleo-financiero que levantan bloqueos → `SUPERVISOR_CUMPLIMIENTO` | sin `OFICIAL_CUMPLIMIENTO` + `LEVANTAR` en tests | `grep` sin resultados | HECHO |
| H2.S1.M4 | (agregada por AMB-1) Rol `APROBADOR_PUBLICIDAD`; `OPERADOR_PUBLICIDAD` sin `PUBLICIDAD_APROBAR_CAMPANA` (DELETE re-ejecutable); matriz de roles (34) y etiquetas de tests de publicidad | semilla dos veces igual; spec y tests verdes | psql ×2 + `yarn ng test matriz-de-roles` + `:servicios:publicidad:webTest` | HECHO |

## H3 — El bloqueo que se ve es el real (GU-10)
**CA:** Dado un operador con un `bloqueo_cuenta` vigente, cuando se lista o consulta, entonces `bloqueadoHasta` y
`motivoBloqueo` salen de ese bloqueo; liberado o vencido, salen nulos.
**DoD:** test de repositorio contra PostgreSQL real (Testcontainers) en CI.
**Estado:** HECHO localmente (evidencia en `evidencia/`); CI pendiente al abrir el PR

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Lectura de `bloqueo_cuenta` vigente en `listar`/`consultar` y el mapeador | 3 casos (vigente, liberado, vencido) | CI `UsuariosInternosRepositorioTest` | HECHO |

## H4 — Roles asignables a personal interno (GU-13 backend)
**CA:** Dado el catálogo, cuando se piden los roles con `asignablesAPersonalInterno=true`, entonces no vienen
`PARTICIPANTE`, `ORGANIZADOR`, ámbito `GRUPO`, `ANUNCIANTE` ni `SERVICIO_*`; sin el parámetro vienen todos; y asignar
uno de esos a un operador se rechaza con `ROL_NO_ASIGNABLE`.
**DoD:** unitario del átomo, web test de `/roles`, test de repositorio de `asignarRoles`.
**Estado:** HECHO localmente (evidencia en `evidencia/`); CI pendiente al abrir el PR

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Átomo `RolesParaPersonalInterno` + unitario | admite/rechaza por código y ámbito | CI unitario | HECHO |
| H4.S1.M2 | `GET /roles?asignablesAPersonalInterno=true` (yaml + controlador) | filtra; sin parámetro igual que antes | CI `RolesControllerWebTest` | HECHO |
| H4.S1.M3 | `asignarRoles` rechaza no asignables (AP-CU08-08) | 422 con código 8-8 | CI `UsuariosInternosRepositorioTest` | HECHO |

## H5 — Segundo factor real y listado paginado (GU-17, solo lectura)
**CA:** Un factor sin confirmar o inactivo no cuenta como segundo factor; el listado devuelve como mucho `limite`
filas, en orden `creadoEn desc, id desc`, y el cursor trae la página siguiente sin repetir ni saltear.
**DoD:** tests de repositorio y web en CI.
**Estado:** HECHO localmente (evidencia en `evidencia/`); CI pendiente al abrir el PR

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H5.S1.M1 | `tieneSegundoFactor` = activo y confirmado | factor sin confirmar → false | CI `UsuariosInternosRepositorioTest` | HECHO |
| H5.S1.M2 | Listado paginado (yaml aditivo, clase de lectura, filtro de rol en SQL) | dos páginas sin solapes; cursor a medias → 422 | CI repositorio + web | HECHO |

## H6 — Gates y entrega
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Verificadores locales en 0 | todos exit 0 | `verificar_*.py`, `inventario_endpoints.py --check`, `generar_postman.py`/`generar_ddl.py` sin diff, `openapi-generator validate` | HECHO |
| H6.S1.M1b | (agregada) `verificar_seguridad.py` acepta bloques `sql` sin `tabla` en la semilla (`b.get("tabla")`) | exit 0 | `python3 scripts/verificar_seguridad.py` | HECHO |
| H6.S1.M2 | PR a `dev` con CI verde | checks verdes | GitHub checks | EN CURSO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Gradle local no resuelve dependencias (429) | No se compila Java local | La CI compila y prueba; formato copiado de vecinos |
| Conflicto de merge con lote B en `identidad.yaml` y `UsuariosInternosRepositorio` | Rebase manual | Cambios aditivos y acotados; aviso enviado a la sesión principal |
| AMB-1 sin respuesta | Dos pares de P-3 sin activar | Registrado como `DECISION_REQUIRED` |
