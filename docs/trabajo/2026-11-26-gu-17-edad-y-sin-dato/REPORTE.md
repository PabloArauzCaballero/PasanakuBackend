> **AVANCE: 8 / 8 — 100,0 %.** (#543 fusionado en `dev` con CI verde en `692558f9c`)

# Reporte — GU-17 · edad mínima 18 (A9) y operadores «sin dato» (A8)

- Fecha: 2026-11-26 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-c-credencial` (desde `origin/dev` tras #538)
- Decisiones de Pablo (vía sesión principal): A9 → 18 años, código estable, test de límite. A8 → marcar «sin dato» donde vale exactamente 1990-01-01, migración con nombre explícito, reversible, sin tocar la base a mano; el backoffice pide completar la real.
- Peldaño de evidencia alcanzado: **REGRESSION_VERIFIED para el alcance de #543** (unitarios, web y SQL real local + CI completa verde, incluida la integración contra base); la UI se verificó contra backend simulado declarado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Edad mínima 18 (`EDAD_MINIMA_DEL_OPERADOR`): 18 hoy pasa; 17 años 364 días, futura y ausente se rechazan; borde del 29 de febrero | `./gradlew :servicios:identidad:test --tests '*CU08AltaConFechaReal*'` | BUILD SUCCESSFUL (6 pruebas) |
| H1.S1.M2 | AP-CU08-13/14/15 y criterios en CU-08 | `verificar_criterios.py; verificar_boveda.py` | «Sin divergencias» / TODO OK |
| H1.S2.M1 | Modal de alta: error por campo (futura, menor de 18) y mapeo del servidor; la regla vive en un solo lugar (`dominio/fecha-de-nacimiento.ts`) | `npx ng test --include='src/app/rutas/sistemas/**/*.spec.ts'` | 50 archivos, 224 pruebas pasan |
| H2.S1.M1 | `usuario.fecha_nacimiento` nullable (`<<NULL>>`) + R-SEG-18: solo el personal interno puede estar sin dato | `psql` (evidencia/sql-r-seg-18.txt) | participante sin fecha → `ERROR: R-SEG-18`; operador sin fecha → COMMIT; UPDATE a NULL de un participante → rechazado |
| H2.S2.M2 | Backoffice: aviso «Falta la fecha de nacimiento» + modal con paso de seguridad; capturas 390/834/1440 × claro/oscuro miradas (aviso, error de fecha futura, alta con menor de 18) | `yarn typecheck`, `yarn lint`, `ng test` sistemas (224), `npx playwright test e2e/visual-fecha-operador.e2e.ts --workers=1 --project=chromium` | todo OK; `18 passed (52.3s)`; capturas móvil y escritorio en `evidencia/capturas/` (tablet tomada y mirada, no versionada) |
| H2.S3.M1 | Gates y deriva sobre el head | `spotlessCheck`, `testBarrido`, `contractTest`, `verificar_boveda/criterios/seguridad/carriles`, `inventario_endpoints --check`, `generar_ddl`/`extraer_sql`/`generar_postman`/`catalogo_de_datos --escribir` sin diff | todo en verde / sin deriva |
| H2.S2.M1 | Completar la fecha (backend): `PATCH /usuarios-internos/{id}` con `fechaNacimiento` valida (AP-CU08-13/14), escribe con la precondición «solo si está NULL» (AP-CU08-15 si ya hay), exige step-up y deja bitácora `OPERADOR_FECHA_COMPLETADA` | CI de #543, job «Los cinco corredores» (incluye `CU08CompletarFechaTest` y `CU08FechaDeNacimientoDeLaBaseTest`) | `success` en el head `692558f9c` (18 checks verdes o saltados; salida en la corrida 45751030502) |
| H2.S1.M2 | Corrección generada `sql/15_infra/correccion_gu17_fecha_ficticia.sql` (acotada por corte, de una sola vez, con reversa) | `psql` (evidencia/sql-a8.txt, sql-reaplicacion.txt) | 1.ª corrida: 1 fila (solo el operador previo con 1990-01-01); 2.ª: `UPDATE 0`; reversa restituye; `aplicar.sql` dos veces sin errores |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| A10 · migraciones versionadas | DECIDIDA (2026-11-26, sesión principal) | Alcanza la corrección generada por `generar_ddl.py`. No se abre ADR de Flyway/Liquibase: la regla 97.1 pide una sola fuente de verdad del esquema y el repo ya la declara (modelo → DDL generado); sumar Flyway sería meter la segunda a la vez. Coherente con `docs/operacion/schema-changes.md`, con corte y reversa probados (`evidencia/sql-*.txt`) |
| Bitácora de la corrección de datos | declarado | La corrección no deja asiento por operador; si se exige, hace falta decidirlo con Pablo |

## Evidencia
`evidencia/sql-a8.txt`, `evidencia/sql-r-seg-18.txt`, `evidencia/sql-reaplicacion.txt` (PostgreSQL 16 local, datos sintéticos).

## No cubierto
- La integración contra base de la alta, la edad y completar fecha se ejecutó solo en CI (verde); no se corrió en la máquina de desarrollo (sin Docker).
- El corte `2026-11-26 03:17:43+00` es el instante en que #538 entró a `dev`; un entorno que despliegue más tarde no cambia nada, pero un operador dado de alta con el alta VIEJA después de ese corte conservaría 1990-01-01 sin marcar (no debería existir: el alta nuevo exige fecha).
- `sql-a8.txt` se capturó antes de agregar `SET CONSTRAINTS` a la corrección; `sql-reaplicacion.txt` prueba la versión final.

## Desvíos del plan
Se descubrió que `aplicar.sql` corre en una transacción: la corrección dejaba eventos diferidos del trigger de R-SEG-18 y rompía el siguiente `ALTER TABLE`; se agregó `SET CONSTRAINTS ALL IMMEDIATE/DEFERRED` alrededor del UPDATE (probado en `sql-reaplicacion.txt`).

## Riesgos residuales
- `SET CONSTRAINTS ALL DEFERRED` deja diferidas las demás restricciones diferibles por el resto de la transacción de `aplicar.sql` (solo DDL, sin datos).
- Edad en el borde del 29 de febrero: se rechaza hasta el 1 de marzo (supuesto conservador, A12; confirmar).

## Decisiones y ambigüedades
- A10: decidida (ver Pendiente): la corrección generada con corte y reversa alcanza; sin ADR de Flyway. A11: corte = instante de #538. A12: 29 de febrero cumple el 1 de marzo.
