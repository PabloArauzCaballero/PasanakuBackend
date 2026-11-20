# AUDITORÍA — identidad-cumplimiento (lote L4)

> Estado del documento: **EN CONSTRUCCIÓN**. Se escribe por tandas y cada tanda se sube a la rama
> `auditoria/identidad-cumplimiento-2026-11-20`. Las secciones marcadas «pendiente» todavía no se cerraron.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | identidad-cumplimiento (L4): `servicios/identidad`, `servicios/cumplimiento`, KYC, UIF/ASFI, retiros con doble aprobación |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — `git diff --stat origin/test origin/dev` vacío: **mismo árbol** |
| Historial | completo (`git fetch --unshallow`; `git rev-parse --is-shallow-repository` → `false`; 1878 commits en `origin/test`) |
| Archivos / líneas cubiertos | pendiente (anexo §12) |

## 2. Resumen

Pendiente de cierre. Tabla de severidades y diez más graves se completan al terminar la lectura.

## 3. Huella y mapa de conexiones

Pendiente.

## 4. Distancia al flujo nuevo

Pendiente (inventarios a, b y tabla c de ambigüedades A-1 a A-4).

## 5. Hallazgos

### IDCU-REESCRITURA-001 — CU-40 «Evaluar límites» existe dos veces; la copia de cumplimiento está muerta y diverge de la que mueve dinero
- Clase / Severidad / Confianza: REESCRITURA (también ACOPLE y DEFECTO) / S2 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java:34-98`;
  `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java:46-110,113-118`;
  `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:41-120` (la viva)
- Evidencia:
  - Javadoc de cumplimiento, `LimiteRepositorio.java:49-52`: «El consumo se lee con {@code FOR UPDATE} … leerlo sin bloquear la fila permite que dos operaciones simultaneas … pasen el tope diario».
  - La consulta (`:72-110`) es un `leftJoin(DSL.table(DSL.name("nucleo_financiero", "consumo_limite")))` **sin** `forUpdate()`: `grep -n 'forUpdate\|FOR UPDATE' LimiteRepositorio.java` → solo la línea 49 (el comentario).
  - La versión de nucleo-financiero sí bloquea: `var consumo = bloqueando ? limites.acumuladoBloqueado(...)` y ofrece `exigirDentroDe(dsl, …)` dentro de la transacción del dinero.
- Cómo se comprobó: `grep -rn 'CU40EvaluarLimites\b' servicios/cumplimiento/src` → solo pruebas (`ArmadoDeOla3.java:70`, `BaseDeCumplimiento.java:82`, `CU40Test`, `CU40RechazosTest`); ningún controlador ni consumidor de `servicios/cumplimiento/src/main/**/web` lo invoca; `grep` de una ruta `/limites` en el OpenAPI de cumplimiento → sin operación. Los consumidores reales (`CU11RetirarSaldo`, `SolicitudDeRetiro`, `CU12TransferirSaldo`, `CU10RecargarSaldo`) importan el de nucleo-financiero.
- Impacto: hoy ninguno en dinero (la copia no se ejecuta en producción), pero hay dos respuestas para la misma regla R-LIM-01: la copia muerta no bloquea, resuelve la ventana distinto (`ventana_inicio/ventana_fin` de la fila vs `VentanaDeLimite.resolver`) y emite `cumplimiento.limites_evaluados` con el `monto` en el payload. Si alguien la cablea creyendo el Javadoc, dos operaciones concurrentes pasan el tope. Las pruebas `CU40Test`/`CU40RechazosTest` dan cobertura a código que no corre y no a la regla viva.
- Causa de fondo: CU-40 se implementó en dos carriles; la decisión de en qué servicio vive quedó escrita como «divergencia declarada» (`LimiteRepositorio.java:59-71`) en lugar de resolverse.
- Solución de fondo: un único CU-40 en `nucleo-financiero` (dueño de `consumo_limite` y de la transacción); retirar la copia de cumplimiento con sus pruebas; si cumplimiento necesita el rastro, consumirlo por evento (`nucleo_financiero.limite_rechazo`) en vez de leer el esquema ajeno.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo-financiero (lote L1), esquema `nucleo_financiero` (`consumo_limite`, `cuenta_billetera`), `catalogo.limite_operativo_billetera`
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad) y Fiabilidad; reglas 98.1.1 (lectura de base ajena), 96.3.1, 00 §1.1 (pieza duplicada)
- Esfuerzo estimado: chico

### IDCU-ACOPLE-002 — Cumplimiento lee tablas del esquema `nucleo_financiero`
- Clase / Severidad / Confianza: ACOPLE / S3 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java:83-84` (`nucleo_financiero.consumo_limite`), `:113-118` (`nucleo_financiero.cuenta_billetera`)
- Evidencia: `.from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))`; el propio archivo lo declara: «INVARIANTE-11 DECLARADO · `consumo_limite` vive en `nucleo_financiero`» (`:60`).
- Cómo se comprobó: lectura del archivo; `python3 scripts/auditar_backend.py` lo lista en «DIVERGENCIAS DECLARADAS» (`evidencia/auditar_backend.txt`).
- Impacto: un cambio de columna en `nucleo_financiero` rompe cumplimiento sin que ninguna prueba de contrato lo avise; el rol de base de cumplimiento necesita permisos sobre el esquema ajeno. Severidad S3 porque el único camino que lo usa es la copia muerta de IDCU-REESCRITURA-001.
- Causa de fondo: la misma que IDCU-REESCRITURA-001.
- Solución de fondo: se resuelve al retirar la copia; si cumplimiento necesitara el nivel de diligencia de la cuenta, pedirlo por API o recibirlo por evento.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo-financiero
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad); regla 98.1.1 y 98.1.2
- Esfuerzo estimado: chico

