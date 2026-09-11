import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import type { SalidaVerificacionCertificado } from 'clientes/angular/transparencia'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { GATEWAY } from '../nucleo/gateway'

/**
 * CU-75 — Verificar un certificado de reputación.
 *
 * `GET /verificar/:codigo`, sin sesión. Un código inexistente y uno revocado
 * responden **igual** (CU-75, flujo alternativo 4a): esta página no distingue esos
 * dos casos porque el backend ya no distingue — no hay nada que "arreglar" acá.
 */
@Component({
  selector: 'ap-verificador-de-certificado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <ap-estado-de-pantalla [recurso]="verificacion" mensajeVacio="No hay nada para verificar." etiquetaDeCarga="Consultando el certificado" (reintentar)="verificacion.reload()">
      @if (verificacion.hasValue() && verificacion.value(); as v) {
        @switch (v.estado) {
          @case ('VIGENTE') {
            <p role="status" class="ok">Certificado vigente. Emitido el {{ v.emitidoEn }}, vence el {{ v.expiraEn }}.</p>
          }
          @case ('VENCIDO') {
            <p role="status" class="mal">Certificado vencido el {{ v.expiraEn }}. Ya no acredita nada.</p>
          }
          @case ('REVOCADO') {
            <p role="status" class="mal">Certificado revocado. Ya no es válido.</p>
          }
          @default {
            <p role="status" class="mal">Código no válido.</p>
          }
        }
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    .ok { color: var(--ok-texto); }
    .mal { color: var(--danger-texto, var(--error-texto)); }
  `,
})
export class VerificadorDeCertificado {
  private readonly gateway = inject(GATEWAY)
  readonly codigo = input.required<string>()

  protected readonly verificacion = httpResource<SalidaVerificacionCertificado>(() => ({
    url: `${this.gateway}/verificar/${this.codigo()}`,
  }))
}
