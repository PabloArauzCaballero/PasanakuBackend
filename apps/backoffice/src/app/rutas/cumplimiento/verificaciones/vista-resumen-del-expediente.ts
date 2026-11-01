import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { formatearDia, formatearFecha } from '@aportaya/ui/fecha/fecha'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { carpetaDelExpediente, vencimientoDe } from '../dominio/cu02-expedientes'
import { caraEnPalabras, documentoEnPalabras, estadoEnPalabras, explicacionDelEstado, fotosEnNumeros, fotosFaltantes, tiempoEnCola, vencimientoEnPalabras } from '../dominio/cu02-presentacion'
import { textosVerificaciones } from './textos-verificaciones'

const T = textosVerificaciones.ficha
const R = T.rotulos
const A = T.ausente

/** Un dato ausente no se deja en blanco ni como «—» mudo: se dice por qué falta, en gris. */
const oExplicado = (etiqueta: string, valor: string | undefined | null, motivo: string, extra: Partial<ClaveValor> = {}): ClaveValor =>
  valor ? { etiqueta, valor, ...extra } : { etiqueta, valor: motivo, tono: 'apagado' }

/**
 * La pestaña «Resumen» de la ficha: la persona, el documento y el expediente, cada uno en su sección clave–valor.
 * **Ningún dato queda en blanco**: si el contrato no lo trae, se explica por qué (cuenta sin correo, número que no
 * viaja a propósito, expediente sin resolver). Los identificadores van como código copiable, para cruzarlos con la
 * bitácora o con soporte sin transcribirlos.
 */
@Component({
  selector: 'ap-vista-resumen-del-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SeccionClaveValor],
  template: `
    <ap-seccion-clave-valor [titulo]="t.persona.titulo" [descripcion]="t.persona.descripcion" [items]="persona()" />
    <ap-seccion-clave-valor [titulo]="t.documento.titulo" [descripcion]="t.documento.descripcion" [items]="documento()" />
    <ap-seccion-clave-valor class="ancha" [titulo]="t.expediente.titulo" [descripcion]="t.expediente.descripcion" [items]="expedienteItems()" />
  `,
  styles: `
    :host { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 24rem), 1fr)); gap: var(--s4); align-items: start; min-width: 0; }
    .ancha { grid-column: 1 / -1; }
  `,
})
export class VistaResumenDelExpediente {
  readonly expediente = input.required<ExpedienteEnRevision>()
  protected readonly t = T

  protected readonly persona = computed<ClaveValor[]>(() => {
    const e = this.expediente()
    return [
      { etiqueta: R.nombre, valor: e.nombreCompleto },
      { etiqueta: R.telefono, valor: e.telefono, mono: true },
      oExplicado(R.correo, e.correo, A.correo, { mono: true }),
      { etiqueta: R.usuarioId, valor: e.usuarioId, mono: true },
    ]
  })

  protected readonly documento = computed<ClaveValor[]>(() => {
    const e = this.expediente()
    const d = documentoEnPalabras(e.documento)
    const v = vencimientoDe(e)
    return [
      oExplicado(R.tipo, d.tipo, A.documento),
      oExplicado(R.lugar, d.lugar, d.tipo ? A.lugar : A.documento),
      { etiqueta: R.numero, valor: A.numero, tono: 'apagado' },
      oExplicado(R.vence, e.fechaExpiracionDocumento ? formatearDia(e.fechaExpiracionDocumento) : null, A.vence, { tono: v === 'vigente' ? undefined : 'aviso' }),
      { etiqueta: R.vigencia, valor: vencimientoEnPalabras(e), tono: v === 'vigente' ? 'ok' : 'aviso' },
    ]
  })

  protected readonly expedienteItems = computed<ClaveValor[]>(() => {
    const e = this.expediente()
    const faltan = fotosFaltantes(e)
    return [
      { etiqueta: R.estado, valor: estadoEnPalabras(e.estado) },
      { etiqueta: R.queSignifica, valor: explicacionDelEstado(e.estado), texto: true },
      { etiqueta: R.iniciada, valor: formatearFecha(e.iniciadaEn) },
      oExplicado(R.resuelta, e.resueltaEn ? formatearFecha(e.resueltaEn) : null, A.resuelta),
      { etiqueta: R.enCola, valor: tiempoEnCola(e, new Date()) + (e.resueltaEn ? ' (hasta la decisión)' : ' (y sigue esperando)') },
      oExplicado(R.motivo, e.motivoRechazo, A.motivo, { tono: 'error' }),
      { etiqueta: R.fotos, valor: fotosEnNumeros(e) + (faltan.length ? ` · faltan: ${faltan.map(caraEnPalabras).join(', ')}` : ' · están todas'), tono: faltan.length ? 'aviso' : 'ok' },
      { etiqueta: R.verificacionId, valor: e.verificacionId, mono: true },
      { etiqueta: R.carpeta, valor: carpetaDelExpediente(e.usuarioId), mono: true },
    ]
  })
}
