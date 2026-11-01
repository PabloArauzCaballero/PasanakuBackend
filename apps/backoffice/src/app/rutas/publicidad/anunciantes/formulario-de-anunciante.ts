import { ChangeDetectionStrategy, Component, computed, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoMonto, aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { accionesDeAnunciante } from '../dominio/cu110-anunciantes'
import { traducirError } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import { EntradaAnuncianteMonedaEnum as Moneda, EntradaAnuncianteTipoEnum as Tipo, type EntradaAnunciante } from 'clientes/angular/publicidad/model/entradaAnunciante'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * CU-110 · El alta de un anunciante, en un diálogo. R-PUB-01: el anunciante es un organizador **o** un socio comercial,
 * y el contrato exige el identificador del que corresponde (el otro va prohibido). Antes el formulario mandaba siempre
 * «socio comercial» sin `socioComercialId`, que el servidor rechaza: el botón «Guardar» no podía funcionar nunca.
 * Ante un fallo lo escrito se queda y el error se muestra en el diálogo; el botón no admite doble envío.
 */
@Component({
  selector: 'ap-formulario-de-anunciante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, CampoMonto, Dialogo, ErrorDeEnvio, ListaDeOpciones],
  template: `
    <ap-dialogo [titulo]="t.nuevoAnunciante" [textoDeConfirmar]="t.guardar" [(abierto)]="abierto" [cargando]="guardando()" [hayCambiosSinGuardar]="sucio()" (confirmar)="guardar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <ap-lista-de-opciones [etiqueta]="t.tipo" [opciones]="tipos" [(valor)]="tipo" [error]="falta() && !tipo() ? t.obligatorio : undefined" />
        <ap-campo [etiqueta]="tipo() === 'ORGANIZADOR' ? t.organizadorId : t.socioId" [ayuda]="t.ayudaId" [(valor)]="referencia" [error]="errorDeReferencia()" />
        <ap-campo [etiqueta]="t.razonSocial" [(valor)]="razonSocial" [error]="falta() && !razonSocial().trim() ? t.obligatorio : undefined" />
        <ap-lista-de-opciones [etiqueta]="t.moneda" [opciones]="monedas" [(valor)]="moneda" />
        <ap-campo-monto [etiqueta]="t.limiteGasto" [(valor)]="limite" />
        <ap-error-de-envio [error]="error()" />
      </div>
    </ap-dialogo>
  `,
  styles: `.formulario { display: grid; gap: var(--s3); }`,
})
export class FormularioDeAnunciante {
  private readonly acciones = accionesDeAnunciante()
  protected readonly t = textosPublicidad.anunciantes
  readonly abierto = model(false)
  /** Solo después del 2xx: quien aloja recarga el listado. */
  readonly creado = output<string>()

  protected readonly tipos = [
    { valor: Tipo.Organizador, texto: 'Organizador' },
    { valor: Tipo.SocioComercial, texto: 'Socio comercial' },
  ]
  protected readonly monedas = [
    { valor: Moneda.Bob, texto: 'Bolivianos (BOB)' },
    { valor: Moneda.Usd, texto: 'Dólares (USD)' },
  ]
  protected readonly tipo = signal<string>(Tipo.SocioComercial)
  protected readonly referencia = signal('')
  protected readonly razonSocial = signal('')
  protected readonly moneda = signal<string>(Moneda.Bob)
  protected readonly limite = signal('')
  protected readonly falta = signal(false)
  protected readonly guardando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly sucio = computed(() => !!(this.referencia() || this.razonSocial() || this.limite()))
  protected readonly errorDeReferencia = computed(() => {
    if (!this.falta()) return undefined
    if (!this.referencia().trim()) return this.t.obligatorio
    return UUID.test(this.referencia().trim()) ? undefined : this.t.idInvalido
  })

  /** Abre limpio: un alta nueva no hereda lo de la anterior. */
  abrir(): void {
    this.tipo.set(Tipo.SocioComercial)
    this.referencia.set('')
    this.razonSocial.set('')
    this.moneda.set(Moneda.Bob)
    this.limite.set('')
    this.falta.set(false)
    this.error.set(null)
    this.abierto.set(true)
  }

  async guardar(): Promise<void> {
    if (this.guardando()) return
    this.falta.set(true)
    if (this.errorDeReferencia() || !this.razonSocial().trim() || !this.tipo()) return
    const limite = this.limite() ? aCadenaDelContrato(this.limite()) : null
    const id = this.referencia().trim().toLowerCase()
    const entrada: EntradaAnunciante = {
      tipo: this.tipo() as EntradaAnunciante['tipo'],
      razonSocialFacturacion: this.razonSocial().trim(),
      moneda: this.moneda() as EntradaAnunciante['moneda'],
      ...(this.tipo() === Tipo.Organizador ? { organizadorId: id } : { socioComercialId: id }),
      ...(limite ? { limiteGastoMensual: limite } : {}),
    }
    this.guardando.set(true)
    this.error.set(null)
    try {
      const salida = await this.acciones.darDeAlta(entrada)
      this.abierto.set(false)
      this.creado.emit(salida.anuncianteId)
    } catch (e) {
      this.error.set(traducirError(e))
    } finally {
      this.guardando.set(false)
    }
  }
}
