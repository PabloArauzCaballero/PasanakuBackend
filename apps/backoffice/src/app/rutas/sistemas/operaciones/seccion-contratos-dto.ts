import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { CampoDTO, DTOOperacion } from './dominio/tipos'

@Component({
  selector: 'ap-seccion-contratos-dto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, TablaDeDatos],
  template: `
    <section class="seccion-dto">
      @if (dtoEntrada(); as entrada) {
        <div class="bloque-dto">
          <div class="dto-header">
            <span class="eyebrow">Request Payload / Parámetros</span>
            <h4 class="nombre-dto"><code>{{ entrada.nombre }}</code></h4>
          </div>
          <ap-tabla-de-datos
            [titulo]="'Campos de entrada (' + entrada.campos.length + ')'"
            [columnas]="columnas"
            [filas]="entrada.campos"
            [identidad]="porCampo"
            densidad="compacta"
          >
            <ng-template #celda let-c let-columna="columna">
              @switch (columna.clave) {
                @case ('nombre') {
                  <span class="mono-bold">{{ c.nombre }}</span>
                }
                @case ('tipo') {
                  <ap-chip-estado tono="info">{{ c.tipo }}</ap-chip-estado>
                }
                @case ('requerido') {
                  <ap-chip-estado [tono]="c.requerido ? 'aviso' : 'neutro'">
                    {{ c.requerido ? 'Requerido' : 'Opcional' }}
                  </ap-chip-estado>
                }
                @case ('validacion') {
                  <code class="val-code">{{ c.validacion }}</code>
                }
                @case ('descripcion') {
                  <span class="desc-texto">{{ c.descripcion }}</span>
                }
                @default {
                  {{ c[columna.clave] }}
                }
              }
            </ng-template>
          </ap-tabla-de-datos>
        </div>
      }

      @if (dtoSalida(); as salida) {
        <div class="bloque-dto">
          <div class="dto-header">
            <span class="eyebrow">Response Body (HTTP 200/201)</span>
            <h4 class="nombre-dto"><code>{{ salida.nombre }}</code></h4>
          </div>
          <ap-tabla-de-datos
            [titulo]="'Campos de respuesta (' + salida.campos.length + ')'"
            [columnas]="columnasSalida"
            [filas]="salida.campos"
            [identidad]="porCampo"
            densidad="compacta"
          >
            <ng-template #celda let-c let-columna="columna">
              @switch (columna.clave) {
                @case ('nombre') {
                  <span class="mono-bold">{{ c.nombre }}</span>
                }
                @case ('tipo') {
                  <ap-chip-estado tono="ok">{{ c.tipo }}</ap-chip-estado>
                }
                @case ('descripcion') {
                  <span class="desc-texto">{{ c.descripcion }}</span>
                }
                @default {
                  {{ c[columna.clave] }}
                }
              }
            </ng-template>
          </ap-tabla-de-datos>
        </div>
      }
    </section>
  `,
  styles: `
    :host { display: block; width: 100%; }
    .seccion-dto { display: flex; flex-direction: column; gap: var(--s4); }
    .bloque-dto { display: flex; flex-direction: column; gap: var(--s2); padding: var(--s3); background: var(--fondo-tarjeta); border: var(--borde-fino) solid var(--borde-tarjeta); border-radius: var(--r-md); }
    .dto-header { display: flex; flex-direction: column; gap: 0.15rem; }
    .eyebrow { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-apagado); font-weight: 600; }
    .nombre-dto { margin: 0; font-size: 1.1rem; color: var(--text); }
    .mono-bold { font-family: var(--f-mono, monospace); font-weight: 600; font-size: 0.8125rem; color: var(--text); }
    .val-code { font-family: var(--f-mono, monospace); font-size: 0.75rem; color: var(--brand-texto); background: var(--fondo-control); padding: 0.15rem 0.35rem; border-radius: var(--r-sm); }
    .desc-texto { font-size: 0.8125rem; color: var(--text-suave); }
  `,
})
export class SeccionContratosDto {
  readonly dtoEntrada = input<DTOOperacion | null>(null)
  readonly dtoSalida = input<DTOOperacion | null>(null)

  protected readonly porCampo = (c: CampoDTO) => c.nombre

  protected readonly columnas: Columna<CampoDTO>[] = [
    { clave: 'nombre', titulo: 'Campo / Atributo', mono: true },
    { clave: 'tipo', titulo: 'Tipo de dato' },
    { clave: 'requerido', titulo: 'Obligatoriedad' },
    { clave: 'validacion', titulo: 'Validaciones / Restricciones' },
    { clave: 'descripcion', titulo: 'Descripción' },
  ]

  protected readonly columnasSalida: Columna<CampoDTO>[] = [
    { clave: 'nombre', titulo: 'Campo / Atributo', mono: true },
    { clave: 'tipo', titulo: 'Tipo de dato' },
    { clave: 'descripcion', titulo: 'Descripción' },
  ]
}
