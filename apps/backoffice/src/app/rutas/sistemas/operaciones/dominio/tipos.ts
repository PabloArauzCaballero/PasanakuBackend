export type MetodoHttp = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
export type NivelRiesgo = 'CRITICO' | 'ALTO' | 'MEDIO' | 'BAJO'

export type TablaImpactada = {
  esquema: string
  tabla: string
  operacion: 'INSERT' | 'UPDATE' | 'SELECT' | 'DELETE' | 'INSERT / UPDATE'
  impacto: string
  principal: boolean
  auditoria: boolean
}

export type CampoDTO = {
  nombre: string
  tipo: string
  requerido: boolean
  validacion: string
  descripcion: string
}

export type DTOOperacion = {
  nombre: string
  campos: CampoDTO[]
}

export type DecisionOperacion = {
  codigo: string
  http: number | null
  condicion: string
  siguiente: string
}

export type FlujoOperacion = {
  id: string
  nombre: string
  paso: string
  precondicion: string
  exito: string
  decisiones: DecisionOperacion[]
}

export type OperacionDelSistema = {
  id: string
  servicio: string
  metodo: MetodoHttp
  ruta: string
  operationId: string
  modulo: string
  cu: string | null
  proposito: string
  explicacionNegocio: string
  explicacionSistema: string
  permiso: string
  riesgo: NivelRiesgo
  sensible: boolean
  pideSesion: boolean
  idempotencia: boolean
  tablasImpactadas: TablaImpactada[]
  dtoEntrada: DTOOperacion | null
  dtoSalida: DTOOperacion | null
  payloadEjemplo: unknown
  payloadRespuesta: unknown
  flujo: FlujoOperacion
}

export type MetricasOperaciones = {
  total: number
  mutaciones: number
  lecturas: number
  criticas: number
  idempotentes: number
}

export type FiltrosOperaciones = {
  q: string
  servicio: string
  metodo: string
  riesgo: string
  flujo: string
}
