import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

export const ENTENDER = 'Entender AportaYa'

/**
 * Los tutoriales del **sitio público**: los hace alguien sin cuenta, que todavía está
 * decidiendo si confiar. Por eso ninguno pide datos ni promete nada: muestran dónde
 * está escrito qué cobra AportaYa y cómo se verifica lo que dice.
 */
export const queEsAportaYa: TutorialDefinicion = {
  id: 'sitio-que-es',
  version: '1.0.0',
  titulo: 'Qué es AportaYa',
  descripcion: 'El pasanaku de siempre, sin el cuaderno. De qué se trata y por dónde se empieza.',
  categoria: ENTENDER,
  ruta: '/',
  minutos: 2,
  dificultad: 'inicial',
  obligatorio: true,
  siguiente: 'sitio-como-funciona',
  pasos: [
    {
      id: 'que-hace',
      titulo: 'Un pasanaku, pero sin cuaderno',
      descripcion: 'Un grupo junta plata todos los meses y cada mes le toca a alguien. AportaYa lleva la cuenta, cobra los aportes y entrega el turno; el grupo sigue siendo el grupo.',
      objetivo: 'sitio-que-hace',
      ruta: '/',
      posicion: 'abajo',
      automatica: 'desplazar',
    },
    {
      id: 'menu',
      titulo: 'Todo lo que promete está escrito',
      descripcion: 'Cómo funciona, qué cobra, cómo custodia tu plata y cómo se verifica un sorteo. Cada cosa tiene su página, y ninguna necesita que te crees una cuenta.',
      objetivo: 'sitio-menu',
      posicion: 'abajo',
    },
    {
      id: 'empezar',
      titulo: 'Y cuando quieras, se empieza acá',
      descripcion: 'La app es donde armás tu grupo y aportás. Este sitio es para mirar antes de entrar.',
      objetivo: 'sitio-empezar',
      posicion: 'abajo',
    },
  ],
}

export const comoFunciona: TutorialDefinicion = {
  id: 'sitio-como-funciona',
  version: '1.0.0',
  titulo: 'Cómo funciona un pasanaku digital',
  descripcion: 'Grupos, aportes, turnos por sorteo y entrega del fondo, en orden.',
  categoria: ENTENDER,
  ruta: '/como-funciona',
  minutos: 3,
  dificultad: 'inicial',
  requisitos: ['sitio-que-es'],
  siguiente: 'sitio-cotizar',
  pasos: [
    {
      id: 'recorrido',
      titulo: 'De armar el grupo a cobrar tu turno',
      descripcion: 'Esta página cuenta el recorrido completo: quién puede entrar, cada cuánto se aporta, cómo se decide el orden de los turnos y qué pasa si alguien no paga.',
      objetivo: 'como-funciona-titulo',
      ruta: '/como-funciona',
      posicion: 'abajo',
      accion: { tipo: 'navegar', ruta: '/como-funciona' },
    },
    {
      id: 'el-sorteo',
      titulo: 'El orden no lo elige nadie a dedo',
      descripcion: 'Los turnos salen de un sorteo que después cualquiera puede repetir y comprobar. Eso se mira en Transparencia, sin cuenta y sin permiso.',
      objetivo: 'sitio-menu',
      posicion: 'abajo',
    },
  ],
}
