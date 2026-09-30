# Matriz de seguridad — OWASP API Security Top 10:2023

> H5 del carril PR4-seguridad (Marcelo). Cada fila: Componente, Amenaza, Control,
> Test, Evidencia, Estado, Riesgo residual. Lo que se afirma "verde" tiene comando y
> salida pegados en algún documento de `docs/auditoria-produccion/`; lo que no, dice
> `TODO`/`REVISAR` en vez de inventar.

## Bitácora encadenada — dónde vive y quién la usa

**Hallazgo previo a este carril, encontrado corriendo el grep, no supuesto:**
`comun.bitacora_evento` (`sql/10_tablas/09_auditoria_reportes/bitacora_evento.sql`)
tiene disparador de cadena de hash (`fn_aud_encadenar_bitacora`,
`sql/40_reglas/restricciones.sql:90-104`) y bloqueo consultivo propio
(`pg_advisory_xact_lock(hashtext('cadena_bitacora_evento'))`) desde antes de este
carril, pero:

```text
$ grep -rn "bitacora_evento" --include=*.java servicios plataforma
(cero resultados en código de PRODUCCIÓN — solo aparece en dos archivos de TEST:
 AislamientoEsquemaTest.java, AppendOnlyRepositorioTest.java, y ninguno la ESCRIBE)
```

Es decir: **ningún servicio dejaba rastro en la bitácora encadenada antes de este
carril**, para NINGUNA operación crítica, en NINGÚN servicio. El único escritor de
cualquier tabla de auditoría era `auditoria.ReporteRepositorio`, para su propia
`comun.registro_acceso_datos` (un mecanismo distinto: acceso a datos sensibles en un
reporte, no una operación crítica de las que pide el §H7.S6).

| CU crítico (§H7.S6) | Servicio | ¿Escribía `bitacora_evento` antes de este carril? | Estado en este carril |
|---|---|---|---|
| Login / login fallido | identidad | No | Hallazgo entregado a Richard (PR1); no se edita `identidad` (OUT de este carril) |
| Challenge MFA | identidad | No | Hallazgo a Richard |
| Cambio de rol/permiso | identidad | No | Hallazgo a Richard |
| Transferencia | nucleo-financiero | No | Hallazgo a Justin (PR2); no se edita `nucleo-financiero` |
| Retiro | nucleo-financiero | No | Hallazgo a Justin |
| **Aprobación** (reembolso) | **aportes** | No | **CORREGIDO en este carril**: `AuditoriaRepositorio` + `CU19ReembolsarPago.aprobar` ahora escriben `comun.bitacora_evento` (`accion = 'APROBACION'`). Test: `AuditoriaCriticaTest.aprobarReembolsoQuedaAuditado` |
| Rechazo | — | — | `aportes` no tiene un CU de "rechazo" explícito distinto de la disputa; `registrarDisputa` no es un rechazo en el sentido de §H7.S6 |
| Cambio de configuración sensible | aportes (`darDeAltaProveedor`, CU-99) | No | **`A MEDIAS` declarado**: identificado como candidato (alta de proveedor de pago es config sensible) pero NO instrumentado en esta corrida por presupuesto de tiempo — queda para la siguiente sesión, con el mismo patrón de `AuditoriaRepositorio` ya construido |
| cumplimiento (varios CU) | cumplimiento | No | Sin dueño este turno (solo lectura) — hallazgo sin destinatario, declarado tal cual |

**Estado H5.S1: A MEDIAS.** Un CU crítico de `aportes` (aprobación de reembolso)
queda auditado y probado; el resto de los CU críticos del sistema (login, MFA,
retiro, transferencia — todos fuera de `servicios/aportes/**`) tienen el mismo hueco,
documentado como hallazgo para sus dueños, sin editar código ajeno (regla del
carril).

## Matriz OWASP API Security Top 10:2023

