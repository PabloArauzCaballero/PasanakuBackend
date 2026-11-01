import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import { Monto } from '@aportaya/ui/monto/monto'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { BarraDeLotes } from '../barra-de-lotes'
import { lotesDeAnunciantes, type Anunciante } from '../dominio/cu110-anunciantes'
import { filtroEnElServidor } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import { FormularioDeAnunciante } from './formulario-de-anunciante'

const t = textosPublicidad.anunciantes
const TIPOS: Record<Anunciante['tipo'], string> = { ORGANIZADOR: 'Organizador', SOCIO_COMERCIAL: 'Socio comercial' }
const limite = (a: Anunciante) => (a.limiteGastoMensual ? `${a.limiteGastoMensual.monto} ${a.limiteGastoMensual.moneda}` : t.sinLimite)

/**
 * CU-110 · Anunciantes, con la anatomía única del listado (2026-10-26): cifras de lo cargado, buscador por razón
 * social, tipo, estado y cuenta, filtros de estado (en el servidor), tipo y moneda (en lo cargado), CSV/JSON de lo
 * filtrado y paginación. El contrato **no tiene** editar ni eliminar anunciante: la fila ofrece lo que sí existe
 * detrás —el desempeño y la liquidación de su cuenta publicitaria (CU-113/114)—.
 */
