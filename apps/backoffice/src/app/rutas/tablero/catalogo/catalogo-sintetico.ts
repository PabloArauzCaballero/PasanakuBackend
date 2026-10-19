import { ColumnaDelCatalogoOrigenNegocioEnum as Origen, ColumnaDelCatalogoSensibilidadEnum as S, type CatalogoDeDatos, type TablaDelCatalogo } from 'clientes/angular/auditoria'

/** Catálogo SINTÉTICO para las pruebas: la forma del contrato, con tablas reales del modelo y textos cortos. */
export function tabla(nombre: string, esquema: string, extra: Partial<TablaDelCatalogo> = {}): TablaDelCatalogo {
  return {
    tabla: nombre,
    esquema,
    modulo: '03',
    nombreModulo: 'Aportes',
    servicio: esquema,
    appendOnly: false,
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
  columnas: [
    { nombre: 'id', tipo: 'UUID', nulable: false, pk: true, negocio: 'Identificador estable', origenNegocio: Origen.Convencion, sistema: 'UUID, obligatoria, clave primaria.', sensibilidad: S.Ninguna },
    { nombre: 'monto_esperado', tipo: 'DECIMAL(14,2)', nulable: false, pk: false, negocio: 'Lo que se pactó', origenNegocio: Origen.Ficha, sistema: 'DECIMAL(14,2), obligatoria.', sensibilidad: S.Financiero },
    { nombre: 'grupo_id', tipo: 'UUID', nulable: false, pk: false, referencia: 'grupo', sistema: 'UUID, obligatoria, referencia a `grupo`.', sensibilidad: S.Ninguna },
  ],
  relaciones: { salientes: [{ columna: 'grupo_id', tabla: 'grupo', esquema: 'grupos' }], entrantes: [{ columna: 'obligacion_id', tabla: 'pago', esquema: 'aportes' }] },
})

export function catalogo(cantidadExtra = 0): CatalogoDeDatos {
  const extra = Array.from({ length: cantidadExtra }, (_, i) => tabla(`tabla_${String(i).padStart(2, '0')}`, 'erp'))
  return {
    esquemas: ['aportes', 'erp', 'grupos'],
    tablas: [OBLIGACION, tabla('grupo', 'grupos', { negocio: { queEs: 'Un pasanaku con sus reglas' } }), tabla('credencial_acceso', 'identidad', { servicio: undefined }), ...extra],
  }
}
