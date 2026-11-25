import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Sesion } from '../../../nucleo/sesion'
import { crearListadoPorCursor } from '../../operacion/comun/listado-por-cursor'
import { ListadoPorCursorVista } from '../../operacion/comun/listado-por-cursor-vista'
import { pedidorDeRecuperaciones, type Decision, type EstadoDeRecuperacion, type SolicitudDeRecuperacion } from '../dominio/cuenta'
import { DecisionDeRecuperacion } from './decision-de-recuperacion'

export const PERMISO_DE_DECIDIR = 'SEGURIDAD_ACCESO_RESTABLECER'
const ESTADOS: Record<string, string> = {
  PENDIENTE: 'Pendiente de decisión', APROBADA: 'Aprobada, en espera', RECHAZADA: 'Rechazada', EMITIDA: 'Invitación emitida', COMPLETADA: 'Completada', VENCIDA: 'Vencida',
}
const TONOS: Record<string, Tono> = { PENDIENTE: 'aviso', APROBADA: 'info', RECHAZADA: 'error', EMITIDA: 'info', COMPLETADA: 'ok', VENCIDA: 'neutro' }
const ALCANCES: Record<string, string> = { CLAVE: 'Olvidó su clave', FACTOR: 'Perdió su autenticador' }
const COLUMNAS: Columna<SolicitudDeRecuperacion>[] = [
  { clave: 'nombres', titulo: 'Persona' }, { clave: 'alcance', titulo: 'Qué pidió' }, { clave: 'estado', titulo: 'Estado' },
  { clave: 'solicitadaEn', titulo: 'Pedido' }, { clave: 'motivo', titulo: 'Motivo' }, { clave: 'enfriamientoHasta', titulo: 'Espera hasta' },
]
const nombreDe = (s: SolicitudDeRecuperacion) => `${s.nombres} ${s.apellidos}`
const EXPORTABLES: ColumnaExportable<SolicitudDeRecuperacion>[] = [
  { clave: 'nombres', titulo: 'Persona', valor: nombreDe }, { clave: 'alcance', titulo: 'Qué pidió', valor: (s) => ALCANCES[s.alcance] },
  { clave: 'estado', titulo: 'Estado', valor: (s) => ESTADOS[s.estado] }, { clave: 'solicitadaEn', titulo: 'Pedido' },
  { clave: 'motivo', titulo: 'Motivo' }, { clave: 'enfriamientoHasta', titulo: 'Espera hasta', valor: (s) => s.enfriamientoHasta ?? '' },
]

/**
 * GU-C · Recuperaciones de acceso (`/acceso/recuperaciones`): la bandeja de quien tiene `SEGURIDAD_ACCESO_RESTABLECER`
 * (`listarRecuperaciones`, cursor `antesDe`). Arranca en «Pendiente de decisión». Aprobar abre un plazo de espera antes de emitir el
 * enlace; el titular no decide la suya (lo exige el servidor, `AP-CU09-09`). La ruta ya está guardada por permiso: acá se vuelve a
 * preguntar solo para no ofrecer acciones a quien llegó sin él.
 */
