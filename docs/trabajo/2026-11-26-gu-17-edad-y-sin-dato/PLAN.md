# Plan — GU-17 · edad mínima 18 (A9) y operadores «sin dato» (A8)

- Fecha: 2026-11-26 · Repos afectados: PasanakuBackend · Predecesor: [2026-11-26-gu-17-fecha-real](../2026-11-26-gu-17-fecha-real/PLAN.md) (#538, ya en `dev`)
- Decisiones de Pablo (2026-11-26, vía sesión principal): **A9** edad mínima 18 años, error de contrato con código estable, test de límite (exactamente 18 hoy sí; 17 años 364 días no). **A8** marcar «sin dato» a los operadores con `1990-01-01`: migración versionada con nombre explícito, solo donde vale exactamente esa fecha, reversible, sin tocar la base a mano; y el backoffice pide completar la real.
- Resultado observable: el alta rechaza menores de 18 con AP-CU08-14; los operadores previos con la fecha ficticia quedan `NULL` (sin dato) y el backoffice les pide la fecha real; ningún participante puede quedar sin fecha.
- Kill-test: dar de alta a alguien de 17 años y 364 días y ver que responde 201.

## Alcance
- IN: regla de 18 años (backend, contrato documentado, modal), modelo (`usuario.fecha_nacimiento` nullable + invariante en la base para participantes), corrección de datos generada y reversible, completar la fecha desde el backoffice.
- OUT: cualquier otro dato ficticio; adopción de Flyway/Liquibase.
- **Hallazgo / ambigüedad A10 (DECISION_REQUIRED, sigue abierta):** el repo NO tiene migraciones versionadas (`docs/operacion/schema-changes.md`, Q-01/AMB-7: el esquema se regenera desde el modelo y no existe tabla de control ni rollback; adoptar Flyway es un ADR pendiente). Para no inventar un framework, la «migración versionada» se implementa como corrección **generada por `generar_ddl.py`, con nombre explícito**, acotada por fecha de registro para que sea de una sola vez (un operador nacido de verdad el 1990-01-01 dado de alta después del corte no se toca) y con su reversa escrita y probada. Confirmar con Pablo si alcanza o se abre el ADR de migraciones.
- A11: el corte de la corrección es la fecha del cambio de contrato (2026-11-26); el valor es una constante del generador, no un parámetro de negocio.

## H1 — Edad mínima de 18 años (A9)
**CA:** Dado el alta de un operador, cuando nació hace menos de 18 años, entonces se rechaza con AP-CU08-14; cuando cumple exactamente 18 hoy, entonces se acepta.
**DoD:** tests dirigidos + gates locales. **Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Backend: `fechaDeNacimientoReal` exige ≥ 18 años (sin literal fuera de una constante nombrada) | 18 hoy pasa; 17 años 364 días y la futura se rechazan con AP-CU08-14 | `./gradlew :servicios:identidad:test --tests '*CU08AltaConFechaReal*'` | HECHO |
| H1.S1.M2 | CU-08: código AP-CU08-14 y criterios de borde | `verificar_criterios` y `verificar_boveda` en 0 | scripts | HECHO |
| H1.S2.M1 | Modal: error por campo para menores de 18 y mapeo de AP-CU08-14 | spec pasa; no deja crear | `ng test` de `sistemas` | HECHO |

## H2 — Operadores «sin dato» (A8)
**CA:** Dado un operador con `fecha_nacimiento = 1990-01-01` registrado antes del corte, cuando se aplica el esquema, entonces queda `NULL`; la segunda aplicación no cambia nada; la reversa la restituye.
**DoD:** aplicación real sobre PostgreSQL con salida pegada. **Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Modelo: `usuario.fecha_nacimiento` `<<NULL>>` + invariante en la base (solo el personal interno puede no tenerla) | participante sin fecha → rechazo de la base; operador sin fecha → acepta | `psql` con ambos casos | HECHO |
| H2.S1.M2 | Corrección generada con nombre explícito, acotada por corte, con reversa | primera corrida marca N filas; segunda marca 0; reversa restituye | `psql` con salida | HECHO |
| H2.S2.M1 | Backend: el detalle del operador indica que falta la fecha y se puede completar una vez (con motivo y step-up, mismas reglas que las demás ediciones) | se completa: queda guardada con bitácora; ya completa: se rechaza | `test` + `webTest` | A MEDIAS |
| H2.S2.M2 | Backoffice: aviso «Falta la fecha de nacimiento» en el detalle y acción para completarla | spec; captura | `ng test` + Playwright | A MEDIAS |
| H2.S3.M1 | Gates y deriva | `generar_ddl` sin diff, `verificar_*` OK, lint/typecheck | scripts | A MEDIAS |
