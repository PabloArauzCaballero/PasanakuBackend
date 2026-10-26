import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CabeceraDeSeccion } from '@aportaya/ui/cabecera-de-seccion/cabecera-de-seccion'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { DialogoDeConfirmacion } from '@aportaya/ui/dialogo-de-confirmacion/dialogo-de-confirmacion'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Interruptor } from '@aportaya/ui/interruptor/interruptor'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { Tarjeta } from '@aportaya/ui/tarjeta/tarjeta'
import { mensajeDe } from '../../../nucleo/errores'
import { ResumenDelCatalogoRemoto, clasificacionDe, fichaRemota } from './dominio'
import { GuardadoDeFicha } from './dominio/guardado-de-ficha'
import { ESTADOS, REVISIONES, borradorDe, entradaDe, gobiernoDe, type Borrador } from './gobierno'
import { textosCatalogo } from './textos'
import { textosGobierno as t } from './textos-gobierno'

/**
 * «Configurar tabla» (CU-130): la edición de la ficha de gobierno en su propia página, como en el
 * portal de Atlas: tarjeta «Ficha de negocio» con tooltip por campo, clasificación derivada (no se
 * edita) y un diálogo que dice qué va a cambiar antes de guardar. Se guarda con la versión leída; si
 * otra persona guardó antes, lo dice y el borrador se conserva.
 */
@Component({
  selector: 'ap-pantalla-de-ficha-de-gobierno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Boton, CabeceraDePagina, CabeceraDeSeccion, Campo, ChipEstado, DialogoDeConfirmacion, EstadoDeError, EstadoDePantalla, Interruptor, ListaDeOpciones, Tarjeta],
  template: `
    <ap-estado-de-pantalla [recurso]="remoto" [mensajeVacio]="textos.ficha.noExiste(esquema() + '.' + tabla())" [etiquetaDeCarga]="textos.cargando" (reintentar)="remoto.reload()">
      @if (ficha(); as f) {
        <ap-cabecera-de-pagina icono="baseDeDatos" [eyebrow]="t.eyebrow" [titulo]="t.configurar" [descripcion]="t.descripcion(f.esquema + '.' + f.tabla)">
          <ap-boton acciones variante="fantasma" tamano="sm" icono="flechaIzquierda" (pulsado)="volver()">{{ t.volver }}</ap-boton>
        </ap-cabecera-de-pagina>
        <form class="formulario" (submit)="$event.preventDefault(); pedirConfirmacion()">
          <ap-tarjeta [relieve]="true">
            <ap-cabecera-de-seccion icono="libro" [titulo]="t.fichaDeNegocio" [descripcion]="t.fichaDeNegocioDescripcion" />
            <div class="campos">
              <ap-campo [etiqueta]="t.responsable" [valor]="borrador().responsable" (valorChange)="cambiar('responsable', $event)" [tooltip]="t.ayudaResponsable" />
              <ap-lista-de-opciones [etiqueta]="t.retencion" [opciones]="opcionesRetencion()" [valor]="borrador().politicaRetencionId" (valorChange)="cambiar('politicaRetencionId', $event)" textoVacio="Sin política" [tooltip]="t.ayudaRetencion" />
              <ap-lista-de-opciones [etiqueta]="t.estado" [opciones]="opcionesEstado" [valor]="borrador().estado" (valorChange)="cambiar('estado', $event)" [tooltip]="t.ayudaEstado" />
              <ap-lista-de-opciones [etiqueta]="t.revision" [opciones]="opcionesRevision" [valor]="borrador().estadoRevision" (valorChange)="cambiar('estadoRevision', $event)" [tooltip]="t.ayudaRevision" />
              <ap-area-de-texto class="ancho" [etiqueta]="t.nota" [maximo]="500" [valor]="borrador().notaRevision" (valorChange)="cambiar('notaRevision', $event)" [tooltip]="t.ayudaNota" />
            </div>
          </ap-tarjeta>
          <ap-tarjeta [relieve]="true">
            <ap-cabecera-de-seccion icono="candado" [titulo]="t.clasificacion" [descripcion]="t.clasificacionDescripcion" />
            <div class="chips">
              <ap-chip-estado [tono]="personales() ? 'pii' : 'apagado'">{{ textos.ficha.clasificacion.personales }}: {{ personales() ? 'Sí' : 'No' }}</ap-chip-estado>
              <ap-chip-estado [tono]="financieros() ? 'info' : 'apagado'">{{ textos.ficha.clasificacion.financieros }}: {{ financieros() ? 'Sí' : 'No' }}</ap-chip-estado>
            </div>
            <ap-interruptor [activo]="borrador().criticaParaAuditoria" (activoChange)="cambiar('criticaParaAuditoria', $event)">{{ t.critica }}</ap-interruptor>
            <p class="ayuda">{{ t.ayudaCritica }}</p>
          </ap-tarjeta>
          @if (error()) { <ap-estado-de-error [titulo]="t.noSeGuardo" [mensaje]="error()!" [conReintento]="false" /> }
          <div class="acciones">
            <ap-boton variante="fantasma" icono="cerrar" (pulsado)="volver()">{{ t.cancelar }}</ap-boton>
            <ap-boton variante="primario" icono="guardar" tipo="submit" [deshabilitado]="cambios() === 0 || guardando()" [cargando]="guardando()">{{ t.guardar }}</ap-boton>
          </div>
        </form>
        <ap-dialogo-de-confirmacion [titulo]="t.confirmarTitulo" [descripcion]="t.confirmarDescripcion(f.esquema + '.' + f.tabla, cambios())" [textoDeConfirmar]="t.guardar" [cargando]="guardando()" [(abierto)]="confirmando" (confirmar)="guardar()" />
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    ap-estado-de-pantalla, .formulario { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .campos { display: grid; gap: var(--s4); grid-template-columns: repeat(auto-fit, minmax(min(100%, 16rem), 1fr)); margin-top: var(--s4); }
    .ancho { grid-column: 1 / -1; }
    .chips { display: flex; flex-wrap: wrap; gap: var(--s2); margin: var(--s4) 0; }
    .ayuda { margin: var(--s1) 0 0; font: var(--t-ayuda); color: var(--text-3); }
    .acciones { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: var(--s2); }
  `,
})
export class PantallaDeFichaDeGobierno {
  readonly esquema = input.required<string>()
  readonly tabla = input.required<string>()
  protected readonly t = t
  protected readonly textos = textosCatalogo
  protected readonly remoto = fichaRemota(() => this.esquema(), () => this.tabla())
  private readonly resumen = inject(ResumenDelCatalogoRemoto)
  private readonly guardado$ = inject(GuardadoDeFicha)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  protected readonly opcionesEstado = Object.entries(ESTADOS).map(([valor, texto]) => ({ valor, texto }))
  protected readonly opcionesRevision = Object.entries(REVISIONES).map(([valor, texto]) => ({ valor, texto }))
  protected readonly ficha = computed(() => (this.remoto.hasValue() ? this.remoto.value() : undefined))
  protected readonly opcionesRetencion = computed(() =>
    (this.resumen.recurso.hasValue() ? (this.resumen.recurso.value()?.politicasDeRetencion ?? []) : []).map((p) => ({ valor: p.id, texto: t.opcionRetencion(p.entidad, p.mesesRetencionActiva, p.accionAlVencer) })),
  )
  protected readonly personales = computed(() => {
    const f = this.ficha()
    return f ? clasificacionDe(f).personales + clasificacionDe(f).secretos > 0 : false
  })
  protected readonly financieros = computed(() => {
    const f = this.ficha()
    return f ? clasificacionDe(f).financieros > 0 : false
  })
  private readonly actual = computed(() => (this.ficha() ? gobiernoDe(this.ficha()!) : undefined))
  protected readonly borrador = signal<Borrador>(borradorDe({ estado: 'ACTIVA', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 0 } as never))
  protected readonly cambios = computed(() => {
    const a = this.actual()
    if (!a) return 0
    const base = borradorDe(a)
    return (Object.keys(base) as (keyof Borrador)[]).filter((k) => String(this.borrador()[k]).trim() !== String(base[k]).trim()).length
  })
  protected readonly confirmando = signal(false)
  protected readonly guardando = signal(false)
  protected readonly error = signal<string | null>(null)
  private cargado = false

