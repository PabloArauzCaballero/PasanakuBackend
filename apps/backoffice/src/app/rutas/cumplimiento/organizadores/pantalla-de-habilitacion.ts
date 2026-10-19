import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { SeccionDeExpediente } from '@aportaya/ui/seccion-de-expediente/seccion-de-expediente'
import type { Habilitacion } from 'clientes/angular/organizador'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { crearHabilitar, habilitacionDe, habilitacionVacia } from '../dominio/cu90-habilitacion'
import { textosCumplimiento } from '../textos'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { ErrorTraducido } from '../../../nucleo/errores'

/**
 * CU-90 · Postular a organizador y habilitarse.
 */
@Component({
  selector: 'ap-pantalla-de-habilitacion-organizador',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ErrorDeEnvio, EstadoDePantalla, CabeceraDePagina, ChipEstado, Boton, SeccionDeExpediente],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="persona" />
      <ap-estado-de-pantalla [recurso]="habilitacion" [vacio]="vacio" [mensajeVacio]="t.noHabilitado" [etiquetaDeCarga]="t.cargando" (reintentar)="habilitacion.reload()">
        @if (habilitacion.hasValue() && habilitacion.value(); as h) {
          <h2>Estado de la habilitación</h2>
          <ap-error-de-envio [error]="errorDeEnvio()" />
          <ap-seccion-de-expediente [titulo]="t.titulo" [datos]="datosDe(h)">
            <ap-chip-estado [tono]="h.habilitado ? 'ok' : 'neutro'">{{ h.habilitado ? t.habilitado : t.noHabilitado }}</ap-chip-estado>
            @if (!h.habilitado) {
              <ap-boton variante="primario" [cargando]="habilitando()" (pulsado)="habilitar()">{{ t.habilitar }}</ap-boton>
            }
          </ap-seccion-de-expediente>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
  `,
})
export class PantallaDeHabilitacionOrganizador {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = textosCumplimiento.organizadores
  readonly organizadorId = input.required<string>()
  protected readonly habilitacion = habilitacionDe(this.organizadorId)
  protected readonly vacio = habilitacionVacia
  protected readonly habilitando = signal(false)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  private readonly enviarHabilitacion = crearHabilitar()

  protected datosDe(h: Habilitacion) {
    return [
      { nombre: this.t.nivel, valor: h.nivel },
      { nombre: this.t.limiteDeGrupos, valor: String(h.limiteDeGrupos) },
      { nombre: this.t.limiteDeMonto, valor: h.limiteDeMonto },
      { nombre: this.t.gruposActivos, valor: String(h.gruposActivos) },
    ]
  }

  protected habilitar(): void {
    this.habilitando.set(true)
    this.errorDeEnvio.set(null)
    this.enviarHabilitacion(this.organizadorId()).subscribe({
      next: () => {
        this.habilitando.set(false)
        this.habilitacion.reload()
      },
      error: (e: ErrorTraducido) => {
        this.errorDeEnvio.set(e)
        this.habilitando.set(false)
      },
    })
  }
}
