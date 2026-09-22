---
tags:
  - arquitectura
  - adr
  - seguridad
  - infraestructura
titulo: "ADR-050 — Rate limiting distribuido en el borde"
estado: aceptada
fecha: 2026-09-21
---

# ADR-050 — Rate limiting distribuido en el borde

> Las rutas que mueven dinero o abren sesión se cortan por tasa en el gateway,
> con estado compartido en Redis — no por réplica, no en NGINX.

## Contexto

El gateway corre en varias réplicas detrás de NGINX (H2.S4, H3). Un límite por
tasa en memoria local (`bucket4j` sin backend compartido, o un `limit_req` de
NGINX por proceso) cuenta distinto en cada réplica: con tres réplicas, un
atacante que reparte sus peticiones entre ellas ve, de hecho, el triple del
límite declarado. Las rutas que esto tiene que cortar de verdad — login,
registro, retiro, transferencia — son justo las que un límite por réplica no
protege.

El hallazgo previo H (planes/00 §87) ya marcaba esto: "CI rojo en Spotless"
tapaba todo lo demás, pero el borde nunca tuvo limitador real, solo la
promesa de uno (AMB-6 del plan madre).

## Decisión

**Redis entra al stack** (`despliegue/compose/base.yml`, y
`docker-compose.coolify.yml` en la promoción) como backend compartido del
`RequestRateLimiter` de Spring Cloud Gateway (algoritmo *token bucket*,
implementación oficial `RedisRateLimiter`). Las réplicas comparten el mismo
contador porque comparten el mismo Redis: no importa cuál de ellas atienda la
petición siguiente.

### Qué se limita

`scripts/modelo.py → RUTAS_SENSIBLES` (regla 97: catálogo cerrado, no un
`if` disperso) declara las rutas — hoy cuatro, verificadas contra un
contrato OpenAPI real: `POST /sesiones` (login), `POST /usuarios`
(registro), `POST /billetera/retiros` (retiro), `POST /billetera/transferencias`
(transferencia). `scripts/generar_gateway.py` las traduce a rutas del
gateway con el filtro `RequestRateLimiter`, **antes** de la ruta general del
servicio (Spring Cloud Gateway usa la primera ruta que matchea).

El encargo original pedía ocho (sumando refresco de sesión, reseteo de
contraseña, el desafío de MFA y OTP). Esas cuatro **no tienen endpoint
todavía** — las está escribiendo el carril de identidad esta misma noche
(PR1, Richard). Agregarlas cuando existan es una entrada más en
`RUTAS_SENSIBLES`, no un cambio de diseño.

### La clave del limitador

`LimitadorDeTasa.ipYUsuarioKeyResolver` (`plataforma/gateway`) resuelve por
IP del cliente, combinada con el valor crudo (sin decodificar) de
`Authorization` cuando existe. El gateway no trae Spring Security ni decodifica
JWT — no le toca, lo autoriza cada servicio — así que "usuario" acá es "el
mismo token", no una identidad verificada. Antes de tener sesión (login,
registro) la clave es solo la IP.

### Los números

`replenishRate: 5`, `burstCapacity: 10` (5 peticiones por segundo sostenidas,
ráfaga de 10). **`DECISION_REQUIRED`**: son un valor de arranque razonable
—alcanza para un reintento humano normal y corta una fuerza bruta— pero
nadie del negocio los confirmó. Están en `scripts/generar_gateway.py`, una
constante, no repartidos en catorce YAML.

### Redis caído

**El default de la librería es lo contrario de lo que este ADR pedía.**
Verificado contra el código fuente de `RedisRateLimiter`
(`spring-cloud-gateway-server-webflux`, catch final de `isAllowed()`): ante
cualquier error hablando con Redis devuelve `allowed=true` — el comentario
del propio framework dice literalmente *"we don't want a hard dependency on
Redis to allow traffic"*. Reproducido apagando el contenedor de Redis de
verdad: la petición a `/api/v1/sesiones` pasaba igual, con `500` del backend
en vez de un rechazo — fail **open**, no fail closed.

Se corrige con un filtro propio, `LimitadorDeTasa.denegarSiRedisNoResponde`
(`plataforma/gateway`): corre después de `RequestRateLimiter` (que ya puso
`X-RateLimit-Remaining`) y antes de `NettyRoutingFilter` (el que reenvía al
backend, orden `LOWEST_PRECEDENCE`). Si ve el centinela `X-RateLimit-Remaining:
-1` que la librería usa para señalar "no pude preguntarle a Redis", corta la
cadena ahí mismo — el backend nunca ve la petición — y devuelve `503` +
`Retry-After`. Una ruta no sensible (sin `RequestRateLimiter`) no tiene esa
cabecera y no la toca este filtro: verificado con `/api/v1/grupos` contra el
mismo Redis caído, sigue devolviendo el 500 normal de backend inalcanzable.

Evidencia completa (código fuente citado, antes/después con Redis real
apagado): [evidencia/H3-S1-M4-redis-caido-fail-closed.txt](../auditoria-produccion/evidencia/H3-S1-M4-redis-caido-fail-closed.txt).

## Alternativas descartadas

- **`limit_req` de NGINX, por proceso.** Más simple, cero dependencias
  nuevas — y exactamente el problema del contexto: no comparte estado entre
  réplicas del gateway ni entre instancias de NGINX si algún día hay más de
  una. Se documenta acá para que nadie la reproponga sin releer esto.
- **Limitador en memoria del gateway (`bucket4j` local).** Mismo problema:
  un contador por proceso dice "5 por segundo" y entrega "5 × réplicas".

## Consecuencias

- Redis es infraestructura nueva y compartida: alguien tiene que operarla
  (backup no aplica — es un caché de contadores, se reconstruye solo — pero
  sí monitoreo de que esté arriba).
- El gateway ahora tiene una dependencia externa además de los catorce
  servicios: su *readiness* no depende de Redis (una ruta sensible que
  deniega no es un gateway caído), pero su comportamiento de seguridad sí.
- Cuatro de las ocho rutas pedidas quedan sin cubrir hasta que el carril de
  identidad publique sus endpoints — registrado como hallazgo, no oculto.

## Ver también

[[ADR-025 Empaquetado y despliegue de los servicios]] ·
[[_Arquitectura]]
