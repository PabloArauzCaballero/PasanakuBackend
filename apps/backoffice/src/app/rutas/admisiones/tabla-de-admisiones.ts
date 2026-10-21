import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { SolicitudDeAdmision } from './dominio/cu68-admision'
import { textosAdmisiones } from './textos'

type FilaAdmision = SolicitudDeAdmision & { acciones?: string }

@Component({
  selector: 'ap-tabla-de-admisiones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ChipEstado, RouterLink, TablaDeDatos],
  template: `
    <ap-tabla-de-datos
      [titulo]="t.bandejaTitulo"
      [columnas]="columnas"
      [filas]="filas()"
      [identidad]="identidad"
      densidad="compacta"
      [cabecera]="true"
      [textoRegistros]="t.registros"
      [totalDeRegistros]="total()"
    >
      <ng-template #celda let-fila let-columna="columna">
        @switch (columna.clave) {
          @case ('solicitudId') {
            <a [routerLink]="['/admisiones', fila.solicitudId]" class="enlace-mono" [attr.aria-label]="'Abrir solicitud ' + fila.solicitudId">
              <code>{{ acortarUuid(fila.solicitudId) }}</code>
            </a>
          }
          @case ('postulante') { <strong>{{ fila.postulante }}</strong> }
          @case ('grupoCodigo') { <span class="chip-grupo">{{ fila.grupoCodigo }}</span> }
          @case ('cuposSolicitados') { <span class="cupos">{{ fila.cuposSolicitados }}</span> }
          @case ('puntaje') { <span class="puntaje">{{ fila.puntaje ?? '—' }}</span> }
          @case ('fechaSolicitud') { <time [attr.datetime]="fila.fechaSolicitud">{{ dia(fila.fechaSolicitud) }}</time> }
          @case ('estado') { <ap-chip-estado [tono]="tonoEstado(fila.estado)">{{ fila.estado }}</ap-chip-estado> }
          @case ('acciones') {
            <ap-boton variante="fantasma" tamano="sm" [routerLink]="['/admisiones', fila.solicitudId]">
              {{ t.verExpediente }}
            </ap-boton>
          }
          @default { {{ fila[columna.clave] }} }
        }
      </ng-template>
    </ap-tabla-de-datos>
  `,
  styles: `
    .enlace-mono { color: var(--color-primario); text-decoration: none; font-weight: 600; }
    .enlace-mono:hover { text-decoration: underline; }
    .chip-grupo { font-family: var(--f-mono, monospace); font-size: 0.85em; padding: var(--s1) var(--s2); border-radius: var(--r-sm); background: var(--surface-2); border: var(--borde-fino) solid var(--border); color: var(--text); }
    .cupos, .puntaje { font-variant-numeric: tabular-nums; font-weight: 500; }
  `,
})
export class TablaDeAdmisiones {
  readonly filas = input.required<SolicitudDeAdmision[]>()
  readonly total = input.required<number>()

  protected readonly t = textosAdmisiones
  protected readonly identidad = (s: SolicitudDeAdmision) => s.solicitudId

  protected readonly columnas: Columna<FilaAdmision>[] = [
    { clave: 'solicitudId', titulo: 'Identificador', mono: true },
    { clave: 'postulante', titulo: 'Postulante', ordenable: true },
    { clave: 'grupoCodigo', titulo: 'Grupo', mono: true },
    { clave: 'cuposSolicitados', titulo: 'Cupos', numerica: true },
    { clave: 'puntaje', titulo: 'Puntaje', numerica: true },
    { clave: 'fechaSolicitud', titulo: 'Fecha', ordenable: true },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'acciones', titulo: 'Acción', fijaDerecha: true },
  ]

  protected tonoEstado(estado: string): Tono {
    const est = estado.toUpperCase()
    if (est === 'ACEPTADA' || est === 'ACTIVO') return 'ok'
    if (est === 'RECHAZADA' || est === 'CANCELADA') return 'error'
    if (est === 'PENDIENTE' || est === 'ESCALADA') return 'aviso'
    return 'neutro'
  }

  protected dia(iso: string): string {
    if (!iso) return '—'
    return formatearDia(iso.slice(0, 10))
  }

  protected acortarUuid(uuid: string): string {
    if (!uuid || uuid.length <= 12) return uuid
    return `${uuid.slice(0, 8)}…`
  }
}
