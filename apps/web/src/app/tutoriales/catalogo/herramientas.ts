import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

export const HERRAMIENTAS = 'Herramientas públicas'

/** El cotizador: lo que AportaYa cobra, antes de operar y sin cuenta. */
export const cotizarUnaComision: TutorialDefinicion = {
  id: 'sitio-cotizar',
  version: '1.0.0',
  titulo: 'Saber cuánto te van a cobrar',
  descripcion: 'Cotizar una comisión con impuestos incluidos, antes de hacer la operación.',
  categoria: HERRAMIENTAS,
  ruta: '/tarifas',
  minutos: 3,
  dificultad: 'inicial',
  requisitos: ['sitio-que-es'],
  siguiente: 'sitio-plazo',
  pasos: [
    {
      id: 'preaviso',
      titulo: 'Si una tarifa sube, se avisa antes',
      descripcion: 'No cambia de un día para el otro: el tarifario se avisa con anticipación y con la fecha desde la que rige.',
      objetivo: 'tarifas-preaviso',
      ruta: '/tarifas',
      posicion: 'abajo',
      accion: { tipo: 'navegar', ruta: '/tarifas' },
    },
    {
      id: 'cotizador',
      titulo: 'Probá el cotizador',
      descripcion: 'Elegís la operación y el monto, y te dice cuánto se cobra por ella. Es la misma cuenta que hace la app; no es una estimación.',
      objetivo: 'tarifas-cotizador',
      posicion: 'arriba',
      esperaMs: 6000,
      bloquea: false,
      accion: { tipo: 'clic', objetivo: 'tarifas-cotizador' },
      ayudaSiFalla: 'Pulsá el cotizador para que se abra y seguimos.',
    },
  ],
}

/** La calculadora de plazos: la única herramienta del sitio que pide escribir. */
export const calcularUnPlazo: TutorialDefinicion = {
  id: 'sitio-plazo',
  version: '1.0.0',
  titulo: 'Calcular hasta cuándo tenés tiempo',
  descripcion: 'Los plazos de reclamo se cuentan en días hábiles. Acá se calcula el vencimiento exacto.',
  categoria: HERRAMIENTAS,
  ruta: '/plazos',
  minutos: 3,
  dificultad: 'intermedio',
  requisitos: ['sitio-que-es'],
  pasos: [
    {
      id: 'por-que',
      titulo: 'Los feriados no cuentan',
      descripcion: 'Un plazo de cinco días hábiles no vence en cinco días corridos. La calculadora usa el mismo calendario que la plataforma, así que el número que ves acá es el que vale.',
      objetivo: 'plazos-por-que',
      ruta: '/plazos',
      posicion: 'abajo',
      accion: { tipo: 'navegar', ruta: '/plazos' },
    },
    {
      id: 'calcular',
      titulo: 'Poné la fecha y calculá',
      descripcion: 'Desde qué día corre el plazo y cuántos días hábiles son. Pulsá «Calcular» para ver el vencimiento.',
      objetivo: 'plazos-calculadora',
      posicion: 'arriba',
      esperaMs: 6000,
      bloquea: false,
      accion: { tipo: 'clic', objetivo: 'plazos-calcular' },
      ayudaSiFalla: 'Pulsá «Calcular» para ver la fecha de vencimiento.',
    },
  ],
}

/** La transparencia: el argumento más fuerte del sitio, y el que más cuesta explicar. */
export const verificarSinCuenta: TutorialDefinicion = {
  id: 'sitio-verificar',
  version: '1.0.0',
  titulo: 'Comprobar que el sorteo no está arreglado',
  descripcion: 'Qué se puede verificar desde afuera, sin cuenta y sin pedirle permiso a nadie.',
  categoria: HERRAMIENTAS,
  ruta: '/transparencia',
  minutos: 3,
  dificultad: 'intermedio',
  requisitos: ['sitio-como-funciona'],
  pasos: [
    {
      id: 'que-se-verifica',
      titulo: 'Se publica la semilla, no la promesa',
      descripcion: 'Antes del sorteo se publica un compromiso; después, la semilla. Con las dos, cualquiera repite el sorteo y comprueba que el orden es el que salió.',
      objetivo: 'transparencia-titulo',
      ruta: '/transparencia',
      posicion: 'abajo',
      accion: { tipo: 'navegar', ruta: '/transparencia' },
    },
    {
      id: 'tambien-vos',
      titulo: 'Y lo puede hacer cualquiera, no solo vos',
      descripcion: 'Un participante puede pasarle el enlace a quien quiera. Verificar no exige tener cuenta: si hiciera falta permiso, no sería verificación.',
      objetivo: 'transparencia-titulo',
      posicion: 'abajo',
    },
  ],
}
