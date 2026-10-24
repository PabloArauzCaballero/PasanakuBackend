import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { ResumenDelCatalogoRemoto, clasificacionDe, fichaRemota } from './dominio'
import { ESTADOS, REVISIONES, gobiernoDe, tonoDeEstado, tonoDeRevision } from './gobierno'
import { textosCatalogo } from './textos'
import { VistaColumnas } from './vista-columnas'
import { VistaGeneral } from './vista-general'
import { VistaGobierno } from './vista-gobierno'
import { VistaOperaciones } from './vista-operaciones'
import { VistaRelaciones } from './vista-relaciones'

const VISTAS = ['general', 'columnas', 'operaciones', 'relaciones', 'gobierno'] as const
type Vista = (typeof VISTAS)[number]

/**
 * La ficha de una tabla del catálogo, como la del portal de Atlas: cabecera con `esquema.tabla`, su
 * propósito y las insignias de datos personales, revisión y estado; «Editar ficha» para quien puede;
 * y cinco pestañas (Vista general · Columnas · Relaciones · Operaciones · Gobierno) cuyo estado vive en `?vista=`.
 * `esquema` y `tabla` llegan del path (`withComponentInputBinding`): el nombre solo no alcanza, porque la
 * infraestructura de mensajería repite nombres en cada esquema. Se pide solo esta tabla, no el catálogo entero.
 */
@Component({
  selector: 'ap-ficha-de-tabla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, EstadoDePantalla, Pestanas, RouterLink, VistaColumnas, VistaGeneral, VistaGobierno, VistaOperaciones, VistaRelaciones],
  template: `
    <ap-estado-de-pantalla [recurso]="remoto" [mensajeVacio]="t.noExiste(esquema() + '.' + tabla())" [etiquetaDeCarga]="textos.cargando" (reintentar)="remoto.reload()">
      @if (ficha(); as f) {
        <ap-cabecera-de-pagina icono="baseDeDatos" [eyebrow]="t.eyebrow" [titulo]="f.esquema + '.' + f.tabla" [descripcion]="f.negocio.queEs ?? t.sinDescripcion">
          @if (puedeEditar()) { <ap-boton acciones variante="primario" tamano="sm" (pulsado)="editar()">{{ t.editar }}</ap-boton> }
          @if (personales()) { <ap-chip-estado acciones tono="pii">{{ textos.columnasLista.personales }}</ap-chip-estado> }
          <ap-chip-estado acciones [tono]="tonoRevision()">{{ revision() }}</ap-chip-estado>
          <ap-chip-estado acciones [tono]="tonoEstado()">{{ estado() }}</ap-chip-estado>
        </ap-cabecera-de-pagina>
        <ap-pestanas [etiqueta]="t.etiquetaDeVistas" estilo="segmentada" [pestanas]="pestanas()" [elegida]="vista()" (elegidaChange)="irA($event)" />
        <div role="tabpanel" [id]="'panel-' + vista()" [attr.aria-labelledby]="'pestana-' + vista()">
          @switch (vista()) {
            @case ('columnas') { <ap-vista-columnas [tabla]="f" (cambio)="remoto.reload()" /> }
            @case ('relaciones') { <ap-vista-relaciones [tabla]="f" /> }
            @case ('operaciones') { <ap-vista-operaciones [tabla]="f" /> }
            @case ('gobierno') { <ap-vista-gobierno [tabla]="f" [politicas]="politicas()" /> }
            @default { <ap-vista-general [tabla]="f" [politicas]="politicas()" /> }
          }
        </div>
      } @else {
        <p role="status" class="nota">{{ t.noExiste(esquema() + '.' + tabla()) }}</p>
        <a routerLink="/tablero/catalogo-de-datos" class="volver">{{ t.volver }}</a>
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    ap-estado-de-pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .nota { margin: 0; color: var(--text); }
    .volver { justify-self: start; min-height: var(--area-tactil); display: inline-flex; align-items: center; color: var(--brand-texto); }
    .volver:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class FichaDeTabla {
  readonly esquema = input.required<string>()
  readonly tabla = input.required<string>()
  protected readonly textos = textosCatalogo
  protected readonly t = textosCatalogo.ficha
  protected readonly remoto = fichaRemota(() => this.esquema(), () => this.tabla())
  private readonly resumen = inject(ResumenDelCatalogoRemoto)
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  constructor() {
    inject(ExplicadorDeVista).fijar(textosCatalogo.proposito)
  }

  protected readonly ficha = computed(() => (this.remoto.hasValue() ? this.remoto.value() : undefined))
  protected readonly politicas = computed(() => (this.resumen.recurso.hasValue() ? (this.resumen.recurso.value()?.politicasDeRetencion ?? []) : []))
  protected readonly puedeEditar = computed(() => this.sesion.puede('CATALOGO_DATOS_EDITAR'))
  protected readonly personales = computed(() => {
    const f = this.ficha()
    if (!f) return false
    const c = clasificacionDe(f)
    return c.personales + c.secretos > 0
  })
  private readonly gobierno = computed(() => {
    const f = this.ficha()
    return f ? gobiernoDe(f) : undefined
  })
  protected readonly estado = computed(() => ESTADOS[this.gobierno()?.estado ?? ''] ?? '')
  protected readonly tonoEstado = computed(() => tonoDeEstado(this.gobierno()?.estado ?? ''))
  protected readonly revision = computed(() => REVISIONES[this.gobierno()?.estadoRevision ?? ''] ?? '')
  protected readonly tonoRevision = computed(() => tonoDeRevision(this.gobierno()?.estadoRevision ?? ''))
  /** Un valor desconocido o ausente en la URL cae en la primera pestaña: un enlace viejo nunca deja la ficha en blanco. */
  protected readonly vista = computed<Vista>(() => {
    const v = this.consulta()?.get('vista') ?? ''
    return (VISTAS as readonly string[]).includes(v) ? (v as Vista) : 'general'
  })
  protected readonly pestanas = computed<Pestana[]>(() => {
    const f = this.ficha()
    return [
      { valor: 'general', texto: this.t.vistas.general },
      { valor: 'columnas', texto: this.t.vistas.columnas, cuenta: f?.columnas.length },
      // Sin contador: sumaría casos de uso mientras la primera sección habla de operaciones directas y se contradirían.
      { valor: 'operaciones', texto: this.t.vistas.operaciones },
      { valor: 'relaciones', texto: this.t.vistas.relaciones, cuenta: f ? f.relaciones.salientes.length + f.relaciones.entrantes.length : undefined },
      { valor: 'gobierno', texto: this.t.vistas.gobierno },
    ]
  })

  protected irA(vista: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista: vista === 'general' ? null : vista }, queryParamsHandling: 'merge' })
  }

  protected editar(): void {
    void this.router.navigate(['ficha'], { relativeTo: this.ruta })
  }
}
