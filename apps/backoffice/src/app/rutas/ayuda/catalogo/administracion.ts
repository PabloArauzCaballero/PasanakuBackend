import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

export const CONTABILIDAD = 'Contabilidad'
export const SISTEMAS = 'Sistemas'

export const cerrarUnPeriodo: TutorialDefinicion = {
  id: 'contabilidad-periodo',
  version: '1.0.0',
  titulo: 'Entender el período contable',
  descripcion: 'Qué es un período abierto, qué cambia al cerrarlo y por qué no se reabre a la ligera.',
  categoria: CONTABILIDAD,
  ruta: '/contabilidad',
  permisos: ['ver:contabilidad'],
  minutos: 4,
  dificultad: 'intermedio',
  requisitos: ['intro-plataforma'],
  pasos: [
    {
      id: 'el-periodo',
      titulo: 'Todo asiento cae en un período',
      descripcion: 'Mientras el período está abierto, se pueden registrar movimientos con esa fecha. Cerrado, no: los asientos van al siguiente.',
      objetivo: 'contabilidad-periodo',
      ruta: '/contabilidad',
      posicion: 'abajo',
      esperaMs: 6000,
    },
    {
      id: 'cerrar-es-serio',
      titulo: 'Cerrar es una decisión, no un botón',
      descripcion: 'El cierre congela las cifras con las que se arman los estados financieros. Se hace cuando la conciliación cuadra, no cuando se acaba el mes.',
      objetivo: 'contabilidad-periodo',
      posicion: 'abajo',
    },
    {
      id: 'y-el-menu',
      titulo: 'El resto de la sección cuelga de acá',
      descripcion: 'Presupuesto, compras, cobros, activos y estados financieros miran todos el mismo período. Si un número no cuadra, empezá por revisar cuál está abierto.',
      objetivo: 'menu-contabilidad',
      posicion: 'derecha',
    },
  ],
}

export const mirarLosServicios: TutorialDefinicion = {
  id: 'sistemas-servicios',
  version: '1.0.0',
  titulo: 'Ver si la plataforma está sana',
  descripcion: 'Qué muestra el panel de servicios y qué hacer cuando algo está en rojo.',
  categoria: SISTEMAS,
  ruta: '/sistemas',
  permisos: ['ver:sistemas'],
  minutos: 3,
  dificultad: 'avanzado',
  requisitos: ['intro-plataforma'],
  pasos: [
    {
      id: 'servicios',
      titulo: 'Un renglón por servicio',
      descripcion: 'Cada servicio de la plataforma reporta su estado. Rojo no siempre es caída: puede ser un objetivo de servicio incumplido, que es un aviso temprano.',
      objetivo: 'sistemas-servicios',
      ruta: '/sistemas',
      posicion: 'abajo',
      esperaMs: 6000,
    },
    {
      id: 'que-hacer',
      titulo: 'Lo primero es mirar despliegues',
      descripcion: 'La mayoría de los incidentes empieza en un cambio reciente. Despliegues, en esta misma sección, dice qué se soltó y cuándo.',
      objetivo: 'menu-sistemas',
      posicion: 'derecha',
    },
    {
      id: 'incidentes',
      titulo: 'Y si es de verdad, se abre un incidente',
      descripcion: 'Un incidente abierto es lo que hace que alguien se haga cargo y que después quede escrito qué pasó. Sin incidente, el problema se arregla y se olvida.',
      objetivo: 'menu-sistemas',
      posicion: 'derecha',
    },
  ],
}
