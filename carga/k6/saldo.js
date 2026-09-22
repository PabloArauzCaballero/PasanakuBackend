// H4.S1 — CU (consultarSaldo), GET /api/v1/billetera/{cuentaId}/saldo. La
// única lectura de los cinco escenarios: el saldo se DERIVA del libro en
// cada consulta (comentario del propio contrato), así que es la que más
// aprieta a Postgres por segundo si el patrón de acceso es "consultar antes
// de cada operación", que es el real.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, uuid } from './_comun.js';

export const options = {
    scenarios: {
        saldo: { executor: 'constant-vus', vus: 20, duration: '30s' },
    },
    thresholds: {
        http_req_duration: ['p(95)<300', 'p(99)<800'],
    },
};

export default function () {
    // Cuenta sintética: no existe -> 404/401 real, mide el costo del camino
    // hasta ese rechazo (índice, JOIN de RLS), no el de un acierto de caché
    // inventado.
    const respuesta = http.get(`${BASE_URL}/api/v1/billetera/${uuid()}/saldo`);
    check(respuesta, {
        'responde (no cae la ruta)': (r) => r.status !== 0,
        'no es 5xx': (r) => r.status < 500,
    });
}
