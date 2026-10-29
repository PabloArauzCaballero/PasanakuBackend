# Stock — flujos `perfil` + `sistema` (rediseño con skills de marketing)

> **AVANCE: 0 / 8 pantallas.** (se actualiza al cerrar)

## Plan (regla 20)

- Resultado observable: las 8 pantallas de `perfil` y `sistema` dicen qué hacen, con una
  acción principal con verbo concreto, sin prometer lo que el código no hace.
- Kill-test: alguna captura `despues-marketing` muestra jerga («KYC», «expediente»), un error
  que es un botón con una oración adentro, o un texto que promete algo no implementado.
- IN: archivos listados en el encargo + `seccion_verificacion_kyc.dart` (cabecera del perfil,
  solo la usa `pantalla_perfil.dart`).
- OUT: `identidad/textos.dart`, envoltorio `Scaffold`/`appBar`, `packages/diseno_flutter`,
  proveedores y dominio (comportamiento).

| ID | Microtarea | DoD |
|---|---|---|
| M1 | Perfil + tarjeta de identidad: sin «KYC», error accionable, textos a archivo propio | analyze + tests de perfil + captura |
| M2 | Completar verificación: copy | analyze + `pantalla_completar_kyc_errores_test` + captura |
| M3 | Dispositivos: pregunta clara y CTA con el destino real | analyze + captura |
| M4 | Verificación profunda: `GrupoRadio` del sistema en lugar de `SegmentedButton` crudo, CTA honesto | analyze + captura |
| M5 | Contraseña: CTA con verbo + objeto, reglas más legibles | analyze + captura |
| M6 | Baja: consecuencia como título, CTA nombra la acción, salida intacta | `pantalla_baja_test` + captura |
| M7 | Notificaciones: vacío honesto | analyze + captura |
| M8 | Ayuda: revisión; solo se cambia lo medible | captura |
