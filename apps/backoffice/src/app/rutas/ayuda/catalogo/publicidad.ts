import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

export const PUBLICIDAD = 'Publicidad'

/**
 * Crear una campaña es el registro más importante que se da de alta **desde el
 * backoffice** (todo lo demás nace en la app del participante o en un proceso).
 *
 * El tutorial abre el formulario y te hace escribir el nombre, pero **no guarda**: el
 * último paso te deja frente al botón y se corre a un lado. Guardar es una decisión
 * tuya, con datos tuyos, no un efecto de una lección.
 */
export const crearUnaCampana: TutorialDefinicion = {
  id: 'publicidad-crear-campana',
  version: '1.0.0',
  titulo: 'Dar de alta una campaña',
  descripcion: 'Desde la tabla hasta el formulario: qué pide, qué valida y qué pasa después de guardar.',
  categoria: PUBLICIDAD,
  ruta: '/publicidad/campanas',
  permisos: ['ver:publicidad'],
  minutos: 5,
  dificultad: 'intermedio',
  requisitos: ['intro-plataforma'],
  siguiente: 'tablas-y-filtros',
  pasos: [
    {
      id: 'la-tabla',
      titulo: 'Todo empieza en la tabla',
      descripcion: 'Acá están las campañas que existen, con su estado y su presupuesto. Antes de crear una, conviene mirar si ya está.',
      objetivo: 'publicidad-tabla-campanas',
      ruta: '/publicidad/campanas',
      posicion: 'arriba',
      esperaMs: 6000,
    },
    {
      id: 'abrir-el-formulario',
      titulo: 'Abrí el formulario',
      descripcion: 'Pulsá «Nueva campaña». Se abre una ventana sobre la tabla; no vas a perder lo que estabas mirando.',
      objetivo: 'publicidad-nueva-campana',
      posicion: 'abajo',
      accion: { tipo: 'clic', objetivo: 'publicidad-nueva-campana' },
      ayudaSiFalla: 'Pulsá el botón «Nueva campaña» para seguir.',
      bloquea: false,
    },
    {
      id: 'el-formulario',
      titulo: 'Dos datos, y los dos obligatorios',
      descripcion: 'Nombre y presupuesto. El presupuesto se escribe como monto, con su moneda: la pantalla nunca recalcula plata por su cuenta.',
      objetivo: 'publicidad-formulario-campana',
      posicion: 'derecha',
      esperaMs: 5000,
      accion: { tipo: 'aparezca', objetivo: 'publicidad-formulario-campana' },
    },
    {
      id: 'guardar-es-tuyo',
      titulo: 'Guardar lo decidís vos',
      descripcion: 'Acá te soltamos. Si querés practicar, cerrá la ventana con «Cancelar»: no se crea nada. Si de verdad vas a crear la campaña, completá los datos reales y guardá.',
      objetivo: 'publicidad-formulario-campana',
      posicion: 'derecha',
    },
  ],
}

export const tablasYFiltros: TutorialDefinicion = {
  id: 'tablas-y-filtros',
  version: '1.0.0',
  titulo: 'Tablas, orden y filtros',
  descripcion: 'Cómo se recorre una tabla larga, cómo se ordena y por qué el estado de la tabla vive en la dirección.',
  categoria: PUBLICIDAD,
  ruta: '/publicidad/campanas',
  permisos: ['ver:publicidad'],
  minutos: 4,
  dificultad: 'intermedio',
  requisitos: ['publicidad-crear-campana'],
  pasos: [
    {
      id: 'tabla-larga',
      titulo: 'Las tablas no traen todo de golpe',
      descripcion: 'Se piden de a páginas y se dibujan solo las filas que se ven. Por eso una tabla de miles de filas abre igual de rápido que una de diez.',
      objetivo: 'publicidad-tabla-campanas',
      ruta: '/publicidad/campanas',
      posicion: 'arriba',
      esperaMs: 6000,
    },
    {
      id: 'teclado',
      titulo: 'Se recorre con el teclado',
      descripcion: 'Las flechas mueven la fila activa y Enter la abre. Para una jornada entera de revisión, el teclado gana siempre.',
      objetivo: 'publicidad-tabla-campanas',
      posicion: 'arriba',
    },
    {
      id: 'la-direccion',
      titulo: 'El filtro viaja en la dirección',
      descripcion: 'Al ordenar o filtrar, la dirección del navegador cambia. Eso significa que podés copiarla y mandársela a alguien: va a ver exactamente lo mismo que vos.',
      objetivo: 'publicidad-tabla-campanas',
      posicion: 'arriba',
    },
  ],
}
