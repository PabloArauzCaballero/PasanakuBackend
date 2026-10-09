# E-H1-04 · Registro de decisiones comerciales abiertas y modo simulado

Fecha: 2026-10-08. Fuentes: PLAN.md §1.2, §5.1, §12; `investigacion/2026-10-07-modelo-financiero/INFORME.md` (secciones «Decisiones pendientes para concretar la implementación» y las citadas por fila); E-H1-02 y E-H1-03 de este directorio; `seeders/minimos/*` (solo lectura).

**Regla de lectura.** Todas las decisiones están **ABIERTAS**. La columna «Valor para simulación» es un supuesto reversible, **SINTÉTICO**, que sirve para construir y probar; no es una decisión del dueño ni habilita nada con dinero real. «Dueño» es el responsable **propuesto** por el PLAN §12; ninguna persona concreta fue designada ni consultada. Ninguna de estas filas afirma que exista autorización, contrato o aprobación.

| ID | Decisión | Dueño propuesto (PLAN §12) | Valor para simulación (SINTÉTICO) | Capacidad / operación real bloqueada hasta cerrar | Fuente |
| --- | --- | --- | --- | --- | --- |
| D-01 | Alcance de autorización propia y aliados (banco, SAFI, proveedor de pagos) | Dirección + asesoría boliviana | Proveedores ficticios identificados (`herramientas/proveedor_simulado/`) | Todas las operaciones con fondos de terceros (F02, F14–F18, F20–F21) | PLAN §12 f.1; INFORME «Autorizaciones y alianzas» |
| D-02 | Naturaleza y exigibilidad del respaldo del pozo (garantía contractual, seguro, afianzamiento, capital propio) | Dirección + legal + finanzas | La empresa cubre todo faltante del escenario | Activación de grupos garantizados; promesa de «pozo completo» (F05, F14) | PLAN §12 f.2; ADR-F-01 |
| D-03 | Capital, caja y límites agregados de cobertura | Finanzas + órgano de gobierno | Reserva sintética declarada. El INFORME **no** fija porcentaje ni capital mínimo | Nuevos compromisos de cobertura; abrir grupos reales | PLAN §12 f.3; INFORME «Cómo calcular el respaldo necesario» |
| D-04 | Comisión de servicio: cargo separado (A) o deducción con bruto/neto (B); impuestos y facturación | Producto + finanzas + fiscal | Tabla sintética versionada (ilustrativo INFORME: 1 % sobre Bs 72.000 por ciclo) | Cobros reales (F19, F14) | PLAN §12 f.4; ADR-F-02 |
| D-05 | Cesión de turno: qué se cede, aportes pendientes, comprador permitido | Producto + legal | Solo el derecho de cobro; el vendedor conserva sus aportes pendientes | Mercado real de turnos (F10–F13) | PLAN §12 f.5; INFORME «Venta y compra de turnos» |
| D-06 | Titularidad y custodia de saldos, pozos y reserva | Legal + proveedor | Cuentas ficticias separadas | Recibir, mantener o invertir fondos (F02, F15, F16) | PLAN §12 f.6; INFORME «Cinco patrimonios…» |
| D-07 | Plazos, tasas, cuotas y rescates de DPF/fondo | Banco / SAFI + producto | Supuestos con fecha y etiqueta demo (ilustrativo INFORME: 4 % nominal, base 365; **no** es tasa de un banco) | Productos reales de inversión (F16, F17, F20) | PLAN §12 f.7; INFORME «Cuánto rinde realmente el dinero temporal» |
| D-08 | Comisión sobre ganancias («regalía») | Legal + fiscal + aliado | Fórmula ilustrativa, **desactivada** en real | Ese cargo (F21) | PLAN §12 f.8; INFORME «Lo que llamaste regalía» |
| D-09 | Inversión del dinero de los grupos | Dirección + legal + riesgo + participantes según estructura | Desactivada | Uso de esos fondos | PLAN §12 f.9; INFORME «Alternativa B» (recomienda no invertir los pozos al inicio) |
| D-10 | Retención de evidencia y derechos de datos | Legal + seguridad | Política de pruebas sin PII real | Conservación productiva | PLAN §12 f.10; INFORME «Decisión humana y auditoría» (retención 10 años del RD BCB 140/2025 no es regla universal) |
| D-11 | SLA, RPO/RTO y guardias | Operaciones + proveedor | Objetivos de ensayo, no promesas | Lanzamiento con compromiso contractual | PLAN §12 f.11; PLAN H13 (no inventar SLA contractuales) |
| D-12 | ¿Existen grupos sin organizador humano («organizador digital»)? | Producto | No (comportamiento del árbol de trabajo) | F05 para ese tipo de grupo | ADR-F-03 |
| D-13 | Umbrales de gobierno: doble control, pagos manuales extraordinarios, cambios de límites, liberación excepcional | Gobierno + tesorería | Sin umbral; el PLAN §5.1 dice que son «decisiones de gobierno pendientes, no constantes inventadas» | Pagos manuales y excepciones con dinero real | PLAN §5.1 |
| D-14 | Umbrales económicos del algoritmo de admisión | Producto + riesgo | Reglas explicables con umbrales **sintéticos**; el INFORME desaconseja «30 % de ingresos» o «score 700» como si fueran requisitos bolivianos | F08 con efecto sobre personas reales | INFORME «Primera versión propuesta» |
| D-15 | ¿Coexisten el fondo mutual (CU-23) y el respaldo empresarial? | Producto + finanzas | Coexisten, nombrados aparte | F14 | ADR-F-01 |
| D-16 | Límite monetario por operación de billetera/QR | Legal + cumplimiento | **Ninguno fijo en código**; el INFORME recuerda que el artículo 22 del RD BCB 140/2025 (cuatro salarios mínimos) exige confirmar instrumento, salario vigente y circulares | F02, F15, F18 con dinero real | INFORME «Pagos y QR» |

