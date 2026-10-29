# Plan — Cerrar los pendientes de Sistemas

- Fecha: 2026-10-29 · Repos afectados: Pasanaku (backoffice + auditoria, aportes, notificaciones, tarifas, comun-mensajeria) · Rama: `claude/sistemas-pendientes-20261029` sobre `origin/test` (`c5166653`)
- Predecesor: [`2026-10-28-sistemas-datos-reales-y-menu-fijo`](../2026-10-28-sistemas-datos-reales-y-menu-fijo/REPORTE.md)
- Resultado observable: con un `ADMIN_PLATAFORMA`, las pestañas de Sistemas que hoy dicen «fuente no disponible» muestran datos que salen de tablas reales del servicio dueño: Revisión de accesos, Incidentes, Webhooks, Proveedores y Outbox. Despliegues y Respaldos quedan con una propuesta registrada, porque ninguna tabla los guarda.
- Kill-test: abrir `/sistemas/incidentes` (o `webhooks`, `proveedores`, `outbox`, `accesos`) y ver «fuente no disponible» → NO está hecho.

## Hallazgos
| Pestaña | Fuente real encontrada | Endpoint hoy |
|---|---|---|
| Revisión de accesos | `identidad` · `GET /usuarios-internos` (`UsuarioInternoResumen`: roles, estado, segundo factor, bloqueo) | **existe** |
| Incidentes | `auditoria.incidente_operativo` (`sql/10_tablas/09_auditoria_reportes/incidente_operativo.sql`) | no |
| Webhooks | `aportes.webhook_pasarela` + `notificaciones.evento_entrega_mensaje` | no (solo los `POST` de recepción) |
| Proveedores | `aportes.proveedor_pago`, `notificaciones.proveedor_mensajeria`, costos en `tarifas.costo_proveedor_operacion` (`proveedor_id` → M3) | no (solo `POST /pagos/proveedores`) |
| Outbox | `<esquema>.evento_dominio` de cada servicio (`sql/15_infra/mensajeria.sql`); Prometheus solo tiene `aportaya.outbox.publicaciones_total` | no |
| Despliegues | ninguna tabla; el despliegue de TEST no deja registro | no |
| Respaldos | `pg_dump` previo al despliegue en el VPS (`despliegue/TEST.md:505`), sin registro consultable | no |

Patrones a copiar: contrato primero (`openapi/*.yaml` → interfaz generada), controlador con `@Permiso("TABLERO_SISTEMAS_VER") @PermisoAlternativo({"AUDITORIA_LEER"})` (`IndicadoresController.java:135`), repositorio jOOQ con SQL plano (`DisponibilidadRepositorio.java`), lectura en `datos.conContexto` (`ConsultarBitacora.java`); en el front, adaptador real del puerto registrado en `proveedor-fuentes.compartido.ts` (como `dominio/salud-de-servicios.ts`).

## Alcance
- IN: los contratos, controladores, casos de uso y repositorios nuevos de solo lectura; los adaptadores del front y sus pantallas; los tests; el despliegue a TEST si el procedimiento de `despliegue/TEST.md` es ejecutable desde acá.
- OUT: escribir en tablas ajenas; leer el esquema de otro servicio (regla 98.1); interruptores (no hay modelo); cambiar el procedimiento de despliegue.
- Ambigüedades:
  1. «Incidentes» de Sistemas = `incidente_operativo` (operación), no `incidente_seguridad` (cumplimiento, regulatorio). Supuesto: operativo. Confirmar con Pablo.
  2. Outbox: la pantalla pasa de «lista de mensajes» a «por servicio: pendientes, fallidos, publicados», porque leer filas de 15 esquemas desde uno viola 98.1. Confirmar con Pablo.
  3. Despliegues y Respaldos: `DECISION_REQUIRED`. Hace falta decidir quién registra (el script de despliegue escribiendo en una tabla de auditoria, o Coolify). Se deja la propuesta; a quién: Pablo.

## H1 — Revisión de accesos con datos de identidad
**CA:** dado un `ADMIN_PLATAFORMA`, cuando abre Revisión de accesos, entonces ve una fila por persona interna y rol, con su departamento, estado y si tiene segundo factor. Si identidad falla, ve el error.
**DoD:** spec del adaptador en los tres niveles + spec de pantalla · **Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Adaptador `puertoAccesos` sobre `GET /usuarios-internos` | correcto (2 personas, 3 roles → 3 filas), límite (persona sin roles → 1 fila «sin rol»), inválido (500 → rechazo traducido) | `ng test …/dominio/accesos*.spec.ts` | HECHO (`evidencia/H1-accesos.txt`) |
| H1.S1.M2 | Registrarlo en `proveedor-fuentes.compartido.ts` y mostrar estado y segundo factor | spec de pantalla PASS | `ng test …/accesos/*.spec.ts …/dominio/*.spec.ts` | HECHO (192/192) |

