# Reporte — `GET /grupos/solicitudes` (bandeja de «Admisiones» del backoffice, CU-68)

> **AVANCE: 2 / 4 microtareas HECHO — 50,0 %.** Las otras dos (consulta y controlador) están escritas; **el CI ya compiló el código de producción** (`compileJava` OK, formato y reglas propias de `grupos` OK) pero **la prueba de integración contra PostgreSQL nunca se ha ejecutado**, y una prueba web mía falló en CI (corregida, ver «Historia del CI»).

- Fecha: 2026-10-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/awesome-turing-5wmr4r` · Repo: PasanakuBackend (servicio `grupos`)
- Peldaño de evidencia alcanzado: **WRITTEN + gates estáticos** (los gates Python pasan). No `RUNS`: el generador OpenAPI y `compileJava` no se pudieron ejecutar aquí. No `TESTED`.

## Historia del CI (actualizado 2026-10-29)
- PR #191 (mergeado a `dev` el 26/10 con esta prueba en rojo): `GruposBandejaWebTest.sinParametros` esperaba `limite = null`, pero el contrato declara `default: 200` y Spring lo aplica → recibe `200`. **Error de la prueba**, no del endpoint. Evidencia: `Wanted: bandeja(isNull(), isNull(), any())` / `Actual: bandeja(null, 200, ContextoSesion[... rol=SOPORTE ...])`.
- Antes de eso el CI fue encontrando, en este orden: formato palantir (3 cortes de línea), un `armar()` que chocaba con un método estático de `BaseDeCU68`, y la regla `sin-umbral-literal` (que detecta por nombre: `TOPE_DE_BANDEJA = 200` → `FILAS_DE_LA_BANDEJA`).
- PR #266 corrige la prueba web. Su CI **no llegó a ejecutar `grupos:webTest` ni `BandejaDeAdmisionesRepositorioTest`**: `check` se corta antes en `auditoria:testBarrido` por código ajeno de `dev` (`SaludDeServicios.java` con `99.5` y `100` literales, `DisponibilidadDelServicio.java`, `IndicadoresController.java` con 379 líneas; commits de Pablo del 28–29/10). Hasta que eso se arregle, mis dos pruebas siguen sin ejecutarse en CI.
- La bóveda (ADR-053, índice de skills, «51 ADR») la corrige el PR #262; no se duplicó.

## Qué cambia
La pantalla «Admisiones» del backoffice llamaba `GET /grupos/solicitudes`, que ningún servicio publicaba → 404 → «No encontramos lo que buscabas». Ahora `grupos` la publica: arreglo de hasta 200 solicitudes de **todos** los grupos, las más nuevas primero, con `grupoCodigo`, `postulante`, `estado` con los nombres de la pantalla (`ACEPTADA` = `APROBADA` en la base) y filtros opcionales `estado` y `limite`. Permiso `SOLICITUD_INGRESO_VER` (ya existe y lo tienen ADMIN_PLATAFORMA, SOPORTE y OPERATIONS_*). Sin teléfono ni correo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | Ruta y esquema `SolicitudDeAdmision` en `grupos.yaml` | `python3 scripts/verificar_contratos_limites.py` · `verificar_pruebas_web.py` · `verificar_criterios.py` · `verificar_boveda.py` | PASS (exit 0 cada uno) |
| H1.M4 | La ruta sale de «conocidas sin contrato»; Postman regenerado; ejemplo del mock creado | `python3 scripts/verificar_rutas_frontend.py` → `TODO OK` (antes pedía sacarla: «YA tienen contrato») · `python3 scripts/generar_postman.py` | PASS |

## A medias
### H1.M2 — consulta en `CU68AceptarIngreso.bandeja`
- Qué anda: escrita con el patrón exacto de la cola del organizador (contexto de sistema por la RLS) y de `BitacoraRepositorio` (`?::varchar IS NULL OR …`, `LIMIT ?`); `BandejaDeAdmisionesRepositorioTest` (6 casos: varios grupos y orden, código del grupo, `APROBADA`→`ACEPTADA`, filtro por estado, estado desconocido, límite).
- Qué no anda / no se probó: **nada se ejecutó**. La prueba necesita PostgreSQL (Testcontainers).
- Qué falta exactamente: que el CI corra `14 · integrationTest`. Riesgo concreto a mirar: que la RLS deje ver filas con rol `SOPORTE` (el contexto de sistema debería, no está verificado contra la base real).
- Dónde quedó: `servicios/grupos/.../aplicacion/CU68AceptarIngreso.java`; compila según revisión, sin compilar.

### H1.M3 — controlador `listarBandejaDeAdmisiones`
- Qué anda: `@Permiso("SOLICITUD_INGRESO_VER")`, mapeo a `SolicitudDeAdmision`; `GruposBandejaWebTest` (7 casos: lista, filtro, sin parámetros, 403 participante, 403 organizador, 401, 400 por límite).
- Qué no anda / no se probó: no se compiló ni se ejecutó; la firma `(String estado, Integer limite)` se infirió del patrón de `calcularPlazo…` (enum de consulta → `String`).
- Qué falta exactamente: CI (`compileJava`, `13b · webTest`, `SeguridadWebTest`).
- Dónde quedó: `servicios/grupos/.../web/RutasDeSolicitudes.java`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| **`GET /grupos/solicitudes-escaladas` (H4.B.M2)** | **BLOQUEADO · `DECISION_REQUIRED`** | `fichas-de-contrato.md:121`: `solicitud_ingreso` no tiene plazo (48 h) ni estado de escalamiento y negocio no decidió qué es «escalada» (¿vencer = `EXPIRADA` o escalar a Operaciones?). La pantalla además espera `fechaLimiteOrganizador`, `escaladaEn`, `factores[]`, `canal`, que la base no guarda. Pablo debe decidir (opciones: leer «PENDIENTE con PROPUESTA y sin RESOLUCIÓN» sin plazo, o agregar columnas en `sql/`). Hasta entonces esa pantalla sigue en 404. |
| Pasar la bandeja a cursor `{items, proximoFecha, proximoId}` | TODO | Hoy es un arreglo acotado porque la pantalla pagina en cliente (regla 96.6 incumplida de forma declarada). |
| «Retiros en revisión» da error | TODO | La ruta `GET /billetera/retiros` existe en contrato y código; hace falta reproducirla con un backend vivo (URL de TEST + rol) para ver qué devuelve. |

## No cubierto
- Todo lo ejecutable: compilación, pruebas web, prueba de integración, RLS contra PostgreSQL, y el backoffice real contra el endpoint.
- Formato Spotless (palantir): no se pudo ejecutar el formateador (Maven Central limita la IP); se ajustó a mano a 120 columnas y estilo de cadena. Si el CI marca formato, `spotlessApply`.
- La pantalla del backoffice no se tocó; espera `ACEPTADA`/`PENDIENTE`/`RECHAZADA`/`EXPIRADA` y `postulante`, que ahora llegan.

## Desvíos del plan
- La prueba de integración no se llama `CU68…`: el gate `verificar_criterios.py` exige que todo `CU68*Test` sea un escenario Gherkin de la bóveda, y no se editan los requisitos del dueño. Se llama `BandejaDeAdmisionesRepositorioTest` (patrón que ya ejecuta `integrationTest`).
- No se regeneraron `catalogo-operaciones.json`, `operaciones.json` ni `matriz.md`: el generador arrastra operaciones de `dev` que ya estaban sin catalogar (`obtenerTablaDelCatalogo`, `firmarReglamentoDelGrupo`, …) y ningún gate lo exige. Deriva previa anotada, no corregida.

## Riesgos residuales
- `postulante` es `nombre_visible` o «Usuario ab12cd34»: `grupos` no puede leer nombres de `identidad` (invariante 11). Mejorarlo exige un endpoint por lote en `identidad`.
- Bitácora de lectura R-SEG-02 sin implementar para esta lectura (no devuelve teléfono/correo; falta el catálogo de propósitos A-8).
- La consulta usa contexto de sistema: toda la protección está en el permiso del controlador.

## Decisiones y ambigüedades (a confirmar con Pablo)
1. Forma: arreglo plano + `estado`/`limite` en vez del sobre con cursor.
2. `ACEPTADA` en la respuesta aunque la base diga `APROBADA` (la pantalla ya espera `ACEPTADA`; `CANCELADA` no se emite).
3. Fallback de `postulante`.
4. Qué significa «escalada» (bloquea M2).
