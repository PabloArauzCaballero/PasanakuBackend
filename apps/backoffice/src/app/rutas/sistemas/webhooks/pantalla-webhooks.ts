import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { PUERTO_WEBHOOKS, type Webhook } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const DIRECCION: Record<Webhook['direccion'], string> = { entrante: 'Entrante', saliente: 'Saliente' }
const ESTADO: Record<Webhook['ultimoEstado'], string> = { ok: 'Entregado', error: 'Con error' }

/** Webhooks entrantes y salientes con su último estado de entrega. Solo lectura: reintentar una entrega no tiene contrato. */
@Component({
  selector: 'ap-pantalla-webhooks',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="rayo">
        <ap-boton acciones icono="recarga" [deshabilitado]="webhooks.isLoading()" (pulsado)="webhooks.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de webhooks" [cifras]="cifras()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por origen, dirección o estado de la última entrega." [exportables]="exportables" nombreDeRegistros="webhooks" [textoVacio]="t.vacio" explicacionVacio="Cuando se configure un webhook, aparece acá con su última entrega." (reintentar)="webhooks.reload()">
        <ng-template #celda let-f let-c="columna">
          @switch (c.clave) {
            @case ('ultimoEstado') { <ap-chip-estado [tono]="f.ultimoEstado === 'ok' ? 'ok' : 'error'">{{ estadoDe(f) }}</ap-chip-estado> }
            @case ('direccion') { {{ direccionDe(f) }} }
            @default { {{ f[c.clave] }} }
          }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaWebhooks {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.webhooks
  private readonly puerto = inject(PUERTO_WEBHOOKS)
  protected readonly webhooks = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.webhooks)
  private readonly filas = filasDe<Webhook>(this.webhooks)
  protected readonly listado = crearListado<Webhook>({
    filas: this.filas,
    camposDeBusqueda: ['origen', (w) => DIRECCION[w.direccion], (w) => ESTADO[w.ultimoEstado], 'ultimaEntregaEl'],
    filtros: [
      { nombre: 'direccion', etiqueta: 'Dirección', valor: (w) => w.direccion, texto: (v) => DIRECCION[v as Webhook['direccion']] ?? v },
      { nombre: 'estado', etiqueta: 'Último estado', valor: (w) => w.ultimoEstado, texto: (v) => ESTADO[v as Webhook['ultimoEstado']] ?? v },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Webhooks', valor: contar(f), icono: 'rayo' },
      { etiqueta: 'Entrantes', valor: contar(f, (w) => w.direccion === 'entrante'), icono: 'bandeja' },
      { etiqueta: 'Salientes', valor: contar(f, (w) => w.direccion === 'saliente'), icono: 'enviar' },
      { etiqueta: 'Con error', valor: contar(f, (w) => w.ultimoEstado === 'error'), icono: 'alerta', tono: 'error' },
    ]
  })
  protected readonly columnas: Columna<Webhook>[] = [
    { clave: 'origen', titulo: 'Origen', ordenable: true },
    { clave: 'direccion', titulo: 'Dirección', ordenable: true },
    { clave: 'ultimoEstado', titulo: 'Último estado', ordenable: true },
    { clave: 'ultimaEntregaEl', titulo: 'Última entrega', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Webhook>[] = [
    { clave: 'origen', titulo: 'Origen' },
    { clave: 'direccion', titulo: 'Dirección', valor: (w) => DIRECCION[w.direccion] },
    { clave: 'ultimoEstado', titulo: 'Último estado', valor: (w) => ESTADO[w.ultimoEstado] },
    { clave: 'ultimaEntregaEl', titulo: 'Última entrega' },
  ]
  protected estadoDe(w: Webhook): string {
    return ESTADO[w.ultimoEstado]
  }
  protected direccionDe(w: Webhook): string {
    return DIRECCION[w.direccion]
  }
}
