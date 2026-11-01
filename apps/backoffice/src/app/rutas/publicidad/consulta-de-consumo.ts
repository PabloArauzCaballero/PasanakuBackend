import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { lotesDeAnunciantes } from './dominio/cu110-anunciantes'

export type PedidoDeConsumo = { cuenta: string; periodo: string; anunciante: string }

/** El mes cerrado anterior a hoy, `AAAA-MM`: es lo que se mira o se factura por omisión. */
export function mesAnterior(hoy = new Date()): string {
  const d = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/**
 * CU-113/114 · Elegir la cuenta publicitaria y el período, para el desempeño y para la liquidación. Antes cada pantalla
 * pedía pegar el identificador de la cuenta en un campo de texto libre; ahora la cuenta se elige por la razón social del
 * anunciante (`listarAnunciantes`) y el período con un selector de mes. Con `?cuenta=` (y opcionalmente `?periodo=`) en la URL (la acción de fila de
 * Anunciantes) llega elegida y se consulta sola, sobre el mes anterior.
 */
@Component({
  selector: 'ap-consulta-de-consumo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ListaDeOpciones],
  template: `
    <form class="consulta" (submit)="$event.preventDefault(); consultar()" novalidate>
      <ap-lista-de-opciones etiqueta="Cuenta publicitaria" [opciones]="cuentas()" [(valor)]="cuenta" [textoVacio]="anunciantes.carga().tipo === 'cargando' ? 'Cargando cuentas…' : 'Elegí una cuenta'" [error]="errorDeCuenta()" />
      <label class="mes">
        <span>Período</span>
        <input type="month" [value]="periodo()" [max]="maximo" (change)="periodo.set($any($event.target).value)" />
      </label>
      <ap-boton tipo="submit" variante="primario" icono="buscar">{{ textoAccion() }}</ap-boton>
    </form>
  `,
  styles: `
    .consulta { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); align-items: end; gap: var(--s3); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .mes { display: grid; gap: var(--s1); font: var(--t-cuerpo-chico); font-weight: 600; color: var(--text-2); }
    .mes input { min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); font: var(--t-cuerpo); }
    .mes input:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class ConsultaDeConsumo {
  protected readonly anunciantes = lotesDeAnunciantes()
  readonly textoAccion = input('Consultar')
  readonly consulta = output<PedidoDeConsumo>()
  protected readonly maximo = mesAnterior()
  private readonly enLaUrl = toSignal(inject(ActivatedRoute).queryParamMap)
  protected readonly cuenta = signal('')
  protected readonly periodo = signal(mesAnterior())
  private readonly intento = signal(false)
  protected readonly cuentas = computed(() =>
    this.anunciantes.filas().filter((a) => a.cuentaPublicitariaId).map((a) => ({ valor: a.cuentaPublicitariaId!, texto: a.razonSocialFacturacion })),
  )
  protected readonly errorDeCuenta = computed(() => {
    const c = this.anunciantes.carga()
    if (c.tipo === 'error') return `No pudimos traer las cuentas: ${c.mensaje}`
    return this.intento() && !this.cuenta() ? 'Elegí una cuenta.' : undefined
  })

  constructor() {
    this.anunciantes.recargar()
    // La cuenta de la URL se consulta en cuanto la lista de cuentas la confirma (y una sola vez).
    let consultada = false
    effect(() => {
      const pedida = this.enLaUrl()?.get('cuenta')
      if (!pedida || consultada || !this.cuentas().some((c) => c.valor === pedida)) return
      consultada = true
      const periodo = this.enLaUrl()?.get('periodo')
      untracked(() => {
        if (periodo && /^\d{4}-\d{2}$/.test(periodo)) this.periodo.set(periodo)
        this.cuenta.set(pedida)
        this.consultar()
      })
    })
  }

  consultar(): void {
    this.intento.set(true)
    if (!this.cuenta() || !/^\d{4}-\d{2}$/.test(this.periodo())) return
    const anunciante = this.cuentas().find((c) => c.valor === this.cuenta())?.texto ?? ''
    this.consulta.emit({ cuenta: this.cuenta(), periodo: this.periodo(), anunciante })
  }
}
