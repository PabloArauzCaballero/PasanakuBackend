# Carril K · 19 rutas del frontend sin contrato (scripts/verificar_rutas_frontend.py, RC=0, conteo sin cambios: 19)

Verificado por codigo: erp.yaml solo tiene un `get` (validarPlantillaDeAsiento) y publicidad.yaml uno (consumoDelPeriodo);
ErpController y PublicidadController no implementan ningun listado, y los repositorios de ambos servicios no tienen consultas
de listado. Declarar el contrato sin el endpoint rompe la compilacion (el generador crea metodos abstractos que alguien
debe implementar) y publicaria una ruta falsa. Por eso NO se bajo el conteo: no hay endpoint existente que declarar.

| Ruta | Dueno que debe construirla | Falta (ademas del codigo) |
|---|---|---|
| GET /erp/activos | erp | decision de producto: columnas y filtros del listado, permiso (CONTABILIDAD_ERP_ACTIVOS_FIJOS?) |
| GET /erp/cuentas-por-cobrar | erp | idem, permiso CONTABILIDAD_ERP_COBRAR |
| GET /erp/facturas-de-proveedor | erp | idem; ademas la factura es append-only: estado/monto_pagado se derivan de pagos |
| GET /erp/ordenes-de-compra | erp | idem, permiso CONTABILIDAD_ERP_COMPRAS |
| GET /erp/partidas-presupuestarias | erp | idem, permiso CONTABILIDAD_ERP_PRESUPUESTO |
| GET /erp/ejercicios/{x}/periodos | erp | idem, permiso CONTABILIDAD_ERP_CERRAR |
| GET /erp/estados-financieros/{x}/documento | erp | formato del documento (PDF/XLSX) = decision de Contabilidad |
| GET /publicidad/socios-comerciales/{x} | publicidad | que campos del socio ve el backoffice (datos personales: regla 90.2.8) |
| GET /publicidad/anunciantes | publicidad | listado paginado; permiso |
| GET /publicidad/campanas | publicidad | listado paginado; permiso |
| GET /publicidad/piezas-creativas | publicidad | cola de moderacion: criterio de orden/estado = Moderacion |
| GET /reclamos | cumplimiento (NO tocado) | bandeja de CU-52 |
| GET /grupos/solicitudes-escaladas | grupos (carril I, NO tocado) | CU-68 |
| GET /identidad/tutoriales/progreso, PUT/DELETE .../{x} (3) | identidad (carril I, NO tocado) | ADR-045 |
| POST /sesion/refrescar | identidad (carril I, NO tocado) | refresco por cookie |
| POST /extraccion/accesos, /extraccion/exportaciones (2) | no existe servicio `extraccion` | decision de arquitectura: quien lo construye |
