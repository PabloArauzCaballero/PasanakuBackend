import { ChangeDetectionStrategy, Component, PendingTasks, effect, inject, signal, untracked } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoMonto, aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna, type EstadoColeccion, type Orden } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Sesion } from '../../../nucleo/sesion'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { accionesDeAnunciante, cargadorDeAnunciantes, type Anunciante } from '../dominio/cu110-anunciantes'
import { textosPublicidad } from '../textos'
import { EntradaAnuncianteMonedaEnum, EntradaAnuncianteTipoEnum } from 'clientes/angular/publicidad/model/entradaAnunciante'

const COLUMNAS: Columna<Anunciante>[] = [
  { clave: 'razonSocialFacturacion', titulo: 'Razón social', ordenable: true },
  { clave: 'tipo', titulo: 'Tipo' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'limiteGastoMensual', titulo: 'Límite mensual', numerica: true },
]
const TAMANO_DE_PAGINA = 50

/**
 * CU-110 · Anunciantes. Migrada al organismo canónico `@aportaya/ui/tabla-de-datos`
 * (PR7 §H4.S1.M2) — antes usaba `TablaDeDatosVirtualizada`. `cargadorDeAnunciantes()` trae
 * la lista completa por HTTP y esta pantalla es quien pagina y ordena en memoria (ya lo
 * hacía así el propio adaptador, ver `dominio/cu110-anunciantes.ts:29-43`).
 *
 * **Petición atrasada (PR7 §H2.S2.M3).** Cambiar de página dispara una nueva petición; si
 * una respuesta anterior llega después de una más nueva, se descarta con un contador de
 * petición — nunca pisa los datos vigentes ni resucita una fila que ya no está. Mientras
 * hay una petición más nueva en vuelo se sigue mostrando la última página válida con
 * `obsoleta: true`, no una tabla vacía parpadeando.
 */
