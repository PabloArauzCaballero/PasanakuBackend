# Merge · grupos, identidad, aportes

Fecha: 2026-10-09 · Worktree `PasanakuBackend-cierre` · "ours" = lo que corre en TEST (Pablo), "theirs" = nuestros carriles.

## Decisiones por archivo

### identidad
| Archivo | Conflicto | Resolución |
|---|---|---|
| `EmitirTokenDeInvitacion` | firma de Pablo (canal, destino enmascarado, ip, agente) con token SHA-256 vs. emisión idempotente con `Entrada`, HMAC, límite diario y alcance | Gana la nuestra completa: el token ya no es un hash a secas y exige grupo y teléfono. Se conserva de Pablo la intención (ip, agente, correlación, clave de idempotencia en `token_verificacion`), que nuestra versión ya inserta. |
| `UsuariosController` | Pablo agrega `validarTokenDeInvitacion`, correo del alta; nosotros `consumirInvitacion`/`revocarInvitacion` (vía `InvitacionesController`) y `consultarNivelKyc` | Se conservan todos los endpoints y los dos colaboradores (`ValidarTokenDeInvitacion` y `ConsumirInvitacion`). La emisión usa `Entrada` y el agente se trunca a 255 como hacía Pablo. |
| `identidad.yaml` | `/validar` (Pablo) vs `/{tokenId}/consumos` y `/revocacion` (nuestro) | Las tres rutas conviven. `validar` no consume; `consumos` es el canje de un solo uso. |
| `ValidarTokenDeInvitacion` (no conflictiva, pero rota por la fusión) | comparaba `hash_token` con `digest(sha256)`; el token ahora se guarda como HMAC | Compara con `SecretoDeInvitacion.firmar("hash", token)` y exige además que el teléfono destino del alcance coincida con el del usuario de la sesión (más estricto que antes). |
| `CU69ValidarTokenDeInvitacionTest` (de Pablo) | construía emisor/validador con firmas viejas | Solo se adaptó el armado (constructor con `SecretoDeInvitacion`, `Entrada` para emitir). Las aserciones no cambian. |

