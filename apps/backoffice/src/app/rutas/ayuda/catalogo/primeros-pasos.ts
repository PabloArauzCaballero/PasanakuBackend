import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

/** La categoría con la que se agrupan en el centro de ayuda. */
export const PRIMEROS_PASOS = 'Primeros pasos'

/**
 * Los cuatro tutoriales que puede hacer cualquiera con sesión abierta, sin importar su
 * rol. Son DATOS: ninguno importa un componente ni sabe cómo se dibuja un globo.
 */
export const introduccionALaPlataforma: TutorialDefinicion = {
  id: 'intro-plataforma',
  version: '1.0.0',
  titulo: 'Qué es el backoffice de AportaYa',
  descripcion: 'De qué se ocupa esta herramienta, qué parte te toca a vos y dónde está cada cosa.',
  categoria: PRIMEROS_PASOS,
  ruta: '/tablero',
  minutos: 3,
  dificultad: 'inicial',
  obligatorio: true,
  siguiente: 'navegacion-principal',
  pasos: [
    {
      id: 'bienvenida',
      titulo: 'Esto es el backoffice',
      descripcion: 'Desde acá se atiende lo que la app del participante no resuelve sola: verificar identidades, mirar saldos, cerrar períodos, revisar campañas. Ninguna pantalla de acá la ve un participante.',
      objetivo: 'tablero-accesos',
      posicion: 'abajo',
      automatica: 'desplazar',
    },
    {
      id: 'tu-rol',
      titulo: 'Tu rol decide qué ves',
      descripcion: 'Acá arriba está el rol con el que entraste. El menú muestra solo las secciones que tu rol abre; si no ves una, no es un error.',
      objetivo: 'cabecera-rol',
      posicion: 'abajo',
    },
    {
      id: 'el-menu',
      titulo: 'El menú es el mapa',
      descripcion: 'Cada entrada es un dominio completo. Se puede volver al tablero en cualquier momento sin perder nada de lo que estabas mirando.',
      objetivo: 'menu-principal',
      posicion: 'derecha',
    },
    {
      id: 'ayuda-en-pantalla',
      titulo: 'Este botón te trae de vuelta',
      descripcion: 'En cualquier pantalla con tutorial vas a ver este botón. Lo pulsás y te explica esa pantalla, sin salir de ella.',
      objetivo: 'lanzador-de-tutorial',
      posicion: 'abajo',
    },
  ],
}

export const navegacionPrincipal: TutorialDefinicion = {
  id: 'navegacion-principal',
  version: '1.0.0',
  titulo: 'Moverse por las secciones',
  descripcion: 'Cómo ir de una sección a otra, qué hay en cada una y cómo volver.',
  categoria: PRIMEROS_PASOS,
  ruta: '/tablero',
  minutos: 3,
  dificultad: 'inicial',
  requisitos: ['intro-plataforma'],
  siguiente: 'usar-el-centro-de-ayuda',
  pasos: [
    {
      id: 'tablero',
      titulo: 'El tablero es el punto de entrada',
      descripcion: 'Cada tarjeta lleva a un dominio y dice de qué se ocupa. Es lo que ves al entrar, y a lo que volvés con el logo.',
      objetivo: 'tablero-accesos',
      ruta: '/tablero',
      posicion: 'abajo',
    },
    {
      id: 'abrir-una-seccion',
      titulo: 'Probá abrir una sección',
      descripcion: 'Pulsá cualquier entrada del menú. Te llevamos con vos: el tutorial sigue abierto del otro lado.',
      objetivo: 'menu-principal',
      posicion: 'derecha',
      accion: { tipo: 'clic', objetivo: 'menu-principal' },
      ayudaSiFalla: 'Pulsá una de las entradas del menú de la izquierda para seguir.',
    },
    {
      id: 'volver',
      titulo: 'Y volver siempre es posible',
      descripcion: 'El menú no se va nunca: está en todas las pantallas, con la sección actual marcada. No hace falta el botón de atrás del navegador.',
      objetivo: 'menu-principal',
      posicion: 'derecha',
    },
  ],
}

export const tuSesionYTuRol: TutorialDefinicion = {
  id: 'sesion-y-rol',
  version: '1.0.0',
  titulo: 'Tu sesión, tu rol y lo que podés hacer',
  descripcion: 'Por qué dos personas ven cosas distintas y qué significa que una acción esté deshabilitada.',
  categoria: PRIMEROS_PASOS,
  ruta: '/tablero',
  minutos: 2,
  dificultad: 'inicial',
  requisitos: ['intro-plataforma'],
  pasos: [
    {
      id: 'el-rol',
      titulo: 'Entraste con un rol',
      descripcion: 'El rol es lo que el servidor usa para decidir qué te deja hacer. No se cambia desde acá: lo asigna Sistemas.',
      objetivo: 'cabecera-rol',
      posicion: 'abajo',
    },
    {
      id: 'el-menu-filtrado',
      titulo: 'El menú ya viene filtrado',
      descripcion: 'Solo aparecen las secciones que tu rol abre. Ocultar es una comodidad: aunque escribieras la dirección a mano, el servidor te diría que no.',
      objetivo: 'menu-principal',
      posicion: 'derecha',
    },
    {
      id: 'acciones-apagadas',
      titulo: 'Una acción apagada no es una falla',
      descripcion: 'Cuando un botón está deshabilitado es porque falta algo —un dato, una firma, un permiso—. La pantalla siempre dice qué falta; si no lo dice, es un error que vale la pena reportar.',
      objetivo: 'tablero-accesos',
      posicion: 'abajo',
    },
  ],
}

export const usarElCentroDeAyuda: TutorialDefinicion = {
  id: 'usar-el-centro-de-ayuda',
  version: '1.0.0',
  titulo: 'Usar el centro de tutoriales',
  descripcion: 'Buscar, filtrar, retomar lo que dejaste a medias y repetir lo que quieras.',
  categoria: PRIMEROS_PASOS,
  ruta: '/ayuda',
  minutos: 2,
  dificultad: 'inicial',
  pasos: [
    {
      id: 'avance',
      titulo: 'Tu avance, de un vistazo',
      descripcion: 'Acá está cuánto llevás hecho. Cuenta los tutoriales completados sobre los que tu rol puede ver.',
      objetivo: 'ayuda-avance',
      ruta: '/ayuda',
      posicion: 'abajo',
    },
    {
      id: 'buscar',
      titulo: 'Buscar por palabra',
      descripcion: 'Escribí lo que querés aprender —«campaña», «expediente», «período»— y la lista se recorta mientras escribís.',
      objetivo: 'ayuda-buscador',
      posicion: 'abajo',
      accion: { tipo: 'escribir', objetivo: 'ayuda-buscador', minimo: 2 },
      ayudaSiFalla: 'Escribí al menos dos letras en el buscador para seguir.',
      automatica: 'enfocar',
    },
    {
      id: 'filtrar',
      titulo: 'Filtrar por estado y categoría',
      descripcion: 'Pendientes, en progreso, completados u obligatorios. Es la forma rápida de ver qué te falta.',
      objetivo: 'ayuda-filtros',
      posicion: 'abajo',
    },
    {
      id: 'repetir',
      titulo: 'Repetir cuesta un clic',
      descripcion: 'Ningún tutorial se agota. «Reiniciar» borra tu avance de ese tutorial y lo deja como el primer día.',
      objetivo: 'ayuda-lista',
      posicion: 'arriba',
    },
  ],
}