@Component({
  selector: 'ap-pantalla-de-anunciantes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, BarraDeLotes, Boton, CabeceraDePagina, ChipEstado, FormularioDeAnunciante, Icono, ListadoDeDatos, Monto, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Publicidad" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (puedeGestionar()) {
          <ap-boton acciones icono="personaMas" variante="primario" (pulsado)="formulario().abrir()">{{ t.nuevoAnunciante }}</ap-boton>
        }
      </ap-cabecera-de-pagina>
      <!-- Las cifras cuentan lo cargado: sin carga (error o cargando) no se muestran, un «0» afirmaría algo que no se midió. -->
      @if (lotes.carga().tipo === 'lista') {
        <div class="cifras">
          <ap-tarjeta-kpi etiqueta="Cargados" [valor]="'' + lotes.filas().length" icono="lista" [pista]="lotes.hayMas() ? 'Hay más en el servidor' : 'Todos los del filtro'" />
          <ap-tarjeta-kpi etiqueta="Activos" [valor]="'' + cuenta('ACTIVO')" icono="verificado" tono="ok" pista="En lo cargado" />
          <ap-tarjeta-kpi etiqueta="Suspendidos" [valor]="'' + cuenta('SUSPENDIDO')" icono="pausa" [tono]="cuenta('SUSPENDIDO') ? 'aviso' : 'neutro'" pista="En lo cargado" />
          <ap-tarjeta-kpi etiqueta="Dados de baja" [valor]="'' + cuenta('DADO_DE_BAJA')" icono="prohibido" pista="En lo cargado" />
        </div>
      }
      @if (creado()) { <ap-alerta tono="ok">{{ t.creado }}</ap-alerta> }
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="lotes.carga()" [identidad]="porId" [exportables]="exportables" nombreDeRegistros="anunciantes" [ayudaDeBusqueda]="t.ayudaBusqueda" [textoVacio]="t.sinAnunciantes" [explicacionVacio]="t.explicacionVacio" (reintentar)="lotes.recargar()">
        <ap-barra-de-lotes accionesDeListado [hayMas]="lotes.hayMas()" [cargando]="lotes.cargandoMas()" [error]="lotes.errorDeMas()" (cargarMas)="lotes.cargarMas()" />
        <ng-template #celda let-a let-columna="columna">
          @switch (columna.clave) {
            @case ('razonSocialFacturacion') { <span class="nombre"><ap-icono [nombre]="a.tipo === 'ORGANIZADOR' ? 'grupo' : 'banco'" tamano="s4" />{{ a.razonSocialFacturacion }}</span> }
            @case ('tipo') { {{ textoDeTipo(a) }} }
            @case ('estado') { <ap-chip-estado [tono]="a.estado === 'ACTIVO' ? 'ok' : a.estado === 'DADO_DE_BAJA' ? 'error' : 'aviso'">{{ textoDeEstado(a) }}</ap-chip-estado> }
            @case ('limiteGastoMensual') {
              @if (a.limiteGastoMensual) { <ap-monto [monto]="a.limiteGastoMensual.monto" [moneda]="a.limiteGastoMensual.moneda" [etiqueta]="t.columnaLimite" /> } @else { {{ t.sinLimite }} }
            }
            @default { {{ a[columna.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-a>
          @if (a.cuentaPublicitariaId) {
            <ap-acciones-de-fila [fila]="a.razonSocialFacturacion">
              <button type="button" apAccionDeFila [attr.aria-label]="'Desempeño de ' + a.razonSocialFacturacion" (click)="irA('desempeno', a)"><ap-icono nombre="grafico" tamano="s4" /><span>Desempeño</span></button>
              <button type="button" apAccionDeFila [attr.aria-label]="'Liquidar ' + a.razonSocialFacturacion" (click)="irA('liquidacion', a)"><ap-icono nombre="recibo" tamano="s4" /><span>Liquidar</span></button>
            </ap-acciones-de-fila>
          } @else { <span class="sin-cuenta">{{ t.sinCuenta }}</span> }
        </ng-template>
      </ap-listado>
    </section>
    <ap-formulario-de-anunciante (creado)="alCrear()" />
  `,
  styles: `
    /* El host de ap-listado es una grilla de columna automática: sin esto, una tabla ancha lo ensancha (pedido al dueño del kit). */
    ap-listado { grid-template-columns: minmax(0, 1fr); }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(min(100%, 11rem), 1fr)); }
    .nombre { display: inline-flex; align-items: center; gap: var(--s2); max-width: 34ch; font-weight: 600; color: var(--text); white-space: normal; overflow-wrap: anywhere; }
    .sin-cuenta { color: var(--text-3); font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeAnunciantes {
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  protected readonly t = t
  protected readonly textoDeTipo = (a: Anunciante) => TIPOS[a.tipo]
  protected readonly textoDeEstado = (a: Anunciante) => t.estados[a.estado]
  protected readonly formulario = viewChild.required(FormularioDeAnunciante)
  protected readonly lotes = lotesDeAnunciantes()
  protected readonly creado = signal(false)
  protected readonly puedeGestionar = () => this.sesion.puede('PUBLICIDAD_ANUNCIANTES')
  protected readonly porId = (a: Anunciante) => a.anuncianteId
  protected readonly cuenta = (estado: Anunciante['estado']) => this.porEstado()[estado] ?? 0
  private readonly porEstado = computed(() => this.lotes.filas().reduce<Record<string, number>>((n, a) => ({ ...n, [a.estado]: (n[a.estado] ?? 0) + 1 }), {}))

  protected readonly listado = crearListado<Anunciante>({
    filas: this.lotes.filas,
    camposDeBusqueda: ['razonSocialFacturacion', (a) => TIPOS[a.tipo], (a) => t.estados[a.estado], 'cuentaPublicitariaId'],
    filtros: [
      { nombre: 'estado', etiqueta: 'Estado', valor: (a) => a.estado, opciones: (['ACTIVO', 'SUSPENDIDO', 'DADO_DE_BAJA'] as const).map((v) => ({ valor: v, texto: t.estados[v] })), ayuda: 'Se consulta al servidor.' },
      { nombre: 'tipo', etiqueta: 'Tipo', valor: (a) => a.tipo, texto: (v) => TIPOS[v as Anunciante['tipo']] ?? v },
      { nombre: 'moneda', etiqueta: 'Moneda del límite', valor: (a) => a.limiteGastoMensual?.moneda ?? 'SIN_LIMITE', texto: (v) => (v === 'SIN_LIMITE' ? t.sinLimite : v) },
    ],
    valorParaOrden: (a, clave) => (clave === 'limiteGastoMensual' ? (a.limiteGastoMensual?.monto ?? '') : (a as Record<string, unknown>)[clave]),
  })
  protected readonly columnas: Columna<Anunciante>[] = [
    { clave: 'razonSocialFacturacion', titulo: 'Razón social', ordenable: true },
    { clave: 'tipo', titulo: 'Tipo', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'limiteGastoMensual', titulo: 'Límite mensual', numerica: true, ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Anunciante>[] = [
    { clave: 'anuncianteId', titulo: 'Identificador' },
    { clave: 'razonSocialFacturacion', titulo: 'Razón social' },
    { clave: 'tipo', titulo: 'Tipo', valor: (a) => TIPOS[a.tipo] },
    { clave: 'estado', titulo: 'Estado', valor: (a) => t.estados[a.estado] },
    { clave: 'cuentaPublicitariaId', titulo: 'Cuenta publicitaria' },
    { clave: 'limiteGastoMensual', titulo: 'Límite mensual', valor: limite },
    { clave: 'creadoEn', titulo: 'Alta' },
  ]

  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
    filtroEnElServidor(this.listado, this.lotes)
  }

  protected irA(destino: 'desempeno' | 'liquidacion', a: Anunciante): void {
    void this.router.navigate(['/publicidad', destino], { queryParams: { cuenta: a.cuentaPublicitariaId } })
  }

  protected alCrear(): void {
    this.creado.set(true)
    this.lotes.recargar()
  }
}