### IDCU-DEFECTO-003 — La fecha de vigencia de los límites se calcula en UTC, no en `America/La_Paz`
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java:51`
- Evidencia: `var hoy = reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate();` — mientras `plataforma/comun-dominio/.../Reloj.java:26-30` ofrece `ZONA = ZoneId.of("America/La_Paz")` y `hoy()`.
- Cómo se comprobó: lectura de ambos archivos.
- Impacto: entre las 20:00 y las 24:00 de La Paz (UTC-4) el código ya considera «mañana» para `vigente_desde`/`vigente_hasta`: un tope que vence hoy deja de aplicarse cuatro horas antes y uno nuevo entra cuatro horas antes. Hoy acotado porque la copia no se ejecuta (IDCU-REESCRITURA-001).
- Causa de fondo: no se usa `Reloj.hoy()`.
- Solución de fondo: usar `reloj.hoy()` en todo cálculo de fecha calendario; se resuelve junto con la unificación de CU-40.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: la copia viva de nucleo-financiero tiene el mismo patrón (ver §10).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); ISO 8601
- Esfuerzo estimado: chico

### IDCU-SEG-004 — TEST corre con perfil `local`: segundo factor «000000» para cualquiera y clave JWT en memoria permitida
- Clase / Severidad / Confianza: SEG (también CONFIG y REG) / S2 / confirmado (por documentación y código; el entorno TEST no se tocó)
- Ubicaciones: `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java:73-80,82-91`; `despliegue/TEST.md:86-88,458`; `docker-compose.coolify.yml:448`
- Evidencia:
  - `EmisorDeAcceso.java:74-75`: `boolean produccion = entorno.acceptsProfiles(Profiles.of("staging", "production")); if (generada && produccion) { throw … }` → con cualquier otro perfil, una clave vacía genera una **en memoria** (`:80`) y solo deja un `WARN` (`:89`).
  - `despliegue/TEST.md:86-87`: «TEST corre con el perfil `local`, que trae el segundo factor de desarrollo: **el código es `000000` para cualquiera**».
  - `docker-compose.coolify.yml:448`: `JWT_CLAVE_FIRMA: ${JWT_CLAVE_FIRMA}` sin `:?` que obligue a definirla.
- Cómo se comprobó: lectura; no se accedió al servidor TEST (prohibido por el metaprompt §5).
- Impacto: en un servidor expuesto, el segundo factor del backoffice no protege nada (cualquier credencial de operador filtrada basta); con dos réplicas y la clave vacía, cada réplica firma con la suya. Hoy TEST no maneja dinero real; pero el perfil es la única barrera entre «desarrollo» y «compartido», y el fail-closed no cubre el perfil que efectivamente usa el entorno compartido.
- Causa de fondo: el fail-closed se ata a nombres de perfil (`staging`, `production`) y no a «entorno compartido»; TEST quedó en `local` como decisión documentada.
- Solución de fondo: un perfil propio para TEST que exija `JWT_CLAVE_FIRMA` y deshabilite el segundo factor fijo, o invertir la lógica (fallar salvo perfil explícitamente descartable); `${JWT_CLAVE_FIRMA:?}` en el compose.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios (validan contra el JWKS de identidad); backoffice
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); OWASP ASVS (autenticación multifactor); regla 90.3.4 (config que no falla al arrancar); habilitación de dinero real condicionada al dossier H15
- Esfuerzo estimado: chico

### IDCU-SEG-005 — Los servidores de recurso no validan emisor ni audiencia; el token de acceso no los lleva
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/ConfiguracionDelDecodificador.java:27-28` (punto de contacto, transversal); `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java:112-121` (acceso sin `iss`/`aud`), `:155-157` (evidencia step-up con `iss`/`aud`, misma clave)
- Evidencia: `return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();` (sin `setJwtValidator`); el constructor del token de acceso no llama `.issuer(...)` ni `.audience(...)`.
- Cómo se comprobó: lectura. Refutación: `SesionDeLaPeticion.java:71-77` rechaza todo token sin claim `rol`, y la evidencia step-up no lo trae → hoy un token de evidencia no sirve como token de acceso. Por eso queda S3 (defensa en profundidad), no S1.
- Impacto: la separación entre tipos de token depende de que ningún otro token firmado con la misma clave lleve `rol`; cualquier token futuro firmado por identidad con ese claim sería aceptado por los catorce servicios.
- Causa de fondo: hallazgo previo `docs/auditoria-produccion/security-matrix.md:52` («JWT sin `iss`/`aud`») — **sigue vigente** en el commit auditado (la parte de clave efímera fue cerrada para `staging`/`production`, ver IDCU-SEG-004).
- Solución de fondo: emitir `iss` y `aud` en el token de acceso y validar ambos en el decodificador compartido.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios; `plataforma/comun-web` (lote L5)
- Estándares y normas: ISO/IEC 25010 Seguridad; OWASP API Security Top 10 (API2 Broken Authentication); regla 98.6.1
- Esfuerzo estimado: chico

