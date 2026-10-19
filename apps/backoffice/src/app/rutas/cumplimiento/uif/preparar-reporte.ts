import { ChangeDetectionStrategy, Component, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { SalidaReporteRegulatorio } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ListaDeOpciones, opcionesDe } from '../compartido/campos'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import { accionesDeReportes, cuerpoDeGeneracion, validarGeneracion, type CampoDeGeneracion, type Recibo } from '../dominio/cu43-reportes'
import { sinErrores, type Errores } from '../dominio/validacion'
import { textosUif } from '../textos-uif'

/** CU-43 · paso 1, preparar (`generarReporteRegulatorio`). No aprueba ni envía: eso es otro paso y otra persona. */
@Component({
  selector: 'ap-preparar-reporte',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, ListaDeOpciones, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.consecuencia }}</p>
        <ap-campo [etiqueta]="t.periodo" [(valor)]="periodo" [ayuda]="t.periodoAyuda" [error]="errores().periodo" autocompletar="off" />
        <ap-campo [etiqueta]="t.catalogo" [(valor)]="catalogoCodigo" [error]="errores().catalogoCodigo" autocompletar="off" />
        <ap-lista-de-opciones [etiqueta]="t.formulario" [opciones]="FORMULARIOS" [(valor)]="formulario" [error]="errores().formulario" />
        <ap-error-de-envio [error]="errorGeneral()" />
      </div>
    </ap-dialogo>
  `,
  styles: `.formulario { display: grid; gap: var(--s3); } p { margin: 0; }`,
})
export class PrepararReporte {
  private readonly acciones = accionesDeReportes()
  protected readonly t = textosUif.preparacion
  protected readonly FORMULARIOS = opcionesDe(textosUif.formularios)

  readonly abierto = model(false)
  readonly preparo = output<Recibo<SalidaReporteRegulatorio>>()

  protected readonly periodo = signal('')
  protected readonly catalogoCodigo = signal('')
  protected readonly formulario = signal('')
  protected readonly enviando = signal(false)
  protected readonly errores = signal<Errores<CampoDeGeneracion>>({})
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  async enviar(): Promise<void> {
    if (this.enviando()) return
    const errores = validarGeneracion(this.periodo(), this.catalogoCodigo(), this.formulario())
    this.errores.set(errores)
    this.errorGeneral.set(null)
    if (!sinErrores(errores)) return
    this.enviando.set(true)
    try {
      const recibo = await this.acciones.generar(cuerpoDeGeneracion(this.periodo(), this.catalogoCodigo(), this.formulario()))
      this.abierto.set(false)
      this.preparo.emit(recibo)
    } catch (e) {
      this.errorGeneral.set(traducirError(e))
    } finally {
      this.enviando.set(false)
    }
  }
}
