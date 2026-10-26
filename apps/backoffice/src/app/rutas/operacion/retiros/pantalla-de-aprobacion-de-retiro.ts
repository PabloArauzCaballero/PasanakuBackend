import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Monto } from '@aportaya/ui/monto/monto'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { senalDeRed } from '../../../nucleo/senal-de-red'
import { Sesion } from '../../../nucleo/sesion'
import {
  crearAprobarRetiro,
  MOTIVO_MAXIMO_RETIRO,
  type DesenlaceDeRetiro,
  type EntradaAprobacionRetiro,
  type SalidaAprobacionRetiro,
} from './dominio/cu11-aprobacion-de-retiro'
import type { RetiroEnRevision } from './dominio/cu11-retiros-en-revision'
import { textosRetiros } from './textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

type Envio = 'inactivo' | 'enviando' | 'confirmado' | 'sinConfirmar' | 'rechazado'

/**
 * CU-11 · la segunda firma de un retiro (`aprobarRetiro`, permiso `RETIRO_APROBAR`).
 */
@Component({
  selector: 'ap-pantalla-de-aprobacion-de-retiro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, GrupoRadio, Monto],
  template: `
    <section class="pantalla" aria-labelledby="titulo-retiro">
      <ap-cabecera-de-pagina id="titulo-retiro" [titulo]="t.titulo" [descripcion]="t.proposito" icono="retiro" />
      <p class="aviso">{{ t.sinListado }}</p>
      @if (resumen; as r) {
        <dl class="resumen" [attr.aria-label]="t.resumen">
          <dt>{{ t.bandeja.columnas.solicitado }}</dt><dd><ap-monto [monto]="r.solicitado.monto" [moneda]="r.solicitado.moneda" /></dd>
          <dt>{{ t.bandeja.columnas.costo }}</dt><dd><ap-monto [monto]="r.costo.monto" [moneda]="r.costo.moneda" /></dd>
          <dt>{{ t.bandeja.columnas.neto }}</dt><dd><ap-monto [monto]="r.neto.monto" [moneda]="r.neto.moneda" /></dd>
        </dl>
      }

      @if (resultado(); as r) {
        <ap-alerta [tono]="r.estado === 'AUTORIZADA' ? 'ok' : 'info'">
          {{ r.estado === 'AUTORIZADA' ? t.confirmadaAutorizada : t.confirmadaRechazada }}
          <span class="traza">{{ t.aprobadaPor }} {{ r.aprobadaPor }}</span>
        </ap-alerta>
        <div class="acciones">
          <ap-boton variante="secundario" [icono]="ordenId() ? 'flechaIzquierda' : 'mas'" (pulsado)="ordenId() ? volver() : otra()">{{ ordenId() ? t.volverABandeja : t.otra }}</ap-boton>
        </div>
      } @else {
        @if (!puedeAprobar()) {
          <ap-alerta tono="info">{{ t.sinPermiso }}</ap-alerta>
        }
        @if (sinRed()) {
          <ap-alerta tono="aviso">{{ t.sinConexion }}</ap-alerta>
        }
        @if (envio() === 'sinConfirmar') {
          <ap-alerta tono="aviso" [titulo]="t.sinConfirmar">{{ t.sinConfirmarAyuda }}</ap-alerta>
        }
        @if (avisoDelServidor(); as mensaje) {
          <ap-alerta tono="error">{{ mensaje }}</ap-alerta>
        }

        <form (submit)="$event.preventDefault(); enviar()" novalidate>
          <ap-campo [etiqueta]="t.ordenEtiqueta" [ayuda]="t.ordenAyuda" [error]="errorOrden()" [deshabilitado]="bloqueado() || !!ordenId()" [(valor)]="ordenEscrita" />
          <fieldset class="bloque" [disabled]="bloqueado()">
            <ap-grupo-radio [etiqueta]="t.desenlaceEtiqueta" [opciones]="opciones" [(elegido)]="desenlace" />
          </fieldset>
          @if (errorDesenlace()) {
            <p class="error-campo" role="alert">{{ errorDesenlace() }}</p>
          }
          @if (desenlace() === 'AUTORIZADA') {
            <p class="advertencia">{{ t.advertenciaAutorizar }}</p>
          } @else if (desenlace() === 'RECHAZADA') {
            <p class="advertencia">{{ t.advertenciaRechazar }}</p>
          }
          <ap-campo [etiqueta]="t.motivoEtiqueta" [ayuda]="t.motivoAyuda" [error]="errorMotivo()" [deshabilitado]="bloqueado()" [(valor)]="motivo" />
          <div class="acciones">
            <ap-boton variante="primario" tipo="submit" icono="enviar" [cargando]="envio() === 'enviando'" [deshabilitado]="bloqueado()">
              {{ envio() === 'sinConfirmar' ? t.reintentar : envio() === 'enviando' ? t.resolviendo : t.resolver }}
            </ap-boton>
          </div>
        </form>
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    .pantalla > * { margin: 0; }
    .aviso, .advertencia { color: var(--text-2); }
    .traza { display: block; margin-top: var(--s1); font-size: .875rem; overflow-wrap: anywhere; }
    form { display: flex; flex-direction: column; gap: var(--s4); }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    .error-campo { margin: 0; color: var(--err-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; }
    .resumen { display: grid; grid-template-columns: auto 1fr; gap: var(--s1) var(--s3); margin: 0; } dd { margin: 0; }
  `,
})
export class PantallaDeAprobacionDeRetiro {
  protected readonly t = textosRetiros
  private readonly sesion = inject(Sesion)
  private readonly aprobar = crearAprobarRetiro()
  private readonly router = inject(Router)
  /** `retiros/:ordenId` (enlace profundo o fila de la bandeja): la orden llega fijada por la ruta. */
  readonly ordenId = input<string | undefined>(undefined)
  /** La fila de la bandeja, si se llegó desde ahí; en un enlace profundo no hay (el contrato no consulta la orden). */
  protected readonly resumen = (this.router.currentNavigation()?.extras.state?.['retiro'] ?? null) as RetiroEnRevision | null

