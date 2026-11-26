# Plan — GU-17 · el alta pide la fecha de nacimiento real + cierre de H6.S1.M2

- Fecha: 2026-11-26 · Repos afectados: PasanakuBackend · Predecesor: [2026-11-26-gu-c-cierre](../2026-11-26-gu-c-cierre/PLAN.md)
- Decisión de Pablo (2026-11-26, vía sesión principal): GU-17 → **pedir la fecha real en el alta**; el formulario del backoffice la exige; ningún valor inventado ni columna con default ficticio. Enfriamiento tras reinscribir factor: PT24H **confirmado**.
- Resultado observable: dar de alta un operador sin fecha de nacimiento real se rechaza; con ella, `usuario.fecha_nacimiento` guarda exactamente esa fecha. La invitación tiene capturas de errores por campo y del QR TOTP.
- Kill-test: dar de alta con el contrato viejo (sin `fechaNacimiento`) y ver que no responde 201.

## Alcance
- IN: contrato `EntradaAltaUsuarioInterno` (campo obligatorio, aditivo en la práctica: único consumidor es el backoffice, en este mismo PR), `GestionarUsuariosInternos`/repositorio, modal de alta, ejemplos simulados, criterios CU-08, capturas de invitación (errores por campo, QR TOTP).
- OUT: edad mínima (no hay regla de negocio fuente; no se inventa), migración de operadores existentes con fecha ficticia (ver ambigüedad A8).
- Ambigüedades: A8 — los operadores ya dados de alta antes de este cambio conservan `1990-01-01` ficticio; corregirlos exige que cada persona declare su fecha (¿quién la carga? ¿RR.HH.?). A9 — edad mínima del operador: sin fuente; hoy solo se rechaza fecha futura. Confirmar con Pablo/RR.HH.

## H1 — Alta con fecha real
**CA:** Dado el alta de un operador, cuando falta la fecha de nacimiento o es futura, entonces se rechaza con AP-CU08-13 y no se crea nada; cuando es válida, entonces queda guardada tal cual.
**DoD:** tests dirigidos + gates locales.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato: `fechaNacimiento` (date) obligatoria | el generador procesa el YAML | `./gradlew :servicios:identidad:generarServidorOpenApi --rerun-tasks` | HECHO |
| H1.S1.M2 | Backend: valida y guarda la fecha real (sin literal 1990) | futura/ausente → AP-CU08-13; válida → se guarda | `./gradlew :servicios:identidad:test --tests '*AltaConFechaReal*'` | HECHO |
| H1.S1.M3 | Criterios CU-08 + código AP-CU08-13 | `verificar_criterios` y `verificar_boveda` en 0 | scripts | HECHO |
| H1.S2.M1 | Modal de alta: campo «Fecha de nacimiento» obligatorio con error por campo | spec del modal pasa; el envío se bloquea sin fecha | `ng test` del modal | HECHO |
| H1.S2.M2 | Ejemplos simulados y llamadas existentes al día | typecheck y lint del backoffice en 0 | `yarn typecheck` · `yarn lint` | HECHO |
| H2.S1.M1 | Capturas de la invitación: errores por campo, QR TOTP y carga | miradas en 3 viewports × 2 temas | Playwright + `evidencia/` | HECHO |
