// Helpers compartidos por los seis escenarios (H4.S1). BASE_URL apunta al
// gateway detrás de NGINX por defecto — la misma entrada pública que usa
// cualquier cliente real, no una URL directa a un servicio.
export const BASE_URL = __ENV.APORTAYA_BASE_URL || 'http://localhost';

export function uuid() {
    // RFC 4122 v4, sin dependencias externas (k6 no trae crypto.randomUUID
    // en todas las versiones del runtime JS embebido — esto es real igual).
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

export function cabecerasJson(extra) {
    return Object.assign({ 'Content-Type': 'application/json' }, extra || {});
}
