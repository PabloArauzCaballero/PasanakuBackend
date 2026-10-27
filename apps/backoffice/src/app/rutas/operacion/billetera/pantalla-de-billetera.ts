import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core'
import { DatePipe } from '@angular/common'
import { Router } from '@angular/router'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { PosicionesDeSaldo } from '@aportaya/ui/posiciones-de-saldo/posiciones-de-saldo'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { posicionesDe, saldoDe, saldoEnCero } from '../dominio/cu13-consultar-saldo'
import { textosOperacion } from '../textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * CU-13 · el saldo de una cuenta, para atender una consulta o un reclamo. `cuentaId` entra por la URL
 * (`withComponentInputBinding`). Se puede volver a pedir el saldo (nunca se muestra uno cacheado) y consultar otra
 * cuenta sin salir de la pantalla: navega a su URL, así el permiso de la ruta (`BILLETERA_VER_TERCEROS`) vuelve a decidir.
 */
@Component({
  selector: 'ap-pantalla-de-billetera',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, PosicionesDeSaldo, Alerta, Boton, CabeceraDePagina, Campo, DatePipe],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="operacion-billetera" eyebrow="Operación · CU-13" [titulo]="t.titulo" [descripcion]="t.proposito" icono="billetera">
        <ap-boton acciones icono="recarga" [cargando]="saldo.isLoading()" (pulsado)="saldo.reload()">{{ t.actualizar }}</ap-boton>
      </ap-cabecera-de-pagina>
      <div class="cuerpo">
        <section class="tarjeta" [attr.aria-label]="t.saldoDe">
          <p class="cuenta"><span>{{ t.saldoDe }}</span> <code>{{ cuentaId() }}</code></p>
          <ap-estado-de-pantalla data-tutorial-id="operacion-saldo" [recurso]="saldo" [vacio]="enCero" [mensajeVacio]="t.enCero" [etiquetaDeCarga]="t.cargando" (reintentar)="saldo.reload()">
            @if (saldo.hasValue() && saldo.value(); as s) {
              @if (lectura(s); as l) {
                @if (l.ok) {
                  <ap-posiciones-de-saldo [posiciones]="l.posiciones" perspectiva="soporte">{{ t.alcance }}</ap-posiciones-de-saldo>
                } @else {
                  <ap-alerta tono="error">{{ t.incoherente }}</ap-alerta>
                }
              }
              <p class="corte">{{ t.alCorteDe }} <time [attr.datetime]="s.alCorteDe">{{ s.alCorteDe | date: 'dd/MM/yyyy HH:mm' : '-0400' }} (La Paz)</time></p>
            }
          </ap-estado-de-pantalla>
        </section>
        <section class="tarjeta" [attr.aria-label]="t.otraCuenta">
          <h2>{{ t.otraCuenta }}</h2>
          <form (submit)="$event.preventDefault(); consultar()" novalidate>
            <ap-campo [etiqueta]="t.cuentaEtiqueta" [ayuda]="t.cuentaAyuda" [error]="error()" [(valor)]="otra" />
            <div><ap-boton variante="primario" tipo="submit" icono="buscar">{{ t.consultar }}</ap-boton></div>
          </form>
        </section>
      </div>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .cuerpo { display: grid; gap: var(--s5); }
    @media (min-width: 64rem) { .cuerpo { grid-template-columns: 2fr 1fr; align-items: start; } }
    .tarjeta { display: grid; gap: var(--s4); min-width: 0; padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    h2 { margin: 0; font: var(--t-titulo-3); color: var(--text); }
    form { display: grid; gap: var(--s3); }
    .cuenta { margin: 0; display: flex; flex-wrap: wrap; gap: var(--s2); color: var(--text-2); font: var(--t-cuerpo-chico); }
    code { font-family: var(--font-d); font-variant-numeric: tabular-nums; color: var(--text); overflow-wrap: anywhere; }
    .corte { margin: var(--s3) 0 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeBilletera {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
    // Al consultar otra cuenta la ruta es la misma: el campo se vacía cuando llega el nuevo `cuentaId`.
    effect(() => {
      this.cuentaId()
      this.otra.set('')
    })
  }
  private readonly router = inject(Router)
  readonly cuentaId = input.required<string>()
  protected readonly t = textosOperacion.billetera
  protected readonly saldo = saldoDe(this.cuentaId)
  protected readonly enCero = saldoEnCero
  protected readonly lectura = posicionesDe
  protected readonly otra = signal('')
  protected readonly error = signal<string | undefined>(undefined)

  protected consultar(): void {
    const id = this.otra().trim().toLowerCase()
    if (!UUID.test(id)) {
      this.error.set(this.t.cuentaError)
      return
    }
    this.error.set(undefined)
    void this.router.navigate(['/operacion/billetera', id])
  }
}