### IDCU-DOC-006 — El cliente de aprobación de retiros dice que no existe la bandeja que el archivo vecino consume
- Clase / Severidad / Confianza: DOC (también PISADA) / S4 / confirmado
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/retiros/dominio/cu11-aprobacion-de-retiro.ts:10-12,15-18`; `apps/backoffice/src/app/rutas/operacion/retiros/dominio/cu11-retiros-en-revision.ts:3-4,12`; `servicios/nucleo-financiero/src/main/resources/openapi/*.yaml:457-460`
- Evidencia: «El contrato NO publica un listado ni una consulta de órdenes en revisión: la pantalla trabaja con el identificador que llega en el aviso» vs `GET /billetera/retiros` («Bandeja de retiros que esperan la segunda aprobacion») usado en `cu11-retiros-en-revision.ts:12`. Además `cu11-aprobacion-de-retiro.ts` declara los tipos a mano (`:15-18`) mientras el vecino importa el cliente generado (`clientes/angular/nucleo-financiero/model/...`).
- Cómo se comprobó: lectura de ambos archivos y `grep -n retiros` del OpenAPI de nucleo-financiero.
- Impacto: comentario que miente y dos convenciones para el mismo contrato en la misma carpeta: si el OpenAPI cambia `EntradaAprobacionRetiro`, el tipo escrito a mano no se entera.
- Causa de fondo: dos agentes en momentos distintos (el listado se agregó después).
- Solución de fondo: usar los tipos del cliente generado y borrar el comentario obsoleto.
- Relación con el flujo nuevo: APROB (pantalla de la doble aprobación de retiros, ver §4.a)
- Conexiones y módulos afectados: nucleo-financiero (contrato)
- Estándares y normas: ISO/IEC 25010 Mantenibilidad; regla 96.4.1
- Esfuerzo estimado: chico

## 6. Matriz de contratos

Pendiente.

## 7. Evidencia obligatoria del módulo

Pendiente.

## 8. Cobertura por clase

Pendiente.

## 9. Lo que está bien

- No hay confianza en cabeceras de identidad sin firmar: `grep -rniE 'X-User|X-Usuario|X-Actor|X-Rol'` sobre identidad, cumplimiento y `comun-web` → sin resultados; la identidad se resuelve del JWT validado (`SesionDeLaPeticion.java:66-84`).
- El emisor de tokens falla al arrancar sin clave en `staging`/`production` (`EmisorDeAcceso.java:75-79`).

## 10. Observado fuera de alcance

- `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:80,132` — `reloj.ahora().atOffset(ZoneOffset.UTC)` para la vigencia de límites (mismo patrón que IDCU-DEFECTO-003; lote L1).

## 11. Límites de esta auditoría

- Gradle no corrió: `./gradlew :servicios:identidad:testBarrido` → `BUILD FAILED`, «Received status code 429 from server: Too Many Requests» al resolver dependencias de `buildSrc` (`evidencia/gradle-identidad-testBarrido.txt`). Además la generación jOOQ exige PostgreSQL con el esquema aplicado (`buildSrc/src/main/kotlin/aportaya.jooq.gradle.kts:22`) y el metaprompt prohíbe aplicar migraciones. Todas las pruebas Java del módulo y el barrido de tamaño/umbrales/secretos quedan **NO COMPROBADO**.
- `yarn typecheck` (backoffice) no corrió: «packageManager yarn@4.18.0 … current global version of Yarn is 1.22.22» (Corepack deshabilitado; no se instaló nada). Flutter/Dart no están instalados. **NO COMPROBADO**.
- No se ejecutaron `scripts/inventario_endpoints.py` ni `scripts/matriz_de_pruebas.py` sin `--check`/`--servicio` porque escriben archivos del repo; `inventario_endpoints.py --check` sí corrió.

## 12. Anexo de cobertura de archivos

Pendiente.
