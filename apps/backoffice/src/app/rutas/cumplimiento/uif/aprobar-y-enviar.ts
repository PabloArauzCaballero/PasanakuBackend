import { ChangeDetectionStrategy, Component, effect, input, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Casilla } from '@aportaya/ui/casilla/casilla'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { ReporteRegulatorioEnLista, SalidaReporteRegulatorio } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ListaDeOpciones, opcionesDe } from '../compartido/campos'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import { accionesDeReportes, cuerpoDeEnvio, validarEnvio, type CampoDeEnvio, type Recibo } from '../dominio/cu43-reportes'
import { sinErrores, type Errores } from '../dominio/validacion'
import { textosUif } from '../textos-uif'

/**
 * CU-43 · pasos 2 y 3: aprobar y dejar constancia del envío (`aprobarYEnviarReporte`). La
 * confirmación dice la consecuencia exacta; el período sale de la fila y no se edita.
 */
@Component({
  selector: 'ap-aprobar-y-enviar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, Casilla, ListaDeOpciones, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo(reporte()?.periodo ?? '')" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.consecuencia }}</p>
        @if (errores().periodo) { <p class="error" role="alert">{{ errores().periodo }}</p> }
        <ap-campo [etiqueta]="t.catalogo" [(valor)]="catalogoCodigo" [ayuda]="t.catalogoAyuda" [error]="errores().catalogoCodigo" autocompletar="off" />
        <ap-lista-de-opciones [etiqueta]="t.canal" [opciones]="CANALES" [(valor)]="canal" [error]="errores().canal" />
        <ap-campo [etiqueta]="t.constancia" [(valor)]="numeroConstancia" [error]="errores().numeroConstancia" autocompletar="off" />
        <ap-casilla [(marcada)]="aceptado">{{ t.aceptado }}</ap-casilla>
        <ap-campo [etiqueta]="t.reintentos" tipo="number" modoDeEntrada="numeric" [(valor)]="reintentos" [error]="errores().reintentos" />
        <ap-error-de-envio [error]="errorGeneral()" />
      </div>
    </ap-dialogo>
  `,
  styles: `.formulario { display: grid; gap: var(--s3); } p { margin: 0; } .error { color: var(--err); }`,
})
export class AprobarYEnviar {
  private readonly acciones = accionesDeReportes()
  protected readonly t = textosUif.envio
  protected readonly CANALES = opcionesDe(textosUif.canales)

  readonly abierto = model(false)
  readonly reporte = input<ReporteRegulatorioEnLista | null>(null)
  readonly envio = output<Recibo<SalidaReporteRegulatorio>>()

  protected readonly catalogoCodigo = signal('')
  protected readonly canal = signal('')
  protected readonly numeroConstancia = signal('')
  protected readonly aceptado = signal(false)
  protected readonly reintentos = signal('0')
  protected readonly enviando = signal(false)
  protected readonly errores = signal<Errores<CampoDeEnvio>>({})
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  constructor() {
    // Otro reporte, formulario limpio: lo cargado para un período no se arrastra a otro.
    effect(() => {
      this.reporte()
      this.errores.set({})
      this.errorGeneral.set(null)
    })
  }

  async enviar(): Promise<void> {
    const reporte = this.reporte()
    if (this.enviando() || !reporte) return
    const datos = {
      periodo: reporte.periodo,
      catalogoCodigo: this.catalogoCodigo(),
      canal: this.canal(),
      numeroConstancia: this.numeroConstancia(),
      aceptado: this.aceptado(),
      reintentos: this.reintentos(),
    }
    const errores = validarEnvio(datos)
    this.errores.set(errores)
    this.errorGeneral.set(null)
    if (!sinErrores(errores)) return
    this.enviando.set(true)
    try {
      const recibo = await this.acciones.aprobarYEnviar(cuerpoDeEnvio(datos))
      this.abierto.set(false)
      this.envio.emit(recibo)
    } catch (e) {
      this.errorGeneral.set(traducirError(e))
    } finally {
      this.enviando.set(false)
    }
  }
}
