# Backlog — el administrador del backoffice no puede decidir (auditoría 2026-10-29)

Fuente: dos auditorías estáticas de solo lectura sobre `origin/test` (c6f9b28f) + la causa de admisiones
confirmada contra la base de TEST. Nada de esta lista se ejercitó todavía contra TEST salvo lo marcado.

Permisos reales de `ADMIN_PLATAFORMA` (`sql/60_semillas/10-roles-y-permisos.sql:89-163`): CATALOGO_EDITAR,
CATALOGO_DATOS_EDITAR, ACCESOS_ADMINISTRAR, TARIFARIO_PUBLICAR, AUDITORIA_LEER, SEGURIDAD_ACCESO_RESTABLECER,
SEGURIDAD_FACTOR_REINSCRIBIR, VERIFICACION_RESOLVER, SOLICITUD_INGRESO_VER, SOLICITUD_INGRESO_RESOLVER, RECLAMO_VER,
TABLERO_SISTEMAS_VER, USUARIOS_INTERNOS_*, ROLES_INTERNOS_*, PERMISOS_INTERNOS_LEER.

## Orden de ataque (una vuelta del loop = un ítem, con su PLAN/REPORTE)

| # | Ítem | Clase | Causa con evidencia | Necesita decisión de Pablo |
|---|---|---|---|---|
| 0 | Admisiones sin propuesta | PRODUCT_BUG | PR #235 | no (decidido) |
| 1 | Habilitación de organizador: el botón nunca aparece | PRODUCT_BUG | `rutas/cumplimiento/organizadores/pantalla-de-habilitacion.ts:26` depende de `consultarHabilitacion` `@Permiso("GRUPO_CREAR")` (`OrganizadoresController.java:135`) que el admin no tiene; el POST (`:153`, ADMIN_PLATAFORMA) sí pasaría. Además tipo de respuesta `Habilitacion` vs contrato `SalidaHabilitacion` (`organizador.yaml:328`). Solo se abre por UUID pegado | no |
| 2 | Reclamos: el admin ve la bandeja pero no puede abrir ni responder | SEED/PERMISO | detalle y respuesta piden RECLAMO_ATENDER (`ReclamosController.java:68,75`); admin solo RECLAMO_VER | **sí**: ¿el admin atiende reclamos? |
| 3 | Solicitudes escaladas solo por UUID pegado | PRODUCT_BUG | existe `listarBandejaDeAdmisiones` (SOLICITUD_INGRESO_VER) y la pantalla no la usa | no |
| 4 | Tarifario: no se puede publicar ni poner vigente | FALTA UI | backend `publicarTarifario`/`ponerTarifarioVigente` (`TarifasController.java:108,146`), admin tiene TARIFARIO_PUBLICAR; pantalla solo lectura | no |
| 5 | Reportes: no se pueden ejecutar | FALTA UI | `ejecutarReporte` (`ReportesController.java:90`, AUDITORIA_LEER); pantalla lo excluye | no |
| 6 | Sistemas: 7 pantallas sin fuente en producción + despliegues simulado | FALTA BACKEND | `proveedor-fuentes.compartido.ts` → `adaptadorFuenteNoDisponible`; `pantalla-despliegues.ts:127` operador fijo y TODO | sí: alcance |
| 7 | Roles internos: no se pueden editar | FALTA CONTRATO | ROLES_INTERNOS_GESTIONAR sembrado, sin operación en `identidad.yaml` | sí |
| 8 | No existen pantallas de grupos, participantes, garantía ni organizadores | FALTA MÓDULO | no hay carpetas en `rutas/` ni entradas en `nucleo/menu.ts` | **sí**: alcance y orden |
| 9 | UIF, requerimientos, continuidad, custodia, cierres: el admin ve y no decide | POLÍTICA | piden CUMPLIMIENTO_REPORTAR, OFICIAL_CUMPLIMIENTO, RESPONSABLE_RIESGOS, TESORERIA, CONTABILIDAD | **sí**: segregación de funciones (rol de cumplimiento) |
| 10 | Desembolsos, reversos, actas, eventos de riesgo, incidentes: backend con escrituras, UI sin acciones | FALTA UI | ver auditoría | sí: alcance |

Contabilidad y Publicidad no abren para ADMIN_PLATAFORMA por diseño de semillas (segregación), no por defecto.
