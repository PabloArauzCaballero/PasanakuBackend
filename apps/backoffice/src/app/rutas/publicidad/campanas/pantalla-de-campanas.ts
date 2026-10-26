import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import { Monto } from '@aportaya/ui/monto/monto'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { BarraDeLotes } from '../barra-de-lotes'
import { ESTADOS_DE_CAMPANA, lotesDeCampanas, type Campana } from '../dominio/cu111-campanas'
import { filtroEnElServidor } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import { FormularioDeCampana } from './formulario-de-campana'

const t = textosPublicidad.campanas
const TONOS: Record<Campana['estado'], Tono> = { BORRADOR: 'apagado', EN_REVISION: 'aviso', ACTIVA: 'ok', PAUSADA: 'neutro', RECHAZADA: 'error', FINALIZADA: 'info' }
const dinero = (d: { monto: string; moneda: string }) => `${d.monto} ${d.moneda}`

/**
 * CU-111 · Campañas. La lista es la cola de Operaciones (`listarCampanas` exige `PUBLICIDAD_APROBAR_CAMPANA`) y abre en
 * «En revisión», que es lo que espera a alguien; «Todos» pide los seis estados. Quien gestiona crea desde acá; aprobar o
 * rechazar vive en otra ruta con su propio `canMatch` (segregación de funciones), a la que lleva «Revisar». El contrato
 * no tiene editar, pausar ni eliminar campaña: la fila no los ofrece.
 */
