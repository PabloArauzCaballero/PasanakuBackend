# Análisis — Cuánto cobrar por cada turno (vuelta) del pasanaku

- Fecha: 2026-10-30 · Autor: sesión `pasanaku-1f` a pedido de Pablo · Estado: **recomendación, no decisión**.
- Modelo reproducible: `evidencia/modelo_comision.py` → salida literal en `evidencia/modelo_comision.txt`.
- Decide: Pablo (dueño del producto). Si se adopta, entra por CU-34 (tarifario nuevo con preaviso) o por
  corrección del seed `04-tarifario.json` antes de que exista un grupo con tarifa congelada en producción.

## 1. Recomendación en una línea

**Cobrar el 2 % de la bolsa bruta por cada turno, deducido al liquidar la entrega, impuestos incluidos,
con piso de Bs 10 y techo de Bs 300.** Mismo concepto que ya existe (`COM_ENTREGA`), misma forma de
cobro (`DEDUCCION_DE_ENTREGA`), mismo sujeto obligado (quien cobra el turno). Cambian tres números:
`valor_porcentual` 0,3 → 2,0 · `monto_maximo` 50 → 300 · tramos `regla_tarifa` eliminados o alineados.

## 2. Qué hay hoy y por qué pierde plata

Tarifario v1 sembrado (`seeders/minimos/04-tarifario.json`): `COM_ENTREGA` = 0,3 % de la bolsa bruta,
piso Bs 10, **techo Bs 50**, IVA 13 % e IT 3 % por dentro (`05-impuestos.json`), tramos 0,35 / 0,30 /
0,25 %. Costos sembrados (`14-proveedores-externos.json`, URLs `example.bo` → **supuestos del proyecto,
no tarifas contratadas**): QR 0,35 % por cobro, ACH Bs 2,50 por desembolso, WhatsApp Bs 0,28 y SMS
Bs 0,42 por mensaje, push Bs 0.

Con esos supuestos, el 0,3 % con techo de Bs 50 **da margen negativo en 4 de 5 escenarios** antes de
pagar un solo servidor (columna «margen» de `modelo_comision.txt`):

| Grupo | Bolsa | Comisión hoy | Neto de impuestos | Costo directo (push) | Margen |
|---|---|---|---|---|---|
| 6 × Bs 200 | 1 200 | 10,00 (piso) | 8,40 | 6,70 | **+1,70** |
| 6 × Bs 500 | 3 000 | 10,00 (piso) | 8,40 | 13,00 | **−4,60** |
| 10 × Bs 500 | 5 000 | 15,00 | 12,60 | 20,00 | **−7,40** |
| 12 × Bs 1 000 | 12 000 | 36,00 | 30,24 | 44,50 | **−14,26** |
| 20 × Bs 1 000 | 20 000 | 50,00 (techo) | 42,00 | 72,50 | **−30,50** |

Dos causas: (a) el QR cuesta 0,35 % de la bolsa y la comisión es 0,3 % —se cobra menos que lo que
cuesta cobrar—; (b) el techo de Bs 50 convierte el porcentaje en 0,25 % efectivo sobre bolsas grandes,
justo donde el costo proporcional es mayor. El impuesto por dentro (16 %) se lleva otro sexto.

## 3. El piso de costos: dónde está el punto de equilibrio

Solo costos directos (QR + ACH), push gratis, sin piso ni techo:

| Grupo | QR al 0,35 % (seed) | QR gratis |
|---|---|---|
| 6 × Bs 200 | 0,66 % | 0,25 % |
| 6 × Bs 500 | 0,52 % | 0,10 % |
| 10 × Bs 500 | 0,48 % | 0,06 % |
| 12 × Bs 1 000 | 0,44 % | 0,02 % |
| 20 × Bs 1 000 | 0,43 % | 0,01 % |

Fuentes públicas de 2026 indican que el QR interoperable boliviano no cobra comisión al comercio
(BCP: «pagar y/o cobrar mediante QR no tiene ninguna comisión adicional»; QR Simple «sin comisión»),
pero **no encontré una norma del BCB que lo fije**: la gratuidad normada es la de transferencias
electrónicas hasta Bs 50 000. Para una ETF que cobra a una cuenta de custodia, el costo real lo fija el
contrato con el banco. **Hasta tenerlo, el 0,35 % es el supuesto prudente.** Lo que cobre de verdad el
proveedor termina en `costo_proveedor_operacion` (skill `qr-pagos`), que es donde se medirá el margen.

