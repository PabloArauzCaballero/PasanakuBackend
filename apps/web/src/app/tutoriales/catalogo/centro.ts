import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'

/** Cómo se usa el propio centro de tutoriales del sitio. */
export const usarElCentro: TutorialDefinicion = {
  id: 'sitio-usar-el-centro',
  version: '1.0.0',
  titulo: 'Usar esta guía',
  descripcion: 'Buscar, filtrar y repetir cualquiera de los recorridos.',
  categoria: 'Entender AportaYa',
  ruta: '/tutoriales',
  minutos: 2,
  dificultad: 'inicial',
  pasos: [
    {
      id: 'avance',
      titulo: 'Lo que llevás visto queda marcado',
      descripcion: 'Se guarda en este navegador, no en una cuenta: acá no hay que registrarse para nada.',
      objetivo: 'ayuda-avance',
      ruta: '/tutoriales',
      posicion: 'abajo',
    },
    {
      id: 'buscar',
      titulo: 'Buscá por palabra',
      descripcion: 'Escribí «tarifa», «sorteo», «plazo» y la lista se recorta mientras escribís.',
      objetivo: 'ayuda-buscador',
      posicion: 'abajo',
      accion: { tipo: 'escribir', objetivo: 'ayuda-buscador', minimo: 2 },
      ayudaSiFalla: 'Escribí al menos dos letras en el buscador para seguir.',
      automatica: 'enfocar',
    },
    {
      id: 'repetir',
      titulo: 'Ninguno se gasta',
      descripcion: 'Podés repetir cualquier recorrido las veces que quieras, o reiniciarlo para empezar de cero.',
      objetivo: 'ayuda-lista',
      posicion: 'arriba',
    },
  ],
}
