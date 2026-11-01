import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { DialogoDeConfirmacion } from '@aportaya/ui/dialogo-de-confirmacion/dialogo-de-confirmacion'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../../contabilidad/listado/listado-por-cursor'
import { TablaPorCursor } from '../../contabilidad/listado/tabla-por-cursor'
import { cargadorDeTarifarios, crearPonerVigente, type TarifarioEnLista } from '../dominio/cu34-tarifarios'
import { NO_DISPONIBLE } from '../dominio/listado'
import { textosConfiguracion } from '../textos'
import { importeSinMoneda } from '../../operacion/comun/importe-sin-moneda'

const t = textosConfiguracion.tarifario
const ESTADOS: Record<string, string> = t.estados
const simulacion = (r: TarifarioEnLista) => (r.simulacion ? `${importeSinMoneda(String(r.simulacion.deltaIngresoEstimado))} (${t.deltaSinMoneda}) · ${t.usuarios(Number(r.simulacion.usuariosImpactados))}` : NO_DISPONIBLE)

const COLUMNAS: Columna<TarifarioEnLista>[] = [
  { clave: 'codigo', titulo: 'Código', mono: true },
  { clave: 'version', titulo: 'Versión', numerica: true },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'monedaBase', titulo: 'Moneda' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta' },
  { clave: 'diasPreaviso', titulo: 'Preaviso (días)', numerica: true },
  { clave: 'simulacion', titulo: 'Última simulación' },
]

const EXPORTABLES = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'version', titulo: 'Versión' },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'estado', titulo: 'Estado', valor: (r: TarifarioEnLista) => ESTADOS[r.estado] ?? r.estado },
  { clave: 'monedaBase', titulo: 'Moneda' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta', valor: (r: TarifarioEnLista) => r.vigenteHasta ?? NO_DISPONIBLE },
  { clave: 'diasPreaviso', titulo: 'Preaviso (días)' },
  { clave: 'simulacion', titulo: 'Última simulación', valor: simulacion },
]

const TONO: Record<TarifarioEnLista['estado'], Tono> = { VIGENTE: 'ok', EN_PREAVISO: 'aviso', BORRADOR: 'info', SUSTITUIDO: 'neutro', ARCHIVADO: 'neutro' }

/**
 * CU-34 · Historial del tarifario (carril L4). Con `TARIFARIO_PUBLICAR`, cada versión EN_PREAVISO ofrece «Poner en
 * vigencia» (`ponerTarifarioVigente`): el servidor decide si el preaviso se cumplió. Publicar una versión nueva no se hace
 * acá (acto de comité, decisión pendiente de Finanzas/Legal), ni editar ni borrar una versión, que el contrato no publica. Listado por cursor del servidor (vigencia descendente): el estado viaja como `estados`; el buscador y la
 * moneda filtran la página cargada. `simulacion.deltaIngresoEstimado` viaja SIN moneda: se muestra como texto, no como
 * importe. Un campo opcional ausente se dice «no disponible».
 */
