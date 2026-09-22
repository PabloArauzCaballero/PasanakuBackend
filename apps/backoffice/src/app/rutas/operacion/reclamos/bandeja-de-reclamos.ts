import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { cargadorDeReclamos, vencido, type EstadoReclamo, type ReclamoDeBandeja } from '../dominio/cu52-reclamos'
import { textosOperacion } from '../textos'

const TONO_POR_ESTADO: Record<EstadoReclamo, 'ok' | 'aviso' | 'error' | 'info' | 'neutro'> = {
  INGRESADO: 'info',
  EN_ANALISIS: 'aviso',
  RESPONDIDO: 'ok',
  CERRADO: 'neutro',
  ELEVADO: 'error',
}

const COLUMNAS: ColumnaVirtual<ReclamoDeBandeja>[] = [
  { clave: 'codigo', titulo: 'Código', ordenable: true },
  { clave: 'categoria', titulo: 'Categoría' },
  { clave: 'canalIngreso', titulo: 'Canal' },
  { clave: 'plazoRespuesta', titulo: 'Plazo', ordenable: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'reclamoId', titulo: 'Acciones' },
]

/**
 * D-18 · Bandeja de reclamos (CU-52), sobre `TablaDeDatosVirtualizada` (del shell, no
 * se duplica) y ordenada por plazo desde el `cargador` (`cargarReclamos`, dominio):
 * el que vence antes va primero. `BarraDeFiltros` guarda el filtro de estado en la URL.
 * Segregación de funciones: **cualquier operador la ve**; solo quien tiene
 * `RECLAMO_ATENDER` puede abrir el diálogo de respuesta — el mismo permiso que CU-52
 * exige del lado del backend para `reclamo.respondido`, así el 403 real nunca es la
 * primera línea de defensa, solo la confirma.
 */
@Component({
  selector: 'ap-bandeja-de-reclamos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, ChipEstado, RelojDePlazo, Boton, Dialogo, TablaDeDatosVirtualizada, BarraDeFiltros],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1 data-tutorial-id="operacion-reclamos">{{ t.titulo }}</h1>
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtrosDeEstado" />
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.titulo"
        [columnas]="COLUMNAS"
        [cargador]="cargador"
        [ordenPermitido]="['plazoRespuesta', 'codigo']"
        [identidad]="identidad"
        [filtros]="filtrosActivos()"
      >
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('estado') {
              <ap-chip-estado [tono]="tonoDe(r)">{{ etiquetaDe(r) }}</ap-chip-estado>
            }
            @case ('plazoRespuesta') {
              <ap-reloj-de-plazo [etiqueta]="t.plazo" [venceIso]="r.plazoRespuesta" [ahoraIso]="ahoraIso" [norma]="t.norma" />
            }
            @case ('reclamoId') {
              @if (puedeAtender()) {
                <ap-boton [deshabilitado]="r.estado === 'CERRADO'" (pulsado)="abrirConfirmacion(r)">{{ t.responder }}</ap-boton>
              }
            }
            @default {
              {{ r[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </main>

    @if (seleccionado(); as r) {
      <ap-dialogo
        [titulo]="'Responder ' + r.codigo"
        [textoDeConfirmar]="'Confirmar respuesta de ' + r.codigo"
        [abierto]="dialogoAbierto()"
        (abiertoChange)="dialogoAbierto.set($event)"
        (confirmar)="confirmarRespuesta()"
        (cancelar)="cerrarConfirmacion()"
      >
        <p>{{ t.confirmarResponder(r.codigo) }}</p>
      </ap-dialogo>
    }
  `,
  styles: `
    main { padding: var(--s5); max-width: 64rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
  `,
})
export class BandejaDeReclamos {
  private readonly sesion = inject(Sesion)
  protected readonly t = textosOperacion.reclamos
  protected readonly TONO = TONO_POR_ESTADO
  protected readonly ETIQUETA: Record<EstadoReclamo, string> = textosOperacion.reclamos.estados
  protected readonly ahoraIso = new Date().toISOString()
  protected readonly COLUMNAS = COLUMNAS
  protected readonly identidad = (r: ReclamoDeBandeja) => r.reclamoId
  protected readonly cargador = cargadorDeReclamos()
  protected readonly filtrosDeEstado: Filtro[] = [
    { valor: 'INGRESADO', texto: 'Ingresado' },
    { valor: 'EN_ANALISIS', texto: 'En análisis' },
    { valor: 'ELEVADO', texto: 'Elevado' },
  ]
  private readonly ruta = inject(ActivatedRoute)
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegido = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean)[0]
    const filtros: Record<string, string> = {}
    if (elegido) filtros['estado'] = elegido
    return filtros
  })

  protected readonly seleccionado = signal<ReclamoDeBandeja | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly puedeAtender = computed(() => this.sesion.puede('RECLAMO_ATENDER'))
  protected readonly vencido = vencido

  tonoDe(r: ReclamoDeBandeja): 'ok' | 'aviso' | 'error' | 'info' | 'neutro' {
    return this.TONO[r.estado]
  }

  etiquetaDe(r: ReclamoDeBandeja): string {
    return this.ETIQUETA[r.estado]
  }

  abrirConfirmacion(r: ReclamoDeBandeja): void {
    this.seleccionado.set(r)
    this.dialogoAbierto.set(true)
  }

  cerrarConfirmacion(): void {
    this.dialogoAbierto.set(false)
    this.seleccionado.set(null)
  }

  confirmarRespuesta(): void {
    // El envío real queda pendiente del contrato `POST /reclamos/{id}/respuesta`
    // (ver el supuesto declarado en dominio/cu52-reclamos.ts). El diálogo ya exige
    // la confirmación con el código concreto delante, que es lo que pide el gate.
    this.cerrarConfirmacion()
  }
}
