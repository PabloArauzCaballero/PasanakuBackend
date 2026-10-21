import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ServicioOperaciones } from './dominio/servicio-operaciones'
import { TablaOperaciones } from './tabla-operaciones'

@Component({
  selector: 'ap-pantalla-operaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TarjetaKPI, BarraDeFiltros, EstadoVacio, TablaOperaciones],
  template: `
    <section class="pantalla-ops">
      <ap-cabecera-de-pagina
        icono="baseDeDatos"
        eyebrow="Sistemas · Catálogo"
        titulo="Operaciones del sistema"
        descripcion="Catálogo exhaustivo de las operaciones y contratos de todos los microservicios de AportaYa: tablas que afectan, DTOs, validaciones, payloads y árbol de decisiones por flujo."
      />

      <div class="kpis">
        <ap-tarjeta-kpi etiqueta="Operaciones registradas" [valor]="'' + metricas().total" icono="baseDeDatos" tono="info" />
        <ap-tarjeta-kpi etiqueta="Mutaciones (POST/PUT)" [valor]="'' + metricas().mutaciones" icono="reloj" tono="aviso" />
        <ap-tarjeta-kpi etiqueta="Consultas (GET)" [valor]="'' + metricas().lecturas" icono="baseDeDatos" tono="ok" />
        <ap-tarjeta-kpi etiqueta="Críticas y sensibles" [valor]="'' + metricas().criticas" icono="candado" tono="error" />
        <ap-tarjeta-kpi etiqueta="Con clave de idempotencia" [valor]="'' + metricas().idempotentes" icono="verificado" tono="neutro" />
      </div>

      <ap-barra-de-filtros
        etiquetaDeBusqueda="Buscar operación"
        ayudaDeBusqueda="Buscar por ruta, operationId, propósito, tabla afectada o DTO…"
        [busqueda]="q()"
        [selectores]="selectores()"
        (busquedaChange)="q.set($event)"
        (selectorChange)="elegirSelector($event.nombre, $event.valor)"
        (limpiar)="limpiarFiltros()"
      />

      @if (filtradas().length === 0) {
        <ap-estado-vacio
          motivo="porFiltro"
          titulo="Ninguna operación coincide con la búsqueda o filtros"
          mensaje="Intenta ajustar el texto de búsqueda o restablecer los filtros de servicio y método."
          (actuar)="limpiarFiltros()"
        />
      } @else {
        <ap-tabla-operaciones [operaciones]="filtradas()" />
      }
    </section>
  `,
  styles: `
    :host { display: block; width: 100%; min-width: 0; }
    .pantalla-ops { display: flex; flex-direction: column; gap: var(--s4); width: 100%; }
    .kpis { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr)); }
  `,
})
export class PantallaOperaciones {
  private readonly servicioOps = inject(ServicioOperaciones)
  protected readonly metricas = this.servicioOps.metricas

  protected readonly q = signal('')
  protected readonly filtroServicio = signal('')
  protected readonly filtroMetodo = signal('')
  protected readonly filtroRiesgo = signal('')
  protected readonly filtroFlujo = signal('')

  constructor() {
    inject(ExplicadorDeVista).fijar(
      'Catálogo de operaciones del sistema: detalla qué tablas impacta cada endpoint, su propósito de negocio y de sistema, sus DTOs con validaciones, payload de ejemplo y en qué flujo participa con todas sus decisiones.'
    )
  }

  protected readonly filtradas = computed(() =>
    this.servicioOps.filtrar({
      q: this.q(),
      servicio: this.filtroServicio(),
      metodo: this.filtroMetodo(),
      riesgo: this.filtroRiesgo(),
      flujo: this.filtroFlujo(),
    })
  )

  protected readonly selectores = computed<Selector[]>(() => [
    {
      nombre: 'servicio',
      etiqueta: 'Servicio',
      valor: this.filtroServicio(),
      opciones: [
        { valor: '', texto: 'Todos los servicios' },
        ...this.servicioOps.serviciosUnicos().map((s) => ({ valor: s, texto: s })),
      ],
    },
    {
      nombre: 'metodo',
      etiqueta: 'Método',
      valor: this.filtroMetodo(),
      opciones: [
        { valor: '', texto: 'Todos los métodos' },
        { valor: 'GET', texto: 'GET (Lectura)' },
        { valor: 'POST', texto: 'POST (Creación / Efecto)' },
        { valor: 'PUT', texto: 'PUT (Reemplazo / Actualización)' },
        { valor: 'DELETE', texto: 'DELETE (Eliminación)' },
      ],
    },
    {
      nombre: 'riesgo',
      etiqueta: 'Nivel de riesgo',
      valor: this.filtroRiesgo(),
      opciones: [
        { valor: '', texto: 'Cualquier riesgo' },
        { valor: 'CRITICO', texto: 'Crítico' },
        { valor: 'ALTO', texto: 'Alto' },
        { valor: 'MEDIO', texto: 'Medio' },
        { valor: 'BAJO', texto: 'Bajo' },
      ],
    },
    {
      nombre: 'flujo',
      etiqueta: 'Flujo de negocio',
      valor: this.filtroFlujo(),
      opciones: [
        { valor: '', texto: 'Todos los flujos' },
        ...this.servicioOps.flujosUnicos().map((f) => ({ valor: f.id, texto: f.nombre })),
      ],
    },
  ])

  protected elegirSelector(nombre: string, valor: string): void {
    if (nombre === 'servicio') this.filtroServicio.set(valor)
    else if (nombre === 'metodo') this.filtroMetodo.set(valor)
    else if (nombre === 'riesgo') this.filtroRiesgo.set(valor)
    else if (nombre === 'flujo') this.filtroFlujo.set(valor)
  }

  protected limpiarFiltros(): void {
    this.q.set('')
    this.filtroServicio.set('')
    this.filtroMetodo.set('')
    this.filtroRiesgo.set('')
    this.filtroFlujo.set('')
  }
}
