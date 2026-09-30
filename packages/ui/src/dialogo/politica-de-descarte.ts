/**
 * Política de descarte única del organismo `Dialogo` (H3.S2, `entregables/contrato-dialogo.md`).
 * Un solo lugar para que las tres rutas de cierre —botón, `Escape`, fondo— pregunten lo mismo
 * cuando hay un borrador sucio: `[puedeDescartar]` de `ap-dialogo` espera exactamente esta forma,
 * `() => boolean`, evaluada en el momento del intento de cierre (no antes: el borrador puede
 * ensuciarse después de que el consumidor arma la función).
 *
 * Q-L3 (ambigüedad registrada, PR8 §5): ante la duda, se protege. Ningún consumidor decide "este
 * modal no necesita confirmación" salvo que `sucio` nunca devuelva `true` — eso es una decisión
 * de datos (el formulario en cuestión), no una excepción a la política.
 */
export function confirmarDescarteSiSucio(sucio: () => boolean, mensaje = '¿Descartar los cambios sin guardar?'): () => boolean {
  return () => (sucio() ? confirm(mensaje) : true)
}