Lo que el modelo **no** incluye y tiene que salir del margen: KYC por participante (proveedor
biométrico, costo no está en el repo), infraestructura (VPS Contabo compartido con Atlas, costo no
está en el repo), soporte, cumplimiento (UIF/ASFI), incobrables y adquisición. Por eso el punto de
equilibrio directo no es el precio: es el piso del que hay que alejarse.

## 4. El techo de valor: con qué compara el participante

- **El pasanaku informal.** Gratis, pero con riesgo de que la organizadora desaparezca o alguien no
  pague; el producto vende justamente la ausencia de ese riesgo (sorteo verificable, KYC, mayor
  inmutable). Hipótesis a confirmar con Pablo: en la práctica informal el organizador suele quedarse
  con la primera vuelta o cobrar una cuota; si es así, un 2 % es muy inferior a «una cuota de N».
- **Money Fellows (Egipto)**, la referencia más grande de ROSCA digital con custodia: cobra comisión
  administrativa de **hasta 16 % para los primeros turnos, bajando a 0 % en los últimos**
  ([cómo funciona](https://www.moneyfellows.com/en-us/how-it-works); coberturas antiguas citan 5–6 %
  de arranque: [MENAbytes 2019](https://www.menabytes.com/moneyfellows-1-million/),
  [CommerzVentures](https://commerzventures.com/news/moneyfellows)). Enseña dos cosas: la disposición
  a pagar por el turno temprano (es crédito) es alta, y por el turno tardío (es ahorro) es casi nula.
- **Apps de tandas en México** y gestores de ROSCA gratuitos (RoscaApp, Roscas, Jamiya): no custodian
  fondos y no cobran por entrega; no son comparables con una ETF que mueve la plata.
- **Tarjetas en Bolivia**: 3–4 % por transacción según guías comerciales (no oficiales): el número que
  la gente ya conoce como «comisión cara».

Rango razonable: por encima de ~0,7 % (costos directos pesimistas) y claramente por debajo del 3–4 %
«de tarjeta». Dentro de ese rango, el 2 % es el número redondo explicable: «dos de cada cien, una sola
vez, cuando cobrás».

## 5. Por qué 2 % y no 1 %, 1,5 % o 2,5 %

Margen por turno con push gratis (de `modelo_comision.txt`), y margen de contribución sobre el neto:

| Tarifa | 6 × 500 (bolsa 3 000) | 12 × 1 000 (bolsa 12 000) | Contribución (6 × 500) |
|---|---|---|---|
| 1,0 % | +12,20 | +56,30 | 48 % |
| 1,5 % | +24,80 | +106,70 | 66 % |
| **2,0 %** | **+37,40** | **+157,10** | **74 %** |
| 2,5 % | +50,00 | +207,50 | 79 % |

- **1 %** cubre lo directo pero no deja casi nada para KYC, soporte e infraestructura: un participante
  de 6 × 500 paga Bs 30 brutos → Bs 25 netos → Bs 12 después de costos directos, para pagar su KYC y
  su parte de servidores. En el grupo chico (6 × 200) el margen es Bs 3,38. Demasiado fino para arrancar.
- **1,5 %** es viable si el QR resulta gratis. Es el candidato para **bajar** después de medir.
- **2 %** deja ~Bs 37 por turno en el grupo típico (6 × 500) y ~Bs 150 en uno grande, con 74 % de
  contribución: el rango que una operación de pagos necesita para pagar lo fijo. Y es más fácil bajar
  de 2 a 1,5 que subir de 1 a 2 (skill `pricing`: «pick a price you can learn from»; subir exige
  preaviso y hiere confianza).
- **2,5 %** empieza a sonar a tarjeta y no aporta un salto de margen que lo justifique en el lanzamiento.

Lo que paga una persona, en concreto, al 2 %: en un grupo de 6 × Bs 500 recibe Bs 3 000 y se le
descuentan Bs 60 **una vez en todo el ciclo** (≈ Bs 10 por mes de ahorro). Ingreso de la plataforma por
ese grupo en el ciclo completo: Bs 302 netos de impuestos (vs Bs 50 hoy).

## 6. Piso y techo

- **Piso Bs 10**: se mantiene. Evita que una bolsa chica (5 × 100 = 500) deje una comisión de Bs 10 que
  no cubre ni el desembolso. Al 2 % el piso recién actúa por debajo de bolsa Bs 500.
- **Techo Bs 300** (en vez de 50): el techo es una promesa de transparencia («nunca más de…») valiosa
  para consumidor financiero, pero a Bs 50 destruye el margen. A Bs 300 cubre bolsas hasta Bs 15 000 al
  2 % y sobre Bs 20 000 baja al 1,5 % efectivo (margen +179,50): sigue muy por encima del equilibrio.
- **Tramos**: la tabla `regla_tarifa` ya existe. Si se prefiere un escalón explícito en vez de techo:
  2,0 % hasta Bs 10 000 · 1,75 % hasta 25 000 · 1,5 % en adelante, piso 10, sin techo. Es equivalente
  en espíritu; el techo único es más fácil de explicar en el tarifario público.

## 7. Qué NO recomiendo todavía (fase 2, decisión aparte)

Precio por posición del turno (más para el primero, menos o cero para el último), como Money Fellows.
Captura mucha más disposición a pagar y es exactamente lo que el mercado grande hace, pero exige: regla
de tarifa dependiente del turno (hoy `regla_tarifa` condiciona por monto, no por posición), explicación
línea por línea en la entrega (regla 91.3.4) y una decisión de equidad que es de Pablo. El 2 % plano
convive con eso: después puede ser 2 % ± un ajuste por posición.

## 8. Supuestos declarados y qué confirmar

| # | Supuesto | Procedencia | Confirmar con |
|---|---|---|---|
| 1 | QR cuesta 0,35 % por cobro | seed `14-proveedores-externos.json` (URL `example.bo`) | contrato real con el banco custodio |
| 2 | ACH Bs 2,50 por desembolso | ídem | ídem |
| 3 | 3 mensajes por cupo y período | supuesto propio | `15-eventos-y-plantillas.json` cuando se definan los canales activos |
| 4 | IVA 13 % + IT 3 % por dentro | `05-impuestos.json` | contador |
| 5 | KYC, infra, soporte: fuera del modelo | no hay cifra en el repo | Pablo (costo del VPS y del proveedor biométrico) |
| 6 | El organizador informal cobra una vuelta o cuota | hipótesis, **no verificada** | Pablo / conocimiento del mercado |
| 7 | Un porcentaje de ETF se publica con preaviso | CU-34 ya lo modela | cumplimiento (ASFI) |

## 9. Referencias externas consultadas (2026-10-30)

- Money Fellows — [How it works](https://www.moneyfellows.com/en-us/how-it-works) · [App Store](https://apps.apple.com/us/app/money-fellows-online-circles/id1113492041) · [MENAbytes](https://www.menabytes.com/moneyfellows-1-million/) · [CommerzVentures](https://commerzventures.com/news/moneyfellows)
- QR en Bolivia — [BCP: QR sin comisión](https://www.bcp.com.bo/QR) · [Shoperly: QR Simple 2026](https://www.shoperly.app/blog/cobrar-online-bolivia-qr-simple) · [Tarifario Banco Eco 02.96](https://www.baneco.com.bo/assets/content/docs/footers/tarifario/Tarifario%20Comisiones%20Productos%20y%20Servicios-0296.pdf) · [BCB: Pagos QR](https://www.bcb.gob.bo/?q=pagos_qr_bcb_bolivia) · [BCB: Informe de vigilancia 2024](https://www.bcb.gob.bo/webdocs/publicacionesbcb/2025/06/30/SISTEMA%20DE%20PAGOS%202024.pdf) · [ASFI: el uso del QR](https://www.asfi.gob.bo/sites/default/files/2025-07/El%20uso%20del%20QR%20ha%20dinamizado%20el%20sistema%20de%20pagos%20en%20Bolivia.pdf)
- Gestores de ROSCA sin custodia — [RoscaApp](https://roscaapp.com/) · [Roscas](https://roscas.io/) · [Jamiya](https://jamiya.om/en/) · [Circlys](https://play.google.com/store/apps/details?id=com.jmeyte.app&hl=en_US)
- Tandas en México — [Expansión 2018](https://expansion.mx/tecnologia/2018/01/19/yahoo-replicara-las-tandas-en-una-app-llamada-tanda) · [Condusef vía adn40](https://www.adn40.mx/mexico/que-hacer-si-no-me-dan-mi-tanda-esto-dice-condusef/) — sin cifras de comisión.
