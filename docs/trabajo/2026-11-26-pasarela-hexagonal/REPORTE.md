> **AVANCE: 10 / 12 — 83,3 %.**

# Reporte — Pasarela de pagos simulada con arquitectura hexagonal

- Fecha: 2026-11-26 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/pasarela-hexagonal` (desde `origin/dev`)
- Peldaño de evidencia alcanzado (regla 30): **TESTED** (tests dirigidos y suites `test`/`webTest` verdes contra PostgreSQL 16 real).
  No `VERIFIED`: no hay recorrido HTTP/E2E ni CI todavía.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1–M3 | Puertos `PasarelaDePagos`, `InterpreteDeWebhook`, `SimuladorDePagoDePasarela` (dominio, `Dinero` con moneda, sin float) | `PasarelaSimuladaTest` | PASS 8/8 ([evidencia](./evidencia/pruebas.txt)) |
| H1.S2.M1 | `PasarelaSimulada` determinista e idempotente por clave | `PasarelaSimuladaTest` | PASS |
| H1.S2.M2 | `InterpreteDeWebhookJson` (parseo movido desde CU-100) | `InterpreteDeWebhookJsonTest` | PASS 3/3 |
| H1.S2.M3 | Guardia `!local & !test`, esqueleto `real/` fail-closed, `application.yml` | `PasarelaConfiguracionTest` + arranque Spring real con `production` | PASS 3/3; arranque falla con el mensaje esperado |
| H1.S2.M4 | CU-100 consume el puerto; suites existentes adaptadas | `:servicios:aportes:test :webTest` | 82/82 y 101/101 PASS |
| H1.S3.M1 | Flujo simulador→CU-100: correcto, duplicado, borde 300 s/301 s, monto distinto, orden inexistente, firma inválida | `CU100PasarelaSimuladaTest` | PASS 6/6 |
| H1.S3.M2 | Reglas ArchUnit de la pasarela | `ArquitecturaTest` + mutación | PASS 8/8; con un import de adaptador en `aplicacion`, 1 falla |
| H1.S4.M1 | Guía `docs/operacion/pasarela-real-como-enchufar.md` | lectura | entregada |

## A medias
### H1.S2.M5 — `ArranqueTest` con `@ActiveProfiles("test")`
- Qué anda: una copia temporal apuntada al PG local levanta con perfil `test` (2/2) y falla con `production`.
- Qué no anda: el `ArranqueTest` real no se ejecutó aquí (pide Testcontainers y no hay Docker).
- Qué falta exactamente: que la CI lo corra en verde.
- Dónde quedó: `ArranqueTest.java` editado, compila.

### H1.S4.M2 — spotless, push, PR, CI verde
- Qué anda: `spotlessCheck` pasa; commit y push de la rama.
- Qué no anda: CI aún sin resultado a la hora de escribir esto.
- Qué falta exactamente: PR contra `dev` fuera de borrador, CI verde, aviso a `@parent`.
- Dónde quedó: rama `claude/pasarela-hexagonal`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Adaptador real | BLOQUEADO (decisión de negocio P-15: sin proveedor contratado) | Pablo contrata proveedor; guía en `docs/operacion/` |
| `CU21` usando `crearOrdenDeCobro` | `DECISION_REQUIRED` | Pablo: ¿se construye la cadena `orden_cobro → intento_pago`? |
| Endpoint HTTP para disparar el webhook simulado en TEST | `DECISION_REQUIRED` | Pablo |

## Evidencia
Ver [evidencia/pruebas.txt](./evidencia/pruebas.txt) (conteos y salidas recortadas de las corridas).

## No cubierto
- Todo lo anterior se verificó **contra el doble**: ningún proveedor real, formato de webhook real ni esquema de firma real.
- `integrationTest` completo: 9 fallos por `ENVIRONMENT` (sin Docker: `AislamientoEsquemaTest`, `ArranqueTest`) y `DATA` (la fixtura
  `FixturaDeAportes.usuario()` no borra usuarios; colisiona al reusar la base). Ninguno ejercita este cambio; CI usa contenedor nuevo.
- Sin recorrido HTTP del webhook simulado, sin traza con `correlationId`, sin prueba de caída del dependido (regla 98.8): el doble es
  en proceso, no hay salto remoto.
- Regla 91.6 (asientos, cuadre, reversa): este cambio **no genera asientos ni acredita**; CU-100 solo marca el webhook
  `PROCESADO`. Idempotencia sí ejercitada (mismo `idEvento` ⇒ `DUPLICADO`, una fila, un pago).

## Desvíos del plan
- `PasarelaConfiguracionTest` se movió al paquete `..pasarela` porque la regla ArchUnit nueva (correctamente) marcó que el
  paquete `simulada` no puede referenciar a `real`.

## Riesgos residuales
- Guardia `!local & !test` es estricta: un entorno con perfil `staging` y sin adaptador real no arranca (es lo pedido).
- El doble guarda órdenes en memoria; un reinicio las pierde.

## Decisiones y ambigüedades
- Código de proveedor del doble: se reusa `QR_INTEROP` (sembrado) por configuración, sin inventar un catálogo nuevo. Confirmar con Pablo.
- Firma: se mantiene HMAC-SHA256 sobre `timestamp.cuerpo` (Q-02); un proveedor real con otro esquema exigiría decisión.
- Secreto: el doble firma con el secreto del entorno (`APORTES_WEBHOOK_SECRETO`); no existe secreto de pasarela real ni se inventó uno.