| # | Amenaza | Componente | Control | Test | Evidencia | Estado | Riesgo residual |
|---|---|---|---|---|---|---|---|
| API1 | Broken Object Level Authorization (BOLA) | Todos los endpoints con `{id}` en la ruta | `@Permiso` por operación + rol; ownership fino (IDOR) NO verificado por CU en esta corrida | — | `endpoints.md` columna Ownership = "revisar (id en la ruta)" en 6 endpoints de `aportes` | **REVISAR** (H7.S2, matriz IDOR, no es microtarea de este encargo — el encargo de Marcelo no la incluye explícitamente) | Un operador con `BILLETERA_OPERAR` puede operar sobre CUALQUIER pago/reembolso, no solo los de su grupo asignado — es el modelo de backoffice actual, no necesariamente un defecto, pero no está DEMOSTRADO con test negativo |
| API2 | Broken Authentication | `identidad` (JWT), todos los servicios (JWKS) | RS256 + JWKS; hallazgos previos VIGENTES (RSA efímera si falta clave, solo `WARN`; JWT sin `iss`/`aud`) — `PLAN.md` hallazgo G | — | `docs/auditoria-produccion/PLAN.md` §2.4 fila G | **TODO** (PR1, Richard — H3/H5 del plan madre) | Producción podría arrancar con clave efímera sin fallar el arranque — solo un WARN en log |
| API3 | Broken Object Property Level Authorization (mass assignment) | Todos los contratos OpenAPI | `additionalProperties: false` + límites explícitos | `scripts/verificar_contratos_limites.py` | Exit 0 en `aportes` (4 hallazgos corregidos); 83 hallazgos en 12 servicios más, listados como pendientes no bloqueantes | **HECHO en `aportes`; PENDIENTE en el resto** (dueños de cada servicio) | Un cliente puede mandar campos no declarados en los 12 servicios restantes hasta que adopten el gate |
| API4 | Unrestricted Resource Consumption | Contratos con arrays/paginación | `maxLength`/`maximum`/`maxItems`; paginación con techo | `scripts/verificar_contratos_limites.py` (función `_revisar_paginacion`) | Mismo exit 0 de arriba | **HECHO en `aportes`** | — |
| API5 | Broken Function Level Authorization | Todos los controladores | `@Permiso`/`@Publico` + `SabanaDeSeguridadWeb` (401 sin sesión, 403 sin permiso, barrido cruzado) | `SeguridadWebTest` por servicio (ya existente, no de este carril) | — | **HECHO** (mecanismo preexistente, verificado por inventario H2.S1) | — |
| API6 | Unrestricted Access to Sensitive Business Flows | Webhook de `aportes`, rutas sensibles del gateway | Firma HMAC + ventana + dedupe (webhook); rate limiting en el borde: **NO CONSTRUIDO** | `CU100WebhookTest` (8 casos) | `carriles/PR4-seguridad.md` §H1.S2 | **HECHO en el webhook**; **TODO el rate limiting** (H7.S4, Pablo — Redis en el gateway, AMB-6) | Sin rate limiting, un atacante puede probar firmas de webhook a alta frecuencia (aunque cada intento con firma inválida no cuesta una escritura, sí cuesta CPU de verificación HMAC) |
| API7 | Server Side Request Forgery (SSRF) | Clientes HTTP salientes | Inventario de `*PorHttp`/`RestClient`/`HttpClient` | `grep -rn "PorHttp\|RestClient\|HttpClient" servicios/aportes/src/main` | **Cero resultados**: `aportes` no tiene clientes HTTP salientes propios | **HECHO en `aportes`** (sin superficie) | Otros servicios no inventariados en esta corrida (fuera de alcance — no es `aportes`) |
| API8 | Security Misconfiguration | Todo el repo | `scripts/verificar_seguridad.py` (7 bloques, el 7º nuevo de este carril) | `python3 scripts/verificar_seguridad.py` | `EXIT=0 · TODO OK · 3 aviso(s)` (2 preexistentes + 1 nuevo, `DestinoRepositorio.java:34`, no bloqueante) | **HECHO** | Los avisos S-8/S-9 (permisos/canales de token faltantes) siguen abiertos, son decisión de negocio, no de implementación |
| API9 | Improper Inventory Management | Todos los endpoints del sistema | `scripts/inventario_endpoints.py --check` | Ver arriba | `endpoints relevados: 158 · hallazgos: 0` | **HECHO** | — |
| API10 | Unsafe Consumption of APIs | Webhook de la pasarela (el único "API de terceros" que `aportes` consume, en sentido inverso: la pasarela nos consume a nosotros) | Ídem API6: firma, ventana, dedupe, validación de monto contra el pago | `CU100WebhookTest` | Ídem | **HECHO** | La resolución del secreto real (vault/HSM) no existe — `SecretoWebhookSinConfigurar` falla cerrado fuera de `local`/`test`, `DECISION_REQUIRED` declarado |

## Para el CI

```yaml
- name: Matriz de seguridad y bitácora crítica
  run: |
    python3 scripts/verificar_seguridad.py
    ./gradlew :servicios:aportes:integrationTest --tests '*AuditoriaCriticaTest*'
```
