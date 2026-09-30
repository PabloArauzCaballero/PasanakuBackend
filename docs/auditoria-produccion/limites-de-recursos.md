# Límites de recursos — H4.S1.M3

Cada valor con su archivo fuente. Nada inventado: lo que no está configurado
explícitamente se declara "sin configurar, corre con el default", no se le
adivina un número.

## Pool de conexiones a la base

| Quién | Valor | Fuente |
|---|---:|---|
| `identidad` (Hikari) | 20 | `servicios/identidad/src/main/resources/application.yml:20` |
| `nucleo-financiero` (Hikari) | 20 | `servicios/nucleo-financiero/src/main/resources/application.yml:14` |
| Los otros 12 servicios (Hikari, cada uno) | 5 | `servicios/*/src/main/resources/application.yml:14` |
| **Suma de los 14 Hikari** | **100** | 2×20 + 12×5 |
| PgBouncer, `DEFAULT_POOL_SIZE` | 40 | `despliegue/compose/base.yml:88` |
| PgBouncer, `MAX_CLIENT_CONN` | 1000 | `despliegue/compose/base.yml:87` |
| PostgreSQL, `max_connections` | 300 | `despliegue/compose/base.yml:36` |

**Lectura:** los 14 servicios pueden abrir hasta 100 conexiones simultáneas
hacia PgBouncer (`MAX_CLIENT_CONN=1000` de sobra). PgBouncer, en modo
`transaction` (`despliegue/compose/base.yml:83`), multiplexa esas 100 contra
un pool de solo 40 hacia Postgres — el cuello de botella real está ahí, no en
`max_connections` de Postgres (300, con margen). Un pico donde los 14
servicios pidan su máximo a la vez (100 peticiones) espera turno detrás de
las 40 de PgBouncer; no hay overflow hacia Postgres directo.

## Tamaño de request / multipart

| Quién | Valor | Fuente |
|---|---:|---|
| NGINX, `client_max_body_size` | 12 MB | `despliegue/nginx/aportaya.conf:30` |

Sin límite explícito de multipart por servicio (`spring.servlet.multipart.*`)
en ningún `application.yml` de los 14 — corre con el default de Spring Boot
(1 MB por archivo, 10 MB por request) *dentro* del límite de NGINX. Si algún
caso de uso sube archivos más grandes que eso (fotos de identidad, por
ejemplo), el 413 lo devuelve Spring antes que NGINX — no verificado contra
ningún caso de uso puntual, se declara la brecha.

## Buffers de Kafka

**Sin configurar** en ningún `application.yml` ni en
`plataforma/comun-mensajeria` (verificado: `grep -rn "batch-size\|buffer-memory\|max-poll-records\|fetch-max"`
sin resultados). Corre con los defaults de `spring-kafka`/Kafka client:
`batch.size=16384`, `buffer.memory=33554432`, `max.poll.records=500`. No se
midió si estos defaults alcanzan bajo carga real — es lo que la corrida de
`carga/k6/outbox.js` con Kafka arriba de verdad tendría que revelar, y hoy no
hay ningún consumidor productivo corriendo (solo los tópicos existen).

## Pool de clientes HTTP salientes (entre servicios)

**Sin configurar explícitamente** — no se encontró ningún `RestClient`,
`maxConnTotal` ni `maxConnPerRoute` declarado en `plataforma/comun-*`
(verificado, sin resultados). Los servicios de este backend no se llaman
entre sí por HTTP en el camino síncrono (arquitectura por eventos vía
outbox — ADR de mensajería); el único cliente HTTP saliente conocido hoy es
el que el gateway usa para enrutar (Spring Cloud Gateway, Netty reactivo,
`spring.cloud.gateway.server.webflux.httpclient.connect-timeout: 2000` /
`response-timeout: 10s`, `plataforma/gateway/src/main/resources/application.yml`).

## Hilos

Sin `server.tomcat.threads.*` ni configuración explícita de pool de hilos en
ningún servicio — corre con los defaults de Spring Boot/Tomcat embebido
(200 hilos máximo, 10 mínimo). El gateway, al ser reactivo (Netty), no usa
este modelo — su concurrencia depende del *event loop* de Netty, sin un
número de "hilos máximos" comparable.

## No cubierto

- Ningún valor de esta tabla se contrastó contra una corrida de carga real
  con los 14 servicios arriba — hoy solo el gateway está construido en esta
  máquina (H1.S2.M6). Los números de Hikari/PgBouncer/Postgres son los
  DECLARADOS en el código, no los OBSERVADOS bajo presión.
- Kafka y el pool HTTP saliente: no hay nada que medir todavía porque no hay
  tráfico real que los ejercite.
