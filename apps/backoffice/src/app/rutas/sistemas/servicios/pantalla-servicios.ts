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
import { PUERTO_SERVICIOS, type EstadoServicio } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const TONO: Record<EstadoServicio['estado'], 'ok' | 'aviso' | 'error'> = { operativo: 'ok', degradado: 'aviso', caido: 'error' }
const ESTADO: Record<EstadoServicio['estado'], string> = { operativo: 'Operativo', degradado: 'Degradado', caido: 'Caído' }

/**
 * Estado de servicios y SLO con presupuesto de error. Inyecta `PUERTO_SERVICIOS` (quién lo resuelve —simulado o
 * «no disponible»— lo decide `sistemas.routes.ts`) y pinta la colección con el kit de listado: buscador, filtro por
 * estado, CSV/JSON de lo filtrado y paginación. Es de solo lectura: no hay acciones de fila sin contrato detrás.
 */
@Component({
  selector: 'ap-pantalla-servicios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="sistemas-servicios" eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="servidor">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="servicios.isLoading()" (pulsado)="servicios.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de servicios" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado data-tutorial-id="sistemas-servicios-estado" [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por nombre del servicio, estado o fecha de la última interrupción." [exportables]="exportables" nombreDeRegistros="servicios" [textoVacio]="t.vacio" explicacionVacio="Cuando un servicio publique su SLO, aparece acá con su presupuesto de error." (reintentar)="servicios.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'estado') { <ap-chip-estado [tono]="tonoDe(f)">{{ estadoDe(f) }}</ap-chip-estado> } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaServicios {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.servicios
  private readonly puerto = inject(PUERTO_SERVICIOS)
  protected readonly servicios = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.servicios)
  private readonly filas = filasDe<EstadoServicio>(this.servicios)
  protected readonly listado = crearListado<EstadoServicio>({
    filas: this.filas,
    camposDeBusqueda: ['nombre', (s) => ESTADO[s.estado], 'ultimaInterrupcion'],
    filtros: [{ nombre: 'estado', etiqueta: 'Estado', valor: (s) => s.estado, texto: (v) => ESTADO[v as EstadoServicio['estado']] ?? v }],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Servicios', valor: contar(f), icono: 'servidor' },
      { etiqueta: 'Operativos', valor: contar(f, (s) => s.estado === 'operativo'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Degradados', valor: contar(f, (s) => s.estado === 'degradado'), icono: 'advertencia', tono: 'aviso' },
      { etiqueta: 'Caídos', valor: contar(f, (s) => s.estado === 'caido'), icono: 'alerta', tono: 'error' },
    ]
  })
  protected readonly columnas: Columna<EstadoServicio>[] = [
    { clave: 'nombre', titulo: 'Servicio', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'disponibilidad30d', titulo: 'Disponibilidad 30d', numerica: true },
    { clave: 'presupuestoErrorRestante', titulo: 'Presupuesto de error restante', numerica: true },
    { clave: 'ultimaInterrupcion', titulo: 'Última interrupción', ordenable: true },
  ]
  protected tonoDe(s: EstadoServicio) {
    return TONO[s.estado]
  }
  protected estadoDe(s: EstadoServicio): string {
    return ESTADO[s.estado]
  }
  protected readonly exportables: ColumnaExportable<EstadoServicio>[] = [
    { clave: 'nombre', titulo: 'Servicio' },
    { clave: 'estado', titulo: 'Estado', valor: (s) => ESTADO[s.estado] },
    { clave: 'disponibilidad30d', titulo: 'Disponibilidad 30d' },
    { clave: 'presupuestoErrorRestante', titulo: 'Presupuesto de error restante' },
    { clave: 'ultimaInterrupcion', titulo: 'Última interrupción' },
  ]
}
