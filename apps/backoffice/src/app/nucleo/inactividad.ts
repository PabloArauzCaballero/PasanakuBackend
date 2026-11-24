import { DestroyRef, Injectable, InjectionToken, effect, inject, signal, untracked } from '@angular/core'
import { RefrescoDeSesion } from './refresco-de-sesion'
import { Sesion } from './sesion'

/**
 * ADR-038 §1 y P-8 (GU-15): la sesión del backoffice se cierra tras 15 minutos sin actividad. Dos minutos antes se avisa.
 * Los números viven en un token para que una prueba los pueda acortar; el servidor tiene su propio tope
 * (`aportaya.sesion.inactividad-operador`), igual de largo.
 */
export interface PlazosDeInactividad {
  /** Sin actividad durante esto, la sesión se cierra. */
  readonly limiteMs: number
  /** Cuánto antes del cierre aparece el aviso. */
  readonly avisoAntesMs: number
  /** Cada cuánto, como mucho, un operador activo renueva su sesión: es lo que el servidor ve como actividad. */
  readonly latidoMs: number
}

export const PLAZOS_DE_INACTIVIDAD = new InjectionToken<PlazosDeInactividad>('PLAZOS_DE_INACTIVIDAD', {
  factory: () => ({ limiteMs: 15 * 60_000, avisoAntesMs: 2 * 60_000, latidoMs: 5 * 60_000 }),
})

export type EstadoDeInactividad = 'ACTIVA' | 'AVISO' | 'CERRADA'

/** Lo que cuenta como que hay una persona: tocar, teclear, desplazar. Mover el puntero también, sin teclado no hay otra. */
const EVENTOS_HUMANOS = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'] as const
const CANAL = 'aportaya-backoffice-actividad'
/** Entre pestañas se avisa la actividad como mucho cada tanto: un `pointermove` por píxel no es un mensaje cada vez. */
const ECO_ENTRE_PESTANAS_MS = 5_000
const CADA_REVISION_MS = 1_000

/**
 * Vigila la actividad humana del operador y decide el estado de su sesión: `ACTIVA`, `AVISO` (faltan dos minutos) o
 * `CERRADA` (pasaron quince). No cierra nada por sí mismo: el shell muestra el aviso y hace el cierre, que revoca la
 * sesión en el servidor y la corta en todos los servicios (lista de revocación, ADR-030).
 *
 * - **Latido**: mientras hay actividad, renueva la sesión cada `latidoMs` como mucho. Para el servidor, la actividad es
 *   eso; sin latido, quien escribe un formulario largo sin pedir nada a la red parecería ausente.
 * - **Varias pestañas**: comparten la sesión, así que la actividad de una cuenta para todas (`BroadcastChannel`). Sin
 *   esto, la pestaña olvidada cerraría la sesión de la que se está usando.
 * - **Suspensión del equipo**: el estado se calcula con la hora real en cada revisión, no contando tics, así que al
 *   despertar una laptop con la sesión vencida se cierra de inmediato.
 */
@Injectable({ providedIn: 'root' })
export class VigilanteDeInactividad {
  private readonly sesion = inject(Sesion)
  private readonly refresco = inject(RefrescoDeSesion)
  private readonly plazos = inject(PLAZOS_DE_INACTIVIDAD)

  private readonly estadoInterno = signal<EstadoDeInactividad>('ACTIVA')
  readonly estado = this.estadoInterno.asReadonly()
  /** Cuándo se cerraría la sesión si nadie hace nada (epoch ms): el aviso lo muestra como hora. */
  readonly cierraEn = signal<number>(0)

  private ultimaActividad = Date.now()
  private ultimoLatido = Date.now()
  private ultimoEco = 0
  private latiendo = false
  private revisiones: ReturnType<typeof setInterval> | null = null
  private canal: BroadcastChannel | null = null
  private readonly alActuar = (): void => this.registrarActividad()

  constructor() {
    // Una sesión que se abre (ingreso o refresco) cuenta como actividad y como latido.
    effect(() => {
      const token = this.sesion.token()
      untracked(() => {
        if (!token) return
        this.ultimoLatido = Date.now()
        if (this.estadoInterno() === 'CERRADA') this.ultimaActividad = Date.now()
        this.revisar()
      })
    })
    inject(DestroyRef).onDestroy(() => this.detener())
  }

  /** Empieza a escuchar. Idempotente: el shell puede montarse más de una vez. */
  iniciar(): void {
    if (this.revisiones !== null) return
    this.ultimaActividad = Date.now()
    this.estadoInterno.set('ACTIVA')
    for (const evento of EVENTOS_HUMANOS) document.addEventListener(evento, this.alActuar, { passive: true, capture: true })
    if (typeof BroadcastChannel === 'function') {
      this.canal = new BroadcastChannel(CANAL)
      this.canal.onmessage = (m: MessageEvent<unknown>) => {
        if (typeof m.data === 'number' && m.data > this.ultimaActividad) this.marcar(m.data)
      }
    }
    this.revisiones = setInterval(() => this.revisar(), CADA_REVISION_MS)
  }

  detener(): void {
    for (const evento of EVENTOS_HUMANOS) document.removeEventListener(evento, this.alActuar, { capture: true })
    this.canal?.close()
    this.canal = null
    if (this.revisiones !== null) clearInterval(this.revisiones)
    this.revisiones = null
  }

  /** Hubo una persona: reinicia la cuenta, cierra el aviso, avisa a las otras pestañas y, si toca, late. */
  registrarActividad(): void {
    if (this.estadoInterno() === 'CERRADA' || !this.sesion.abierta()) return
    const ahora = Date.now()
    this.marcar(ahora)
    if (ahora - this.ultimoEco >= ECO_ENTRE_PESTANAS_MS) {
      this.ultimoEco = ahora
      this.canal?.postMessage(ahora)
    }
    if (ahora - this.ultimoLatido >= this.plazos.latidoMs) this.latir()
  }

  /** Recalcula el estado con la hora real. Público para que el shell lo pida al volver a la pestaña. */
  revisar(): void {
    if (!this.sesion.abierta() || this.estadoInterno() === 'CERRADA') return
    const inactivo = Date.now() - this.ultimaActividad
    this.cierraEn.set(this.ultimaActividad + this.plazos.limiteMs)
    if (inactivo >= this.plazos.limiteMs) this.estadoInterno.set('CERRADA')
    else if (inactivo >= this.plazos.limiteMs - this.plazos.avisoAntesMs) this.estadoInterno.set('AVISO')
    else this.estadoInterno.set('ACTIVA')
  }

  /** Después de cerrar (o de volver a entrar), se empieza de cero. */
  reiniciar(): void {
    this.ultimaActividad = Date.now()
    this.estadoInterno.set('ACTIVA')
  }

  private marcar(instante: number): void {
    this.ultimaActividad = instante
    if (this.estadoInterno() === 'AVISO') this.estadoInterno.set('ACTIVA')
  }

  private latir(): void {
    if (this.latiendo) return
    this.latiendo = true
    this.ultimoLatido = Date.now()
    this.refresco.refrescar().subscribe({
      next: (nueva) => {
        this.latiendo = false
        this.sesion.abrir(nueva.acceso, nueva.permisos ?? this.sesion.permisos(), nueva.rol ?? this.sesion.rol() ?? '', this.sesion.sujeto())
      },
      // Un latido que falla no cierra nada por sí mismo: la próxima petición verá el 401 y el interceptor decide.
      error: () => {
        this.latiendo = false
      },
    })
  }
}
