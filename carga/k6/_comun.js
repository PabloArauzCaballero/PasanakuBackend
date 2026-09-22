// Helpers compartidos por los seis escenarios (H4.S1). BASE_URL apunta al
// gateway detrás de NGINX por defecto — la misma entrada pública que usa
// cualquier cliente real, no una URL directa a un servicio.
import crypto from 'k6/crypto';

export const BASE_URL = __ENV.APORTAYA_BASE_URL || 'http://localhost';

export function uuid() {
    // RFC 4122 v4, con k6/crypto.randomBytes: aleatoriedad criptográfica de
    // verdad, no la del objeto matemático global que la regla 90 prohíbe
    // (scripts/verificar_seguridad.py, prohibición 6). k6 no expone un
    // generador de UUID como el navegador, pero sí bytes aleatorios reales
    // (verificado contra grafana.com/docs/k6/latest/javascript-api/k6-crypto/
    // el 2026-09-22); el formateo RFC 4122 de abajo es texto, no criptografía.
    const b = crypto.randomBytes(16);
    b[6] = (b[6] & 0x0f) | 0x40; // versión 4
    b[8] = (b[8] & 0x3f) | 0x80; // variante RFC 4122
    const hex = Array.from(b, (byte) => byte.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function cabecerasJson(extra) {
    return Object.assign({ 'Content-Type': 'application/json' }, extra || {});
}
