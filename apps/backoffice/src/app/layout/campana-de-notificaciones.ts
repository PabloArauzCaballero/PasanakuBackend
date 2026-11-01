import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { AvisosInternosServicio } from '../nucleo/avisos-internos'

/**
 * H5.S0 · Campana de notificaciones y avisos internos para operadores.
 * Muestra el globo con el conteo de no leídos, el panel desplegable con la lista
 * de avisos y permite marcar avisos individuales o todos como leídos.
 *
 * El panel resuelve sus cuatro estados (95.2): cargando, con avisos, vacío y error con reintento — antes un 500 se
 * pintaba como «no hay avisos», que es mentir. Cada acción lleva ícono y texto; «Ver detalle» solo para enlaces
 * internos (empiezan con `/`): un enlace de otro origen no se sigue desde acá.
 */
@Component({
  selector: 'ap-campana-de-notificaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '(document:click)': 'clicAfuera($event)', '(keydown.escape)': 'abierto.set(false)' },
  template: `
    <button
      type="button"
      class="disparador"
      [attr.aria-expanded]="abierto()"
      [attr.aria-controls]="id"
      [attr.aria-label]="etiquetaAria()"
      (click)="alternar()">
      <ap-icono nombre="campana" tamano="s5" />
      @if (noLeidos() > 0) {
        <span class="insignia" aria-hidden="true">{{ noLeidos() > 99 ? '99+' : noLeidos() }}</span>
      }
    </button>

    @if (abierto()) {
      <div [id]="id" class="panel" role="region" aria-label="Avisos del sistema">
        <div class="cabecera-panel">
          <div class="titulo-con-conteo">
            <strong>Avisos internos</strong>
            @if (noLeidos() > 0) {
              <span class="conteo-texto">({{ noLeidos() }} sin leer)</span>
            }
          </div>
          @if (noLeidos() > 0) {
            <button type="button" class="accion-todos" (click)="marcarTodos()"><ap-icono nombre="aprobar" tamano="s4" />Marcar leídos</button>
          }
        </div>

        @if (servicio.error() && !avisos().length) {
          <div class="estado-panel" role="alert">
            <ap-icono nombre="advertencia" tamano="s6" />
            <p>No pudimos traer los avisos. Puede ser la conexión o el servicio de notificaciones.</p>
            <button type="button" class="accion-todos" (click)="recargar()"><ap-icono nombre="deshacer" tamano="s4" />Volver a intentar</button>
          </div>
        } @else if (servicio.cargando() && !avisos().length) {
          <p class="estado-panel" role="status">Cargando avisos…</p>
        } @else {
        <ul class="lista-avisos" role="list">
          @for (aviso of avisos(); track aviso.id) {
            <li class="item-aviso" [class.no-leido]="!aviso.leidoEn">
              <div class="cuerpo-aviso">
                <div class="fila-superior">
                  <span class="tipo-tag" [attr.data-tipo]="aviso.tipo">{{ aviso.tipo }}</span>
                  <time class="hora">{{ formatearHora(aviso.creadoEn) }}</time>
                </div>
                <strong class="titulo-aviso">{{ aviso.titulo }}</strong>
                <p class="mensaje-aviso">{{ aviso.mensaje }}</p>
                <div class="acciones-item">
                  @if (aviso.enlace?.startsWith('/')) {
                    <button type="button" class="enlace-accion" (click)="navegar(aviso.enlace!)"><ap-icono nombre="flechaDerecha" tamano="s4" />Ver detalle</button>
                  }
                  @if (!aviso.leidoEn) {
                    <button type="button" class="accion-leido" (click)="marcarUno(aviso.id)"><ap-icono nombre="ojo" tamano="s4" />Marcar leído</button>
                  }
                </div>
              </div>
            </li>
          } @empty {
            <li class="sin-avisos">
              <ap-icono nombre="verificado" tamano="s6" />
              <p>No tenés avisos pendientes. Cuando el sistema necesite tu atención, aparece acá.</p>
            </li>
          }
        </ul>
        }
      </div>
    }
  `,
  styles: `
    :host { position: relative; display: inline-block; }
    .disparador { position: relative; display: inline-flex; align-items: center; justify-content: center; min-width: var(--area-tactil); min-height: var(--area-tactil); border: var(--borde-fino) solid transparent; border-radius: var(--r-pill); background: transparent; color: var(--text); cursor: pointer; }
    .disparador:hover { background: var(--surface-2); }
    .disparador:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .insignia { position: absolute; top: var(--s1); right: var(--s1); min-width: var(--s4); height: var(--s4); padding: 0 var(--s1); border-radius: var(--r-pill); background: var(--danger-surface); color: var(--danger-ink); font: var(--t-etiqueta); line-height: var(--s4); text-align: center; font-weight: 700; }
    .panel { position: absolute; right: 0; top: calc(100% + var(--s1)); z-index: 35; width: calc(var(--s7) * 5.5); max-height: calc(var(--s7) * 7); overflow-y: auto; margin: 0; padding: 0; border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-3); }
    .cabecera-panel { display: flex; align-items: center; justify-content: space-between; padding: var(--s2) var(--s3); border-bottom: var(--borde-fino) solid var(--border); background: var(--surface-2); font: var(--t-cuerpo-chico); }
    .titulo-con-conteo { display: flex; align-items: center; gap: var(--s1); }
    .conteo-texto { color: var(--text-3); }
    .estado-panel { display: grid; justify-items: center; gap: var(--s2); margin: 0; padding: var(--s5) var(--s3); color: var(--text-2); font: var(--t-cuerpo-chico); text-align: center; }
    .accion-todos, .enlace-accion, .accion-leido { display: inline-flex; align-items: center; gap: var(--s1); }
    .accion-todos { border: 0; background: transparent; color: var(--brand); font: var(--t-etiqueta); cursor: pointer; text-decoration: underline; }
    .accion-todos:hover { color: var(--text); }
    .lista-avisos { list-style: none; margin: 0; padding: 0; }
    .item-aviso { padding: var(--s3); border-bottom: var(--borde-fino) solid var(--border); transition: background .15s ease; }
    .item-aviso:last-child { border-bottom: 0; }
    .item-aviso.no-leido { background: var(--surface-2); }
    .item-aviso:hover { background: var(--surface-3); }
    .fila-superior { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--s1); }
    .tipo-tag { font: var(--t-etiqueta); font-weight: 600; text-transform: uppercase; color: var(--text-3); }
    .hora { font: var(--t-etiqueta); color: var(--text-3); }
    .titulo-aviso { display: block; font: var(--t-cuerpo-chico); color: var(--text); margin-bottom: var(--s1); }
    .mensaje-aviso { margin: 0 0 var(--s2); font: var(--t-etiqueta); color: var(--text-2); line-height: 1.4; }
    .acciones-item { display: flex; gap: var(--s2); align-items: center; }
    .enlace-accion, .accion-leido { border: 0; background: transparent; padding: 0; font: var(--t-etiqueta); cursor: pointer; color: var(--brand); }
    .accion-leido { color: var(--text-3); text-decoration: underline; }
    .enlace-accion:hover, .accion-leido:hover { color: var(--text); }
    .sin-avisos { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: var(--s2); padding: var(--s5) var(--s3); color: var(--text-3); font: var(--t-cuerpo-chico); text-align: center; }
  `,
})
export class CampanaDeNotificaciones {
  private static secuencia = 0
  readonly id = `ap-campana-${++CampanaDeNotificaciones.secuencia}`

