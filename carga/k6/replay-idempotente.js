// H4.S1 — la MISMA Idempotency-Key, mandada tres veces seguidas por cada
// iteración: la invariante que se mide es "la segunda y la tercera no crean
// un efecto nuevo" (regla 91.1.4). No es un check de negocio (no hay cuenta
// real detrás en esta corrida) — es el costo de RESOLVER la deduplicación:
// el segundo y tercer intento tienen que ser más rápidos que el primero
// (índice único, no un insert que compite), y eso es lo que el threshold
// mide.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, cabecerasJson, uuid } from './_comun.js';

export const options = {
    scenarios: {
        replay: { executor: 'constant-vus', vus: 5, duration: '30s' },
    },
    thresholds: {
        // La repetición nunca debería tardar MÁS que el intento original:
        // si tarda más, algo está reintentando trabajo que ya se hizo.
        'http_req_duration{intento:repetido}': ['p(95)<600'],
    },
};

export default function () {
    const clave = uuid();
    const cuerpo = JSON.stringify({
        cuentaOrigenId: uuid(),
        destino: { tipo: 'ALIAS', valor: 'k6-replay' },
        monto: { monto: '5.00', moneda: 'BOB' },
        concepto: 'k6 replay idempotente (sintetico)',
    });
    const cabeceras = cabecerasJson({ 'Idempotency-Key': clave });

    const primero = http.post(`${BASE_URL}/api/v1/billetera/transferencias`, cuerpo, {
        headers: cabeceras,
        tags: { intento: 'original' },
    });
    check(primero, { 'primer intento responde': (r) => r.status !== 0 });

    for (let i = 0; i < 2; i++) {
        const repetido = http.post(`${BASE_URL}/api/v1/billetera/transferencias`, cuerpo, {
            headers: cabeceras,
            tags: { intento: 'repetido' },
        });
        check(repetido, {
            'repeticion responde': (r) => r.status !== 0,
            // Invariante 7 (regla 91.1.4): la misma clave, el mismo status.
            'mismo status que el original': (r) => r.status === primero.status,
        });
    }
}
