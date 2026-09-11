import { httpResource, HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina, PaginaServidor } from '../../../nucleo/tabla/tipos'

/**
 * CU-52 · Atender un reclamo en plazo (D-18: el Punto de Reclamo tiene puerta en la
 * app; el backoffice tenía la bandeja, esto es su lectura).
 *
 * **Supuesto declarado**: `servicios/cumplimiento/openapi/cumplimiento.yaml` reserva el
 * prefijo `/reclamos` (línea 5 del archivo) pero todavía no publica la operación de
 * bandeja (`GET /reclamos`) ni la de respuesta (`POST /reclamos/{id}/respuesta`). Los
 * campos de abajo son los que declara `SalidaCU52` en el propio CU-52, más `categoria`,
 * `canalIngreso` y `responsableId` que el flujo principal exige guardar. Se pide al
 * carril de `cumplimiento` (backend) escribir la operación; hasta entonces esta pantalla
 * llama al gateway con esta forma y el contrato real puede diferir en el nombre de ruta.
 */
export type EstadoReclamo = 'INGRESADO' | 'EN_ANALISIS' | 'RESPONDIDO' | 'CERRADO' | 'ELEVADO'

export type ReclamoDeBandeja = {
  reclamoId: string
  codigo: string
  categoria: 'COMISION' | 'OPERACION_NO_RECONOCIDA' | 'SALDO' | 'SERVICIO' | 'DATOS_PERSONALES' | 'GRUPO'
  canalIngreso: 'APP' | 'WEB' | 'TELEFONO' | 'PRESENCIAL' | 'CORREO'
  montoReclamado?: { monto: string; moneda: string }
  fechaIngreso: string
  plazoRespuesta: string
  diasHabilesPlazo: number
  estado: EstadoReclamo
  responsableId: string | null
  responsableNombre: string | null
}

/** La bandeja completa: la ordena por vencimiento quien la consume (regla del gate). */
export function bandejaDeReclamos() {
  const gateway = inject(GATEWAY)
  return httpResource<ReclamoDeBandeja[]>(() => `${gateway}/reclamos`)
}

/** Un reclamo vencido sin respuesta es el que manda en la cola (CU-52, 5c). */
export const vencido = (r: ReclamoDeBandeja, ahoraIso: string): boolean =>
  new Date(r.plazoRespuesta).getTime() < new Date(ahoraIso).getTime() && r.estado !== 'CERRADO' && r.estado !== 'RESPONDIDO'

/** Ordena por plazo de vencimiento ascendente: el que vence antes va primero. */
export function ordenadosPorVencimiento(reclamos: readonly ReclamoDeBandeja[]): ReclamoDeBandeja[] {
  return [...reclamos].sort((a, b) => new Date(a.plazoRespuesta).getTime() - new Date(b.plazoRespuesta).getTime())
}

/**
 * El `cargador` que pide `TablaDeDatosVirtualizada` (`nucleo/tabla/`, del shell): la
 * tabla es la única pieza de grilla del backoffice, no se duplica.
 *
 * **Supuesto adicional declarado**: como `GET /reclamos` (ver arriba) todavía no pagina
 * ni ordena del lado del servidor, esta función pide la lista completa una vez y
 * pagina/ordena/filtra en el adaptador — **no** dentro de la tabla, que sigue sin saber
 * de HTTP ni de memoria. Cuando el backend publique paginación real, este adaptador es
 * el único lugar que cambia.
 */
export function cargarReclamos(http: HttpClient, gateway: string): CargadorDePagina<ReclamoDeBandeja> {
  return async (pedido) => {
    const todos = await firstValueFrom(http.get<ReclamoDeBandeja[]>(`${gateway}/reclamos`))
    const filtrados = pedido.filtros['estado'] ? todos.filter((r) => r.estado === pedido.filtros['estado']) : todos
    const ordenados = pedido.orden
      ? [...filtrados].sort((a, b) => {
          const clave = pedido.orden!.clave as keyof ReclamoDeBandeja
          const [va, vb] = [String(a[clave] ?? ''), String(b[clave] ?? '')]
          return pedido.orden!.sentido === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
        })
      : ordenadosPorVencimiento(filtrados)
    const inicio = (pedido.pagina - 1) * pedido.tamano
    const pagina: PaginaServidor<ReclamoDeBandeja> = { filas: ordenados.slice(inicio, inicio + pedido.tamano), total: ordenados.length }
    return pagina
  }
}

/**
 * Fábrica en contexto de inyección: la pantalla la llama en un inicializador de campo
 * (contexto de inyección válido) y así **no importa `HttpClient` en `rutas/`** — el
 * checker `sin red en vista` de `scripts/verificar_frontend.py` lo exige fuera de
 * `dominio/` y `nucleo/`.
 */
export function cargadorDeReclamos(): CargadorDePagina<ReclamoDeBandeja> {
  return cargarReclamos(inject(HttpClient), inject(GATEWAY))
}
