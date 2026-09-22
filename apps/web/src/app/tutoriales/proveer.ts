import type { Provider } from '@angular/core'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { IDENTIDAD_DE_TUTORIALES, visitanteAnonimo } from '@aportaya/tutoriales/identidad'
import { PRESENTACION_DEL_CENTRO } from '@aportaya/tutoriales/centro/textos'
import { CARGADORES_DE_TUTORIALES, RUTAS_DEL_PRODUCTO } from '@aportaya/tutoriales/registro'

/**
 * Las rutas del sitio contra las que se valida que ningún tutorial señale una página
 * que no existe. Sale de `app.routes.ts`; la prueba del catálogo falla si dejan de
 * coincidir.
 */
export const RUTAS_DEL_SITIO: readonly string[] = [
  '/',
  '/como-funciona',
  '/seguridad',
  '/tarifas',
  '/transparencia',
  '/preguntas',
  '/plazos',
  '/reclamos',
  '/descargar',
  '/tutoriales',
]

/**
 * **El enchufe del motor en el sitio público.**
 *
 * Tres diferencias con el backoffice, y las tres entran por token:
 * 1. **Nadie tiene cuenta**: la identidad es [[visitanteAnonimo]], el avance se guarda
 *    por navegador y ningún tutorial pide permisos.
 * 2. **El catálogo es otro**: el del sitio, que se trae por `import()` y por lo tanto
 *    no pesa en el primer pintado de una página de contenido.
 * 3. **No hay almacén remoto**: sin cuenta no hay dónde sincronizar, y está bien así.
 */
export function proveerTutorialesDelSitio(): Provider[] {
  return [
    { provide: CARGADORES_DE_TUTORIALES, useValue: () => import('./catalogo/catalogo').then((m) => m.CATALOGO_DEL_SITIO), multi: true },
    { provide: RUTAS_DEL_PRODUCTO, useValue: RUTAS_DEL_SITIO },
    { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
    { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
    { provide: IDENTIDAD_DE_TUTORIALES, useValue: visitanteAnonimo },
    {
      provide: PRESENTACION_DEL_CENTRO,
      useValue: {
        titulo: 'Guía interactiva',
        proposito: 'Recorridos guiados sobre las páginas de verdad, sin crear ninguna cuenta. Nada de lo que hagas acá te compromete a nada.',
      },
    },
  ]
}
