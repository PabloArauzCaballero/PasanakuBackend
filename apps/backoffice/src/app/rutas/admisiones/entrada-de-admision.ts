import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { SolicitudesDelGrupo } from './solicitudes-del-grupo'
import { textosAdmisiones } from './textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * Puerta de entrada a las solicitudes de ingreso, en una tarjeta con dos pestañas:
 * - **Por grupo**: las pendientes de un grupo (`listarSolicitudesDeIngreso`), con «Resolver» en cada fila. El grupo
 *   queda en la URL (`?grupo=`) para poder pegar el enlace.
 * - **Por solicitud**: abrir una por su identificador, el que llega en el aviso.
 * El contrato no tiene un listado de pendientes de **todos** los grupos: por eso se elige el grupo primero.
 */
@Component({
  selector: 'ap-entrada-de-admision',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, Campo, Pestanas, SolicitudesDelGrupo],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Admisiones" [titulo]="t.buscarTitulo" [descripcion]="t.buscarProposito" icono="personaMas" />
      <div class="tarjeta">
        <ap-pestanas [etiqueta]="t.formasDeAbrir" [pestanas]="pestanas" [elegida]="vista()" (elegidaChange)="cambiarVista($event)" />
        <div role="tabpanel" [id]="'panel-' + vista()" [attr.aria-labelledby]="'pestana-' + vista()" class="panel">
          @if (vista() === 'grupo') {
            <form class="fila" (submit)="$event.preventDefault(); verGrupo()" novalidate>
              <ap-campo [etiqueta]="t.grupoEtiqueta" [ayuda]="t.grupoAyuda" [error]="errorGrupo()" [(valor)]="grupo" />
              <ap-boton variante="primario" tipo="submit" icono="buscar">{{ t.grupoAccion }}</ap-boton>
            </form>
            @if (grupoEnUrl(); as g) { <ap-solicitudes-del-grupo [grupoId]="g" /> } @else { <p class="aviso">{{ t.grupoSinElegir }}</p> }
          } @else {
            <p class="aviso">{{ t.buscarSinListado }}</p>
            <form class="fila" (submit)="$event.preventDefault(); abrir()" novalidate>
              <ap-campo [etiqueta]="t.buscarEtiqueta" [ayuda]="t.buscarAyuda" [error]="error()" [(valor)]="id" />
              <ap-boton variante="primario" tipo="submit" icono="externo">{{ t.buscarAccion }}</ap-boton>
            </form>
          }
        </div>
      </div>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .tarjeta { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); min-width: 0; }
    .panel { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .fila { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: start; gap: var(--s3); }
    .fila ap-boton { margin-top: var(--s5); }
    .aviso { margin: 0; color: var(--text-2); }
    @media (max-width: 40rem) { .fila { grid-template-columns: 1fr; } .fila ap-boton { margin-top: 0; } }
  `,
})
export class EntradaDeAdmision {
  protected readonly t = textosAdmisiones
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)
  protected readonly pestanas: Pestana[] = [
    { valor: 'grupo', texto: textosAdmisiones.porGrupo },
    { valor: 'solicitud', texto: textosAdmisiones.porSolicitud },
  ]
  protected readonly vista = computed(() => (this.consulta()?.get('vista') === 'solicitud' ? 'solicitud' : 'grupo'))
  protected readonly grupoEnUrl = computed(() => {
    const g = this.consulta()?.get('grupo') ?? ''
    return UUID.test(g) ? g.toLowerCase() : ''
  })
  protected readonly grupo = signal(this.grupoEnUrl())
  protected readonly errorGrupo = signal('')
  protected readonly id = signal('')
  protected readonly error = signal('')

  constructor() {
    inject(ExplicadorDeVista).fijar(textosAdmisiones.buscarProposito)
  }

  protected cambiarVista(vista: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista: vista === 'grupo' ? null : vista }, queryParamsHandling: 'merge' })
  }

  protected verGrupo(): void {
    const valor = this.grupo().trim()
    if (!UUID.test(valor)) {
      this.errorGrupo.set(this.t.grupoError)
      return
    }
    this.errorGrupo.set('')
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { grupo: valor.toLowerCase() }, queryParamsHandling: 'merge' })
  }

  protected abrir(): void {
    const valor = this.id().trim()
    if (!UUID.test(valor)) {
      this.error.set(this.t.buscarError)
      return
    }
    this.error.set('')
    void this.router.navigate(['/admisiones', valor.toLowerCase()])
  }
}
