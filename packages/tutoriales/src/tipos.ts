/**
 * **El contrato del motor de tutoriales.** Solo tipos: ni una importación de Angular,
 * ni una línea de DOM. Un tutorial es DATO —se declara en `rutas/ayuda/catalogo/`— y
 * el motor no sabe nada de ninguno en particular. Agregar un tutorial es agregar un
 * archivo de configuración; el núcleo no se toca.
 *
 * Los textos viven acá y no en `textos.ts` de un dominio a propósito: un tutorial es
 * una unidad completa (qué enseña, dónde, a quién, en qué orden), y partirlo entre dos
 * archivos obliga a editar dos lugares para cambiar una frase.
 */

/** Dónde se planta el globo respecto del elemento resaltado. */
export type PosicionDeGlobo = 'arriba' | 'abajo' | 'izquierda' | 'derecha' | 'centro'

/** Cuánto pide el tutorial de quien lo hace. Ordena el catálogo, no bloquea nada. */
export type Dificultad = 'inicial' | 'intermedio' | 'avanzado'

/**
 * Lo que el paso espera que haga la persona antes de dejarla avanzar.
 *
 * **Ninguna acción escribe nada en el servidor.** El motor observa lo que ya ocurre en
 * la pantalla (un clic, un campo con texto, una ruta que cambió); nunca dispara una
 * operación por su cuenta. Un tutorial no paga, no aprueba y no borra.
 */
export type AccionEsperada =
  /** El paso se lee y se sigue con «Siguiente». Es el caso por defecto. */
  | { readonly tipo: 'ninguna' }
  /** Hay que pulsar el elemento resaltado (o el que diga `objetivo`). */
  | { readonly tipo: 'clic'; readonly objetivo?: string }
  /** Hay que escribir en un campo: `minimo` caracteres para darlo por hecho. */
  | { readonly tipo: 'escribir'; readonly objetivo?: string; readonly minimo?: number }
  /** Hay que elegir una opción de un `select`, radio o segmentado. */
  | { readonly tipo: 'elegir'; readonly objetivo?: string }
  /** Hay que llegar a una ruta. El motor la compara con la URL del router. */
  | { readonly tipo: 'navegar'; readonly ruta: string }
  /** Hay que hacer que aparezca algo (una fila, un panel, un modal abierto). */
  | { readonly tipo: 'aparezca'; readonly objetivo: string }

/** Lo único que el motor hace solo, y las dos cosas que hace son inocuas. */
export type AccionAutomatica = 'desplazar' | 'enfocar'

/** Un paso: una sola idea, un solo elemento, una sola acción. */
export interface PasoDeTutorial {
  readonly id: string
  readonly titulo: string
  readonly descripcion: string
  /** El `data-tutorial-id` del elemento a resaltar. Sin él, el globo va centrado. */
  readonly objetivo?: string
  readonly posicion?: PosicionDeGlobo
  /** La ruta en la que este paso tiene sentido. El motor navega si hace falta. */
  readonly ruta?: string
  /** Solo para validar el orden declarado; el orden real es el del arreglo. */
  readonly orden?: number
  readonly accion?: AccionEsperada
  /** Qué decirle a quien se quedó trabado. Se muestra si la acción no se cumple. */
  readonly ayudaSiFalla?: string
  /** Cuánto esperar a un elemento que llega después de una petición. */
  readonly esperaMs?: number
  /** El paso se salta si la sesión no tiene este permiso. */
  readonly permiso?: string
  readonly automatica?: AccionAutomatica
  /**
   * Si es `false`, el hueco del foco deja pasar el ratón: es lo que permite que la
   * persona pulse de verdad el botón que el paso le está señalando.
   */
  readonly bloquea?: boolean
}

/** Un tutorial completo. Es una constante exportada, no una clase. */
export interface TutorialDefinicion {
  readonly id: string
  /** Cambiarla es decir «esto ya no es el mismo tutorial» (ver `avance.ts`). */
  readonly version: string
  readonly titulo: string
  readonly descripcion: string
  readonly categoria: string
  /** Dónde empieza. También es lo que enciende el lanzador contextual de la pantalla. */
  readonly ruta?: string
  /** Se muestra solo si la sesión tiene TODOS estos permisos. */
  readonly permisos?: readonly string[]
  readonly minutos?: number
  readonly dificultad: Dificultad
  readonly obligatorio?: boolean
  /** Ids de tutoriales que conviene haber hecho antes. No bloquean: avisan. */
  readonly requisitos?: readonly string[]
  /** El que se ofrece al terminar este. */
  readonly siguiente?: string
  readonly pasos: readonly PasoDeTutorial[]
}

/** Qué le pasó a una persona con un tutorial. */
export type EstadoDeProgreso = 'pendiente' | 'en-progreso' | 'completado' | 'omitido'

/** La fila de progreso. Es lo que viaja al almacén, local o remoto. */
export interface ProgresoDeTutorial {
  readonly tutorialId: string
  readonly version: string
  readonly estado: EstadoDeProgreso
  readonly pasoId: string | null
  readonly indice: number
  readonly iniciadoEn: string
  readonly terminadoEn: string | null
  readonly ultimaInteraccion: string
  readonly repeticiones: number
}

/** Por qué el motor no pudo seguir. Nunca deja la pantalla trabada sin decir nada. */
export type MotivoDeProblema = 'sin-objetivo' | 'sin-ruta' | 'accion-pendiente'

export interface ProblemaEnEjecucion {
  readonly motivo: MotivoDeProblema
  readonly pasoId: string
  readonly detalle: string
}
