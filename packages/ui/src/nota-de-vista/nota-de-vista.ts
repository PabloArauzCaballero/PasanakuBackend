import { ChangeDetectionStrategy, Component, OnInit, input, model } from '@angular/core'
import { Icono } from '../icono/icono'

/**
 * «Notas de la vista»: qué muestra esta pantalla y cómo se usa, en una línea plegada que solo
 * se vuelve superficie al abrirla. Reemplaza a la banda de propósito siempre abierta: una franja
 * de color debajo del título en todas las vistas pintaba de aviso la zona más ancha de la pantalla
 * en pantallas donde no pasa nada (así lo resolvió el portal de Atlas).
 *
 * El chevron sustituye a «Ver/Ocultar»: su dirección ya dice si está plegada. Con `clave`, el
 * estado se recuerda por sesión de navegación (si el almacenamiento falla, no pasa nada).
 */
@Component({
  selector: 'ap-nota-de-vista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '[class.abierta]': 'abierta()' },
  template: `
    <button type="button" [attr.aria-expanded]="abierta()" [attr.aria-controls]="id" (click)="alternar()">
      <ap-icono nombre="bombilla" tamano="s4" class="bombilla" />
      <span class="etiqueta">{{ etiqueta() }}</span>
      <ap-icono nombre="chevronAbajo" tamano="s4" class="chevron" />
    </button>
    @if (abierta()) {
      <div class="cuerpo" [id]="id"><ng-content /></div>
    }
  `,
  styles: `
    :host { display: block; border: var(--borde-fino) solid transparent; border-radius: var(--r-lg); transition: border-color var(--mov-micro) ease-out, background-color var(--mov-micro) ease-out; }
    :host(.abierta) { border-color: var(--border); background: var(--surface); }
    button { display: flex; width: 100%; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: 0; border-radius: var(--r-lg); background: transparent; color: var(--text-3); font: var(--t-campo-etiqueta); text-align: left; cursor: pointer; }
    button:hover { background: var(--surface-2); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .bombilla, .chevron { color: var(--brand-texto); flex: none; }
    .chevron { margin-left: auto; transition: transform var(--mov-entrada) var(--curva-llega); }
    :host(.abierta) .chevron { transform: rotate(180deg); }
    .cuerpo { padding: var(--s3) var(--s4); border-top: var(--borde-fino) solid var(--border); font: var(--t-cuerpo-chico); color: var(--text); max-width: 80ch; }
  `,
})
export class NotaDeVista implements OnInit {
  private static secuencia = 0
  readonly id = `ap-nota-de-vista-${++NotaDeVista.secuencia}`
  readonly etiqueta = input('Notas de la vista')
  /** Con clave, el estado abierto/cerrado sobrevive a la navegación dentro de la sesión. */
  readonly clave = input<string>()
  readonly abierta = model(false)

  ngOnInit(): void {
    const clave = this.clave()
    if (clave) this.abierta.set(leer(clave))
  }

  alternar(): void {
    this.abierta.set(!this.abierta())
    const clave = this.clave()
    if (clave) guardar(clave, this.abierta())
  }
}

const PREFIJO = 'ap-nota-de-vista:'

function leer(clave: string): boolean {
  try {
    return sessionStorage.getItem(PREFIJO + clave) === '1'
  } catch {
    return false
  }
}

function guardar(clave: string, abierta: boolean): void {
  try {
    sessionStorage.setItem(PREFIJO + clave, abierta ? '1' : '0')
  } catch {
    // Sin almacenamiento (modo privado, cuota): la nota sigue funcionando, solo no recuerda.
  }
}
