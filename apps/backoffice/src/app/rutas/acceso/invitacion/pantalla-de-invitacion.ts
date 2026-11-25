import { ChangeDetectionStrategy, Component, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { MarcoDeAcceso } from '../compartido/marco-de-acceso'
import { crearInvitacion, type SalidaCompletarInvitacion, type SalidaConsultaInvitacion, type SalidaInicioEnrolamientoTotp } from '../dominio/cuenta'
import { errorDeCuenta, MENSAJE_TOKEN_INVALIDO, type ErrorDeCuenta } from '../dominio/errores-de-cuenta'
import { FormularioDeInvitacion, type DatosDeInvitacion } from './formulario-de-invitacion'

type Estado = 'consultando' | 'formulario' | 'invalida' | 'sinRespuesta' | 'lista'

const TITULOS: Record<Estado, string> = {
  consultando: 'Estamos revisando tu invitación',
  formulario: 'Terminá de configurar tu acceso',
  invalida: 'Esta invitación no se puede usar',
  sinRespuesta: 'No pudimos revisar tu invitación',
  lista: 'Listo, ya podés entrar',
}
const CEJAS = { ALTA: 'Alta de operador', CLAVE: 'Clave nueva', FACTOR: 'Autenticador nuevo' } as const
const CIERRE = {
  ALTA: 'Tu cuenta quedó activa.',
  CLAVE: 'Tu clave nueva quedó registrada.',
  FACTOR: 'Tu autenticador nuevo quedó activo.',
} as const

/**
 * GU-C · Invitación del operador (`/acceso/invitacion#token=…`). El token llega en el **fragmento** (el servidor nunca lo ve en un
 * registro de acceso) y se saca de la URL **antes** de hacer nada con él: vive solo en memoria de esta pantalla. Pasos según el alcance
 * que dice `consultarInvitacion`: `ALTA` (clave + autenticador), `CLAVE` (clave) o `FACTOR` (clave actual + autenticador).
 * Un token vencido, usado o falso responde igual (`AP-CU09-04`) y acá se muestra igual: no se distingue la causa.
 */
@Component({
  selector: 'ap-pantalla-de-invitacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Esqueleto, EstadoDeError, FormularioDeInvitacion, MarcoDeAcceso, RouterLink],
  template: `
    <ap-marco-de-acceso #marco [ceja]="consulta() ? ceja() : 'Invitación'" [titulo]="titulo()" [bajada]="bajada()">
      @switch (estado()) {
        @case ('consultando') { <ap-esqueleto [lineas]="4" etiqueta="Revisando tu invitación" /> }
        @case ('invalida') {
          <ap-alerta tono="error">{{ mensajeInvalida }}</ap-alerta>
          <div class="acciones"><a class="enlace" routerLink="/acceso/recuperar">Recuperar mi acceso</a></div>
        }
        @case ('sinRespuesta') {
          <ap-estado-de-error titulo="No pudimos revisar tu invitación." mensaje="Tu enlace sigue guardado en esta pantalla: probá de nuevo cuando tengas conexión." [trazaId]="trazaId()" (reintentar)="consultar()" />
        }
        @case ('formulario') {
          <ap-formulario-de-invitacion #formulario [consulta]="consulta()!" [factor]="factor()" [generandoFactor]="generandoFactor()" [enviando]="enviando()" [errorDelServidor]="error()" (iniciarFactor)="iniciarFactor()" (completar)="completar($event)" (cambio)="error.set(null)" />
        }
        @case ('lista') {
          <ap-alerta tono="ok">{{ cierre() }}</ap-alerta>
          <p class="aviso">Para entrar usá tu clave y el código de 6 dígitos de tu aplicación de autenticación.</p>
          <ap-boton variante="primario" icono="flechaDerecha" [ancho]="true" (pulsado)="irAlIngreso()">Ir al ingreso</ap-boton>
        }
      }
    </ap-marco-de-acceso>
  `,
  styles: `
    :host { display: block; }
    .acciones { display: flex; }
    .enlace { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--brand-texto); }
    .enlace:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .aviso { margin: 0; font: var(--t-cuerpo); color: var(--text-2); }
  `,
})
export class PantallaDeInvitacion {
  private readonly api = crearInvitacion()
  private readonly router = inject(Router)
  private readonly injector = inject(Injector)
  private readonly marco = viewChild.required<MarcoDeAcceso>('marco')
  private readonly formulario = viewChild<FormularioDeInvitacion>('formulario')
  /** Solo en memoria: nunca se escribe en la URL, el almacenamiento ni un registro. */
  private readonly token: string | null

  protected readonly mensajeInvalida = MENSAJE_TOKEN_INVALIDO
  protected readonly estado = signal<Estado>('consultando')
  protected readonly consulta = signal<SalidaConsultaInvitacion | null>(null)
  protected readonly factor = signal<SalidaInicioEnrolamientoTotp | null>(null)
  protected readonly generandoFactor = signal(false)
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorDeCuenta | null>(null)
  protected readonly trazaId = signal<string | undefined>(undefined)
  private readonly hecho = signal<SalidaCompletarInvitacion | null>(null)
  protected readonly titulo = computed(() => TITULOS[this.estado()])
  protected readonly ceja = computed(() => CEJAS[this.consulta()?.alcance ?? 'ALTA'])
  protected readonly bajada = computed(() => {
    const nombres = this.consulta()?.nombres
    return this.estado() === 'formulario' && nombres ? `Hola, ${nombres}. Completá estos pasos para entrar al backoffice.` : undefined
  })
  protected readonly cierre = computed(() => {
    const h = this.hecho()
    if (!h) return ''
    const sesiones = h.sesionesCerradas > 0 ? ` Cerramos ${h.sesionesCerradas} ${h.sesionesCerradas === 1 ? 'sesión abierta' : 'sesiones abiertas'} de tu cuenta por seguridad.` : ''
    return CIERRE[h.alcance] + sesiones
  })

  constructor() {
    const fragmento = new URLSearchParams(inject(ActivatedRoute).snapshot.fragment ?? '')
    this.token = fragmento.get('token')?.trim() || null
    // Primero se saca de la URL (y del historial), después se usa: así no queda en la barra ni en «volver».
    if (fragmento.has('token')) void this.router.navigate([], { relativeTo: inject(ActivatedRoute), fragment: undefined, queryParamsHandling: 'preserve', replaceUrl: true })
    if (this.token) this.consultar()
    else this.pasarA('invalida')
  }

  protected consultar(): void {
    if (!this.token) return
    this.estado.set('consultando')
    this.api.consultar(this.token).subscribe({
      next: (c) => { this.consulta.set(c); this.pasarA('formulario') },
      error: (e: ErrorTraducido) => {
        const err = errorDeCuenta(e)
        this.trazaId.set(err.trazaId)
        // Solo `AP-CU09-04` dice que el enlace no sirve; cualquier otra cosa (red, 5xx, límite de intentos) es «no sabemos» y se reintenta.
        this.pasarA(err.tokenInvalido ? 'invalida' : 'sinRespuesta')
      },
    })
  }

  protected iniciarFactor(): void {
    if (!this.token || this.generandoFactor()) return
    this.generandoFactor.set(true)
    this.error.set(null)
    this.api.iniciarFactor(this.token).subscribe({
      next: (f) => { this.generandoFactor.set(false); this.factor.set(f) },
      error: (e: ErrorTraducido) => this.alFallar(e, () => this.generandoFactor.set(false)),
    })
  }

  protected completar(datos: DatosDeInvitacion): void {
    const factor = this.factor()
    if (!this.token || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)
    this.api.completar({ token: this.token, ...datos, ...(factor ? { factorId: factor.factorId } : {}) }).subscribe({
      next: (r) => { this.enviando.set(false); this.hecho.set(r); this.factor.set(null); this.pasarA('lista') },
      error: (e: ErrorTraducido) => this.alFallar(e, () => this.enviando.set(false)),
    })
  }

  protected irAlIngreso(): void {
    void this.router.navigateByUrl('/ingreso')
  }

  /** Un error del servidor nunca borra lo escrito: solo se muestra, en su campo si lo tiene. */
  private alFallar(e: ErrorTraducido, soltar: () => void): void {
    soltar()
    const err = errorDeCuenta(e)
    if (err.tokenInvalido) return this.pasarA('invalida')
    if (err.factorPerdido) this.factor.set(null)
    this.error.set(err)
    if (err.campo === 'codigo') this.formulario()?.limpiarCodigo()
  }

  /** Cambia de estado y lleva el foco al título: quien usa lector de pantalla se entera de que la pantalla cambió. */
  private pasarA(estado: Estado): void {
    this.estado.set(estado)
    afterNextRender(() => document.getElementById(this.marco().idTitulo)?.focus(), { injector: this.injector })
  }
}
