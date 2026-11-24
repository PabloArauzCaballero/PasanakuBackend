import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { DialogoDeConfirmacion } from '@aportaya/ui/dialogo-de-confirmacion/dialogo-de-confirmacion'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { ListaDeOpciones } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { Tono } from '@aportaya/ui/tono/tono'
import { EntradaVersionParametroMonedaEnum } from 'clientes/angular/tarifas/model/entradaVersionParametro'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import {
  cargadorDeParametrosFinancieros,
  crearAprobarVersionDeParametro,
  crearCargarHistorialDeParametro,
  crearProponerVersionDeParametro,
  operadoresDe,
  type EntradaVersionDeParametro,
  type ParametroFinanciero,
  type VersionDeParametro,
} from '../dominio/cu132-parametros'
import { textosConfiguracion } from '../textos'
import { crearNombresDeOperadores } from './nombres-de-operadores'
import { BORRADOR_VACIO, TONO, validarBorrador, type Borrador, type CampoDePropuesta, type ErroresDeCampo, type Historial } from './modelo-pantalla-parametros'

/** CU-132 · Administrar valores solo desde el catálogo y con fuente declarada; el catálogo no incorpora valores por defecto. */
@Component({
  selector: 'ap-pantalla-de-parametros-financieros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, ChipEstado, DialogoDeConfirmacion, Esqueleto, EstadoDeError, EstadoVacio, Fecha, ListaDeOpciones, NgTemplateOutlet],
  templateUrl: './pantalla-de-parametros-financieros.html',
  styleUrl: './pantalla-de-parametros-financieros.scss',
})
export class PantallaDeParametrosFinancieros implements OnInit {
  constructor() {
    inject(ExplicadorDeVista).fijar(textosConfiguracion.parametros.proposito)
  }

  protected readonly t = textosConfiguracion
  protected readonly monedas = [{ valor: 'BOB', texto: 'BOB' }, { valor: 'USD', texto: 'USD' }]
  protected readonly cargando = signal(false)
  protected readonly parametros = signal<ParametroFinanciero[]>([])
  protected readonly pendientes = computed(() => this.parametros().reduce((n, p) => n + p.propuestas.length, 0))
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly errorDePropuesta = signal<ErrorTraducido | null>(null)
  protected readonly versionEnAprobacion = signal<VersionDeParametro | null>(null)
  protected readonly confirmandoAprobacion = signal(false)
  protected readonly aviso = signal('')
  protected readonly enviando = signal(false)
  protected readonly codigoEnEdicion = signal<string | null>(null)
  protected readonly borrador = signal<Borrador>({ ...BORRADOR_VACIO })
  protected readonly erroresDeCampo = signal<ErroresDeCampo>({})
  protected readonly historial = signal<Historial>({})
  protected readonly historialAbierto = signal<Record<string, boolean>>({})
  protected readonly cargandoHistorial = signal<Record<string, boolean>>({})
  protected readonly erroresHistorial = signal<Record<string, ErrorTraducido | undefined>>({})
  protected readonly erroresDeAprobacion = signal<Record<string, ErrorTraducido | undefined>>({})
  protected readonly operadores = crearNombresDeOperadores(textosConfiguracion.parametros)
  private readonly sesion = inject(Sesion)
  protected readonly puedeAdministrar = () => this.sesion.puede('TARIFARIO_ADMINISTRAR')
  private readonly cargarCatalogo = cargadorDeParametrosFinancieros()
  private readonly cargarHistorial = crearCargarHistorialDeParametro()
  private readonly proponer = crearProponerVersionDeParametro()
  private readonly aprobarVersion = crearAprobarVersionDeParametro()

  ngOnInit(): void {
    this.recargar()
  }

  protected recargar(): void {
    this.cargando.set(true)
    this.error.set(null)
    this.cargarCatalogo.subscribe({
      next: (lista) => { this.parametros.set(lista); this.cargando.set(false); this.operadores.resolver(operadoresDe(lista.flatMap((p) => [...p.vigentes, ...p.propuestas]))) },
      error: (e: ErrorTraducido) => { this.error.set(e); this.cargando.set(false) },
    })
  }

  protected abrirPropuesta(p: ParametroFinanciero): void {
    this.codigoEnEdicion.set(p.codigo)
    this.borrador.set({ ...BORRADOR_VACIO })
    this.erroresDeCampo.set({})
    this.errorDePropuesta.set(null)
    this.aviso.set('')
  }

