# Plan — Rediseño visual del Caso de cumplimiento (CU-44) y de Parámetros financieros (CU-132)

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (`apps/backoffice`, `packages/ui` solo para parametrizar `GrupoRadio`) · Predecesor: `2026-11-23-continuar-traspaso`
- Resultado observable: el oficial de cumplimiento ve en el caso un resumen (estado, paso en curso, plazo, causal) y trabaja la causal sin recorrer la escalera entera en móvil; en escritorio la escalera es un riel lateral. En parámetros, cada parámetro muestra de un vistazo qué rige hoy, qué espera aprobación (cuatro ojos) y su historial.
- Kill-test: abrir `visual-caso-movil-claro-inicial.png`; si la escalera de 6 pasos sigue ocupando la pantalla antes de la causal, no está hecho.

## Alcance
- IN: `rutas/cumplimiento/casos/{pantalla-de-caso,formulario-de-caso}.ts` (+ componente de resumen nuevo en la misma carpeta), `rutas/configuracion/parametros/*`, sus specs, `e2e/caso-de-cumplimiento.e2e.ts`, `e2e/visual-parametros-financieros.e2e.ts`, PNG `evidencia/visual-caso-*`, `packages/ui/src/grupo-radio` (input `variante` aditivo, default igual al actual).
- OUT: contratos, dominio (`cu44-caso.ts`, `cu132-parametros.ts`), autorización, textos de negocio verificados por tests, `packages/simulado`, móvil.
- Ambigüedades: el contrato de CU-44 no publica alerta de origen ni titular, y `VersionDeParametro` no publica quién propuso/aprobó ni cuándo. Supuesto: no se inventan; la pantalla lo declara. Confirmar con Pablo si se agregan al contrato.

## H1 — Pantallas rediseñadas con evidencia visual
**CA:** Dado un operador con permiso, cuando abre cada pantalla en 390/834/1440 px y en ambos temas, entonces la jerarquía (resumen → acción; vigente → pendiente → historial) se lee sin desborde y los E2E siguen en verde.
**DoD:** typecheck, lint, tests unitarios y a11y dirigidos, los dos E2E visuales y la regresión (`accesibilidad.e2e.ts`, matriz de roles/menú) en verde; capturas antes/después miradas.
**Estado:** HECHO

### H1.S1 — Caso de cumplimiento
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Capturas ANTES | existen en `evidencia/backoffice-antes/` | `ls` | HECHO |
| H1.S1.M2 | `GrupoRadio` variante `tarjetas` | radiogroup accesible, default sin cambios | `ng test` grupo-radio/caso | HECHO |
| H1.S1.M3 | Resumen del caso + riel lateral / escalera plegable | móvil: causal visible en el primer pliegue | E2E caso 18/18 + captura | HECHO |

### H1.S2 — Parámetros financieros
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Jerarquía vigente / pendiente (cuatro ojos) / historial | se lee de un vistazo en 3 viewports | E2E parámetros 10/10 + spec | HECHO |

### H1.S3 — Regresión y cierre
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Regresión y gates | todo verde | typecheck, lint, accesibilidad.e2e, roles/menú | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambiar `GrupoRadio` compartido (39 usos) | romper otras pantallas | input aditivo con default actual; correr suite completa |
| Límite de 200 líneas | gate rojo | partir en componentes de presentación |
