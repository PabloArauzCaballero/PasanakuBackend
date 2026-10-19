import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { OpcionDeRetencion, TablaDelCatalogo } from 'clientes/angular/auditoria'
import { ESTADOS, REVISIONES, gobiernoDe, tieneDatosFinancieros, tieneDatosPersonales } from './gobierno'
import { textosCatalogo } from './textos'

/** Pestaña «Gobierno» de la ficha: clasificación de datos, auditoría y responsabilidad y vigencia, como en Atlas. */
@Component({
  selector: 'ap-vista-gobierno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SeccionClaveValor],
  template: `
    <ap-seccion-clave-valor [titulo]="t.gobierno.clasificacion" [descripcion]="t.gobierno.clasificacionDescripcion" [items]="clasificacion()" />
    <ap-seccion-clave-valor [titulo]="t.gobierno.auditoria" [descripcion]="t.gobierno.auditoriaDescripcion" [items]="auditoria()" />
    <ap-seccion-clave-valor [titulo]="t.gobierno.vigencia" [descripcion]="t.gobierno.vigenciaDescripcion" [items]="vigencia()" />
  `,
  styles: `:host { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(min(100%, 22rem), 1fr)); align-items: start; }`,
})
export class VistaGobierno {
  readonly tabla = input.required<TablaDelCatalogo>()
  readonly politicas = input<readonly OpcionDeRetencion[]>([])
  protected readonly t = textosCatalogo.ficha

  protected readonly clasificacion = computed<ClaveValor[]>(() => {
    const f = this.tabla()
    const personales = tieneDatosPersonales(f)
    return [
      { etiqueta: this.t.clasificacion.personales, valor: personales, tono: personales ? 'pii' : undefined },
      { etiqueta: this.t.clasificacion.financieros, valor: tieneDatosFinancieros(f) },
      { etiqueta: this.t.clasificacion.secretos, valor: f.columnas.some((c) => c.sensibilidad === 'SECRETO') },
      { etiqueta: this.t.identificacion.appendOnly, valor: f.appendOnly },
    ]
  })
  protected readonly auditoria = computed<ClaveValor[]>(() => {
    const g = gobiernoDe(this.tabla())
    return [
      { etiqueta: this.t.responsabilidad.critica, valor: g.criticaParaAuditoria, tono: g.criticaParaAuditoria ? 'aviso' : undefined },
      { etiqueta: this.t.contexto.reglas, valor: this.tabla().reglas.join(' · ') },
    ]
  })
  protected readonly vigencia = computed<ClaveValor[]>(() => {
    const g = gobiernoDe(this.tabla())
    const retencion = this.politicas().find((p) => p.id === g.politicaRetencionId)
    return [
      { etiqueta: this.t.contexto.responsable, valor: g.responsable, tono: g.responsable ? undefined : 'apagado' },
      { etiqueta: this.t.responsabilidad.retencion, valor: retencion ? `${retencion.entidad} · ${retencion.mesesRetencionActiva} meses · ${retencion.accionAlVencer.toLowerCase()}` : null },
      { etiqueta: 'Estado', valor: ESTADOS[g.estado] ?? g.estado },
      { etiqueta: this.t.responsabilidad.revision, valor: REVISIONES[g.estadoRevision] ?? g.estadoRevision },
      { etiqueta: this.t.responsabilidad.revisadoPor, valor: g.revisadoPor, mono: true },
      { etiqueta: this.t.responsabilidad.nota, valor: g.notaRevision },
    ]
  })
}
