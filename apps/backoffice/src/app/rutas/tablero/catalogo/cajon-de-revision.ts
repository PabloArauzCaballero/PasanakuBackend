import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, signal } from '@angular/core'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Boton } from '@aportaya/ui/boton/boton'
import { CajonLateral } from '@aportaya/ui/cajon-lateral/cajon-lateral'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { ColumnaDelCatalogo, TablaDelCatalogo } from 'clientes/angular/auditoria'
import { mensajeDe } from '../../../nucleo/errores'
import { Sesion } from '../../../nucleo/sesion'
import { GuardadoDeRevision } from './dominio/guardado-de-revision'
import { CONFIANZAS, DECISIONES, borradorDeRevision, decisionEnPalabras, entradaDeRevision, faltaEnRevision, type BorradorDeRevision } from './revision'
import { textosCatalogo } from './textos'

/**
 * El cajón «Revisar columna X» (CU-131): decisión, confianza opcional y motivo (obligatorio al
 * rechazar), con la revisión vigente arriba. Guarda con la versión leída; si otra persona revisó
 * antes, lo dice y conserva lo escrito. Sin permiso de edición, solo muestra la revisión.
 */
@Component({
  selector: 'ap-cajon-de-revision',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Boton, CajonLateral, EstadoDeError, ListaDeOpciones, SeccionClaveValor],
  template: `
    <ap-cajon-lateral [titulo]="t.titulo(columna()?.nombre ?? '')" [(abierto)]="abierto">
      @if (columna(); as c) {
        <div class="cuerpo">
          <ap-seccion-clave-valor titulo="Lo que dice el catálogo" [items]="resumen()" />
          @if (puedeEditar()) {
            <form (submit)="$event.preventDefault(); guardar()">
              <ap-lista-de-opciones [etiqueta]="t.decision" [opciones]="decisiones" [valor]="borrador().decision" (valorChange)="cambiar('decision', $event)" [tooltip]="t.ayudaDecision" [error]="intentado() ? faltas().decision : undefined" />
              <ap-lista-de-opciones [etiqueta]="t.confianza" [opciones]="confianzas" [valor]="borrador().confianza" (valorChange)="cambiar('confianza', $event)" [textoVacio]="t.sinConfianza" [tooltip]="t.ayudaConfianza" />
              <ap-area-de-texto [etiqueta]="t.motivo" [maximo]="500" [valor]="borrador().motivo" (valorChange)="cambiar('motivo', $event)" [tooltip]="t.ayudaMotivo" [error]="intentado() ? faltas().motivo : undefined" />
              @if (error()) { <ap-estado-de-error [titulo]="t.noSeGuardo" [mensaje]="error()!" [conReintento]="false" /> }
            </form>
          } @else {
            <p class="ayuda">{{ t.soloLectura }}</p>
          }
        </div>
      }
      @if (columna() && puedeEditar()) {
        <ap-boton acciones variante="fantasma" icono="cerrar" (pulsado)="abierto.set(false)">Cancelar</ap-boton>
      }
      @if (columna() && puedeEditar()) {
        <ap-boton acciones variante="primario" icono="guardar" [cargando]="guardando()" [deshabilitado]="guardando()" (pulsado)="guardar()">{{ guardando() ? t.guardando : t.guardar }}</ap-boton>
      }
    </ap-cajon-lateral>
  `,
  styles: `
    .cuerpo { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); }
    form { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); }
    .ayuda { margin: 0; color: var(--text-3); }
  `,
})
export class CajonDeRevision {
  readonly tabla = input.required<TablaDelCatalogo>()
  readonly columna = input<ColumnaDelCatalogo | null>(null)
  readonly abierto = model(false)
  readonly guardado = output<void>()
  protected readonly t = textosCatalogo.revision
  protected readonly decisiones = Object.entries(DECISIONES).map(([valor, texto]) => ({ valor, texto }))
  protected readonly confianzas = Object.entries(CONFIANZAS).map(([valor, texto]) => ({ valor, texto }))
  private readonly sesion = inject(Sesion)
  private readonly remoto = inject(GuardadoDeRevision)
  protected readonly puedeEditar = computed(() => this.sesion.puede('CATALOGO_DATOS_EDITAR'))
  protected readonly borrador = signal<BorradorDeRevision>(borradorDeRevision())
  protected readonly faltas = computed(() => faltaEnRevision(this.borrador()))
  protected readonly intentado = signal(false)
  protected readonly guardando = signal(false)
  protected readonly error = signal<string | null>(null)
  protected readonly resumen = computed<ClaveValor[]>(() => {
    const c = this.columna()
    if (!c) return []
    return [
      { etiqueta: 'Tipo', valor: c.tipo, mono: true },
      { etiqueta: 'Negocio', valor: c.negocio },
      { etiqueta: 'Sistema', valor: c.sistema },
      { etiqueta: 'Revisión vigente', valor: decisionEnPalabras(c.revision) },
      { etiqueta: this.t.revisadoPor, valor: c.revision?.revisadoPor, mono: true },
    ]
  })

  constructor() {
    // Cada columna arranca con su revisión vigente como borrador; un cierre descarta lo no guardado.
    effect(() => {
      const c = this.columna()
      this.borrador.set(borradorDeRevision(c?.revision))
      this.intentado.set(false)
      this.error.set(null)
    })
  }

  protected cambiar<K extends keyof BorradorDeRevision>(campo: K, valor: BorradorDeRevision[K]): void {
    this.borrador.update((b) => ({ ...b, [campo]: valor }))
  }

  protected async guardar(): Promise<void> {
    const c = this.columna()
    if (!c || this.guardando()) return
    this.intentado.set(true)
    if (Object.keys(this.faltas()).length) return
    this.guardando.set(true)
    this.error.set(null)
    try {
      const t = this.tabla()
      await this.remoto.guardar(t.esquema, t.tabla, c.nombre, entradaDeRevision(this.borrador(), c.revision?.version ?? 0))
      this.abierto.set(false)
      this.guardado.emit()
    } catch (e) {
      const http = e instanceof HttpErrorResponse ? e : undefined
      this.error.set(mensajeDe(http?.error?.codigo, http?.status ?? 0))
    } finally {
      this.guardando.set(false)
    }
  }
}
