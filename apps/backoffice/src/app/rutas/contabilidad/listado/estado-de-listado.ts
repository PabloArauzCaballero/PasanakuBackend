import { ChangeDetectionStrategy, Component, input, signal, type Signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { mensajeDe, type ErrorTraducido } from '../../../nucleo/errores'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'

/**
 * **Los estados vacío y error de una bandeja sobre `TablaDeDatosVirtualizada`.** La tabla del shell (`nucleo/tabla`,
 * fuera de este lote) solo dice «Cargando…»: no dibuja vacío ni error, y un cargador que rechaza deja una promesa sin
 * atender. Este envoltorio observa el cargador SIN cambiar lo que pide: si falla, guarda el error ya traducido por
 * `erroresInterceptor` (nunca el cuerpo crudo del backend) y le devuelve a la tabla una página vacía; si la primera
 * página llega sin filas, lo marca como vacío. La pantalla pinta `<ap-estado-de-listado>` al lado de la tabla.
 *
 * Lo usan las bandejas de Contabilidad (CU-101 a CU-105) y de Publicidad (CU-110 a CU-112). **Pedido al dueño de
 * `nucleo/tabla`**: que la tabla resuelva estos dos estados y este archivo desaparezca.
 *
 * `textos` traduce los códigos `AP-…` que el catálogo de `nucleo/errores.ts` todavía no conoce (los de filtro de
 * listado); cuando el catálogo los tenga, sobran.
 */
export type ListadoObservado<T> = {
  readonly cargador: CargadorDePagina<T>
  readonly error: Signal<ErrorTraducido | null>
  readonly vacio: Signal<boolean>
}

export function observarListado<T>(cargador: CargadorDePagina<T>, textos: Readonly<Record<string, string>> = {}): ListadoObservado<T> {
  const error = signal<ErrorTraducido | null>(null)
  const vacio = signal(false)
  const observado: CargadorDePagina<T> = async (pedido) => {
    try {
      const pagina = await cargador(pedido)
      error.set(null)
      vacio.set(pedido.pagina <= 1 && pagina.filas.length === 0)
      return pagina
    } catch (e) {
      error.set(traducir(e, textos))
      vacio.set(false)
      return { filas: [], total: 0 }
    }
  }
  return { cargador: observado, error: error.asReadonly(), vacio: vacio.asReadonly() }
}

function traducir(e: unknown, textos: Readonly<Record<string, string>>): ErrorTraducido {
  if (e && typeof e === 'object' && 'mensaje' in e) {
    const traducido = e as ErrorTraducido
    const propio = traducido.codigo ? textos[traducido.codigo] : undefined
    return propio ? { ...traducido, mensaje: propio } : traducido
  }
  return { mensaje: mensajeDe(undefined, 500) }
}

@Component({
  selector: 'ap-estado-de-listado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, EstadoVacio],
  template: `
    @if (error(); as e) {
      <ap-alerta tono="error" [titulo]="tituloError()">
        {{ e.mensaje }}
        @if (e.trazaId) { <span class="traza">Código de seguimiento: {{ e.trazaId }}</span> }
      </ap-alerta>
    } @else if (vacio()) {
      <div role="status"><ap-estado-vacio [motivo]="filtrado() ? 'porFiltro' : 'sinDatos'" [titulo]="mensajeVacio()" explicacion="" /></div>
    }
  `,
  styles: `
    :host { display: block; }
    .traza { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .85em; }
  `,
})
export class EstadoDeListado {
  readonly error = input<ErrorTraducido | null>(null)
  readonly vacio = input(false)
  readonly mensajeVacio = input.required<string>()
  readonly filtrado = input(false)
  readonly tituloError = input('No se pudo cargar la lista')
}
