# Gobierno de usuarios — inventario, brechas y preguntas

- Fecha: 2026-11-24 · Base: `origin/dev` cb651d9f (+ rama `claude/nombre-operador-20261124`)
- Método: lectura de requisitos (`docs/CasosDeUso/CU-04, CU-08, CU-09`, `Flujo de pantallas · backoffice administrador.md`, `Flujo funcional · usuario administrador.md`, `Seguridad.md`, ADR-030, ADR-038, semilla `sql/60_semillas/10-roles-y-permisos.sql`) contra código (`servicios/identidad`, `plataforma/comun-web`, `apps/backoffice/src/app/rutas/sistemas`) y capturas de las 4 pantallas de «Personal y accesos» contra el simulado.
- Peldaño: `DISCOVERED`. Nada de esto se ejecutó contra un backend real. GU-01 y GU-05 verificados a mano por la sesión principal.

## Por qué no se ve

1. Está al fondo del menú: Plataforma › Sistemas › «Personal y accesos» (`apps/backoffice/src/app/nucleo/menu.ts:163-172`); el requisito lo ubica en `operacion/roles` (AF-17, `Flujo de pantallas · backoffice administrador.md:301-304`).
2. Solo entran `ADMIN_PLATAFORMA` y `RESPONSABLE_SEGURIDAD` (`rutas/sistemas/guardia-rol-sistemas.ts:20,26`); `INTERNAL_IDENTITY_ADMIN`, el rol hecho para esto, rebota.
3. Con el simulado las pantallas muestran relleno automático («nombres apellidos», «roles[0]»): no hay ejemplos curados.
4. Varias piezas clave no funcionan de punta a punta (ver brechas altas).

## Lo que existe

| Capacidad | Backend (identidad) | Backoffice | Tests |
|---|---|---|---|
| Listar operadores (sin paginar) | `listarUsuariosInternos` | `/sistemas/usuarios-internos` | web + repositorio + specs |
| Ficha del operador | `consultarUsuarioInterno` | `/sistemas/usuarios-internos/:id` | ídem |
| Alta con clave provisional y rol | `crearUsuarioInterno` | modal de alta | spec + web |
| Editar / suspender / reactivar | `actualizarUsuarioInterno` (PATCH) | gestor de usuario | web |
| Reemplazar roles | `PUT /usuarios-internos/{id}/roles` → `RotacionDeRolesInternos` | modal asignar roles | web |
| Desbloquear | `desbloquearUsuarioInterno` | botón | web |
| Catálogo de roles y permisos | `listarRoles`, `listarPermisos` (solo lectura) | 2 pantallas de solo lectura | web + specs |
| Revisión de accesos | reusa `GET /usuarios-internos` | `/sistemas/accesos` (solo lectura) | spec mínimo |
| CU-08 por asignación (ámbito, vigencia, justificación) | `CU08AsignarRol`, `CU08RevocarRol` **sin controlador** | — | CU08 tests |
| Segregación de funciones | `SegregacionDeFunciones` (códigos inexistentes) | — | tests con los mismos códigos |
| No autoasignarse / no quedarse sin administrador | constraint + app | — | CU08 |
| Operador sin TOTP no entra | trigger + `CU04Autenticar` | AP-CU04-06 cae a error genérico | CU04 |
| Enrolar TOTP | `/sesion/factores/totp*` (exige sesión) | sin pantalla | web |
| Step-up | `/sesiones/desafios*` (solo nucleo-financiero) | — | tests |
| Cambio de clave / baja (CU-09) | casos de uso **sin endpoint** | — | CU09 tests |
| Recuperación asistida / reinscripción del factor | ausente | ausente | — |
| Bitácora de cambios de acceso | solo outbox, sin consumidor | — | — |
| Participantes: KYC y bloqueo por autoridad | sí | `/cumplimiento/verificaciones`, `/cumplimiento/bloqueos` | parcial |

## Catálogo de brechas

