import { ChangeDetectionStrategy, Component, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Casilla } from '@aportaya/ui/casilla/casilla'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { SalidaPruebaDeContinuidad } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ListaDeOpciones, opcionesDe } from '../compartido/campos'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import type { Recibo } from '../dominio/cu43-reportes'
import { CAMPO_DE_CODIGO_CU56, cuerpoDePrueba, registrarPrueba, validarPrueba, type CampoDePrueba, type DatosDePrueba } from '../dominio/cu56-continuidad'
import { sinErrores, type Errores } from '../dominio/validacion'
import { textosColas } from '../textos-colas'

const VACIO: DatosDePrueba = { planContinuidadId: '', tipo: '', fecha: '', ejecutadaPor: '', actaComiteId: '', rto: '', rpo: '', evidenciaUrl: '', impactoEnClientes: false }

/** CU-56 · registrar una prueba (`registrarPruebaDeContinuidad`), solo el rol RESPONSABLE_RIESGOS. */
@Component({
  selector: 'ap-registrar-prueba',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, Casilla, ListaDeOpciones, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.consecuencia }}</p>
        <ap-campo [etiqueta]="t.plan" [valor]="d().planContinuidadId" (valorChange)="cambiar('planContinuidadId', $event)" [error]="errores().planContinuidadId" autocompletar="off" />
        <ap-lista-de-opciones [etiqueta]="t.tipo" [opciones]="TIPOS" [valor]="d().tipo" (valorChange)="cambiar('tipo', $event)" [error]="errores().tipo" />
        <ap-campo [etiqueta]="t.fecha" tipo="date" [valor]="d().fecha" (valorChange)="cambiar('fecha', $event)" [error]="errores().fecha" />
        <ap-campo [etiqueta]="t.ejecutadaPor" [valor]="d().ejecutadaPor" (valorChange)="cambiar('ejecutadaPor', $event)" [error]="errores().ejecutadaPor" autocompletar="off" />
        <ap-campo [etiqueta]="t.acta" [ayuda]="t.actaAyuda" [valor]="d().actaComiteId" (valorChange)="cambiar('actaComiteId', $event)" [error]="errores().actaComiteId" autocompletar="off" />
        <ap-campo [etiqueta]="t.rto" tipo="number" modoDeEntrada="numeric" [valor]="d().rto" (valorChange)="cambiar('rto', $event)" [error]="errores().rto" />
        <ap-campo [etiqueta]="t.rpo" tipo="number" modoDeEntrada="numeric" [valor]="d().rpo" (valorChange)="cambiar('rpo', $event)" [error]="errores().rpo" />
        <ap-campo [etiqueta]="t.evidencia" [valor]="d().evidenciaUrl" (valorChange)="cambiar('evidenciaUrl', $event)" [error]="errores().evidenciaUrl" autocompletar="off" />
        <ap-casilla [marcada]="d().impactoEnClientes" (marcadaChange)="cambiar('impactoEnClientes', $event)">{{ t.impacto }}</ap-casilla>
        <ap-error-de-envio [error]="errorGeneral()" />
      </div>
    </ap-dialogo>
  `,
  styles: `.formulario { display: grid; gap: var(--s3); } p { margin: 0; }`,
})
export class RegistrarPrueba {
  private readonly registrar = registrarPrueba()
  protected readonly t = textosColas.registro
  protected readonly TIPOS = opcionesDe(textosColas.tiposDePrueba)

  readonly abierto = model(false)
  readonly registro = output<Recibo<SalidaPruebaDeContinuidad>>()

  protected readonly d = signal<DatosDePrueba>({ ...VACIO })
  protected readonly enviando = signal(false)
  protected readonly errores = signal<Errores<CampoDePrueba>>({})
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  protected cambiar<K extends keyof DatosDePrueba>(campo: K, valor: DatosDePrueba[K]): void {
    this.d.update((d) => ({ ...d, [campo]: valor }))
  }

  async enviar(): Promise<void> {
    if (this.enviando()) return
    const errores = validarPrueba(this.d())
    this.errores.set(errores)
    this.errorGeneral.set(null)
    if (!sinErrores(errores)) return
    this.enviando.set(true)
    try {
      const recibo = await this.registrar(cuerpoDePrueba(this.d()))
      this.d.set({ ...VACIO })
      this.abierto.set(false)
      this.registro.emit(recibo)
    } catch (e) {
      const error = traducirError(e)
      const campo = error.codigo ? CAMPO_DE_CODIGO_CU56[error.codigo] : undefined
      if (campo) this.errores.set({ [campo]: error.mensaje })
      else this.errorGeneral.set(error)
    } finally {
      this.enviando.set(false)
    }
  }
}
