import { ChangeDetectionStrategy, Component, effect, output, signal, untracked, type Signal } from '@angular/core'
import { aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import type { OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ConfirmacionDeConciliacion } from './confirmacion-de-conciliacion'
import { crearConciliarCustodia, type EntradaConciliacionCustodia, type SalidaConciliacionCustodia } from './dominio/cu50-conciliaciones'
import { FormularioDeConciliacionVista } from './formulario-de-conciliacion-vista'
import { ReciboDeConciliacion } from './recibo-de-conciliacion'
import { textosCustodia } from './textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const DIA = /^\d{4}-\d{2}-\d{2}$/

/**
 * Contenedor Smart de CU-50 · Tesorería registra la conciliación de un día (`conciliarCustodia`).
 * Orquesta la validación de dominio, la llamada al backend con Idempotency-Key y el ciclo de vida
 * de la transacción, delegando el pintado exclusivamente a componentes presentacionales (Dumb):
 * - `FormularioDeConciliacionVista` para captura y validaciones visuales
 * - `ConfirmacionDeConciliacion` para el diálogo modal de confirmación
 * - `ReciboDeConciliacion` para el comprobante de éxito y detalle
 */
@Component({
  selector: 'ap-formulario-de-conciliacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConfirmacionDeConciliacion, FormularioDeConciliacionVista, ReciboDeConciliacion],
  template: `
    <h2>{{ t.formulario.titulo }}</h2>
    @if (recibo(); as r) {
      <ap-recibo-de-conciliacion [recibo]="r" (otra)="otra()" />
    } @else {
      <ap-formulario-de-conciliacion-vista
        [(cuenta)]="cuenta"
        [(fecha)]="fecha"
        [(moneda)]="moneda"
        [(custodia)]="custodia"
        [(transito)]="transito"
        [errores]="errores()"
        [deshabilitado]="enviando()"
        [aviso]="aviso()"
        [tonoAviso]="sinConfirmar() ? 'aviso' : 'error'"
        [opcionesMoneda]="monedas"
        (solicitarRevision)="revisar()"
      />
    }
    <ap-confirmacion-de-conciliacion
      [pendiente]="pendiente()"
      [cargando]="enviando()"
      (confirmar)="enviar(pendiente()!)"
      (cancelar)="pendiente.set(null)"
    />
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.1em; }
  `,
})
export class FormularioDeConciliacion {
  readonly registrada = output<SalidaConciliacionCustodia>()

  protected readonly t = textosCustodia
  private readonly conciliar = crearConciliarCustodia()

  protected readonly monedas: OpcionDeRadio[] = [
    { valor: 'BOB', texto: 'BOB' },
    { valor: 'USD', texto: 'USD' },
  ]
  protected readonly cuenta = signal('')
  protected readonly fecha = signal('')
  protected readonly moneda = signal<string | null>(null)
  protected readonly custodia = signal('')
  protected readonly transito = signal('')
  protected readonly errores = signal<Record<string, string>>({})
  protected readonly pendiente = signal<EntradaConciliacionCustodia | null>(null)
  protected readonly enviando = signal(false)
  protected readonly aviso = signal('')
  protected readonly sinConfirmar = signal(false)
  protected readonly recibo = signal<SalidaConciliacionCustodia | null>(null)

  constructor() {
    // Un error marcado se va en cuanto la persona corrige ESE campo: no queda pegado a un valor ya válido.
    const campos: [string, Signal<unknown>][] = [['cuenta', this.cuenta], ['fecha', this.fecha], ['moneda', this.moneda], ['custodia', this.custodia], ['transito', this.transito]]
    for (const [nombre, valor] of campos) {
      effect(() => {
        const ahora = valor()
        untracked(() => this.errores()[nombre] && ahora !== this.revisado[nombre] && this.errores.update((e) => Object.fromEntries(Object.entries(e).filter(([k]) => k !== nombre))))
      })
    }
  }
  /** Los valores tal como estaban al revisar: el error de un campo vale mientras su valor no cambie. */
  private revisado: Record<string, unknown> = {}

  protected revisar(): void {
    this.revisado = { cuenta: this.cuenta(), fecha: this.fecha(), moneda: this.moneda(), custodia: this.custodia(), transito: this.transito() }
    const cuenta = this.cuenta().trim().toLowerCase()
    const moneda = this.moneda() as EntradaConciliacionCustodia['saldoCustodia']['moneda'] | null
    const custodia = aCadenaDelContrato(this.custodia())
    const transito = aCadenaDelContrato(this.transito())
    const e: Record<string, string> = {}
    if (!UUID.test(cuenta)) e['cuenta'] = this.t.formulario.cuentaError
    if (!DIA.test(this.fecha())) e['fecha'] = this.t.formulario.fechaError
    if (!moneda) e['moneda'] = this.t.formulario.monedaError
    if (custodia === null) e['custodia'] = this.t.formulario.montoError
    if (transito === null) e['transito'] = this.t.formulario.montoError
    this.errores.set(e)
    if (Object.keys(e).length || !moneda || custodia === null || transito === null) return
    this.pendiente.set({
      cuentaCustodiaId: cuenta,
      fecha: this.fecha(),
      saldoCustodia: { monto: custodia, moneda },
      saldoEnTransito: { monto: transito, moneda },
    })
  }

  protected enviar(entrada: EntradaConciliacionCustodia): void {
    if (this.enviando()) return
    this.enviando.set(true)
    this.aviso.set('')
    this.sinConfirmar.set(false)
    this.conciliar(entrada).subscribe({
      next: (r) => {
        this.enviando.set(false)
        this.pendiente.set(null)
        this.recibo.set(r)
        this.registrada.emit(r)
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

  protected otra(): void {
    this.recibo.set(null)
    this.custodia.set('')
    this.transito.set('')
  }
}