| ID | Brecha | Severidad | Dónde | Depende de |
|---|---|---|---|---|
| GU-01 | La segregación de funciones nunca se dispara: compara `ENTREGAS_AUTORIZAR`… que no existen (la semilla usa `ENTREGA_AUTORIZAR`); faltan los pares de contabilidad/tesorería y publicidad; los tests usan los mismos códigos inventados | **Alta** | identidad `SegregacionDeFunciones` + tests | P-3 |
| GU-02 | Un operador dado de alta no puede entrar: la clave provisional no se entrega por ningún canal y el enrolamiento TOTP exige una sesión que sin TOTP no se abre; no hay `acceso/enrolamiento` | **Alta** | identidad + backoffice `rutas/acceso` | P-1 |
| GU-03 | La clave provisional no se obliga a cambiar y no hay endpoint de cambio de clave (CU-09 sin controlador) | **Alta** | identidad + backoffice | P-1 |
| GU-04 | No existen recuperación de contraseña ni reinscripción del factor del operador (ADR-038, CU-09 §8-9); quien pierde el teléfono queda afuera | **Alta** | identidad (modelo) + backoffice | P-4 |
| GU-05 | La pantalla usa un reemplazo completo de roles sin ámbito, vigencia ni justificación; el CU-08 real está escrito y desconectado; tres documentos definen tres contratos distintos | **Alta** | identidad + backoffice | P-2 |
| GU-06 | Reemplazar roles reescribe quién y cuándo otorgó cada rol y cierra sesiones aunque solo se agregue uno | Media | identidad | P-2 |
| GU-07 | Los cambios de acceso no quedan en la bitácora ni avisan a nadie (requisito normativo) | **Alta** | identidad + auditoría (+ notificaciones) | P-5 |
| GU-08 | No vencen los accesos temporales ni hay informe de accesos | Media | identidad (trabajo + informe) + backoffice | P-6 |
| GU-09 | «Revisión de accesos» es solo lectura: «Ámbito» muestra el departamento, «Otorgado el» siempre «—», sin revocar | Media | identidad + backoffice | P-2, P-6 |
| GU-10 | Los bloqueos reales por intentos fallidos no se ven (se lee una columna vieja que nadie escribe) | Media | identidad (mapeo) | — |
| GU-11 | Las acciones de gobierno no piden step-up; nadie impide actuar sobre sí mismo ni suspender al último administrador | **Alta** | identidad + backoffice | P-7 |
| GU-12 | `INTERNAL_IDENTITY_ADMIN` no puede abrir las pantallas; `RESPONSABLE_SEGURIDAD` entra pero recibe 403 | Media | backoffice | P-2 |
| GU-13 | El selector de roles ofrece `PARTICIPANTE`, `ORGANIZADOR`, `SERVICIO_*`… y todos se asignan como GLOBAL | Media | identidad + backoffice | — |
| GU-14 | No hay baja de empleado: suspender deja los roles vivos y reactivar los devuelve sin revisión | Media | identidad + backoffice | P-2 |
| GU-15 | Revocar no corta en el acto: el token vive hasta 15 min; no hay vencimiento por inactividad | Media | identidad + plataforma | P-8 |
| GU-16 | Los roles no se pueden crear ni editar | Baja | identidad + backoffice | P-9 |
| GU-17 | El alta inventa `fecha_nacimiento = 1990-01-01`; «tiene segundo factor» cuenta factores sin confirmar; listado sin paginar | Baja–media | identidad | — |
| GU-18 | Las cuentas de servicio no tienen gobierno propio | Media | identidad | P-10 |

## Preguntas para Pablo (bloquean construir)