@Component({
  selector: 'ap-pantalla-de-tarifario',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ChipEstado, DialogoDeConfirmacion, Fecha, TablaPorCursor],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Configuración" [titulo]="t.titulo" [descripcion]="t.proposito" />
      <ap-alerta tono="info">{{ puedeActivar() ? t.ayudaDeAcciones : comun.soloLectura }}</ap-alerta>
      @if (puesto(); as p) { <ap-alerta tono="ok">{{ p }}</ap-alerta> }
      @if (error(); as e) { <ap-alerta tono="error">{{ e.mensaje }}</ap-alerta> }
      <ap-tabla-por-cursor [titulo]="t.titulo" [columnas]="columnas()" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="versiones" [textoVacio]="t.vacio" ayudaDeBusqueda="Busca por código o nombre en la página cargada: el servidor no busca por texto.">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ etiqueta(r) }}</ap-chip-estado> }
            @case ('vigenteDesde') { <ap-fecha [iso]="r.vigenteDesde" /> }
            @case ('vigenteHasta') { @if (r.vigenteHasta) { <ap-fecha [iso]="r.vigenteHasta" /> } @else { {{ noDisponible }} } }
            @case ('simulacion') { {{ simulacion(r) }} }
            @case ('accion') {
              @if (r.estado === 'EN_PREAVISO') {
                <ap-boton variante="secundario" tamano="sm" [deshabilitado]="activando()" (pulsado)="pedirConfirmacion(r)">{{ t.ponerVigente }}</ap-boton>
              }
            }
            @default { {{ r[columna.clave] ?? noDisponible }} }
          }
        </ng-template>
      </ap-tabla-por-cursor>
      @if (elegido(); as r) {
        <ap-dialogo-de-confirmacion
          [titulo]="t.confirmarTitulo(r.codigo, r.version)"
          [descripcion]="t.confirmarDescripcion"
          [textoDeConfirmar]="t.confirmar"
          [destructivo]="true"
          [cargando]="activando()"
          [(abierto)]="confirmando"
          (confirmar)="activar(r)"
          (cancelar)="elegido.set(null)"
        />
      }
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
  `,
})
export class PantallaDeTarifario {
  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
  }

  protected readonly t = t
  protected readonly comun = textosConfiguracion.comun
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly simulacion = simulacion
  protected readonly identidad = (r: TarifarioEnLista) => r.tarifarioId
  protected readonly etiqueta = (r: TarifarioEnLista) => ESTADOS[r.estado] ?? r.estado
  protected readonly listado = crearListadoPorCursor<TarifarioEnLista>({
    cargador: cargadorDeTarifarios(),
    camposDeBusqueda: ['codigo', 'nombre'],
    servidor: { nombre: 'estado', etiqueta: textosConfiguracion.comun.estado, opciones: opcionesDe(ESTADOS), inicial: filtroInicialDeLaUrl(opcionesDe(ESTADOS)) },
    filtros: [{ nombre: 'moneda', etiqueta: 'Moneda', valor: (r) => r.monedaBase }],
  })
  private readonly sesion = inject(Sesion)
  private readonly ponerVigente = crearPonerVigente()
  /** El mismo permiso que exige `tarifas` (`@Permiso("TARIFARIO_PUBLICAR")`); el servidor vuelve a decidir igual. */
  protected readonly puedeActivar = computed(() => this.sesion.puede('TARIFARIO_PUBLICAR'))
  protected readonly columnas = computed<Columna<TarifarioEnLista>[]>(() =>
    this.puedeActivar() ? [...COLUMNAS, { clave: 'accion' as keyof TarifarioEnLista, titulo: 'Acción', fijaDerecha: true }] : COLUMNAS,
  )
  protected readonly elegido = signal<TarifarioEnLista | null>(null)
  protected readonly confirmando = signal(false)
  protected readonly activando = signal(false)
  protected readonly puesto = signal<string | null>(null)
  protected readonly error = signal<ErrorTraducido | null>(null)

  protected pedirConfirmacion(r: TarifarioEnLista): void {
    this.puesto.set(null)
    this.error.set(null)
    this.elegido.set(r)
    this.confirmando.set(true)
  }

  protected activar(r: TarifarioEnLista): void {
    if (this.activando()) return
    this.activando.set(true)
    this.ponerVigente(r.tarifarioId).subscribe({
      next: () => {
        this.activando.set(false)
        this.confirmando.set(false)
        this.elegido.set(null)
        this.puesto.set(t.puesto(r.codigo, r.version))
        this.listado.recargar()
      },
      error: (e: ErrorTraducido) => {
        this.activando.set(false)
        this.confirmando.set(false)
        this.elegido.set(null)
        this.error.set(e)
        this.listado.recargar()
      },
    })
  }

  protected tonoDe(r: TarifarioEnLista): Tono {
    return TONO[r.estado] ?? 'neutro'
  }
}
