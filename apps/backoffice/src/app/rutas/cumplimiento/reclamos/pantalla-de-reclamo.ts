import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { claveDeBorrador, type ReclamoDelConsumidor } from '../dominio/cu52-reclamo'
import { textosCumplimiento } from '../textos'

/**
 * CU-52 · Atender un reclamo en plazo.
 */
@Component({
  selector: 'ap-pantalla-de-reclamo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, RelojDePlazo],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="advertencia" />
      <ap-reloj-de-plazo [etiqueta]="t.plazo" [venceIso]="reclamo().plazoVenceEn" [ahoraIso]="ahoraIso()" [norma]="t.norma" />

      <label for="respuesta">{{ t.respuesta }}</label>
      <textarea id="respuesta" [value]="respuesta()" (input)="alEscribir($any($event.target).value)" rows="6"></textarea>

      <ap-boton variante="primario" icono="enviar" (pulsado)="enviar()">{{ t.enviar }}</ap-boton>
    </section>
  `,
  styles: `
    .pantalla { max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    textarea { width: 100%; min-height: 8rem; padding: var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); font: inherit; background: var(--field); color: var(--text); }
  `,
})
export class PantallaDeReclamo implements OnInit {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly servicioBorrador = inject(ServicioBorrador)
  protected readonly t = textosCumplimiento.reclamos

  readonly reclamo = input.required<ReclamoDelConsumidor>()
  /** Reloj de referencia inyectado, nunca `new Date()` dentro de la plantilla. */
  readonly ahoraIso = input<string>(new Date().toISOString())

  protected readonly respuesta = signal('')

  async ngOnInit(): Promise<void> {
    const guardado = await this.servicioBorrador.leer<string>(claveDeBorrador(this.reclamo().id))
    if (guardado) this.respuesta.set(guardado)
  }

  protected alEscribir(valor: string): void {
    this.respuesta.set(valor)
    void this.servicioBorrador.guardar(claveDeBorrador(this.reclamo().id), valor)
  }

  protected enviar(): void {
    void this.servicioBorrador.borrar(claveDeBorrador(this.reclamo().id))
  }
}
