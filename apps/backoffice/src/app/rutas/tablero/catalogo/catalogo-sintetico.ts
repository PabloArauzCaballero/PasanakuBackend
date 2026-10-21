import {
  ColumnaDelCatalogoOrigenNegocioEnum as Origen,
  ColumnaDelCatalogoSensibilidadEnum as S,
  EstadoDeRevision,
  EstadoDeTabla,
  RelacionDelCatalogoAlBorrarEnum as AlBorrar,
  RelacionDelCatalogoCardinalidadEnum as Cardinalidad,
  TipoDeTabla,
  type FilaDelCatalogo,
  type PaginaDeTablasDelCatalogo,
  type ResumenDelCatalogo,
  type TablaDelCatalogo,
} from 'clientes/angular/auditoria'

/** Catálogo SINTÉTICO para las pruebas: la forma del contrato, con tablas reales del modelo y textos cortos. */
export function tabla(nombre: string, esquema: string, extra: Partial<TablaDelCatalogo> = {}): TablaDelCatalogo {
  return {
    tabla: nombre,
    esquema,
    tipo: TipoDeTabla.Negocio,
    modulo: '03',
    nombreModulo: 'Aportes',
    servicio: esquema,
    appendOnly: false,
    clasificacion: { personales: 0, financieros: 0, secretos: 0, derivados: 0 },
    negocio: { queEs: `Qué es ${nombre}` },
    reglas: [],
    columnas: [{ nombre: 'id', tipo: 'UUID', nulable: false, pk: true, sistema: 'UUID, obligatoria, clave primaria.', sensibilidad: S.Ninguna }],
    relaciones: { salientes: [], entrantes: [] },
    sistema: `Vive en el esquema \`${esquema}\`.`,
    ...extra,
  }
}

export const OBLIGACION = tabla('obligacion_aporte', 'aportes', {
  negocio: { queEs: 'Lo que un cupo debe en un período', negocio: 'Es el eje del sistema financiero', porQue: 'Sin ella no hay deuda formal' },
  reglas: ['ck_obligacion_saldo'],
  clasificacion: { personales: 0, financieros: 1, secretos: 0, derivados: 0 },
  columnas: [
    { nombre: 'id', tipo: 'UUID', nulable: false, pk: true, negocio: 'Identificador estable', origenNegocio: Origen.Convencion, sistema: 'UUID, obligatoria, clave primaria.', sensibilidad: S.Ninguna },
    { nombre: 'monto_esperado', tipo: 'DECIMAL(14,2)', nulable: false, pk: false, negocio: 'Lo que se pactó', origenNegocio: Origen.Ficha, sistema: 'DECIMAL(14,2), obligatoria.', sensibilidad: S.Financiero },
    { nombre: 'grupo_id', tipo: 'UUID', nulable: false, pk: false, referencia: 'grupo', esquemaReferencia: 'grupos', sistema: 'UUID, obligatoria, referencia a `grupo`.', sensibilidad: S.Ninguna },
  ],
  relaciones: {
    salientes: [{ columna: 'grupo_id', tabla: 'grupo', esquema: 'grupos', columnaDestino: 'id', restriccion: 'fk_obligacion_aporte_grupo_id', alBorrar: AlBorrar.Restrict, cardinalidad: Cardinalidad.N1, obligatoria: true }],
    entrantes: [{ columna: 'obligacion_id', tabla: 'pago', esquema: 'aportes', columnaDestino: 'id', restriccion: 'fk_pago_obligacion_id', alBorrar: AlBorrar.SetNull, cardinalidad: Cardinalidad._11, obligatoria: false }],
  },
})

export const CREDENCIAL = tabla('credencial_acceso', 'identidad', { servicio: undefined })

/** La fila de la lista que el servidor arma para una tabla (sin columnas). */
export function fila(t: TablaDelCatalogo): FilaDelCatalogo {
  return {
    tabla: t.tabla,
    esquema: t.esquema,
    tipo: t.tipo ?? TipoDeTabla.Negocio,
    modulo: t.modulo,
    nombreModulo: t.nombreModulo,
    ...(t.servicio ? { servicio: t.servicio } : {}),
    ...(t.negocio.queEs ? { queEs: t.negocio.queEs } : {}),
    appendOnly: t.appendOnly,
    columnas: t.columnas.length,
    columnasRevisadas: 0,
    relacionesSalientes: t.relaciones.salientes.length,
    relacionesEntrantes: t.relaciones.entrantes.length,
    clasificacion: t.clasificacion ?? { personales: 0, financieros: 0, secretos: 0, derivados: 0 },
    gobierno: t.gobierno ?? { estado: EstadoDeTabla.Activa, estadoRevision: EstadoDeRevision.Detectada, criticaParaAuditoria: false, version: 0 },
  }
}

/** Una página como la sirve `listarTablasDelCatalogo`. */
export function paginaDe(tablas: readonly TablaDelCatalogo[], pagina = 1, limite = 20, total = tablas.length): PaginaDeTablasDelCatalogo {
  return { items: tablas.map(fila), pagina, limite, total, totalPaginas: Math.ceil(total / limite) }
}

export function resumen(extra: Partial<ResumenDelCatalogo> = {}): ResumenDelCatalogo {
  return {
    tablas: 3,
    tablasDeNegocio: 3,
    tablasDeInfraestructura: 0,
    columnas: 5,
    columnasRevisadas: 0,
    relaciones: 1,
    conDatosPersonales: 1,
    conDatosFinancieros: 1,
    criticasParaAuditoria: 0,
    sinResponsable: 3,
    porEstado: { ACTIVA: 3, PLANIFICADA: 0, CANDIDATA_A_RETIRO: 0, RETIRADA: 0, DESHABILITADA: 0 },
    porRevision: { DETECTADA: 3, NECESITA_REVISION: 0, APROBADA: 0, RECHAZADA: 0 },
    esquemas: [
      { nombre: 'aportes', servicio: 'aportes', descripcion: 'Aportes, Pagos QR y Conciliación.', tablas: 1, tablasDeNegocio: 1, columnas: 3, relaciones: 1, appendOnly: 0, conDatosPersonales: 0 },
      { nombre: 'catalogo', descripcion: 'Catálogos compartidos.', tablas: 1, tablasDeNegocio: 1, columnas: 1, relaciones: 0, appendOnly: 0, conDatosPersonales: 0 },
      { nombre: 'grupos', servicio: 'grupos', descripcion: 'Grupos, Cupos y Turnos.', tablas: 1, tablasDeNegocio: 1, columnas: 1, relaciones: 0, appendOnly: 0, conDatosPersonales: 0 },
    ],
    modulos: [{ codigo: '03', nombre: 'Aportes', tablas: 3 }],
    politicasDeRetencion: [],
    ...extra,
  }
}
