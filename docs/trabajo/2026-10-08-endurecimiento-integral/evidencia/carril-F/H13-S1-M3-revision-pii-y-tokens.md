# H13.S1.M3 (parte documental) · Revisión de logs, URLs y eventos por PII y tokens en claro

Fecha: 2026-10-08. Alcance: **solo lectura** de los servicios nuevos o modificados por Codex en el árbol sin commitear de `PasanakuBackend` (39 archivos `src/main/*.java` que `git status --short` marca como modificados o nuevos, más `herramientas/proveedor_simulado/` y `herramientas/aliado_simulado/`). No se arregló nada. Peldaño: DISCOVERED (búsqueda por patrones y lectura de los hits); no se ejecutó ninguna prueba ni se corrió el servicio.

Patrones usados (Grep): llamadas de log (`BITACORA|LOGGER|log\.(info|warn|error|debug)|System\.out|printStackTrace|print\(|log_message`); URL armadas con datos (`.uri(`, `URI.create`, concatenaciones con `telefono|token|secreto|correo|documento`); mensajes de excepción con variables sensibles; campos sensibles nuevos en los OpenAPI (`git diff HEAD`); `Map.of` de eventos del outbox con `telefono|token|secreto|nombre|documento|correo|enlace|hash|nonce`; configuración de secretos.

## Hallazgos (a corregir por quien es dueño del código; no corregidos aquí)

| # | Severidad | Dónde (ruta:línea) | Qué es | Regla |
| --- | --- | --- | --- | --- |
| 1 | Media | `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java:206` | El cliente nuevo llama `.uri("/usuarios/por-telefono?telefono={t}", telefonoE164)`: el **teléfono E.164 viaja en la query string**, y queda en logs de acceso del gateway, proxies e historiales. Esa línea no existe en HEAD (`git show HEAD:…HechosPorHttp.java \| grep por-telefono` no devuelve nada). El contrato `GET /usuarios/por-telefono` con `telefono` como query ya está en HEAD (`servicios/identidad/src/main/resources/openapi/identidad.yaml:338` en HEAD; `:368-380` en el árbol de trabajo), así que el problema es del contrato y ahora además lo consume el cliente nuevo | 90.2.2 (prohibido PII en URL), 98.5.2 |
| 2 | Media-baja | `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/SecretoDeInvitacion.java:18-30` y `servicios/identidad/src/main/resources/application.yml:39` | La clave HMAC de invitaciones llega como `${INVITACIONES_CLAVE:}` con **valor por defecto vacío**; la longitud mínima (32) se valida **recién al primer uso** (`:29-30`), no al arrancar. El servicio arranca sano sin secreto y falla en la primera emisión de invitación | 90.3.4 (la configuración se valida al arrancar y el servicio falla si falta) |
| 3 | Baja | `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/DesafioDeDesarrollo.java:50` | `BITACORA.info("desafio {} de desarrollo para el usuario {}: el codigo es {}", …, CODIGO)` registra el código del segundo factor. **Pre-existente en HEAD** (no es de Codex) y el bean está acotado con `@Profile("local")` (`:32`), con un código constante de desarrollo, no un secreto real. Se lista solo para que no se active fuera de `local` | 90.2.9 |
| 4 | Observación | `sql/10_tablas/01_identidad_usuarios/alcance_invitacion.sql:9` (`telefono_destino VARCHAR(20) NOT NULL`) y `sql/10_tablas/02_grupos_turnos/invitacion.sql:8` (`telefono_invitado`) | El teléfono del invitado se guarda **en claro** en dos esquemas (necesario para validar destinatario). No es un incumplimiento por sí mismo, pero falta política de retención/minimización (decisión D-10 de E-H1-04) y confirmar que ningún log ni evento lo copie | 90.2.8, 97 |

## Controles encontrados (lo que sí está bien, con ruta)

- **Los logs de invitación registran solo identificadores**: `EmitirTokenDeInvitacion.java:93` (`emisorId`), `:140-145` (`tokenId`, `grupoId`, `emisorId`, `revocadasPorReemision`); `ConsumirInvitacion.java:59` (`tokenId`), `:118` (`tokenId`, `grupoId`), `:154` (`tokenId`, `emisorId`). Ninguno incluye teléfono ni secreto.
- **`toString` redactado**: `servicios/grupos/.../dominio/puertos/HechosDeOtrosServicios.java:75` y `servicios/identidad/.../aplicacion/EmitirTokenDeInvitacion.java:172` devuelven `token=REDACTADO`.
- **El secreto de invitación viaja en el cuerpo, no en la URL**, y es `writeOnly` en el OpenAPI (`token`, patrón `^[0-9a-f]{64}$`; `telefonoDestino` también `writeOnly`); en la ruta solo va `tokenId` (UUID, no secreto): `HechosPorHttp.java:164,179`. La descripción del contrato pide «no registrar ni incluir en URL de consulta».
- **Sin impresión ni log de peticiones en los servidores de prueba**: `herramientas/proveedor_simulado/servidor.py:51-53` y `herramientas/aliado_simulado/servidor.py:37-38` anulan `log_message` («sin URLs, credenciales ni importes»); los únicos `print` imprimen puerto y la leyenda «sin dinero real» (`:171` y `:135`).
- **Claves del proveedor simulado**: exige `PASANAKU_AMBIENTE=simulado` (`servidor.py:164-165`), escucha solo en `127.0.0.1`, claves de operación y control distintas y de al menos 32 caracteres (`:43`).
- **Mensajes de excepción**: la búsqueda de concatenaciones con variables sensibles en `ErrorDeNegocio`/`IllegalState` no devolvió ningún caso en los 39 archivos.
- **Eventos del outbox**: la búsqueda de `telefono|token|secreto|nombre|documento|correo|enlace|hash|nonce` junto a `Map.of`/`EventoDominio` en `EmitirTokenDeInvitacion`, `ConsumirInvitacion`, `CanjearInvitacion` y `CU68AceptarIngreso` no devolvió coincidencias.

## No cubierto
- No se ejecutó nada: ni los servicios, ni las pruebas, ni se capturó tráfico real; «no hay hit» significa que la búsqueda por patrones no lo encontró, no que no exista.
- No se revisaron los manejadores globales de errores, el gateway, los logs de acceso HTTP, la configuración de Logback ni el servicio de notificaciones (por donde sale el enlace de invitación al destinatario).
- No se inspeccionaron los tests por datos reales (las pruebas deberían usar datos sintéticos) ni el frontend.
- Los payloads de eventos se buscaron por palabras clave, no se leyeron uno por uno.
- «Rotación de secretos ensayada» (parte del DoD de H13.S1.M3, E-H13-03) **no** se hizo: esta entrega es solo la revisión documental. **H13.S1.M3 sigue A MEDIAS.**
