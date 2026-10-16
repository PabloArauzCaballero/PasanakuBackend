import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Monto } from '@aportaya/ui/monto/monto'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { crearConciliarCustodia, type EntradaConciliacionCustodia, type SalidaConciliacionCustodia } from './dominio/cu50-conciliaciones'
import { textosCustodia } from './textos'

const T = textosCustodia.formulario
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const DIA = /^\d{4}-\d{2}-\d{2}$/

/**
 * CU-50 · Tesorería registra la conciliación de un día (`conciliarCustodia`). Dos pasos: el formulario se valida y
 * recién entonces se abre la confirmación con la consecuencia exacta (cuenta, día, extracto y tránsito); el envío
 * bloquea el botón, conserva lo escrito ante cualquier fallo y el recibo (con su identificador) aparece con el 201.
 * Los montos viajan como cadena del contrato (`aCadenaDelContrato`), nunca como número.
 */
@Component({
  selector: 'ap-formulario-de-conciliacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, Dialogo, GrupoRadio, Monto],
  template: `
    <h2>{{ T.titulo }}</h2>
    @if (recibo(); as r) {
      <ap-alerta [tono]="r.estado === 'CUADRADA' ? 'ok' : 'aviso'" [titulo]="T.recibo">
        <dl class="recibo">
          <dt>{{ T.reciboId }}</dt><dd class="id">{{ r.conciliacionId }}</dd>
          <dt>{{ t.columnas.estado }}</dt><dd>{{ t.estados[r.estado] }}</dd>
          <dt>{{ t.columnas.saldoEmitido }}</dt><dd><ap-monto [monto]="r.saldoEmitido.monto" [moneda]="r.saldoEmitido.moneda" /></dd>
          <dt>{{ t.columnas.diferencia }}</dt><dd><strong>{{ r.estado === 'CUADRADA' ? t.cuadra : t.descuadre }}:</strong> <ap-monto [monto]="r.diferencia.monto" [moneda]="r.diferencia.moneda" /></dd>
          <dt>{{ t.columnas.ratioCobertura }}</dt><dd>{{ r.ratioCobertura }} · {{ r.cumpleEncaje ? t.cumpleEncaje : t.noCumpleEncaje }}</dd>
        </dl>
      </ap-alerta>
      <div><ap-boton (pulsado)="otra()">{{ T.otra }}</ap-boton></div>
    } @else {
      @if (aviso()) { <ap-alerta [tono]="sinConfirmar() ? 'aviso' : 'error'">{{ aviso() }}</ap-alerta> }
      <form (submit)="$event.preventDefault(); revisar()" novalidate>
        <ap-campo [etiqueta]="T.cuenta" [ayuda]="T.cuentaAyuda" [error]="errores()['cuenta']" [deshabilitado]="enviando()" [(valor)]="cuenta" />
        <ap-campo tipo="date" [etiqueta]="T.fecha" [error]="errores()['fecha']" [deshabilitado]="enviando()" [(valor)]="fecha" />
        <fieldset class="bloque" [disabled]="enviando()"><ap-grupo-radio [etiqueta]="T.moneda" [opciones]="monedas" [(elegido)]="moneda" /></fieldset>
        @if (errores()['moneda']) { <p class="error-campo" role="alert">{{ errores()['moneda'] }}</p> }
        <ap-campo modoDeEntrada="decimal" marcador="0,00" [prefijo]="moneda() ?? ''" [etiqueta]="T.saldoCustodia" [error]="errores()['custodia']" [deshabilitado]="enviando()" [(valor)]="custodia" />
        <ap-campo modoDeEntrada="decimal" marcador="0,00" [prefijo]="moneda() ?? ''" [etiqueta]="T.saldoEnTransito" [error]="errores()['transito']" [deshabilitado]="enviando()" [(valor)]="transito" />
        <div><ap-boton variante="primario" tipo="submit" [deshabilitado]="enviando()">{{ T.revisar }}</ap-boton></div>
      </form>
    }
    @if (pendiente(); as p) {
      <ap-dialogo [titulo]="T.titulo" [textoDeConfirmar]="T.confirmar" [abierto]="true" [cargando]="enviando()" (confirmar)="enviar(p)" (cancelar)="pendiente.set(null)">
        <p>{{ T.consecuencia(p.cuentaCustodiaId, dia(p.fecha)) }}</p>
        <p>{{ T.extracto }}: <ap-monto [monto]="p.saldoCustodia.monto" [moneda]="p.saldoCustodia.moneda" /> · {{ T.transito }}: <ap-monto [monto]="p.saldoEnTransito.monto" [moneda]="p.saldoEnTransito.moneda" /></p>
      </ap-dialogo>
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.1em; }
    form { display: grid; gap: var(--s3); max-width: 32rem; }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    .error-campo { margin: 0; color: var(--err-texto); font-size: .875rem; }
    .recibo { display: grid; grid-template-columns: auto 1fr; gap: var(--s1) var(--s3); margin: 0; } dd { margin: 0; } .id { overflow-wrap: anywhere; }
  `,
})
export class FormularioDeConciliacion {
  readonly registrada = output<SalidaConciliacionCustodia>()
  protected readonly T = T
  protected readonly t = textosCustodia
  private readonly conciliar = crearConciliarCustodia()
  protected readonly monedas: OpcionDeRadio[] = [{ valor: 'BOB', texto: 'BOB' }, { valor: 'USD', texto: 'USD' }]
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
  protected readonly dia = formatearDia

  protected revisar(): void {
    const cuenta = this.cuenta().trim().toLowerCase()
    const moneda = this.moneda() as EntradaConciliacionCustodia['saldoCustodia']['moneda'] | null
    const custodia = aCadenaDelContrato(this.custodia())
    const transito = aCadenaDelContrato(this.transito())
    const e: Record<string, string> = {}
    if (!UUID.test(cuenta)) e['cuenta'] = T.cuentaError
    if (!DIA.test(this.fecha())) e['fecha'] = T.fechaError
    if (!moneda) e['moneda'] = T.monedaError
    if (custodia === null) e['custodia'] = T.montoError
    if (transito === null) e['transito'] = T.montoError
    this.errores.set(e)
    if (Object.keys(e).length || !moneda || custodia === null || transito === null) return
    this.pendiente.set({ cuentaCustodiaId: cuenta, fecha: this.fecha(), saldoCustodia: { monto: custodia, moneda }, saldoEnTransito: { monto: transito, moneda } })
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
        this.aviso.set(this.sinConfirmar() ? T.sinConfirmar : (T.rechazos[e.codigo ?? ''] ?? e.mensaje))
      },
    })
  }

  protected otra(): void {
    this.recibo.set(null)
    this.custodia.set('')
    this.transito.set('')
  }
}
