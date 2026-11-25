---
tags:
  - caso-uso
  - modulo/01-identidad-usuarios-y-seguridad
codigo: CU-135
criticidad: alta
actores: [Administrador de identidades internas, Segunda persona aprobadora, Oficial de cumplimiento, Sistema]
normas: [ASFI Seguridad de la Información, ASFI control interno, segregación de funciones]
---

# CU-135 — Crear, editar y retirar roles personalizados con step-up y cuatro ojos

> **Objetivo.** Que la plataforma pueda definir un puesto nuevo (un rol con sus permisos)
> sin tocar la base ni desplegar, sin que nadie reúna funciones incompatibles y sin que un
> solo operador pueda darse, él solo, un rol que mueve plata o decide sobre la identidad de
> otras personas. Cada paso confirma con segundo factor, dice por qué y deja su rastro.

> Decisión de Pablo P-9 (2026-11-24, GU-16): «sí, los roles se crean y se editan desde el
> backoffice». Los roles `es_sistema` del catálogo (los sembrados) **no** se editan ni se
> retiran por acá: solo los personalizados.

## Actores y disparador

- **Administrador de identidades internas** (`ROLES_INTERNOS_GESTIONAR`): propone y aplica.
  Disparador: un puesto nuevo aprobado por la organización, o un cambio de funciones.
- **Segunda persona aprobadora** (`ROLES_INTERNOS_GESTIONAR`, distinta de quien propuso):
  resuelve las solicitudes a cuatro ojos.
- **Oficial de cumplimiento / Auditor** (`ROLES_INTERNOS_LEER`): lee las solicitudes.

## Precondiciones

1. Sesión de backoffice con segundo factor y una evidencia de step-up vigente por cada acto
   (se consume una sola vez).
2. Los permisos existen en el catálogo (`identidad.permiso`); no se inventan desde acá.
3. Para editar o retirar: el rol existe, no es `es_sistema` y no está retirado.

## Flujo principal

1. El operador abre «Roles internos» y elige *Crear rol*: código, nombre, descripción,
   permisos del catálogo y motivo. Confirma con su segundo factor.
2. El sistema valida la forma (código en mayúsculas, sin `SERVICIO_`, 3 a 30 caracteres),
   que los permisos existan y que **no reúnan un par incompatible** (`R-SEG-04`).
3. Si ningún permiso es de riesgo `ALTO` o `CRÍTICO` del catálogo, escribe el rol (`GLOBAL`,
   `es_sistema = false`, con quién y cuándo lo creó) y sus permisos, y deja
   `identidad.rol_creado` en la bitácora, todo en la misma transacción.
4. Si algún permiso es de riesgo `ALTO` o `CRÍTICO`, **no** crea el rol: escribe una
   solicitud `PENDIENTE` y deja `identidad.rol_solicitado`. Responde 202.
5. Otra persona abre la solicitud, la aprueba o la rechaza con su segundo factor y su motivo.
   Al aprobar se vuelven a comprobar todas las reglas y recién ahí se escribe el rol.
6. Editar sigue el mismo camino: el nombre, la descripción y los permisos cambian; el código
   no. Solo **agregar** permisos de riesgo `ALTO` o `CRÍTICO` exige cuatro ojos; quitar
   permisos nunca.
7. Retirar escribe `retirado_en` (la fila no se borra) y deja `identidad.rol_retirado`.

## Flujos alternativos

| Caso | Qué pasa |
| --- | --- |
| Código ya usado o con forma inválida | 422 `CODIGO_EN_USO` o `CODIGO_INVALIDO`; no se escribe nada |
| Un permiso no existe en el catálogo | 422 `PERMISO_INEXISTENTE` con la lista; no se inventa |
| Los permisos reúnen un par incompatible | 422 `PAR_INCOMPATIBLE` con el par; ni rol ni solicitud |
| Rol de sistema | 422 `ROL_DE_SISTEMA`: no se edita ni se retira |
| Un titular vigente quedaría con un par incompatible por el cambio | 422 `ASIGNADO_QUEDARIA_EN_CONFLICTO` |
| El rol tiene titulares vigentes y se quiere retirar | 422 `CON_ASIGNACIONES_VIGENTES`: se revocan primero |
| Quien propuso intenta resolver su propia solicitud | 422 `APROBADOR_ES_EL_PROPONENTE`; la base también lo impide |
| Sin evidencia de step-up válida o ya usada | 422 `STEP_UP_REQUERIDO`; no se escribe nada |
| Dos personas resuelven la misma solicitud a la vez | Gana una; la otra recibe `SOLICITUD_YA_RESUELTA` |

