import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, RouterLink } from '@angular/router'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { EstadoDeListado, observarListado } from '../../contabilidad/listado/estado-de-listado'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoMonto, aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { accionesDeCampana, cargadorDeCampanas, type Campana } from '../dominio/cu111-campanas'
import { textosPublicidad } from '../textos'
import { EntradaCampanaMonedaEnum, EntradaCampanaObjetivoEnum } from 'clientes/angular/publicidad/model/entradaCampana'

const COLUMNAS: ColumnaVirtual<Campana>[] = [
  { clave: 'nombre', titulo: 'Campaña', ancho: '2' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'fechaInicio', titulo: 'Inicio' },
  { clave: 'presupuestoTotal', titulo: 'Presupuesto', numerica: true },
  { clave: 'presupuestoConsumido', titulo: 'Consumido', numerica: true },
  { clave: 'campanaId', titulo: 'Aprobación' },
]

const FILTROS: Filtro[] = [
  { valor: 'EN_REVISION', texto: 'En revisión' },
  { valor: 'ACTIVA', texto: 'Activa' },
  { valor: 'PAUSADA', texto: 'Pausada' },
  { valor: 'RECHAZADA', texto: 'Rechazada' },
  { valor: 'FINALIZADA', texto: 'Finalizada' },
  { valor: 'BORRADOR', texto: 'Borrador' },
]

/**
 * CU-111 · Lado **gestión**: crear campañas. Nunca aprueba desde acá — el botón de
 * aprobar/rechazar vive en `PanelDeAprobacionDeCampana`, montado por una ruta distinta
 * con su propio `canMatch` (`PUBLICIDAD_APROBAR_CAMPANA`), no por un `if` en esta
 * pantalla.
 */
@Component({
  selector: 'ap-pantalla-de-campanas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Boton, CabeceraDePagina, Dialogo, Campo, CampoMonto, ChipEstado, EstadoDeListado, Fecha, Monto, RouterLink, TablaDeDatosVirtualizada],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina
        data-tutorial-id="publicidad-campanas"
        [titulo]="t.tituloGestion"
        [descripcion]="t.propositoGestion"
        icono="estrella"
      >
        @if (puedeGestionar()) {
          <ap-boton acciones data-tutorial-id="publicidad-nueva-campana" (pulsado)="abrir()">{{ t.nuevaCampana }}</ap-boton>
        }
      </ap-cabecera-de-pagina>
      @if (puedeVerLaCola()) {
        <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="FILTROS" />
        <p class="nota">{{ t.porOmision }}</p>
        <ap-estado-de-listado [error]="listado.error()" [vacio]="listado.vacio()" [mensajeVacio]="t.sinCampanas" [filtrado]="!!filtrosActivos()['estado']" />
        <ap-tabla-de-datos-virtualizada data-tutorial-id="publicidad-tabla-campanas" [titulo]="t.tituloGestion" [columnas]="COLUMNAS" [cargador]="listado.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtrosActivos()">
          <ng-template #celda let-c let-columna="columna">
            @switch (columna.clave) {
              @case ('estado') {
                <ap-chip-estado [tono]="c.estado === 'ACTIVA' ? 'ok' : c.estado === 'RECHAZADA' ? 'error' : 'neutro'">{{ textoDeEstado(c) }}</ap-chip-estado>
              }
              @case ('fechaInicio') { <ap-fecha [iso]="c.fechaInicio" [conHora]="false" /> }
              @case ('presupuestoTotal') { <ap-monto [monto]="c.presupuestoTotal.monto" [moneda]="c.presupuestoTotal.moneda" [etiqueta]="t.columnaPresupuesto" /> }
              @case ('presupuestoConsumido') { <ap-monto [monto]="c.presupuestoConsumido.monto" [moneda]="c.presupuestoConsumido.moneda" [etiqueta]="t.columnaConsumido" /> }
              @case ('campanaId') {
                @if (c.estado === 'EN_REVISION') { <a [routerLink]="'/publicidad/campanas/' + c.campanaId + '/aprobacion'">{{ t.revisar }}</a> }
              }
              @default {
                {{ c[columna.clave] }}
              }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      } @else {
        <p class="nota" role="status">{{ t.soloOperaciones }}</p>
      }
    </section>

    <ap-dialogo [titulo]="t.nuevaCampana" [textoDeConfirmar]="t.guardar" [abierto]="dialogoAbierto()" [cargando]="guardando()" (abiertoChange)="dialogoAbierto.set($event)" (confirmar)="guardar()" (cancelar)="cerrar()">
      <div class="formulario" data-tutorial-id="publicidad-formulario-campana">
        <ap-campo [etiqueta]="t.nombre" [(valor)]="nombre" [error]="intentoGuardar() && !nombre() ? 'Obligatorio' : undefined" />
        <ap-campo-monto [etiqueta]="t.presupuestoTotal" [(valor)]="presupuesto" [error]="intentoGuardar() && !presupuesto() ? 'Obligatorio' : undefined" />
      </div>
    </ap-dialogo>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    .nota { color: var(--text-2); margin: 0; }
    a { color: var(--text); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .formulario { display: grid; gap: var(--s3); }
  `,
})
export class PantallaDeCampanas {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.propositoGestion)
  }
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeCampana()
  protected readonly t = textosPublicidad.campanas
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS = FILTROS
  protected readonly identidad = (c: Campana) => c.campanaId
  protected readonly listado = observarListado(cargadorDeCampanas(), textosPublicidad.errores)
  protected readonly puedeGestionar = () => this.sesion.puede('PUBLICIDAD_ANUNCIANTES')
  protected readonly puedeVerLaCola = () => this.sesion.puede('PUBLICIDAD_APROBAR_CAMPANA')
  protected readonly textoDeEstado = (c: Campana) => this.t.estados[c.estado]
  private readonly queryParamMap = toSignal(inject(ActivatedRoute).queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegido = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean)[0]
    return elegido ? { estado: elegido } : ({} as Record<string, string>)
  })

  protected readonly dialogoAbierto = signal(false)
  protected readonly guardando = signal(false)
  protected readonly intentoGuardar = signal(false)
  protected readonly nombre = signal('')
  protected readonly presupuesto = signal('')

  abrir(): void {
    this.nombre.set('')
    this.presupuesto.set('')
    this.intentoGuardar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
  }

  async guardar(): Promise<void> {
    this.intentoGuardar.set(true)
    const presupuestoContrato = this.presupuesto() ? aCadenaDelContrato(this.presupuesto()) : null
    if (!this.nombre().trim() || !presupuestoContrato) return
    this.guardando.set(true)
    try {
      // Hueco declarado: esta pantalla no captura todavía la cuenta publicitaria del
      // anunciante autenticado ni los conjuntos (`EntradaCampana.conjuntos` exige
      // mínimo 1) — el servidor va a rechazar esto con AP-CU111-0x hasta que se sume
      // el formulario de conjuntos (fuera del alcance que fijó `docs/Flujo de
      // pantallas · backoffice administrador.md` §6 para F14). Queda anotado en el
      // informe del carril como pendiente, no oculto.
      await this.acciones.crear({
        cuentaPublicitariaId: '00000000-0000-4000-8000-000000000000',
        nombre: this.nombre(),
        objetivo: EntradaCampanaObjetivoEnum.VisibilidadMarca,
        presupuestoTotal: presupuestoContrato,
        moneda: EntradaCampanaMonedaEnum.Bob,
        fechaInicio: new Date().toISOString(),
        conjuntos: [],
      })
      this.cerrar()
    } finally {
      this.guardando.set(false)
    }
  }
}
