# Matriz de trazabilidad — backoffice (carril A · A0)

> GENERADA por `matriz.py` desde el flujo de pantallas, los CU, los OpenAPI y la semilla de permisos.
> No se edita a mano: `python3 .../entregables/matriz.py` la reescribe y `--verificar` falla si quedó vieja.

Estados: **existe** = hay operaciones del CU y al menos un `GET` · **sin lectura atribuida** = hay
operaciones del CU pero ningún `GET` atribuido a él (puede existir un `GET` sin CU: ver la última
sección) · **ausente** = ninguna operación atribuida al CU ·
**sin contrato** = el CU no declara servicio. Permiso **no catalogado** = la columna «Exige» del CU
nombra un código que la semilla no tiene (S-8: lo decide Seguridad, no se inventa).
Una operación se atribuye a un CU por su comentario `# CU-NN` en el YAML o por el
`Traza.marcarCasoDeUso("CU-NN")` del método que la implementa. **SIN IMPLEMENTAR** = el contrato
la publica pero ningún controlador la sobrescribe (responde el ejemplo del generador).

| Pantalla | Fuente | CU | Servicio | Operaciones (verbo ruta · operationId) | Exige (CU) | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| `acceso/ingreso` | Flujo de pantallas | — | — | — | — | **sin CU en el flujo** |
| `acceso/desafio` | Flujo de pantallas | — | — | — | — | **sin CU en el flujo** |
| `acceso/enrolamiento` | Flujo de pantallas | CU-08 | identidad | — | `ACCESOS_ADMINISTRAR` | **ausente** |
| `acceso/recuperacion` | Flujo de pantallas | CU-09 | identidad | — | — | **ausente** |
| `acceso/aprobaciones` | Flujo de pantallas | — | — | — | — | **sin CU en el flujo** |
| `acceso/reinscripcion-factor` | Flujo de pantallas | CU-09 | identidad | — | — | **ausente** |
| `operacion/conciliacion` | Flujo de pantallas | CU-50 | nucleo-financiero | — | — | **ausente** |
| `operacion/cierre-diario` | Flujo de pantallas | CU-51 | aportes | — | `CONTABILIDAD` (no catalogado) | **ausente** |
| `operacion/desembolsos` | Flujo de pantallas | CU-28 | entregas | `POST /desembolsos/{ordenId}/respuesta` · anotarRespuestaDesembolso<br>`POST /desembolsos` · emitirOrdenDesembolso | `TESORERIA_OPERAR` (no catalogado) | **sin lectura atribuida** |
| `operacion/desembolsos` | Flujo de pantallas | CU-14 | nucleo-financiero | `POST /billetera/reversos` · reversarTransaccion | `REVERSO_AUTORIZAR` | **sin lectura atribuida** |
| `operacion/reclamos` | Flujo de pantallas | CU-52 | cumplimiento | `GET /reclamos/usuarios/{usuarioId}/efecto` · consultarEfectoDeReclamos<br>`GET /reclamos/{reclamoId}` · consultarReclamo<br>`GET /reclamos` · listarBandejaDeReclamos<br>`POST /reclamos/{reclamoId}/respuesta` · responderReclamo | `RECLAMO_ATENDER`, `RECLAMO_VER` | **existe** |
| `operacion/reclamos` | Flujo de pantallas | CU-53 | cumplimiento | `GET /reclamos/usuarios/{usuarioId}/efecto` · consultarEfectoDeReclamos | `RECLAMO_ATENDER` | **existe** |
| `operacion/politicas-resolucion` | Flujo de pantallas | — | — | — | — | **sin CU en el flujo** |
| `operacion/roles` | Flujo de pantallas | CU-08 | identidad | — | `ACCESOS_ADMINISTRAR` | **ausente** |
| `operacion/tarifario` | Flujo de pantallas | CU-34 | tarifas | `GET /tarifas/vigentes/{codigo}` · consultarTarifarioVigente<br>`POST /tarifas/tarifarios/{tarifarioId}/vigencia` · ponerTarifarioVigente<br>`POST /tarifas/tarifarios` · publicarTarifario | `TARIFARIO_PUBLICAR` | **existe** |
| `operacion/reportes` | Flujo de pantallas | CU-58 | auditoria | `POST /reportes/exportaciones/{exportacionId}/descargas` · descargarExportacion<br>`POST /reportes/{definicionId}/ejecuciones` · ejecutarReporte | — | **sin lectura atribuida** |
| `operacion/fondeo` | Flujo de pantallas | CU-99 | aportes | `POST /pagos/proveedores` · darDeAltaProveedor<br>`POST /pagos/enrutamiento` · enrutarCobro | `BILLETERA_OPERAR`, `PROVEEDORES_ADMINISTRAR` (no catalogado) | **sin lectura atribuida** |
| `operacion/mensajeria` | Flujo de pantallas | CU-83 | notificaciones | — | — | **ausente** |
| `operacion/mensajeria` | Flujo de pantallas | CU-80 | notificaciones | `POST /notificaciones` · emitirNotificacion<br>`POST /notificaciones/supresion/consultas` · verificarSupresion | — | **sin lectura atribuida** |
| `cumplimiento/verificaciones` | Flujo de pantallas | CU-01 | identidad | `POST /usuarios/verificaciones/correo/{verificacionId}/confirmacion` · confirmarVerificacionCorreo<br>`POST /usuarios` · registrarUsuario<br>`POST /usuarios/verificaciones/correo` · solicitarVerificacionCorreo<br>`POST /usuarios/{usuarioId}/titularidad/verificacion` · verificarTitularidad | — | **sin lectura atribuida** |
| `cumplimiento/verificaciones` | Flujo de pantallas | CU-02 | cumplimiento | `POST /cumplimiento/usuarios/{usuarioId}/diligencia` · elevarDiligencia | `ANALISTA_CUMPLIMIENTO` (no catalogado), `PARTICIPANTE` (no catalogado) | **sin lectura atribuida** |
| `cumplimiento/alertas` | Flujo de pantallas | CU-44 | cumplimiento | `POST /cumplimiento/casos` · abrirCasoLft<br>`POST /cumplimiento/casos/{casoId}/decision` · decidirCasoLft<br>`GET /cumplimiento/alertas` · listarBandejaDeAlertas<br>`GET /cumplimiento/casos` · listarBandejaDeCasos | `CUMPLIMIENTO_ALERTAS`, `CUMPLIMIENTO_CASOS`, `OFICIAL_CUMPLIMIENTO` (no catalogado) | **existe** |
| `cumplimiento/casos` | Flujo de pantallas | CU-41 | cumplimiento | — | `PARTICIPANTE` (no catalogado) | **ausente** |
| `cumplimiento/casos` | Flujo de pantallas | CU-42 | cumplimiento | — | — | **ausente** |
| `cumplimiento/uif` | Flujo de pantallas | CU-43 | cumplimiento | — | `CUMPLIMIENTO_REPORTAR` | **ausente** |
| `cumplimiento/uif` | Flujo de pantallas | CU-45 | cumplimiento | — | `OFICIAL_CUMPLIMIENTO` (no catalogado) | **ausente** |
| `cumplimiento/riesgo` | Flujo de pantallas | CU-54 | cumplimiento | `POST /cumplimiento/riesgos/eventos` · registrarRiesgoOperativo | `RESPONSABLE_RIESGOS` (no catalogado) | **sin lectura atribuida** |
| `cumplimiento/riesgo` | Flujo de pantallas | CU-55 | cumplimiento | `POST /cumplimiento/seguridad/incidentes/{incidenteId}/notificaciones` · notificarTitulares<br>`POST /cumplimiento/seguridad/incidentes` · registrarIncidente<br>`POST /cumplimiento/seguridad/incidentes/{incidenteId}/reportes` · reportarIncidente | `RESPONSABLE_SEGURIDAD` (no catalogado) | **sin lectura atribuida** |
| `cumplimiento/riesgo` | Flujo de pantallas | CU-56 | cumplimiento | — | `RESPONSABLE_RIESGOS` (no catalogado) | **ausente** |
| `cumplimiento/gobierno` | Flujo de pantallas | CU-94 | cumplimiento | — | `GOBIERNO_SESIONAR` (no catalogado) | **ausente** |
| `contabilidad/periodo` | Flujo de pantallas | CU-100 | erp | `POST /erp/ejercicios` · abrirEjercicioFiscal<br>`POST /erp/periodos/{periodoId}/cierre` · cerrarPeriodoContable<br>`GET /erp/periodos/vigente` · consultarPeriodoVigente<br>`GET /erp/ejercicios` · listarEjerciciosFiscales | `CONTABILIDAD_ERP_CERRAR`, `CONTABILIDAD_ERP_REPORTES` | **existe** |
| `contabilidad/presupuesto` | Flujo de pantallas | CU-101 | erp | `POST /erp/presupuestos/{presupuestoId}/aprobacion` · aprobarPresupuesto<br>`POST /erp/presupuestos` · crearPresupuesto | `CONTABILIDAD_ERP_PRESUPUESTO` | **sin lectura atribuida** |
| `contabilidad/compras` | Flujo de pantallas | CU-102 | erp | `POST /erp/ordenes-de-compra/{ordenId}/aprobacion` · aprobarOrdenDeCompra<br>`POST /erp/ordenes-de-compra` · crearOrdenDeCompra<br>`POST /erp/terceros` · darDeAltaTercero | `CONTABILIDAD_ERP_COMPRAS` | **sin lectura atribuida** |
| `contabilidad/compras` | Flujo de pantallas | CU-103 | erp | `POST /erp/facturas-de-proveedor/{facturaId}/pagos` · pagarFacturaDeProveedor<br>`POST /erp/facturas-de-proveedor` · registrarFacturaDeProveedor | `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR`, `CONTABILIDAD_ERP_PAGAR` | **sin lectura atribuida** |
| `contabilidad/cobros` | Flujo de pantallas | CU-104 | erp | `POST /erp/cuentas-por-cobrar` · abrirCuentaPorCobrar<br>`POST /erp/cuentas-por-cobrar/{cuentaId}/cobros` · registrarCobro | `CONTABILIDAD_ERP_COBRAR` | **sin lectura atribuida** |
| `contabilidad/activos` | Flujo de pantallas | CU-105 | erp | `POST /erp/periodos/{periodoId}/depreciaciones` · correrDepreciacionDelPeriodo<br>`POST /erp/activos/{activoId}/depreciaciones` · depreciarActivo | `CONTABILIDAD_ERP_ACTIVOS_FIJOS` | **sin lectura atribuida** |
| `contabilidad/estados` | Flujo de pantallas | CU-106 | erp | `POST /erp/periodos/{periodoId}/estados-financieros` · generarEstadoFinanciero<br>`GET /erp/plantillas-de-asiento/{plantillaId}/validacion` · validarPlantillaDeAsiento | `CONTABILIDAD_ERP_CERRAR`, `CONTABILIDAD_ERP_REPORTES` | **existe** |
| `publicidad/partners` | Flujo de pantallas | — | — | — | — | **sin CU en el flujo** |
| `publicidad/anunciantes` | Flujo de pantallas | CU-110 | publicidad | `POST /publicidad/anunciantes` · darDeAltaAnunciante<br>`POST /publicidad/socios-comerciales` · postularSocioComercial<br>`POST /publicidad/socios-comerciales/{socioId}/verificacion` · verificarSocioComercial | `PUBLICIDAD_ANUNCIANTES` | **sin lectura atribuida** |
| `publicidad/campanas` | Flujo de pantallas | CU-111 | publicidad | `POST /publicidad/campanas/{campanaId}/aprobacion` · aprobarCampana<br>`POST /publicidad/campanas` · crearCampana<br>`POST /publicidad/campanas/{campanaId}/rechazo` · rechazarCampana | `PUBLICIDAD_APROBAR_CAMPANA` | **sin lectura atribuida** |
| `publicidad/moderacion` | Flujo de pantallas | CU-112 | publicidad | `POST /publicidad/piezas-creativas/{piezaId}/revision` · moderarPiezaCreativa<br>`POST /publicidad/piezas-creativas` · subirPiezaCreativa | `PUBLICIDAD_MODERAR` | **sin lectura atribuida** |
| `publicidad/liquidacion` | Flujo de pantallas | CU-114 | publicidad | `GET /publicidad/cuentas/{cuentaId}/consumo` · consumoDelPeriodo<br>`POST /publicidad/cuentas/{cuentaId}/liquidaciones` · liquidarPeriodo | — | **existe** |
| `operacion/retiros` | ruta real en apps/backoffice/src/app/rutas/operacion/operacion.routes.ts; ADR-049 | CU-11 | nucleo-financiero | `POST /billetera/retiros/{ordenId}/aprobacion` · aprobarRetiro<br>`GET /billetera/retiros` · listarRetirosEnRevision<br>`POST /billetera/retiros` · solicitarRetiro | `BILLETERA_OPERAR`, `RETIRO_APROBAR` | **existe** |

## Resumen

- ausente: 13
- existe: 8
- sin CU en el flujo: 5
- sin lectura atribuida: 17

## Lecturas (`GET`) que no están atribuidas a ningún CU

Existen en el contrato pero ni su comentario ni su controlador dicen a qué CU sirven; pueden
alimentar una pantalla de arriba. Atribuirlas es tarea del dueño de cada contrato, no de esta matriz.

| Servicio | Operación |
| --- | --- |
| entregas | `GET /entregas/mercado/ofertas/{ofertaId}` · consultarOfertaParaPago |
| entregas | `GET /entregas/mercado/ofertas` · listarOfertas |
| identidad | `GET /identidad/verificaciones` · listarVerificaciones |
| identidad | `GET /identidad/verificaciones/{verificacionId}/fotos/{cara}/contenido` · verContenidoDeFoto |
| identidad | `GET /identidad/verificaciones/{verificacionId}/fotos/{cara}` · verFotoDelExpediente |
| notificaciones | `GET /notificaciones/supresion` · consultarSupresion |
| nucleo-financiero | `GET /billetera/cuentas/propia` · consultarCuentaPropia |
