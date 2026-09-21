import { InjectionToken } from '@angular/core'

/**
 * **Cómo se presenta el centro en cada producto.** Es lo único del centro que cambia
 * entre el backoffice y el sitio público: el resto de los textos —«Comenzar»,
 * «Pendiente», «Antes conviene hacer»— dicen lo mismo en los dos.
 *
 * Sin esto, el sitio público le decía a un visitante sin cuenta que estaba aprendiendo
 * a usar «el backoffice».
 */
export interface PresentacionDelCentro {
  readonly titulo: string
  readonly proposito: string
}

export const PRESENTACION_DEL_CENTRO = new InjectionToken<PresentacionDelCentro>('PresentacionDelCentro')

/** Lo que se muestra si nadie dice otra cosa. */
export const presentacionPorOmision: PresentacionDelCentro = {
  titulo: 'Centro de tutoriales',
  proposito: 'Aprender haciendo, sobre las pantallas de verdad. Ningún tutorial toca datos ni confirma operaciones.',
}

/** Los textos del centro de tutoriales, en voz de marca. Un archivo por dominio. */
export const textosAyuda = {
  buscar: 'Buscar un tutorial',
  filtrarEstado: 'Filtrar por estado',
  filtrarCategoria: 'Filtrar por módulo',
  todos: 'Todos',
  pendientes: 'Pendientes',
  enProgreso: 'En progreso',
  completados: 'Completados',
  obligatorios: 'Obligatorios',
  recomendado: 'Recomendado para vos',
  avanceTitulo: 'Tu avance',
  avanceEtiqueta: 'Avance general de los tutoriales',
  avanceDe: (hechos: number, total: number): string => `${hechos} de ${total} tutoriales completados`,
  sinResultados: 'Ningún tutorial coincide con lo que buscaste. Probá con otra palabra o sacá los filtros.',
  sinTutoriales: 'Todavía no hay tutoriales para tu rol. Cuando los haya, aparecen acá.',
  comenzar: 'Comenzar',
  continuar: 'Continuar',
  repetir: 'Repetir',
  reiniciar: 'Reiniciar',
  minutos: (n: number): string => `${n} min`,
  pasos: (n: number): string => (n === 1 ? '1 paso' : `${n} pasos`),
  obligatorio: 'Obligatorio',
  requisitos: 'Antes conviene hacer',
  estado: {
    pendiente: 'Pendiente',
    'en-progreso': 'En progreso',
    completado: 'Completado',
    omitido: 'A medias',
  },
  dificultad: {
    inicial: 'Inicial',
    intermedio: 'Intermedio',
    avanzado: 'Avanzado',
  },
  problemasTitulo: 'Problemas en el catálogo',
  problemasAyuda: 'Esto lo ve cualquiera que entre, a propósito: un tutorial mal escrito es un error de la herramienta, no del catálogo secreto de alguien.',
} as const
