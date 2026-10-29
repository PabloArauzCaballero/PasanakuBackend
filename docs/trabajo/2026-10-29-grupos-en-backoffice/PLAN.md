# Plan — Grupos en el backoffice: listado y detalle con participantes

- Fecha: 2026-10-29 · Repos afectados: monorepo Pasanaku (servicios/grupos, apps/backoffice) · Predecesor: `2026-10-29-backoffice-decisiones` (decisión de Pablo: empezar por Grupos)
- Rama: `claude/backlog-decisiones-20261029` → se renombra al empezar el código (`claude/grupos-backoffice-20261029`)
- Resultado observable: un operador `ADMIN_PLATAFORMA` (o auditor con `AUDITORIA_LEER`) entra a «Grupos», ve todos los
  grupos con su estado y ocupación, filtra por estado, y abre un grupo para ver sus datos y la lista de participantes.
- Kill-test: en TEST hay grupos (`grupos.grupo`) y la pantalla no los lista, o un PARTICIPANTE recibe 200 → no está hecho.

## Hallazgos
- No existe ningún endpoint de listado de grupos para backoffice: `consultarDetalleDelGrupo` exige `PARTICIPANTE`
  (`RutasDelGrupo.java:67`); el resto es del participante u organizador (`grupos.yaml`, 37 operaciones).
- No hay permiso de catálogo «ver grupos» (`sql/60_semillas/10-roles-y-permisos.sql`: solo GRUPO_ADMINISTRAR, GRUPO_CREAR).
  Patrón existente para lecturas de backoffice: `AUDITORIA_LEER` (reversos, custodia, cierres, desembolsos en `nucleo/menu.ts`).
- Patrón a copiar en backend: `listarBandejaDeAdmisiones` (`grupos.yaml:56`, `RutasDeSolicitudes.java:105`, `BandejaDeAdmisionesRepositorio`).
- Columnas de `grupos.grupo`: codigo_publico, nombre, monto_aporte NUMERIC(14,2), moneda, periodicidad, cupos_totales,
  cupos_ocupados, fecha_inicio, estado, visibilidad, organizador_id, usa_fondo_garantia…

## Alcance
- IN: `GET /grupos/backoffice` (listado acotado, filtro por estado) y `GET /grupos/backoffice/{grupoId}` (detalle +
  participantes: alias, estado, organizador sí/no, número de cupo, fecha de ingreso). `@Permiso("ADMIN_PLATAFORMA")` +
  `@PermisoAlternativo({"AUDITORIA_LEER"})`. Pantallas de listado y detalle, sección propia `ver:grupos`.
- OUT: acciones sobre grupos (suspender, cancelar, sustituir administrador ya existe aparte); importes por participante;
  contacto de nadie; aportes/mora (otro servicio).
- Ambigüedades: datos de participantes en una vista de backoffice → solo alias y estado (respuesta mínima, regla 90.1.8).
  La bitácora de lectura (90.2.7) aplica a datos FINANCIEROS de una persona; esta vista no los trae. Confirmar con Pablo.

## H1 — El operador ve los grupos y sus participantes
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato: dos operaciones + esquemas `GrupoDeBackoffice`, `DetalleDeGrupoDeBackoffice`, `ParticipanteDeBackoffice` | sin campos de contacto ni importes por persona | `./gradlew :servicios:grupos:compileJava` (genera interfaz) | HECHO |
| H1.S1.M2 | Consulta jOOQ + rutas | admin 200; AUDITORIA_LEER 200; PARTICIPANTE 403; sin token 401 | `webTest` del controlador | HECHO |
| H1.S1.M3 | Integración contra Postgres real | listado acotado, filtro por estado, detalle con participantes, grupo inexistente 422 | `integrationTest` | HECHO |
| H1.S2.M1 | Cliente Angular regenerado + dominio | tipos generados | `yarn typecheck` | TODO |
| H1.S2.M2 | Pantalla de listado | carga / datos / vacío / error | spec dirigido | TODO |
| H1.S2.M3 | Pantalla de detalle | datos + participantes; inexistente → mensaje | spec dirigido | TODO |
| H1.S2.M4 | Menú (≤ 8 pestañas), sección, ruta | menu.spec + menu-y-guardias + matriz de roles | specs | TODO |
| H1.S3.M1 | Regresión completa backoffice + grupos | sin rojos nuevos | suites completas, comparadas con el árbol limpio | TODO |
| H1.S3.M2 | Postman + `generar_ddl`/CI coherentes | CI sin rojos nuevos | `gh pr checks` | TODO |
| H1.S3.M3 | Verificación en TEST | listado con los grupos reales | llamada con token + captura | TODO |

## Hallazgos durante la ejecución
- Un ayudante nuevo inyectado por setter en `RutasDelGrupo` rompía **90/114** tests web (cada test mockea a mano los ayudantes): se agregó el mock de `GruposDeBackoffice` en los 13 tests web del controlador. Suite web después: 122/123.
- Preexistentes, iguales en el árbol limpio de `origin/test` (no se tocan): `GruposBandejaWebTest.sinParametros` (1/7: el generador entrega el `default: 200`) y `BandejaDeAdmisionesRepositorioTest` (6/6: `fecha_resolucion` con tipo texto en el fixture).
- `grupos.grupo` no guarda fecha de creación: el listado ordena por `fecha_inicio DESC NULLS FIRST, codigo_publico, id`.
- El test de integración tiene que llamarse `*RepositorioTest` para caer en el corredor con Postgres.

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Exponer datos de participantes de más | privacidad | esquema mínimo + revisión contra `data-privacy-financial` |
| RLS de `grupos` filtra al rol de backoffice | listado vacío | contexto de sistema como en `OrganizadorDecideIngreso.sistema()`, declarado |
