/**
 * CU-52 · Atender un reclamo en plazo (skill `debido-proceso`, `reclamos-consumidor`).
 *
 * El plazo que se muestra es el GUARDADO al registrar el reclamo
 * (`reclamo_cliente.fecha_limite_respuesta`), con su fecha de inicio visible — nunca
 * uno recalculado en el cliente. Por eso el dominio no expone ninguna función que
 * "calcule" el plazo: solo lee lo que vino del servidor.
 *
 * **Contrato pendiente:** `cumplimiento.yaml` declara los códigos de error de CU-52 y
 * CU-53 (AP-CU52-01..04, AP-CU53-01..03) pero no una ruta HTTP para reclamos — el
 * prefijo `/reclamos` está reservado en el encabezado del contrato pero sin
 * operaciones. Se declara como hueco.
 */
export type ReclamoDelConsumidor = {
  id: string
  registradoEn: string
  plazoVenceEn: string
  norma: string
  respuesta: string | null
}

export const claveDeBorrador = (reclamoId: string): string => `cumplimiento:reclamo:${reclamoId}:respuesta`