## Parámetros que hoy están en el repositorio sin marca de procedencia o sin aprobación

Hallazgos de solo lectura sobre `seeders/minimos/` (HEAD y árbol de trabajo); **no se corrigieron** (regla 97.4 y 97.5.4: no se infieren parámetros financieros; los seeds no son mi propiedad de archivos).

| Archivo | Qué contiene | Marca actual | Problema |
| --- | --- | --- | --- |
| `seeders/minimos/04-tarifario.json` (línea 2) | «tarifario v1: 0,3 % de la bolsa con piso Bs 10 y techo Bs 50, a cargo del beneficiario del turno, **deducido de la entrega**» | Ninguno (`entorno: minimo`) | Parámetro comercial sin fuente ni etiqueta de sintético, y modelo B (deducción) que contradice la regla 7 si se comunica «pozo completo» (ver ADR-F-02, D-04) |
| `seeders/minimos/05-impuestos.json` | IVA `alicuota: 0.13`, `INCLUIDO_EN_PRECIO` | `estado: "confirmar con el área tributaria"` | Marcado pendiente; falta fuente y fecha |
| `seeders/minimos/03-limites-operativos.json` | Límites en USD «derivados de fuentes secundarias» | `estado: PROVISIONAL` + advertencia de que legal debe verificarlos | Marcado como provisional; relacionado con D-16 |

## Qué falta para cumplir el criterio «marcado sintético»
H1.S1.M4 pide que un parámetro no aprobado usado en demo esté **marcado sintético y no habilite producción**. En lo inspeccionado **no existe un campo uniforme** (`origen: SINTETICO`, fuente, fecha) en los seeds: cada archivo usa su propio texto libre (`estado`, `advertencia`, `nota`). Por eso esta microtarea queda **A MEDIAS**: el registro existe (este archivo), pero la marca en los artefactos de datos no está verificada ni uniforme, y el bloqueo de producción por capacidad (flag del servidor) no se inspeccionó.

## No cubierto
- Ninguna decisión fue elevada a una persona; este registro no tiene aprobaciones.
- No se revisaron los seeds de `seeders/dev/`, ni los valores por defecto de `application*.yml`, ni el frontend.
- No se verificó que algún servicio rechace operaciones reales cuando la capacidad esté sin habilitar.
