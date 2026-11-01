# Reporte — Grupos en el backoffice: listado y detalle con cupos

> **AVANCE: 8 / 10 — 80,0 %.**

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/grupos-backoffice-20261029` (desde `origin/test`)
- Peldaño de evidencia: backend `TESTED` (web + integración contra Postgres real) · frontend `TESTED` contra HTTP simulado · sin ejercitar en TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Contrato: `listarGruposDeBackoffice`, `consultarGrupoDeBackoffice`, esquemas sin contacto ni importes por persona | `./gradlew :servicios:grupos:compileJava` | EXIT 0 |
| H1.S1.M2 | Rutas `@Permiso("ADMIN_PLATAFORMA")` + `@PermisoAlternativo({"AUDITORIA_LEER"})`; matriz: admin 200, auditor 200, participante 403, organizador 403, soporte 403, sin sesión 401, límite fuera de rango 400, inexistente 422 `AP-CU64-01` | `./gradlew :servicios:grupos:webTest` | `GruposDeBackofficeWebTest` 9/9; suite web 122/123 (el rojo es preexistente, ver abajo) |
| H1.S1.M3 | Consulta real: listado con aporte exacto, filtro por estado, límite acotado, detalle con cupos (alias, organizador, libre), inexistente | `./gradlew :servicios:grupos:integrationTest --tests '*GruposDeBackofficeRepositorioTest'` | 4/4 |
| H1.S2.M1 | Cliente Angular regenerado + dominio | `yarn typecheck` | EXIT 0 |
| H1.S2.M2 | Listado: carga, datos, vacío, búsqueda en cliente, filtro por estado del esquema, error | `ng test` dirigidos | `evidencia/specs-dirigidos.txt` 185/185 |
| H1.S2.M3 | Detalle: título con el nombre, datos, cupos con alias / libre / organiza, sin cupos, error | ídem | PASS |
| H1.S2.M4 | Subgrupo «Grupos y admisiones» en Operación (Operación sigue abriendo en Admisiones); `ver:grupos`; tablero; prueba negativa SOPORTE | menú + guardias + matriz + layout | PASS |
| H1.S3.M1 | Regresión completa del backoffice | `npx ng test --include='src/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'` | **183 archivos, 1294/1294** (`evidencia/regresion-backoffice.txt`) |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S3.M2 | TODO | CI del PR (Postman regenerado incluido) |
| H1.S3.M3 | TODO | Despliegue de TEST y una llamada con token: en TEST hay 2 grupos `ABIERTO_A_INSCRIPCION` |

## Evidencia
```text
grupos webTest:          tests=123 failures=1  (GruposBandejaWebTest.sinParametros, preexistente)
grupos integrationTest:  tests=271 failures=6  (BandejaDeAdmisionesRepositorioTest 6/6, preexistente)
GruposDeBackofficeWebTest 9/9 · GruposDeBackofficeRepositorioTest 4/4
backoffice regresión completa: Test Files 183 passed (183) · Tests 1294 passed (1294)
yarn typecheck → EXIT 0
```

## No cubierto
- Ejercicio en TEST; prueba visual en tres viewports y dos temas; spec a11y de las pantallas nuevas.
- Bitácora de lectura (regla 90.2.7): la vista no trae datos financieros de una persona, por eso no se registra la lectura. Confirmar con Pablo.

## Desvíos del plan
- **Regresiones atrapadas antes del PR** (por correr las suites completas):
  - El ayudante nuevo rompía 90/114 tests web de `GruposController` (cada test mockea a mano sus ayudantes): se agregó el mock en los 13 tests.
  - `nucleo/secciones.spec.ts` enumeraba las secciones de un token con `AUDITORIA_LEER`; ahora incluye `ver:grupos`, que es la consecuencia buscada (lectura). Se actualizó la expectativa y el título.
  - `nucleo/menu.spec.ts` fija que Operación abra en Admisiones: «Grupos» quedó al final del subgrupo para no cambiar ese comportamiento.
- Los estados del filtro se corrigieron contra `ck_grupo_estado` (`sql/10_tablas/02_grupos_turnos/grupo.sql:37`, 9 estados) y las periodicidades contra `ck_grupo_periodicidad` (se agregó BIMENSUAL); la primera versión tenía valores no verificados.

## Riesgos residuales
- Preexistentes en `origin/test`, no tocados: `GruposBandejaWebTest.sinParametros` (el generador aplica `default: 200`) y `BandejaDeAdmisionesRepositorioTest` (6/6, `fecha_resolucion` con tipo texto en el fixture).
- El listado trae hasta 200 grupos; la búsqueda filtra sobre eso.
- `fechaInicio` sale tipada sin `null` en el cliente generado aunque el contrato admite `null`.

## Decisiones y ambigüedades
- Permiso: `ADMIN_PLATAFORMA` o `AUDITORIA_LEER` (patrón de las lecturas del backoffice); no se creó un permiso nuevo.
- Seguridad (90.6): amenaza = exponer datos de participantes o abrir la consulta a quien no opera la plataforma; control = permiso en el controlador + respuesta mínima; pruebas negativas (participante, organizador, soporte, sin sesión) en verde.
