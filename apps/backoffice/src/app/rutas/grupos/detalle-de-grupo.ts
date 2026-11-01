import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { formatearMonto } from '@aportaya/ui/monto/formatear'
import { Icono } from '@aportaya/ui/icono/icono'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import { grupoDeBackoffice } from './dominio/consulta-de-grupos'
import { tonoDeGrupo } from './listado-de-grupos'
import { textosGrupos } from './textos'

/**
 * Un grupo visto desde el backoffice: sus datos y, por cupo, quién lo ocupa (alias y estado). Solo lectura: el
 * contrato no trae contacto ni importes de ninguna persona, y esta pantalla no los inventa.
 */
@Component({
  selector: 'ap-detalle-de-grupo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, EstadoDePantalla, Icono, RouterLink, SeccionClaveValor],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="titulo()" [descripcion]="t.detalleProposito" />
      <a class="volver" routerLink="/grupos"><ap-icono nombre="flechaIzquierda" tamano="s4" />{{ t.volver }}</a>
      <ap-estado-de-pantalla [recurso]="detalle" [mensajeVacio]="t.sinCupos" [etiquetaDeCarga]="t.cargandoDetalle" (reintentar)="detalle.reload()">
        @if (detalle.hasValue() && detalle.value(); as d) {
          <div class="cuerpo">
            <ap-seccion-clave-valor [titulo]="t.datos" [items]="datos()" />
            <section aria-labelledby="titulo-cupos">
              <h2 id="titulo-cupos">{{ t.cupos }}</h2>
              @if (d.cupos.length === 0) {
                <p class="vacio">{{ t.sinCupos }}</p>
              } @else {
                <ol class="cupos">
                  @for (c of d.cupos; track c.numero) {
                    <li>
                      <span class="numero">{{ t.cupo }} {{ c.numero }}</span>
                      <span class="quien">{{ c.alias ?? t.libre }}</span>
                      @if (c.esOrganizador) { <ap-chip-estado tono="info">{{ t.organiza }}</ap-chip-estado> }
                      <ap-chip-estado [tono]="c.alias ? 'neutro' : 'aviso'">{{ c.estadoParticipante ?? c.estado }}</ap-chip-estado>
                    </li>
                  }
                </ol>
              }
            </section>
          </div>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: flex; flex-direction: column; gap: var(--s5); max-width: 56rem; }
    .volver { display: inline-flex; align-items: center; gap: var(--s1); color: var(--text-2); text-decoration: none; }
    .volver:hover { color: var(--text); text-decoration: underline; }
    .cuerpo { display: flex; flex-direction: column; gap: var(--s5); }
    h2 { margin: 0 0 var(--s3); font-size: 1.1rem; }
    .vacio { margin: 0; color: var(--text-2); }
    .cupos { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s2); }
    .cupos li { display: flex; align-items: center; gap: var(--s3); flex-wrap: wrap; padding: var(--s3) var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); }
    .numero { font-variant-numeric: tabular-nums; color: var(--text-2); min-width: 4.5rem; }
    .quien { flex: 1 1 10rem; font-weight: 600; overflow-wrap: anywhere; }
  `,
})
export class DetalleDeGrupo {
  protected readonly t = textosGrupos
  readonly grupoId = input.required<string>()

  constructor() {
    inject(ExplicadorDeVista).fijar(textosGrupos.detalleProposito)
  }

  protected readonly detalle = grupoDeBackoffice(this.grupoId)
  private readonly grupo = computed(() => (this.detalle.hasValue() ? this.detalle.value()?.grupo : undefined))
  protected readonly titulo = computed(() => this.grupo()?.nombre ?? this.t.titulo)

  protected readonly datos = computed<ClaveValor[]>(() => {
    const g = this.grupo()
    if (!g) return []
    return [
      { etiqueta: this.t.codigo, valor: g.codigo, mono: true },
      { etiqueta: this.t.estado, valor: this.t.estados[g.estado] ?? g.estado, tono: tonoDeGrupo(g.estado) },
      { etiqueta: this.t.aporte, valor: formatearMonto({ monto: g.montoAporte, moneda: g.moneda }) },
      { etiqueta: this.t.periodicidad, valor: this.t.periodicidades[g.periodicidad] ?? g.periodicidad },
      { etiqueta: this.t.ocupacion, valor: `${g.cuposOcupados} / ${g.cuposTotales}` },
      { etiqueta: this.t.inicio, valor: g.fechaInicio ? formatearDia(g.fechaInicio.slice(0, 10)) : this.t.sinFecha },
    ]
  })
}
