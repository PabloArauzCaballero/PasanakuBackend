# Plan — GU-C · cierre de lo que quedó sin cubrir

- Fecha: 2026-11-26 · Repos afectados: PasanakuBackend · Predecesor: [2026-11-25-gu-c](../2026-11-25-gu-c/PLAN.md) (#522, ya en `dev`)
- Resultado observable: tras reinscribir el factor por recuperación asistida, la cuenta entra pero no obtiene step-up durante el enfriamiento; el backoffice de `/cuenta` tiene prueba visual real; el PLAN anterior refleja el estado real.
- Kill-test: reinscribir un factor y pedir un step-up de inmediato: si responde 200, no está hecho.

## Alcance
- IN: enfriamiento post-reinscripción (ADR-038 §5), validación formal del OpenAPI, capturas UI de las tres pantallas con datos sintéticos, estado real del plan anterior.
- OUT: fecha de nacimiento del alta (GU-17), correo real, consumidor de notificaciones.
- Ambigüedades: (A6) duración del enfriamiento post-reinscripción: supuesto PT24H, igual que el de la recuperación (A1); confirmar con Pablo / cumplimiento. (A7) GU-17: `usuario.fecha_nacimiento` es NOT NULL; resolver exige decidir entre columna nullable (cambio de modelo + ADR) o pedir la fecha real en el alta. `DECISION_REQUIRED`, no se inventa.

## H1 — Cierre
**CA:** Dado un operador cuyo factor se reinscribió hace menos del enfriamiento, cuando abre un step-up, entonces recibe AP-CU04-11 y nada se emite; pasado el plazo, funciona.
**DoD:** tests dirigidos en verde + gates locales.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | OpenAPI de identidad válido | el generador lo procesa sin error | `./gradlew :servicios:identidad:generarServidorOpenApi --rerun-tasks` → BUILD SUCCESSFUL | TODO |
| H1.S2.M1 | Criterio CU-04 del enfriamiento + código AP-CU04-11 | `verificar_criterios` y `verificar_boveda` en 0 | scripts | TODO |
| H1.S2.M2 | `CU04StepUp.abrir` rechaza dentro del enfriamiento (config, sin literal) | tests del criterio: dentro rechaza, fuera pasa, sin reinscripción pasa | `./gradlew :servicios:identidad:test --tests '*StepUpTest'` | TODO |
| H1.S3.M1 | Capturas UI 390/834/1440 × claro/oscuro, animaciones off, miradas | 6 capturas por pantalla revisadas | `evidencia/` | TODO |
| H1.S4.M1 | Estado real del PLAN de #522 | cada fila con su evidencia | diff del PLAN | TODO |
