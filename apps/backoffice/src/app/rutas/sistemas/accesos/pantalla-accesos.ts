import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Casilla } from '@aportaya/ui/casilla/casilla'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { accionesDeAccesos, informeDeAccesos, type FilaDeAcceso, type PaginaDeAccesos, type SeccionDeAccesos, type SalidaRevocacionDeRol } from '../dominio/gobierno-de-accesos'
import { PERMISOS_DE_GESTION_DE_PERSONAL } from '../personal-y-accesos'
import { textosSistemas } from '../textos'
import { DialogoDeActo } from '../usuarios-internos/dialogo-de-acto'

const LIMITE_DE_PAGINA = 25
const SECCIONES: { valor: SeccionDeAccesos; texto: string }[] = [
  { valor: 'VIVOS', texto: 'Accesos vivos' },
  { valor: 'POR_VENCER', texto: 'Por vencer en 7 días' },
  { valor: 'SIN_TOTP', texto: 'Sin segundo factor' },
]
const VACIOS: Record<SeccionDeAccesos, string> = {
  VIVOS: 'No hay accesos vivos.',
  POR_VENCER: 'No hay accesos que venzan en los próximos 7 días.',
  SIN_TOTP: 'No hay accesos globales sin segundo factor.',
}
type FilaVisible = FilaDeAcceso & { accion?: undefined }

