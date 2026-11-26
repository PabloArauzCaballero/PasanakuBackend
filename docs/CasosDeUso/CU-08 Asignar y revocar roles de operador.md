---
tags:
  - caso-uso
  - modulo/01-identidad-usuarios-y-seguridad
codigo: CU-08
criticidad: alta
actores: [Administrador de plataforma, Oficial de cumplimiento, Sistema]
normas: [ASFI Seguridad de la Información, ASFI control interno, segregación de funciones]
---

# CU-08 — Asignar y revocar roles de operador

> **Objetivo.** Que cada operador tenga exactamente los permisos que su función
> exige, por el tiempo que la exige, y que quitarle el acceso sea tan rápido y tan
> auditable como dárselo.

## Actores y disparador

- **Actor principal:** administrador de plataforma.
- **Disparadores:** alta de un empleado; cambio de función; licencia o baja;
  hallazgo de la revisión periódica de accesos; orden del oficial de cumplimiento.

## Precondiciones

1. Existe el [[rol]] con `codigo`, `ambito` y su conjunto de [[permiso]] en
   [[rol_permiso]]; los roles `es_sistema = true` no se editan desde la interfaz.
2. Quien otorga tiene `ACCESOS_ADMINISTRAR` (o `USUARIOS_INTERNOS_GESTIONAR`, con el que entra
   `INTERNAL_IDENTITY_ADMIN`, decisión P-2b), **confirma el acto con step-up** (`evidenciaMfa`
   de propósito `ADMIN`, un solo uso, decisión P-7) y **no es** el destinatario de la
   asignación (`R-SEG-07`, `ck_asignacion_no_autoasignada`). `R-SEG-04` es otra cosa
   —quien autoriza no ejecuta— y confundirlas produce la prueba equivocada.
3. El destinatario tiene [[usuario]] activo y perfil en [[usuario_interno]]. **Si el rol es de ámbito `GLOBAL`, el
   alta no termina en la asignación**: hasta que enrole su [[factor_mfa]] `TOTP` no
   puede abrir sesión, porque `R-SEG-10` no deja insertar la [[sesion]]
   ([[ADR-038 Acceso administrativo · segundo factor y recuperación asistida]]).

## Flujo principal

1. Se elige el [[rol]] y el `ambito` de la asignación. Los valores son los del enum
   `AmbitoRol`: **`GLOBAL`** para funciones internas de la plataforma, **`GRUPO`**
   cuando el rol solo aplica a un grupo concreto —en ese caso `ambito_id` guarda el
   `grupo_id` y **sin él la asignación se rechaza**— y `ORGANIZACION` para el alcance
   societario. No existe un ámbito `PLATAFORMA`: el que manda es el `.puml`.
2. Se fija `vigente_hasta` cuando el acceso es temporal: cobertura de vacaciones,
   consultoría externa, investigación puntual. Un acceso sin fecha de fin es una
   decisión, no un descuido, y así se pide confirmarla.
3. **En la misma transacción**:
   - se crea [[asignacion_rol]] con `usuario_id`, `rol_id`, `ambito`, `ambito_id`,
     `otorgada_por` y `otorgada_en`;
   - se registra en [[bitacora_evento]] quién otorgó qué a quién y con qué
     justificación;
   - se emite `evento_dominio` `rol.asignado`.
4. El permiso efectivo se calcula como la unión de los [[permiso]] de los roles
   **vigentes** del usuario: ni revocados, ni vencidos. La aplicación no cachea ese
   cálculo más allá de la vida del token de sesión.
5. Al revocar se escribe `revocada_en` y `motivo_revocacion`; **la fila no se
   borra** (`R-AUD-01`). Las [[sesion]] activas del usuario se invalidan en el acto:
   revocar un permiso y dejar viva la sesión que lo usaba no es revocar nada.
   **Agregar** un rol no cierra sesiones ni reescribe quién y cuándo otorgó los roles
   que el operador ya tenía (GU-06).
