# Inventario de endpoints — generado por `scripts/inventario_endpoints.py`

> No editar a mano: se regenera con `python3 scripts/inventario_endpoints.py`. El CI falla (`--check`) si un endpoint implementado no esta en su OpenAPI, o viceversa (H2.S1 del carril PR4-seguridad).

**Total de endpoints relevados:** 158.

| Service | Method | Path | Permission | Ownership | Idempotency | Rate limit | MFA | Audit | Tests | Sensible |
|---|---|---|---|---|---|---|---|---|---|---|
| aportes | GET | `/aportes/grupos/{grupoId}/morosos` | BILLETERA_VER | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| aportes | POST | `/aportes/obligaciones/{obligacionId}/pagos` | BILLETERA_OPERAR | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| aportes | GET | `/aportes/participantes/{participanteId}/estado` | BILLETERA_VER | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| aportes | POST | `/pagos/enrutamiento` | BILLETERA_OPERAR | n/a | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| aportes | POST | `/pagos/proveedores` | ADMIN_PLATAFORMA | n/a | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| aportes | POST | `/pagos/reembolsos/{reembolsoId}/aprobacion` | REVERSO_AUTORIZAR | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| aportes | POST | `/pagos/webhooks/{proveedorCodigo}` | PUBLICO | n/a | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| aportes | POST | `/pagos/{pagoId}/disputas` | SOPORTE | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| aportes | POST | `/pagos/{pagoId}/reembolsos` | BILLETERA_OPERAR | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| auditoria | POST | `/auditoria/derechos` | DATOS_SENSIBLES_LEER | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| auditoria | GET | `/indicadores` | AUDITORIA_LEER | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| auditoria | POST | `/reportes/exportaciones/{exportacionId}/descargas` | AUDITORIA_LEER | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| auditoria | POST | `/reportes/{definicionId}/ejecuciones` | AUDITORIA_LEER | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | GET | `/cumplimiento/contratos/vigentes` | SIN_ANOTACION | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/contratos/{contratoId}/aceptaciones` | PARTICIPANTE | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/riesgos/eventos` | RESPONSABLE_RIESGOS | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/seguridad/incidentes` | RESPONSABLE_SEGURIDAD | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/seguridad/incidentes/{incidenteId}/notificaciones` | RESPONSABLE_SEGURIDAD | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/seguridad/incidentes/{incidenteId}/reportes` | RESPONSABLE_SEGURIDAD | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/usuarios/{usuarioId}/diligencia` | ANALISTA_CUMPLIMIENTO | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | POST | `/cumplimiento/usuarios/{usuarioId}/pep` | PARTICIPANTE | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| cumplimiento | GET | `/licencia/alcance` | SOPORTE | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| entregas | POST | `/cuentas-bancarias` | BILLETERA_OPERAR | n/a | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| entregas | GET | `/cuentas-bancarias/{cuentaId}/disponibilidad` | BILLETERA_OPERAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| entregas | POST | `/cuentas-bancarias/{cuentaId}/principal` | BILLETERA_OPERAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| entregas | POST | `/cuentas-bancarias/{cuentaId}/verificacion` | BILLETERA_OPERAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| entregas | POST | `/desembolsos` | ENTREGA_EJECUTAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| entregas | POST | `/desembolsos/{ordenId}/respuesta` | ENTREGA_EJECUTAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| entregas | POST | `/entregas` | ENTREGA_EJECUTAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| entregas | POST | `/entregas/{entregaId}/autorizacion` | ENTREGA_AUTORIZAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| entregas | POST | `/entregas/{entregaId}/ejecucion` | ENTREGA_EJECUTAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/activos/{activoId}/depreciaciones` | CONTABILIDAD_ERP_ACTIVOS_FIJOS | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/cuentas-por-cobrar` | CONTABILIDAD_ERP_COBRAR | n/a | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| erp | POST | `/erp/cuentas-por-cobrar/{cuentaId}/cobros` | CONTABILIDAD_ERP_COBRAR | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| erp | POST | `/erp/ejercicios` | CONTABILIDAD_ERP_CERRAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/facturas-de-proveedor` | CONTABILIDAD_ERP_CUENTAS_POR_PAGAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/facturas-de-proveedor/{facturaId}/pagos` | CONTABILIDAD_ERP_PAGAR | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| erp | POST | `/erp/ordenes-de-compra` | CONTABILIDAD_ERP_COMPRAS | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/ordenes-de-compra/{ordenId}/aprobacion` | CONTABILIDAD_ERP_COMPRAS | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| erp | POST | `/erp/periodos/{periodoId}/cierre` | CONTABILIDAD_ERP_CERRAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/periodos/{periodoId}/depreciaciones` | CONTABILIDAD_ERP_ACTIVOS_FIJOS | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/periodos/{periodoId}/estados-financieros` | CONTABILIDAD_ERP_REPORTES | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | GET | `/erp/plantillas-de-asiento/{plantillaId}/validacion` | CONTABILIDAD_ERP_REPORTES | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/presupuestos` | CONTABILIDAD_ERP_PRESUPUESTO | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| erp | POST | `/erp/presupuestos/{presupuestoId}/aprobacion` | CONTABILIDAD_ERP_PRESUPUESTO | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| erp | POST | `/erp/terceros` | CONTABILIDAD_ERP_COMPRAS | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | GET | `/cobranza/restricciones/vigentes/{usuarioId}` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/cobranza/restricciones/{restriccionId}/levantamiento` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/garantia/disoluciones/{disolucionId}/cierre` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| garantia | POST | `/garantia/fondos/{grupoId}/devolucion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| garantia | POST | `/garantia/grupos/{grupoId}/disolucion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| garantia | POST | `/garantia/reemplazos/{reemplazoId}/aprobacion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| garantia | POST | `/garantia/reemplazos/{reemplazoId}/ejecucion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| garantia | POST | `/incumplimientos` | GRUPO_ADMINISTRAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/incumplimientos/{expedienteId}/aval/ejecucion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| garantia | POST | `/incumplimientos/{expedienteId}/cobertura` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/incumplimientos/{expedienteId}/descargo` | PARTICIPANTE | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/incumplimientos/{expedienteId}/descargo/resolucion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/incumplimientos/{expedienteId}/reemplazo` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| garantia | POST | `/incumplimientos/{expedienteId}/restriccion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/acuerdos` | PARTICIPANTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/acuerdos/{acuerdoId}/votos` | PARTICIPANTE | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/grupos` | GRUPO_CREAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | GET | `/grupos/calendario/calcular` | PARTICIPANTE | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | GET | `/grupos/participantes/alias/{alias}` | BILLETERA_OPERAR | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | GET | `/grupos/participantes/{usuarioId}/actividad` | BILLETERA_VER | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | GET | `/grupos/sorteos/{sorteoId}/paquete` | PARTICIPANTE | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/grupos/{grupoId}/cupos/{cupoId}/traspasos` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/grupos/{grupoId}/invitaciones` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/grupos/{grupoId}/postulaciones` | PARTICIPANTE | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/grupos/{grupoId}/retiros` | PARTICIPANTE | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | si (retiro) | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| grupos | POST | `/grupos/{grupoId}/sorteo` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/grupos/{grupoId}/sorteo/revelacion` | GRUPO_ADMINISTRAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| grupos | POST | `/turnos/permutas` | PARTICIPANTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | GET | `/identidad/verificaciones` | VERIFICACION_RESOLVER | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | POST | `/identidad/verificaciones/{verificacionId}/decision` | VERIFICACION_RESOLVER | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | GET | `/identidad/verificaciones/{verificacionId}/fotos/{cara}` | VERIFICACION_RESOLVER | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | GET | `/identidad/verificaciones/{verificacionId}/fotos/{cara}/contenido` | VERIFICACION_RESOLVER | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | POST | `/sesiones` | PUBLICO | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | POST | `/usuarios` | PUBLICO | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | GET | `/usuarios/por-telefono` | GRUPO_ADMINISTRAR | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | POST | `/usuarios/tokens/invitacion` | GRUPO_ADMINISTRAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| identidad | POST | `/usuarios/{usuarioId}/documentos` | PUBLICO | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| identidad | POST | `/usuarios/{usuarioId}/titularidad/verificacion` | DATOS_SENSIBLES_LEER | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| notificaciones | POST | `/notificaciones` | SOPORTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| notificaciones | GET | `/notificaciones/supresion` | SOPORTE | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| notificaciones | POST | `/notificaciones/webhooks/{proveedor}` | PUBLICO | n/a | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| nucleo-financiero | POST | `/billetera/bloqueos` | DATOS_SENSIBLES_LEER | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| nucleo-financiero | POST | `/billetera/cierres` | BILLETERA_OPERAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| nucleo-financiero | POST | `/billetera/recargas` | BILLETERA_OPERAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| nucleo-financiero | POST | `/billetera/recargas/{ordenId}/acreditacion` | BILLETERA_OPERAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| nucleo-financiero | POST | `/billetera/retenciones` | BILLETERA_OPERAR | n/a | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| nucleo-financiero | POST | `/billetera/retenciones/{retencionId}/cierre` | BILLETERA_OPERAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| nucleo-financiero | POST | `/billetera/retiros` | BILLETERA_OPERAR | n/a | si | borde (gateway, H7.S4 — Pablo) | si (retiro) | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| nucleo-financiero | POST | `/billetera/reversos` | REVERSO_AUTORIZAR | n/a | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| nucleo-financiero | POST | `/billetera/transferencias` | BILLETERA_OPERAR | n/a | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| nucleo-financiero | GET | `/billetera/{cuentaId}/extracto` | BILLETERA_VER | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| nucleo-financiero | GET | `/billetera/{cuentaId}/saldo` | BILLETERA_VER | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| organizador | POST | `/automatizacion/reglas` | ADMIN_PLATAFORMA | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/automatizacion/reglas/{reglaId}/activacion` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/automatizacion/tareas` | ORGANIZADOR | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/automatizacion/tareas/{tareaId}/aprobacion` | ORGANIZADOR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| organizador | POST | `/automatizacion/tareas/{tareaId}/ejecuciones` | ORGANIZADOR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/contratos/{contratoId}/firma` | ORGANIZADOR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/contratos/{contratoId}/rescision` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/postulaciones` | PARTICIPANTE | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/postulaciones/{solicitudId}/aprobacion` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| organizador | POST | `/organizadores/sanciones/{sancionId}/apelacion` | ORGANIZADOR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| organizador | POST | `/organizadores/sanciones/{sancionId}/resolucion` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| organizador | POST | `/organizadores/{organizadorId}/contratos` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/{organizadorId}/evaluaciones` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | GET | `/organizadores/{organizadorId}/habilitacion` | GRUPO_CREAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/{organizadorId}/habilitacion` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| organizador | POST | `/organizadores/{organizadorId}/sanciones` | ADMIN_PLATAFORMA | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| publicidad | POST | `/publicidad/anunciantes` | PUBLICIDAD_ANUNCIANTES | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/anuncios` | PUBLICIDAD_CAMPANA_GESTIONAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/campanas` | PUBLICIDAD_CAMPANA_GESTIONAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/campanas/{campanaId}/aprobacion` | PUBLICIDAD_APROBAR_CAMPANA | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| publicidad | POST | `/publicidad/campanas/{campanaId}/rechazo` | PUBLICIDAD_APROBAR_CAMPANA | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| publicidad | POST | `/publicidad/conversiones` | PUBLICIDAD_CAMPANA_GESTIONAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | GET | `/publicidad/cuentas/{cuentaId}/consumo` | PUBLICIDAD_ANUNCIANTES | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| publicidad | POST | `/publicidad/cuentas/{cuentaId}/liquidaciones` | PUBLICIDAD_LIQUIDAR | revisar (id en la ruta) | si | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| publicidad | POST | `/publicidad/espacios/{espacioId}/entrega` | PUBLICIDAD_CAMPANA_GESTIONAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/impresiones/{impresionId}/clic` | PUBLICIDAD_CAMPANA_GESTIONAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/piezas-creativas` | PUBLICIDAD_ANUNCIANTES | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/piezas-creativas/{piezaId}/revision` | PUBLICIDAD_MODERAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/socios-comerciales` | PUBLICIDAD_ANUNCIANTES | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| publicidad | POST | `/publicidad/socios-comerciales/{socioId}/verificacion` | PUBLICIDAD_ANUNCIANTES | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/comisiones/cotizaciones` | BILLETERA_OPERAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/comisiones/cotizaciones/{cotizacionId}/aceptacion` | BILLETERA_OPERAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/comisiones/devengos` | BILLETERA_OPERAR | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/comisiones/devengos/{devengoId}/cobros` | BILLETERA_OPERAR | revisar (id en la ruta) | no | borde (gateway, H7.S4 — Pablo) | no aplica | critica (ver security-matrix.md) | ver *WebTest del servicio | si |
| tarifas | POST | `/comisiones/{devengoId}/devoluciones` | REVERSO_AUTORIZAR | revisar (id en la ruta) | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/facturas` | CONTABILIDAD | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/tarifas/liquidaciones/{periodo}/cierre` | CONTABILIDAD | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/tarifas/precios/resolver` | BILLETERA_VER | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/tarifas/segmentos` | CATALOGO_EDITAR | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/tarifas/tarifarios` | TARIFARIO_PUBLICAR | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | POST | `/tarifas/tarifarios/{tarifarioId}/vigencia` | TARIFARIO_PUBLICAR | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| tarifas | GET | `/tarifas/vigentes/{codigo}` | BILLETERA_VER | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | GET | `/publico/grupos/{grupoId}/bloques` | PUBLICO | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | GET | `/publico/grupos/{grupoId}/verificacion` | PUBLICO | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | GET | `/publico/sorteos/{sorteoId}/verificacion` | PUBLICO | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/bloques` | AUDITORIA_LEER | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/certificados` | PARTICIPANTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/certificados/{codigo}/revocacion` | PARTICIPANTE | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/eventos` | SOPORTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/eventos/{eventoId}/compensacion` | SOPORTE | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/insignias/evaluacion` | SOPORTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/insignias/{otorgadaId}/revocacion` | SOPORTE | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/puntajes` | SOPORTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/resenas` | PARTICIPANTE | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/resenas/{resenaId}/moderacion` | MODERADOR_CONTENIDO | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/riesgo/alertas/{alertaId}/cierre` | ANALISTA_CUMPLIMIENTO | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/riesgo/evaluacion` | ANALISTA_CUMPLIMIENTO | n/a | si | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | POST | `/reputacion/snapshots` | SOPORTE | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | GET | `/reputacion/{usuarioId}/puntaje` | PARTICIPANTE | revisar (id en la ruta) | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
| transparencia | GET | `/verificar/{codigo}` | PUBLICO | n/a | no | n/a | no aplica | no critica | ver *WebTest del servicio | no |