@Component({
  selector: 'ap-pantalla-accesos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Casilla, DialogoDeActo, EstadoDePantalla, Fecha, ListaDeOpciones, Paginacion, TablaDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Personal y accesos" [titulo]="t.titulo" [descripcion]="t.proposito">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="accesos.isLoading()" (pulsado)="accesos.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-lista-de-opciones etiqueta="Qué accesos revisar" [valor]="seccion()" [opciones]="secciones" (valorChange)="cambiarSeccion($event)" />
      @if (aviso(); as mensaje) { <ap-alerta tono="ok" titulo="Revocación confirmada">{{ mensaje }}</ap-alerta> }
      <ap-estado-de-pantalla [recurso]="accesos" [vacio]="estaVacio" [mensajeVacio]="mensajeVacio()" etiquetaDeCarga="Cargando informe de accesos" (reintentar)="accesos.reload()">
        @if (informe(); as i) {
          <ap-tabla-de-datos titulo="Asignaciones de acceso" [columnas]="columnas()" [filas]="i.items" [identidad]="identidad" [totalDeRegistros]="i.total" [cabecera]="true" textoRegistros="Asignaciones" textoVacio="No quedan asignaciones en esta página.">
            <ng-template #celda let-fila let-columna="columna">
              @switch (columna.clave) {
                @case ('ambito') { {{ fila.ambito }} @if (fila.ambitoId) { <span class="ambito">{{ fila.ambitoId }}</span> } }
                @case ('totpConfirmado') { {{ fila.totpConfirmado ? 'Confirmado' : 'Pendiente' }} }
                @case ('vigenteHasta') { @if (fila.vigenteHasta) { <ap-fecha [iso]="fila.vigenteHasta" /> } @else { Sin vencimiento } }
                @case ('otorgadaEn') { <ap-fecha [iso]="fila.otorgadaEn" /> }
                @case ('accion') { <ap-boton variante="peligro" tamano="sm" (pulsado)="pedirRevocacion(fila)">Revocar<span class="solo-lector"> {{ fila.rolCodigo }} de {{ fila.nombre }}</span></ap-boton> }
                @default { {{ fila[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos>
          <ap-paginacion etiqueta="Paginación del informe de accesos" [(pagina)]="pagina" [totalDeFilas]="i.total" [porPagina]="limite" nombreDeRegistros="asignaciones" />
        }
      </ap-estado-de-pantalla>

      @if (paraRevocar(); as fila) {
        <ap-dialogo-de-acto titulo="Revocar asignación" textoDeConfirmar="Revocar acceso" [destructivo]="true" [maximoDeMotivo]="120" efecto="La persona pierde este acceso. Para devolverlo, hay que otorgar otra asignación." [accion]="revocar" [abierto]="true" (abiertoChange)="!$event && paraRevocar.set(null)" (completado)="alRevocar($event)">
          <p>{{ fila.nombre }} · {{ fila.rolCodigo }}</p>
          <ap-casilla [(marcada)]="cerrarSesiones">Cerrar también sus sesiones activas</ap-casilla>
        </ap-dialogo-de-acto>
      }
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    ap-lista-de-opciones { justify-self: start; }
    ap-paginacion { margin-top: var(--s3); }
    .solo-lector { position: absolute; clip-path: inset(50%); overflow: hidden; white-space: nowrap; }
    .ambito { display: block; overflow-wrap: anywhere; }
  `,
})
export class PantallaAccesos {
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeAccesos()
  protected readonly t = textosSistemas.accesos
  protected readonly secciones = SECCIONES
  protected readonly seccion = signal<SeccionDeAccesos>('VIVOS')
  protected readonly pagina = signal(1)
  protected readonly limite = LIMITE_DE_PAGINA
  protected readonly accesos = informeDeAccesos(this.seccion, this.pagina, this.limite)
  protected readonly informe = computed(() => this.accesos.hasValue() ? this.accesos.value() : undefined)
  protected readonly mensajeVacio = computed(() => VACIOS[this.seccion()])
  protected readonly estaVacio = (informe: PaginaDeAccesos) => informe.total === 0
  protected readonly identidad = (fila: FilaDeAcceso) => fila.asignacionId
  protected readonly paraRevocar = signal<FilaDeAcceso | null>(null)
  protected readonly cerrarSesiones = signal(true)
  protected readonly aviso = signal<string | null>(null)
  protected readonly gestiona = computed(() => PERMISOS_DE_GESTION_DE_PERSONAL.some((p) => this.sesion.permisos().includes(p)))
  protected readonly columnas = computed<Columna<FilaVisible>[]>(() => {
    const columnas: Columna<FilaVisible>[] = [
      { clave: 'nombre', titulo: 'Persona' },
      { clave: 'rolCodigo', titulo: 'Rol' },
      { clave: 'ambito', titulo: 'Ámbito' },
      { clave: 'estadoCuenta', titulo: 'Estado de la cuenta' },
      { clave: 'otorgadaEn', titulo: 'Otorgado el' },
      { clave: 'vigenteHasta', titulo: 'Vigente hasta' },
      { clave: 'totpConfirmado', titulo: 'Segundo factor' },
    ]
    if (this.gestiona()) columnas.push({ clave: 'accion', titulo: 'Acciones' })
    return columnas
  })

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected cambiarSeccion(valor: string): void {
    const seccion = SECCIONES.find(({ valor: opcion }) => opcion === valor)
    if (!seccion || seccion.valor === this.seccion()) return
    this.pagina.set(1)
    this.seccion.set(seccion.valor)
    this.aviso.set(null)
  }

  protected pedirRevocacion(fila: FilaDeAcceso): void {
    if (!this.gestiona()) return
    this.cerrarSesiones.set(true)
    this.aviso.set(null)
    this.paraRevocar.set(fila)
  }

  protected readonly revocar = (evidenciaMfa: string, motivoRevocacion: string) =>
    this.acciones.revocar(this.paraRevocar()!.asignacionId, { motivoRevocacion, cerrarSesiones: this.cerrarSesiones(), evidenciaMfa })

  protected alRevocar(resultado: unknown): void {
    const r = resultado as SalidaRevocacionDeRol
    this.aviso.set(r.yaEstabaRevocada ? 'Esa asignación ya estaba revocada.' : `Se revocó el acceso. Sesiones cerradas: ${r.sesionesCerradas}.`)
    if (this.pagina() > 1 && this.informe()?.items.length === 1) this.pagina.update((p) => p - 1)
    else this.accesos.reload()
  }
}
