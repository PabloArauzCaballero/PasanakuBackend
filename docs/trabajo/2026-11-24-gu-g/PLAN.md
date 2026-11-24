# Plan — Lote G · CU de gobierno de participantes (solo documento)

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (solo `docs/` y `planes/`) · Predecesor: [INVENTARIO de gobierno de usuarios](https://github.com/pabloarauzcaballero/pasanakubackend/blob/claude/modest-newton-4ckcid/docs/trabajo/2026-11-24-gobierno-de-usuarios/INVENTARIO.md), decisión P-11
- Rama: `claude/gu-g-cu-participantes` · PR contra `dev` (no se mergea)
- Resultado observable: Pablo puede leer en `docs/CasosDeUso/` un CU **borrador** que especifica el directorio y la ficha del participante en el backoffice y la suspensión/bloqueo de su cuenta por seguridad con debido proceso, con toda regla no respaldada marcada «PROPUESTA — requiere aprobación de Pablo» y las preguntas abiertas listadas, sin que la bóveda deje de validar.
- Kill-test: `python3 scripts/verificar_boveda.py` no termina en `TODO OK`, o el CU presenta como hecho una regla económica/plazo sin fuente.

## Alcance
- IN: un CU nuevo (`CU-134`); su fila en `_CasosDeUso.md` y `total_casos`; las cifras «112 casos de uso» en `planes/` que el verificador exige actualizar; PLAN/REPORTE/evidencia de este lote.
- IN (agregado durante la ejecución, regla 20.6.6): la línea «Ver también» recíproca en CU-07, 17, 27, 44, 52, 55 y 93 (`verificar_boveda.py` exige reciprocidad), y el catálogo de datos regenerado (`docs/catalogo-de-datos.md`, `servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json`), que la CI exige al día tras agregar un CU que nombra tablas.
- OUT: código, OpenAPI, DDL, semillas, `docs/Restricciones.md` (las restricciones nuevas quedan **propuestas** dentro del CU, no se dan de alta hasta que Pablo apruebe), entidades nuevas del modelo, archivos de los lotes A/B/E.
- Ambigüedades registradas:
  - Número del CU: el siguiente libre en la bóveda es CU-133, pero `CU133*` y `CU136*` ya se usan como prefijo de pruebas en `servicios/entregas` (y `verificar_criterios.py` toma la clase `CU<NN>` como "implementado"). Supuesto: se usa **CU-134**. Confirmar con Pablo.
  - Servicio dueño: `identidad` (es dueño de `usuario.estado`, `bloqueo_cuenta`, `restriccion_usuario`). Las sanciones de participante viven en `garantia` (`sancion` exige `registro_id` de incumplimiento). Supuesto: el expediente de seguridad es de `identidad`. Confirmar con Pablo.
  - Toda regla económica (qué pasa con aportes/entregas en curso) queda como DECISION_REQUIRED.

## H1 — CU borrador de gobierno de participantes listo para aprobación
**CA:** Dado el CU-134 en la bóveda, cuando Pablo lo lee, entonces encuentra causales cerradas, expediente, notificación, plazo de descargo, decisión motivada, FIRME, apelación por otra persona, efectos sobre grupos/dinero, permisos y segregación, datos mínimos con rastro de lectura, y cada regla sin fuente marcada PROPUESTA con opciones.
**DoD:** `verificar_boveda.py` → `TODO OK`; `verificar_criterios.py` → exit 0 (CU-134 pendiente); `verificar_seguridad.py` y `verificar_carriles.py` exit 0; PR a `dev` con CI en verde.
**Estado:** EN CURSO — falta H1.S3.M3

### H1.S1 — Relevamiento de lo existente
**CA:** Dado el pedido, cuando se escribe el CU, entonces cada pieza existente se referencia y no se duplica.
**DoD:** tabla de hallazgos en el REPORTE con rutas.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Relevar estados, tablas y CU existentes (CU-04, 07, 08, 17, 25, 27, 44, 52, 55, 70, 93; `usuario`, `bloqueo_cuenta`, `restriccion_usuario`, `sancion`, `apelacion_sancion`, `politica_sancion`, `registro_acceso_datos`; R-SEG, R-GAR, R-ORG-05) | Hallazgos con ruta en el REPORTE | `grep -n "ck_usuario_estado\|ck_bloqueo_cuenta_motivo\|ck_restriccion_usuario_tipo" sql/10_tablas/01_identidad_usuarios/*.sql` → los CHECK citados | HECHO |

### H1.S2 — Escribir el CU
**CA:** Dado el CU-134, cuando se lo valida, entonces tiene todas las secciones de la plantilla y los wikilinks resuelven.
**DoD:** `python3 scripts/verificar_boveda.py` → `TODO OK`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Escribir `CU-134 …md` con la plantilla de `caso-de-uso`, propuestas marcadas y preguntas para Pablo | El archivo existe y no hay wikilink roto | `python3 scripts/verificar_boveda.py \| grep -E "wikilink\|CU-134\|TODO OK\|FALLA"` | HECHO |
| H1.S2.M2 | Agregar la fila al índice `_CasosDeUso.md` y subir `total_casos` a 113 | Índice y total coherentes | `python3 scripts/verificar_boveda.py \| tail -3` → `TODO OK` | HECHO |
| H1.S2.M3 | Actualizar las cifras «112 casos de uso» que el verificador marque en `planes/` | Sin cifras desfasadas | `python3 scripts/verificar_boveda.py \| grep -A2 "CIFRAS"` → OK | HECHO |

### H1.S3 — Gates y entrega
**CA:** Dado el PR, cuando corre la CI, entonces queda en verde.
**DoD:** salidas pegadas en `evidencia/` y checks del PR en verde.
**Estado:** EN CURSO — falta H1.S3.M3 (CI del PR)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Correr los verificadores de bóveda locales del workflow `boveda-y-esquema.yml` | Todos exit 0 | `verificar_seguridad.py; verificar_boveda.py; verificar_carriles.py; verificar_criterios.py` → exit 0 | HECHO |
| H1.S3.M2 | REPORTE.md con preguntas para Pablo | Tres secciones + preguntas | `python3 .claude/hooks/report_gate.py --self-test` y lectura del archivo | HECHO |
| H1.S3.M3 | Commit, push y PR a `dev` con CI en verde | PR abierto, checks verdes | estado de checks del PR | EN CURSO — commit y PR en curso |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Presentar como regla algo inventado | Pablo aprueba algo sin fuente | Todo lo no respaldado va «PROPUESTA — requiere aprobación de Pablo» con opciones |
| El verificador exige entidades/restricciones existentes | CI rojo | Solo wikilinks a entidades existentes; lo nuevo en `código` y marcado propuesto |
| Deber de reserva (CU-44) choca con "notificar al afectado" | Tipping-off | Causal de inteligencia financiera fuera de este CU: se deriva a CU-44 |
