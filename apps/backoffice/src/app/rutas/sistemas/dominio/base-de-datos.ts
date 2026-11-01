import { httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { EsquemaDelCatalogo, ResumenDelCatalogo, SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import type { Tono } from '@aportaya/ui/tono/tono'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * Base de datos · lo que la base ES de verdad, sin inventar migraciones. El esquema no se versiona con migraciones: se
 * genera desde el modelo (`sql/aplicar.sql`, «Generado por scripts/generar_ddl.py») y lo aplica
 * `despliegue/aplicar-esquema.sh`. Por eso esta vista no lista migraciones: lista cada esquema con su servicio dueño
 * y sus cifras (`obtenerResumenDelCatalogo`), y al lado si ese servicio alcanza la base ahora (`consultarSaludDeServicios`:
 * el readiness mira la base y Kafka, `SaludDeLaBase.java`).
 */
export type FilaDeEsquema = {
  id: string
  esquema: string
  servicio: string
  queEs: string
  tablas: number
  columnas: number
  relaciones: number
  appendOnly: number
  conDatosPersonales: number
  conexion: string
  tono: Tono
}

const CONEXION: Record<string, { conexion: string; tono: Tono }> = {
  ARRIBA: { conexion: 'conectado', tono: 'ok' },
  DEGRADADO: { conexion: 'con problemas (base o Kafka)', tono: 'aviso' },
  CAIDO: { conexion: 'el servicio no responde', tono: 'error' },
}
const SIN_DUENO = { conexion: 'compartido', tono: 'neutro' as Tono }
const SIN_DATO = { conexion: 'sin dato', tono: 'neutro' as Tono }

/** `nucleo_financiero` (esquema) y `nucleo-financiero` (servicio) son el mismo dueño. */
const clave = (nombre: string) => nombre.replaceAll('_', '-')

/** Un esquema por fila, con la conexión de su dueño. Sin salud (falló o sin permiso) la columna dice «sin dato», nunca «conectado». */
export function filasDeEsquemas(resumen: ResumenDelCatalogo, salud: SalidaSaludDeServicios | undefined): FilaDeEsquema[] {
  const porServicio = new Map((salud?.servicios ?? []).map((s) => [clave(s.servicio), s.estado as string]))
  return resumen.esquemas.map((e: EsquemaDelCatalogo) => {
    const estado = e.servicio ? porServicio.get(clave(e.servicio)) : undefined
    const conexion = !e.servicio ? SIN_DUENO : estado ? (CONEXION[estado] ?? SIN_DATO) : SIN_DATO
    return {
      id: e.nombre,
      esquema: e.nombre,
      servicio: e.servicio ?? '—',
      queEs: e.descripcion,
      tablas: e.tablas,
      columnas: e.columnas,
      relaciones: e.relaciones,
      appendOnly: e.appendOnly,
      conDatosPersonales: e.conDatosPersonales,
      ...conexion,
    }
  })
}

export function recursoResumenDeLaBase() {
  const gateway = inject(GATEWAY)
  return httpResource<ResumenDelCatalogo>(() => `${gateway}/indicadores/catalogo-de-datos/resumen`)
}
