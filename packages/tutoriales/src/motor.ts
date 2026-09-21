import { Injectable, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { BITACORA_DE_TUTORIALES } from './bitacora'
import { cerrado, enPaso, progresoInicial } from './avance'
import { EncendidoDeTutorial } from './encendido'
import { LocalizadorDeObjetivo } from './objetivo'
import { ProgresoDeTutoriales } from './progreso'
import { RegistroDeTutoriales } from './registro'
import type { PasoDeTutorial, ProblemaEnEjecucion, ProgresoDeTutorial, TutorialDefinicion } from './tipos'

/**
 * **El motor: dónde estoy, cómo sigo y qué hago si el paso no se puede dar.**
 *
 * Es una máquina de estados con señales; no dibuja nada. El overlay, el globo y el
 * teclado viven en `layout/tutoriales/`, y el catálogo en `rutas/ayuda/catalogo/`.
 * Entre los tres no hay ningún `if` con el id de un tutorial concreto.
 *
 * Tres reglas que no se negocian:
 * 1. **No se traba.** Un objetivo que no aparece es un problema declarado con reintento
 *    y salida, nunca una pantalla congelada.
 * 2. **No opera.** Lo único que ejecuta solo es desplazar y enfocar. Ni un clic.
 * 3. **No inventa permisos.** Los pasos ya vienen recortados por `RegistroDeTutoriales`.
 */
@Injectable({ providedIn: 'root' })
export class MotorDeTutoriales {
  private readonly registro = inject(RegistroDeTutoriales)
  private readonly localizador = inject(LocalizadorDeObjetivo)
  private readonly progresos = inject(ProgresoDeTutoriales)
  private readonly encendido = inject(EncendidoDeTutorial)
  private readonly bitacora = inject(BITACORA_DE_TUTORIALES)
  private readonly router = inject(Router)

  private readonly tutorialActivo = signal<TutorialDefinicion | null>(null)
  private readonly indiceActual = signal(0)
  private readonly progresoActual = signal<ProgresoDeTutorial | null>(null)

  readonly tutorial = this.tutorialActivo.asReadonly()
  readonly indice = this.indiceActual.asReadonly()
  readonly activo = computed(() => this.tutorialActivo() !== null)
  readonly paso = computed<PasoDeTutorial | null>(() => this.tutorialActivo()?.pasos[this.indiceActual()] ?? null)
  readonly total = computed(() => this.tutorialActivo()?.pasos.length ?? 0)
  readonly esUltimo = computed(() => this.indiceActual() >= this.total() - 1)

  /** El elemento que hay que resaltar. `null` es «todavía no» o «no está». */
  readonly elemento = signal<HTMLElement | null>(null)
  /** Lo que impide seguir, en palabras. Se muestra en el globo, con reintento. */
  readonly problema = signal<ProblemaEnEjecucion | null>(null)
  /** La acción que el paso pedía ya está hecha. */
  readonly accionCumplida = signal(false)
  /** Está preguntando si de verdad quiere abandonar a la mitad. */
  readonly confirmandoSalida = signal(false)
  /** No pudimos guardar el avance (cuota, modo privado). Se avisa, no se esconde. */
  readonly sinGuardar = signal(false)

  readonly puedeAvanzar = computed(() => this.paso()?.accion === undefined || this.paso()?.accion?.tipo === 'ninguna' || this.accionCumplida())

  /** Empieza de cero. Si ya se había hecho, suma repetición al completarlo. */
  async iniciar(tutorialId: string, repeticiones = 0): Promise<void> {
    const tutorial = this.registro.buscar(tutorialId)
    if (tutorial === undefined || tutorial.pasos.length === 0) return
    this.encender(tutorial)
    this.progresoActual.set(progresoInicial(tutorial, ahora(), repeticiones))
    this.bitacora.anotar({ tipo: 'inicio', tutorialId })
    await this.ir(0)
  }

  /** Retoma donde quedó. Un índice fuera de rango vuelve al principio. */
  async continuar(tutorialId: string, progreso: ProgresoDeTutorial): Promise<void> {
    const tutorial = this.registro.buscar(tutorialId)
    if (tutorial === undefined || tutorial.pasos.length === 0) return
    this.encender(tutorial)
    this.progresoActual.set(progreso)
    this.bitacora.anotar({ tipo: 'inicio', tutorialId })
    await this.ir(Math.min(Math.max(progreso.indice, 0), tutorial.pasos.length - 1))
  }

  async avanzar(): Promise<void> {
    if (!this.puedeAvanzar()) {
      const paso = this.paso()
      if (paso !== null) this.anotarProblema('accion-pendiente', paso, paso.ayudaSiFalla ?? 'Hacé lo que pide el paso para seguir.')
      return
    }
    if (this.esUltimo()) return this.terminar()
    await this.ir(this.indiceActual() + 1)
  }

  async retroceder(): Promise<void> {
    if (this.indiceActual() === 0) return
    await this.ir(this.indiceActual() - 1)
  }

  /** Reintenta el paso actual: vuelve a buscar el objetivo desde cero. */
  async reintentar(): Promise<void> {
    await this.ir(this.indiceActual())
  }

  /** Pide confirmación si va por la mitad; si recién empezó, cierra sin preguntar. */
  pedirSalida(): void {
    if (this.indiceActual() === 0 || this.esUltimo()) return void this.omitir()
    this.confirmandoSalida.set(true)
  }

  seguirEnElTutorial(): void {
    this.confirmandoSalida.set(false)
  }

  /** Abandona: queda como omitido, con el paso donde se fue. Se puede repetir después. */
  omitir(): void {
    const tutorial = this.tutorialActivo()
    const progreso = this.progresoActual()
    if (tutorial !== null && progreso !== null) {
      this.bitacora.anotar({ tipo: 'omitido', tutorialId: tutorial.id, pasoId: this.paso()?.id ?? null })
      this.guardar(cerrado(enPaso(progreso, this.indiceActual(), this.paso()?.id ?? null, ahora()), 'omitido', ahora()))
    }
    this.apagar()
  }

  private async terminar(): Promise<void> {
    const tutorial = this.tutorialActivo()
    const progreso = this.progresoActual()
    if (tutorial !== null && progreso !== null) {
      this.bitacora.anotar({ tipo: 'fin', tutorialId: tutorial.id })
      this.guardar(cerrado({ ...progreso, indice: tutorial.pasos.length }, 'completado', ahora()))
    }
    this.apagar()
  }

  /**
   * El corazón: navegar si el paso vive en otra pantalla, esperar al elemento y dejar
   * todo listo. Cada regreso temprano deja un problema declarado, nunca silencio.
   */
  private async ir(indice: number): Promise<void> {
    const tutorial = this.tutorialActivo()
    const paso = tutorial?.pasos[indice]
    if (tutorial === undefined || tutorial === null || paso === undefined) return
    this.indiceActual.set(indice)
    this.accionCumplida.set(false)
    this.problema.set(null)
    this.elemento.set(null)
    this.bitacora.anotar({ tipo: 'paso', tutorialId: tutorial.id, pasoId: paso.id, indice })
    const progreso = this.progresoActual()
    if (progreso !== null) this.guardar(enPaso(progreso, indice, paso.id, ahora()))

    // El primer paso hereda la ruta del tutorial: `ruta` de la definición es «dónde
    // empieza». Sin esto, abrir desde el centro de ayuda un tutorial de otra pantalla
    // dejaba a la persona en el centro, con el primer objetivo imposible de encontrar.
    if (!(await this.llegarA(paso.ruta ?? (indice === 0 ? tutorial.ruta : undefined)))) return
    if (paso.objetivo === undefined) return
    const elemento = await this.localizador.esperar(paso.objetivo, paso.esperaMs)
    if (elemento === null) {
      this.anotarProblema('sin-objetivo', paso, `No encontramos «${paso.objetivo}» en esta pantalla.`)
      return
    }
    this.elemento.set(elemento)
    if (paso.automatica !== undefined) this.localizador.acercar(elemento, paso.automatica === 'enfocar')
  }

  /** Lleva a esa ruta si hace falta. `false` si no se pudo. */
  private async llegarA(ruta: string | undefined): Promise<boolean> {
    if (ruta === undefined) return true
    const actual = this.router.url.split('?')[0]
    if (actual === ruta || actual?.startsWith(`${ruta}/`) === true) return true
    const llegó = await this.router.navigateByUrl(ruta).catch(() => false)
    const paso = this.paso()
    if (llegó === false && paso !== null) {
      this.anotarProblema('sin-ruta', paso, `No pudimos abrir «${ruta}». Puede que tu rol no tenga esa sección.`)
      return false
    }
    return llegó !== false
  }

  private anotarProblema(motivo: ProblemaEnEjecucion['motivo'], paso: PasoDeTutorial, detalle: string): void {
    this.problema.set({ motivo, pasoId: paso.id, detalle })
    const tutorialId = this.tutorialActivo()?.id
    if (tutorialId !== undefined) this.bitacora.anotar({ tipo: 'problema', tutorialId, pasoId: paso.id, detalle })
  }

  private guardar(progreso: ProgresoDeTutorial): void {
    this.progresoActual.set(progreso)
    this.progresos.guardar(progreso).subscribe({ next: () => this.sinGuardar.set(false), error: () => this.sinGuardar.set(true) })
  }

  private encender(tutorial: TutorialDefinicion): void {
    this.tutorialActivo.set(tutorial)
    this.encendido.activo.set(true)
  }

  private apagar(): void {
    this.tutorialActivo.set(null)
    this.encendido.activo.set(false)
    this.elemento.set(null)
    this.problema.set(null)
    this.confirmandoSalida.set(false)
    this.indiceActual.set(0)
    this.progresoActual.set(null)
  }
}

const ahora = (): string => new Date().toISOString()
