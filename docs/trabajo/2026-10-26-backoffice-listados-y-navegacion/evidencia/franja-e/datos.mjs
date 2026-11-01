// Datos SINTÉTICOS (doble declarado, regla 65) con la forma de erp.yaml / tarifas.yaml / notificaciones.yaml / auditoria.yaml.
const id = (i) => `00000000-0000-4000-8000-${String(i).padStart(12, '0')}`
const bob = (m) => ({ monto: m, moneda: 'BOB' })
const LARGO = 'Proveedor de Servicios Integrales de Mantenimiento Preventivo y Correctivo del Altiplano Sociedad de Responsabilidad Limitada'

export const RECURSOS = {
  partidas: {
    ruta: '/api/v1/erp/partidas-presupuestarias',
    fila: (i) => ({ partidaId: id(i), presupuestoId: id(900), presupuestoNombre: i === 3 ? 'Operación Ñandú' : `Presupuesto ${i}`, estado: ['BORRADOR', 'APROBADO', 'CERRADO'][i % 3], centroCostoId: id(800 + (i % 2)), centroCostoNombre: i % 2 ? 'Marketing' : 'Tecnología', cuentaContableId: id(700), periodoContableId: id(600), anio: 2026, mes: (i % 12) + 1, fechaInicioPeriodo: '2026-03-01', montoPresupuestado: bob('1000.00'), montoEjecutado: bob(i % 4 === 0 ? '1250.75' : '640.10') }),
  },
  ordenes: {
    ruta: '/api/v1/erp/ordenes-de-compra',
    fila: (i) => ({ ordenCompraId: id(i), numero: `OC-${String(i).padStart(4, '0')}`, terceroComercialId: id(500), terceroRazonSocial: i === 2 ? LARGO : i % 2 ? 'Proveedor Andino SRL' : 'Insumos del Sur SA', centroCostoId: null, centroCostoNombre: i % 3 ? 'Tecnología' : null, estado: ['BORRADOR', 'APROBADA', 'RECIBIDA_PARCIAL', 'RECIBIDA_TOTAL', 'CANCELADA'][i % 5], monto: bob(`${800 + i}.00`), creadaEn: '2026-09-01T12:00:00Z' }),
  },
  facturas: {
    ruta: '/api/v1/erp/facturas-de-proveedor',
    fila: (i) => ({ facturaProveedorId: id(i), numeroFactura: `F-${String(i).padStart(4, '0')}`, terceroComercialId: id(500), terceroRazonSocial: i % 2 ? 'Proveedor Andino SRL' : 'Insumos del Sur SA', ordenCompraId: id(1), fechaEmision: '2026-09-02', fechaVencimiento: i % 4 === 0 ? '2026-01-10' : '2099-10-02', estado: ['APROBADA', 'PAGADA_PARCIAL', 'PAGADA', 'REGISTRADA', 'ANULADA'][i % 5], monto: bob('800.00'), montoPagado: bob('0.00'), saldoPendiente: bob(i % 5 === 2 ? '0.00' : '800.00'), aprobadaPorLaSesion: i % 7 === 0, asientoContableId: null }),
  },
  cuentas: {
    ruta: '/api/v1/erp/cuentas-por-cobrar',
    fila: (i) => ({ cuentaPorCobrarId: id(i), origenTipo: ['FACTURA_PUBLICIDAD', 'VENTA_SERVICIO', 'OTRO'][i % 3], origenId: id(400), terceroComercialId: i % 6 ? id(300) : null, terceroRazonSocial: i % 6 ? (i === 4 ? 'Cooperativa Ñuñoa' : `Anunciante ${i}`) : null, fechaVencimiento: '2026-11-01', estado: ['PENDIENTE', 'COBRADA_PARCIAL', 'COBRADA', 'INCOBRABLE'][i % 4], monto: bob('300.00'), cobrado: bob('0.00'), saldoPendiente: bob(i % 4 === 2 ? '0.00' : '300.00') }),
  },
  activos: {
    ruta: '/api/v1/erp/activos',
    fila: (i) => ({ activoFijoId: id(i), codigo: `AF-${String(i).padStart(3, '0')}`, descripcion: i === 5 ? 'Servidor de oficina con almacenamiento redundante y fuente doble para la sala de cómputo' : `Bien de uso ${i}`, categoriaNombre: i % 2 ? 'Vehículos' : 'Equipos de computación', vidaUtilMeses: 36, fechaAdquisicion: '2026-01-15', estado: i % 6 === 0 ? 'DADO_DE_BAJA' : 'ACTIVO', costoAdquisicion: bob('3600.00'), valorResidual: bob('0.00'), depreciacionAcumulada: bob('900.00'), valorEnLibros: bob('2700.00'), depreciadoEnElPeriodoVigente: i % 3 === 0 }),
  },
  tarifarios: {
    ruta: '/api/v1/tarifas/tarifarios',
    fila: (i) => ({ tarifarioId: id(i), codigo: `TAR-${i}`, version: i, nombre: i === 1 ? 'Tarifa general' : `Tarifa ${i}`, estado: ['VIGENTE', 'EN_PREAVISO', 'BORRADOR', 'SUSTITUIDO', 'ARCHIVADO'][i % 5], monedaBase: i % 2 ? 'USD' : 'BOB', vigenteDesde: '2026-11-01T04:00:00Z', diasPreaviso: 30 }),
  },
  plantillas: {
    ruta: '/api/v1/notificaciones/plantillas',
    campoEstado: 'estadoAprobacion',
    fila: (i) => ({ plantillaId: id(i), codigo: `PLANTILLA_${i}`, evento: `EVENTO_${i}`, canal: i % 2 ? 'WHATSAPP' : 'SMS', categoriaProveedor: 'UTILITY', estadoAprobacion: ['APROBADA', 'BORRADOR', 'RECHAZADA'][i % 3], activa: i % 2 === 0, versionId: id(i), version: 1, idioma: 'es', vigenteDesde: '2026-09-01T04:00:00Z' }),
  },
  envios: {
    ruta: '/api/v1/notificaciones/envios',
    fila: (i) => ({ envioId: id(i), notificacionId: id(200), proveedorId: id(100), proveedor: i % 2 ? 'PROVEEDOR_A' : 'PROVEEDOR_B', versionPlantillaId: id(1), canal: i % 2 ? 'SMS' : 'WHATSAPP', estado: ['FALLIDO', 'ENTREGADO', 'EXPIRADO', 'PENDIENTE'][i % 4], orden: 1, intentos: 3, maxIntentos: 3, costo: '0.0125', moneda: 'USD', codigoError: i % 4 === 0 ? 'E-EJEMPLO' : undefined, encoladoEn: '2026-10-16T12:00:00Z' }),
  },
  definiciones: {
    ruta: '/api/v1/reportes',
    exacta: true,
    cursor: ['despuesDe', 'despuesDeId', 'proximoDespuesDe', 'proximoDespuesDeId'],
    fila: (i) => ({ definicionId: id(i), tipo: i % 2 ? 'FINANCIERO' : 'OPERATIVO', nombre: `Reporte ${i}`, descripcion: 'Sintético', permisoRequerido: 'AUDITORIA_LEER', contieneDatosSensibles: i % 3 === 0 }),
  },
  ejecuciones: {
    ruta: '/api/v1/reportes/ejecuciones',
    fila: (i) => ({ ejecucionId: id(i), definicionId: id((i % 5) + 1), solicitadoPor: 'oculto', estado: ['COMPLETADA', 'FALLIDA', 'EN_COLA'][i % 3], filasGeneradas: i, duracionMs: 15, iniciadaEn: '2026-10-16T12:00:00Z' }),
  },
}

/** Una página por cursor (índice de la última fila en `antesDeId`), filtrada por `estados` como el contrato. */
export function pagina(recurso, url, { total = 25, campoEstado } = {}) {
  const [pa, pb, ra, rb] = recurso.cursor ?? ['antesDe', 'antesDeId', 'proximoAntesDe', 'proximoAntesDeId']
  const params = url.searchParams
  const limite = Number(params.get('limite') ?? 25)
  const estados = params.getAll('estados').flatMap((e) => e.split(',')).filter(Boolean)
  let filas = Array.from({ length: total }, (_, i) => recurso.fila(i + 1))
  if (estados.length && campoEstado) filas = filas.filter((f) => estados.includes(f[campoEstado]))
  const desde = params.get(pb) ? Number(params.get(pb).slice(-12)) : 0
  const items = filas.slice(desde, desde + limite)
  const cuerpo = { items }
  if (desde + limite < filas.length) {
    cuerpo[ra] = '2026-01-01'
    cuerpo[rb] = id(desde + limite)
  }
  void pa
  return cuerpo
}
