import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ServicioOperaciones } from './dominio/servicio-operaciones'
import { SeccionArbolDecisiones } from './seccion-arbol-decisiones'
import { SeccionContratosDto } from './seccion-contratos-dto'
import { SeccionPayloadEjemplo } from './seccion-payload-ejemplo'
import { SeccionTablasImpactadas } from './seccion-tablas-impactadas'

const PESTANAS: Pestana[] = [
  { valor: 'resumen', texto: 'Vista general' },
  { valor: 'tablas', texto: 'Tablas impactadas' },
  { valor: 'contratos', texto: 'Contratos y DTOs' },
  { valor: 'decisiones', texto: 'Árbol de decisiones' },
  { valor: 'payload', texto: 'Payloads de ejemplo' },
]

@Component({
  selector: 'ap-pantalla-operacion-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    CabeceraDePagina,
    ChipEstado,
    Pestanas,
    SeccionClaveValor,
    SeccionTablasImpactadas,
    SeccionContratosDto,
    SeccionArbolDecisiones,
    SeccionPayloadEjemplo,
  ],
  template: `
    @if (operacion(); as op) {
      <section class="detalle-op">
        <ap-cabecera-de-pagina
          [eyebrow]="'Sistemas · ' + op.servicio"
          [titulo]="op.metodo + ' ' + op.ruta"
          [descripcion]="op.proposito"
        >
          <ap-chip-estado acciones [tono]="tonoMetodo(op.metodo)">{{ op.metodo }}</ap-chip-estado>
          <ap-chip-estado acciones [tono]="tonoRiesgo(op.riesgo)">{{ op.riesgo }}</ap-chip-estado>
          @if (op.sensible) {
            <ap-chip-estado acciones tono="pii">Dato sensible / PII</ap-chip-estado>
          }
          @if (op.idempotencia) {
            <ap-chip-estado acciones tono="neutro">Idempotente</ap-chip-estado>
          }
        </ap-cabecera-de-pagina>

        <ap-pestanas
          etiqueta="Secciones de la operación"
          estilo="segmentada"
          [pestanas]="pestanas"
          [elegida]="pestanaActiva()"
          (elegidaChange)="pestanaActiva.set($event)"
        />

        <div class="panel-contenido" role="tabpanel">
          @switch (pestanaActiva()) {
            @case ('tablas') {
              <ap-seccion-tablas-impactadas [tablas]="op.tablasImpactadas" />
            }
            @case ('contratos') {
              <ap-seccion-contratos-dto [dtoEntrada]="op.dtoEntrada" [dtoSalida]="op.dtoSalida" />
            }
            @case ('decisiones') {
              <ap-seccion-arbol-decisiones [decisiones]="op.flujo.decisiones" />
            }
            @case ('payload') {
              <ap-seccion-payload-ejemplo [payloadEntrada]="op.payloadEjemplo" [payloadRespuesta]="op.payloadRespuesta" />
            }
            @default {
              <div class="grilla-resumen">
                <ap-seccion-clave-valor titulo="Identificación técnica" descripcion="Coordenadas del endpoint en el código" [items]="itemsTecnicos()" />
                <ap-seccion-clave-valor titulo="Contexto de negocio" descripcion="Qué representa y para qué existe" [items]="itemsNegocio()" />
                <ap-seccion-clave-valor class="ancha" titulo="Ubicación en el flujo" descripcion="Paso dentro del recorrido operativo" [items]="itemsFlujo()" />
                <ap-seccion-clave-valor class="ancha" titulo="A nivel de sistema" descripcion="Garantías transaccionales y de aislamiento" [items]="itemsSistema()" />
              </div>
            }
          }
        </div>
      </section>
    } @else {
      <div class="no-encontrada">
        <p>No se encontró la operación solicitada con identificador <code>{{ id() }}</code>.</p>
        <a routerLink="/sistemas/operaciones">Volver al catálogo de operaciones</a>
      </div>
    }
  `,
  styles: `
    :host { display: block; width: 100%; min-width: 0; }
    .detalle-op { display: flex; flex-direction: column; gap: var(--s4); width: 100%; }
    .panel-contenido { display: flex; flex-direction: column; min-width: 0; }
    .grilla-resumen { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(min(100%, 22rem), 1fr)); align-items: start; }
    .ancha { grid-column: 1 / -1; }
    .no-encontrada { display: flex; flex-direction: column; gap: var(--s2); padding: var(--s4); }
  `,
})
export class PantallaOperacionDetalle {
  readonly id = input.required<string>()
  private readonly servicioOps = inject(ServicioOperaciones)

  protected readonly pestanaActiva = signal('resumen')
  protected readonly pestanas = PESTANAS

  constructor() {
    inject(ExplicadorDeVista).fijar(
      'Ficha de operación del sistema: anatomía técnica, tablas afectadas, DTOs y validaciones, payload de ejemplo y decisiones posibles dentro del flujo.'
    )
  }

  protected readonly operacion = computed(() => this.servicioOps.buscarPorId(this.id()))

  protected readonly itemsTecnicos = computed<ClaveValor[]>(() => {
    const op = this.operacion()
    if (!op) return []
    return [
      { etiqueta: 'Servicio', valor: op.servicio, mono: true },
      { etiqueta: 'Método HTTP', valor: op.metodo, mono: true },
      { etiqueta: 'Ruta', valor: op.ruta, mono: true },
      { etiqueta: 'operationId', valor: op.operationId, mono: true },
      { etiqueta: 'Caso de uso', valor: op.cu ?? 'Operación de plataforma', mono: !!op.cu },
      { etiqueta: 'Autorización / Rol', valor: op.permiso, mono: true },
    ]
  })

  protected readonly itemsNegocio = computed<ClaveValor[]>(() => {
    const op = this.operacion()
    if (!op) return []
    return [
      { etiqueta: 'Propósito', valor: op.proposito },
      { etiqueta: 'Módulo de negocio', valor: op.modulo },
      { etiqueta: 'Explicación de negocio', valor: op.explicacionNegocio },
      { etiqueta: 'Nivel de riesgo', valor: op.riesgo },
    ]
  })

  protected readonly itemsFlujo = computed<ClaveValor[]>(() => {
    const op = this.operacion()
    if (!op) return []
    return [
      { etiqueta: 'Flujo asociado', valor: op.flujo.nombre },
      { etiqueta: 'Paso en el flujo', valor: op.flujo.paso },
      { etiqueta: 'Precondición requerida', valor: op.flujo.precondicion },
      { etiqueta: 'Resultado exitoso', valor: op.flujo.exito },
    ]
  })

  protected readonly itemsSistema = computed<ClaveValor[]>(() => {
    const op = this.operacion()
    if (!op) return []
    return [
      { etiqueta: 'Implementación Spring', valor: op.explicacionSistema },
      { etiqueta: 'Exige clave de idempotencia', valor: op.idempotencia ? 'Sí (header / parámetro)' : 'No' },
      { etiqueta: 'Requiere sesión activa', valor: op.pideSesion ? 'Sí (Token JWT)' : 'No' },
    ]
  })

  protected tonoMetodo(m: string): Tono {
    switch (m) {
      case 'GET': return 'info'
      case 'POST': return 'ok'
      case 'PUT': case 'PATCH': return 'aviso'
      case 'DELETE': return 'error'
      default: return 'neutro'
    }
  }

  protected tonoRiesgo(r: string): Tono {
    switch (r) {
      case 'CRITICO': return 'error'
      case 'ALTO': return 'aviso'
      default: return 'neutro'
    }
  }
}
