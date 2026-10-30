# Reporte — La votación del traspaso dice a quién entra · contraseña una sola vez

> **AVANCE: 7 / 7 — 100 %.**

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/votacion-con-entrante`
- Peldaño: `TESTED` (Postgres real con Testcontainers; app con backend simulado). Sin ejercicio en el servidor TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | `acuerdo.entrante_participante_id UUID NULL` en `02_grupos_turnos.puml`; `generar_ddl.py` ahora emite `15_infra/columnas_agregadas.sql` (`ADD COLUMN IF NOT EXISTS`, solo columnas opcionales listadas en `COLUMNAS_AGREGADAS`) y el comentario de la columna después del `ADD` | `psql -f sql/aplicar.sql` ×2 sobre base existente, ×1 sobre base nueva | exit 0, 0 errores, columna presente (nullable uuid) en las tres |
| H1.M2 | `POST /acuerdos` con `TRASPASO_CUPO` exige `referenciaAfectadaId` y `usuarioEntranteId` (participante del grupo); si no, AP-CU63-06 | `gradle :servicios:grupos:webTest` | 130/130 |
| H1.M3 | CU-64 solo acepta el acuerdo si su `entrante_participante_id` es quien entra; un acuerdo sin persona no autoriza a nadie | `gradle :servicios:grupos:integrationTest` | 285/285; mutante (quitar el filtro) → 2 pruebas en rojo |
| H1.M4 | `GET /acuerdos` devuelve `usuarioEntranteId` (lectura con rol de sistema: la RLS solo deja ver la fila propia de `participante`) | idem | PASS |
| H1.M5 | La app pide la votación con `usuarioEntranteId`; la aprobada solo habilita «Traspasar cupo» para esa persona | `flutter test test/pasanaku/pantalla_traspaso_test.dart` | 15/15 |
| H2.M1 | Registro: un solo campo «Contraseña» con «Mostrar contraseña» | `flutter test test/identidad` · goldens regenerados y mirados (claro/oscuro, 360 y 600) | PASS |
| H2.M2 | Regresión | suite app · `verificar_frontend.py movil` · `verificar_criterios` · `verificar_boveda` | 1147 en verde · TODO OK · sin divergencias · TODO OK |

## A medias
Ninguna.

## Pendiente
Ninguna dentro del alcance.

## No cubierto
- Servidor TEST: la columna llega por `aplicar.sql` en el próximo despliegue de la imagen del esquema; no lo vi aplicado allá.
- Acuerdos de traspaso aprobados ANTES de este cambio (sin persona) dejan de servir: hay que volver a votar.

## Desvíos del plan
- Se reemplazó la prueba «si las dos no coinciden, no avanza» por «la contraseña se pide una vez y se puede ver lo escrito»: el requisito cambió, no se debilitó.

## Riesgos residuales
- `columnas_agregadas.sql` no es un sistema de migraciones: solo agrega columnas opcionales.

## Decisiones y ambigüedades
- Persona entrante como `usuarioEntranteId` en el contrato (igual que `traspasarCupo`); en la base, `participante.id`.