  constructor() {
    // El borrador arranca con lo guardado una sola vez: un reload tras guardar no pisa lo que se está escribiendo.
    effect(() => {
      const a = this.actual()
      if (a && !this.cargado) {
        this.borrador.set(borradorDe(a))
        this.cargado = true
      }
    })
  }

  protected cambiar<K extends keyof Borrador>(campo: K, valor: Borrador[K]): void {
    this.borrador.update((b) => ({ ...b, [campo]: valor }))
  }
  protected pedirConfirmacion(): void {
    if (this.cambios() > 0 && !this.guardando()) this.confirmando.set(true)
  }
  protected volver(): void {
    void this.router.navigate(['..'], { relativeTo: this.ruta, queryParams: { vista: 'gobierno' } })
  }

  protected async guardar(): Promise<void> {
    const f = this.ficha()
    const a = this.actual()
    if (!f || !a || this.guardando()) return
    this.guardando.set(true)
    this.error.set(null)
    try {
      await this.guardado$.guardar(f.esquema, f.tabla, entradaDe(this.borrador(), a.version))
      this.confirmando.set(false)
      this.remoto.reload()
      // Las cifras del catálogo (estado, revisión, responsable) cambiaron con esta ficha.
      this.resumen.recurso.reload()
      this.volver()
    } catch (e) {
      const http = e instanceof HttpErrorResponse ? e : undefined
      this.confirmando.set(false)
      this.error.set(mensajeDe(http?.error?.codigo, http?.status ?? 0))
    } finally {
      this.guardando.set(false)
    }
  }
}
