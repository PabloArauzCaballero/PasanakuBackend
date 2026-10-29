import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Icono } from '@aportaya/ui/icono/icono'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { CARAS_DEL_EXPEDIENTE, caraEnPalabras, explicacionDeLaCara } from '../dominio/cu02-presentacion'
import { textosVerificaciones } from './textos-verificaciones'
import { TiraDeFotos } from './tira-de-fotos'

/**
 * La pestaña «Documento y fotos»: qué es cada una de las cinco caras y si está cargada, la lista de lo que hay que
 * mirar antes de decidir, y la tira de fotos (que pide los binarios recién al abrirla, de a una y con registro).
 */
@Component({
  selector: 'ap-vista-fotos-del-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, TiraDeFotos],
  template: `
    <header>
      <h2>{{ t.titulo }}</h2>
      <p>{{ t.descripcion }}</p>
    </header>
    <ul class="caras" aria-label="Las cinco caras del expediente">
      @for (cara of caras; track cara) {
        <li [class.falta]="!expediente().fotos.includes(cara)">
          <ap-icono [nombre]="expediente().fotos.includes(cara) ? 'imagen' : 'advertencia'" tamano="s4" />
          <span><strong>{{ nombre(cara) }}</strong> · {{ expediente().fotos.includes(cara) ? 'Cargada' : 'Falta' }}<br /><span class="tenue">{{ explicacion(cara) }}</span></span>
        </li>
      }
    </ul>
    <ap-tira-de-fotos [expediente]="expediente()" />
    <section class="mirar" aria-labelledby="que-mirar">
      <h3 id="que-mirar">{{ t.queMirar }}</h3>
      <ol>
        @for (item of t.checklist; track $index) { <li>{{ item }}</li> }
      </ol>
    </section>
  `,
  styles: `
    :host { display: grid; gap: var(--s4); min-width: 0; }
    header h2 { margin: 0; font: var(--t-titulo-3); color: var(--text); }
    header p { margin: var(--s1) 0 0; max-width: 80ch; font: var(--t-cuerpo-chico); color: var(--text-2); }
    .caras { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 16rem), 1fr)); gap: var(--s2); }
    .caras li { display: flex; align-items: flex-start; gap: var(--s2); padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); color: var(--ok-texto); font: var(--t-cuerpo-chico); }
    .caras li strong { color: var(--text); }
    .caras li.falta { color: var(--aviso-texto); background: var(--warn-bg); }
    .tenue { color: var(--text-2); }
    .mirar { padding: var(--s3) var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); }
    .mirar h3 { margin: 0 0 var(--s2); font: var(--t-cuerpo-fuerte); color: var(--text); }
    .mirar ol { margin: 0; padding-left: var(--s4); display: grid; gap: var(--s1); font: var(--t-cuerpo-chico); color: var(--text-2); }
  `,
})
export class VistaFotosDelExpediente {
  readonly expediente = input.required<ExpedienteEnRevision>()
  protected readonly t = textosVerificaciones.ficha.fotos
  protected readonly caras = CARAS_DEL_EXPEDIENTE
  protected readonly nombre = caraEnPalabras
  protected readonly explicacion = explicacionDeLaCara
}
