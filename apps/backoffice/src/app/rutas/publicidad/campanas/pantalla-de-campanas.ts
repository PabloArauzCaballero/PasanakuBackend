import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoMonto, aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { accionesDeCampana, cargadorDeCampanas, type Campana } from '../dominio/cu111-campanas'
import { textosPublicidad } from '../textos'
import { EntradaCampanaMonedaEnum, EntradaCampanaObjetivoEnum } from 'clientes/angular/publicidad/model/entradaCampana'

const COLUMNAS: ColumnaVirtual<Campana>[] = [
  { clave: 'nombre', titulo: 'Campaña', ordenable: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'presupuestoTotal', titulo: 'Presupuesto', numerica: true },
]

/**
 * CU-111 · Lado **gestión**: crear campañas. Nunca aprueba desde acá — el botón de
 * aprobar/rechazar vive en `PanelDeAprobacionDeCampana`, montado por una ruta distinta
 * con su propio `canMatch` (`PUBLICIDAD_APROBAR_CAMPANA`), no por un `if` en esta
 * pantalla. Es el mismo patrón de segregación que desembolsos/compras del resto del
 * backoffice (gestor ≠ aprobador). La creación va sin conjuntos: `EntradaCampana`
 * exige al menos uno; se declara hueco (armar conjuntos es su propio formulario, fuera
 * del alcance que definió `docs/Flujo de pantallas · backoffice administrador.md` §6
 * para esta pantalla — solo `FormularioAnunciante`/`PanelAprobacionCampana`).
 */
@Component({
  selector: 'ap-pantalla-de-campanas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo, CampoMonto, ChipEstado, Monto, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.propositoGestion" />
    <main>
      <header>
        <h1 data-tutorial-id="publicidad-campanas">{{ t.tituloGestion }}</h1>
        @if (puedeGestionar()) {
          <ap-boton data-tutorial-id="publicidad-nueva-campana" (pulsado)="abrir()">{{ t.nuevaCampana }}</ap-boton>
        }
      </header>
      <ap-tabla-de-datos-virtualizada data-tutorial-id="publicidad-tabla-campanas" [titulo]="t.tituloGestion" [columnas]="COLUMNAS" [cargador]="cargador" [ordenPermitido]="['nombre']" [identidad]="identidad">
        <ng-template #celda let-c let-columna="columna">
          @switch (columna.clave) {
            @case ('estado') {
              <ap-chip-estado [tono]="c.estado === 'ACTIVA' ? 'ok' : c.estado === 'RECHAZADA' ? 'error' : 'neutro'">{{ textoDeEstado(c.estado) }}</ap-chip-estado>
            }
            @case ('presupuestoTotal') {
              <ap-monto [monto]="c.presupuestoTotal" [moneda]="c.moneda" />
            }
            @default {
              {{ c[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </main>

    <ap-dialogo [titulo]="t.nuevaCampana" [textoDeConfirmar]="t.guardar" [abierto]="dialogoAbierto()" [cargando]="guardando()" (abiertoChange)="dialogoAbierto.set($event)" (confirmar)="guardar()" (cancelar)="cerrar()">
      <div class="formulario" data-tutorial-id="publicidad-formulario-campana">
        <ap-campo [etiqueta]="t.nombre" [(valor)]="nombre" [error]="intentoGuardar() && !nombre() ? 'Obligatorio' : undefined" />
        <ap-campo-monto [etiqueta]="t.presupuestoTotal" [(valor)]="presupuesto" [error]="intentoGuardar() && !presupuesto() ? 'Obligatorio' : undefined" />
      </div>
    </ap-dialogo>
  `,
  styles: `
    main { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    header { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    h1 { margin: 0; }
    .formulario { display: grid; gap: var(--s3); }
  `,
})
export class PantallaDeCampanas {
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeCampana()
  protected readonly t = textosPublicidad.campanas
  protected readonly COLUMNAS = COLUMNAS
  protected readonly identidad = (c: Campana) => c.campanaPublicitariaId
  protected readonly cargador = cargadorDeCampanas()
  protected readonly puedeGestionar = () => this.sesion.puede('PUBLICIDAD_ANUNCIANTES')
  protected readonly textoDeEstado = (estado: Campana['estado']) => this.t.estados[estado]

  protected readonly dialogoAbierto = signal(false)
  protected readonly guardando = signal(false)
  protected readonly intentoGuardar = signal(false)
  protected readonly nombre = signal('')
  protected readonly presupuesto = signal('')

  abrir(): void {
    this.nombre.set('')
    this.presupuesto.set('')
    this.intentoGuardar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
  }

  async guardar(): Promise<void> {
    this.intentoGuardar.set(true)
    const presupuestoContrato = this.presupuesto() ? aCadenaDelContrato(this.presupuesto()) : null
    if (!this.nombre().trim() || !presupuestoContrato) return
    this.guardando.set(true)
    try {
      // Hueco declarado: esta pantalla no captura todavía la cuenta publicitaria del
      // anunciante autenticado ni los conjuntos (`EntradaCampana.conjuntos` exige
      // mínimo 1) — el servidor va a rechazar esto con AP-CU111-0x hasta que se sume
      // el formulario de conjuntos (fuera del alcance que fijó `docs/Flujo de
      // pantallas · backoffice administrador.md` §6 para F14). Queda anotado en el
      // informe del carril como pendiente, no oculto.
      await this.acciones.crear({
        cuentaPublicitariaId: '00000000-0000-4000-8000-000000000000',
        nombre: this.nombre(),
        objetivo: EntradaCampanaObjetivoEnum.VisibilidadMarca,
        presupuestoTotal: presupuestoContrato,
        moneda: EntradaCampanaMonedaEnum.Bob,
        fechaInicio: new Date().toISOString(),
        conjuntos: [],
      })
      this.cerrar()
    } finally {
      this.guardando.set(false)
    }
  }
}
