import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../../contabilidad/listado/listado-por-cursor'
import { cargadorDeDefiniciones, cargadorDeEjecuciones, type DefinicionDeReporteEnLista, type EjecucionDeReporteEnLista } from '../dominio/cu58-reportes'
import { textosConfiguracion } from '../textos'
import { TablaDeCatalogo, TablaDeEjecuciones } from './tablas-de-reportes'

const PESTANAS: Pestana[] = [
  { valor: 'catalogo', texto: 'Catálogo de reportes' },
  { valor: 'ejecuciones', texto: 'Historial de ejecuciones' },
]
const ESTADOS: Record<string, string> = textosConfiguracion.reportes.estados

/**
 * CU-58 · Catálogo de reportes e historial de ejecuciones, de solo lectura y en una tarjeta con pestañas. Los dos
 * listados se arman acá (contenedor) y las tablas solo dibujan: así el historial nombra cada ejecución con las páginas
 * del catálogo ya vistas, aunque la pestaña del catálogo no esté abierta; si la definición no se vio, queda el
 * identificador, nunca un nombre supuesto.
 *
 * Ejecutar un reporte (`ejecutarReporte`) y la exportación con huella y tope del servidor (`nucleo/exportador.ts`)
 * **no son parte de esta pantalla** y no se tocan: «Descargar CSV / JSON» de acá baja lo filtrado del listado en el
 * navegador, que es otra cosa.
 */
@Component({
  selector: 'ap-pantalla-de-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, Pestanas, TablaDeCatalogo, TablaDeEjecuciones],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Configuración · CU-58" [titulo]="t.titulo" [descripcion]="t.proposito" icono="documento" />
      <ap-alerta tono="info" titulo="Solo lectura">{{ comun.soloLectura }}</ap-alerta>
      <ap-pestanas etiqueta="Reportes" [pestanas]="PESTANAS" [(elegida)]="pestana" />
      <div role="tabpanel" [id]="'panel-' + pestana()" [attr.aria-labelledby]="'pestana-' + pestana()">
        @if (pestana() === 'catalogo') { <ap-tabla-de-catalogo [listado]="catalogo" /> } @else { <ap-tabla-de-ejecuciones [listado]="historial" [nombres]="nombres()" /> }
      </div>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s4); min-width: 0; }
    [role='tabpanel'] { min-width: 0; }
  `,
})
export class PantallaDeReportes {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosConfiguracion.reportes
  protected readonly comun = textosConfiguracion.comun
  protected readonly PESTANAS = PESTANAS
  private readonly filtroDeLaUrl = filtroInicialDeLaUrl(opcionesDe(ESTADOS))
  protected readonly pestana = signal(this.filtroDeLaUrl ? 'ejecuciones' : 'catalogo')
  /** Nombre por definición, de las páginas del catálogo que ya se vieron. */
  protected readonly nombres = signal<Readonly<Record<string, string>>>({})
  protected readonly catalogo = crearListadoPorCursor<DefinicionDeReporteEnLista>({
    cargador: this.recordandoNombres(cargadorDeDefiniciones()),
    camposDeBusqueda: ['nombre', 'tipo', 'descripcion'],
    filtros: [
      { nombre: 'tipo', etiqueta: 'Tipo', valor: (r) => r.tipo },
      { nombre: 'sensibilidad', etiqueta: 'Sensibilidad', valor: (r) => (r.contieneDatosSensibles ? 'SI' : 'NO'), opciones: [{ valor: 'SI', texto: this.t.sensible }, { valor: 'NO', texto: this.t.noSensible }] },
    ],
  })
  protected readonly historial = crearListadoPorCursor<EjecucionDeReporteEnLista>({
    cargador: cargadorDeEjecuciones(),
    camposDeBusqueda: [(r) => this.nombres()[r.definicionId] ?? r.definicionId, 'hashResultado'],
    servidor: { nombre: 'estado', etiqueta: textosConfiguracion.comun.estado, opciones: opcionesDe(ESTADOS), inicial: this.filtroDeLaUrl },
  })

  private recordandoNombres(cargador: CargadorDePagina<DefinicionDeReporteEnLista>): CargadorDePagina<DefinicionDeReporteEnLista> {
    return async (pedido) => {
      const pagina = await cargador(pedido)
      this.nombres.update((n) => ({ ...n, ...Object.fromEntries(pagina.filas.map((d) => [d.definicionId, d.nombre])) }))
      return pagina
    }
  }
}