| ID | Pregunta | Desbloquea |
|---|---|---|
| P-1 | ¿Cómo recibe su primer acceso un operador nuevo? (a) enlace de invitación de un solo uso al correo institucional que lleva al enrolamiento TOTP; (b) mostrar la clave provisional una vez a quien da el alta; (c) sesión limitada solo para enrolar | GU-02, GU-03 |
| P-2 | ¿Contrato de CU-08 por asignación (`/accesos/asignaciones`, con ámbito, vigencia, justificación y revocación individual) o reemplazo completo? ¿Ubicación en `operacion/roles` (AF-17) o se queda en Plataforma › Sistemas? ¿Qué roles entran? | GU-05, GU-06, GU-09, GU-12, GU-14 |
| P-3 | Lista cerrada de pares incompatibles con los códigos reales | GU-01 |
| P-4 | Recuperación asistida: ¿se modela aprobador y plazo de enfriamiento (ADR-038 S-7)? | GU-04 |
| P-5 | ¿Quién se entera de cada asignación o revocación y por qué canal? ¿Basta la bitácora? | GU-07 |
| P-6 | ¿Campañas formales de recertificación de accesos o alcanza el informe semanal de CU-08? | GU-08, GU-09 |
| P-7 | ¿Cuatro ojos en alta, reactivación y desbloqueo, o alcanza con step-up + motivo? | GU-11 |
| P-8 | ¿Se aceptan hasta 15 min de token vivo tras revocar, o se implementa la lista de revocación (ADR-030)? ¿Inactividad del backoffice? | GU-15 |
| P-9 | ¿Roles editables desde la interfaz o catálogo fijo por semilla? | GU-16 |
| P-10 | ¿Cuentas de servicio gobernadas desde el backoffice o solo por despliegue? | GU-18 |
| P-11 | ¿Directorio/ficha del participante y suspensión de su cuenta desde el backoffice (con debido proceso)? Hoy ningún CU lo pide | — |

## Plan de sesiones hijas (borrador — no se lanza hasta responder P-1…P-11)

| Lote | Contenido | Archivos (disjuntos) | Arranca cuando |
|---|---|---|---|
| A | Correcciones de identidad sin decisión: GU-01, GU-10, GU-13 (backend), GU-17 | `SegregacionDeFunciones`, `UsuarioInternoMapeador`, consultas de `UsuariosInternosRepositorio`, `RolesYPermisosRepositorio`, tests | P-3 |
| B | CU-08 completo en backend: GU-05, GU-06, GU-07 (emisión), GU-08, GU-11, GU-14 | `identidad.yaml` (accesos), `AccesosController` nuevo, `CU08*`, `RotacionDeRolesInternos`, `GestionarUsuariosInternos`, `trabajos/` | P-2, P-5, P-6, P-7 |
| C | Credencial y factor: GU-02, GU-03, GU-04 | `SesionController`, CU-09 web, `CU04Autenticar`, `/cuenta/*`, DDL; backoffice `rutas/ingreso`, `rutas/acceso/*` | P-1, P-4; después de B (comparten `identidad.yaml`) |
| D | Backoffice de gobierno: GU-09, GU-12, UI de GU-05/08/11/13/14, reubicación del menú, ejemplos curados del simulado | `rutas/sistemas/{accesos,usuarios-internos,roles-internos}`, `guardia-rol-sistemas.ts`, `menu.ts`, matriz de roles | P-2; contra un doble del contrato de B |
| E | Auditoría y revocación en caliente: GU-07 (consumidor), GU-15 | `servicios/auditoria/consumo`, `plataforma/comun-web/seguridad`, `EmisorDeAcceso` | P-5, P-8 |

Fuera hasta tener decisión: GU-16 (P-9), GU-18 (P-10), P-11.
Máximo 4 sesiones a la vez (CLAUDE.md, regla 70); cada una con su PLAN.md, REPORTE.md, PR a `dev` y CI verde.

## Decisiones de Pablo (2026-11-24)

