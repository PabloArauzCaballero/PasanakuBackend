# Franja C — Cumplimiento (`rutas/cumplimiento/**`)

> **AVANCE: 0 / 14 — 0 %.** (se recalcula al cerrar)

- Rama: `claude/franja-c` · worktree `/home/user/wt-c` · servidor `127.0.0.1:4303`
- Hallazgos del descubrimiento:
  - `cumplimiento.yaml` no tiene ninguna operación PUT/PATCH/DELETE: en toda la sección **no hay Editar ni Eliminar** posibles; las acciones de fila son de dominio (Decidir, Aprobar y enviar, Responder) o Ver.
  - Las 8 bandejas paginan por cursor en el servidor (`limite` ≤ 100, sin total, sin parámetro de búsqueda).
  - Dos navegaciones idénticas: `ap-pestanas-de-seccion` (layout) y `ap-navegacion-de-cumplimiento` (marco).
  - Botón muerto: «Confirmar decisión» de `casos/:casoId` solo borraba el borrador local (no hay operación).

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | `crearBandeja` por cursor (reemplaza `observarBandeja`) | filtros y filas por página viajan al servidor; 403 → denegado; error con traza; la última carga gana | spec `bandeja-por-cursor.spec.ts` | TODO |
| H3.S1.M2 | `ap-listado-de-bandeja` (barra + exportar + tabla + paginación + estados; reemplaza `ap-estado-de-bandeja`) | anatomía del kit con los 5 estados | spec + a11y | TODO |
| H3.S2.M1 | Alertas de monitoreo | buscador, filtros estado/severidad, CSV/JSON, selección → Abrir caso | spec 3 niveles + navegador | TODO |
| H3.S2.M2 | Casos de investigación | acción de fila «Decidir» ícono+nombre | spec + navegador | TODO |
| H3.S2.M3 | Reportes UIF | acción de fila «Aprobar y enviar» solo en GENERADO; sin Eliminar | spec + navegador | TODO |
| H3.S2.M4 | Requerimientos de autoridad | acción «Responder» solo si no respondido/archivado | spec + navegador | TODO |
| H3.S2.M5 | Colas de lectura (continuidad, actas, eventos, incidentes) | kit en las 4; Registrar prueba en continuidad | spec + navegador | TODO |
| H3.S3.M1 | Verificación de identidad con `crearListado` | buscador nombre/teléfono/correo/documento, filtros, paginación, CSV/JSON tal como se ve | spec + navegador | TODO |
| H3.S4.M1 | Caso (`casos/:casoId`): botón muerto | «Confirmar» hace algo real o no se dibuja | spec | TODO |
| H3.S4.M2 | Íconos y layout en fichas (habilitación, indisponible, caso) | cada botón con ícono + texto | spec | TODO |
| H3.S4.M3 | Marco: sin navegación duplicada, a todo el ancho, ícono por vista | una sola navegación | spec | TODO |
| H3.S5.M1 | Recorrido en navegador (aceptado/límite/error + descargas) | requests en la red y archivos verificados | script `recorrido.mjs` | TODO |
| H3.S5.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas | sin desbordes | Read de los PNG | TODO |
| H3.S5.M3 | Comandos de cierre | tsc, lint, verificar_frontend, tests verdes | salida literal | TODO |