## H2 — Incidentes desde `auditoria.incidente_operativo`
**CA:** la pantalla lista los incidentes reales, del más reciente al más viejo, con severidad, estado y sistema afectado; paginado.
**DoD:** test de integración contra Postgres real + web test con autorización negativa + spec del front · **Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Contrato `GET /indicadores/operacion/incidentes` (paginado, orden `detectado_en desc, id`) | el contrato compila y genera la interfaz | `./gradlew :servicios:auditoria:compileJava` | HECHO |
| H2.S1.M2 | Repositorio + caso de uso + controlador | integración: 3 incidentes → orden correcto; límite 0 filas; `limite` fuera de rango → 400 | `./gradlew :servicios:auditoria:test --tests '*Incidente*'` | HECHO (evidencia/H2-incidentes-integracion.txt) |
| H2.S1.M3 | Autorización negativa | sin token 401; sin permiso 403 | web test | HECHO (evidencia/H2-auditoria-regresion.txt) |
| H2.S2.M1 | Adaptador del front + pantalla | spec en tres niveles | `ng test` | HECHO (evidencia/H2-incidentes-front.txt) |

## H3 — Webhooks desde aportes y notificaciones
**CA:** la pantalla lista los últimos webhooks recibidos de la pasarela (aportes) y de mensajería (notificaciones), con dirección, origen, estado y fecha; nunca el `payload_crudo` ni la firma.
**DoD:** integración en cada servicio + autorización negativa + spec del front · **Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | aportes `GET /pagos/webhooks-recibidos` | integración PASS; respuesta sin payload ni firma | `./gradlew :servicios:aportes:test --tests '*Webhook*Recibidos*'` | HECHO (evidencia/H3-aportes.txt) |
| H3.S1.M2 | notificaciones `GET /notificaciones/webhooks-recibidos` | ídem | `./gradlew :servicios:notificaciones:test --tests '*WebhooksRecibidos*'` | HECHO (evidencia/H3-notificaciones.txt) |
| H3.S2.M1 | Adaptador del front que une ambas fuentes; si una falla, se muestra la otra con aviso | spec en tres niveles | `ng test` | HECHO (evidencia/H3-H4-front.txt) |

## H4 — Proveedores desde aportes, notificaciones y tarifas
**CA:** la pantalla lista los proveedores de pago y de mensajería activos, con categoría y el costo real del último período cerrado (de tarifas), con su moneda; nunca la referencia de credenciales.
**DoD:** integración en los tres servicios + spec del front · **Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | aportes `GET /pagos/proveedores` | sin `referencia_credenciales` | test aportes | HECHO (evidencia/H3-aportes.txt) |
| H4.S1.M2 | notificaciones `GET /notificaciones/proveedores` | ídem | test notificaciones | HECHO (evidencia/H3-notificaciones.txt) |
| H4.S1.M3 | tarifas `GET /tarifas/costos-de-proveedor?periodo=AAAA-MM` (suma `DECIMAL` por proveedor y moneda) | suma exacta; dos monedas → dos filas | test tarifas | HECHO (evidencia/H4-tarifas.txt) |
| H4.S2.M1 | Adaptador del front (une por `proveedor_id`; costo `—` si no hay) | spec en tres niveles | `ng test` | HECHO (evidencia/H3-H4-front.txt) |

## H5 — Outbox por servicio
**CA:** la pantalla muestra, por servicio, cuántos eventos tiene pendientes y fallidos en su outbox ahora, y cuántos publicó en las últimas 24 h.
**DoD:** test del medidor + test de la consulta + spec del front · **Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | `comun-mensajeria`: medidor `aportaya.outbox.eventos{estado}` de la propia tabla | con 2 pendientes y 1 fallido, el medidor da 2 y 1 | `./gradlew :plataforma:comun-mensajeria:test` | TODO |
| H5.S1.M2 | auditoria `GET /indicadores/operacion/outbox` sobre Prometheus (`ConsultaPrometheus`) | Prometheus caído → `fuenteDisponible: false`, no 500 | test auditoria | TODO |
| H5.S2.M1 | Pantalla Outbox con la nueva forma | spec en tres niveles | `ng test` | TODO |

## H6 — Despliegues y Respaldos
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Propuesta escrita de registro (tabla, quién escribe, contrato) y la pantalla lo explica en vez de «fuente no disponible» genérico | documento + texto en pantalla | revisión del documento | TODO |

## H7 — TEST y cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H7.S1.M1 | Regresión: backoffice completo + tests de los servicios tocados + build | todo verde | comandos pegados | TODO |
| H7.S1.M2 | Despliegue a TEST según `despliegue/TEST.md` y verificación con sesión real | las pestañas muestran datos en TEST | capturas en TEST | TODO |
| H7.S1.M3 | Deuda: `PUERTO_BASE_DE_DATOS` sin uso | el puerto se retira del árbol | `ng test` | TODO |
| H7.S1.M4 | `nucleo/menu.ts` vuelve a ≤200 líneas (lo pasé a 202 en #224; `verificar_frontend` falla) | `verificar_frontend` sin fallas | `python3 scripts/verificar_frontend.py` | HECHO (evidencia/verificadores-ci.txt) |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin credenciales de TEST | H7.S1.M2 no llega a VERIFIED | Declararlo; pedirle a Pablo el ingreso |
| `BarridoTest` de cada servicio exige que todo endpoint esté en el contrato y decida su acceso | Fallo de arranque | Contrato primero, `@Permiso` en cada operación |
| Tablas vacías en TEST | Pantallas vacías aunque estén cableadas | El vacío dice por qué; no se siembran datos falsos |
