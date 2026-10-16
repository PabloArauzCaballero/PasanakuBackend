# Hallazgos corregidos — PR11-Sesion.Frontend

## F-1 — Diez `401` concurrentes disparaban diez refrescos (kill-test #1)

**Evidencia:** `sesion.interceptor.spec.ts` — caracterización con 10 peticiones concurrentes
contra `sesion.interceptor.ts` (versión de `dev@a23bcb1`): `expected 1 to be 1, actual: 10`
(`evidencia/H2-S1-M2.txt`).

**Causa raíz:** `sesionInterceptor` llamaba `http.post('/sesion/refrescar', ...)` directo,
una vez por cada petición que recibía `401`. No había ningún mecanismo que compartiera un
refresco en vuelo.

**Corrección:** `refresco-de-sesion.ts` (`RefrescoDeSesion`) — single-flight con
`shareReplay(1)` sobre un `HttpClient` sin interceptores (`HttpBackend`, ver
`entregables/decision-httpbackend.md`). `sesionInterceptor` pasa a consumirlo.

**Pruebas:** `sesion.interceptor.spec.ts`, 6 casos (único, 10 concurrentes, límite con 5
tardías, error, rotación, protección de bucle) — todos PASS, `evidencia/H2-S2-completo.txt`.

## F-2 — `F5` sobre una ruta protegida siempre caía a `/ingreso` (kill-test #2)

**Evidencia:** `permisos.ts` (versión de `dev@a23bcb1`) — `requiereSesion()` decidía de
forma síncrona con `sesion.abierta()`, que arrancaba en `false` en cada carga de página
(no había ningún intento de restaurar la sesión desde la cookie de refresco).

**Causa raíz:** no existía ningún `AuthBootstrap`: nada llamaba a
`POST /sesion/refrescar` al arrancar, así que toda recarga con cookie válida se
interpretaba como "sin sesión".

**Corrección:**
- `sesion.ts` — máquina de estados (`UNKNOWN`/`RESTORING`/`AUTHENTICATED`/`ANONYMOUS`/`ERROR`).
- `auth-bootstrap.ts` (`restaurarSesion`/`inicializarSesion`) — `provideAppInitializer` que
  intenta el refresco antes de montar cualquier ruta, con timeout de 5s.
- `permisos.ts` — `requiereSesion()` espera la resolución del estado (no deja pasar en
  `UNKNOWN`/`RESTORING`), y distingue `ANONYMOUS` (→ `/ingreso?volverA=`) de `ERROR`
  (→ `/arranque?volverA=`, pantalla accionable con "Reintentar").
- `restaurando-sesion.ts` — el componente de las dos pantallas (restaurando / error).
- `pantalla-de-ingreso.ts` — respeta `volverA` tras el login (solo si es una ruta interna).

**Pruebas:** `sesion.spec.ts` (8, máquina de estados), `auth-bootstrap.spec.ts` (5, éxito /
401 / error / timeout / concurrencia), `permisos.spec.ts` (4 nuevos, `requiereSesion`),
`restaurando-sesion.spec.ts` (3) y `restaurando-sesion.a11y.spec.ts` (2, sin violaciones)
— todos PASS, `evidencia/H3-completo-38-pass.txt`.

## F-3 — La auditoría de acceso se registraba DOS veces por cada lectura (hallazgo nuevo, no estaba en el plan original)

**Evidencia:** al escribir el spec de los tres niveles de `registro-de-acceso.interceptor.ts`
(H4.S2), el nivel "correcto" mostraba `POST /extraccion/accesos` **dos veces** por una sola
lectura exitosa (`backend.expectOne` fallaba con "found 2 requests").

**Causa raíz:** el `tap` del interceptor (versión de `dev@a23bcb1` y mi primera reescritura)
reaccionaba a **cualquier evento** del stream de `HttpEvent` que atraviesa un interceptor
funcional (que puede traer más de un evento por petición), no solo a la respuesta final
(`HttpResponse`). Con el backend de pruebas, dos eventos llegaban por la misma lectura y el
registro salía dos veces — en producción, contra un backend real, esto habría duplicado
cada entrada de auditoría.

**Corrección:** el `tap` ahora filtra `evento instanceof HttpResponse` antes de registrar.

**Pruebas:** `registro-de-acceso.interceptor.spec.ts`, 5 casos (bandera apagada, correcto,
límite sin marca, límite dos lecturas seguidas, inválido con `Avisos`/consola) — todos PASS,
`evidencia/H4-y-final-43-pass.txt`.

## No corregido en este carril (declarado, no arreglado — regla 00 §3)

Ver `entregables/decision-doble-clientes-angular.md`: siete errores de tipos preexistentes
en publicidad/contabilidad/cumplimiento bloquean el `build`/`test:front` **sin acotar** de
`apps/backoffice`. No son de este carril; quedan como hallazgo de baseline.