@Component({
  selector: 'ap-pantalla-de-anunciantes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo, CampoMonto, ChipEstado, Monto, TablaDeDatos, Paginacion],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <header>
        <h1>{{ t.titulo }}</h1>
        @if (puedeGestionar()) {
          <ap-boton (pulsado)="abrir()">{{ t.nuevoAnunciante }}</ap-boton>
        }
      </header>
      <ap-tabla-de-datos
        [titulo]="t.titulo"
        [columnas]="COLUMNAS"
        [estado]="estado()"
        [(orden)]="orden"
        [identidad]="identidad"
        textoVacio="Todavía no hay anunciantes."
      >
        <ng-template #celda let-a let-columna="columna">
          @switch (columna.clave) {
            @case ('tipo') {
              {{ a.tipo === 'ORGANIZADOR' ? 'Organizador' : 'Socio comercial' }}
            }
            @case ('estado') {
              <ap-chip-estado [tono]="a.estado === 'ACTIVA' ? 'ok' : 'neutro'">{{ a.estado === 'ACTIVA' ? 'Activa' : 'Suspendida' }}</ap-chip-estado>
            }
            @case ('limiteGastoMensual') {
              @if (a.limiteGastoMensual) {
                <ap-monto [monto]="a.limiteGastoMensual" [moneda]="a.moneda" />
              } @else {
                Sin límite
              }
            }
            @default {
              {{ a[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos>
      <ap-paginacion [(pagina)]="pagina" [totalDeFilas]="total()" [porPagina]="tamanoDePagina" />
    </main>

    <ap-dialogo [titulo]="t.nuevoAnunciante" [textoDeConfirmar]="t.guardar" [abierto]="dialogoAbierto()" (abiertoChange)="dialogoAbierto.set($event)" (confirmar)="guardar()" (cancelar)="cerrar()" [cargando]="guardando()">
      <div class="formulario">
        <ap-campo [etiqueta]="t.razonSocial" [(valor)]="razonSocial" [error]="intentoGuardar() && !razonSocial() ? 'Obligatorio' : undefined" />
        <ap-campo-monto [etiqueta]="t.limiteGasto" [(valor)]="limite" />
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
export class PantallaDeAnunciantes {
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeAnunciante()
  private readonly cargador = cargadorDeAnunciantes()
  private readonly tareasPendientes = inject(PendingTasks)
  protected readonly t = textosPublicidad.anunciantes
  protected readonly COLUMNAS = COLUMNAS
  protected readonly tamanoDePagina = TAMANO_DE_PAGINA
  protected readonly identidad = (a: Anunciante) => a.anuncianteId
  protected readonly puedeGestionar = () => this.sesion.puede('PUBLICIDAD_ANUNCIANTES')

  protected readonly pagina = signal(1)
  protected readonly orden = signal<Orden | null>(null)
  protected readonly total = signal(0)
  protected readonly estado = signal<EstadoColeccion<Anunciante>>({ tipo: 'cargando' })
  private contadorDePeticion = 0

  protected readonly dialogoAbierto = signal(false)
  protected readonly guardando = signal(false)
  protected readonly intentoGuardar = signal(false)
  protected readonly razonSocial = signal('')
  protected readonly limite = signal('')

  constructor() {
    effect(() => {
      const pedido = { pagina: this.pagina(), tamano: this.tamanoDePagina, orden: this.orden(), filtros: {} }
      // `cargar` resuelve por `firstValueFrom` fuera de la petición HTTP misma: sin registrarla
      // como tarea pendiente, `ApplicationRef.isStable()` (zoneless) da por estable la app en
      // cuanto el interceptor de HTTP libera SU tarea (`finalize` sobre el observable), antes de
      // que este `await` termine de escribir `estado`. `whenStable()` de un test entonces
      // resuelve mudo, con la tabla todavía en 'cargando'. `PendingTasks.run` mantiene la app
      // inestable hasta que la promesa completa — HTTP incluido — termina de verdad.
      untracked(() => this.tareasPendientes.run(() => this.cargar(pedido)))
    })
  }

  /** Pide la página al cargador HTTP y descarta el resultado si ya se pidió una página más nueva mientras tanto. */
  private async cargar(pedido: { pagina: number; tamano: number; orden: Orden | null; filtros: Record<string, string> }): Promise<void> {
    const id = ++this.contadorDePeticion
    this.estado.update((actual) => (actual.tipo === 'lista' ? { ...actual, obsoleta: true } : { tipo: 'cargando' }))
    try {
      const respuesta = await this.cargador(pedido)
      if (id !== this.contadorDePeticion) return // respuesta atrasada: no reemplaza la vigente ni revive selección descartada
      this.total.set(respuesta.total)
      this.estado.set({ tipo: 'lista', filas: respuesta.filas })
    } catch (error) {
      if (id !== this.contadorDePeticion) return
      this.estado.set({ tipo: 'error', ...aEstadoDeError(error, id) })
    }
  }

  /** Vuelve a pedir la página vigente desde cero (tras dar de alta un anunciante); `cargar` ya descarta lo atrasado por su cuenta. */
  private recargar(): void {
    const pedido = { pagina: this.pagina(), tamano: this.tamanoDePagina, orden: this.orden(), filtros: {} }
    this.tareasPendientes.run(() => this.cargar(pedido))
  }

  abrir(): void {
    this.razonSocial.set('')
    this.limite.set('')
    this.intentoGuardar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
  }

  async guardar(): Promise<void> {
    this.intentoGuardar.set(true)
    if (!this.razonSocial().trim()) return
    this.guardando.set(true)
    try {
      const limiteContrato = this.limite() ? aCadenaDelContrato(this.limite()) : null
      await this.acciones.darDeAlta({
        tipo: EntradaAnuncianteTipoEnum.SocioComercial,
        razonSocialFacturacion: this.razonSocial(),
        moneda: EntradaAnuncianteMonedaEnum.Bob,
        ...(limiteContrato ? { limiteGastoMensual: limiteContrato } : {}),
      })
      this.cerrar()
      this.recargar()
    } finally {
      this.guardando.set(false)
    }
  }
}

/**
 * `erroresInterceptor` (`nucleo/errores.interceptor.ts`) ya traduce cualquier error HTTP a
 * `ErrorTraducido` con `trazaId` — el identificador de petición real del repo, no uno
 * inventado para esta pantalla. Si por algo no llega traducido (falla fuera de HTTP), se
 * cae al `motivo` genérico con el número de petición local como identificador.
 */
function aEstadoDeError(error: unknown, idLocal: number): { idPeticion: string; motivo: 'sin-permiso' | 'no-encontrado' | 'desconocido'; mensaje: string } {
  const e = error as Partial<ErrorTraducido> | null
  if (e && typeof e.mensaje === 'string') {
    const motivo = e.estado === 403 ? 'sin-permiso' : e.estado === 404 ? 'no-encontrado' : 'desconocido'
    return { idPeticion: e.trazaId ?? String(idLocal), motivo, mensaje: e.mensaje }
  }
  return { idPeticion: String(idLocal), motivo: 'desconocido', mensaje: 'No se pudo cargar la lista de anunciantes.' }
}
