# Plan — publicar `GET /grupos/solicitudes` (bandeja de admisiones del backoffice, CU-68)

- Fecha: 2026-10-25 · Repos afectados: PasanakuBackend (servicio `grupos`) · Predecesor: H4.B.M1 de `2026-10-24-backoffice-correccion-y-atlas`
- Resultado observable: un usuario del backoffice con `SOLICITUD_INGRESO_VER` abre «Admisiones» y ve las solicitudes de ingreso de todos los grupos, en vez de «No encontramos lo que buscabas» (404 de `GET /grupos/solicitudes`).
- Kill-test: `GET /api/v1/grupos/solicitudes` con sesión de backoffice → hoy 404; con el cambio, 200 y un arreglo.

## Alcance
- IN: contrato OpenAPI de `grupos`, controlador, consulta en el caso de uso, pruebas web, ajuste del bloque de rutas conocidas sin contrato, ejemplo del mock.
- OUT: **`GET /grupos/solicitudes-escaladas`** (H4.B.M2): `DECISION_REQUIRED` — `docs/trabajo/2026-10-16-backoffice-carril-a/entregables/fichas-de-contrato.md:121` dice que `solicitud_ingreso` no tiene plazo ni estado de escalamiento y que negocio no decidió qué significa «escalada»; no se inventa. Tampoco: «Retiros en revisión», cambios en la pantalla del backoffice, lectura de nombres desde `identidad`, bitácora de lectura R-SEG-02 (catálogo de propósitos A-8 sin definir).
- Hechos (con ruta): la pantalla llama `GET ${gateway}/grupos/solicitudes` y espera un arreglo (`apps/backoffice/src/app/rutas/admisiones/dominio/cu68-admision.ts:113-118`); la lista por grupo ya existe (`RutasDeSolicitudes.java:78`, `OrganizadorDecideIngreso.java:40`); el gateway enruta `/grupos/**` (`plataforma/gateway/.../rutas.yml:101`); el permiso `SOLICITUD_INGRESO_VER` existe y lo tienen ADMIN_PLATAFORMA, SOPORTE y OPERATIONS_* (`sql/60_semillas/10-roles-y-permisos.sql:75`).
- Ambigüedades / decisiones tomadas (a confirmar con Pablo):
  1. **Forma**: arreglo plano acotado (`maxItems 200`, más nueva primero) con filtro opcional `estado` y `limite`, y NO el sobre con cursor del resto del sistema, porque la pantalla filtra y pagina en cliente sobre un arreglo (no puedo ejecutar el backoffice contra clientes generados aquí). Deuda: pasar a cursor cuando la pantalla se migre (regla 96.6).
  2. **Estados**: la base dice `APROBADA`; la pantalla espera `ACEPTADA`. La respuesta de este endpoint publica `ACEPTADA` (y el filtro también); `CANCELADA` no existe en la base y no se emite.
  3. **`postulante`**: el servicio `grupos` no puede leer nombres de `identidad` (invariante 11). Se usa `nombre_visible` si existe y, si no, «Usuario ab12cd34» (primeros 8 del id). Mejorarlo exige un endpoint por lote en `identidad`.
  4. Sin teléfono ni correo (aunque la tabla los guarda): no hay catálogo de propósitos para la bitácora de lectura.
  5. La consulta corre con contexto de sistema, como la cola del organizador, porque la RLS de `solicitud_ingreso` solo deja pasar a `BACKOFFICE/CUMPLIMIENTO/AUDITOR` y los roles sembrados del backoffice son otros. La puerta es el permiso del controlador.
- Limitación de verificación: sin Docker ni Maven aquí no se puede compilar con el generador ni correr Testcontainers; los gates Python sí corren. La verificación completa es la del CI de la rama.

## H1 — Bandeja de admisiones
**CA:** Dado un usuario con `SOLICITUD_INGRESO_VER`, cuando pide `GET /grupos/solicitudes`, entonces recibe 200 con las solicitudes de todos los grupos (más nuevas primero), con `estado` filtrable; sin ese permiso recibe 403 y sin sesión 401, y la respuesta no trae teléfono ni correo.
**DoD:** gates Python de contratos/rutas/pruebas web en verde · `GruposBandejaWebTest` verde en CI · prueba de integración de la consulta contra PostgreSQL real verde en CI.
**Estado:** A MEDIAS (falta la ejecución de las pruebas en el CI)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Contrato: ruta `/grupos/solicitudes` y esquema `SolicitudDeAdmision` (`additionalProperties:false`, límites) | los gates de contrato pasan | `python3 scripts/verificar_contratos_limites.py` | HECHO |
| H1.M2 | Consulta en `CU68AceptarIngreso` (SQL con `JOIN grupos.grupo`, filtro por estado, límite) | devuelve filas con `grupoCodigo` y estado traducido | prueba de integración (CI) | A MEDIAS |
| H1.M3 | Controlador con `@Permiso("SOLICITUD_INGRESO_VER")` | 200/403/401 según permiso | `GruposBandejaWebTest` (CI) | A MEDIAS |
| H1.M4 | Quitar la ruta de las conocidas sin contrato, regenerar Postman y el ejemplo del mock (el catálogo de operaciones y la matriz NO: arrastran deriva ajena) | los gates de rutas dejan de listarla | `python3 scripts/verificar_rutas_frontend.py` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| La firma generada del método no coincide con la que escribo | no compila | patrón idéntico a otros endpoints con `enum`→`String` y `Integer` opcional; el CI lo confirma |
| RLS oculta filas | bandeja vacía | contexto de sistema + prueba de integración con dos grupos |
| `SeguridadWebTest` exige mocks nuevos | pruebas rojas | no se agrega dependencia al controlador: el método va en `CU68AceptarIngreso`, que ya se mockea |
