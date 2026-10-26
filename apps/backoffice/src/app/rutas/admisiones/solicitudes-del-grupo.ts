import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos, type CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { mensajeDe, type ErrorTraducido } from '../../nucleo/errores'
import { solicitudesDelGrupo, type SolicitudDeIngreso } from './dominio/cu68-admision'
import { textosAdmisiones } from './textos'

const t = textosAdmisiones
const ORIGEN = (s: SolicitudDeIngreso) => (s.porEnlace ? t.porEnlace : t.porPostulacion)

/**
 * CU-68 · Las solicitudes pendientes de un grupo (`listarSolicitudesDeIngreso`), con la anatomía única del listado. La
 * acción de fila es la de dominio: «Resolver» abre el expediente, donde se acepta o se rechaza con motivo. No hay
 * editar ni eliminar: una solicitud no se edita desde backoffice, se resuelve.
 */
@Component({
  selector: 'ap-solicitudes-del-grupo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, ChipEstado, Fecha, Icono, ListadoDeDatos],
  template: `
    <ap-listado [titulo]="t.listaTitulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" [identidad]="porId" [exportables]="exportables" nombreDeRegistros="solicitudes" [ayudaDeBusqueda]="t.listaAyuda" [textoVacio]="t.listaVacia" [explicacionVacio]="t.listaVaciaExplicacion" (reintentar)="solicitudes.reload()">
      <ng-template #celda let-s let-columna="columna">
        @switch (columna.clave) {
          @case ('nombreVisible') { <span class="quien"><ap-icono nombre="persona" tamano="s4" />{{ s.nombreVisible || t.sinNombre }}</span> @if (s.mensaje) { <span class="mensaje">{{ s.mensaje }}</span> } }
          @case ('porEnlace') { <ap-chip-estado [tono]="s.porEnlace ? 'info' : 'neutro'">{{ origen(s) }}</ap-chip-estado> }
          @case ('fechaSolicitud') { <ap-fecha [iso]="s.fechaSolicitud" /> }
          @case ('puntaje') { {{ s.puntaje ?? '—' }} }
          @default { {{ s[columna.clave] }} }
        }
      </ng-template>
      <ng-template #acciones let-s>
        <ap-acciones-de-fila [fila]="s.nombreVisible || s.solicitudId">
          <button type="button" apAccionDeFila [attr.aria-label]="t.resolverFila + ' ' + (s.nombreVisible || s.solicitudId)" (click)="resolver(s)"><ap-icono nombre="balanza" tamano="s4" /><span>{{ t.resolverFila }}</span></button>
        </ap-acciones-de-fila>
      </ng-template>
    </ap-listado>
  `,
  styles: `
    .quien { display: inline-flex; align-items: center; gap: var(--s2); font-weight: 600; color: var(--text); }
    .mensaje { display: block; margin-top: var(--s1); max-width: 40ch; color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; overflow-wrap: anywhere; }
  `,
})
export class SolicitudesDelGrupo {
  private readonly router = inject(Router)
  protected readonly t = t
  readonly grupoId = input.required<string>()
  protected readonly solicitudes = solicitudesDelGrupo(this.grupoId)
  private readonly filas = computed(() => (this.solicitudes.hasValue() ? (this.solicitudes.value() ?? []) : []))
  protected readonly carga = computed<CargaDeListado>(() => {
    if (this.solicitudes.isLoading()) return { tipo: 'cargando' }
    const e = this.solicitudes.error() as Partial<ErrorTraducido> | undefined
    return e ? { tipo: 'error', mensaje: e.mensaje ?? mensajeDe(undefined, 500), idPeticion: e.trazaId } : { tipo: 'lista' }
  })
  protected readonly porId = (s: SolicitudDeIngreso) => s.solicitudId
  protected readonly origen = ORIGEN
  protected readonly listado = crearListado<SolicitudDeIngreso>({
    filas: this.filas,
    camposDeBusqueda: ['nombreVisible', 'mensaje', 'solicitudId', ORIGEN],
    filtros: [
      { nombre: 'origen', etiqueta: 'Cómo la pidió', valor: ORIGEN },
      { nombre: 'cupos', etiqueta: 'Cupos pedidos', valor: (s) => String(s.cuposSolicitados) },
    ],
    ordenInicial: { clave: 'fechaSolicitud', sentido: 'asc' },
  })
  protected readonly columnas: Columna<SolicitudDeIngreso>[] = [
    { clave: 'nombreVisible', titulo: 'Quién la pide', ordenable: true },
    { clave: 'cuposSolicitados', titulo: 'Cupos', numerica: true, ordenable: true },
    { clave: 'puntaje', titulo: 'Puntaje', ordenable: true },
    { clave: 'porEnlace', titulo: 'Origen', ordenable: true },
    { clave: 'fechaSolicitud', titulo: 'Pedida', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<SolicitudDeIngreso>[] = [
    { clave: 'solicitudId', titulo: 'Solicitud' },
    { clave: 'nombreVisible', titulo: 'Quién la pide' },
    { clave: 'cuposSolicitados', titulo: 'Cupos' },
    { clave: 'puntaje', titulo: 'Puntaje' },
    { clave: 'porEnlace', titulo: 'Origen', valor: ORIGEN },
    { clave: 'mensaje', titulo: 'Mensaje' },
    { clave: 'fechaSolicitud', titulo: 'Pedida' },
  ]

  protected resolver(s: SolicitudDeIngreso): void {
    void this.router.navigate(['/admisiones', s.solicitudId])
  }
}
