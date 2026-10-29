import { DatePipe } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Icono } from '@aportaya/ui/icono/icono'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { tonoDeEstado } from './bandeja-de-postulaciones'
import { accionDe, expedienteDe, type DecisionDeHabilitacion } from './dominio/cu90-postulaciones'
import { FormularioDeHabilitacion } from './formulario-de-habilitacion'
import { textosPostulaciones } from './textos'

/**
 * CU-90 · El expediente de una postulación a organizador: los datos, cada decisión registrada (inmutable) y, si el
 * servidor lo declara en `acciones`, el formulario para resolverla o revisarla. Ocultar el formulario no protege nada
 * —el servidor exige `ADMIN_PLATAFORMA` y que quien decide no sea quien postuló—, pero ofrecer una acción que siempre
 * falla es peor.
 */
@Component({
  selector: 'ap-pantalla-de-postulacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, ChipEstado, DatePipe, EstadoDePantalla, FormularioDeHabilitacion, Icono, RouterLink, SeccionClaveValor],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.expedienteTitulo" [descripcion]="t.expedienteProposito" />
      <a class="volver" routerLink="/postulaciones-organizador"><ap-icono nombre="flechaIzquierda" tamano="s4" />{{ t.volver }}</a>
      <ap-estado-de-pantalla [recurso]="expediente" [mensajeVacio]="t.sinDecisiones" [etiquetaDeCarga]="t.cargandoExpediente" (reintentar)="expediente.reload()">
        @if (expediente.hasValue() && expediente.value(); as x) {
          <div class="cuerpo">
            <div class="estado">
              <ap-chip-estado [tono]="tono(x.solicitud.estado)">{{ t.estados[x.solicitud.estado] ?? x.solicitud.estado }}</ap-chip-estado>
            </div>
            <ap-seccion-clave-valor [titulo]="t.datos" [items]="datos()" />
            @if (ultimaRegistrada(); as d) {
              <ap-alerta tono="ok">{{ d.nueva ? t.registrada : t.repetida }}</ap-alerta>
            }
            <section aria-labelledby="titulo-historial">
              <h2 id="titulo-historial">{{ t.historial }}</h2>
              @if (x.decisiones.length === 0) {
                <p class="vacio">{{ t.sinDecisiones }}</p>
              } @else {
                <ol class="decisiones">
                  @for (d of x.decisiones; track d.id) {
                    <li>
                      <header>
                        <h3>{{ t.fases[d.fase] ?? d.fase }}</h3>
                        <ap-chip-estado [tono]="d.decision === 'APROBAR' || d.decision === 'CONFIRMAR' ? 'ok' : 'error'">{{ t.decisiones[d.decision] ?? d.decision }}</ap-chip-estado>
                      </header>
                      <p class="motivo">{{ d.motivo }}</p>
                      <p class="meta">
                        {{ t.revision }} {{ d.revision }} ·
                        <time [attr.datetime]="d.ocurridaEn">{{ d.ocurridaEn | date: 'dd/MM/yyyy HH:mm' : '-0400' }} (La Paz)</time>
                      </p>
                    </li>
                  }
                </ol>
              }
            </section>
            @if (accion(); as a) {
              <ap-formulario-de-habilitacion
                [solicitudId]="solicitudId()"
                [accion]="a"
                [revision]="x.solicitud.revision"
                (registrada)="alRegistrar($event)"
                (desactualizada)="expediente.reload()"
              />
            } @else {
              <ap-alerta tono="info">{{ t.sinAcciones }}</ap-alerta>
            }
          </div>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: flex; flex-direction: column; gap: var(--s5); max-width: 48rem; }
    .volver { display: inline-flex; align-items: center; gap: var(--s1); color: var(--text-2); text-decoration: none; }
    .volver:hover { color: var(--text); text-decoration: underline; }
    .cuerpo { display: flex; flex-direction: column; gap: var(--s5); }
    h2 { margin: 0 0 var(--s3); font-size: 1.1rem; }
    h3 { margin: 0; font-size: 1rem; }
    .vacio { margin: 0; color: var(--text-2); }
    .decisiones { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s3); }
    .decisiones li { padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); display: flex; flex-direction: column; gap: var(--s2); }
    header { display: flex; justify-content: space-between; align-items: center; gap: var(--s3); flex-wrap: wrap; }
    .motivo { margin: 0; overflow-wrap: anywhere; }
    .meta { margin: 0; color: var(--text-2); font-size: .875rem; }
  `,
})
export class PantallaDePostulacion {
  protected readonly t = textosPostulaciones
  readonly solicitudId = input.required<string>()

  constructor() {
    inject(ExplicadorDeVista).fijar(textosPostulaciones.expedienteProposito)
  }

  protected readonly expediente = expedienteDe(this.solicitudId)
  protected readonly ultimaRegistrada = signal<DecisionDeHabilitacion | null>(null)
  protected readonly accion = computed(() => (this.expediente.hasValue() && this.expediente.value() ? accionDe(this.expediente.value()!.solicitud) : null))
  protected readonly tono = tonoDeEstado

  /** Los datos de la solicitud. El contrato no trae el nombre: se muestra el identificador, nunca un dato inventado. */
  protected readonly datos = computed<ClaveValor[]>(() => {
    const s = this.expediente.hasValue() ? this.expediente.value()?.solicitud : undefined
    if (!s) return []
    return [
      { etiqueta: this.t.postulante, valor: s.usuarioId, mono: true },
      { etiqueta: this.t.reputacion, valor: s.reputacionAlSolicitar ?? null },
      { etiqueta: this.t.kyc, valor: s.kycReforzadoId ? this.t.kycHecha : this.t.kycFalta },
      { etiqueta: this.t.fechaSolicitud, valor: s.fechaSolicitud.slice(0, 10) },
      { etiqueta: this.t.fechaResolucion, valor: s.fechaResolucion?.slice(0, 10) ?? null },
      ...(s.motivoRechazo ? [{ etiqueta: this.t.motivoRechazo, valor: s.motivoRechazo, texto: true }] : []),
    ]
  })

  protected alRegistrar(d: DecisionDeHabilitacion): void {
    this.ultimaRegistrada.set(d)
    this.expediente.reload()
  }
}
