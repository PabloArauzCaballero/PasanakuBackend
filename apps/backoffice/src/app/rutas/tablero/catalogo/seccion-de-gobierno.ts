import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Interruptor } from '@aportaya/ui/interruptor/interruptor'
import type { OpcionDeRetencion, TablaDelCatalogo } from 'clientes/angular/auditoria'
import { mensajeDe } from '../../../nucleo/errores'
import { Sesion } from '../../../nucleo/sesion'
import { AreaDeTexto, ListaDeOpciones, opcionesDe } from '../../cumplimiento/compartido/campos'
import { GuardadoDeFicha } from './dominio/guardado-de-ficha'
import { ESTADOS, POR_OMISION, REVISIONES, borradorDe, entradaDe, gobiernoDe, tonoDeEstado, tonoDeRevision, type Borrador } from './gobierno'
import { textosGobierno as t } from './textos-gobierno'

/**
 * CU-130 · la ficha de gobierno de una tabla, dentro de su ficha del catálogo. Quien tiene `CATALOGO_DATOS_EDITAR` la
 * edita; los demás la leen. Se guarda con la versión leída: si otra persona guardó antes, el servidor responde
 * AP-CU130-01, el borrador se conserva y se ofrece recargar. El botón se bloquea mientras guarda (95.3.4).
 */
@Component({
  selector: 'ap-seccion-de-gobierno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, AreaDeTexto, Boton, Campo, ChipEstado, Interruptor, ListaDeOpciones],
  template: `
    <section class="gobierno" aria-labelledby="titulo-gobierno">
      <h2 id="titulo-gobierno">{{ t.titulo }}</h2>
      <p class="estado">
        <ap-chip-estado [tono]="tonoEstado()">{{ estados[actual().estado] }}</ap-chip-estado>
        <ap-chip-estado [tono]="tonoRevision()">{{ revisionesTexto[actual().estadoRevision] }}</ap-chip-estado>
        <span class="version">{{ t.version(actual().version) }}</span>
      </p>
      @if (puedeEditar()) {
        <form class="formulario" (submit)="$event.preventDefault(); guardar()">
          <ap-lista-de-opciones [etiqueta]="t.estado" [opciones]="opcionesEstado" [valor]="borrador().estado" (valorChange)="cambiar('estado', $event)" [ayuda]="t.ayudaEstado" />
          <ap-lista-de-opciones [etiqueta]="t.revision" [opciones]="opcionesRevision" [valor]="borrador().estadoRevision" (valorChange)="cambiar('estadoRevision', $event)" />
          <ap-campo [etiqueta]="t.responsable" [valor]="borrador().responsable" (valorChange)="cambiar('responsable', $event)" [ayuda]="t.ayudaResponsable" />
          <ap-lista-de-opciones [etiqueta]="t.retencion" [opciones]="opcionesRetencion()" [valor]="borrador().politicaRetencionId" (valorChange)="cambiar('politicaRetencionId', $event)" [ayuda]="t.ayudaRetencion" />
          <ap-interruptor [activo]="borrador().criticaParaAuditoria" (activoChange)="cambiar('criticaParaAuditoria', $event)">{{ t.critica }}</ap-interruptor>
          <ap-area-de-texto class="ancho" [etiqueta]="t.nota" [maximo]="500" [valor]="borrador().notaRevision" (valorChange)="cambiar('notaRevision', $event)" />
          @if (error()) {
            <ap-alerta class="ancho" tono="error" [titulo]="t.noSeGuardo">{{ error() }}</ap-alerta>
          }
          @if (guardado()) {
            <ap-alerta class="ancho" tono="ok">{{ t.guardado }}</ap-alerta>
          }
          <div class="acciones ancho">
            <ap-boton variante="primario" tipo="submit" [cargando]="guardando()" [deshabilitado]="guardando()">{{ guardando() ? t.guardando : t.guardar }}</ap-boton>
          </div>
        </form>
      } @else {
        <dl>
          <dt>{{ t.responsable }}</dt><dd>{{ actual().responsable ?? t.sinAsignar }}</dd>
          <dt>{{ t.critica }}</dt><dd>{{ actual().criticaParaAuditoria ? 'Sí' : 'No' }}</dd>
          @if (actual().notaRevision) { <dt>{{ t.nota }}</dt><dd>{{ actual().notaRevision }}</dd> }
        </dl>
        <p class="ayuda">{{ t.soloLectura }}</p>
      }
    </section>
  `,
  styles: `
    .gobierno { display: grid; gap: var(--s3); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    h2 { margin: 0; }
    .estado { display: flex; flex-wrap: wrap; gap: var(--s2); align-items: center; margin: 0; }
    .version { color: var(--text-3); font-size: .9em; }
    .formulario { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr)); align-items: start; }
    .ancho { grid-column: 1 / -1; }
    .acciones { display: flex; justify-content: flex-end; }
    dl { display: grid; gap: var(--s2); margin: 0; }
    dt { font-weight: 600; }
    dd { margin: 0; color: var(--text-2); }
    .ayuda { margin: 0; color: var(--text-3); }
  `,
})
export class SeccionDeGobierno {
  readonly tabla = input.required<TablaDelCatalogo>()
  readonly politicas = input<readonly OpcionDeRetencion[]>([])
  /** Avisa que se guardó, para que la ficha recargue el catálogo. */
  readonly cambio = output<void>()

  protected readonly t = t
  protected readonly estados = ESTADOS
  protected readonly revisionesTexto = REVISIONES
  protected readonly opcionesEstado = opcionesDe(ESTADOS)
  protected readonly opcionesRevision = opcionesDe(REVISIONES)
  private readonly guardado$ = inject(GuardadoDeFicha)
  private readonly sesion = inject(Sesion)

  protected readonly actual = computed(() => gobiernoDe(this.tabla()))
  protected readonly tonoEstado = computed(() => tonoDeEstado(this.actual().estado))
  protected readonly tonoRevision = computed(() => tonoDeRevision(this.actual().estadoRevision))
  protected readonly puedeEditar = computed(() => this.sesion.puede('CATALOGO_DATOS_EDITAR'))
  protected readonly opcionesRetencion = computed(() =>
    this.politicas().map((p) => ({ valor: p.id, texto: t.opcionRetencion(p.entidad, p.mesesRetencionActiva, p.accionAlVencer) })),
  )
  protected readonly borrador = signal<Borrador>(borradorDe(POR_OMISION))
  protected readonly guardando = signal(false)
  protected readonly guardado = signal(false)
  protected readonly error = signal<string | null>(null)

  constructor() {
    // Al cambiar de tabla, o al recargar tras guardar, el borrador vuelve a lo guardado.
    effect(() => this.borrador.set(borradorDe(this.actual())))
  }

  protected cambiar<K extends keyof Borrador>(campo: K, valor: Borrador[K]): void {
    this.guardado.set(false)
    this.borrador.update((b) => ({ ...b, [campo]: valor }))
  }

  protected async guardar(): Promise<void> {
    if (this.guardando()) return
    this.guardando.set(true)
    this.error.set(null)
    try {
      const tabla = this.tabla()
      await this.guardado$.guardar(tabla.esquema, tabla.tabla, entradaDe(this.borrador(), this.actual().version))
      this.guardado.set(true)
      this.cambio.emit()
    } catch (e) {
      const http = e instanceof HttpErrorResponse ? e : undefined
      this.error.set(mensajeDe(http?.error?.codigo, http?.status ?? 0))
    } finally {
      this.guardando.set(false)
    }
  }
}
