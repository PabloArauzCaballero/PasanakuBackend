import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { EntradaReporteFormatoEnum } from 'clientes/angular/auditoria/model/entradaReporte'
import type { ErrorTraducido } from '../../../nucleo/errores'
import {
  crearEjecutarReporte,
  FORMATOS,
  JUSTIFICACION_MAXIMA,
  JUSTIFICACION_MINIMA,
  type DefinicionDeReporteEnLista,
  type ParametroEsperado,
  type SalidaReporte,
} from '../dominio/cu58-reportes'
import { textosConfiguracion } from '../textos'

const FORMATO: Record<string, EntradaReporteFormatoEnum> = {
  CSV: EntradaReporteFormatoEnum.Csv,
  JSON: EntradaReporteFormatoEnum.Json,
  PDF: EntradaReporteFormatoEnum.Pdf,
  XLSX: EntradaReporteFormatoEnum.Xlsx,
}

/**
 * CU-58 · Ejecutar un reporte del catálogo. Un campo por cada `parametrosEsperados` de la definición (su lista blanca:
 * nada más se manda), la justificación si tiene datos sensibles y, opcional, el formato de exportación. El servidor
 * vuelve a validar todo y exige el `permisoRequerido`; esto solo evita mandar algo que siempre fallaría.
 */
@Component({
  selector: 'ap-formulario-de-ejecucion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, AreaDeTexto, Boton, Campo, ListaDeOpciones],
  template: `
    <section class="tarjeta" [attr.aria-label]="t.ejecutarTitulo(definicion().nombre)">
      <h2>{{ t.ejecutarTitulo(definicion().nombre) }}</h2>
      @if (salida(); as s) {
        <ap-alerta tono="ok">
          {{ t.resultado(s.estado, s.filasGeneradas) }}
          <p class="huella">{{ t.huella }}: <code>{{ s.hashResultado }}</code></p>
          @if (s.exportacionId) { <p class="huella">{{ t.exportacion }}: <code>{{ s.exportacionId }}</code></p> }
        </ap-alerta>
      }
      @if (error(); as e) { <ap-alerta tono="error">{{ e.mensaje }}</ap-alerta> }
      <form (submit)="$event.preventDefault(); enviar()" novalidate>
        @for (p of parametros(); track p.nombre) {
          <ap-campo
            [etiqueta]="p.nombre + (p.obligatorio ? '' : ' ' + t.opcional)"
            [tipo]="p.tipo === 'FECHA' ? 'date' : 'text'"
            [error]="errores()[p.nombre] ?? ''"
            [deshabilitado]="enviando()"
            [valor]="valores()[p.nombre] ?? ''"
            (valorChange)="poner(p.nombre, $event)"
          />
        } @empty {
          <p class="ayuda">{{ t.sinParametros }}</p>
        }
        @if (definicion().contieneDatosSensibles) {
          <ap-area-de-texto [etiqueta]="t.justificacion" [ayuda]="t.justificacionAyuda" [maximo]="maximo" [error]="errorDeJustificacion()" [deshabilitado]="enviando()" [(valor)]="justificacion" />
        }
        <ap-lista-de-opciones [etiqueta]="t.formato" [opciones]="formatos" [textoVacio]="t.sinExportar" [(valor)]="formato" />
        <div class="acciones">
          <ap-boton variante="primario" tipo="submit" [cargando]="enviando()" [deshabilitado]="enviando()">{{ enviando() ? t.enviando : t.enviar }}</ap-boton>
          <ap-boton variante="secundario" [deshabilitado]="enviando()" (pulsado)="cerrar.emit()">{{ t.cancelar }}</ap-boton>
        </div>
      </form>
    </section>
  `,
  styles: `
    .tarjeta { display: grid; gap: var(--s3); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    h2 { margin: 0; font: var(--t-titulo-3); }
    form { display: grid; gap: var(--s3); }
    .ayuda { margin: 0; color: var(--text-2); }
    .huella { margin: var(--s1) 0 0; overflow-wrap: anywhere; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; }
  `,
})
export class FormularioDeEjecucion {
  protected readonly t = textosConfiguracion.reportes
  protected readonly maximo = JUSTIFICACION_MAXIMA
  protected readonly formatos = FORMATOS.map((f) => ({ valor: f, texto: f }))

  readonly definicion = input.required<DefinicionDeReporteEnLista>()
  readonly ejecutada = output<SalidaReporte>()
  readonly cerrar = output<void>()

  private readonly ejecutar = crearEjecutarReporte()
  protected readonly parametros = computed<ParametroEsperado[]>(() => this.definicion().parametrosEsperados ?? [])
  protected readonly valores = signal<Record<string, string>>({})
  protected readonly errores = signal<Record<string, string>>({})
  protected readonly justificacion = signal('')
  protected readonly errorDeJustificacion = signal('')
  protected readonly formato = signal('')
  protected readonly enviando = signal(false)
  protected readonly salida = signal<SalidaReporte | null>(null)
  protected readonly error = signal<ErrorTraducido | null>(null)

  protected poner(nombre: string, valor: string): void {
    this.valores.update((v) => ({ ...v, [nombre]: valor }))
  }

  protected enviar(): void {
    if (this.enviando()) return
    const errores: Record<string, string> = {}
    const parametros: Record<string, string> = {}
    for (const p of this.parametros()) {
      const valor = (this.valores()[p.nombre] ?? '').trim()
      if (valor) parametros[p.nombre] = valor
      else if (p.obligatorio) errores[p.nombre] = this.t.faltaParametro
    }
    this.errores.set(errores)
    const justificacion = this.justificacion().trim()
    const sensible = this.definicion().contieneDatosSensibles
    this.errorDeJustificacion.set(
      !sensible ? '' : justificacion.length < JUSTIFICACION_MINIMA ? this.t.justificacionCorta : justificacion.length > JUSTIFICACION_MAXIMA ? this.t.justificacionLarga : '',
    )
    if (Object.keys(errores).length || this.errorDeJustificacion()) return

    this.enviando.set(true)
    this.error.set(null)
    this.salida.set(null)
    const formato = FORMATO[this.formato()]
    this.ejecutar(this.definicion().definicionId, {
      parametros,
      ...(formato ? { formato } : {}),
      ...(sensible ? { justificacion } : {}),
    }).subscribe({
      next: (s) => {
        this.enviando.set(false)
        this.salida.set(s)
        this.ejecutada.emit(s)
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(false)
        this.error.set(e)
      },
    })
  }
}
