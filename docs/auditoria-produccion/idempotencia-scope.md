# Idempotencia — scope de cada índice único vs. su lectura Java

> H1.S1.M2 / H1.S4.M1 del carril PR4 (Marcelo). Barrido repo-wide de
> `clave_idempotencia`, `Idempotency-Key`, `porClaveIdempotencia`, `idempoten`.
>
> **Comando ejecutado:**
>
> ```text
> $ grep -rn "porClaveIdempotencia" --include=*.java servicios plataforma
> servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/OrdenRetiroRepositorio.java:96
> servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/OrdenRecargaRepositorio.java:77
> servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/LibroDeBilletera.java:110
> servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU12TransferirSaldo.java:72
> servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU11RetirarSaldo.java:85
> servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU10RecargarSaldo.java:82
> servicios/aportes/src/test/java/bo/aportaya/aportes/CU21Test.java:137
> servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java:64
> servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:63
> servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/EnvioRepositorio.java:63
> ```
>
> (`exit=0`, 10 líneas de código — la línea 137 es el comentario del test nuevo, no una
> lectura.)

## Tabla índice → lectura → veredicto

| Índice único (`restricciones.sql`) | Tabla | Lectura Java | Scope de la lectura | Veredicto | Dueño |
|---|---|---|---|---|---|
| `uq_tx_idem` (`:236-238`) | `transaccion_billetera` | `LibroDeBilletera.porClaveIdempotencia` (`:110-119`) | `COALESCE(iniciada_por,centinela)`, `origen_tipo`, `clave` | **EN SCOPE** — coincide exactamente con el índice | PR2 (Justin) — ya corregido en su carril |
| `uq_recarga_idem` (`:239-240`) | `orden_recarga` | `OrdenRecargaRepositorio.porClaveIdempotencia` (`:70-75` según `CU10RecargarSaldo:82`) | `(cuenta_billetera_id, clave)` | **EN SCOPE** | PR2 — **FUERA DE SCOPE de este carril**, dueño: PR2 |
| `uq_retiro_idem` (`:241-242`) | `orden_retiro` | `OrdenRetiroRepositorio.porClaveIdempotencia` (`:87-92` según `CU11RetirarSaldo:85`) | `(cuenta_billetera_id, clave)` | **EN SCOPE** | PR2 — **FUERA DE SCOPE**, dueño: PR2 |
| `uq_devengo_idem` (`:243-245`) | `devengo_comision` | ninguna encontrada (`tarifas.DevengoRepositorio` no tiene `porClaveIdempotencia`) | — | Sin lectura Java hallada; CA de H1.S4 la admite (\"o no existe lectura, solo `ON CONFLICT`\") — **verificar en `tarifas` si usa `ON CONFLICT DO NOTHING`**; `tarifas` es de solo lectura este turno (sin dueño) | Hallazgo, sin dueño este turno |
| `uq_orden_cobro_idem` (`:251-252`) | `orden_cobro` | **no existe entidad Java**: `aportes` no implementa `orden_cobro`/`intento_pago` (solo el CU21 síncrono que registra `pago` directo) | — | **NO IMPLEMENTADO** — hallazgo de alcance, ver `security-matrix.md` §SSRF/webhook y H1.S2 abajo | `aportes` (Marcelo) — declarado, no se construye la cadena completa QR este turno (fuera del alcance de seguridad transversal) |
| `uq_intento_pago_idem` (`:253-254`) | `intento_pago` | ídem anterior — no implementado | — | **NO IMPLEMENTADO** | ídem |
| `uq_pago_idem` (`:255-256`) | `pago` | `PagoRepositorio.porClaveIdempotencia` (`:64-70`) | **ANTES**: solo `clave_idempotencia` (más ancho que el índice) — **hallazgo C del PLAN.md, VIGENTE al leer el SHA base**. **DESPUÉS (este carril)**: `(obligacion_id, clave)` | **CORREGIDO** — test `CU21Test.mismaClaveOtraObligacionEsOtroPago` | `aportes` (Marcelo) — H1.S1.M3 HECHO |
| `uq_webhook_idem` (`:257-258`) | `webhook_pasarela` | **no existía** antes de este carril; se agrega `WebhookRepositorio.porClaveDeEvento` en H1.S2 | `(proveedor_id, clave)` | **CONSTRUIDO en este carril** (H1.S2) | `aportes` (Marcelo) |
| `uq_cotizacion_idem` (`:259-260`) | `cotizacion_comision` | ninguna encontrada (`tarifas.CotizacionRepositorio` no tiene `porClaveIdempotencia`) | — | Sin lectura Java hallada; mismo caso que `uq_devengo_idem` | Hallazgo, sin dueño este turno |
| `uq_token_verificacion_idem` (`:261-263`) | `token_verificacion` | no buscado en `identidad` (fuera de mi alcance) | — | **FUERA DE SCOPE** | PR1 (Richard) |
| `uq_respuesta_idempotente` (`comun-web`) | `respuesta_idempotente` | `Idempotencia.exigirNueva`/`guardarRespuesta` (`plataforma/comun-web/.../idempotencia/Idempotencia.java:43-51,85-90`) | **SOLO** `(clave_idempotencia, operacion)`, sin `usuario_id` — hallazgo A del PLAN.md, sin uso real (ningún CU llama a `exigirNueva` hoy; solo 7 comentarios de esqueleto, ver H6.S2.M3) | **VIGENTE, latente** | PR3 (Leo) — **FUERA DE SCOPE**, `plataforma/*` es OUT explícito de este carril |
| — (sin índice único propio) | `envio_notificacion` (`notificaciones`) | `EnvioRepositorio.porClaveIdempotencia` (`:63-67`) | Solo `clave_idempotencia`; el índice real es `uq_envio_idempotencia (notificacion_id, clave)` — más ancho que el índice, mismo patrón que el hallazgo C | **VIGENTE** | `notificaciones` es "sin dueño este turno: solo lectura" (Daily-Noche-2026-09-21.md §4) — **hallazgo para el equipo, no se edita** |

**Fila `FUERA DE SCOPE`**: cero filas fuera de la explicación de dueño (regla del DoD de
H1.S1). Las de `nucleo-financiero` (PR2) ya estaban corregidas al momento de leer el
SHA de `origin/dev` usado como base de este carril — se listan igual para que la tabla
quede completa, no porque haya trabajo pendiente ahí.

## Veredicto de H1.S1 / H1.S4

- **12 índices con `clave_idempotencia`** relevados (10 de `restricciones.sql:236-263`
  + `uq_respuesta_idempotente` + `uq_envio_idempotencia` de `notificaciones`).
- **1 corregido en este carril**: `uq_pago_idem` (`aportes`).
- **2 sin lectura Java** (`uq_devengo_idem`, `uq_cotizacion_idem` en `tarifas`):
  hallazgo, sin dueño asignado este turno.
- **2 no implementados** (`uq_orden_cobro_idem`, `uq_intento_pago_idem`): la cadena
  `orden_cobro → intento_pago` de QR/pasarela que describe `payments-qr-integration`
  no existe en Java todavía; `aportes` resuelve el cobro de forma síncrona en
  `CU21CobrarAporte`. Construir esa cadena completa es una feature de producto, no
  una corrección de scope de idempotencia — se declara como hallazgo de alcance
  (no se inventa la feature en un carril de seguridad transversal, regla 00).
- **1 hallazgo en servicio ajeno sin dueño** (`notificaciones.EnvioRepositorio`).
- **Las de `nucleo-financiero` (PR2) y `identidad` (PR1)**: fuera de scope de este
  carril; ya corregidas o a cargo de su dueño.
