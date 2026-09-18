import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { ExpedienteEnRevisionFotosEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import { carpetaDelExpediente, crearPedirFoto } from '../dominio/cu02-expedientes'
import { textosCumplimiento } from '../textos'

/**
 * Las tres caras del expediente: anverso, reverso y prueba de vida, **una al lado de
 * la otra**.
 *
 * Juntas y no de a una: aprobar es cotejar la cara contra el documento, y cotejar es
 * mirar las dos a la vez. Antes cada foto reemplazaba a la anterior en el mismo
 * hueco, así que la decisión se tomaba de memoria.
 *
 * Los enlaces se piden **al abrir**, nunca al listar: cada foto de una cédula es un
 * dato personal sensible y cada lectura queda registrada. Una grilla que precarga
 * todas las caras de toda la cola es una filtración cómoda.
 */
@Component({
  selector: 'ap-tira-de-fotos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    <p class="carpeta">
      {{ t.carpeta }}: <code>{{ carpeta() }}</code>
    </p>

    <p class="fotos">
      {{ t.fotos }}:
      @for (cara of caras; track cara) {
        @if (expediente().fotos.includes(cara)) {
          <span class="tiene">{{ cara }}</span>
        } @else {
          <span class="falta">{{ cara }} {{ t.faltante }}</span>
        }
      }
      <ap-boton variante="fantasma" [deshabilitado]="expediente().fotos.length === 0" (pulsado)="alternar()">
        {{ abierto() ? t.ocultarFotos : t.verFotos }}
      </ap-boton>
    </p>

    @if (abierto()) {
      <div class="tira">
        @for (cara of caras; track cara) {
          <figure>
            <figcaption>{{ cara }}</figcaption>
            @if (url()[cara]; as u) {
              <img [src]="u" [alt]="t.fotoDe + ' ' + expediente().nombreCompleto + ' — ' + cara" />
            } @else if (expediente().fotos.includes(cara)) {
              <p class="hueco">{{ t.cargandoFoto }}</p>
            } @else {
              <p class="hueco">{{ t.faltante }}</p>
            }
          </figure>
        }
      </div>
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s3); }
    .carpeta { margin: 0; color: var(--text-2); font-size: .8125rem; }
    .carpeta code { font-family: var(--fuente-mono, monospace); overflow-wrap: anywhere; }
    .fotos { display: flex; align-items: center; gap: var(--s2); flex-wrap: wrap; margin: 0; color: var(--text-2); font-size: .875rem; }
    .tiene { color: var(--text-2); font-weight: 600; }
    .falta { color: var(--text-3); }
    /* Las tres juntas; en pantalla angosta se apilan solas y no rompen el ancho. */
    .tira { display: grid; grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr)); gap: var(--s3); }
    figure { margin: 0; display: flex; flex-direction: column; gap: var(--s1); }
    figcaption { font-size: .75rem; color: var(--text-2); letter-spacing: .04em; }
    .hueco { margin: 0; padding: var(--s4); text-align: center; color: var(--text-3); font-size: .8125rem;
             border: var(--borde-fino) dashed var(--border); border-radius: var(--r-md); }
    img { width: 100%; border-radius: var(--r-md); border: var(--borde-fino) solid var(--border); }
  `,
})
export class TiraDeFotos {
  readonly expediente = input.required<ExpedienteEnRevision>()

  protected readonly t = textosCumplimiento.verificaciones
  protected readonly caras = Object.values(ExpedienteEnRevisionFotosEnum)
  protected readonly abierto = signal(false)
  protected readonly url = signal<Partial<Record<ExpedienteEnRevisionFotosEnum, string>>>({})
  protected readonly carpeta = computed(() => carpetaDelExpediente(this.expediente().usuarioId))

  private readonly pedirFoto = crearPedirFoto()

  /**
   * Al abrir pide **solo las caras que existen**: pedir una que no está cargada
   * devuelve 422 y llenaría la bitácora de lecturas que nunca leyeron nada.
   */
  protected alternar(): void {
    const seAbre = !this.abierto()
    this.abierto.set(seAbre)
    if (!seAbre) return
    const e = this.expediente()
    for (const cara of this.caras) {
      if (!e.fotos.includes(cara) || this.url()[cara]) continue
      this.pedirFoto(e.verificacionId, cara).subscribe((enlace) => {
        this.url.update((actual) => ({ ...actual, [cara]: enlace.url }))
      })
    }
  }
}
