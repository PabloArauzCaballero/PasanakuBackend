# Franja F — Publicidad, Tablero y Admisiones

- Fecha: 2026-10-26 · Rama: `claude/franja-f` (worktree `/home/user/wt-f`) · Base: `c9ac3966`
- Resultado observable: en Publicidad, Tablero y Admisiones cada listado tiene buscador multicampo, filtros, exportar CSV/JSON de lo filtrado, filas por página / ir a / anterior / siguiente y acciones de fila con ícono + nombre; los botones que no hacían nada (alta de campaña con cuenta inventada, aprobación sin respuesta, liquidación con errores mudos) o se arreglan o se quitan.
- Kill-test: en Anunciantes, escribir un texto sin coincidencias muestra «Quitar filtros» y «Descargar CSV» baja solo el encabezado.

## Alcance
- IN: `apps/backoffice/src/app/rutas/{publicidad,tablero,admisiones}/**` (sin `*.routes.ts`) y sus specs/e2e.
- OUT: `packages/ui`, `nucleo`, `layout`, rutas, contratos.

## Microtareas
(estado actualizado al cierre — ver tabla al final)
