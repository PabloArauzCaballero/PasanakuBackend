# ADR (carril D) · Inversiones voluntarias: servicio nuevo `inversiones`

Estado: PROPUESTO por el carril D, 2026-10-08. Alcance: H10 y H11.M3-M5 del plan integral.
Decide el carril; la validación de negocio queda en DECISION_REQUIRED (ver al final).

## Contexto

Hay que ofrecer DPF y cuotas de fondo de un aliado (banco / SAFI) como inversión voluntaria
del titular, con saldo retenido, posición, devengo, valoración, rescate, intereses y comisión de
éxito. NOTAS.md "Brechas" confirma que nada de esto existe en el código productivo.
PLAN §8.1 pide un ADR "después de buscar equivalentes".

Búsqueda de equivalentes (hecha antes de decidir, regla 98.7 y 97.4.6):

| Candidato | Qué tiene | Por qué no alcanza |
| --- | --- | --- |
| `nucleo-financiero` | billetera, retenciones, libro contable | Es dueño del dinero del usuario, no de contratos con un aliado. Meter producto/posición/devengo ahí mezcla un ciclo de vida de meses (DPF) con el de un saldo y agranda el servicio N1 del que depende todo el cobro. Además es del carril A. |
| `tarifas` | tarifario, cotización, devengo de comisión de la plataforma (CU30-36) | Se reutiliza para cobrar cargos de la plataforma (F19). La comisión de éxito del aliado/plataforma sobre cuotas NO es una tarifa por operación: depende de marca máxima por posición. |
| `garantia` (`fondo_garantia`, `movimiento_fondo`) | fondo mutual del grupo | Es la cobertura del pozo; el PLAN §4.1 exige separar posiciones voluntarias de la cobertura. Reutilizarlo mezclaría patrimonios. |
| `erp` | contabilidad de gestión | Consume hechos; no crea posiciones. |
| Catálogos (`catalogo.*`) | tarifario, impuesto, límites | `impuesto` existe y lo escribe `tarifas`; no hay tasas de DPF ni valor de cuota. No hay producto de inversión en ningún módulo (grep de `inversi`/`DPF` en `docs/entidades`: sin resultados de dominio). |

## Decisión

Se crea el servicio **`inversiones`** (esquema `inversiones`, rol `svc_inversiones`, módulo 15 del modelo,
prefijo de ruta `/inversiones`). Justificación contra 98.7, de forma explícita:

1. **Ciclo de vida de datos propio y dueño claro (bounded context real).** Producto, versión de condiciones,
   consentimiento, orden, posición, devengo, valoración, rescate y comisión viven meses o años y se
   corrigen con contra-movimiento; ninguno existe hoy en otro servicio.
2. **Perfil de riesgo distinto que conviene aislar.** Toca dinero del titular y depende de un aliado externo
   con respuesta perdida, pendiente y rechazo. Aislarlo evita que una caída o un bug del aliado arrastre
   a `nucleo-financiero` (N1). Nivel de criticidad propuesto: N3 (se difiere sin consecuencia externa;
   el libro no depende de él).
3. No se crea "para que no crezca el otro": se desplegaría y cambiaría a otro ritmo (cadencia del aliado,
   cálculo diario de devengo) y el ahorro de acoplamiento es verificable (ArchUnit prohíbe imports cruzados).

## Fronteras

- **Dueño de**: `producto_inversion`, `version_condiciones`, `consentimiento_inversion`, `orden_inversion`,
  `posicion_inversion`, `movimiento_posicion` (append-only), `valor_cuota` (append-only),
  `devengo_dpf` (append-only), `rescate_inversion`, `distribucion_interes`, `comision_exito`,
  `comprobante_inversion` (append-only). No hay claves foráneas a otros esquemas: los IDs de billetera,
  retención y transacción son UUID opacos que viven en el contrato (98.1).