6. Un trabajo diario (`VencimientoDeAccesos`, 00:05 America/La_Paz) vence las asignaciones
   cuyo `vigente_hasta` pasó: escribe `revocada_en = vigente_hasta` con motivo
   `VENCIMIENTO`, cierra las sesiones del titular y deja `identidad.rol_vencido` con su
   pedido de aviso (al titular y al oficial de cumplimiento). Toma las filas
   `FOR UPDATE SKIP LOCKED` y la escritura exige `revocada_en IS NULL`: una segunda
   corrida, o una segunda réplica, no vence nada dos veces.
7. **El alta se cierra con el enrolamiento, no con la asignación.** Para un rol
   `GLOBAL` se avisa al destinatario que debe enrolar su TOTP y guardar sus códigos
   de respaldo (`acceso/enrolamiento`,
   [[Flujo de pantallas · backoffice administrador]] §2.0.3). Un operador con rol y
   sin factor es un operador que **no entra**: eso es lo correcto, y por eso el
   informe semanal de accesos vivos separa los que todavía no enrolaron.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | El rol es de ámbito `GRUPO` y no se indica `ambito_id` | Rechazo `AMBITO_INCOMPLETO`: un rol de grupo sin grupo sería un rol de plataforma disfrazado |
| 3a | La combinación de roles rompe la segregación de funciones | Rechazo `INCOMPATIBILIDAD_DE_FUNCIONES`: nadie autoriza y ejecuta el mismo tipo de operación (`R-SEG-04`). Pares cerrados (P-3, decisión de Pablo 2026-11-24): `ENTREGA_AUTORIZAR`↔`ENTREGA_EJECUTAR`; `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR`↔`CONTABILIDAD_ERP_PAGAR`; `BLOQUEO_AUTORIDAD_REGISTRAR`↔`BLOQUEO_AUTORIDAD_LEVANTAR`; `PUBLICIDAD_ANUNCIANTES`↔`PUBLICIDAD_APROBAR_CAMPANA`↔`PUBLICIDAD_MODERAR` (tres personas) |
| 3b | El rol es `GLOBAL` y el usuario no tiene TOTP | La asignación **se crea y es válida** —[[asignacion_rol]] no tiene columna de estado y no se inventa una—, pero el usuario no abre sesión hasta enrolar el factor (`R-SEG-10`). La respuesta lo dice con `SIN_MFA_CONFIGURADO` y deriva al enrolamiento |
| 5a | Se revoca el último rol con permiso de administración | Rechazo `ULTIMO_ADMINISTRADOR`: la plataforma nunca queda sin quien administre accesos. Se cuentan **personas** con la cuenta `ACTIVO` y una asignación **vigente** (sin revocar, ya otorgada, sin vencer) de `ACCESOS_ADMINISTRAR`, no filas: un segundo administrador suspendido o vencido no cuenta. Vale para revocar, suspender, dar de baja y el reemplazo deprecado |
| — | Sin step-up válido de quien actúa | Rechazo `STEP_UP_REQUERIDO`. Vale para asignar, revocar, alta, suspensión, reactivación, baja y desbloqueo (GU-11). Editar departamento o cargo no es un acto de gobierno y no lo pide |
| — | Quien actúa es el afectado | Rechazo `AUTOASIGNACION`: nadie se asigna, se revoca, se suspende, se reactiva, se da de baja ni se desbloquea a sí mismo |
| — | El destinatario no es un operador con la cuenta `ACTIVO` | Rechazo `DESTINATARIO_NO_HABILITADO` (precondición 3) |
| — | El ámbito pedido no es el del rol | Rechazo `AMBITO_INCOMPLETO`: el ámbito de la asignación es el del rol, como el que mira `R-SEG-10` |
| — | La misma asignación viva ya existe | Idempotente: responde la misma `asignacionId` con `yaExistia=true`, sin escribir ni anunciar nada. Revocar una ya revocada responde `yaEstabaRevocada=true` |
| — | Baja del empleado | Se revocan **todas** sus asignaciones en un acto (motivo `BAJA: …`), se cierran sus sesiones, la cuenta queda en `BAJA_LABORAL` —estado propio de la baja del personal interno, distinto de la baja voluntaria del participante (decisión de Pablo 2026-11-24)— y se conserva el histórico completo. Repetirla responde `yaEstabaDeBaja=true` |
| — | Suspensión del operador | Suspender revoca **todas** sus asignaciones vivas (motivo `SUSPENSION: …`) y cierra sus sesiones. **Reactivar devuelve la cuenta sin roles**: se vuelven a otorgar uno por uno, con su justificación. Así ningún rol queda vivo esperando una reactivación sin revisión (GU-14; decidido por Pablo el 2026-11-24: se reasignan a cuatro ojos) |
| 6a | Una asignación vencida todavía sin revocar al reasignar el mismo rol | Se la cierra como vencida (`revocada_en = vigente_hasta`) y se escribe la nueva |
| — | Acceso de emergencia fuera de horario | Se otorga con `vigente_hasta` de horas y queda marcado para revisión obligatoria del oficial de cumplimiento |

