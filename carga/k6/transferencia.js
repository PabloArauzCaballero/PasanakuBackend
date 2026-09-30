// H4.S1 — CU-12, POST /api/v1/billetera/transferencias (ruta sensible,
// H3.S1). Contrato: EntradaTransferencia. Cada VU manda una Idempotency-Key
// NUEVA por iteración (a propósito: esto mide el costo de la escritura real,
// no de la deduplicación — para eso está replay-idempotente.js).
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, cabecerasJson, uuid } from './_comun.js';

export const options = {
    scenarios: {
        transferencia: { executor: 'constant-vus', vus: 5, duration: '30s' },
    },
    thresholds: {
        http_req_duration: ['p(95)<600'],
    },
};

export default function () {
    const cuerpo = JSON.stringify({
        cuentaOrigenId: uuid(),
        destino: { tipo: 'ALIAS', valor: 'k6-carga' },
        monto: { monto: '10.00', moneda: 'BOB' },
        concepto: 'k6 carga (sintetico)',
    });
    const respuesta = http.post(`${BASE_URL}/api/v1/billetera/transferencias`, cuerpo, {
        headers: cabecerasJson({ 'Idempotency-Key': uuid() }),
    });
    check(respuesta, {
        'responde (no cae la ruta)': (r) => r.status !== 0,
        'no es 5xx': (r) => r.status < 500,
    });
}
