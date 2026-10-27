import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { textosOperacion } from '../textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * D-15 · solicitudes de ingreso que el organizador dejó vencer (48 horas).
 *
 * **Sin contrato (2026-10-26, franja B).** Ni `grupos.yaml` ni el simulado (`packages/simulado/generado/prism/todos.yaml`)
 * publican una cola de solicitudes escaladas: solo `listarSolicitudesDeIngreso` (las pendientes de UN grupo),
 * `resolverAdmision` y `historialAdmision` por solicitud. La pantalla pedía `GET /grupos/solicitudes-escaladas`, que
 * responde 404, y mostraba aceptar/rechazar sin enviar nada. Ahora no pide nada: dice que la cola no existe y lleva al
 * camino que sí existe (Admisiones, por identificador). Cuando `grupos` publique la operación, esta vista se vuelve
 * un listado con el kit.
 */
@Component({
  selector: 'ap-cola-de-solicitudes-escaladas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, EstadoVacio],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Operación · D-15" [titulo]="t.titulo" [descripcion]="t.proposito" icono="personaMas" />
      <ap-estado-vacio icono="enlace" [titulo]="t.sinContrato" [explicacion]="t.sinContratoExplicacion" />
      <section class="tarjeta" [attr.aria-label]="t.abrirTitulo">
        <h2>{{ t.abrirTitulo }}</h2>
        @if (puedeAdmisiones()) {
          <form (submit)="$event.preventDefault(); abrir()" novalidate>
            <ap-campo [etiqueta]="t.solicitudEtiqueta" [ayuda]="t.solicitudAyuda" [error]="error()" [(valor)]="solicitud" />
            <div><ap-boton variante="primario" tipo="submit" icono="externo">{{ t.abrir }}</ap-boton></div>
          </form>
        } @else {
          <ap-alerta tono="info">{{ t.sinAdmisiones }}</ap-alerta>
        }
      </section>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .tarjeta { display: grid; gap: var(--s4); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    h2 { margin: 0; font: var(--t-titulo-3); color: var(--text); }
    form { display: grid; gap: var(--s3); }
    @media (min-width: 48rem) { form { grid-template-columns: 1fr auto; align-items: start; } form > div { padding-top: var(--s5); } }
  `,
})
export class ColaDeSolicitudesEscaladas {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  protected readonly t = textosOperacion.solicitudesEscaladas
  /** La interfaz no autoriza: solo evita ofrecer un enlace que `ver:admisiones` rebotaría. Admisiones vuelve a chequear. */
  protected readonly puedeAdmisiones = computed(() => this.sesion.puede('ver:admisiones'))
  protected readonly solicitud = signal('')
  protected readonly error = signal<string | undefined>(undefined)

  protected abrir(): void {
    const id = this.solicitud().trim().toLowerCase()
    if (!UUID.test(id)) {
      this.error.set(this.t.solicitudError)
      return
    }
    this.error.set(undefined)
    void this.router.navigate(['/admisiones', id])
  }
}
