# Plan — Catálogo de metadata del backoffice, con estado y revisión como el portal de Atlas

- Fecha: 2026-10-18 · Repos afectados: monorepo Pasanaku (auditoria, sql, seeders, backoffice) · Predecesor: `2026-10-17-acceso-por-correo-y-backoffice` (H6 a medias)
- Resultado observable: un operador con permiso abre «Catálogo de datos» desde el menú del backoffice, ve cada tabla con su **estado** (Activa, Planificada, Candidata a retiro, Retirada, Deshabilitada), su **revisión**, responsable, retención y sensibilidad; filtra por esos campos y edita la ficha de gobierno de una tabla, que queda guardada en la base y auditada.
- Kill-test: en TEST, cambiar el estado de una tabla a «Candidata a retiro», recargar, y que siga así; si vuelve a «Activa», no está hecho.

## Pedido literal (2026-10-18, Pablo)
> «FALTA LA CATALOGACION DE LA METADA, LOS ELEMENTOS QUE ESTAN ACTIVOS TAL COMO ESTA EN EL ADMIN PORTAL»

## Hallazgos (fase 1)
- Atlas `AtlasAdminPortal/src/features/data-catalog/`: lista con columnas Bloque, Schema, Tabla, Entidad, Módulo, Responsable, Datos personales, Financiera, Riesgo, Crítica para auditoría, Retención, Revisión, Estado; filtros por búsqueda, bloque y revisión; ficha con formulario de metadata (`PATCH /systems/data-entities/:id/metadata`).
- Atlas persiste en `system_data_entity_catalog` (`AtlasBackend/src/database/migrations/20260703001000-add-systems-ops-catalog-qa-audit.ts`): `status` ∈ ACTIVE/PLANNED/DEPRECATED/DISABLED (el portal agrega DEPRECATED_CANDIDATE), `review_status` ∈ AUTO_DETECTED/NEEDS_REVIEW/APPROVED/REJECTED, `data_owner`, `retention_policy_code`, banderas de sensibilidad.
- Pasanaku hoy: `apps/backoffice/src/app/rutas/tablero/catalogo/` muestra el catálogo **solo lectura**, servido por `GET /indicadores/catalogo-de-datos` (auditoria), que lee un JSON estático generado por `scripts/catalogo_de_datos.py` (`CatalogoDeDatosVigente.java`). No hay estado, ni revisión, ni responsable, ni nada editable. **No está en el menú** (`layout/shell-financiero.ts:124-140`).
- Esquema: fuente de verdad PlantUML `docs/entidades/*.puml` → `scripts/generar_ddl.py` → `sql/10_tablas/…`. Retención ya existe: `auditoria.politica_retencion`. Bitácora: `auditoria.bitacora_evento`.
- Permisos: `CATALOGO_EDITAR` existe pero es «Editar catálogos regulatorios» (`seeders/minimos/10-roles-y-permisos.json:163`), usado por tarifas.

## Alcance
- IN: tabla de gobierno por tabla del modelo; endpoint de lectura enriquecido (aditivo) y de edición; permiso nuevo; pantalla de lista estilo Atlas con métricas y filtros; ficha con sección y formulario de gobierno; entrada en el menú; despliegue en TEST.
- OUT: revisión por columna (Atlas la tiene; queda como siguiente paso), catálogos operativos con versiones (`operations/catalogs` de Atlas), glosario y dominios de negocio, tipos de usuario (H5 del trabajo anterior).
- Ambigüedades registradas:
  1. «Catalogación de la metadata, los elementos activos» puede ser (a) el catálogo de datos con estado por tabla o (b) los catálogos operativos de valores con elementos activos/inactivos. **Supuesto: (a)**, que es la pantalla «Catálogo de datos» de Atlas con su ficha de metadata. Confirmar con Pablo.
  2. Sensibilidad: en Atlas son banderas editables; acá ya se derivan del modelo por columna. **Supuesto:** se muestran derivadas (una sola fuente de verdad), no editables. Confirmar con Pablo.

## Decisiones
- Permiso nuevo `CATALOGO_DATOS_EDITAR` (requiere MFA, otorgado a ADMIN_PLATAFORMA): reusar `CATALOGO_EDITAR` cambiaría su significado (regla 00 §1.6).
- Una tabla sin ficha guardada se muestra como Activa / Detectada (mismo valor por omisión que Atlas); no se siembran filas inventadas.
- Concurrencia: `version` en la fila; la edición manda la versión leída y el `UPDATE` la exige (regla 96.3.2); si no coincide, 409.

