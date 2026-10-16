import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { crearEjecutarCierreDiario, hoyEnLaPaz, type SalidaCierreDiario } from './dominio/cu51-cierres-diarios'
import { textosCierres } from './textos'

const T = textosCierres.formulario
const DIA = /^\d{4}-\d{2}-\d{2}$/

/**
 * CU-51 · Contabilidad cierra un día (`ejecutarCierreDiario`). Los prerrequisitos que el contrato declara se muestran
 * antes de pedir nada; la confirmación dice la consecuencia exacta; el botón se bloquea mientras envía y el comprobante
 * (con su identificador, y si el día ya estaba cerrado) aparece recién con el 2xx.
 */
@Component({
  selector: 'ap-formulario-de-cierre',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, Dialogo],
  template: `
    <h2>{{ T.titulo }}</h2>
    <ul class="requisitos">
      @for (r of T.requisitos; track r) { <li>{{ r }}</li> }
    </ul>
    @if (comprobante(); as c) {
      <ap-alerta [tono]="c.cuadrado ? 'ok' : 'aviso'" [titulo]="T.comprobante">
        @if (c.yaExistia) { <p>{{ T.yaExistia }}</p> }
        <dl class="comprobante">
          <dt>{{ T.cierreId }}</dt><dd class="id">{{ c.cierreId }}</dd>
          <dt>{{ t.columnas.fecha }}</dt><dd>{{ dia(c.fecha) }}</dd>
          <dt>{{ t.columnas.cuadrado }}</dt><dd>{{ c.cuadrado ? t.cuadrado : t.conObservaciones }}</dd>
          <dt>{{ T.excepcionesAbiertas }}</dt><dd>{{ c.excepcionesAbiertas }}</dd>
          @if (c.motivoDelDescuadre) { <dt>{{ T.motivo }}</dt><dd>{{ c.motivoDelDescuadre }}</dd> }
        </dl>
      </ap-alerta>
      <div><ap-boton (pulsado)="comprobante.set(null)">{{ T.otro }}</ap-boton></div>
    } @else {
      @if (aviso()) { <ap-alerta [tono]="sinConfirmar() ? 'aviso' : 'error'">{{ aviso() }}</ap-alerta> }
      <form (submit)="$event.preventDefault(); revisar()" novalidate>
        <ap-campo tipo="date" [etiqueta]="T.fecha" [error]="errorFecha()" [deshabilitado]="enviando()" [(valor)]="fecha" />
        <div><ap-boton variante="primario" tipo="submit" [deshabilitado]="enviando()">{{ T.revisar }}</ap-boton></div>
      </form>
    }
    @if (pendiente(); as p) {
      <ap-dialogo [titulo]="T.titulo" [textoDeConfirmar]="T.confirmar + ' ' + dia(p)" [abierto]="true" [cargando]="enviando()" (confirmar)="enviar(p)" (cancelar)="pendiente.set(null)">
        <p>{{ T.consecuencia(dia(p)) }}</p>
      </ap-dialogo>
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.1em; }
    .requisitos { margin: 0; padding-left: var(--s5); color: var(--text-2); display: grid; gap: var(--s1); }
    form { display: grid; gap: var(--s3); max-width: 24rem; }
    .comprobante { display: grid; grid-template-columns: auto 1fr; gap: var(--s1) var(--s3); margin: 0; } dd { margin: 0; } .id { overflow-wrap: anywhere; }
  `,
})
export class FormularioDeCierre {
  readonly cerrado = output<SalidaCierreDiario>()
  /** Inyectable en pruebas: el «hoy» de La Paz contra el que se valida el día. */
  hoy = (): string => hoyEnLaPaz()
  protected readonly T = T
  protected readonly t = textosCierres
  private readonly ejecutar = crearEjecutarCierreDiario()
  protected readonly fecha = signal('')
  protected readonly errorFecha = signal('')
  protected readonly pendiente = signal<string | null>(null)
  protected readonly enviando = signal(false)
  protected readonly aviso = signal('')
  protected readonly sinConfirmar = signal(false)
  protected readonly comprobante = signal<SalidaCierreDiario | null>(null)
  protected readonly dia = formatearDia

  protected revisar(): void {
    const fecha = this.fecha()
    // Comparación de cadenas AAAA-MM-DD: el orden lexicográfico es el cronológico.
    const valida = DIA.test(fecha) && fecha < this.hoy()
    this.errorFecha.set(valida ? '' : T.fechaError)
    if (valida) this.pendiente.set(fecha)
  }

  protected enviar(fecha: string): void {
    if (this.enviando()) return
    this.enviando.set(true)
    this.aviso.set('')
    this.sinConfirmar.set(false)
    this.ejecutar({ fecha }).subscribe({
      next: (c) => {
        this.enviando.set(false)
        this.pendiente.set(null)
        this.comprobante.set(c)
        this.cerrado.emit(c)
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(false)
        this.pendiente.set(null)
        const estado = e.estado ?? 0
        this.sinConfirmar.set(!!e.sinConexion || !!e.tiempoAgotado || estado === 0 || estado >= 500)
        this.aviso.set(this.sinConfirmar() ? T.sinConfirmar : (T.rechazos[e.codigo ?? ''] ?? e.mensaje))
      },
    })
  }
}
