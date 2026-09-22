// H4.S1 — CU-04, POST /api/v1/sesiones (ruta sensible, H3.S1: rate-limited).
// Contrato real: servicios/identidad/src/main/resources/openapi/identidad.yaml
// -> EntradaAutenticacion. Con datos sintéticos (regla 97.6): el teléfono no
// corresponde a ninguna cuenta real, así que el 422 AP-CU04-01
// (CREDENCIAL_INVALIDA) es la respuesta ESPERADA de este escenario — mide el
// costo de la ruta hasta el rechazo, que es donde más tráfico entra.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, cabecerasJson } from './_comun.js';

export const options = {
    scenarios: {
        login: { executor: 'constant-vus', vus: 10, duration: '30s' },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    const cuerpo = JSON.stringify({
        telefonoE164: '+59170000000',
        credencial: 'credencial-sintetica-k6',
        huellaDispositivo: 'k6-carga-' + __VU,
        plataforma: 'WEB',
    });
    const respuesta = http.post(`${BASE_URL}/api/v1/sesiones`, cuerpo, { headers: cabecerasJson() });
    check(respuesta, {
        'responde (no cae la ruta)': (r) => r.status !== 0,
        'no es 5xx': (r) => r.status < 500,
    });
}
