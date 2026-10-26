import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Icono, type NombreDeIcono } from '@aportaya/ui/icono/icono'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { Sesion } from '../../../nucleo/sesion'
import { nombreDelPeriodo, type PeriodoVigente } from '../dominio/cu100-periodos'
import { textosContabilidad } from '../textos'

/** Código del contrato cuando ningún período contiene hoy (America/La_Paz). */
export const SIN_PERIODO_VIGENTE = 'AP-CU100-04'

export type EstadoDelPeriodoDeHoy = 'cargando' | 'sinVigente' | 'error' | 'listo'

/** De lo que devolvió `GET /erp/periodos/vigente` al estado que se dibuja. `AP-CU100-04` no es un error técnico: es un hecho. */
export function estadoDelPeriodoDeHoy(periodo: PeriodoVigente | null, error: ErrorTraducido | null): EstadoDelPeriodoDeHoy {
  if (error) return error.codigo === SIN_PERIODO_VIGENTE ? 'sinVigente' : 'error'
  return periodo ? 'listo' : 'cargando'
}

/**
 * CU-100 · «Período de hoy»: lo que respondió `GET /erp/periodos/vigente`, y los accesos a las otras pantallas de
 * Contabilidad que el permiso de la sesión habilita (activos y estados financieros van al período de hoy, así que sin él
 * no se ofrecen). Sin período de hoy (`AP-CU100-04`) lo dice con un aviso y **no propone otro**.
 */
@Component({
  selector: 'ap-periodo-de-hoy',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Icono, RouterLink],
  template: `
    <section aria-labelledby="titulo-vigente">
      <h2 id="titulo-vigente"><ap-icono nombre="reloj" tamano="s4" />{{ t.vigente }}</h2>
      @switch (estado()) {
        @case ('cargando') {
          <p role="status">{{ t.cargandoVigente }}</p>
        }
        @case ('sinVigente') {
          <ap-alerta tono="aviso" [titulo]="t.sinVigenteTitulo">{{ t.sinVigente }}</ap-alerta>
        }
        @case ('error') {
          <ap-alerta tono="error" [titulo]="t.noSePudoVigente">
            {{ error()?.mensaje }}
            @if (error()?.trazaId) { <span class="traza">Código de seguimiento: {{ error()?.trazaId }}</span> }
          </ap-alerta>
        }
        @case ('listo') {
          <p class="hoy">{{ texto() }}</p>
        }
      }
      @if (enlaces().length > 0) {
        <nav [attr.aria-label]="t.secciones">
          <ul>
            @for (e of enlaces(); track e.ruta) {
              <li><a [routerLink]="e.ruta"><ap-icono [nombre]="e.icono" tamano="s4" />{{ e.texto }}<ap-icono class="flecha" nombre="chevronDerecha" tamano="s4" /></a></li>
            }
          </ul>
        </nav>
      }
    </section>
  `,
  styles: `
    section { display: grid; gap: var(--s3); }
    h2 { display: flex; align-items: center; gap: var(--s2); margin: 0; font-size: 1.05em; }
    .hoy { margin: 0; font-weight: 600; }
    .traza { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .85em; }
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s2); }
    a { display: flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); color: var(--brand-texto); font-weight: 600; text-decoration: none; transition: background-color var(--mov-micro) ease-out; }
    a:hover { background: var(--brand-bg); }
    .flecha { margin-left: auto; color: var(--text-3); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PeriodoDeHoy {
  protected readonly t = textosContabilidad.periodo
  readonly periodo = input<PeriodoVigente | null>(null)
  readonly error = input<ErrorTraducido | null>(null)
  private readonly sesion = inject(Sesion)

  protected readonly estado = computed(() => estadoDelPeriodoDeHoy(this.periodo(), this.error()))
  protected readonly texto = computed(() => {
    const p = this.periodo()
    return p ? this.t.hoyEn(nombreDelPeriodo(p.anio, p.mes), this.t.estados[p.estado]) : ''
  })

  protected readonly enlaces = computed(() => {
    const hoy = this.periodo()?.periodoId ?? null
    const e = this.t.enlaces
    const candidatos: { ruta: string; texto: string; permiso: string; icono: NombreDeIcono; necesitaHoy?: boolean }[] = [
      { ruta: '/contabilidad/presupuesto', texto: e.presupuesto, permiso: 'CONTABILIDAD_ERP_PRESUPUESTO', icono: 'grafico' },
      { ruta: '/contabilidad/compras', texto: e.compras, permiso: 'CONTABILIDAD_ERP_COMPRAS', icono: 'recibo' },
      { ruta: '/contabilidad/cobros', texto: e.cobros, permiso: 'CONTABILIDAD_ERP_COBRAR', icono: 'moneda' },
      { ruta: `/contabilidad/activos/${hoy}`, texto: e.activos, permiso: 'CONTABILIDAD_ERP_ACTIVOS_FIJOS', icono: 'paquete', necesitaHoy: true },
      { ruta: `/contabilidad/estados/${hoy}`, texto: e.estados, permiso: 'CONTABILIDAD_ERP_REPORTES', icono: 'balanza', necesitaHoy: true },
    ]
    return candidatos.filter((c) => this.sesion.puede(c.permiso) && (!c.necesitaHoy || hoy !== null))
  })
}