@Component({
  selector: 'ap-pantalla-de-campanas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, BarraDeLotes, Boton, CabeceraDePagina, ChipEstado, Fecha, FormularioDeCampana, Icono, ListadoDeDatos, Monto, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="publicidad-campanas" eyebrow="Publicidad" [titulo]="t.tituloGestion" [descripcion]="t.propositoGestion" icono="estrella">
        @if (puedeGestionar()) {
          <ap-boton acciones data-tutorial-id="publicidad-nueva-campana" icono="mas" variante="primario" (pulsado)="formulario().abrir()">{{ t.nuevaCampana }}</ap-boton>
        }
      </ap-cabecera-de-pagina>
      @if (creada()) { <ap-alerta tono="ok">{{ t.creada }}</ap-alerta> }
      @if (puedeVerLaCola()) {
        <div class="cifras">
          <ap-tarjeta-kpi etiqueta="Cargadas" [valor]="'' + lotes.filas().length" icono="lista" [pista]="lotes.hayMas() ? 'Hay más en el servidor' : 'Todas las del filtro'" />
          <ap-tarjeta-kpi etiqueta="En revisión" [valor]="'' + cuenta('EN_REVISION')" icono="reloj" [tono]="cuenta('EN_REVISION') ? 'aviso' : 'neutro'" pista="Esperan aprobación" />
          <ap-tarjeta-kpi etiqueta="Activas" [valor]="'' + cuenta('ACTIVA')" icono="reproducir" tono="ok" pista="En lo cargado" />
          <ap-tarjeta-kpi etiqueta="Rechazadas" [valor]="'' + cuenta('RECHAZADA')" icono="rechazar" pista="En lo cargado" />
        </div>
        <ap-listado data-tutorial-id="publicidad-tabla-campanas" [titulo]="t.tituloLista" [columnas]="columnas" [listado]="listado" [carga]="lotes.carga()" [identidad]="porId" [exportables]="exportables" nombreDeRegistros="campañas" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.sinCampanas" [explicacionVacio]="t.porOmision" (reintentar)="lotes.recargar()">
          <ap-barra-de-lotes accionesDeListado [hayMas]="lotes.hayMas()" [cargando]="lotes.cargandoMas()" [error]="lotes.errorDeMas()" (cargarMas)="lotes.cargarMas()" />
          <ng-template #celda let-c let-columna="columna">
            @switch (columna.clave) {
              @case ('nombre') { <span class="nombre">{{ c.nombre }}</span> }
              @case ('estado') { <ap-chip-estado [tono]="tonoDe(c)">{{ textoDeEstado(c) }}</ap-chip-estado> }
              @case ('objetivo') { {{ textoDeObjetivo(c) }} }
              @case ('fechaInicio') { <ap-fecha [iso]="c.fechaInicio" [conHora]="false" /> }
              @case ('presupuestoTotal') { <ap-monto [monto]="c.presupuestoTotal.monto" [moneda]="c.presupuestoTotal.moneda" [etiqueta]="t.columnaPresupuesto" /> }
              @case ('presupuestoConsumido') { <ap-monto [monto]="c.presupuestoConsumido.monto" [moneda]="c.presupuestoConsumido.moneda" [etiqueta]="t.columnaConsumido" /> }
              @default { {{ c[columna.clave] }} }
            }
          </ng-template>
          <ng-template #acciones let-c>
            @if (c.estado === 'EN_REVISION') {
              <ap-acciones-de-fila [fila]="c.nombre">
                <button type="button" apAccionDeFila [attr.aria-label]="t.revisar + ' ' + c.nombre" (click)="revisar(c)"><ap-icono nombre="aprobar" tamano="s4" /><span>{{ t.revisar }}</span></button>
              </ap-acciones-de-fila>
            } @else { <span class="sin-accion">{{ t.sinAccion }}</span> }
          </ng-template>
        </ap-listado>
      } @else {
        <ap-alerta tono="info">{{ t.soloOperaciones }}</ap-alerta>
      }
    </section>
    <ap-formulario-de-campana (creada)="alCrear()" />
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(min(100%, 11rem), 1fr)); }
    .nombre { font-weight: 600; color: var(--text); overflow-wrap: anywhere; }
    .sin-accion { color: var(--text-3); font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeCampanas {
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  protected readonly t = t
  protected readonly formulario = viewChild.required(FormularioDeCampana)
  protected readonly lotes = lotesDeCampanas()
  protected readonly creada = signal(false)
  protected readonly porId = (c: Campana) => c.campanaId
  protected readonly puedeGestionar = () => this.sesion.puede('PUBLICIDAD_ANUNCIANTES')
  protected readonly puedeVerLaCola = () => this.sesion.puede('PUBLICIDAD_APROBAR_CAMPANA')
  protected readonly textoDeEstado = (c: Campana) => t.estados[c.estado]
  protected readonly textoDeObjetivo = (c: Campana) => t.objetivos[c.objetivo]
  protected readonly tonoDe = (c: Campana) => TONOS[c.estado]
  protected readonly cuenta = (estado: Campana['estado']) => this.porEstado()[estado] ?? 0
  private readonly porEstado = computed(() => this.lotes.filas().reduce<Record<string, number>>((n, c) => ({ ...n, [c.estado]: (n[c.estado] ?? 0) + 1 }), {}))

  protected readonly listado = crearListado<Campana>({
    filas: this.lotes.filas,
    camposDeBusqueda: ['nombre', (c) => t.estados[c.estado], (c) => t.objetivos[c.objetivo], 'campanaId', 'anuncianteId'],
    filtros: [
      { nombre: 'estado', etiqueta: 'Estado', valor: (c) => c.estado, opciones: ESTADOS_DE_CAMPANA.map((v) => ({ valor: v, texto: t.estados[v] })), ayuda: 'Se consulta al servidor. «Todos» trae los seis estados.' },
      { nombre: 'objetivo', etiqueta: 'Objetivo', valor: (c) => c.objetivo, texto: (v) => t.objetivos[v as Campana['objetivo']] ?? v },
      { nombre: 'moneda', etiqueta: 'Moneda', valor: (c) => c.presupuestoTotal.moneda },
    ],
    valorParaOrden: (c, clave) => (clave === 'presupuestoTotal' ? c.presupuestoTotal.monto : clave === 'presupuestoConsumido' ? c.presupuestoConsumido.monto : (c as Record<string, unknown>)[clave]),
  })
  protected readonly columnas: Columna<Campana>[] = [
    { clave: 'nombre', titulo: 'Campaña', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'objetivo', titulo: 'Objetivo', ordenable: true },
    { clave: 'fechaInicio', titulo: 'Inicio', ordenable: true },
    { clave: 'presupuestoTotal', titulo: 'Presupuesto', numerica: true, ordenable: true },
    { clave: 'presupuestoConsumido', titulo: 'Consumido', numerica: true, ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Campana>[] = [
    { clave: 'campanaId', titulo: 'Identificador' },
    { clave: 'nombre', titulo: 'Campaña' },
    { clave: 'estado', titulo: 'Estado', valor: (c) => t.estados[c.estado] },
    { clave: 'objetivo', titulo: 'Objetivo', valor: (c) => t.objetivos[c.objetivo] },
    { clave: 'fechaInicio', titulo: 'Inicio' },
    { clave: 'presupuestoTotal', titulo: 'Presupuesto', valor: (c) => dinero(c.presupuestoTotal) },
    { clave: 'presupuestoConsumido', titulo: 'Consumido', valor: (c) => dinero(c.presupuestoConsumido) },
    { clave: 'anuncianteId', titulo: 'Anunciante' },
  ]

  constructor() {
    inject(ExplicadorDeVista).fijar(t.propositoGestion)
    // La cola abre en lo que espera a alguien; el efecto de abajo hace la primera consulta con este filtro.
    this.listado.filtrar('estado', 'EN_REVISION')
    if (this.puedeVerLaCola()) filtroEnElServidor(this.listado, this.lotes)
  }

  /** La campaña viaja en el estado de la navegación: el contrato no tiene `GET /campanas/{id}` para releerla. */
  protected revisar(c: Campana): void {
    void this.router.navigate(['/publicidad/campanas', c.campanaId, 'aprobacion'], { state: { campana: c } })
  }

  protected alCrear(): void {
    this.creada.set(true)
    if (this.puedeVerLaCola()) this.lotes.recargar()
  }
}
