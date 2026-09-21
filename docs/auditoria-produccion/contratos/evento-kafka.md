# Contrato — envelope de los eventos de dominio en Kafka (outbox → broker)

- Fijado el 2026-09-21 en el reparto `PasanakuPromptManager/repartos/2026-09-21/PromptNoche`. Lo implementa `Relevo` (`plataforma/comun-mensajeria`, carril PR3 · Leo). Lo consumen los consumidores futuros y la traza de punta a punta.
- Semántica de entrega: **al menos una vez**. Todo consumidor deduplica por `event_id` con `Consumidos.registrar` (`<esquema>.evento_consumido (id_evento, consumidor)`).
- **No se modifica durante el turno.**

## Tema, clave y cuerpo

| Qué | Valor |
|---|---|
| Tema | `aportaya.<tipo>` (p. ej. `aportaya.nucleo_financiero.retiro_solicitado`), como ya hace `Relevo.publicar` |
| Clave de partición | `agregado_id` (lo de un mismo agregado llega en orden) |
| Cuerpo | `evento_dominio.payload` (JSON) sin cambios; **nunca** la entidad del ORM |

## Cabeceras (todas obligatorias salvo `causation_id`)

| Cabecera | Origen en `evento_dominio` | Formato |
|---|---|---|
| `event_id` | `id` | UUID |
| `type` | `tipo` | texto |
| `version` | `version` | texto (`"1"`) |
| `occurred_at` | `ocurrido_en` | ISO-8601 UTC |
| `producer` | `aportaya.esquema` del servicio | texto (`nucleo_financiero`) |
| `correlation_id` | `correlation_id` | UUID |
| `causation_id` | `causation_id` | UUID o ausente |
| `trace_id` | MDC de `Traza` al momento de publicar; si el evento se publica desde el relevo sin request, el `trace_id` guardado en `metadatos.trace_id` | texto |

## Estados y reintentos del outbox

`PENDIENTE → TOMADO → PUBLICADO`, con `FALLIDO` como DLQ lógica tras `aportaya.outbox.intentos-maximos` (10) intentos; backoff exponencial desde `PT1S` hasta `PT5M` con jitter ±20 %; `timeout-publicacion = PT10S`. Todos son configuración, ninguno literal en código. La publicación ocurre **fuera** de toda transacción de PostgreSQL: se toma en una transacción corta, se publica, se marca en otra.

## Lo que un consumidor debe hacer siempre

1. `Consumidos.registrar(dsl, event_id, "<nombre-del-consumidor>")` en la **misma** transacción que el efecto; si devuelve `false`, no hace nada.
2. Restaurar `correlation_id` y `trace_id` en el MDC antes de loguear.
3. Nunca confiar en el orden entre agregados distintos.