### grupos
| Archivo | Conflicto | Resolución |
|---|---|---|
| `HechosDeOtrosServicios` / `HechosPorHttp` | `tokenDeInvitacion(canal, destino)` + `enlaceDeInvitacionValido` (Pablo) vs. `tokenDeInvitacion(clave, grupo, canal, telefono)`, `consumirInvitacion`, `revocarTokenDeInvitacion`, `nivelDeKyc` (nuestro) | Gana la firma nuestra de `tokenDeInvitacion` (identidad ya no emite sin grupo y teléfono; se eliminó el record `TokenDeInvitacion`). Se conserva `enlaceDeInvitacionValido`, que ante cualquier fallo de identidad devuelve falso (denegar por omisión). |
| `CU69Invitar` | Pablo: emisor y alta con rol de sistema (la fila de `participante` está reservada por RLS al proceso interno); nosotros: idempotencia por token, una invitación viva por destino, aceptación con recibo | Se conserva todo; `invitar` y `comprobarEmisor` corren con un contexto interno acotado (nuevo método privado `interno`), `aceptar(recibo)` sigue para el canje. |
| `InvitacionRepositorio` | Pablo borró `aceptar`; nosotros la ampliamos (token, grupo, instante del consumo) | Se conserva la nuestra. |
| `InvitacionesWeb` (Pablo) / `RutasDeInvitacion` (nuestro) | dos orquestadores de la invitación | `InvitacionesWeb.invitar(grupo, clave, cuerpo)` es ahora el único lugar con la lógica de invitar (comprobar emisor, supresión, ya-participa, emitir con clave, compensar revocando el enlace si la invitación no nace). Responde `token`, `expiraEn` y también `enlace` (`aportaya://unirse/...`). `RutasDeInvitacion` delega y añade `Cache-Control: no-store`; también expone `consultarInvitacionPorEnlace` y `aceptarInvitacionPorEnlace` de Pablo. |
| `GruposController` | rutas de invitación de Pablo vs. herencia de `RutasDeInvitacion`/`RutasDeAdmision` | Hereda de la cadena nuestra y conserva `listarSolicitudesDeIngreso`/`decidirSolicitudDeIngreso`/`listarMisParticipaciones` de Pablo. |
| `CU68AceptarIngreso` | Pablo: el organizador decide directo (`pendientes`, `decidir`, `solicitante`); nosotros: propuesta del admin + resolución de backoffice + decisión inmutable (`proponer`, `resolver`, `historial`) | Una clase con los dos caminos. Dos constructores: el de 4 argumentos (Spring, `@Autowired`) y el de 2 de Pablo (`Datos`, `Outbox`) para su camino directo. **Riesgo abierto**: el camino directo del organizador deja entrar sin pasar por backoffice. Se dejó porque corre en TEST y sus pruebas lo exigen; decisión de producto pendiente (ver abajo). |
| `CU68Postular` / `MapeoDePostulacion` | Pablo: la reputación mínima rechaza a quien tiene historial, no al nuevo (B24); nosotros: el motor nunca rechaza, marca `REVISAR_*`/`SIN_DATOS_*` para revisión humana | `EntradaPostulacion` gana el campo `reputacionExcluyente`. Constructor de 13 argumentos (el de Pablo, `sinHistorial`) = evaluación directa con rechazo 68-03 para quien tiene historial; constructores de 12 y 14 argumentos = recomendación humana. La ruta HTTP (`MapeoDePostulacion`) usa la forma de recomendación: **el comportamiento en producción es el nuestro**; el de Pablo queda como modo de la capa de aplicación. |
| `grupos.yaml` | `SalidaInvitacion` | Tiene `enlace` (Pablo), `token` y `expiraEn` (nuestro). Todos los esquemas y rutas de ambos lados conviven. |
| `CU69Test` | Pablo reescribió dos pruebas para el enlace (`CU69Enlace`) | Quedan las de Pablo con su nombre; las nuestras (aceptación con recibo de identidad) se conservan con otro nombre: `criterio2PorReciboDeIdentidad`, `concurrenciaDelReciboDeIdentidad`. |
| `GruposControllerWebTest`, `GruposPostulacionWebTest`, `SeguridadWebTest` | listas de `@MockitoBean` distintas | Unión de las dos listas. `GruposInvitacionesWebTest` (nuestra) importa `InvitacionesWeb` real y mockea `CU69Enlace` y `CU68AceptarIngreso`. |
| `InvitacionesWebTest` (Pablo) | usaba `tokenDeInvitacion(canal, destino)` y `invitar(grupo, cuerpo)` | Solo se adaptaron las firmas (clave, teléfono; `TokenInvitacion`). Aserciones intactas. |
| `CU69RolRealTest` (Pablo) | construía `EntradaInvitacion` sin `expiraEn` | Se añadió el argumento `expiraEn`. |

### aportes
| Archivo | Resolución |
|---|---|
| `AportesController` | Constructor con `ConsultarRecaudoDelPeriodo` (nuestro) y `HechosDeGrupos` (Pablo, admisibilidad B15/B5). |
| `aportes.yaml` | Se conservan los dos bloques de comentario (CU-100 de Pablo, recaudo del pozo nuestro); rutas de ambos. |
| `AportesControllerWebTest`, `SeguridadWebTest` | Unión de los `@MockitoBean` (`grupos` y datos de admisibilidad; `recaudos`; `cu100`). |

## Dos puertas de ingreso que conviven (riesgo para decidir)
1. Invitación: `POST /grupos/invitaciones/enlace/aceptar` (Pablo) crea el participante `ACTIVO` con reglamento firmado, sin expediente humano; `POST /grupos/{g}/invitaciones/{i}/canje` (nuestro) deja una solicitud `PENDIENTE_BACKOFFICE`. Si se quiere que toda entrada pase por admisión humana, hay que retirar o redirigir la de Pablo.
2. Solicitud: `decidirSolicitudDeIngreso` (organizador directo) vs `proponerAdmision`/`resolverAdmision`. Una solicitud resuelta por una vía no se vuelve a resolver por la otra (estado ya no `PENDIENTE`).
3. Aceptar por enlace valida el token en identidad sin consumirlo; el canje sí lo consume. Tras una aceptación por enlace, el canje del mismo token fallaría al aceptar localmente.
