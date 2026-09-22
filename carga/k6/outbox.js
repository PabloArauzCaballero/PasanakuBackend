// H4.S1 — outbox_pending. k6 no tiene driver SQL en la imagen oficial
// (grafana/k6, sin xk6-sql — no se usa ningún build custom en este repo
// todavía, y agregar uno sin más sería un módulo nuevo no pedido). Este
// escenario genera la CARGA que llena el outbox (transferencias reales,
// cada una inserta en `nucleo_financiero.evento_dominio` dentro de la misma
// transacción — ADR de outbox); la PROFUNDIDAD del backlog se mide en
// paralelo con una consulta SQL directa, documentada en README.md, no
// dentro de k6.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, cabecerasJson, uuid } from './_comun.js';

export const options = {
    scenarios: {
        // Ráfaga corta y más intensa que transferencia.js: el objetivo acá
        // no es medir latencia HTTP, es generar suficientes filas nuevas en
        // evento_dominio para que la consulta de backlog tenga algo que
        // medir de verdad.
        outbox: { executor: 'constant-vus', vus: 15, duration: '20s' },
    },
};

export default function () {
    const cuerpo = JSON.stringify({
        cuentaOrigenId: uuid(),
        destino: { tipo: 'ALIAS', valor: 'k6-outbox' },
        monto: { monto: '1.00', moneda: 'BOB' },
        concepto: 'k6 carga de outbox (sintetico)',
    });
    const respuesta = http.post(`${BASE_URL}/api/v1/billetera/transferencias`, cuerpo, {
        headers: cabecerasJson({ 'Idempotency-Key': uuid() }),
    });
    check(respuesta, { 'responde (no cae la ruta)': (r) => r.status !== 0 });
}
