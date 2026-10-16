> **AVANCE: 16 / 16 — 100,0 %.**

# Reporte — A6 · tarifario y mensajería para el backoffice (carril A, subagente)

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `codex/backoffice-a6-tarifas-mensajeria` (commit `55e966c4`, desde `origin/codex/backoffice-dominio-20261015` @ `70747767`), integrada en la rama del carril con el merge `49299219`.
- Peldaño de evidencia alcanzado: **TESTED** en ambas áreas (integración contra PostgreSQL real + MockMvc + regresión del módulo en verde), **re-verificado por el agente principal** sobre el árbol integrado (`../evidencia/A6-01-integracion-tarifas-mensajeria.txt`). No es VERIFIED: no se ejercitó el camino HTTP real por el gateway con token de identidad, ni hay UI que llame a estos endpoints.
- Alcance: tres listados de lectura, sin escritura, sin cambio de esquema y sin permiso nuevo. La viñeta A6 de tarifario y mensajería **no** queda completa (ver Pendiente).
- Nota: el subagente no pudo escribir este archivo (el arnés lo bloquea); el agente principal lo transcribió de su entrega.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Contrato `GET /tarifas/tarifarios` + `AP-CU34-05` | `./gradlew :servicios:tarifas:compileJava --no-build-cache` | PASS EXIT=0 |
| H1.S1.M2 | Repositorio, consulta, mapeo y método del controlador (`TARIFARIO_PUBLICAR` + alternativo `AUDITORIA_LEER`) | `... spotlessApply compileJava` | PASS EXIT=0 |
| H1.S2.M1 | `CU34TarifariosTest` en tres niveles | `integrationTest --tests bo.aportaya.tarifas.CU34TarifariosTest` | PASS 3/3 · `evidencia/tarifas-CU34TarifariosTest.log` |
| H1.S2.M2 | `TarifariosListadoWebTest` 200/200 alt/403/401/422/400 | `:servicios:tarifas:webTest` | PASS 6/6 (total 34/0) |
| H1.S2.M3 | Ficha CU-34 actualizada | `python3 scripts/verificar_criterios.py --servicio tarifas` | PASS |
| H2.S1.M1 | Contratos de plantillas y envíos + `AP-CU80-05`, `AP-CU83-06` | `:servicios:notificaciones:compileJava` | PASS EXIT=0 |
| H2.S1.M2 | Repositorio, consulta, mapeo y dos métodos con `AUDITORIA_LEER` | `... spotlessApply compileJava` | PASS EXIT=0 |
| H2.S2.M1 | `CU83EnviosTest` (estado escrito por el despacho real de CU-83) | `integrationTest --tests ...CU83EnviosTest` | PASS 3/3 |
| H2.S2.M2 | `CU80PlantillasTest` | idem | PASS 3/3 |
| H2.S2.M3 | `MensajeriaAuditoriaWebTest` (la respuesta no contiene destinatario, contenido, cuerpo ni asunto) | `:servicios:notificaciones:webTest` | PASS 6/6 (total 36/0) |
| H2.S2.M4 | Fichas CU-80 y CU-83 actualizadas | `verificar_criterios.py --servicio notificaciones` | PASS |
| H3.M1 | Regresión de tarifas | ver Evidencia | PASS |
| H3.M2 | Regresión de notificaciones (`--rerun`) | ver Evidencia | PASS |
| H3.M3 | Gates de criterios, bóveda y seguridad | `python3 scripts/verificar_*.py` | exit 0 los tres |
| H3.M4 | Postman regenerado, solo en los archivos de estos dos servicios | `python3 scripts/generar_postman.py` | exit 0 |
| H3.M5 | Commit y push | `git push -u origin codex/backoffice-a6-tarifas-mensajeria` | `* [new branch]` |

Endpoints: `GET /tarifas/tarifarios` (`listarTarifarios`, CU-34, `TARIFARIO_PUBLICAR` o `AUDITORIA_LEER`, `AP-CU34-05`) · `GET /notificaciones/plantillas` (`listarPlantillas`, CU-80, `AUDITORIA_LEER`, `AP-CU80-05`) · `GET /notificaciones/envios` (`listarEnvios`, CU-83, `AUDITORIA_LEER`, `AP-CU83-06`).

## A medias

ninguna

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Simulación de impacto como operación propia (CU-34, `simularSobreHistoria`) | DECISION_REQUIRED | Definir qué historia y qué ventana se recalculan; hoy el controlador manda `"{}"`. Decide: negocio / tarifas |
| Enrutamiento de proveedores, consentimiento y edición/aprobación de plantillas | DECISION_REQUIRED | Casos de uso de escritura y un permiso de mensajería sembrado. Decide: dueño del proyecto / seguridad |
| Pantallas del backoffice que consuman los tres listados | TODO (carril B) | — |

