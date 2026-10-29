import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Icono } from '@aportaya/ui/icono/icono'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { esPendiente, expedienteDe, tonoDelExpediente, vencimientoDe } from '../dominio/cu02-expedientes'
import { estadoEnPalabras, explicacionDelEstado, fotosCompletas, fotosEnNumeros, vencimientoEnPalabras } from '../dominio/cu02-presentacion'
import { textosVerificaciones } from './textos-verificaciones'
import { VistaComoFunciona } from './vista-como-funciona'
import { VistaDecisionDelExpediente } from './vista-decision-del-expediente'
import { VistaFotosDelExpediente } from './vista-fotos-del-expediente'
import { VistaResumenDelExpediente } from './vista-resumen-del-expediente'

const VISTAS = ['resumen', 'fotos', 'decision', 'como-funciona'] as const
type Vista = (typeof VISTAS)[number]
const T = textosVerificaciones.ficha

/**
 * CU-02 · la ficha de un expediente de identidad: **una tarjeta a lo ancho con pestañas** (Resumen · Documento y
 * fotos · Decisión · Cómo funciona), con la pestaña en `?vista=` para que un enlace lleve al mismo lugar. Cabecera con
 * el nombre, lo que significa su estado y tres insignias (estado, vigencia, fotos n de 5) que se leen sin entrar.
 * `verificacionId` llega del path (`withComponentInputBinding`).
 */
@Component({
  selector: 'ap-ficha-de-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, EstadoDePantalla, EstadoVacio, Icono, Pestanas, RouterLink, VistaComoFunciona, VistaDecisionDelExpediente, VistaFotosDelExpediente, VistaResumenDelExpediente],
  template: `
    <section class="pantalla">
      <a class="volver" routerLink=".."><ap-icono nombre="flechaIzquierda" tamano="s4" />{{ t.volver }}</a>
      <ap-estado-de-pantalla [recurso]="remoto.recurso" [mensajeVacio]="t.noEncontrado" [etiquetaDeCarga]="t.cargando" (reintentar)="remoto.recurso.reload()">
        @if (remoto.expediente(); as e) {
          <ap-cabecera-de-pagina [eyebrow]="t.eyebrow" [titulo]="e.nombreCompleto" [descripcion]="explicacion(e.estado)">
            <ap-chip-estado acciones [tono]="tono(e)">{{ estado(e.estado) }}</ap-chip-estado>
            <ap-chip-estado acciones [tono]="vence(e) === 'vigente' ? 'ok' : 'aviso'">Documento: {{ vigencia(e) }}</ap-chip-estado>
            <ap-chip-estado acciones [tono]="completas(e) ? 'ok' : 'aviso'" [icono]="completas(e) ? 'imagen' : 'advertencia'">Fotos: {{ enNumeros(e) }}</ap-chip-estado>
          </ap-cabecera-de-pagina>
          <article class="tarjeta">
            <ap-pestanas [etiqueta]="t.etiquetaDeVistas" estilo="segmentada" [pestanas]="pestanas()" [elegida]="vista()" (elegidaChange)="irA($event)" />
            <div role="tabpanel" class="panel" [id]="'panel-' + vista()" [attr.aria-labelledby]="'pestana-' + vista()">
              @switch (vista()) {
                @case ('fotos') { <ap-vista-fotos-del-expediente [expediente]="e" /> }
                @case ('decision') { <ap-vista-decision-del-expediente [expediente]="e" (resuelto)="remoto.recurso.reload()" /> }
                @case ('como-funciona') { <ap-vista-como-funciona /> }
                @default { <ap-vista-resumen-del-expediente [expediente]="e" /> }
              }
            </div>
          </article>
        } @else {
          <ap-estado-vacio motivo="porFiltro" [titulo]="t.noEncontrado" [explicacion]="t.noEncontradoExplicacion" [accionPropia]="t.volver" (actuar)="volver()" />
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    ap-estado-de-pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .volver { justify-self: start; display: inline-flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); color: var(--brand-texto); font: var(--t-cuerpo-chico); font-weight: 600; text-decoration: none; }
    .volver:hover { text-decoration: underline; }
    .volver:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    /* Una sola tarjeta a lo ancho: las pestañas arriba y el panel debajo, nunca bloques apilados con una columna vacía al lado. */
    .tarjeta { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .panel { min-width: 0; }
  `,
})
export class FichaDeExpediente {
  readonly verificacionId = input.required<string>()
  protected readonly t = T
  protected readonly remoto = expedienteDe(this.verificacionId)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)
  protected readonly estado = estadoEnPalabras
  protected readonly explicacion = explicacionDelEstado
  protected readonly tono = tonoDelExpediente
  protected readonly vence = vencimientoDe
  protected readonly vigencia = vencimientoEnPalabras
  protected readonly completas = fotosCompletas
  protected readonly enNumeros = fotosEnNumeros

  constructor() {
    inject(ExplicadorDeVista).fijar(textosVerificaciones.notaDeVista)
  }

  /** Un valor desconocido en la URL cae en Resumen: un enlace viejo nunca deja la ficha en blanco. */
  protected readonly vista = computed<Vista>(() => {
    const v = this.consulta()?.get('vista') ?? ''
    return (VISTAS as readonly string[]).includes(v) ? (v as Vista) : 'resumen'
  })
  protected readonly pestanas = computed<Pestana[]>(() => {
    const e = this.remoto.expediente()
    return [
      { valor: 'resumen', texto: T.vistas.resumen },
      { valor: 'fotos', texto: T.vistas.fotos, cuenta: e?.fotos.length },
      { valor: 'decision', texto: e && esPendiente(e) ? T.vistas.decision + ' pendiente' : T.vistas.decision },
      { valor: 'como-funciona', texto: T.vistas.comoFunciona },
    ]
  })

  protected irA(vista: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista: vista === 'resumen' ? null : vista }, queryParamsHandling: 'merge' })
  }

  protected volver(): void {
    void this.router.navigate(['..'], { relativeTo: this.ruta })
  }
}
