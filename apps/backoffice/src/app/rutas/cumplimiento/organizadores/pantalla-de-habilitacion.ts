import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { SeccionDeExpediente } from '@aportaya/ui/seccion-de-expediente/seccion-de-expediente'
import type { Habilitacion } from 'clientes/angular/organizador'
import { crearHabilitar, habilitacionDe, habilitacionVacia } from '../dominio/cu90-habilitacion'
import { textosCumplimiento } from '../textos'

/**
 * CU-90 · Postular a organizador y habilitarse. Muestra si el organizador está
 * habilitado y hasta dónde (`GET .../habilitacion`, contrato real de `organizador`), y
 * permite habilitarlo tras capacitación (`POST .../habilitacion`) — un botón por
 * pantalla, con clave de idempotencia nueva por intento (invariante 7). El HTTP vive
 * en `dominio/cu90-habilitacion.ts`; esta pantalla solo compone.
 */
@Component({
  selector: 'ap-pantalla-de-habilitacion-organizador',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, BandaDeProposito, ChipEstado, Boton, SeccionDeExpediente],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="habilitacion" [vacio]="vacio" [mensajeVacio]="t.noHabilitado" [etiquetaDeCarga]="t.cargando" (reintentar)="habilitacion.reload()">
        @if (habilitacion.hasValue() && habilitacion.value(); as h) {
          <h2>Estado de la habilitación</h2>
          <ap-seccion-de-expediente [titulo]="t.titulo" [datos]="datosDe(h)">
            <ap-chip-estado [tono]="h.habilitado ? 'ok' : 'neutro'">{{ h.habilitado ? t.habilitado : t.noHabilitado }}</ap-chip-estado>
            @if (!h.habilitado) {
              <ap-boton variante="primario" [cargando]="habilitando()" (pulsado)="habilitar()">{{ t.habilitar }}</ap-boton>
            }
          </ap-seccion-de-expediente>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    h1 { margin-bottom: var(--s2); }
  `,
})
export class PantallaDeHabilitacionOrganizador {
  protected readonly t = textosCumplimiento.organizadores
  readonly organizadorId = input.required<string>()
  protected readonly habilitacion = habilitacionDe(this.organizadorId)
  protected readonly vacio = habilitacionVacia
  protected readonly habilitando = signal(false)
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
    this.enviarHabilitacion(this.organizadorId()).subscribe({
      next: () => {
        this.habilitando.set(false)
        this.habilitacion.reload()
      },
      error: () => this.habilitando.set(false),
    })
  }
}
