import { ChangeDetectionStrategy, Component, computed, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoMonto, aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { lotesDeAnunciantes } from '../dominio/cu110-anunciantes'
import { accionesDeCampana } from '../dominio/cu111-campanas'
import { traducirError } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import { EntradaCampanaMonedaEnum as Moneda, EntradaCampanaObjetivoEnum as Objetivo, type EntradaCampana } from 'clientes/angular/publicidad/model/entradaCampana'
import { ConjuntoModeloPujaEnum as Puja } from 'clientes/angular/publicidad/model/conjunto'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
const t = textosPublicidad.campanas

/**
 * CU-111 · Crear una campaña con su primer conjunto de anuncios. Antes el diálogo mandaba una cuenta publicitaria
 * inventada (`00000000-…`) y `conjuntos: []`, que el contrato rechaza (`minItems: 1`): «Crear campaña» no podía
 * funcionar nunca. Ahora la cuenta se elige entre las de los anunciantes (`listarAnunciantes`) y se carga el conjunto que
 * el contrato exige. Segmento y espacio publicitario van por identificador: el contrato no publica un listado de ellos.
 */
@Component({
  selector: 'ap-formulario-de-campana',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, CampoMonto, Dialogo, ErrorDeEnvio, ListaDeOpciones],
  template: `
    <ap-dialogo [titulo]="t.nuevaCampana" [textoDeConfirmar]="t.crearYEnviar" [(abierto)]="abierto" [cargando]="guardando()" [hayCambiosSinGuardar]="sucio()" (confirmar)="guardar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <fieldset>
          <legend>{{ t.datosCampana }}</legend>
          <ap-lista-de-opciones class="ancho" [etiqueta]="t.cuenta" [opciones]="cuentas()" [(valor)]="cuenta" [textoVacio]="cuentasCargando() ? 'Cargando cuentas…' : 'Elegí una cuenta'" [error]="errorDeCuentas() ?? (falta() && !cuenta() ? t.obligatorio : undefined)" />
          <ap-campo class="ancho" [etiqueta]="t.nombre" [(valor)]="nombre" [error]="falta() && !nombre().trim() ? t.obligatorio : undefined" />
          <ap-lista-de-opciones [etiqueta]="t.objetivo" [opciones]="objetivos" [(valor)]="objetivo" />
          <ap-lista-de-opciones [etiqueta]="t.moneda" [opciones]="monedas" [(valor)]="moneda" />
          <ap-campo-monto [etiqueta]="t.presupuestoTotal" [(valor)]="presupuesto" [error]="falta() && !presupuesto() ? t.obligatorio : undefined" />
          <ap-campo tipo="date" [etiqueta]="t.fechaInicio" [(valor)]="inicio" [error]="falta() && !inicio() ? t.obligatorio : undefined" />
        </fieldset>
        <fieldset>
          <legend>{{ t.conjunto }}</legend>
          <ap-campo class="ancho" [etiqueta]="t.nombreConjunto" [(valor)]="nombreConjunto" [error]="falta() && !nombreConjunto().trim() ? t.obligatorio : undefined" />
          <ap-campo [etiqueta]="t.segmento" [ayuda]="t.ayudaUuid" [(valor)]="segmento" [error]="errorUuid(segmento())" />
          <ap-campo [etiqueta]="t.espacio" [ayuda]="t.ayudaUuid" [(valor)]="espacio" [error]="errorUuid(espacio())" />
          <ap-campo-monto [etiqueta]="t.presupuestoDiario" [(valor)]="diario" [error]="falta() && !diario() ? t.obligatorio : undefined" />
          <ap-campo-monto [etiqueta]="t.pujaMaxima" [(valor)]="puja" [error]="falta() && !puja() ? t.obligatorio : undefined" />
          <ap-lista-de-opciones [etiqueta]="t.modeloPuja" [opciones]="modelos" [(valor)]="modelo" />
        </fieldset>
        <ap-error-de-envio [error]="error()" />
      </div>
    </ap-dialogo>
  `,
  styles: `
    .formulario { display: grid; gap: var(--s4); }
    fieldset { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 13rem), 1fr)); gap: var(--s3); margin: 0; padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); min-width: 0; }
    legend { padding: 0 var(--s1); font-weight: 600; color: var(--text); }
    .ancho { grid-column: 1 / -1; }
  `,
})
export class FormularioDeCampana {
  private readonly acciones = accionesDeCampana()
  private readonly anunciantes = lotesDeAnunciantes()
  protected readonly t = t
  readonly abierto = model(false)
  readonly creada = output<string>()

