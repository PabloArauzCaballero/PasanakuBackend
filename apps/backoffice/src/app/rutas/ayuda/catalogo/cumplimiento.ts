import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

export const CUMPLIMIENTO = 'Cumplimiento'

/**
 * El flujo principal del negocio del backoffice: decidir si una persona es quien dice
 * ser. **El tutorial no aprueba ni rechaza nada**: enseña dónde está cada cosa y para
 * qué sirve. El botón lo pulsa una persona y el servidor vuelve a decidir.
 */
export const resolverUnExpediente: TutorialDefinicion = {
  id: 'cumplimiento-verificaciones',
  version: '1.0.0',
  titulo: 'Revisar un expediente de identidad',
  descripcion: 'La cola de verificación, cómo se filtra, qué mirar en las fotos y por qué el motivo es obligatorio al rechazar.',
  categoria: CUMPLIMIENTO,
  ruta: '/cumplimiento/verificaciones',
  permisos: ['ver:cumplimiento'],
  minutos: 6,
  dificultad: 'intermedio',
  obligatorio: true,
  requisitos: ['intro-plataforma'],
  pasos: [
    {
      id: 'la-cola',
      titulo: 'Esta es la cola de trabajo',
      descripcion: 'Cada tarjeta es alguien esperando su cuenta. Mientras el expediente está acá, esa persona no puede operar: la cola vacía es una buena noticia.',
      objetivo: 'cumplimiento-cola',
      ruta: '/cumplimiento/verificaciones',
      posicion: 'arriba',
      esperaMs: 6000,
    },
    {
      id: 'filtrar',
      titulo: 'Probá los filtros',
      descripcion: '«Por decidir» junta los dos estados que esperan a una persona. Los demás filtros sirven para auditar lo ya resuelto. Pulsá uno.',
      objetivo: 'cumplimiento-filtros',
      posicion: 'abajo',
      accion: { tipo: 'clic', objetivo: 'cumplimiento-filtros' },
      ayudaSiFalla: 'Pulsá cualquiera de los botones de filtro para seguir.',
    },
    {
      id: 'las-fotos',
      titulo: 'Las fotos se piden al mirarlas',
      descripcion: 'No se precargan: cada foto de una cédula es un dato sensible y cada lectura queda registrada con tu nombre. Se abren las tres juntas porque cotejar es mirar la cara y el documento a la vez.',
      objetivo: 'cumplimiento-cola',
      posicion: 'arriba',
    },
    {
      id: 'el-motivo',
      titulo: 'Rechazar exige un motivo',
      descripcion: 'Quien recibe un rechazo tiene derecho a saber por qué, y a presentar descargo. Por eso el botón de rechazar no se habilita hasta que escribís el motivo.',
      objetivo: 'cumplimiento-cola',
      posicion: 'arriba',
    },
    {
      id: 'expediente-incompleto',
      titulo: 'Y no se aprueba lo que no se puede mirar',
      descripcion: 'Si falta una foto, aprobar queda deshabilitado. No es un capricho de la pantalla: aprobar sin ver el documento entero es exactamente lo que la norma prohíbe.',
      objetivo: 'cumplimiento-cola',
      posicion: 'arriba',
    },
  ],
}
