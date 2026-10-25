# Recorrido del backoffice contra el backend real — consolidado por rol

Cada celda: **abre** (la ruta cargó y se quedó en ella), **→/ruta** (el router la redirigió: el rol no tiene permiso para esa sección) y entre paréntesis las llamadas a la API que fallaron (≥400).

| Ruta | Admin (SOPORTE+ADMIN_PLATAFORMA) | CONTABILIDAD | OPERADOR_PUBLICIDAD | OFICIAL_CUMPLIMIENTO | TESORERIA | RESPONSABLE_RIESGOS |
|---|---|---|---|---|---|---|
| `/tablero` | abre | abre | abre | abre | abre | abre |
| `/tablero/financiero` | →/tablero | abre | →/tablero | →/tablero | abre | →/tablero |
| `/tablero/sistemas` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/tablero/catalogo-de-datos` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/tablero/comercial` | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/operacion` | abre | abre | abre | abre | abre | abre |
| `/operacion/reclamos` | abre | →/tablero | →/tablero | abre | →/tablero | →/tablero |
| `/operacion/solicitudes-escaladas` | abre ⚠ (GET /grupos/solicitudes-escaladas 404) | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/operacion/retiros` | →/tablero | →/tablero | →/tablero | →/tablero | abre | →/tablero |
| `/operacion/custodia` | abre | abre | →/tablero | abre | abre | abre |
| `/operacion/cierres-diarios` | abre | abre | →/tablero | abre | abre | abre |
| `/operacion/reversos` | abre | abre | →/tablero | abre | abre | abre |
| `/operacion/desembolsos` | abre | abre | →/tablero | abre | abre | abre |
| `/operacion/estado` | abre | abre | →/tablero | abre | abre | abre |
| `/cumplimiento` | abre | abre | abre | abre | abre | abre ⚠ (GET /identidad/verificaciones 403) |
| `/cumplimiento/verificaciones` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/cumplimiento/alertas` | →/tablero | →/tablero | →/tablero | abre | →/tablero | →/tablero |
| `/cumplimiento/casos` | →/tablero | →/tablero | →/tablero | abre | →/tablero | →/tablero |
| `/cumplimiento/uif/reportes` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/cumplimiento/uif/requerimientos` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/cumplimiento/continuidad` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/cumplimiento/actas` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/cumplimiento/riesgos/eventos` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/cumplimiento/seguridad/incidentes` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/admisiones` | abre ⚠ (GET /grupos/solicitudes 404) | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas` | abre | abre | abre | abre | abre | abre |
| `/sistemas/servicios` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/despliegues` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/base-de-datos` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/respaldos` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/proveedores` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/outbox` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/webhooks` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/accesos` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/incidentes` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/usuarios-internos` | abre ⚠ (GET /usuarios-internos 422) | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/roles-internos` | abre ⚠ (GET /roles 422) | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/permisos-internos` | abre ⚠ (GET /permisos 422) | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/operaciones` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/flujos` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/bitacora` | abre ⚠ (GET /auditoria/bitacora 500) | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/sistemas/catalogos-operativos` | abre | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/contabilidad` | →/tablero | abre ⚠ (GET /erp/periodos/vigente 422) | →/tablero | →/tablero | abre ⚠ (GET /erp/periodos/vigente 422) | →/tablero |
| `/contabilidad/presupuesto` | →/tablero | abre | →/tablero | →/tablero | →/tablero | →/tablero |
| `/contabilidad/compras` | →/tablero | abre | →/tablero | →/tablero | →/tablero | →/tablero |
| `/contabilidad/cobros` | →/tablero | →/tablero | →/tablero | →/tablero | abre | →/tablero |
| `/ayuda` | abre | abre | abre | abre | abre | abre |
| `/publicidad` | →/tablero | →/tablero | →/publicidad/anunciantes | →/tablero | →/tablero | →/tablero |
| `/publicidad/anunciantes` | →/tablero | →/tablero | abre | →/tablero | →/tablero | →/tablero |
| `/publicidad/campanas` | →/tablero | →/tablero | abre | →/tablero | →/tablero | →/tablero |
| `/publicidad/moderacion` | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero | →/tablero |
| `/publicidad/desempeno` | →/tablero | →/tablero | abre | →/tablero | →/tablero | →/tablero |
| `/publicidad/liquidacion` | →/tablero | →/tablero | abre | →/tablero | →/tablero | →/tablero |
| `/configuracion` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/configuracion/tarifario` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/configuracion/mensajeria` | abre | →/tablero | →/tablero | abre | →/tablero | abre |
| `/configuracion/reportes` | abre | →/tablero | →/tablero | abre | →/tablero | abre |

| Rol | rutas que abre | de ellas con llamadas fallidas |
|---|---|---|
| Admin (SOPORTE+ADMIN_PLATAFORMA) | 42 / 57 | 6 |
| CONTABILIDAD | 14 / 57 | 1 |
| OPERADOR_PUBLICIDAD | 9 / 57 | 0 |
| OFICIAL_CUMPLIMIENTO | 24 / 57 | 0 |
| TESORERIA | 14 / 57 | 1 |
| RESPONSABLE_RIESGOS | 21 / 57 | 1 |