## Postcondiciones

- El permiso efectivo de cualquier operador es reconstruible a cualquier fecha
  pasada a partir de las asignaciones y sus vigencias.
- Ningún acceso queda vivo sin alguien que lo haya otorgado con nombre y fecha.

## Contrato · `openapi/identidad.yaml`

Rutas reales (la ruta `/accesos/...` que proponía este documento no está entre los prefijos
que el gateway enruta a `identidad`; quedó bajo `/usuarios-internos`, ambigüedad registrada
en `docs/trabajo/2026-11-24-gu-b/PLAN.md`):

| Operación | Ruta | Exige |
| --- | --- | --- |
| `asignarRolDeOperador` | `POST /usuarios-internos/{id}/asignaciones` (`Idempotency-Key`) | `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_GESTIONAR` + step-up |
| `revocarAsignacionDeRol` | `POST /usuarios-internos/asignaciones/{asignacionId}/revocacion` | ídem (es POST porque lleva el motivo en el cuerpo) |
| `darDeBajaUsuarioInterno` | `POST /usuarios-internos/{id}/baja` | ídem |
| `informeDeAccesos` | `GET /usuarios-internos/accesos/informe?seccion=VIVOS\|POR_VENCER\|SIN_TOTP&pagina&limite` | `ACCESOS_ADMINISTRAR` o `USUARIOS_INTERNOS_LEER` |
| `asignarRolesUsuarioInterno` | `PUT /usuarios-internos/{id}/roles` | **Deprecado**: compatible mientras el backoffice migra; aplica la diferencia (GU-06) |

