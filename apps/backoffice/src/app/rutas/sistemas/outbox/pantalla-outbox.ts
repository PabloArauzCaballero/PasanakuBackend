import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos, type CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { horaDeConsulta } from '../../operacion/estado/dominio/estado-del-sistema'
import { cargaDe } from '../compartido/carga'
import { CifrasDeSistemas, type Cifra } from '../compartido/cifras'
import { filasDeOutbox, recursoOutbox, type FilaDeOutbox } from '../dominio/outbox'
import { textosSistemas } from '../textos'

/**
 * Sistemas › Outbox por servicio, con lo que publica el relevo de cada uno (ver `dominio/outbox.ts`). De solo lectura:
 * los fallidos los rescata el propio relevo cada cinco minutos (`Relevo.rescatarFallidos`), no esta vista.
 */
@Component({
  selector: 'ap-pantalla-outbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="bandeja">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="outbox.isLoading()" (pulsado)="outbox.reload()">Volver a consultar</ap-boton>
      </ap-cabecera-de-pagina>
      @if (consultadoA(); as hora) { <p class="nota" role="status">Consultado a las {{ hora }} (hora de La Paz).</p> }
      <ap-cifras-de-sistemas etiqueta="Resumen del outbox" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por servicio o estado." [exportables]="exportables" nombreDeRegistros="servicios" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" (reintentar)="outbox.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'estado') { <ap-chip-estado [tono]="f.tono">{{ f.estado }}</ap-chip-estado> } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } .nota { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaOutbox {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.outbox
  protected readonly outbox = recursoOutbox()
  private readonly salida = computed(() => (this.outbox.hasValue() ? this.outbox.value() : undefined))
  /** Prometheus no respondió: no se sabe. Se dice como «sin fuente», no como lista vacía. */
  protected readonly carga = computed<CargaDeListado>(() =>
    this.salida()?.fuenteDisponible === false ? { tipo: 'sin-fuente', mensaje: this.t.sinFuente } : this.cargaHttp(),
  )
  private readonly cargaHttp = cargaDe(this.outbox)
  private readonly filas = computed(() => (this.salida()?.fuenteDisponible ? filasDeOutbox(this.salida()!) : []))
  protected readonly consultadoA = computed(() => (this.salida()?.fuenteDisponible ? horaDeConsulta(this.salida()!.consultadoEn) : null))
  protected readonly listado = crearListado<FilaDeOutbox>({
    filas: this.filas,
    camposDeBusqueda: ['servicio', 'estado'],
    filtros: [{ nombre: 'estado', etiqueta: 'Estado', valor: (f) => f.estado }],
    porPagina: 20,
    valorParaOrden: (f, clave) => (clave === 'pendientes' ? f.pendientesN : clave === 'fallidos' ? f.fallidosN : f[clave as keyof FilaDeOutbox]),
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    const suma = (v: (x: FilaDeOutbox) => number) => f.reduce((a, x) => a + v(x), 0).toLocaleString('es-BO')
    return [
      { etiqueta: 'Servicios', valor: String(f.length), icono: 'servidor' },
      { etiqueta: 'Pendientes', valor: suma((x) => x.pendientesN), icono: 'reloj', tono: 'aviso' },
      { etiqueta: 'Fallidos', valor: suma((x) => x.fallidosN), icono: 'alerta', tono: 'error' },
    ]
  })
  protected readonly columnas: Columna<FilaDeOutbox>[] = [
    { clave: 'servicio', titulo: 'Servicio', ordenable: true, mono: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'pendientes', titulo: 'Pendientes', ordenable: true, numerica: true },
    { clave: 'fallidos', titulo: 'Fallidos', ordenable: true, numerica: true },
    { clave: 'esperaMasLarga', titulo: 'Espera más larga' },
    { clave: 'publicados24h', titulo: 'Publicados 24 h', numerica: true },
    { clave: 'errores24h', titulo: 'Errores al publicar 24 h', numerica: true },
  ]
  protected readonly exportables: ColumnaExportable<FilaDeOutbox>[] = this.columnas.map((c) => ({ clave: c.clave as string, titulo: c.titulo }))
}
