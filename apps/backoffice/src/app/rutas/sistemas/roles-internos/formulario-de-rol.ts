import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Campo } from '@aportaya/ui/campo/campo'
import { Casilla } from '@aportaya/ui/casilla/casilla'
import type { PermisoCatalogo } from 'clientes/angular/identidad'
import type { ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import { RIESGO_DE_CUATRO_OJOS } from '../dominio/gobierno-de-roles'

/**
 * Los campos propios de crear o editar un rol personalizado (GU-16). Presentacional: no trae datos ni llama a nadie;
 * quien lo usa le da el catálogo de permisos y recibe lo escrito por `model`. El riesgo de cada permiso se dice con
 * texto, no solo con color (regla 95.4.6): los de riesgo alto o crítico piden a una segunda persona.
 */
@Component({
  selector: 'ap-formulario-de-rol',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Campo, Casilla],
  template: `
    <div class="campos">
      @if (modo() === 'crear') {
        <ap-campo etiqueta="Código del rol" [(valor)]="codigo" ayuda="En mayúsculas, sin espacios; de 3 a 30 caracteres. No puede empezar con SERVICIO_." [error]="errorDe('rol')" />
      }
      <ap-campo etiqueta="Nombre" [(valor)]="nombre" />
      <ap-area-de-texto etiqueta="Descripción (opcional)" [(valor)]="descripcion" [maximo]="255" />
      <fieldset class="permisos">
        <legend>Permisos del rol</legend>
        @if (errorDe('rol') && modo() === 'editar') { <p class="error" role="alert">{{ errorDe('rol') }}</p> }
        <ul>
          @for (p of catalogo(); track p.codigo) {
            <li>
              <ap-casilla [marcada]="seleccion().includes(p.codigo)" (marcadaChange)="alternar(p.codigo, $event)">
                <code>{{ p.codigo }}</code>
                @if (pideSegundaPersona(p)) { <span class="riesgo">· riesgo {{ p.nivelRiesgo === 'CRITICO' ? 'crítico' : 'alto' }}: pide a una segunda persona</span> }
              </ap-casilla>
            </li>
          } @empty {
            <li class="ayuda">El catálogo de permisos no está disponible. Cerrá y volvé a abrir para reintentar.</li>
          }
        </ul>
      </fieldset>
    </div>
  `,
  styles: `
    .campos { display: grid; gap: var(--s3); }
    .permisos { margin: 0; padding: var(--s2) var(--s3); border: var(--borde-fino) solid var(--borde-tarjeta); border-radius: var(--r-md); min-width: 0; }
    legend { padding: 0 var(--s1); font-weight: 600; color: var(--text-2); }
    ul { display: grid; gap: var(--s1); margin: 0; padding: 0; list-style: none; max-height: 16rem; overflow: auto; }
    code { font-family: var(--f-mono); font: var(--t-ayuda); overflow-wrap: anywhere; }
    .riesgo, .ayuda { font: var(--t-ayuda); color: var(--text-2); }
    .error { margin: 0 0 var(--s2); font: var(--t-ayuda); color: var(--text); font-weight: 600; }
  `,
})
export class FormularioDeRol {
  readonly modo = input.required<'crear' | 'editar'>()
  readonly catalogo = input.required<readonly PermisoCatalogo[]>()
  readonly error = input<ErrorDeGobierno | null>(null)
  readonly codigo = model('')
  readonly nombre = model('')
  readonly descripcion = model('')
  readonly seleccion = model<string[]>([])

  protected errorDe(campo: 'rol'): string | undefined {
    const e = this.error()
    return e?.campo === campo ? e.mensaje : undefined
  }
  protected pideSegundaPersona(p: PermisoCatalogo): boolean {
    return RIESGO_DE_CUATRO_OJOS.includes(p.nivelRiesgo)
  }
  protected alternar(codigo: string, marcada: boolean): void {
    this.seleccion.update((s) => (marcada ? [...new Set([...s, codigo])] : s.filter((c) => c !== codigo)))
  }
}