  protected readonly servicio = inject(AvisosInternosServicio)
  private readonly router = inject(Router)
  private readonly el = inject(ElementRef)

  readonly abierto = signal(false)
  readonly avisos = this.servicio.avisos
  readonly noLeidos = this.servicio.noLeidos

  readonly etiquetaAria = computed(() => {
    const n = this.noLeidos()
    return n > 0 ? `Avisos del sistema, ${n} no leídos` : 'Avisos del sistema'
  })

  alternar(): void {
    const proximo = !this.abierto()
    this.abierto.set(proximo)
    if (proximo) {
      this.servicio.cargar().subscribe({ error: () => undefined })
    }
  }

  clicAfuera(e: Event): void {
    if (this.abierto() && e.target instanceof Node && !this.el.nativeElement.contains(e.target)) {
      this.abierto.set(false)
    }
  }

  recargar(): void {
    this.servicio.cargar().subscribe({ error: () => undefined })
  }

  marcarUno(id: string): void {
    this.servicio.marcarLeido(id).subscribe({ error: () => undefined })
  }

  marcarTodos(): void {
    this.servicio.marcarTodosLeidos().subscribe({ error: () => undefined })
  }

  navegar(enlace: string): void {
    this.abierto.set(false)
    void this.router.navigateByUrl(enlace)
  }

  formatearHora(fechaIso: string): string {
    try {
      const f = new Date(fechaIso)
      return f.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    } catch {
      return ''
    }
  }
}
