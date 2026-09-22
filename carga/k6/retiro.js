// H4.S1 — CU-11, POST /api/v1/billetera/retiros (ruta sensible, H3.S1).
// "Proveedor doble" del encargo: el proveedor de desembolso NO se llama
// dentro de esta corrida (invariante 6 — se instruye FUERA de la
// transaccion, y esta corrida no tiene un proveedor real detras). Lo que
// mide es el costo de retener el saldo, que es la parte sincrona.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, cabecerasJson, uuid } from './_comun.js';

export const options = {
    scenarios: {
        retiro: { executor: 'constant-vus', vus: 5, duration: '30s' },
    },
    thresholds: {
        http_req_duration: ['p(95)<600'],
    },
};

export default function () {
    const cuerpo = JSON.stringify({
        cuentaBilleteraId: uuid(),
        monto: { monto: '20.00', moneda: 'BOB' },
        instrumentoDestinoId: uuid(),
        factorMfa: '123456',
    });
    const respuesta = http.post(`${BASE_URL}/api/v1/billetera/retiros`, cuerpo, {
        headers: cabecerasJson({ 'Idempotency-Key': uuid() }),
    });
    check(respuesta, {
        'responde (no cae la ruta)': (r) => r.status !== 0,
        'no es 5xx': (r) => r.status < 500,
    });
}
