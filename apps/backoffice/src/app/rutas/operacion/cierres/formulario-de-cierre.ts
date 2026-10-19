import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ComprobanteDeCierre } from './comprobante-de-cierre'
import { ConfirmacionDeCierre } from './confirmacion-de-cierre'
import { crearEjecutarCierreDiario, hoyEnLaPaz, type SalidaCierreDiario } from './dominio/cu51-cierres-diarios'
import { FormularioDeCierreVista } from './formulario-de-cierre-vista'
import { textosCierres } from './textos'

const DIA = /^\d{4}-\d{2}-\d{2}$/

/**
 * Contenedor Smart de CU-51 · Contabilidad cierra un día (`ejecutarCierreDiario`).
 * Orquesta la validación de fechas (contra el «hoy» de La Paz), la llamada al backend con Idempotency-Key
 * y el estado de la transacción, delegando el pintado exclusivamente a componentes presentacionales (Dumb):
 * - `FormularioDeCierreVista` para los requisitos, captura y botón de envío
 * - `ConfirmacionDeCierre` para el diálogo modal de confirmación con consecuencia explícita
 * - `ComprobanteDeCierre` para el comprobante de éxito y detalle
 */
@Component({
  selector: 'ap-formulario-de-cierre',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ComprobanteDeCierre, ConfirmacionDeCierre, FormularioDeCierreVista],
  template: `
    <h2>{{ t.formulario.titulo }}</h2>
    @if (comprobante(); as c) {
      <ap-comprobante-de-cierre [comprobante]="c" (otro)="comprobante.set(null)" />
    } @else {
      <ap-formulario-de-cierre-vista
        [(fecha)]="fecha"
        [errorFecha]="errorFecha()"
        [deshabilitado]="enviando()"
        [aviso]="aviso()"
        [tonoAviso]="sinConfirmar() ? 'aviso' : 'error'"
        (solicitarRevision)="revisar()"
      />
    }
    <ap-confirmacion-de-cierre
      [pendiente]="pendiente()"
      [cargando]="enviando()"
      (confirmar)="enviar($event)"
      (cancelar)="pendiente.set(null)"
    />
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.1em; }
  `,
})
export class FormularioDeCierre {
  readonly cerrado = output<SalidaCierreDiario>()
  /** Inyectable en pruebas: el «hoy» de La Paz contra el que se valida el día. */
  hoy = (): string => hoyEnLaPaz()

  protected readonly t = textosCierres
  private readonly ejecutar = crearEjecutarCierreDiario()

  protected readonly fecha = signal('')
  protected readonly errorFecha = signal('')
  protected readonly pendiente = signal<string | null>(null)
  protected readonly enviando = signal(false)
  protected readonly aviso = signal('')
  protected readonly sinConfirmar = signal(false)
  protected readonly comprobante = signal<SalidaCierreDiario | null>(null)

  protected revisar(): void {
    const fecha = this.fecha()
    // Comparación de cadenas AAAA-MM-DD: el orden lexicográfico es el cronológico.
    const valida = DIA.test(fecha) && fecha < this.hoy()
    this.errorFecha.set(valida ? '' : this.t.formulario.fechaError)
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
        this.aviso.set(
          this.sinConfirmar()
            ? this.t.formulario.sinConfirmar
            : (this.t.formulario.rechazos[e.codigo ?? ''] ?? e.mensaje),
        )
      },
    })
  }
}
