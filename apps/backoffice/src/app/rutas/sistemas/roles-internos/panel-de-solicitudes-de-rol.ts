import { ChangeDetectionStrategy, Component, computed, output, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { DialogoDeActo } from '../usuarios-internos/dialogo-de-acto'
import { accionesDeRoles, solicitudesDeRol, type SalidaDeRol, type SolicitudDeRol } from '../dominio/gobierno-de-roles'

const TIPO: Readonly<Record<SolicitudDeRol['tipo'], string>> = { CREAR: 'Crear rol', EDITAR: 'Editar rol', RETIRAR: 'Retirar rol' }

/**
 * Las solicitudes a cuatro ojos pendientes (GU-16): lo que una persona propuso y otra tiene que aprobar o rechazar. Quien
 * propuso no puede resolver la suya: el servidor lo rechaza y lo dice (AP-CU135-12). Resuelve sus cuatro estados.
 */
@Component({
  selector: 'ap-panel-de-solicitudes-de-rol',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, DialogoDeActo, EstadoDePantalla],
  template: `
    <section aria-labelledby="titulo-solicitudes" class="panel">
      <h2 id="titulo-solicitudes">Pendientes de una segunda persona</h2>
      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="estaVacio" mensajeVacio="No hay cambios de rol esperando aprobación. Cuando alguien proponga un rol con permisos de riesgo alto o crítico, aparece acá para que otra persona lo resuelva." etiquetaDeCarga="Cargando solicitudes" (reintentar)="recurso.reload()">
        <ul class="lista">
          @for (s of solicitudes(); track s.id) {
            <li>
              <div class="que">
                <strong>{{ tipo(s) }} · {{ s.nombre }}</strong> <code>{{ s.codigo }}</code>
                <span class="sub">{{ s.permisos.length }} {{ s.permisos.length === 1 ? 'permiso' : 'permisos' }} · motivo: {{ s.motivo }}</span>
              </div>
              <div class="acciones">
                <ap-boton variante="primario" tamano="sm" (pulsado)="resolver.set({ s, modo: 'aprobar' })">Aprobar</ap-boton>
                <ap-boton variante="fantasma" tamano="sm" (pulsado)="resolver.set({ s, modo: 'rechazar' })">Rechazar</ap-boton>
              </div>
            </li>
          }
        </ul>
      </ap-estado-de-pantalla>

      @if (resolver(); as r) {
        <ap-dialogo-de-acto [titulo]="(r.modo === 'aprobar' ? 'Aprobar: ' : 'Rechazar: ') + tipo(r.s) + ' ' + r.s.codigo" [textoDeConfirmar]="r.modo === 'aprobar' ? 'Aprobar cambio' : 'Rechazar cambio'" [destructivo]="r.modo === 'rechazar'" [maximoDeMotivo]="160" [efecto]="r.modo === 'aprobar' ? 'El cambio se aplica ahora, con tu nombre como segunda persona.' : 'La propuesta se descarta y no cambia ningún rol.'" [accion]="accion(r)" [abierto]="true" (abiertoChange)="!$event && resolver.set(null)" (completado)="alResolver($event)">
          <p>Permisos: {{ r.s.permisos.join(', ') }}</p>
        </ap-dialogo-de-acto>
      }
    </section>
  `,
  styles: `
    .panel { display: grid; gap: var(--s3); }
    h2 { margin: 0; font: var(--t-titulo-3); }
    .lista { display: grid; gap: var(--s2); margin: 0; padding: 0; list-style: none; }
    li { display: flex; flex-wrap: wrap; gap: var(--s3); align-items: center; justify-content: space-between; padding: var(--s3); border: 1px solid var(--borde-tarjeta); border-radius: var(--r-md); background: var(--fondo-tarjeta); }
    .que { display: grid; gap: var(--s1); min-width: 0; }
    .sub { font: var(--t-ayuda); color: var(--text-2); overflow-wrap: anywhere; }
    code { font-family: var(--f-mono); font: var(--t-ayuda); }
    .acciones { display: flex; gap: var(--s2); }
  `,
})
export class PanelDeSolicitudesDeRol {
  private readonly acciones = accionesDeRoles()
  protected readonly recurso = solicitudesDeRol()
  protected readonly solicitudes = computed(() => (this.recurso.hasValue() ? this.recurso.value() : []))
  protected readonly resolver = signal<{ s: SolicitudDeRol; modo: 'aprobar' | 'rechazar' } | null>(null)
  readonly resuelta = output<SalidaDeRol>()

  protected readonly estaVacio = (l: SolicitudDeRol[]) => l.length === 0
  protected readonly tipo = (s: SolicitudDeRol) => TIPO[s.tipo]
  protected accion(r: { s: SolicitudDeRol; modo: 'aprobar' | 'rechazar' }) {
    return (evidenciaMfa: string, motivo: string) =>
      r.modo === 'aprobar' ? this.acciones.aprobar(r.s.id, { motivo, evidenciaMfa }) : this.acciones.rechazar(r.s.id, { motivo, evidenciaMfa })
  }
  protected alResolver(salida: unknown): void {
    this.recurso.reload()
    this.resuelta.emit(salida as SalidaDeRol)
  }
  /** Para que la pantalla refresque la lista tras proponer algo nuevo. */
  recargar(): void {
    this.recurso.reload()
  }
}
