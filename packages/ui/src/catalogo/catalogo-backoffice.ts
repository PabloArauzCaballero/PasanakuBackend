import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { AccionesDeFila } from '../acciones-de-fila/acciones-de-fila'
import { AreaDeTexto } from '../area-de-texto/area-de-texto'
import { BarraDeFiltros } from '../barra-de-filtros/barra-de-filtros'
import { Boton } from '../boton/boton'
import { CajonLateral } from '../cajon-lateral/cajon-lateral'
import { CodigoCopiable } from '../codigo-copiable/codigo-copiable'
import { DialogoDeConfirmacion } from '../dialogo-de-confirmacion/dialogo-de-confirmacion'
import { Pestanas } from '../pestanas/pestanas'
import { SeccionClaveValor } from '../seccion-clave-valor/seccion-clave-valor'
import { BotonCopiar } from '../boton-copiar/boton-copiar'
import { CabeceraDePagina } from '../cabecera-de-pagina/cabecera-de-pagina'
import { CabeceraDeSeccion } from '../cabecera-de-seccion/cabecera-de-seccion'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Esqueleto } from '../esqueleto/esqueleto'
import { EstadoDeError } from '../estado-de-error/estado-de-error'
import { ExportarListado } from '../exportar-listado/exportar-listado'
import { crearListado } from '../listado/crear-listado'
import { ListadoDeDatos } from '../listado/listado'
import { ListaDeOpciones } from '../lista-de-opciones/lista-de-opciones'
import { Migas } from '../migas/migas'
import { NotaDeVista } from '../nota-de-vista/nota-de-vista'
import { Paginacion } from '../paginacion/paginacion'
import { TarjetaKPI } from '../tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '../tabla-de-datos/tabla-de-datos'
import { Tooltip } from '../tooltip/tooltip'
import { SeccionDeCatalogo } from './seccion'

type Muestra = { id: string; tabla: string; esquema: string }

/**
 * La anatomía de una pantalla del backoffice (la del portal interno de Atlas, con las piezas de
 * la casa): migas, cabecera de página, nota de la vista, cifras, barra de filtros, estados y
 * paginación completa. Es una muestra con datos fijos; no llama a nada.
 */