  protected readonly opciones: OpcionDeRadio[] = [
    { valor: 'AUTORIZADA', texto: this.t.autorizar },
    { valor: 'RECHAZADA', texto: this.t.rechazar },
  ]
  protected readonly ordenEscrita = signal('')
  protected readonly desenlace = signal<string | null>(null)
  protected readonly motivo = signal('')
  protected readonly errorOrden = signal('')
  protected readonly errorDesenlace = signal('')
  protected readonly errorMotivo = signal('')
  protected readonly envio = signal<Envio>('inactivo')
  protected readonly avisoDelServidor = signal('')
  protected readonly resultado = signal<SalidaAprobacionRetiro | null>(null)
  protected readonly sinRed = senalDeRed()
  protected readonly puedeAprobar = computed(() => this.sesion.puede('RETIRO_APROBAR'))
  protected readonly bloqueado = computed(() => this.envio() === 'enviando')

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
    effect(() => {
      const orden = this.ordenId()
      if (orden) this.ordenEscrita.set(orden)
    })
  }

  protected volver(): void {
    void this.router.navigateByUrl('/operacion/retiros')
  }

  protected enviar(): void {
    if (this.bloqueado()) return
    const orden = this.ordenEscrita().trim().toLowerCase()
    const desenlace = this.desenlace() as DesenlaceDeRetiro | null
    const motivo = this.motivo().trim()
    this.errorOrden.set(UUID.test(orden) ? '' : this.t.ordenError)
    this.errorDesenlace.set(desenlace ? '' : this.t.desenlaceRequerido)
    this.errorMotivo.set(motivo.length > MOTIVO_MAXIMO_RETIRO ? this.t.motivoLargo : '')
    if (this.errorOrden() || !desenlace || this.errorMotivo()) return

    // Sin `motivo` vacío: el contrato lo admite ausente y el servidor rechaza campos que no declara.
    const entrada: EntradaAprobacionRetiro = motivo ? { desenlace, motivo } : { desenlace }
    this.envio.set('enviando')
    this.avisoDelServidor.set('')
    this.aprobar(orden, entrada).subscribe({
      next: (r) => {
        this.resultado.set(r)
        this.envio.set('confirmado')
      },
      error: (e: ErrorTraducido) => this.alFallar(e),
    })
  }

  /** Sin respuesta NO es «no pasó»: pudo registrarse. Todo lo escrito se conserva. */
  private alFallar(e: ErrorTraducido): void {
    const estado = e.estado ?? 0
    if (e.sinConexion || e.tiempoAgotado || estado === 0 || estado >= 500) {
      this.envio.set('sinConfirmar')
      return
    }
    this.envio.set('rechazado')
    this.avisoDelServidor.set(this.mensajeDeRechazo(e))
  }

  private mensajeDeRechazo(e: ErrorTraducido): string {
    if (e.codigo === 'AP-CU11-10') return this.t.autoaprobacion
    if (e.estado === 409 || e.codigo === 'AP-CU11-11') return this.t.yaResuelta
    if (e.estado === 404) return this.t.noExiste
    return e.mensaje
  }

  protected otra(): void {
    this.resultado.set(null)
    this.envio.set('inactivo')
    this.ordenEscrita.set('')
    this.desenlace.set(null)
    this.motivo.set('')
  }
}
