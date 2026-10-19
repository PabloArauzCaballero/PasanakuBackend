import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { BarraDeFiltros } from '../barra-de-filtros/barra-de-filtros'
import { BotonCopiar } from '../boton-copiar/boton-copiar'
import { CabeceraDePagina } from '../cabecera-de-pagina/cabecera-de-pagina'
import { CabeceraDeSeccion } from '../cabecera-de-seccion/cabecera-de-seccion'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Esqueleto } from '../esqueleto/esqueleto'
import { EstadoDeError } from '../estado-de-error/estado-de-error'
import { ListaDeOpciones } from '../lista-de-opciones/lista-de-opciones'
import { Migas } from '../migas/migas'
import { NotaDeVista } from '../nota-de-vista/nota-de-vista'
import { Paginacion } from '../paginacion/paginacion'
import { TarjetaKPI } from '../tarjeta-kpi/tarjeta-kpi'
import { Tooltip } from '../tooltip/tooltip'
import { SeccionDeCatalogo } from './seccion'

/**
 * La anatomía de una pantalla del backoffice (la del portal interno de Atlas, con las piezas de
 * la casa): migas, cabecera de página, nota de la vista, cifras, barra de filtros, estados y
 * paginación completa. Es una muestra con datos fijos; no llama a nada.
 */
@Component({
  selector: 'ap-catalogo-backoffice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, BotonCopiar, CabeceraDePagina, CabeceraDeSeccion, ChipEstado, Esqueleto, EstadoDeError, ListaDeOpciones, Migas, NotaDeVista, Paginacion, TarjetaKPI, Tooltip, SeccionDeCatalogo],
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
  readonly selectores = [
    { nombre: 'estado', etiqueta: 'Estado', valor: '', opciones: [{ valor: 'ACTIVA', texto: 'Activa' }, { valor: 'RETIRADA', texto: 'Retirada' }], ayuda: 'Si la tabla sigue en uso.' },
    { nombre: 'revision', etiqueta: 'Revisión', valor: 'APROBADA', opciones: [{ valor: 'APROBADA', texto: 'Aprobada' }] },
  ]
}
