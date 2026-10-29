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
import { PUERTO_INCIDENTES, type Incidente } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const SEVERIDAD: Record<Incidente['severidad'], string> = { sev1: 'SEV-1 · crítica', sev2: 'SEV-2 · alta', sev3: 'SEV-3 · menor' }
const ESTADO: Record<Incidente['estado'], string> = { abierto: 'Abierto', cerrado: 'Cerrado' }

/** Incidentes abiertos y cerrados. Solo lectura: abrir o cerrar un incidente no tiene contrato en esta fuente. */
@Component({
  selector: 'ap-pantalla-incidentes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="incidentes.isLoading()" (pulsado)="incidentes.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de incidentes" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por código, título, severidad, estado, etapa o sistema afectado." [exportables]="exportables" nombreDeRegistros="incidentes" [textoVacio]="t.vacio" explicacionVacio="Cuando se declare un incidente, aparece acá con su severidad." (reintentar)="incidentes.reload()">
        <ng-template #celda let-f let-c="columna">
          @switch (c.clave) {
            @case ('estado') { <ap-chip-estado [tono]="f.estado === 'abierto' ? 'aviso' : 'ok'">{{ estadoDe(f) }}</ap-chip-estado> }
            @case ('severidad') { <ap-chip-estado [tono]="f.severidad === 'sev1' ? 'error' : f.severidad === 'sev2' ? 'aviso' : 'neutro'">{{ severidadDe(f) }}</ap-chip-estado> }
            @default { {{ f[c.clave] ?? '—' }} }
          }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaIncidentes {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.incidentes
  private readonly puerto = inject(PUERTO_INCIDENTES)
  protected readonly incidentes = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.incidentes)
  private readonly filas = filasDe<Incidente>(this.incidentes)
  protected readonly listado = crearListado<Incidente>({
    filas: this.filas,
    camposDeBusqueda: ['titulo', (i) => SEVERIDAD[i.severidad], (i) => ESTADO[i.estado], (i) => i.etapa ?? '', (i) => i.sistema ?? '', 'abiertoEl'],
    filtros: [
      { nombre: 'severidad', etiqueta: 'Severidad', valor: (i) => i.severidad, texto: (v) => SEVERIDAD[v as Incidente['severidad']] ?? v },
      { nombre: 'estado', etiqueta: 'Estado', valor: (i) => i.estado, texto: (v) => ESTADO[v as Incidente['estado']] ?? v },
    ],
    porPagina: 10,
    // «20 oct 2026» ordenado como texto no respeta el calendario: se ordena por el instante cuando lo hay.
    valorParaOrden: (i, clave) => (clave === 'abiertoEl' ? (i.abiertoElIso ?? i.abiertoEl) : i[clave as keyof Incidente]),
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Incidentes', valor: contar(f), icono: 'advertencia' },
      { etiqueta: 'Abiertos', valor: contar(f, (i) => i.estado === 'abierto'), icono: 'bandera', tono: 'aviso' },
      { etiqueta: 'SEV-1 abiertos', valor: contar(f, (i) => i.estado === 'abierto' && i.severidad === 'sev1'), icono: 'alerta', tono: 'error' },
      { etiqueta: 'Cerrados', valor: contar(f, (i) => i.estado === 'cerrado'), icono: 'verificado', tono: 'ok' },
    ]
  })
  protected readonly columnas: Columna<Incidente>[] = [
    { clave: 'titulo', titulo: 'Incidente', ordenable: true },
    { clave: 'severidad', titulo: 'Severidad', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'etapa', titulo: 'Etapa', ordenable: true },
    { clave: 'sistema', titulo: 'Sistema afectado', ordenable: true },
    { clave: 'abiertoEl', titulo: 'Detectado el', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Incidente>[] = [
    { clave: 'titulo', titulo: 'Incidente' },
    { clave: 'severidad', titulo: 'Severidad', valor: (i) => SEVERIDAD[i.severidad] },
    { clave: 'estado', titulo: 'Estado', valor: (i) => ESTADO[i.estado] },
    { clave: 'abiertoEl', titulo: 'Abierto el' },
  ]
  protected estadoDe(i: Incidente): string {
    return ESTADO[i.estado]
  }
  protected severidadDe(i: Incidente): string {
    return SEVERIDAD[i.severidad]
  }
}
