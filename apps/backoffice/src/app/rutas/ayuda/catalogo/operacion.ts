import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

export const OPERACION = 'Operación'

/** Todo lo de acá exige `ver:operacion`: quien no atiende operaciones no lo ve. */
export const mirarUnaBilletera: TutorialDefinicion = {
  id: 'operacion-billetera',
  version: '1.0.0',
  titulo: 'Mirar la billetera de una persona',
  descripcion: 'Qué significa disponible, qué significa retenido y por qué el saldo siempre viene con una hora de corte.',
  categoria: OPERACION,
  ruta: '/operacion',
  permisos: ['ver:operacion'],
  minutos: 4,
  dificultad: 'inicial',
  requisitos: ['intro-plataforma'],
  siguiente: 'operacion-reclamos',
  pasos: [
    {
      id: 'donde-estoy',
      titulo: 'La billetera de un tercero',
      descripcion: 'Esta pantalla muestra el saldo de la cuenta de otra persona. Se abre para atender un reclamo o verificar un movimiento, y cada vez que la abrís queda registrado quién la miró.',
      objetivo: 'operacion-billetera',
      ruta: '/operacion',
      posicion: 'abajo',
    },
    {
      id: 'disponible-y-retenido',
      titulo: 'Disponible no es todo lo que hay',
      descripcion: 'Disponible es lo que puede usar ahora. Retenido es plata suya que está comprometida —un aporte en curso, una garantía—, no plata que perdió.',
      objetivo: 'operacion-saldo',
      posicion: 'arriba',
      automatica: 'desplazar',
    },
    {
      id: 'hora-de-corte',
      titulo: 'Siempre hay una hora de corte',
      descripcion: 'El saldo dice a qué momento corresponde. Si alguien te dice un número distinto al que ves, comparen primero la hora: casi siempre es eso.',
      objetivo: 'operacion-saldo',
      posicion: 'arriba',
    },
  ],
}

export const atenderUnReclamo: TutorialDefinicion = {
  id: 'operacion-reclamos',
  version: '1.0.0',
  titulo: 'Encontrar y leer un reclamo',
  descripcion: 'Cómo se consulta la bandeja, qué dice cada reclamo y qué plazo corre.',
  categoria: OPERACION,
  ruta: '/operacion/reclamos',
  permisos: ['ver:operacion'],
  minutos: 4,
  dificultad: 'intermedio',
  requisitos: ['operacion-billetera'],
  pasos: [
    {
      id: 'la-bandeja',
      titulo: 'La bandeja de reclamos',
      descripcion: 'Cada fila es un reclamo de una persona real, con un plazo legal corriendo. La bandeja es el lugar donde se ve qué está por vencer.',
      objetivo: 'operacion-reclamos',
      ruta: '/operacion/reclamos',
      posicion: 'abajo',
    },
    {
      id: 'el-plazo',
      titulo: 'El plazo no se negocia',
      descripcion: 'Los plazos se cuentan en días hábiles y los fija la norma, no nosotros. Un reclamo sin responder a tiempo es un incumplimiento nuestro, no del reclamante.',
      objetivo: 'operacion-reclamos',
      posicion: 'abajo',
    },
    {
      id: 'volver-a-la-billetera',
      titulo: 'Y si hace falta ver la plata',
      descripcion: 'Casi todo reclamo de dinero se entiende mirando la billetera de quien reclama. Está a un clic, en la misma sección.',
      objetivo: 'menu-operacion',
      posicion: 'derecha',
    },
  ],
}
