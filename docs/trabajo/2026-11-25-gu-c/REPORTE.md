> **AVANCE: 4 / 23 — 17,4 %.** (microtareas HECHO / total del PLAN. El resto está en A MEDIAS: código escrito, compila y pasa unit/web/barrido en local, pero las pruebas de integración solo corren en CI y todavía no hay una corrida verde.)

# Reporte — GU-C · Invitación, credencial y recuperación asistida del operador

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-c-credencial` (PR #522 a `dev`, borrador) · UI: `claude/gu-c-credencial-ui`
- Peldaño de evidencia alcanzado: **RUNS** (compila; unit/web/barrido/contrato en verde en local tras el merge con dev) (backend y contrato) con verificadores de repositorio en 0. Java solo compila y prueba en CI (Gradle local devuelve 429 de Maven Central); hasta que CI corra, no se afirma «compila» ni «tests dirigidos pasan».

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1, H1.S1.M2 | Tabla `solicitud_recuperacion_acceso` y propósitos de token en el modelo | `python3 scripts/generar_ddl.py` | 363 tablas, sin deriva · `evidencia/verificadores.txt` |
| H1.S2.M2 | CU-04/CU-09 al día | `python3 scripts/verificar_criterios.py; python3 scripts/verificar_boveda.py` | sin divergencias / TODO OK |
| H7.S1.M1 | Verificadores locales en 0 | boveda, criterios, seguridad, carriles | TODO OK |

## A medias

### H1 — Contrato y esquema
- Qué anda (verificado en local, salida en `evidencia/verificadores.txt`): `generar_ddl` y `generar_semillas` regeneran sin diff adicional; `verificar_criterios`, `verificar_boveda`, `verificar_seguridad`, `verificar_carriles`, `verificar_contratos_limites` en 0; `inventario_endpoints --check` en 0.
- Qué no anda / no verificado: `openapi-generator validate` no se pudo correr (necesita el jar de Maven Central: 429). El YAML carga con PyYAML y todas las `$ref` resuelven; la validación formal queda a CI.
- Qué falta: ver CI de #522.
- Dónde quedó: `docs/entidades/01_identidad_usuarios.puml`, `docs/Restricciones.md` (R-SEG-13), `sql/` generado, `identidad.yaml` (solo agregados, tag `Cuenta`), `scripts/modelo.py` (prefijo `/cuenta`), gateway regenerado.

### H2–H5 — Backend (invitación, clave, recuperación, bitácora)
- Qué anda: código escrito (`InvitacionesDeCuenta`, `RecuperacionesDeAcceso`, `CambiarClaveDeCuenta`, `BitacoraDeCuenta`, repositorios, `CuentaController`, trabajo `EmisionDeRecuperaciones`) y `CU09CuentaDeOperadorTest` contra base real (8 criterios + rechazos).
- Qué no anda: nada observado todavía; **no se ejecutó nada de Java**.
- Qué falta exactamente: (1) CI verde; (2) enganchar la invitación al alta (`GestionarUsuariosInternos.crear`) **después** de que #516 (lote B) entre a `dev`, para no pisar su constructor; (3) step-up (`/sesiones/desafios`) en la aprobación de recuperaciones, que depende de `EvidenciaDeStepUp` de B; (4) prueba web de los endpoints; (5) fecha de nacimiento inventada del alta (GU-17): requiere hacer opcional `usuario.fecha_nacimiento`, cambio de esquema transversal que no se hizo.

### H6 — Backoffice
- Delegado a un subagente en `claude/gu-c-credencial-ui` (worktree aparte). Estado: ver REPORTE-UI.md cuando entregue.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H7 | TODO | CI verde, merge de `dev`, capturas del lote UI |

## Evidencia
Ver `evidencia/verificadores.txt`.

## No cubierto
- Nada de Java compilado ni probado en local.
- Entrega real de correo (Gmail) y consumo del aviso por `notificaciones` (no existe el consumidor).
- Enfriamiento posterior a reinscribir el factor (ADR-038 §5).
- Prueba de caída de dependencias (regla 98.8): el correo es un doble.

## Desvíos del plan
- La invitación se emite desde un servicio propio y no desde `GestionarUsuariosInternos` (archivo de B) hasta que B esté en `dev`.

## Riesgos residuales
- `tokens` comparten `token_verificacion` con otros propósitos; la política de intentos es la de `politica_token`.
- Un correo que falla tras el commit deja la invitación emitida sin entregar (se re-emite pidiendo recuperación).

## Decisiones y ambigüedades
Ver PLAN.md: A1 plazo de enfriamiento (24 h supuesto), A2 vigencia de la invitación (72 h), A3 quién pide la recuperación, A4 enfriamiento posterior al factor, A5 aviso por correo. A confirmar con Pablo.

## Estado de CI y próximo paso (2026-11-26)
- Resuelto en CI previo: gitleaks, catálogo, spotless, compilación, barrido, specs/lint del frontend, 253/254 integración (el último fallo era el código AP-CU09) y la matriz de rutas públicas (`PoliticaDeAcceso`, cuatro rutas de `/cuenta` con su control).
- Hook de alta y step-up conectados con el contrato real de B (#516) y UI integrada en esta rama.
- Se hizo `merge origin/dev` (#525 y #520 ya estaban): conflictos resueltos (cifras 363/341/192, prefijo `/cuenta` junto a `/cuentas-servicio`, SQL y artefactos regenerados).
- Falta: CI verde sobre el head final; capturas de UI 390/834/1440 × claro/oscuro (el cliente Angular generado no existe en local); `openapi validate` formal (no corrió local).
- No cubierto: fecha de nacimiento del alta (GU-17), enfriamiento tras reinscribir factor (ADR-038 §5), envío real de correo y consumidor de notificaciones, caída de dependencias.