@Component({
  selector: 'ap-catalogo-backoffice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, AreaDeTexto, ExportarListado, ListadoDeDatos, BarraDeFiltros, Boton, BotonCopiar, CajonLateral, CodigoCopiable, DialogoDeConfirmacion, Pestanas, SeccionClaveValor, CabeceraDePagina, CabeceraDeSeccion, ChipEstado, Esqueleto, EstadoDeError, ListaDeOpciones, Migas, NotaDeVista, Paginacion, TarjetaKPI, Tooltip, SeccionDeCatalogo],
  template: `
    <ap-seccion-de-catalogo nombre="Anatomía del backoffice" ancla="backoffice">
      <ap-migas [migas]="[{ texto: 'Inicio', ruta: '/' }, { texto: 'Sistemas', ruta: '/sistemas' }, { texto: 'Catálogo de datos' }]" />
      <ap-cabecera-de-pagina icono="baseDeDatos" eyebrow="Sistemas" titulo="Catálogo de datos" descripcion="Cada tabla con su estado, quién responde por ella y qué retención le aplica.">
        <ap-boton-copiar acciones valor="catálogo" etiqueta="Copiar el enlace" />
      </ap-cabecera-de-pagina>
      <ap-nota-de-vista>Esta vista muestra las tablas reales de la base. Existe para que sistemas y auditoría sepan qué significa cada una sin preguntarlo.</ap-nota-de-vista>
      <div class="cifras">
        <ap-tarjeta-kpi etiqueta="Tablas" valor="338" icono="baseDeDatos" />
        <ap-tarjeta-kpi etiqueta="Activas" valor="336" icono="verificado" tono="ok" pista="de 338" />
        <ap-tarjeta-kpi etiqueta="Sin revisar" valor="335" icono="alerta" tono="aviso" />
        <ap-tarjeta-kpi etiqueta="Con datos personales" valor="48" icono="persona" tono="pii" />
      </div>
      <ap-barra-de-filtros etiquetaDeBusqueda="Buscar tabla, columna o concepto" ayudaDeBusqueda="Busca en el nombre de la tabla, sus columnas y su descripción." [busqueda]="busqueda()" [selectores]="selectores" (busquedaChange)="busqueda.set($event)" />
      <ap-cabecera-de-seccion icono="libro" titulo="Ficha de negocio" descripcion="Qué es, para qué sirve y por qué existe." />
      <div class="chips">
        <ap-chip-estado tono="ok" [vivo]="true">Arriba</ap-chip-estado>
        <ap-chip-estado tono="pii">Datos personales</ap-chip-estado>
        <ap-chip-estado tono="apagado">Detectada</ap-chip-estado>
      </div>
      <ap-lista-de-opciones etiqueta="Estado" [opciones]="[{ valor: 'ACTIVA', texto: 'Activa', descripcion: 'La tabla está en uso.' }]" ayuda="Si la tabla sigue en uso." tooltip="Activa, planificada, candidata a retiro, retirada o deshabilitada." style="max-width: calc(var(--s7) * 6)" />
      <ap-tooltip texto="Un tooltip por hover y foco."><button type="button" class="demo">Pasá por acá</button></ap-tooltip>
      <ap-esqueleto forma="filas" [lineas]="4" etiqueta="Consultando el catálogo" />
      <ap-estado-de-error mensaje="Probá de nuevo en un momento." trazaId="9cfecf77-a16b" />
      <ap-paginacion etiqueta="Paginación del catálogo" [totalDeFilas]="338" [pagina]="1" [opcionesPorPagina]="[10, 20, 50]" />
      <ap-listado titulo="Tablas de muestra" ayudaDeBusqueda="Busca por tabla y esquema." [columnas]="columnasDeMuestra" [listado]="muestra">
        <ng-template #acciones let-f><ap-acciones-de-fila [fila]="f.tabla" [ver]="true" [editar]="true" [eliminar]="true" /></ng-template>
      </ap-listado>
      <ap-exportar-listado nombre="tablas de muestra" [columnas]="columnasDeMuestra" [filas]="filasDeMuestra" />
      <div class="chips"><ap-boton icono="guardar" variante="primario">Guardar</ap-boton><ap-boton icono="descargar">Descargar</ap-boton><ap-boton icono="eliminar" variante="peligro">Eliminar</ap-boton></div>
      <ap-pestanas etiqueta="Vista de la ficha" estilo="segmentada" [pestanas]="[{ valor: 'general', texto: 'Vista general' }, { valor: 'columnas', texto: 'Columnas', cuenta: 18 }, { valor: 'gobierno', texto: 'Gobierno' }]" [(elegida)]="vista" />
      @for (v of ['general', 'columnas', 'gobierno']; track v) {
        <div role="tabpanel" [id]="'panel-' + v" [attr.aria-labelledby]="'pestana-' + v" [hidden]="vista() !== v">Panel «{{ v }}»: lo dibuja quien usa las pestañas.</div>
      }
      <ap-seccion-clave-valor titulo="Identificación técnica" descripcion="Cómo existe la tabla en la base y en el código." [items]="[{ etiqueta: 'Esquema', valor: 'aportes', mono: true }, { etiqueta: 'Tabla', valor: 'pago', mono: true }, { etiqueta: 'Append-only', valor: false }, { etiqueta: 'Crítica para auditoría', valor: true, tono: 'aviso' }]" style="width: 100%; max-width: calc(var(--s7) * 10)" />
      <ap-codigo-copiable valor="9cfecf77-a16b-4726" nombre="el código de seguimiento" />
      <ap-area-de-texto etiqueta="Nota de revisión" [maximo]="500" ayuda="Por qué se decidió así." tooltip="Queda en la bitácora con tu nombre." style="width: 100%; max-width: calc(var(--s7) * 8)" />
      <div class="chips">
        <ap-boton variante="secundario" (pulsado)="cajon.set(true)">Abrir el cajón lateral</ap-boton>
        <ap-boton variante="secundario" (pulsado)="confirmacion.set(true)">Abrir la confirmación</ap-boton>
      </div>
      <ap-cajon-lateral titulo="Revisar columna monto" [(abierto)]="cajon">
        <p>Tu veredicto sobre lo que el sistema dedujo de este campo.</p>
        <ap-boton acciones variante="primario" (pulsado)="cajon.set(false)">Guardar revisión</ap-boton>
      </ap-cajon-lateral>
      <ap-dialogo-de-confirmacion titulo="Retirar la tabla" descripcion="Se marcará aportes.pago como retirada. Las pantallas que la usan dejarán de verla." textoDeConfirmar="Retirar" fraseDeConfirmacion="RETIRAR" [destructivo]="true" [(abierto)]="confirmacion" />
    </ap-seccion-de-catalogo>
  `,
  styles: `
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); width: 100%; }
    .chips { display: flex; flex-wrap: wrap; gap: var(--s2); }
    .demo { min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: inherit; cursor: pointer; }
  `,
})
export class CatalogoBackoffice {
  readonly busqueda = signal('')
  readonly vista = signal('general')
  readonly cajon = signal(false)
  readonly confirmacion = signal(false)
  readonly filasDeMuestra: Muestra[] = Array.from({ length: 23 }, (_, i) => ({ id: `t${i}`, tabla: `tabla_${i + 1}`, esquema: i % 3 ? 'aportes' : 'grupos' }))
  readonly columnasDeMuestra: Columna<Muestra>[] = [{ clave: 'tabla', titulo: 'Tabla', ordenable: true }, { clave: 'esquema', titulo: 'Esquema' }]
  readonly muestra = crearListado<Muestra>({ filas: signal(this.filasDeMuestra), camposDeBusqueda: ['tabla', 'esquema'], filtros: [{ nombre: 'esquema', etiqueta: 'Esquema', valor: (f) => f.esquema }] })
  readonly selectores = [
    { nombre: 'estado', etiqueta: 'Estado', valor: '', opciones: [{ valor: 'ACTIVA', texto: 'Activa' }, { valor: 'RETIRADA', texto: 'Retirada' }], ayuda: 'Si la tabla sigue en uso.' },
    { nombre: 'revision', etiqueta: 'Revisión', valor: 'APROBADA', opciones: [{ valor: 'APROBADA', texto: 'Aprobada' }] },
  ]
}
