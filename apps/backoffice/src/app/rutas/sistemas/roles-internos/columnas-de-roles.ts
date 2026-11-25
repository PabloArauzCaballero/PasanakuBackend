import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { RolCatalogo } from 'clientes/angular/identidad'

export const origenDe = (r: RolCatalogo) => (r.esSistema ? 'Sistema' : 'Personalizado')

/** Las columnas del catálogo de roles y lo que se exporta (datos puros, sin la pantalla). */
export const COLUMNAS: Columna<RolCatalogo>[] = [
  { clave: 'codigo', titulo: 'Código de rol', ordenable: true },
  { clave: 'nombre', titulo: 'Nombre', ordenable: true },
  { clave: 'departamento', titulo: 'Departamento', ordenable: true },
  { clave: 'descripcion', titulo: 'Descripción' },
  { clave: 'esSistema', titulo: 'Origen' },
  { clave: 'permisos', titulo: 'Permisos', numerica: true },
]

export const EXPORTABLES: ColumnaExportable<RolCatalogo>[] = [
  { clave: 'codigo', titulo: 'Código de rol' },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'ambito', titulo: 'Ámbito' },
  { clave: 'departamento', titulo: 'Departamento' },
  { clave: 'descripcion', titulo: 'Descripción' },
  { clave: 'esSistema', titulo: 'Origen', valor: origenDe },
  { clave: 'permisos', titulo: 'Permisos', valor: (r) => r.permisos.join(' | ') },
]