```ts
export const EntradaCU08 = z.object({            // POST /usuarios-internos/{id}/asignaciones
  rolId:         z.string().uuid(),
  ambito:        z.enum(['GLOBAL', 'GRUPO', 'ORGANIZACION']),
  ambitoId:      z.string().uuid().nullable(),
  vigenteHasta:  z.string().datetime().nullable(),
  justificacion: z.string().min(10).max(200),
  evidenciaMfa:  z.string().min(20).max(4096),  // JWT step-up, proposito ADMIN, un solo uso
}).strict()

export const EntradaRevocarCU08 = z.object({     // POST .../asignaciones/{asignacionId}/revocacion
  motivoRevocacion:   z.string().min(10).max(120),
  cerrarSesiones:     z.boolean().default(true),
  evidenciaMfa:       z.string().min(20).max(4096),
}).strict()

export const SalidaCU08 = z.object({
  asignacionId:      z.string().uuid(),
  usuarioId:         z.string().uuid(),
  rolCodigo:         z.string(),
  ambito:            z.enum(['GLOBAL', 'GRUPO', 'ORGANIZACION']),
  ambitoId:          z.string().uuid().nullable(),
  permisosEfectivos: z.array(z.string()),
  requiereMfa:       z.boolean(),
  vigenteHasta:      z.string().datetime().nullable(),
  yaExistia:         z.boolean(),
}).strict()

export const ErroresCU08 = {
  ROL_INEXISTENTE:              'AP-CU08-01',
  AMBITO_INCOMPLETO:            'AP-CU08-02',
  INCOMPATIBILIDAD_DE_FUNCIONES:'AP-CU08-03',
  AUTOASIGNACION:               'AP-CU08-04',
  SIN_MFA_CONFIGURADO:          'AP-CU08-05',
  ULTIMO_ADMINISTRADOR:         'AP-CU08-06',
  LOTE_CON_REPETIDOS:           'AP-CU08-07',
  ROL_NO_ASIGNABLE:             'AP-CU08-08',
  STEP_UP_REQUERIDO:            'AP-CU08-09',
  DESTINATARIO_NO_HABILITADO:   'AP-CU08-10',
  MOTIVO_REQUERIDO:             'AP-CU08-11',
  FILTRO_DE_LISTADO_INVALIDO:   'AP-CU08-12',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `ROL_INEXISTENTE` | El `rolId` no existe o el rol fue dado de baja |
| `AMBITO_INCOMPLETO` | Ámbito `GRUPO` sin `ambitoId` |
| `INCOMPATIBILIDAD_DE_FUNCIONES` | La combinación resultante viola la segregación de funciones (`R-SEG-04`) |
| `AUTOASIGNACION` | Quien otorga es el destinatario: **nadie se amplía sus propios permisos** |
| `SIN_MFA_CONFIGURADO` | El rol es de ámbito `GLOBAL` y el usuario no tiene TOTP confirmado: la asignación queda escrita, pero no podrá entrar hasta enrolarlo (`R-SEG-10`) |
| `ULTIMO_ADMINISTRADOR` | La revocación dejaría la plataforma sin administrador de accesos |
| `LOTE_CON_REPETIDOS` | Al resolver los nombres de operadores que firmaron (`POST /usuarios-internos/nombres`), la lista repite un identificador |
| `ROL_NO_ASIGNABLE` | El rol existe pero no se otorga a personal interno: de participante (`PARTICIPANTE`, `ORGANIZADOR`, todo rol de ámbito `GRUPO`), `ANUNCIANTE` o una identidad de servicio `SERVICIO_*` (GU-13) |
| `STEP_UP_REQUERIDO` | El acto de gobierno no trae una evidencia step-up válida de quien actúa: ajena, vencida, de otro propósito o ya usada |
| `DESTINATARIO_NO_HABILITADO` | El destinatario no tiene perfil interno o su cuenta no está `ACTIVO` |
| `MOTIVO_REQUERIDO` | El alta de un operador llega sin motivo (el contrato lo deja opcional solo por compatibilidad) |
| `FILTRO_DE_LISTADO_INVALIDO` | `AP-CU08-12` · cursor a medias en `GET /usuarios-internos` (GU-17): llega `antesDe` sin `antesDeId` o al revés |

### Contrato de eventos (bitácora y aviso) · forma v1

Cada acto deja, **en la misma transacción** (outbox), dos eventos. El consumidor de la
bitácora es `auditoria` (lote E); el del aviso es `notificaciones`.

| Acción | `tipo` del evento de bitácora | `agregado` |
| --- | --- | --- |
| `ROL_ASIGNADO` | `identidad.rol_asignado` | `asignacion_rol` |
| `ROL_REVOCADO` | `identidad.rol_revocado` | `asignacion_rol` |
| `ROL_VENCIDO` | `identidad.rol_vencido` | `asignacion_rol` |
| `OPERADOR_DADO_DE_ALTA` | `identidad.usuario_interno_creado` | `usuario` |
| `OPERADOR_SUSPENDIDO` | `identidad.usuario_interno_suspendido` | `usuario` |
| `OPERADOR_REACTIVADO` | `identidad.usuario_interno_reactivado` | `usuario` |
| `OPERADOR_DADO_DE_BAJA` | `identidad.usuario_interno_dado_de_baja` | `usuario` |
| `OPERADOR_DESBLOQUEADO` | `identidad.usuario_interno_desbloqueado` | `usuario` |

Carga del evento de bitácora: `version` (`"1"`), `accion`, `actorId` (quien actuó; en
`ROL_VENCIDO` del trabajo diario es el id fijo del proceso), `actorRol` (el rol de la sesión
que actuó), `origen` (`PANEL_ADMIN` para los actos del backoffice, `TAREA_PROGRAMADA` para
el vencimiento: los valores del catálogo de `bitacora_evento` que lee el consumidor de
`auditoria`), `afectadoId`, `motivo` (la justificación o el motivo completo), `ocurridoEn`
(ISO-8601) y el detalle de la acción: `asignacionId`, `rolId`,
`rolCodigo`, `ambito`, `ambitoId`, `vigenteHasta`, `requiereMfa`, `sesionesCerradas`,
`asignacionesRevocadas`, `asignacionIds`, `estadoAnterior`, `vencioEn`, según corresponda.
Pedido de aviso `identidad.aviso_de_acceso_solicitado` (agregado `usuario`, id del
afectado): `version`, `eventoNotificable` = `ACCESO_DE_OPERADOR_CAMBIADO`, `eventoOrigen`
(el tipo de arriba), `accion`, `afectadoId`, `canal` = `CORREO` y `destinatarios`: el
afectado y los oficiales de cumplimiento vigentes y activos (decisión P-5). **Solo
identificadores**: el correo lo resuelve `notificaciones` por el canal verificado; nunca
viaja correo, teléfono ni documento (regla 90.2.1). Cambiar esta forma de forma
incompatible sube `version`.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `permisosEfectivos(asignaciones, ahora)` | Une los permisos de los roles vigentes a una fecha; puro |
| Átomo | `violaSegregacion(permisos)` | Detecta pares autorizar/ejecutar incompatibles; puro y con tabla de pares explícita |
| Molécula | `RolRepositorio` · `AsignacionRolRepositorio` | Persistencia y consulta por usuario y vigencia |
| Molécula | `InvalidadorDeSesiones` | Cierra las sesiones vivas del usuario afectado |
| Organismo | `CU08AsignarRol` · `CU08RevocarRol` | Transacción: valida, escribe, bitácora, evento e invalidación |
| Organismo | `GobiernoDeAccesos` · `CU08DarDeBaja` · `CU08VencerAccesos` · `CU08InformeDeAccesos` | Step-up, nadie sobre sí mismo, nunca sin administrador, idempotencia; baja, vencimiento e informe |
| Molécula | `BitacoraDeAccesos` | Evento de bitácora y pedido de aviso de cada acto, en la transacción del acto |
| Página | `RutasDeAccesos`, servidas por `UsuariosInternosController` (`POST /usuarios-internos/{id}/asignaciones` · `POST /usuarios-internos/asignaciones/{id}/revocacion` · `POST /usuarios-internos/{id}/baja` · `GET /usuarios-internos/accesos/informe`) | Traduce y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `rol.asignado` | Aviso al destinatario y al oficial de cumplimiento | `ACCESOS_ADMINISTRAR` |
| `rol.revocado` | Invalidación de sesiones y aviso | `ACCESOS_ADMINISTRAR` |
| `rol.vencido` | Trabajo diario de caducidad; informe semanal de accesos vivos | — |
| `identidad.aviso_de_acceso_solicitado` | Correo al afectado y al oficial de cumplimiento | — |

## Interfaz

- **App:** sin pantalla. Los roles de operador son internos.
- **Backoffice:** *Accesos*: matriz de usuarios por rol, con vigencia, quién otorgó
  y un botón de revocación inmediata; y el informe de accesos que vencen esta semana.

## Restricciones aplicables

`R-SEG-04` · `R-SEG-07` · `R-SEG-08` · `R-SEG-10` · `R-SEG-12` · `R-AUD-01` · `R-AUD-04`

## Evidencia que deja

[[asignacion_rol]] · [[rol]] · [[permiso]] · [[rol_permiso]] · [[bitacora_evento]] ·
`evento_dominio` · [[sesion]]

## Criterios de aceptación

```gherkin
Dado un rol de ámbito GLOBAL con permisos de cumplimiento
Cuando el administrador se lo asigna a un analista con MFA configurado
Entonces existe una asignacion_rol vigente
Y sus permisos efectivos incluyen los del rol

