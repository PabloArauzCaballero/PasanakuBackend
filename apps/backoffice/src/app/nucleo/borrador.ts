import { Injectable } from '@angular/core'

const BASE_DE_DATOS = 'aportaya-backoffice-borradores'
const ALMACEN = 'borradores'

/**
 * Borrador local de formularios largos (ROS, actas) en `IndexedDB`, **nunca** en
 * `localStorage`: un dato personal en claro ahí lo lee cualquier extensión del
 * navegador. Se guarda por clave de formulario y se borra al enviar con éxito.
 *
 * Supuesto declarado: el cifrado en reposo que pide la ficha (F6) se resuelve con el
 * cifrado de disco del sistema operativo del operador más el candado del dispositivo,
 * como en el resto del backoffice — no hay clave de cifrado de aplicación en el cliente
 * porque no hay dónde guardarla sin exponerla igual. Si esto no alcanza para algún
 * formulario, es una decisión de ese carril (F8), no del shell.
 */
@Injectable({ providedIn: 'root' })
export class ServicioBorrador {
  private base(): Promise<IDBDatabase> {
    return new Promise((resolver, rechazar) => {
      const pedido = indexedDB.open(BASE_DE_DATOS, 1)
      pedido.onupgradeneeded = () => pedido.result.createObjectStore(ALMACEN)
      pedido.onsuccess = () => resolver(pedido.result)
      pedido.onerror = () => rechazar(pedido.error)
    })
  }

  async guardar(clave: string, valor: unknown): Promise<void> {
    const bd = await this.base()
    await new Promise<void>((resolver, rechazar) => {
      const tx = bd.transaction(ALMACEN, 'readwrite')
      tx.objectStore(ALMACEN).put(valor, clave)
      tx.oncomplete = () => resolver()
      tx.onerror = () => rechazar(tx.error)
    })
  }

  async leer<T>(clave: string): Promise<T | undefined> {
    const bd = await this.base()
    return new Promise((resolver, rechazar) => {
      const pedido = bd.transaction(ALMACEN, 'readonly').objectStore(ALMACEN).get(clave)
      pedido.onsuccess = () => resolver(pedido.result as T | undefined)
      pedido.onerror = () => rechazar(pedido.error)
    })
  }

  async borrar(clave: string): Promise<void> {
    const bd = await this.base()
    await new Promise<void>((resolver, rechazar) => {
      const tx = bd.transaction(ALMACEN, 'readwrite')
      tx.objectStore(ALMACEN).delete(clave)
      tx.oncomplete = () => resolver()
      tx.onerror = () => rechazar(tx.error)
    })
  }
}