## Postcondiciones

El rol (o su cambio) existe exactamente una vez, con su autor y su fecha; cada acto tiene su
evento `identidad.rol_*` con `actorId`, `actorRol`, `origen` y `motivo`; ninguna persona
queda con un par incompatible de permisos; una solicitud rechazada no cambia ningún rol.

## Contrato · `openapi/identidad.yaml`

| Operación | Ruta | Exige |
| --- | --- | --- |
| `crearRol` | `POST /roles` | `ROLES_INTERNOS_GESTIONAR` + step-up |
| `editarRol` | `PATCH /roles/{rolId}` | `ROLES_INTERNOS_GESTIONAR` + step-up |
| `retirarRol` | `POST /roles/{rolId}/retiro` | `ROLES_INTERNOS_GESTIONAR` + step-up |
| `listarSolicitudesDeRol` | `GET /roles/solicitudes` | `ROLES_INTERNOS_LEER` |
| `aprobarSolicitudDeRol` | `POST /roles/solicitudes/{solicitudId}/aprobacion` | `ROLES_INTERNOS_GESTIONAR` + step-up |
| `rechazarSolicitudDeRol` | `POST /roles/solicitudes/{solicitudId}/rechazo` | `ROLES_INTERNOS_GESTIONAR` + step-up |

```ts
const ERRORES_CU135 = {
  CODIGO_INVALIDO:              'AP-CU135-01',
  CODIGO_EN_USO:                'AP-CU135-02',
  SIN_PERMISOS:                 'AP-CU135-03',
  PERMISO_INEXISTENTE:          'AP-CU135-04',
  PAR_INCOMPATIBLE:             'AP-CU135-05',
  ROL_DE_SISTEMA:               'AP-CU135-06',
  ROL_INEXISTENTE:              'AP-CU135-07',
  MOTIVO_REQUERIDO:             'AP-CU135-08',
  STEP_UP_REQUERIDO:            'AP-CU135-09',
  CON_ASIGNACIONES_VIGENTES:    'AP-CU135-10',
  SOLICITUD_INEXISTENTE:        'AP-CU135-11',
  APROBADOR_ES_EL_PROPONENTE:   'AP-CU135-12',
  ASIGNADO_QUEDARIA_EN_CONFLICTO: 'AP-CU135-13',
  SOLICITUD_YA_RESUELTA:        'AP-CU135-14',
} as const;
```

| Error | Cuándo se devuelve |
| --- | --- |
| `CODIGO_INVALIDO` | Código o nombre fuera de forma, o descripción demasiado larga |
| `CODIGO_EN_USO` | Ya hay un rol con ese código |
| `SIN_PERMISOS` | El rol no trae ningún permiso |
| `PERMISO_INEXISTENTE` | Algún permiso no está en el catálogo |
| `PAR_INCOMPATIBLE` | Los permisos reúnen un par de `SegregacionDeFunciones` |
| `ROL_DE_SISTEMA` | Se intentó editar o retirar un rol `es_sistema` |
| `ROL_INEXISTENTE` | El rol no existe o ya está retirado |
| `MOTIVO_REQUERIDO` | Motivo vacío o de más de 160 caracteres |
| `STEP_UP_REQUERIDO` | Falta la evidencia de segundo factor, venció o ya se usó |
| `CON_ASIGNACIONES_VIGENTES` | El rol a retirar tiene titulares vigentes |
| `SOLICITUD_INEXISTENTE` | La solicitud no existe |
| `APROBADOR_ES_EL_PROPONENTE` | Quien propuso intentó resolver |
| `ASIGNADO_QUEDARIA_EN_CONFLICTO` | Un titular vigente quedaría con un par incompatible |
| `SOLICITUD_YA_RESUELTA` | La solicitud ya no está pendiente |

