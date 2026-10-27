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
import { restauracionVencida } from '../dominio/datos-simulados'
import { PUERTO_RESPALDOS, type Respaldo } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const RESTAURACION = { vencida: 'Vencida', alDia: 'Al día' } as const

/** GATE: una restauración probada hace más de 30 días (o nunca) aparece marcada VENCIDA, se cuenta arriba y se puede filtrar. */
@Component({
  selector: 'ap-pantalla-respaldos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="nube">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="respaldos.isLoading()" (pulsado)="respaldos.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de respaldos" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por origen del respaldo o por fecha." [exportables]="exportables" nombreDeRegistros="respaldos" [textoVacio]="t.vacio" explicacionVacio="Cuando se tome el primer respaldo, aparece acá con su última restauración probada." (reintentar)="respaldos.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'ultimaRestauracionProbadaEl') {
            @if (vencida(f)) {
              <ap-chip-estado tono="error">{{ f.ultimaRestauracionProbadaEl ?? 'nunca probada' }} · VENCIDA</ap-chip-estado>
            } @else {
              <ap-chip-estado tono="ok">{{ f.ultimaRestauracionProbadaEl }}</ap-chip-estado>
            }
          } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaRespaldos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.respaldos
  private readonly puerto = inject(PUERTO_RESPALDOS)
  protected readonly respaldos = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.respaldos)
  private readonly filas = filasDe<Respaldo>(this.respaldos)
  protected readonly listado = crearListado<Respaldo>({
    filas: this.filas,
    camposDeBusqueda: ['origen', 'tomadoEl', 'ultimaRestauracionProbadaEl'],
    filtros: [
      { nombre: 'origen', etiqueta: 'Origen', valor: (r) => r.origen },
      { nombre: 'restauracion', etiqueta: 'Restauración', valor: (r) => (restauracionVencida(r) ? 'vencida' : 'alDia'), texto: (v) => RESTAURACION[v as keyof typeof RESTAURACION] ?? v },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Respaldos', valor: contar(f), icono: 'nube' },
      { etiqueta: 'Restauración al día', valor: contar(f, (r) => !restauracionVencida(r)), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Restauración vencida', valor: contar(f, (r) => restauracionVencida(r)), icono: 'alerta', tono: 'error', pista: 'Más de 30 días sin probar.' },
    ]
  })
  protected readonly columnas: Columna<Respaldo>[] = [
    { clave: 'origen', titulo: 'Origen', ordenable: true },
    { clave: 'tomadoEl', titulo: 'Tomado el', ordenable: true },
    { clave: 'ultimaRestauracionProbadaEl', titulo: 'Última restauración probada', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Respaldo>[] = [
    { clave: 'origen', titulo: 'Origen' },
    { clave: 'tomadoEl', titulo: 'Tomado el' },
    { clave: 'ultimaRestauracionProbadaEl', titulo: 'Última restauración probada', valor: (r) => r.ultimaRestauracionProbadaEl ?? 'nunca probada' },
    { clave: 'restauracion', titulo: 'Restauración', valor: (r) => (restauracionVencida(r) ? RESTAURACION.vencida : RESTAURACION.alDia) },
  ]
  protected vencida(r: Respaldo): boolean {
    return restauracionVencida(r)
  }
}
