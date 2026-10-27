import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, resource, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Icono } from '@aportaya/ui/icono/icono'
import { Interruptor as InterruptorUI } from '@aportaya/ui/interruptor/interruptor'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { puedeConfirmar } from '../dominio/datos-simulados'
import { PUERTO_DESPLIEGUES, type Despliegue, type DespliguesEInterruptores, type Interruptor } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const ESTADO: Record<Despliegue['estado'], string> = { exitoso: 'Exitoso', 'en curso': 'En curso', revertido: 'Revertido' }

/**
 * Despliegues recientes (listado del kit) + interruptores. El que toca dinero (`tocaDinero`) no se activa con un
 * clic: pide un correo de confirmación distinto al de quien lo solicitó — la interfaz impide confirmarlo solo/a
 * (`puedeConfirmar`), modelando doble persona.
 *
 * `interruptores` es un `linkedSignal`: deriva del recurso pero admite mutación local (activar/pedir/confirmar) sin
 * perder el vínculo — si el recurso se recarga (`reintentar`), vuelve a tomar los valores frescos.
 */
@Component({
  selector: 'ap-pantalla-despliegues',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, ChipEstado, CifrasDeSistemas, Dialogo, Icono, InterruptorUI, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="paquete">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="despliegues.isLoading()" (pulsado)="despliegues.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de despliegues" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado titulo="Despliegues" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por servicio, versión, quién desplegó o estado." nombreDeRegistros="despliegues" [textoVacio]="t.vacio" explicacionVacio="Cuando el pipeline publique un despliegue, aparece acá con su estado." (reintentar)="despliegues.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'estado') { <ap-chip-estado [tono]="f.estado === 'exitoso' ? 'ok' : f.estado === 'revertido' ? 'error' : 'info'">{{ estadoDe(f) }}</ap-chip-estado> } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>

      @if (despliegues.hasValue()) {
        <section class="interruptores" aria-labelledby="titulo-interruptores">
          <h2 id="titulo-interruptores"><ap-icono nombre="interruptor" tamano="s5" />Interruptores</h2>
          @for (i of interruptores(); track i.id) {
            <div class="fila">
              <ap-interruptor [activo]="i.activo" [deshabilitado]="i.tocaDinero" (activoChange)="cambiar(i, $event)">{{ i.nombre }}</ap-interruptor>
              @if (i.tocaDinero) {
                <button type="button" class="pedir-cambio" (click)="pedirCambio(i)"><ap-icono nombre="candado" tamano="s4" />{{ i.activo ? 'Pedir apagarlo' : 'Pedir activarlo' }} (toca dinero)</button>
              }
            </div>
            @if (solicitudPendiente()?.id === i.id) {
              <ap-alerta tono="aviso">{{ t.confirmarPrimeraPersona }}</ap-alerta>
            }
          } @empty {
            <p class="vacio">No hay interruptores configurados.</p>
          }
        </section>
      }

      <ap-dialogo [titulo]="t.dobleConfirmacionTitulo" [textoDeConfirmar]="t.confirmarSegundaPersona" [abierto]="dialogoAbierto()" [confirmarDeshabilitado]="!puedeConfirmarAhora()" (confirmar)="confirmar()" (cancelar)="cancelar()">
        <p>{{ t.dobleConfirmacionCuerpo }}</p>
        <ap-campo etiqueta="Correo de quien confirma" tipo="email" [(valor)]="correoConfirmante" />
        @if (correoConfirmante() && !puedeConfirmarAhora()) {
          <ap-alerta tono="error">No podés confirmar el mismo cambio que pediste. Necesitamos el correo de otra persona.</ap-alerta>
        }
      </ap-dialogo>
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }
    .interruptores { display: grid; gap: var(--s3); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    h2 { display: flex; align-items: center; gap: var(--s2); margin: 0; font: var(--t-titulo-3); color: var(--text); }
    .fila { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); min-width: 0; }
    .vacio { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .pedir-cambio { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--brand-texto); font: var(--t-cuerpo-chico); font-weight: 600; cursor: pointer; }
    .pedir-cambio:hover { background: var(--brand-bg); border-color: var(--brand); }
    .pedir-cambio:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaDespliegues {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.despliegues
  private readonly puerto = inject(PUERTO_DESPLIEGUES)
  protected readonly despliegues = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.despliegues)
  private readonly filas = filasDe<Despliegue, DespliguesEInterruptores>(this.despliegues, (d) => d.despliegues)
  protected readonly listado = crearListado<Despliegue>({
    filas: this.filas,
    camposDeBusqueda: ['servicio', 'version', 'desplegadoPor', (d) => ESTADO[d.estado], 'desplegadoEl'],
    filtros: [
      { nombre: 'servicio', etiqueta: 'Servicio', valor: (d) => d.servicio },
      { nombre: 'estado', etiqueta: 'Estado', valor: (d) => d.estado, texto: (v) => ESTADO[v as Despliegue['estado']] ?? v },
    ],
    porPagina: 10,
  })
  protected readonly columnas: Columna<Despliegue>[] = [
    { clave: 'servicio', titulo: 'Servicio', ordenable: true },
    { clave: 'version', titulo: 'Versión', mono: true },
    { clave: 'desplegadoEl', titulo: 'Desplegado el', ordenable: true },
    { clave: 'desplegadoPor', titulo: 'Por', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
  ]
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Despliegues', valor: contar(f), icono: 'paquete' },
      { etiqueta: 'Exitosos', valor: contar(f, (d) => d.estado === 'exitoso'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Revertidos', valor: contar(f, (d) => d.estado === 'revertido'), icono: 'deshacer', tono: 'error' },
      { etiqueta: 'Interruptores activos', valor: contar(this.interruptores(), (i) => i.activo), icono: 'interruptor' },
    ]
  })

  protected readonly interruptores = linkedSignal<Interruptor[]>(() => (this.despliegues.hasValue() ? (this.despliegues.value()?.interruptores ?? []) : []))
  protected readonly solicitudPendiente = signal<{ id: string; solicitante: string } | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly correoConfirmante = signal('')
  private readonly SOLICITANTE_ACTUAL = 'operador.actual@aportaya.bo' // TODO: reemplazar por Sesion cuando exista el correo del operador en el token

  protected estadoDe(d: Despliegue): string {
    return ESTADO[d.estado]
  }

  cambiar(i: Interruptor, activo: boolean): void {
    if (i.tocaDinero) return // no se toca directo: pasa por pedirCambio + doble confirmación
    this.interruptores.set(this.interruptores().map((x) => (x.id === i.id ? { ...x, activo } : x)))
  }

  pedirCambio(i: Interruptor): void {
    this.solicitudPendiente.set({ id: i.id, solicitante: this.SOLICITANTE_ACTUAL })
    this.correoConfirmante.set('')
    this.dialogoAbierto.set(true)
  }

  protected puedeConfirmarAhora(): boolean {
    const s = this.solicitudPendiente()
    if (!s) return false
    return puedeConfirmar(s.solicitante, this.correoConfirmante())
  }

  confirmar(): void {
    const s = this.solicitudPendiente()
    if (!s || !this.puedeConfirmarAhora()) return
    this.interruptores.set(this.interruptores().map((x) => (x.id === s.id ? { ...x, activo: !x.activo } : x)))
    this.solicitudPendiente.set(null)
    this.dialogoAbierto.set(false)
  }

  cancelar(): void {
    this.solicitudPendiente.set(null)
    this.dialogoAbierto.set(false)
  }
}
