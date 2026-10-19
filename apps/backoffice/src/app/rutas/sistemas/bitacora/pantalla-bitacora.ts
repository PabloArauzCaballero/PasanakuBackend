import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CampoBusqueda } from '@aportaya/ui/campo-busqueda/campo-busqueda'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { bitacora, type EventoBitacora, type PaginaDeBitacora } from '../dominio/bitacora'
import { textosSistemas } from '../textos'
import { CajonDetalleEvento } from './cajon-detalle-evento'

/**
 * Pantalla de registro inmutable del sistema (comun.bitacora_evento):
 * Bitácora encadenada por hash SHA-256 para auditoría y cumplimiento regulatorio.
 */
@Component({
  selector: 'ap-pantalla-bitacora',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    Boton,
    CabeceraDePagina,
    CampoBusqueda,
    CajonDetalleEvento,
    ChipEstado,
    EstadoDePantalla,
    ListaDeOpciones,
    TablaDeDatos,
  ],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="libro" />

      <div class="filtros" role="search" aria-label="Filtros del registro del sistema">
        <ap-lista-de-opciones
          [compacta]="true"
          etiqueta="Entidad"
          [opciones]="opcionesEntidad"
          [(valor)]="filtroEntidad"
          textoVacio="Todas las entidades"
        />
        <ap-lista-de-opciones
          [compacta]="true"
          etiqueta="Acción"
          [opciones]="opcionesAccion"
          [(valor)]="filtroAccion"
          textoVacio="Todas las acciones"
        />
        <div class="busqueda-contenedor">
          <ap-campo-busqueda [etiqueta]="t.buscar" [(valor)]="busqueda" />
        </div>
      </div>

      <ap-estado-de-pantalla
        [recurso]="recurso"
        [vacio]="esVacio"
        [mensajeVacio]="t.vacio"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="recurso.reload()"
      >
        @if (recurso.hasValue() && recurso.value(); as pagina) {
          <ap-tabla-de-datos
            [titulo]="t.titulo"
            [columnas]="columnas"
            [filas]="filasFiltradas()"
          >
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'accion') {
                <ap-chip-estado [tono]="tonoAccion(fila.accion)">{{ fila.accion }}</ap-chip-estado>
              } @else if (columna.clave === 'hashRegistro') {
                <code class="hash">{{ acortar(fila.hashRegistro) }}</code>
              } @else if (columna.clave === 'actor') {
                {{ fila.actorRol || acortar(fila.actorUsuarioId) || '—' }}
              } @else if (columna.clave === 'acciones') {
                <ap-boton variante="fantasma" (pulsado)="abrirDetalle(fila)">
                  {{ t.verDetalle }}
                </ap-boton>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>

      <ap-cajon-detalle-evento [evento]="eventoSeleccionado()" [(abierto)]="cajonAbierto" />
    </section>
  `,
  styles: `
    .pantalla { padding: 0; max-width: 72rem; display: flex; flex-direction: column; gap: var(--s5); }
    .filtros { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .busqueda-contenedor { flex: 1; min-width: 14rem; }
    .hash { font-family: var(--f-mono); font-size: 0.85em; color: var(--text-2); background: var(--surface-2); padding: var(--s1) var(--s2); border-radius: var(--r-sm); }
  `,
})
export class PantallaBitacora {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.bitacora
  readonly filtroEntidad = signal('')
  readonly filtroAccion = signal('')
  readonly busqueda = signal('')
  readonly eventoSeleccionado = signal<EventoBitacora | null>(null)
  readonly cajonAbierto = signal(false)

  protected readonly recurso = bitacora({
    entidad: this.filtroEntidad,
    accion: this.filtroAccion,
  })

  protected readonly esVacio = (pagina: PaginaDeBitacora) => pagina.items.length === 0

  protected readonly filasFiltradas = computed<(EventoBitacora & { actor?: string; acciones?: string })[]>(() => {
    const pagina = this.recurso.value()
    if (!pagina) return []
    const termino = this.busqueda().trim().toLowerCase()
    if (!termino) return pagina.items
    return pagina.items.filter(
      (e) =>
        e.entidad.toLowerCase().includes(termino) ||
        e.accion.toLowerCase().includes(termino) ||
        (e.actorRol && e.actorRol.toLowerCase().includes(termino)) ||
        (e.motivo && e.motivo.toLowerCase().includes(termino)) ||
        e.hashRegistro.toLowerCase().includes(termino),
    )
  })

  protected readonly columnas: Columna<EventoBitacora & { actor?: string; acciones?: string }>[] = [
    { clave: 'secuencia', titulo: '#' },
    { clave: 'fechaHora', titulo: 'Fecha y hora', ordenable: true },
    { clave: 'entidad', titulo: 'Entidad', ordenable: true },
    { clave: 'accion', titulo: 'Acción' },
    { clave: 'actor', titulo: 'Actor' },
    { clave: 'origen', titulo: 'Origen' },
    { clave: 'hashRegistro', titulo: 'Hash' },
    { clave: 'acciones', titulo: 'Detalle' },
  ]

  protected readonly opcionesEntidad: readonly Opcion[] = [
    { valor: 'USUARIO', texto: 'USUARIO' },
    { valor: 'GRUPO', texto: 'GRUPO' },
    { valor: 'CONFIGURACION', texto: 'CONFIGURACION' },
    { valor: 'OBLIGACION_APORTE', texto: 'OBLIGACION_APORTE' },
    { valor: 'ENTREGA_FONDO', texto: 'ENTREGA_FONDO' },
    { valor: 'CUENTA_BANCARIA', texto: 'CUENTA_BANCARIA' },
    { valor: 'ORGANIZADOR', texto: 'ORGANIZADOR' },
    { valor: 'SOLICITUD_INGRESO', texto: 'SOLICITUD_INGRESO' },
    { valor: 'RECLAMO', texto: 'RECLAMO' },
    { valor: 'CASO_INVESTIGACION', texto: 'CASO_INVESTIGACION' },
  ]

  protected readonly opcionesAccion: readonly Opcion[] = [
    { valor: 'CREACION', texto: 'CREACION' },
    { valor: 'ACTUALIZACION', texto: 'ACTUALIZACION' },
    { valor: 'CAMBIO_CONFIGURACION', texto: 'CAMBIO_CONFIGURACION' },
    { valor: 'CAMBIO_ESTADO', texto: 'CAMBIO_ESTADO' },
    { valor: 'CAMBIO_PERMISOS', texto: 'CAMBIO_PERMISOS' },
    { valor: 'APROBACION', texto: 'APROBACION' },
    { valor: 'RECHAZO', texto: 'RECHAZO' },
    { valor: 'SANCION', texto: 'SANCION' },
    { valor: 'ELIMINACION_LOGICA', texto: 'ELIMINACION_LOGICA' },
    { valor: 'INICIO_SESION', texto: 'INICIO_SESION' },
    { valor: 'ACCESO_DATOS', texto: 'ACCESO_DATOS' },
    { valor: 'EXPORTACION', texto: 'EXPORTACION' },
    { valor: 'CONCILIACION', texto: 'CONCILIACION' },
    { valor: 'DESEMBOLSO', texto: 'DESEMBOLSO' },
    { valor: 'LIQUIDACION', texto: 'LIQUIDACION' },
    { valor: 'ANULACION', texto: 'ANULACION' },
  ]

  protected tonoAccion(accion: string): Tono {
    if (accion === 'CREACION' || accion === 'APROBACION' || accion === 'CONCILIACION') return 'ok'
    if (accion === 'SANCION' || accion === 'RECHAZO' || accion === 'ANULACION') return 'error'
    if (accion.startsWith('CAMBIO_') || accion === 'ACTUALIZACION') return 'info'
    return 'neutro'
  }

  protected acortar(valor: string | undefined | null): string {
    if (!valor) return ''
    return valor.length > 12 ? `${valor.slice(0, 10)}…` : valor
  }

  protected abrirDetalle(fila: EventoBitacora) {
    this.eventoSeleccionado.set(fila)
    this.cajonAbierto.set(true)
  }
}