## H1 — Ficha de gobierno persistida
**CA:** Dado el esquema aplicado, cuando se guarda la ficha de una tabla, entonces la fila existe en `auditoria.ficha_gobierno_tabla` con su estado y revisión, y un segundo guardado con versión vieja no pisa al primero.
**DoD:** `generar_ddl.py` exit 0 · deriva bóveda/esquema en verde · integrationTest de auditoria del repositorio.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Entidad `ficha_gobierno_tabla` en `09_auditoria_reportes.puml` + ficha `.md` + enums | DDL generado con CK de estado y revisión, UQ (esquema, tabla), FK a `politica_retencion` | `python3 scripts/generar_ddl.py` → exit 0 | HECHO — `evidencia/H1-S1-M1-esquema.txt` (338 tablas, bóveda «TODO OK», CU-130 escrito) |
| H1.S1.M2 | Permiso `CATALOGO_DATOS_EDITAR` en seeders mínimos y otorgado a ADMIN_PLATAFORMA | SQL de semillas regenerado lo contiene | generador de semillas + segunda corrida idempotente | HECHO — `evidencia/H1-S1-M2-semillas.txt` (mismo md5 en la segunda corrida) |
| H1.S1.M3 | Repositorio jOOQ: leer todas las fichas; guardar con versión (insert o update condicionado) | Test contra PostgreSQL real: guarda, lee, versión vieja → conflicto | `./gradlew :servicios:auditoria:integrationTest --tests '*CU130Test'` | HECHO — `evidencia/H1-cu130-integracion.txt` (5/5) |

## H2 — API
**CA:** Dado un operador con `CATALOGO_DATOS_EDITAR`, cuando hace `PUT …/gobierno` con datos válidos, entonces responde 200 con la versión nueva y queda un evento en bitácora; sin permiso 403; versión vieja 409; campo desconocido 400.
**DoD:** webTest + integrationTest de auditoria verdes; clientes regenerados.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Contrato: `gobierno` (aditivo) en `TablaDelCatalogo`; `PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/gobierno` | YAML válido, clientes generados | `./gradlew :servicios:auditoria:generarClientes` | HECHO — `guardarFichaDeGobierno` en `clientes/angular/auditoria`; Postman regenerado |
| H2.S1.M2 | `GET` superpone la ficha guardada (o el valor por omisión) a cada tabla | Test web: tabla sin ficha → ACTIVA/DETECTADA; con ficha → la guardada | `webTest --tests '*Catalogo*'` | HECHO — `FichaDeGobiernoWebTest.superpone` |
| H2.S1.M3 | `PUT` valida (tabla existe en el catálogo, enums, largos), exige permiso y versión, registra bitácora | 200 / 403 / 404 / 409 / 400 cubiertos | `webTest` + `integrationTest` | HECHO — `evidencia/H2-regresion-auditoria.txt` (test 34, barrido 2, web 51, integración 55; verificar_criterios sin divergencias). Errores de regla salen 422 (convención del proyecto), no 409/404 |

## H3 — Backoffice
**CA:** Dado el operador en el backoffice, cuando abre «Catálogo de datos» desde el menú, entonces ve métricas (tablas, activas, a revisar, con datos personales), filtra por estado, revisión y módulo, y edita la ficha de gobierno desde la ficha de tabla.
**DoD:** specs verdes, typecheck, lint, `verificar_frontend.py`, capturas en 3 anchos × 2 temas miradas.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Dominio: filtros por estado/revisión/módulo y métricas | Spec de dominio verde | `npx ng test --include='src/app/rutas/tablero/catalogo/**'` | HECHO — `gobierno.spec.ts` 3/3 |
| H3.S1.M2 | Lista estilo Atlas: métricas, barra de filtros, columnas con insignias de estado y revisión | Spec + captura | idem + capturas | HECHO — e2e `catalogo-gobierno` 3/3 (contra doble declarado de auditoria) + `evidencia/capturas/cu130-lista-*` (3 anchos × 2 temas, miradas; axe y desborde en verde) |
| H3.S1.M3 | Ficha: sección «Gobierno» y formulario de edición (errores por campo, sin doble envío, 409 explicado) | Spec: guarda, muestra 409 y 403 | idem | HECHO — `seccion-de-gobierno.spec.ts` 4/4 (guarda con versión, sin doble envío, AP-CU130-01 conserva borrador, solo lectura) + `cu130-ficha-*` |
| H3.S1.M4 | Entrada «Catálogo de datos» en el menú (grupo Sistemas) | Spec del shell | `npx ng test --include='src/app/layout/**'` | HECHO — `evidencia/H3-backoffice.txt`: test:front 839/839, lint, tsc, verificar_frontend OK |

## H4 — En TEST
**CA:** El kill-test pasa en `backoffice.aportaya…`.
**DoD:** PR a test y dev mergeados, host reconstruido, captura en vivo mirada.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Desplegado y verificado en vivo | Estado cambiado persiste tras recargar | captura en vivo + consulta a la base | TODO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| El supuesto (a) no es lo que Pablo quería | Rehacer | Declararlo en la entrega y preguntar en una línea |
| Tabla nueva en TEST requiere la imagen del esquema | Backend en cero si falta (memoria aportaya-test-contabo) | Verificar `aportaya/esquema:test` antes del push |