### Contrato de eventos (bitácora) · forma v1

Tipos: `identidad.rol_creado`, `identidad.rol_editado`, `identidad.rol_retirado`,
`identidad.rol_solicitado`, `identidad.rol_solicitud_rechazada`. Carga: `version`, `actorId`,
`actorRol`, `origen` (`PANEL_ADMIN`), `motivo`, `ocurridoEn`, `rolCodigo`, y según el caso
`permisosAgregados`, `permisosQuitados`, `solicitudId`, `propuestaPor`. Nunca datos personales.
La clave del mensaje es el id del rol (o de la solicitud si el rol todavía no existe).

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `ReglasDeRolPersonalizado` | Forma del código, riesgo del conjunto, motivo; puro |
| Átomo | `SegregacionDeFunciones` | Pares incompatibles (lote A); se reusa |
| Molécula | `RolesPersonalizadosRepositorio` | Sentencias sobre `rol`, `rol_permiso` y `solicitud_cambio_rol` |
| Organismo | `CU135GobernarRoles` | Transacción: step-up, reglas, escritura, evento |
| Página | `RolesController` y «Roles internos» del backoffice | Traduce y presenta |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `identidad.rol_creado` · `rol_editado` · `rol_retirado` | Bitácora de auditoría (CAMBIO_PERMISOS) | `ROLES_INTERNOS_GESTIONAR` |
| `identidad.rol_solicitado` · `rol_solicitud_rechazada` | Bitácora | `ROLES_INTERNOS_GESTIONAR` |

## Interfaz

- **Backoffice:** *Sistemas › Personal y accesos › Roles internos*: crear, editar y retirar
  roles personalizados; solicitudes pendientes de la segunda persona; los roles del sistema
  se ven pero no se editan.

## Restricciones aplicables

`R-SEG-04` · `R-AUD-01`

## Evidencia que deja

[[rol]] · [[permiso]] · [[rol_permiso]] · [[solicitud_cambio_rol]] · [[bitacora_evento]] · `evento_dominio`

## Criterios de aceptación

```gherkin
Dado un operador con ROLES_INTERNOS_GESTIONAR y step-up vigente
Cuando crea un rol personalizado con permisos de riesgo bajo o medio
Entonces el rol existe como no de sistema, con sus permisos, y deja identidad.rol_creado con actor y motivo

Dado un operador con ROLES_INTERNOS_GESTIONAR y step-up vigente
Cuando crea un rol con ENTREGA_AUTORIZAR y ENTREGA_EJECUTAR
Entonces se rechaza con PAR_INCOMPATIBLE y no se escribe ningun rol

Dado un rol de sistema del catalogo
Cuando se intenta editarlo o retirarlo
Entonces se rechaza con ROL_DE_SISTEMA y el rol queda intacto

Dado un operador que crea un rol con un permiso de riesgo alto
Cuando otra persona aprueba la solicitud
Entonces el rol existe recien al aprobar y el evento nombra a quien propuso

Dada una solicitud pendiente
Cuando la persona que la propuso intenta aprobarla
Entonces se rechaza con APROBADOR_ES_EL_PROPONENTE y sigue pendiente

Dado un rol con un titular vigente que ya tiene otro rol
Cuando se le agrega un permiso que con el otro rol forma un par incompatible
Entonces se rechaza con ASIGNADO_QUEDARIA_EN_CONFLICTO

Dado un rol personalizado con un titular vigente
Cuando se intenta retirarlo
Entonces se rechaza con CON_ASIGNACIONES_VIGENTES y sin titulares queda retirado y no se asigna mas
```

## Ver también

[[CU-08 Asignar y revocar roles de operador]] · [[CU-04 Autenticar con MFA y registrar dispositivo]] · [[CU-136 Gobernar las cuentas de servicio con credencial de un solo uso]]
