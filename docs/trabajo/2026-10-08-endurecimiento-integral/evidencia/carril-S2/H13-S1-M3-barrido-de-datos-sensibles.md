# H13.S1.M3 · Protección de datos y secretos — barrido automatizado y rotación ensayada

CA del plan: «Dados logs, URLs y eventos, cuando se inspeccionan, entonces no contienen tokens ni PII
innecesaria» (E-H13-03: muestras sintéticas y rotación de secretos ensayada). Reemplaza y completa la revisión
documental `evidencia/carril-F/H13-S1-M3-revision-pii-y-tokens.md`, que sólo miraba 39 archivos.

## 1 · Barrido de fuentes (estático, repo entero)

`plataforma/comun-pruebas/.../seguridad/SalidasSensibles.java` + `SalidasSensiblesBarridoTest` (corre en
`testBarrido`): lee `src/main/java` de los 15 servicios y de la plataforma y busca datos sensibles en las **cuatro
salidas**: argumentos de log, URLs, payload de `EventoDominio`, y texto de excepciones (más `System.out` y
`printStackTrace`). Heurística de nombres a propósito estrecha (si grita seguido se apaga): `telefono`, `correo`,
`password`, `secreto`, `token`, `clave`, `documento`, `otp`… menos los sufijos inofensivos (`Id`, `Hash`,
`Tipo`, `Longitud`…). La lista de deuda conocida (`src/test/resources/seguridad/salidas-sensibles-conocidas.txt`)
**no crece sola y no se pudre**: un hallazgo nuevo rompe el barrido, y una línea que ya no existe también.

`SalidasSensiblesDeteccionTest` (7 pruebas) le da muestras **sintéticas** que filtran teléfono, token, correo y
documento por cada salida, y muestras limpias (ids, hashes, longitudes) que no deben gritar.

Resultado sobre el repo (9 hallazgos al inicio; 1 corregido en este carril):

| Dónde | Salida | Qué | Estado |
|---|---|---|---|
| `servicios/grupos/.../clientes/HechosPorHttp.java:224` | URL | teléfono E.164 en la query (`/usuarios/por-telefono?telefono={t}`) | deuda declarada (grupos + identidad; contrato) |
| `servicios/identidad/.../DesafioDeDesarrollo.java:50` | LOG | «el codigo es {}» (segundo factor; solo perfil `local`, constante) | deuda declarada (identidad) |
| `servicios/erp/.../CU102AltaDeTercero.java:73` | EVENTO | `numeroDocumento` del tercero en el payload a todos los consumidores | deuda declarada (erp) |
| `servicios/publicidad/.../CU110AltaDeAnunciante.java:68` | EVENTO | idem, del socio comercial | deuda declarada (publicidad) |
| `servicios/erp/.../CU102AltaDeTercero.java:52` | ERROR | el mensaje repite el documento enviado | deuda declarada (erp) |
| `servicios/cumplimiento/.../CU03DeclararPep.java:93` | ERROR | el mensaje repetía el **nombre** del beneficiario final | **CORREGIDO**: ahora «el beneficiario final número N de la lista» (`CU03Test` lo sigue exigiendo) |

## 2 · Barrido de secretos con valor por omisión

`SecretosSinDefaultBarridoTest`: ningún `${SECRETO:...}` de los `application*.yml` puede arrancar con default
(90.3.4). Hoy hay 7 con default vacío, declaradas con su razón en la prueba (identidad: `JWT_CLAVE_FIRMA`,
`INVITACIONES_CLAVE`, `GMAIL_*`; inversiones: `INVERSIONES_ALIADO_*`, `INVERSIONES_LIBRO_CREDENCIAL_SERVICIO`). `INVITACIONES_CLAVE` es la preocupante: el
servicio arranca sano y falla recién al emitir la primera invitación (`SecretoDeInvitacion.java:29-30`).

## 3 · Barrido de logs REALES (dinámico)

`despliegue/ensayos/barrido_de_logs.py` lee `<system-out>`/`<system-err>` de los 494 informes de prueba que
dejó la corrida del repo (176 con salida; 6 168 líneas de log de servicios corriendo contra PostgreSQL real) y
busca teléfonos E.164, correos, JWT, `Bearer`, tokens hex de 64, pares `clave=valor` de secreto y documentos
con etiqueta. Salida literal: `barrido-de-logs-reales.log`.

