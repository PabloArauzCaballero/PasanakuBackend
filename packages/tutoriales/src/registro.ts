import { Injectable, InjectionToken, PendingTasks, computed, inject, signal } from '@angular/core'
import { IDENTIDAD_DE_TUTORIALES } from './identidad'
import type { PasoDeTutorial, TutorialDefinicion } from './tipos'
import { validarCatalogo, type ProblemaDeCatalogo, type RutasConocidas } from './validacion'

/** Trae un pedazo del catálogo. Es una función para que el `import()` sea perezoso. */
export type CargadorDeTutoriales = () => Promise<readonly TutorialDefinicion[]>

/**
 * Cada módulo enchufa su cargador con `multi: true`. Agregar tutoriales es agregar un
 * `provide` con su `import()`; el motor, el registro y las pantallas no cambian.
 *
 * **Son cargadores y no valores** por una razón medible: el catálogo entero son varios
 * kilobytes de texto que nadie necesita hasta que alguien pide ayuda. Traerlo eager
 * engordaba el paquete inicial del backoffice un 15 %.
 */
export const CARGADORES_DE_TUTORIALES = new InjectionToken<readonly CargadorDeTutoriales[]>('CargadoresDeTutoriales')

/** Las rutas contra las que se valida el catálogo. Las declara `app.config.ts`. */
export const RUTAS_DEL_PRODUCTO = new InjectionToken<RutasConocidas>('RutasDelProducto')

/**
 * **El registro: qué tutoriales existen y cuáles puede ver quien está mirando.**
 *
 * Filtra por permiso, y filtra DOS veces: el tutorial entero (un operador de
 * contabilidad no ve el de cumplimiento) y cada paso (el mismo tutorial puede tener un
 * paso que solo tiene sentido con un permiso extra). Nunca al revés: el tutorial no
 * abre nada, solo señala lo que la pantalla ya muestra. Si alguien manipulara el
 * catálogo, lo peor que consigue es que le señalen un botón que el servidor no le deja
 * pulsar.
 */
@Injectable({ providedIn: 'root' })
export class RegistroDeTutoriales {
  private readonly identidad = inject(IDENTIDAD_DE_TUTORIALES)
  private readonly rutas = inject(RUTAS_DEL_PRODUCTO, { optional: true }) ?? []
  private readonly cargadores = inject(CARGADORES_DE_TUTORIALES, { optional: true }) ?? []
  private readonly pendientes = inject(PendingTasks)
  private readonly catalogo = signal<readonly TutorialDefinicion[]>([])
  private pedido: Promise<void> | null = null

  /** El catálogo crudo, sin filtrar. Vacío hasta que alguien llame a `cargar()`. */
  readonly todos = this.catalogo.asReadonly()

  /** Lo que está mal escrito en el catálogo. En un catálogo sano, vacío. */
  readonly problemas = computed<readonly ProblemaDeCatalogo[]>(() => validarCatalogo(this.catalogo(), this.rutas))

  /** Los que esta sesión puede ver, ya con sus pasos recortados por permiso. */
  readonly disponibles = computed<readonly TutorialDefinicion[]>(() =>
    this.catalogo().filter((t) => this.alcanzaPara(t)).map((t) => ({ ...t, pasos: this.pasosVisibles(t.pasos) })).filter((t) => t.pasos.length > 0),
  )

  /** Índice por id de lo disponible, que es lo único que el centro de ayuda muestra. */
  readonly porId = computed<ReadonlyMap<string, TutorialDefinicion>>(() => new Map(this.disponibles().map((t) => [t.id, t])))

  /** Las categorías presentes, en el orden en que aparecen en el catálogo. */
  readonly categorias = computed<readonly string[]>(() => [...new Set(this.disponibles().map((t) => t.categoria))])

  /**
   * Trae el catálogo una sola vez, aunque se lo pidan cinco pantallas a la vez.
   *
   * Va dentro de `PendingTasks` para que el renderizado en servidor **espere** al
   * `import()`: sin eso el sitio público prerrenderizaba la guía vacía y la lista
   * aparecía recién al hidratar, que para una página indexable es no existir.
   */
  cargar(): Promise<void> {
    if (this.pedido === null) {
      const trabajo = Promise.all(this.cargadores.map((traer) => traer())).then((partes) => {
        this.catalogo.set(partes.flat())
      })
      this.pedido = trabajo
      this.pendientes.run(() => trabajo)
    }
    return this.pedido
  }

  buscar(id: string): TutorialDefinicion | undefined {
    return this.porId().get(id)
  }

  /**
   * El tutorial que corresponde a una URL: el de la ruta más específica que la URL
   * empieza por. Es lo que enciende el lanzador «¿Cómo funciona esta pantalla?».
   */
  paraLaRuta(url: string): TutorialDefinicion | undefined {
    const limpia = (url.split('?')[0] ?? url).replace(/\/+$/, '') || '/'
    return this.disponibles()
      .filter((t) => t.ruta !== undefined && (limpia === t.ruta || limpia.startsWith(`${t.ruta}/`)))
      .sort((a, b) => (b.ruta?.length ?? 0) - (a.ruta?.length ?? 0))[0]
  }

  private alcanzaPara(t: TutorialDefinicion): boolean {
    return (t.permisos ?? []).every((p) => this.identidad.puede(p))
  }

  private pasosVisibles(pasos: readonly PasoDeTutorial[]): readonly PasoDeTutorial[] {
    return pasos.filter((p) => p.permiso === undefined || this.identidad.puede(p.permiso))
  }
}
