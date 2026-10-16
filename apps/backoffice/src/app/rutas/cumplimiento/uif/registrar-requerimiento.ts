import { ChangeDetectionStrategy, Component, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Casilla } from '@aportaya/ui/casilla/casilla'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { SalidaRequerimientoDeAutoridad } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { AreaDeTexto, FechaYHora, ListaDeOpciones, opcionesDe } from '../compartido/campos'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import type { Recibo } from '../dominio/cu43-reportes'
import { accionesDeRequerimientos, cuerpoDeRequerimiento, validarRequerimiento, type CampoDeRequerimiento, type DatosDeRequerimiento } from '../dominio/cu45-requerimientos'
import { sinErrores, type Errores } from '../dominio/validacion'
import { textosUif } from '../textos-uif'

const VACIO: DatosDeRequerimiento = { autoridad: '', numeroOficio: '', plazoLocal: '', usuarioAfectadoId: '', alcance: '', documentoUrl: '', hashDocumento: '', exigeBloqueoDeSaldo: false }

/** CU-45 · registrar el oficio (`registrarRequerimientoDeAutoridad`). AP-CU45-02/03 se muestran en el campo que corresponde. */
@Component({
  selector: 'ap-registrar-requerimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, Casilla, AreaDeTexto, FechaYHora, ListaDeOpciones, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.consecuencia }}</p>
        <ap-lista-de-opciones [etiqueta]="t.autoridad" [opciones]="AUTORIDADES" [valor]="d().autoridad" (valorChange)="cambiar('autoridad', $event)" [error]="errores().autoridad" />
        <ap-campo [etiqueta]="t.oficio" [valor]="d().numeroOficio" (valorChange)="cambiar('numeroOficio', $event)" [error]="errores().numeroOficio" autocompletar="off" />
        <ap-fecha-y-hora [etiqueta]="t.plazo" [valor]="d().plazoLocal" (valorChange)="cambiar('plazoLocal', $event)" [error]="errores().plazoLocal" />
        <ap-campo [etiqueta]="t.afectado" [valor]="d().usuarioAfectadoId" (valorChange)="cambiar('usuarioAfectadoId', $event)" [error]="errores().usuarioAfectadoId" autocompletar="off" />
        <ap-area-de-texto [etiqueta]="t.alcance" [valor]="d().alcance" (valorChange)="cambiar('alcance', $event)" [error]="errores().alcance" [maximo]="300" />
        <ap-campo [etiqueta]="t.documento" [valor]="d().documentoUrl" (valorChange)="cambiar('documentoUrl', $event)" [error]="errores().documentoUrl" autocompletar="off" />
        <ap-campo [etiqueta]="t.hash" [valor]="d().hashDocumento" (valorChange)="cambiar('hashDocumento', $event)" [error]="errores().hashDocumento" autocompletar="off" />
        <ap-casilla [marcada]="d().exigeBloqueoDeSaldo" (marcadaChange)="cambiar('exigeBloqueoDeSaldo', $event)">{{ t.bloqueo }}</ap-casilla>
        <ap-error-de-envio [error]="errorGeneral()" />
      </div>
    </ap-dialogo>
  `,
  styles: `.formulario { display: grid; gap: var(--s3); } p { margin: 0; }`,
})
export class RegistrarRequerimiento {
  private readonly acciones = accionesDeRequerimientos()
  protected readonly t = textosUif.registro
  protected readonly AUTORIDADES = opcionesDe(textosUif.autoridades)

  readonly abierto = model(false)
  readonly registro = output<Recibo<SalidaRequerimientoDeAutoridad>>()

  protected readonly d = signal<DatosDeRequerimiento>({ ...VACIO })
  protected readonly enviando = signal(false)
  protected readonly errores = signal<Errores<CampoDeRequerimiento>>({})
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  protected cambiar<K extends keyof DatosDeRequerimiento>(campo: K, valor: DatosDeRequerimiento[K]): void {
    this.d.update((d) => ({ ...d, [campo]: valor }))
  }

  async enviar(): Promise<void> {
    if (this.enviando()) return
    const errores = validarRequerimiento(this.d())
    this.errores.set(errores)
    this.errorGeneral.set(null)
    if (!sinErrores(errores)) return
    this.enviando.set(true)
    try {
      const recibo = await this.acciones.registrar(cuerpoDeRequerimiento(this.d()))
      this.d.set({ ...VACIO })
      this.abierto.set(false)
      this.registro.emit(recibo)
    } catch (e) {
      const error = traducirError(e)
      if (error.codigo === 'AP-CU45-02') this.errores.set({ documentoUrl: error.mensaje, hashDocumento: error.mensaje })
      else if (error.codigo === 'AP-CU45-03') this.errores.set({ alcance: error.mensaje })
      else if (error.codigo === 'AP-CU45-01') this.errores.set({ numeroOficio: error.mensaje })
      else this.errorGeneral.set(error)
    } finally {
      this.enviando.set(false)
    }
  }
}
