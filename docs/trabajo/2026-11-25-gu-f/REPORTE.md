# Reporte — Lote F · roles editables (GU-16) y cuentas de servicio (GU-18)

> **AVANCE: 0 / 13 — 0,0 %.** (`microtareas HECHO / total`; ninguna tiene su DoD completo todavía: lo hecho está en `A MEDIAS`.)

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-f-roles-servicio` (sin PR todavía)
- Peldaño de evidencia alcanzado: esquema `RUNS` (aplicado a PostgreSQL 16, exit 0); dominio `WRITTEN` (compila con `javac`, su test no corrió: Gradle local da 429, JUnit solo en CI).

## Completado
ninguna

## A medias
### H1.S1.M1 — `ReglasDeRolPersonalizado`
- Qué anda: compila con `javac` junto a sus dependencias (`AmbitoDeRol`, `RolesParaPersonalInterno`, `SegregacionDeFunciones`, `CodigoError`).
- Qué no anda: `ReglasDeRolPersonalizadoTest` (correcto/límite/inválido) escrito y **no ejecutado**.
- Qué falta exactamente: correrlo en CI (`./gradlew :servicios:identidad:test`).
- Dónde quedó: `servicios/identidad/.../dominio/ReglasDeRolPersonalizado.java` + test; compila.

### H1.S1.M2 — esquema
- Qué anda: `.puml` (`rol.creado_por/creado_en/retirado_en`, `solicitud_cambio_rol`, `cuenta_servicio`), `docs/Restricciones.md` (3 CHECK + 1 de cuenta), fichas en `docs/entidades/01_identidad_usuarios.md`, `generar_ddl.py` y `catalogo_de_datos.py --escribir` regenerados; `aplicar.sql` exit 0; `verificar_narrativas` sin fallas; los CHECK rechazan (ver `evidencia/restricciones-de-esquema.txt`).
- Qué no anda / falta: deriva contra las entidades jOOQ (se genera en el build: CI); segunda aplicación idempotente sobre base ya poblada; permisos de `svc_identidad` verificados (`INSERT`/`UPDATE` = `t`) pero sin la sentencia de «append-only» de la bitácora (no aplica: son tablas con estado).
- Dónde quedó: commit `feat(identidad): esquema…`, empujado.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M1 casos de uso crear/editar/retirar | TODO | EvidenciaDeStepUp de #516 (doble de tres niveles si no entra) |
| H1.S2.M2 aprobación cuatro ojos | TODO | idem |
| H1.S2.M3 operaciones openapi + controlador | TODO | coordinar `identidad.yaml` con B (#516) |
| H2.S1.M1..M3 cuentas de servicio | TODO | A2/A3 (vigencia y TOTP) |
| H3.S1.M1..M2 backoffice | TODO | endpoints |
| H4.S1.M1..M3 CU-135/136, gates, PR | TODO | todo lo anterior |

## Evidencia
- `evidencia/restricciones-de-esquema.txt`: rechazos de `ck_solicitud_cambio_rol_cuatro_ojos`, `ck_solicitud_cambio_rol_rol_requerido`, `ck_cuenta_servicio_revocacion`.
- `python3 scripts/generar_ddl.py` → «Sin pendientes a nivel de datos»; `python3 -I scripts/verificar_narrativas.py` → «Sin fallas».

## No cubierto
Todo lo Java ejecutable, openapi, UI, CU, gates distintos de `verificar_narrativas`, CI.

## Desvíos del plan
Ninguno hasta ahora.

## Riesgos residuales
- #516 (B) toca los mismos archivos generados (`.puml`, `modelo.py`, `metadata_enriquecida.json`, catálogo): al mergear dev, regenerar en vez de resolver a mano.
- `TRASPASO.md` / `INVENTARIO.md` citados en el encargo no existen en ninguna rama remota.

## Decisiones y ambigüedades
A1–A4 en el PLAN (cuatro ojos para todo riesgo ALTO/CRÍTICO del catálogo; vigencia de cuentas de servicio; TOTP; ámbito GLOBAL único). A confirmar: A1 con Pablo/Cumplimiento, A2/A3 con Seguridad, A4 con Pablo.