## Evidencia

```text
tarifas (regresión del subagente): BUILD SUCCESSFUL · integrationTest 106/0 · test 7/0 · testBarrido 2/0 · webTest 34/0 · contractTest NO-SOURCE
notificaciones (regresión con --rerun): BUILD SUCCESSFUL in 33s · integrationTest 66/0 · test 5/0 · testBarrido 2/0 · webTest 36/0 · contractTest NO-SOURCE
verificar_criterios.py → exit 0 «… Sin divergencias» · verificar_boveda.py → exit 0 «TODO OK»
verificar_seguridad.py → exit 0 «0 bloquean … TODO OK · 6 aviso(s)» (ninguno en tarifas ni notificaciones) · generar_postman.py → exit 0

re-verificación del agente principal tras el merge 49299219 (--rerun-tasks): mismos conteos, 0 fallas
→ ../evidencia/A6-01-integracion-tarifas-mensajeria.txt
```

Archivos en `evidencia/` de esta carpeta: regresion-tarifas.log, regresion-tarifas-webTest-rerun.log, regresion-notificaciones.log, conteo-*.txt, tarifas-*.log, notificaciones-*.log, gate-*.log, postman.log, contar_xml.py.

### Gate de seguridad (regla 90.6)

1. **Amenaza:** que la lista de auditoría exponga teléfono, correo o el texto del mensaje (regla 90.2), o que alguien sin permiso vea precios y expedientes (BFLA).
2. **Control:** `AuditoriaDeMensajeriaRepositorio` nombra las columnas una por una y no lee `destinatario`, `contenido_enviado`, `id_mensaje_proveedor`, `clave_idempotencia`, `cuerpo`, `asunto`, `botones`, `url_encabezado_media` ni `payload_crudo`. `@Permiso` en cada método. SQL parametrizado con casts, sin concatenación.
3. **Test:** `MensajeriaAuditoriaWebTest` (403 con `GRUPO_ADMINISTRAR`, 401, el 200 sin campos personales); `CU80PlantillasTest` y `CU83EnviosTest` verifican por reflexión que los records no tienen esos componentes; `TarifariosListadoWebTest` 403/401; `SeguridadWebTest` de ambos servicios en verde.
4. **Resultado:** 0 failures.
5. **Riesgo residual:** `notificacionId` y `proveedorId` son identificadores; quien tenga acceso a la base puede cruzarlos hasta la persona. No hay bitácora de lectura específica para estos listados.

## No cubierto

- Camino HTTP real por el gateway con token de identidad.
- Índices y plan de consulta de los cursores sobre `version_plantilla(vigente_desde, id)` y `envio_notificacion(encolado_en, id)`.
- `evento_entrega_mensaje` no tiene escritor en el código del servicio: `ultimoAcuse` solo se probó con filas insertadas por la prueba.
- `contractTest` sin fuentes (NO-SOURCE) en ambos servicios.

## Desvíos del plan

- `CU83EnviosTest` falló en la limpieza en su primera corrida (TEST_BUG): la fixture compartida no borra `evento_entrega_mensaje` y la clave foránea `fk_evento_entrega_mensaje_envio_id` bloqueaba. La prueba ahora borra sus acuses antes de limpiar. Re-corrida: 3/3.
- Se agregó `evidencia/contar_xml.py`, que no estaba previsto.
- El REPORTE.md no se pudo escribir desde el subagente; lo transcribió el agente principal.

## Riesgos residuales

- `catalogo.tarifario` no se limpia entre pruebas: los tests filtran por un código único; un test futuro sin filtro vería datos de otras pruebas.
- `costo` se lee con `setScale(4, UNNECESSARY)`: si la escala de la columna cambia, la consulta falla en lugar de redondear en silencio.

## Decisiones y ambigüedades

1. Delta de la simulación sin moneda (no se infiere de `moneda_base`). Confirmar con: dueño del modelo de tarifas.
2. Solo `AUDITORIA_LEER` para mensajería. Confirmar con: dueño del proyecto / seguridad.
3. Sin filtro se devuelven todos los estados. Confirmar con: operación.
4. Moneda del envío como texto ISO-4217 libre (`^[A-Z]{3}$`), porque `CHAR(3)` no tiene CHECK. Confirmar con: dueño del modelo de notificaciones.
5. Hallazgo fuera de alcance: `emitirNotificacion` usa `@Permiso("SOPORTE")`, pero `SOPORTE` es un rol, no un permiso. Dueño: notificaciones / seguridad.