Dado un usuario que ya tiene el permiso de autorizar entregas
Cuando se le intenta asignar el rol que ejecuta entregas
Entonces se rechaza con INCOMPATIBILIDAD_DE_FUNCIONES

Dada una asignación vigente con una sesión abierta
Cuando se la revoca
Entonces la asignación queda con revocada_en y motivo
Y la sesión del usuario deja de ser válida

Dada una asignación con vigente_hasta en el pasado
Cuando corre el trabajo diario de caducidad
Entonces deja de contar en los permisos efectivos sin que nadie la borre

Dado un usuario sin factor TOTP
Cuando se le asigna un rol de ámbito GLOBAL
Entonces la asignacion_rol queda escrita y vigente
Y al intentar abrir su sesion la base la rechaza por R-SEG-10

Dado un administrador de plataforma
Cuando intenta asignarse a sí mismo un rol
Entonces la restricción ck_asignacion_no_autoasignada lo rechaza (R-SEG-07)

Dado un usuario con un rol vigente
Cuando se piden los codigos de rol
Entonces vienen
Y los de una asignacion revocada no

Dado un usuario sin roles
Cuando se piden los codigos de rol
Entonces no hay ninguno

Dado un operador activo sin el rol
Cuando el administrador se lo asigna por asignación con justificación y step-up
Entonces la asignación queda con quién y cuándo la otorgó
Y la bitácora registra rol_asignado con la justificación
Y se pide el aviso al afectado y al oficial de cumplimiento