  protected readonly objetivos = Object.values(Objetivo).map((v) => ({ valor: v, texto: t.objetivos[v] }))
  protected readonly monedas = [{ valor: Moneda.Bob, texto: 'Bolivianos (BOB)' }, { valor: Moneda.Usd, texto: 'Dólares (USD)' }]
  protected readonly modelos = [{ valor: Puja.Cpm, texto: 'CPM · por mil impresiones' }, { valor: Puja.Cpc, texto: 'CPC · por clic' }]
  protected readonly cuentas = computed(() =>
    this.anunciantes.filas().filter((a) => a.cuentaPublicitariaId && a.estado === 'ACTIVO').map((a) => ({ valor: a.cuentaPublicitariaId!, texto: a.razonSocialFacturacion })),
  )
  protected readonly cuentasCargando = computed(() => this.anunciantes.carga().tipo === 'cargando')
  protected readonly errorDeCuentas = computed(() => {
    const c = this.anunciantes.carga()
    return c.tipo === 'error' ? `${t.sinCuentas} ${c.mensaje}` : undefined
  })

  protected readonly cuenta = signal('')
  protected readonly nombre = signal('')
  protected readonly objetivo = signal<string>(Objetivo.VisibilidadMarca)
  protected readonly moneda = signal<string>(Moneda.Bob)
  protected readonly presupuesto = signal('')
  protected readonly inicio = signal('')
  protected readonly nombreConjunto = signal('')
  protected readonly segmento = signal('')
  protected readonly espacio = signal('')
  protected readonly diario = signal('')
  protected readonly puja = signal('')
  protected readonly modelo = signal<string>(Puja.Cpm)
  protected readonly falta = signal(false)
  protected readonly guardando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly sucio = computed(() => !!(this.nombre() || this.presupuesto() || this.nombreConjunto() || this.segmento() || this.espacio()))
  protected readonly errorUuid = (v: string) => (!this.falta() ? undefined : !v.trim() ? t.obligatorio : UUID.test(v.trim()) ? undefined : t.idInvalido)

  abrir(): void {
    for (const s of [this.cuenta, this.nombre, this.presupuesto, this.inicio, this.nombreConjunto, this.segmento, this.espacio, this.diario, this.puja]) s.set('')
    this.falta.set(false)
    this.error.set(null)
    this.anunciantes.recargar()
    this.abierto.set(true)
  }

  private valida(): boolean {
    const montos = [this.presupuesto(), this.diario(), this.puja()].every((m) => !!m && aCadenaDelContrato(m))
    return !!(this.cuenta() && this.nombre().trim() && this.inicio() && this.nombreConjunto().trim() && montos && !this.errorUuid(this.segmento()) && !this.errorUuid(this.espacio()))
  }

  async guardar(): Promise<void> {
    if (this.guardando()) return
    this.falta.set(true)
    if (!this.valida()) return
    const entrada: EntradaCampana = {
      cuentaPublicitariaId: this.cuenta(),
      nombre: this.nombre().trim(),
      objetivo: this.objetivo() as EntradaCampana['objetivo'],
      presupuestoTotal: aCadenaDelContrato(this.presupuesto())!,
      moneda: this.moneda() as EntradaCampana['moneda'],
      fechaInicio: new Date(`${this.inicio()}T04:00:00Z`).toISOString(),
      conjuntos: [
        {
          nombre: this.nombreConjunto().trim(),
          segmentoAudienciaId: this.segmento().trim().toLowerCase(),
          espacioPublicitarioId: this.espacio().trim().toLowerCase(),
          presupuestoDiario: aCadenaDelContrato(this.diario())!,
          pujaMaxima: aCadenaDelContrato(this.puja())!,
          modeloPuja: this.modelo() as Puja,
        },
      ],
    }
    this.guardando.set(true)
    this.error.set(null)
    try {
      const salida = await this.acciones.crear(entrada)
      this.abierto.set(false)
      this.creada.emit(salida.campanaPublicitariaId)
    } catch (e) {
      this.error.set(traducirError(e))
    } finally {
      this.guardando.set(false)
    }
  }
}
