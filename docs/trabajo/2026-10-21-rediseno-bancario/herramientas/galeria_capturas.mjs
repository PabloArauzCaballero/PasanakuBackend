// Genera evidencia/galeria.html: el ANTES y el DESPUÉS de cada pantalla, lado a lado,
// filtrable por flujo, estado, viewport y tema. Solo lee evidencia/; sin dependencias.
// Uso: node herramientas/galeria_capturas.mjs [--publicar <carpeta>]
//   Sin argumentos escribe evidencia/galeria.html, que apunta a los PNG sueltos.
//   Con --publicar arma en <carpeta> una copia para el Artifact: las capturas van en un
//   JSON por fase y flujo (paquetes/<fase>-<flujo>.json) que la página carga al mostrarlas.
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const EVIDENCIA = join(dirname(fileURLToPath(import.meta.url)), '..', 'evidencia')
const FASES = ['antes', 'despues']

/** registro.tsv → { "flujo/pantalla|estado|vp|tema": {excepcion, sinContrato, alSalir} } */
function leerRegistro(fase) {
  const ruta = join(EVIDENCIA, fase, 'registro.tsv')
  if (!existsSync(ruta)) return {}
  const notas = {}
  for (const linea of readFileSync(ruta, 'utf8').split('\n').filter(Boolean)) {
    const [pantalla, estado, vp, tema, excepcion, sinContrato, alSalir] = linea.split('\t')
    notas[`${pantalla}|${estado}|${vp}|${tema}`] = { excepcion, sinContrato, alSalir }
  }
  return notas
}

const capturas = {}
for (const fase of FASES) {
  const base = join(EVIDENCIA, fase)
  if (!existsSync(base)) continue
  const notas = leerRegistro(fase)
  for (const flujo of readdirSync(base, { withFileTypes: true }).filter((d) => d.isDirectory())) {
    for (const archivo of readdirSync(join(base, flujo.name)).filter((a) => a.endsWith('.png'))) {
      const [pantalla, estado, vp, tema] = archivo.replace('.png', '').split('__')
      const clave = `${flujo.name}/${pantalla}`
      capturas[clave] ??= { flujo: flujo.name, pantalla, fotos: {} }
      const nota = notas[`${clave}|${estado}|${vp}|${tema}`]
      capturas[clave].fotos[`${fase}|${estado}|${vp}|${tema}`] = {
        src: `${fase}/${flujo.name}/${archivo}`,
        nota:
          [nota?.excepcion !== 'sin-excepcion' && nota?.excepcion, nota?.sinContrato && `Sin contrato: ${nota.sinContrato}`, nota?.alSalir && `Al salir: ${nota.alSalir}`]
            .filter(Boolean)
            .join(' · ') || '',
      }
    }
  }
}

// El orden del plan: primero la cara de la app.
const ORDEN = ['billetera', 'grupos', 'entrada', 'alta', 'perfil', 'sistema']
const rango = (f) => (ORDEN.includes(f) ? ORDEN.indexOf(f) : ORDEN.length)
const indice = Object.values(capturas).sort(
  (a, b) => rango(a.flujo) - rango(b.flujo) || a.pantalla.localeCompare(b.pantalla),
)
const plantilla = readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'galeria.plantilla.html'), 'utf8')
const iPublicar = process.argv.indexOf('--publicar')
if (iPublicar === -1) {
  writeFileSync(join(EVIDENCIA, 'galeria.html'), plantilla.replace('/*INDICE*/[]', JSON.stringify(indice)))
} else {
  const destino = process.argv[iPublicar + 1]
  mkdirSync(join(destino, 'paquetes'), { recursive: true })
  const paquetes = {}
  for (const p of indice) {
    for (const foto of Object.values(p.fotos)) {
      const nombre = `${foto.src.split('/')[0]}-${p.flujo}`
      ;(paquetes[nombre] ??= {})[foto.src] = readFileSync(join(EVIDENCIA, foto.src)).toString('base64')
    }
  }
  for (const [nombre, contenido] of Object.entries(paquetes)) {
    writeFileSync(join(destino, 'paquetes', `${nombre}.json`), JSON.stringify(contenido))
  }
  writeFileSync(
    join(destino, 'galeria.html'),
    plantilla.replace('/*INDICE*/[]', JSON.stringify(indice)).replace("/*MODO*/'archivos'", "'paquetes'"),
  )
  console.log(`${destino}: ${Object.keys(paquetes).length} paquetes`)
}
const fotos = indice.reduce((n, p) => n + Object.keys(p.fotos).length, 0)
console.log(`evidencia/galeria.html · ${indice.length} pantallas · ${fotos} capturas`)