| Pregunta | Decisión |
|---|---|
| P-1 primer acceso | **Invitación por correo** de un solo uso y con vencimiento al correo institucional → elegir clave + enrolar TOTP. Desaparece la clave provisional. |
| P-2a contrato CU-08 | **Por asignación**: `ámbito`, vigencia opcional, justificación; revocación individual con motivo; se conserva quién y cuándo otorgó. |
| P-2b ubicación | **Sección propia visible** «Personal y accesos», abierta por permisos (incluye `INTERNAL_IDENTITY_ADMIN`); nunca se muestra a quien recibiría 403. |
| P-3 segregación | Pares: `ENTREGA_AUTORIZAR`↔`ENTREGA_EJECUTAR`; `CONTABILIDAD_ERP_PAGAR`↔`TESORERIA`/`ENTREGA_EJECUTAR`; `PUBLICIDAD_ANUNCIANTES`↔`PUBLICIDAD_APROBAR_CAMPANA`↔`PUBLICIDAD_MODERAR` (tres personas); `BLOQUEO_AUTORIDAD_REGISTRAR`↔`BLOQUEO_AUTORIDAD_LEVANTAR`. |
| P-3 bloqueo | `OFICIAL_CUMPLIMIENTO` registra; **rol nuevo `SUPERVISOR_CUMPLIMIENTO`** levanta (con lectura de cumplimiento). |
| P-4 recuperación | **Asistida a cuatro ojos** (ADR-038): pedido → aprobación de otra persona de seguridad → enfriamiento → invitación de un solo uso. Se modela aprobador y plazo. |
| P-5 avisos | **Bitácora + correo** a la persona afectada y al oficial de cumplimiento. |
| P-6 revisión | **Informe semanal + vencimientos** (accesos vivos, por vencer, sin TOTP) y caducidad automática de accesos temporales. |
| P-7 control | **Step-up + motivo**; nadie actúa sobre sí mismo; nunca sin administrador. |
| P-8 revocación | **Corte inmediato** (lista de revocación ADR-030, < 1 s) + cierre por **15 min de inactividad** en el backoffice. |
| P-9 roles | **Roles personalizados editables** desde la interfaz (los `es_sistema` siguen fijos). |
| P-10 servicio | **Cuentas de servicio gobernadas** desde el backoffice: inventario, alta y rotación. |
| P-11 participantes | **Sí, con debido proceso**: primero un CU nuevo en la bóveda (causales, plazos, apelación) que Pablo aprueba; recién después se implementa. |

## Plan de sesiones hijas (definitivo)

Reglas comunes: cada sesión parte de `dev` (con #512 integrada), trabaja en su rama, escribe PLAN.md/REPORTE.md en `docs/trabajo/2026-11-24-gu-<lote>/`, abre PR a `dev`, deja la CI en verde y avisa. Lotes disjuntos en archivos; máximo 4 a la vez (CLAUDE.md). La sesión principal revisa, mergea y promueve a `test`.

**Ola 1 (en paralelo):**

| Lote | Rama | Brechas | Archivos que posee |
|---|---|---|---|
| A — Segregación y correcciones de lectura | `claude/gu-a-segregacion` | GU-01 (pares de P-3), rol `SUPERVISOR_CUMPLIMIENTO`, GU-10, GU-13 (backend), `tieneSegundoFactor` y paginación de GU-17 | `dominio/SegregacionDeFunciones`, semilla de roles y permisos, `UsuarioInternoMapeador`, `RolesYPermisosRepositorio`, consultas `listar`/`consultar`, tests CU08 |
| B — CU-08 por asignación | `claude/gu-b-cu08` | GU-05, GU-06, GU-07 (bitácora + evento de aviso), GU-08, GU-11, GU-14 | `identidad.yaml` (accesos / usuarios internos), `AccesosController` nuevo, `CU08*`, `RotacionDeRolesInternos`, `GestionarUsuariosInternos`, `UsuariosInternosRepositorio.actualizar`, `trabajos/` |
| E — Revocación inmediata y auditoría | `claude/gu-e-revocacion` | GU-15, consumidor de GU-07 en auditoría, inactividad 15 min | `plataforma/comun-web/seguridad`, `EmisorDeAcceso`, `servicios/auditoria/consumo`, config de sesión del backoffice |
| G — CU de participantes (solo documento) | `claude/gu-g-cu-participantes` | P-11: CU nuevo con causales, plazos, apelación; pantallas propuestas | `docs/CasosDeUso/` (archivo nuevo) |

**Ola 2 (cuando B esté en `dev`):**

| Lote | Rama | Brechas | Depende de |
|---|---|---|---|
| C — Invitación, credencial y recuperación | `claude/gu-c-credencial` | GU-02, GU-03, GU-04, fecha de nacimiento inventada de GU-17 | B (comparten `identidad.yaml`) |
| D — Backoffice «Personal y accesos» | `claude/gu-d-backoffice` | GU-09, GU-12, UI de GU-05/08/11/13/14, sección propia, ejemplos curados del simulado | contrato de B |
| F — Roles personalizados y cuentas de servicio | `claude/gu-f-roles-servicio` | GU-16, GU-18 | B y D |

**Ola 3:** implementación de participantes, una vez aprobado el CU de G.
