# Por qué el refresco usa `HttpBackend` en vez del `HttpClient` interceptado

## Contexto

`RefrescoDeSesion.refrescar()` (`apps/backoffice/src/app/nucleo/refresco-de-sesion.ts`) hace
`POST /sesion/refrescar` con un `HttpClient` construido a mano sobre `HttpBackend`
(`new HttpClient(inject(HttpBackend))`), en vez de inyectar el `HttpClient` normal de la
aplicación (el que pasa por `withInterceptors([...])` en `app.config.ts`).

## Alternativa descartada: usar el `HttpClient` interceptado

Si el refresco saliera por el `HttpClient` de la app, pasaría de nuevo por
`sesionInterceptor` (reentrada: un refresco que a su vez podría disparar OTRO refresco si
devolviera `401`), por `idempotenciaInterceptor` (le agregaría una clave de idempotencia
que no le corresponde: el refresco no es la operación de negocio que hay que hacer
idempotente) y por `registroDeAccesoInterceptor`/`trazaInterceptor`. Evitar la reentrada
con el `HttpContextToken` `YA_REINTENTADA` que ya existe solo cubre el caso del REINTENTO
de la petición original, no el `POST /sesion/refrescar` en sí, que es una petición nueva
sin ese contexto.

## Decisión

El refresco sale por un `HttpClient` propio construido sobre `HttpBackend`, que es la capa
de transporte de Angular sin ningún interceptor registrado. Consecuencia verificada en
`sesion.interceptor.spec.ts` ("single-flight en el backoffice · caso límite"): el `POST` a
`/sesion/refrescar` no lleva el header `Authorization` del token vencido ni pasa de nuevo
por `sesionInterceptor`.

## Qué se descarta con esto

- Doble refresco por reentrada del propio interceptor sobre su llamada de refresco.
- Clave de idempotencia inyectada sobre una operación que ya es idempotente por diseño del
  lado servidor (cookie de refresco de un solo uso rotativo — `Q-R3` en el carril, sin
  confirmar todavía contra `identidad`).
- Registro de acceso disparado por el propio refresco (no es una lectura de datos de un
  expediente; no corresponde marcarlo con `ACCESO_A_DATOS`).
