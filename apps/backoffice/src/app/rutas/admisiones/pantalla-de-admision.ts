import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Sesion } from '../../nucleo/sesion'
import { historialDe, propuestaVigente, resolucionDe, revisionActual, type DecisionDeIngreso } from './dominio/cu68-admision'
import { FormularioDeResolucion } from './formulario-de-resolucion'
import { HistorialDeAdmision } from './historial-de-admision'
import { textosAdmisiones } from './textos'

/** Permiso de la plataforma que el servidor exige para resolver (`@Permiso("ADMIN_PLATAFORMA")`). */
const PERMISO_DE_RESOLUCION = 'ADMIN_PLATAFORMA'

/**
 * CU-68 · la solicitud de ingreso a un grupo, vista por backoffice.
 *
 * El administrador del grupo propone; el algoritmo solo recomienda; **la resolución es de una persona
 * de backoffice**, con motivo, y es inmutable. Esta pantalla pide el historial real
 * (`GET .../decisiones`) y decide qué se ofrece: el formulario de resolución solo aparece para quien
 * tiene el permiso, sobre una propuesta vigente que no es suya y mientras la solicitud sigue abierta.
 * Ocultar el formulario no protege nada —el servidor vuelve a decidir—, pero ofrecer una acción que
 * siempre falla es peor.
 */
@Component({
  selector: 'ap-pantalla-de-admision',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, BandaDeProposito, Alerta, HistorialDeAdmision, FormularioDeResolucion],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="historial" [vacio]="sinDecisiones" [mensajeVacio]="t.sinDecisiones" [etiquetaDeCarga]="t.cargando" (reintentar)="historial.reload()">
        @if (historial.hasValue()) {
          <div class="cuerpo">
            <ap-historial-de-admision [decisiones]="decisiones()" />
            @if (resolucion()) {
              <ap-alerta [tono]="registradaAhora() ? 'ok' : 'info'">{{ registradaAhora() ? t.confirmada : t.resolucionCerrada }}</ap-alerta>
            } @else if (!puedeResolver()) {
              <ap-alerta tono="info">{{ t.sinPermiso }}</ap-alerta>
            } @else if (esPropiaLaPropuesta()) {
              <ap-alerta tono="aviso">{{ t.propiaPropuesta }}</ap-alerta>
            } @else if (propuesta(); as p) {
              <ap-formulario-de-resolucion
                [solicitudId]="solicitudId()"
                [propuesta]="p"
                [revision]="revision()"
                (registrada)="alRegistrar()"
                (desactualizada)="historial.reload()"
              />
            } @else {
              <ap-alerta tono="info">{{ t.sinDecisiones }}</ap-alerta>
            }
          </div>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 44rem; display: flex; flex-direction: column; gap: var(--s5); }
    h1 { margin: 0; }
    .cuerpo { display: flex; flex-direction: column; gap: var(--s5); }
  `,
})
export class PantallaDeAdmision {
  protected readonly t = textosAdmisiones
  private readonly sesion = inject(Sesion)
  readonly solicitudId = input.required<string>()

  protected readonly historial = historialDe(this.solicitudId)
  protected readonly sinDecisiones = (h: DecisionDeIngreso[]): boolean => h.length === 0
  protected readonly registradaAhora = signal(false)

  protected readonly decisiones = computed(() => this.historial.value() ?? [])
  protected readonly propuesta = computed(() => propuestaVigente(this.decisiones()))
  protected readonly resolucion = computed(() => resolucionDe(this.decisiones()))
  protected readonly revision = computed(() => revisionActual(this.decisiones()))
  protected readonly puedeResolver = computed(() => this.sesion.puede(PERMISO_DE_RESOLUCION))
  /** Aviso, no barrera: el servidor también lo rechaza («otro responsable humano»). */
  protected readonly esPropiaLaPropuesta = computed(() => {
    const p = this.propuesta()
    const yo = this.sesion.sujeto()
    return p !== null && yo !== null && p.actorId === yo
  })

  protected alRegistrar(): void {
    this.registradaAhora.set(true)
    this.historial.reload()
  }
}