- **Consume `nucleo-financiero` SOLO por contrato** (puerto `LibroDelTitular`, adaptador HTTP con timeout,
  circuit breaker declarado y "falla cerrado": ante duda NO se ejecuta el efecto). No edita
  `servicios/nucleo-financiero` (es del carril A).
- **Consume al aliado por el puerto `AliadoDeInversion`**: adaptador HTTP firmado contra
  `herramientas/aliado_simulado` (simulado). Existe un solo adaptador y es simulado; el arranque en
  perfil de producción **falla** si se configura (`GuardiaDeProduccion`-estilo propio).
- Sin transacción distribuida: saga con intención persistida antes del efecto, referencia estable con el aliado,
  consulta de estados ambiguos y compensaciones como operaciones nuevas (98.3, PLAN §8.4).

## Saga de suscripción (el orden importa, 98.3.5)

1. `crearOrden` (TX local): valida consentimiento versionado, producto, mínimo, pozo, idempotencia (UNIQUE) → orden `CREADA`.
2. Retener en el libro (fuera de TX, por contrato; clave de idempotencia derivada de la orden) → `RETENIDA` + `retencion_id`.
3. Enviar al aliado con la referencia estable de la orden → `ENVIADA`; si la respuesta se pierde queda `INCIERTA`
   y SE CONSULTA por referencia; no se libera ni se reenvía a ciegas.
4. Aliado confirma → TX local: posición `ABIERTA` + movimiento `CONSTITUCION` + orden `CONFIRMADA`; luego se
   consolida el débito en el libro (ejecutar retención como débito, idempotente). El disponible no vuelve a subir.
   Aliado rechaza → liberar retención, orden `RECHAZADA`.
5. Un barrido reintenta pasos pendientes: cada uno es idempotente, por eso el orden de dolor es "retenido sin
   posición" (inmovilizado, visible, reintentable) y nunca "posición sin débito".

## Qué queda fuera y por qué (honestidad)

- **El débito/crédito real del libro** (ejecutar la retención como movimiento hacia custodia de inversión;
  acreditar rescate/vencimiento/interés) NO existe en el contrato publicado de `nucleo-financiero`:
  `cerrarRetencion(EJECUTADA)` cierra la retención pero el saldo se deriva de `movimiento_billetera` menos
  retenciones VIGENTES, por lo que sin un movimiento el importe volvería a estar disponible. Se documenta
  el contrato requerido (`contrato-requerido-nucleo.md`) para el carril A. Mientras no exista el productor,
  el adaptador HTTP no tiene prueba de contrato del lado productor y el servicio opera solo contra el doble.
- **Inversión de dinero de grupos: desactivada** (PLAN §4.3). Un saldo afectado a un pozo se rechaza.
- Parámetros comerciales y fiscales: todos SINTÉTICOS, `apto_produccion = false` por CHECK en la base.

## Consecuencias

- Costo: un servicio más (build, descriptor, compose, ruta de gateway, k8s generado). Se acepta porque el
  riesgo de mezclar es mayor que el costo de operar un servicio N3.
- Cambios en archivos compartidos mínimos y declarados en el reporte (modelo, Restricciones, gateway, compose).

## DECISION_REQUIRED (dueño)

| ID | Decisión | Dueño |
| --- | --- | --- |
| DR-INV-01 | Qué aliado (banco/SAFI), con qué facultades y quién contrata con el cliente | Dirección + legal |
| DR-INV-02 | Tasas, plazos, base de días, mínimos y costos reales de cada producto | Aliado + producto |
| DR-INV-03 | Tratamiento fiscal de intereses y de la comisión (alícuotas, retención, rescate anticipado) | Fiscal |
| DR-INV-04 | Existencia, base contractual y fórmula de comisión de éxito (marca máxima, cristalización) | Legal + fiscal + aliado |
| DR-INV-05 | Contrato en `nucleo-financiero` para débito de inversión y crédito de rescate | Carril A (núcleo) |
| DR-INV-06 | Titularidad y custodia de las posiciones | Legal + proveedor |
