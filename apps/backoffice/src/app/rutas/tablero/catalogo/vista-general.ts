import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { OpcionDeRetencion, TablaDelCatalogo } from 'clientes/angular/auditoria'
import { ESTADOS, REVISIONES, gobiernoDe, tieneDatosFinancieros, tieneDatosPersonales } from './gobierno'
import { textosCatalogo } from './textos'

/**
 * Pestaña «Vista general» de la ficha: las secciones clave–valor del portal de Atlas (identificación
 * técnica, contexto de negocio, clasificación, responsabilidad) más la narrativa de sistema que el
 * modelo deriva de la tabla.
 */
@Component({
  selector: 'ap-vista-general',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SeccionClaveValor],
  template: `
    <ap-seccion-clave-valor [titulo]="t.identificacion.titulo" [descripcion]="t.identificacion.descripcion" [items]="identificacion()" />
    <ap-seccion-clave-valor [titulo]="t.contexto.titulo" [descripcion]="t.contexto.descripcion" [items]="contexto()" />
    <ap-seccion-clave-valor [titulo]="t.clasificacion.titulo" [descripcion]="t.clasificacion.descripcion" [items]="clasificacion()" />
    <ap-seccion-clave-valor [titulo]="t.responsabilidad.titulo" [descripcion]="t.responsabilidad.descripcion" [items]="responsabilidad()" />
    <ap-seccion-clave-valor class="ancha" [titulo]="t.sistema.titulo" [descripcion]="t.sistema.descripcion" [items]="sistema()" />
  `,
  styles: `
    :host { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(min(100%, 22rem), 1fr)); align-items: start; }
    .ancha { grid-column: 1 / -1; }
  `,
})
export class VistaGeneral {
  readonly tabla = input.required<TablaDelCatalogo>()
  readonly politicas = input<readonly OpcionDeRetencion[]>([])
  protected readonly t = textosCatalogo.ficha

  protected readonly identificacion = computed<ClaveValor[]>(() => {
    const f = this.tabla()
    return [
      { etiqueta: this.t.identificacion.esquema, valor: f.esquema, mono: true },
      { etiqueta: this.t.identificacion.tabla, valor: f.tabla, mono: true },
      { etiqueta: this.t.identificacion.modulo, valor: `${f.modulo} · ${f.nombreModulo}` },
      { etiqueta: this.t.identificacion.servicio, valor: f.servicio ?? this.t.identificacion.compartido, mono: !!f.servicio },
      { etiqueta: this.t.identificacion.appendOnly, valor: f.appendOnly, tono: f.appendOnly ? 'info' : undefined },
    ]
  })
  protected readonly contexto = computed<ClaveValor[]>(() => {
    const f = this.tabla()
    const g = gobiernoDe(f)
    return [
      { etiqueta: this.t.contexto.queEs, valor: f.negocio.queEs },
      { etiqueta: this.t.contexto.negocio, valor: f.negocio.negocio },
      { etiqueta: this.t.contexto.porQue, valor: f.negocio.porQue },
      { etiqueta: this.t.contexto.responsable, valor: g.responsable, tono: g.responsable ? undefined : 'apagado' },
      { etiqueta: this.t.contexto.reglas, valor: f.reglas.join(' · ') },
    ]
  })
  protected readonly clasificacion = computed<ClaveValor[]>(() => {
    const f = this.tabla()
    const personales = tieneDatosPersonales(f)
    return [
      { etiqueta: this.t.clasificacion.personales, valor: personales, tono: personales ? 'pii' : undefined },
      { etiqueta: this.t.clasificacion.financieros, valor: tieneDatosFinancieros(f) },
      { etiqueta: this.t.clasificacion.secretos, valor: f.columnas.some((c) => c.sensibilidad === 'SECRETO') },
      { etiqueta: this.t.clasificacion.derivados, valor: f.columnas.some((c) => c.sensibilidad === 'DERIVADO') },
    ]
  })
  protected readonly responsabilidad = computed<ClaveValor[]>(() => {
    const g = gobiernoDe(this.tabla())
    const retencion = this.politicas().find((p) => p.id === g.politicaRetencionId)
    return [
      { etiqueta: this.t.responsabilidad.critica, valor: g.criticaParaAuditoria, tono: g.criticaParaAuditoria ? 'aviso' : undefined },
      { etiqueta: this.t.responsabilidad.retencion, valor: retencion ? `${retencion.entidad} · ${retencion.mesesRetencionActiva} meses · ${retencion.accionAlVencer.toLowerCase()}` : null },
      { etiqueta: this.t.responsabilidad.revision, valor: REVISIONES[g.estadoRevision] ?? g.estadoRevision },
      { etiqueta: 'Estado', valor: ESTADOS[g.estado] ?? g.estado },
      { etiqueta: this.t.responsabilidad.version, valor: g.version },
      { etiqueta: this.t.responsabilidad.actualizado, valor: g.actualizadoEn ? new Date(g.actualizadoEn).toLocaleString('es-BO', { dateStyle: 'medium', timeStyle: 'short' }) : null },
      { etiqueta: this.t.responsabilidad.nota, valor: g.notaRevision },
    ]
  })
  protected readonly sistema = computed<ClaveValor[]>(() => {
    const f = this.tabla()
    return [
      { etiqueta: this.t.sistema.narrativa, valor: f.sistema },
      { etiqueta: this.t.sistema.nota, valor: f.notaDeSistema },
    ]
  })
}
