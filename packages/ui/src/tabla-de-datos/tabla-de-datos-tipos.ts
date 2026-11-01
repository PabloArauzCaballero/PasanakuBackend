/** `fijaDerecha`: la columna (de acciones) queda pegada al borde derecho al desplazar; `mono`: identificadores y códigos. */
export type Columna<T> = { clave: keyof T & string; titulo: string; ordenable?: boolean; numerica?: boolean; fijaDerecha?: boolean; mono?: boolean }
export type Orden = { clave: string; sentido: 'asc' | 'desc' }
export type Densidad = 'confortable' | 'compacta'

/**
 * Estado de una colección paginada, provisto por quien consume la tabla (regla: "pagina y
 * filtra quien la usa; ella solo muestra la página" — la tabla nunca hace `fetch`).
 *
 * **Contrato provisorio (regla 65 / ambigüedad Q-J1 del carril PR7).** El tipo de estado
 * "real" del repo (`ViewState`) lo publica Leo en PR8 y todavía no está en `dev`. Mientras
 * tanto este es el doble en tres niveles declarado, contra el que cierra este carril:
 *
 * - **correcto** — `{ tipo: 'lista', filas }` con una colección de filas válidas.
 * - **límite** — `{ tipo: 'lista', filas: [] }` (colección vacía), `{ tipo: 'lista', filas: [f] }`
 *   (una sola fila) y `{ tipo: 'lista', filas, obsoleta: true }` (la respuesta vigente se marcó
 *   vieja porque ya se pidió una más nueva: la tabla la sigue mostrando tal cual, nunca la
 *   reemplaza ni resucita una selección que el consumidor ya descartó).
 * - **inválido** — `{ tipo: 'error', idPeticion, motivo, mensaje }`, con `motivo` distinguiendo
 *   un error sin permiso, un recurso no encontrado y un error genérico con su propio mensaje.
 *
 * **Microtarea de adopción diferida (declarada, no resuelta acá):** cuando `ViewState<T>` esté
 * en `dev`, se agrega un adaptador `ViewState<T[]> → EstadoColeccion<T>` en el punto donde cada
 * consumidor arma este valor; la plantilla del organismo no cambia porque ya consume la unión
 * discriminada por `tipo`, no una forma concreta de `ViewState`.
 */
export type EstadoColeccion<T> =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; filas: T[]; obsoleta?: boolean }
  | { tipo: 'error'; idPeticion: string; motivo: 'sin-permiso' | 'no-encontrado' | 'desconocido'; mensaje: string }
