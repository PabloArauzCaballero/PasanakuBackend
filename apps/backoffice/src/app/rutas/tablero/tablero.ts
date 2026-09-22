import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Sesion } from '../../nucleo/sesion'

type Acceso = { ruta: string; texto: string; permiso: string; descripcion: string }

/**
 * El punto de entrada tras iniciar sesión: a qué domino puede entrar quien opera, según
 * su rol. **Regla cero**: no hay ningún KPI de negocio en esta pantalla porque ningún
 * CU ni la maqueta le da a F6 un contrato de indicadores para el tablero — el tablero
 * con cifras («10 segundos para saber si hay algo que atender», maqueta `operacion/tablero`)
 * es contenido del carril `B1` (F7), no de este shell. Inventar números acá sería
 * exactamente lo que la regla cero prohíbe.
 */
@Component({
  selector: 'ap-tablero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, RouterLink],
  template: `
    <ap-banda-de-proposito texto="Punto de entrada del backoffice: a qué sección podés entrar con tu rol." />
    <main>
      <h1>Tablero</h1>
      <ul class="accesos" data-tutorial-id="tablero-accesos">
        @for (a of accesos; track a.ruta) {
          @if (sesion.puede(a.permiso)) {
            <li>
              <a [routerLink]="['/', a.ruta]" [attr.data-tutorial-id]="'tablero-' + a.ruta">
                <span class="texto">{{ a.texto }}</span>
                <span class="descripcion">{{ a.descripcion }}</span>
              </a>
            </li>
          }
        }
      </ul>
    </main>
  `,
  styles: `
    main { padding: var(--s5); }
    h1 { margin-bottom: var(--s4); }
    .accesos { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: var(--s4); }
    a { display: block; min-height: var(--area-tactil); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); text-decoration: none; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto { display: block; font-weight: 600; color: var(--text); }
    .descripcion { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .9em; }
  `,
})
export class Tablero {
  protected readonly sesion = inject(Sesion)
  protected readonly accesos: Acceso[] = [
    { ruta: 'operacion', texto: 'Operación', permiso: 'ver:operacion', descripcion: 'Billetera, cobranza, entregas y cierre.' },
    { ruta: 'operacion/estado', texto: 'Arquitectura y estado', permiso: 'ver:operacion', descripcion: 'Nivel de criticidad y réplicas por servicio.' },
    { ruta: 'cumplimiento', texto: 'Cumplimiento', permiso: 'ver:cumplimiento', descripcion: 'UIF, alertas, casos y gobierno.' },
    { ruta: 'contabilidad', texto: 'Contabilidad', permiso: 'ver:contabilidad', descripcion: 'Períodos, presupuestos y estados financieros.' },
    { ruta: 'publicidad', texto: 'Publicidad', permiso: 'ver:publicidad', descripcion: 'Campañas y ofertas.' },
    { ruta: 'sistemas', texto: 'Sistemas', permiso: 'ver:sistemas', descripcion: 'Salud de servicios, despliegues y respaldos.' },
  ]
}
