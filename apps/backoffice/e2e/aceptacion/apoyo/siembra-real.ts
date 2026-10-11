import { execFileSync } from 'node:child_process'
import path from 'node:path'
import type { Mundo } from './escenario'

/**
 * Siembra del modo REAL: personas sintéticas con credenciales generadas en el momento (nunca en el repositorio) contra el stack
 * de `despliegue/ensayos/arranque_completo.sh` (identidad, grupos, notificaciones, garantia, transparencia, aportes + gateway).
 *
 * Este módulo es el único punto donde la suite sabe cómo se crean los usuarios en ese stack; el resto de la suite no cambia entre
 * modos. La siembra la hace `despliegue/ensayos/integracion/sembrar_aceptacion.py` (hash Argon2id con la pimienta del entorno,
 * fuera del repo): imprime el `Mundo` como JSON en stdout, y cada llamada crea personas y un grupo NUEVOS.
 *
 * Qué se siembra por SQL y por qué: las personas con su rol y la estructura del grupo, porque ningún servicio las crea hoy
 * (`crearGrupo` exige un organizador habilitado en el servicio `organizador`, tarifario y licencia). Invitar, canjear, proponer
 * y resolver son HTTP real. Sin correos personales: todo es @ejemplo.invalid y teléfonos sintéticos.
 */
export async function sembrarReal(): Promise<Mundo> {
  const raiz = path.resolve(__dirname, '../../../../..')
  const script = path.join(raiz, 'despliegue/ensayos/integracion/sembrar_aceptacion.py')
  const python = process.env['PYTHON'] ?? 'python'
  let salida: string
  try {
    salida = execFileSync(python, [script], {
      encoding: 'utf8',
      env: { ...process.env, PYTHONUTF8: '1' },
      cwd: raiz,
      timeout: 120_000,
    })
  } catch (error) {
    const detalle = error instanceof Error ? error.message : String(error)
    throw new Error(
      `La siembra del modo real falló: ${detalle.slice(0, 400)}. ¿Está arriba el stack (despliegue/ensayos/integracion/correr.sh aceptacion)?`,
    )
  }
  const mundo = JSON.parse(salida) as Mundo
  for (const clave of ['organizador', 'participante', 'participante2', 'operador', 'operador2', 'sinPermiso'] as const) {
    if (!mundo[clave]?.telefono || !mundo[clave]?.clave) throw new Error(`La siembra real no devolvió la cuenta «${clave}»`)
  }
  if (!mundo.grupoId) throw new Error('La siembra real no devolvió el grupo')
  return mundo
}
