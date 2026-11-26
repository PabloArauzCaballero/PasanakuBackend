> **AVANCE: 6 / 7 — 85,7 %.**

# Reporte — CU21 conectado a la pasarela simulada + endpoint del aviso simulado

- Fecha: 2026-11-26 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/pasarela-simulada-cu21-endpoint` (desde `origin/dev`)
- Peldaño de evidencia alcanzado (regla 30): **TESTED** (suites `test`/`webTest` y tests de integración contra PostgreSQL 16 real; arranque
  Spring real con copia temporal de `ArranqueTest`). No `VERIFIED`: sin recorrido HTTP real contra el servidor TEST ni CI todavía.
- Todo lo verificado es contra el **DOBLE** (`QR_INTEROP` simulado): ningún proveedor real.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | `PasarelaDePagos.registraOrdenPorCobro()` (defecto false; el doble true) | `PasarelaSimuladaTest` | PASS |
| H1.M2 | CU21 abre la orden simulada sin cambiar respuestas ni errores | `CU21PagoConSaldoTest` (6) + `CU21PagoConOrdenSimuladaTest` (3) | PASS 9/9 |
| H2.M1 | `POST /qr/simulador/avisos-de-pago` en OpenAPI, esquemas estrictos | generación OpenAPI + `AvisoSimuladoWebTest` | PASS |
| H2.M2 | `CU100EmitirAvisoDePagoSimulado`: emite, entrega a CU-100 real, audita | `CU100AvisoSimuladoTest` | PASS 5/5 (PROCESADO, DUPLICADO, orden desconocida, proveedor inactivo, monto distinto) |
| H2.M3 | Controlador `@Profile` + propiedad, `@Permiso("ADMIN_PLATAFORMA")`, matriz negativa | `AvisoSimuladoWebTest` (5) + `PasarelaSimuladaPerfilTest` (3) | PASS: 401 sin sesión/sin rol, 403 en 7 sesiones, 400 en 5 cuerpos, ruta inexistente en production/staging/sin perfil/modo real |
| H2.M4 | Postman regenerado (lo valida la CI); catálogos de operaciones NO regenerados | `python3 scripts/generar_postman.py` | cambios solo de la operación nueva |

Suites: `test` 79/79 · `webTest` 106/106 · `integrationTest` 175 con 3 fallos `ENVIRONMENT` (sin Docker: `ArranqueTest`, `AislamientoEsquemaTest`).
Arranque Spring real (copia temporal de `ArranqueTest` apuntada al PG local, ya borrada): 2/2 con perfil `test`. Detalle en [evidencia/pruebas.txt](./evidencia/pruebas.txt).

## A medias
### H3.M2 — spotless, push, PR, CI verde, aviso
- Qué anda: `spotlessCheck` verde; todo el código escrito y probado en local.
- Qué no anda: CI y PR todavía sin resultado al escribir esto.
- Qué falta exactamente: un push, PR contra `dev` fuera de borrador, CI verde, aviso a `@parent`.
- Dónde quedó: rama `claude/pasarela-simulada-cu21-endpoint`, compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Activar `QR_INTEROP` en TEST | `DECISION_REQUIRED` | Pablo/Cumplimiento: el seed lo deja `activo = FALSE` (contrato de tercero). Mientras tanto el endpoint responde `AP-CU100-12` explicando qué revisar |
| Regenerar `catalogo-operaciones.json` y `operaciones.json` del backoffice | `DECISION_REQUIRED` | El generador produce ~11 000 líneas de diferencia ajenas a este cambio (los archivos comprometidos ya estaban desfasados); no es parte de la CI y no se mezcla aquí |
| Adaptador real | `BLOQUEADO` | Proveedor contratado (P-15) |

## Evidencia
Ver [evidencia/pruebas.txt](./evidencia/pruebas.txt).

## No cubierto
- Recorrido HTTP del endpoint contra el servidor TEST (la matriz 401/403/400/422 se probó en `@PruebaWeb`, el ciclo con base en integración).
- La colección Postman «humo» incluye la operación nueva: si algún job la corre contra un backend sin perfil `local`/`test`, devolverá 404 (fail-closed).
- «Mismos estados/errores que uno real»: no hay proveedor real que copiar; el doble tiene PENDIENTE/PAGADO.
- Regla 91.6: el aviso concilia el pago ya registrado; no genera asientos ni acredita saldo. Idempotencia sí ejercitada (mismo aviso dos veces ⇒ `DUPLICADO`, una fila, un pago).
- Regla 98.8 (caída del dependido, traza entre servicios): el doble es en proceso, no hay salto remoto.

## Desvíos del plan
- El caso de uso y los códigos de error se numeran `CU-100` / `AP-CU100-11/12` (no `CU-101`): no se inventa un caso de uso nuevo en el catálogo de CU.
- La acción de bitácora es `CONCILIACION` (catálogo cerrado `ck_bitacora_evento_accion`); `AVISO_SIMULADO` fue rechazado por la base y no se agregó valor.
- `CU21PagoConSaldoTest` solo cambió su constructor (recibe una pasarela que no pide orden).

## Riesgos residuales
- En TEST (perfil `local`) los pagos con saldo nuevos quedan con referencia `SIM-…` en vez de `SALDO-…`: solo datos de TEST, respuesta idéntica.
- La orden simulada vive en memoria: un reinicio la olvida y el aviso devuelve `AP-CU100-11`.

## Decisiones y ambigüedades
- Permiso: se reusa `ADMIN_PLATAFORMA` (precedente en `darDeAltaProveedor`) en vez de sembrar un permiso nuevo en la bóveda de roles. Confirmar con Pablo.
- Registro de la orden: capacidad `registraOrdenPorCobro()` en el puerto, no una propiedad: el adaptador real no puede abrir órdenes por error.