Dada una asignación viva igual
Cuando se repite la misma asignación
Entonces se devuelve la misma asignación sin escribir otra ni emitir otro evento

Dado un operador con dos roles
Cuando se le agrega un tercero
Entonces las asignaciones existentes conservan quién y cuándo las otorgó
Y sus sesiones siguen abiertas

Dado un acto de gobierno sin evidencia step-up válida
Cuando se intenta
Entonces se rechaza con STEP_UP_REQUERIDO y no se escribe nada

Dado un administrador
Cuando intenta revocar su propia asignación
Entonces se rechaza con AUTOASIGNACION

Dado un único administrador de accesos activo
Cuando se revoca su asignación
Entonces se rechaza con ULTIMO_ADMINISTRADOR

Dado un segundo administrador con la cuenta suspendida
Cuando se revoca la asignación del único administrador activo
Entonces se rechaza con ULTIMO_ADMINISTRADOR

Dada una asignación revocada
Cuando se la revoca otra vez
Entonces responde que ya estaba revocada sin emitir otro evento

Dado un participante sin perfil interno
Cuando se le intenta asignar un rol
Entonces se rechaza con DESTINATARIO_NO_HABILITADO

Dado un operador con asignaciones y una sesión abierta
Cuando se lo da de baja
Entonces todas sus asignaciones quedan revocadas con motivo sin borrarse
Y su sesión se cierra
Y la cuenta queda en BAJA_LABORAL

Dado un operador dado de baja
Cuando se repite la baja
Entonces responde que ya estaba de baja sin emitir otro evento

Dado un operador con roles
Cuando se lo suspende
Entonces sus asignaciones se revocan
Y al reactivarlo vuelve sin roles

Dada una asignación vencida sin revocar
Cuando corre el trabajo diario de vencimiento
Entonces queda revocada en su vigente_hasta con motivo VENCIMIENTO
Y una segunda corrida no la vuelve a tocar

Dado un acceso que vence en tres días y un operador sin TOTP
Cuando se pide el informe de accesos
Entonces el primero aparece por vencer
Y el segundo aparece sin TOTP

Dado un operador con dos roles por el reemplazo deprecado
Cuando se reemplazan conservando uno
Entonces el que sigue conserva quién y cuándo lo otorgó
Y solo se revoca el que sale
```

## Ver también

[[CU-04 Autenticar con MFA y registrar dispositivo]] · [[CU-09 Cambiar credenciales y solicitar la baja]] · [[ADR-038 Acceso administrativo · segundo factor y recuperación asistida]] · [[Seguridad]] · [[CU-49 Designar al oficial de cumplimiento y capacitar]] · [[CU-55 Gestionar un incidente de seguridad]] · [[CU-90 Postular a organizador y habilitarse]] · [[CU-91 Firmar y rescindir el contrato de organizador]] · [[CU-135 Crear, editar y retirar roles personalizados con step-up y cuatro ojos]] · [[CU-136 Gobernar las cuentas de servicio con credencial de un solo uso]]
