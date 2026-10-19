import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterLink, RouterLinkActive } from '@angular/router'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from './acceso'

/** Cada entrada lleva los MISMOS códigos que su ruta en `cumplimiento.routes.ts`: lo que no se puede abrir, no se ofrece. */
export const ENTRADAS_DE_CUMPLIMIENTO: readonly { ruta: string; texto: string; codigos: readonly string[] }[] = [
  { ruta: '/cumplimiento/verificaciones', texto: 'Identidad', codigos: ['VERIFICACION_RESOLVER'] },
  { ruta: '/cumplimiento/alertas', texto: 'Alertas', codigos: ['CUMPLIMIENTO_ALERTAS'] },
  { ruta: '/cumplimiento/casos', texto: 'Casos', codigos: ['CUMPLIMIENTO_CASOS'] },
  { ruta: '/cumplimiento/uif/reportes', texto: 'Reportes UIF', codigos: ['CUMPLIMIENTO_REPORTAR', 'AUDITORIA_LEER'] },
  { ruta: '/cumplimiento/uif/requerimientos', texto: 'Requerimientos', codigos: ['OFICIAL_CUMPLIMIENTO', 'AUDITORIA_LEER'] },
  { ruta: '/cumplimiento/continuidad', texto: 'Continuidad', codigos: ['RESPONSABLE_RIESGOS', 'AUDITORIA_LEER'] },
  { ruta: '/cumplimiento/actas', texto: 'Actas de comité', codigos: ['OFICIAL_CUMPLIMIENTO', 'AUDITORIA_LEER'] },
  { ruta: '/cumplimiento/riesgos/eventos', texto: 'Eventos de riesgo', codigos: ['RESPONSABLE_RIESGOS', 'AUDITORIA_LEER'] },
  { ruta: '/cumplimiento/seguridad/incidentes', texto: 'Incidentes', codigos: ['RESPONSABLE_SEGURIDAD', 'AUDITORIA_LEER'] },
]

@Component({
  selector: 'ap-navegacion-de-cumplimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav aria-label="Cumplimiento">
      <ul>
        @for (e of entradas(); track e.ruta) {
          <li><a [routerLink]="e.ruta" routerLinkActive="activa" ariaCurrentWhenActive="page">{{ e.texto }}</a></li>
        }
      </ul>
    </nav>
  `,
  styles: `
    ul { display: flex; flex-wrap: wrap; gap: var(--s2); list-style: none; margin: 0; padding: 0; }
    a { display: inline-flex; align-items: center; min-height: var(--area-tactil); padding: 0 var(--s3); border-radius: var(--r-md); color: var(--text-2); text-decoration: none; }
    a.activa { background: var(--surface-2); color: var(--text); font-weight: 600; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class NavegacionDeCumplimiento {
  private readonly sesion = inject(Sesion)
  protected readonly entradas = computed(() => ENTRADAS_DE_CUMPLIMIENTO.filter((e) => tieneAlguno(this.sesion, e.codigos)))
}
