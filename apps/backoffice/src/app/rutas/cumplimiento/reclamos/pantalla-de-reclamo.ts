import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { claveDeBorrador, type ReclamoDelConsumidor } from '../dominio/cu52-reclamo'
import { textosCumplimiento } from '../textos'

/**
 * CU-52 · Atender un reclamo en plazo. `RelojDePlazo` recibe `venceIso` directamente
 * de `reclamo().plazoVenceEn` — el valor que trajo el servidor al registrar el
 * reclamo — y `ahoraIso` se inyecta desde afuera para que la pantalla nunca calcule el
 * plazo por su cuenta (skill `plazos-habiles`: se guarda al notificar, no se
 * recalcula al consultar).
 */
@Component({
  selector: 'ap-pantalla-de-reclamo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, RelojDePlazo, Boton],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-reloj-de-plazo [etiqueta]="t.plazo" [venceIso]="reclamo().plazoVenceEn" [ahoraIso]="ahoraIso()" [norma]="t.norma" />

      <label for="respuesta">{{ t.respuesta }}</label>
      <textarea id="respuesta" [value]="respuesta()" (input)="alEscribir($any($event.target).value)" rows="6"></textarea>

      <ap-boton variante="primario" (pulsado)="enviar()">{{ t.enviar }}</ap-boton>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    textarea { width: 100%; min-height: 8rem; padding: var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); font: inherit; background: var(--field); color: var(--text); }
  `,
})
export class PantallaDeReclamo implements OnInit {
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