  protected cerrarPropuesta(): void {
    this.codigoEnEdicion.set(null)
    this.erroresDeCampo.set({})
    this.errorDePropuesta.set(null)
  }

  protected editar(campo: CampoDePropuesta, valor: string): void {
    this.borrador.update((b) => ({ ...b, [campo]: valor }))
    this.erroresDeCampo.update((e) => ({ ...e, [campo]: undefined }))
    this.errorDePropuesta.set(null)
  }

  protected enviarPropuesta(p: ParametroFinanciero): void {
    if (this.enviando()) return
    const b = this.borrador()
    const errores = validarBorrador(p, b, this.t.parametros)
    this.erroresDeCampo.set(errores)
    if (Object.keys(errores).length) return

    const entrada: EntradaVersionDeParametro = {
      valor: b['valor'].trim(),
      vigenteDesde: new Date(b['vigenteDesde']).toISOString(),
      nivel: p.porNivel ? b['nivel'].trim() : null,
      moneda: p.unidad === 'MONTO' ? (b['moneda'] === 'BOB' ? EntradaVersionParametroMonedaEnum.Bob : EntradaVersionParametroMonedaEnum.Usd) : null,
      procedencia: { fuente: b['fuente'].trim(), referencia: b['referencia'].trim(), fecha: b['fecha'] },
    }
    this.enviando.set(true)
    this.errorDePropuesta.set(null)
    this.proponer(p.codigo, entrada).subscribe({
      next: () => {
        this.enviando.set(false)
        this.aviso.set(this.t.parametros.propuestaRegistrada)
        this.codigoEnEdicion.set(null)
        this.borrador.set({ ...BORRADOR_VACIO })
        this.recargar()
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(false)
        this.errorDePropuesta.set(e)
        if (e.codigo === 'AP-CU132-02') {
          const campos: ErroresDeCampo = {}
          if (p.porNivel) campos['nivel'] = e.mensaje
          if (p.unidad === 'MONTO') campos['moneda'] = e.mensaje
          this.erroresDeCampo.update((actual) => ({ ...actual, ...campos }))
        }
      },
    })
  }

  protected pedirAprobacion(v: VersionDeParametro): void {
    this.versionEnAprobacion.set(v)
    this.confirmandoAprobacion.set(true)
  }

  protected cancelarAprobacion(): void {
    this.versionEnAprobacion.set(null)
  }

  protected aprobar(v: VersionDeParametro): void {
    if (this.enviando()) return
    this.enviando.set(true)
    this.erroresDeAprobacion.update((e) => ({ ...e, [v.codigo]: undefined }))
    this.aprobarVersion(v.id).subscribe({
      next: () => {
        this.enviando.set(false)
        this.confirmandoAprobacion.set(false)
        this.versionEnAprobacion.set(null)
        this.aviso.set(this.t.parametros.aprobada)
        this.recargar()
        if (this.historialAbierto()[v.codigo]) this.cargarVersiones(v.codigo)
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(false)
        this.erroresDeAprobacion.update((actual) => ({ ...actual, [v.codigo]: e }))
      },
    })
  }

  protected alternarHistorial(codigo: string): void {
    const abrir = !this.historialAbierto()[codigo]
    this.historialAbierto.update((h) => ({ ...h, [codigo]: abrir }))
    if (abrir && !this.historial()[codigo] && !this.cargandoHistorial()[codigo]) this.cargarVersiones(codigo)
  }

  private cargarVersiones(codigo: string): void {
    this.cargandoHistorial.update((c) => ({ ...c, [codigo]: true }))
    this.erroresHistorial.update((e) => ({ ...e, [codigo]: undefined }))
    this.cargarHistorial(codigo).subscribe({
      next: (versiones) => {
        this.historial.update((h) => ({ ...h, [codigo]: versiones }))
        this.cargandoHistorial.update((c) => ({ ...c, [codigo]: false }))
        this.operadores.resolver(operadoresDe(versiones))
      },
      error: (e: ErrorTraducido) => {
        this.erroresHistorial.update((actual) => ({ ...actual, [codigo]: e }))
        this.cargandoHistorial.update((c) => ({ ...c, [codigo]: false }))
      },
    })
  }

  protected tono(estado: VersionDeParametro['estado']): Tono { return TONO[estado] }
  protected estado(estado: VersionDeParametro['estado']): string { return this.t.parametros.estados[estado] }
}