@Component({
  selector: 'ap-pantalla-de-recuperaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, CabeceraDePagina, ChipEstado, DecisionDeRecuperacion, Fecha, Icono, ListadoPorCursorVista],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Seguridad" titulo="Recuperaciones de acceso" descripcion="Pedidos de personas que olvidaron su clave o perdieron su autenticador. Decide alguien distinto del titular; si aprobás, hay un plazo de espera antes de emitir el enlace." />
      @if (hecho(); as s) {
        <ap-alerta tono="ok" [titulo]="s.estado === 'RECHAZADA' ? 'Recuperación rechazada' : 'Recuperación aprobada'">
          @if (s.estado === 'RECHAZADA') { Rechazaste el pedido de {{ nombreDe(s) }}. No se emite ningún enlace. }
          @else if (s.enfriamientoHasta) { El enlace para {{ nombreDe(s) }} se emite cuando termine la espera: <ap-fecha [iso]="s.enfriamientoHasta" />. }
          @else { Aprobaste el pedido de {{ nombreDe(s) }}. }
        </ap-alerta>
      }
      <ap-listado-por-cursor titulo="Recuperaciones de acceso" [columnas]="COLUMNAS" [fuente]="l" [exportables]="EXPORTABLES" [identidad]="identidad" ayudaDeBusqueda="Busca por nombre, qué pidió o motivo dentro de la página cargada." textoVacio="No hay recuperaciones con este filtro." explicacionVacio="Cuando alguien use «No puedo entrar» en el ingreso, su pedido aparece acá. Probá con otro estado para ver los ya decididos." textoDenegado="Esta bandeja es de quien tiene permiso para restablecer accesos." nombreDeRegistros="recuperaciones" nombreDeRegistro="recuperación">
        <ng-template #celda let-s let-columna="columna">
          @switch (columna.clave) {
            @case ('nombres') { {{ nombreDe(s) }} }
            @case ('alcance') { {{ ALCANCES[s.alcance] }} }
            @case ('estado') { <ap-chip-estado [tono]="TONOS[s.estado]">{{ ESTADOS[s.estado] }}</ap-chip-estado> }
            @case ('solicitadaEn') { <ap-fecha [iso]="s.solicitadaEn" /> }
            @case ('enfriamientoHasta') { @if (s.enfriamientoHasta) { <ap-fecha [iso]="s.enfriamientoHasta" /> } @else { <span class="nota">—</span> } }
            @default { {{ s[columna.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-s>
          <ap-acciones-de-fila [fila]="'recuperación de ' + nombreDe(s)">
            @if (puedeDecidir() && s.estado === 'PENDIENTE') {
              <button type="button" apAccionDeFila [attr.aria-label]="'Aprobar la recuperación de ' + nombreDe(s)" (click)="abrir(s, 'aprobacion')"><ap-icono nombre="verificado" tamano="s4" /><span>Aprobar</span></button>
              <button type="button" apAccionDeFila [attr.aria-label]="'Rechazar la recuperación de ' + nombreDe(s)" (click)="abrir(s, 'rechazo')"><ap-icono nombre="cerrar" tamano="s4" /><span>Rechazar</span></button>
            } @else { <span class="nota">{{ s.estado === 'PENDIENTE' ? 'Solo lectura' : 'Ya decidida' }}</span> }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado-por-cursor>
    </section>
    @if (elegida(); as e) { <ap-decision-de-recuperacion [solicitud]="e.solicitud" [decision]="e.decision" (decidida)="alDecidir($event)" (cerrar)="elegida.set(null)" /> }
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .nota { color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; }
  `,
})
export class PantallaDeRecuperaciones {
  private readonly sesion = inject(Sesion)
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly ESTADOS = ESTADOS
  protected readonly TONOS = TONOS
  protected readonly ALCANCES = ALCANCES
  protected readonly nombreDe = nombreDe
  protected readonly identidad = (s: SolicitudDeRecuperacion) => s.id
  protected readonly l = crearListadoPorCursor({
    pedir: pedidorDeRecuperaciones(),
    camposDeBusqueda: [nombreDe, (s: SolicitudDeRecuperacion) => ALCANCES[s.alcance], 'motivo'],
    filtrosDeServidor: [{ nombre: 'estado', etiqueta: 'Estado', opciones: (Object.keys(ESTADOS) as EstadoDeRecuperacion[]).map((valor) => ({ valor, texto: ESTADOS[valor] ?? valor })), textoTodos: 'Todos los estados' }],
    filtrosIniciales: { estado: 'PENDIENTE' },
  })
  protected readonly puedeDecidir = computed(() => this.sesion.puede(PERMISO_DE_DECIDIR))
  protected readonly elegida = signal<{ solicitud: SolicitudDeRecuperacion; decision: Decision } | null>(null)
  protected readonly hecho = signal<SolicitudDeRecuperacion | null>(null)

  protected abrir(solicitud: SolicitudDeRecuperacion, decision: Decision): void {
    if (!this.puedeDecidir()) return
    this.hecho.set(null)
    this.elegida.set({ solicitud, decision })
  }

  /** El éxito se anuncia con el 200 y la bandeja se vuelve a pedir: la solicitud decidida sale de «Pendiente». */
  protected alDecidir(s: SolicitudDeRecuperacion): void {
    this.elegida.set(null)
    this.hecho.set(s)
    this.l.recargar()
  }
}