- **0** teléfonos, correos, JWT, Bearer ni documentos en los logs de las pruebas.
- Las 43 coincidencias de «hex de 64» eran **ids de contenedor de Testcontainers** (declarado como contexto
  inofensivo en el script, no por valor).
- **2 hallazgos reales**:
  1. `plataforma/comun-web/.../errores/ManejadorGlobalDeErrores.java:213` (`BITACORA.error("fallo no previsto", e)`) y `:119`
     (`BITACORA.error("restriccion sin traduccion…: {}", mensajeCrudo, e)`): ante una excepción inesperada **loguea el
     mensaje completo**; el `mensajeCrudo` de una violación de unicidad de PostgreSQL puede traer el valor (`Key (col)=(valor)`, no ejercitado). La prueba `ManejadorGlobalDeErroresWebTest` lanza una con `jdbc:postgresql://… password=…`
     (sintético) para probar que la RESPUESTA no filtra; el LOG sí contiene la contraseña. En producción una
     excepción de driver con cadena de conexión terminaría en el log (90.2.1/90.3). Arreglo: loguear clase y
     `correlationId`, y enmascarar el mensaje.
  2. `plataforma/gateway`: el log de arranque imprime `Using generated security password: …` (la autoconfiguración
     de usuario por omisión de Spring Security sigue activa). Arreglo: excluir `UserDetailsServiceAutoConfiguration`
     o declarar la seguridad del gateway.

## 4 · Rotación de secretos ensayada

`despliegue/ensayos/rotacion/ensayo_rotacion.py` + `docs/operacion/rotacion-de-secretos-ensayada.md` +
`RotacionDeSecretosContratoTest`. Resultado literal en `rotacion-ensayo.log` (20 comprobaciones, 0 fallas):

- Inventario: los **10** secretos que el compose desplegado inyecta tienen fila (el runbook anterior listaba 4).
- Higiene: ningún valor de desarrollo aparece en `docker-compose.coolify.yml`.
- JWT: con la clave única de hoy, rotar invalida **200/200** sesiones; con ventana de solapamiento, 0, y retirar la
  clave vieja invalida lo firmado con ella. Hallazgo: `EmisorDeAcceso.java:143` publica un JWKS de una sola clave.
- HMAC y pimienta: ventana de dos claves y migración perezosa, con el conteo de lo que falta para retirar la vieja.

## No cubierto

- La heurística es por **nombre**: un dato sensible con nombre inocente (`valor`, `dato`) pasa. Un `Map.of("x", v)`
  con `v` sensible no se detecta si `v` no se llama como un dato sensible.
- El ensayo de rotación usa material sintético y una réplica de la mecánica; **no rota nada en los servicios ni
  en Coolify** y no mide el tiempo real de un reinicio en producción.
- Los logs revisados son los de las pruebas (datos sintéticos): demuestran lo que el código escribe, no lo que
  producción contiene. No se revisaron logs de acceso del gateway/proxy ni la configuración de Logback.
- El frontend (PasanakuFrontend) y los scripts Python/shell no entran en el barrido de fuentes.


## Cierre en el carril A3 (2026-10-10)

`ManejadorGlobalDeErrores` ya no escribe el mensaje ni el throwable (`RastroSeguro`: clase, SQLSTATE, restriccion, `trazaId`;
`BitacoraDeErroresSinContenidoWebTest`), el log del gateway ya no imprime «Using generated security password» (0 ocurrencias),
`DesafioDeDesarrollo` no loguea el codigo, y `HechosPorHttp` usa `POST /usuarios/busquedas/por-telefono`. La clave de firma JWT
admite ventana de dos claves (`JWT_CLAVE_FIRMA_ANTERIOR`). Siguen declaradas en `salidas-sensibles-conocidas.txt`: documento en el
outbox de erp y publicidad y el eco en el error de erp (duenios: erp, publicidad). Detalle en `evidencia/carril-A3/RESULTADO.md`.
