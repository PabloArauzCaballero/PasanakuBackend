import type { Anunciante } from './dominio/cu110-anunciantes'
import type { Campana } from './dominio/cu111-campanas'
import type { PiezaCreativa } from './dominio/cu112-moderacion'

/** Filas SINTÉTICAS con la forma exacta de `publicidad.yaml` (`AnuncianteEnLista`, `CampanaEnLista`, `PiezaEnLista`), para las pruebas. */
export const anunciante = (p: Partial<Anunciante> = {}): Anunciante => ({
  anuncianteId: 'a-1',
  tipo: 'SOCIO_COMERCIAL',
  razonSocialFacturacion: 'Anunciante SA',
  estado: 'ACTIVO',
  creadoEn: '2026-09-01T12:00:00Z',
  cuentaPublicitariaId: 'cp-1',
  estadoCuenta: 'ACTIVA',
  limiteGastoMensual: null,
  ...p,
})

export const campana = (p: Partial<Campana> = {}): Campana => ({
  campanaId: 'c-1',
  cuentaPublicitariaId: 'cp-1',
  anuncianteId: 'a-1',
  nombre: 'Lanzamiento',
  objetivo: 'VISIBILIDAD_MARCA',
  presupuestoTotal: { monto: '500.00', moneda: 'BOB' },
  presupuestoConsumido: { monto: '120.50', moneda: 'BOB' },
  fechaInicio: '2026-09-01T04:00:00Z',
  fechaFin: null,
  estado: 'EN_REVISION',
  aprobadaPor: null,
  ...p,
})

export const pieza = (p: Partial<PiezaCreativa> = {}): PiezaCreativa => ({
  piezaCreativaId: 'p-1',
  anuncianteId: 'a-1',
  titulo: 'Banner de aniversario',
  texto: null,
  urlRecurso: 'https://cdn.aportaya.test/p-1.png',
  tipoRecurso: 'IMAGEN',
  estadoModeracion: 'PENDIENTE',
  creadaEn: '2026-09-02T12:00:00Z',
  ...p,
})

/** Una página del contrato, sin próxima por omisión. */
export const pagina = <T>(items: T[], proximoFecha: string | null = null, proximoId: string | null = null) => ({ items, proximoFecha, proximoId })
