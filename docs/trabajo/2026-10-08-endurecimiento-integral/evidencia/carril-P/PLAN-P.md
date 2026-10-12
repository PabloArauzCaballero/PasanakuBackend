# Plan del carril P — defectos de codigo que impiden que un invitado acepte (hallados por L3)

- Fecha: 2026-10-11 · Rama: justin/feature/cierre-endurecimiento-2026-10-08 · Predecesor: evidencia/carril-L3/INFORME.md
- Resultado observable: en TEST, el invitado puede consultar y aceptar (o canjear) una invitacion emitida con ENLACE (canal por omision de la app) a un grupo recien creado; un fallo local del canje no quema el enlace.
- Kill-test: crear grupo, emitir con ENLACE y consultar/aceptar con otro usuario -> hoy 500 / 422 AP-CU69-05.
- Plan maestro: el PLAN.md del trabajo (no se edita desde este carril; lo consolida el agente principal). Este archivo es el plan del carril, con las tres capas.

## Alcance
- IN: grupos (apertura, traduccion de rechazos de identidad, canje, rastro), tarifas (congelar precio al consumir `grupos.grupo_creado`), identidad (politica de token versionada: solo semilla), seeders/minimos/13, generador de semillas, despliegue/ensayos (script de siembra, NO se ejecuta), docs de CU-20/CU-69.
- OUT: `nucleo-financiero` (apertura de la cuenta del grupo y espejo contable: decision/dueno pendiente), app movil (no se toca), origin/test, origin/dev, worktree dev, VPS.
- Ambiguedades registradas: ver REPORTE de carril-P (A1 apertura sin esperar la cuenta del grupo; A2 contenido del snapshot de tarifa).

## H1 — El grupo recien creado se abre a inscripciones (CU-20)
**CA:** Dado un grupo BORRADOR, cuando tarifas congela su precio y emite `tarifas.tarifa_congelada`, entonces grupos lo pasa a ABIERTO_A_INSCRIPCION una sola vez (precondicion en el UPDATE), deja historial y evento; el duplicado, el fuera de orden y el grupo ya abierto no hacen nada.
**DoD:** pruebas de tarifas y grupos en verde (XML), `enlace/consultar` deja de dar AP-CU69-05 por estado.
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| P.H1.M1 | tarifas: `CU20CongelarTarifa` + consumidor Kafka de `grupos.grupo_creado` | snapshot con hash, UNIQUE grupo, evento `tarifas.tarifa_congelada`, duplicado no duplica | `./gradlew :servicios:tarifas:test` | TODO |
| P.H1.M2 | grupos: `AbrirInscripcion` + consumidor de `tarifas.tarifa_congelada` | BORRADOR->ABIERTO con precondicion, historial, outbox; idempotente | `./gradlew :servicios:grupos:test` | TODO |
## H2 — Canal ENLACE y traduccion del rechazo de identidad
| P.H2.M1 | grupos: el 422 de identidad al emitir llega como 4xx con su codigo | 422 AP-CU69-05, no 500 | test de `HechosPorHttp` + webTest | TODO |
| P.H2.M2 | politica_token INVITACION_GRUPO: version nueva (seeder + sql + script de siembra para el VPS) | base nueva y TEST poblada terminan con ENLACE permitido; segunda corrida no cambia nada | generar_semillas.py + ensayo con Postgres desechable 5453 | TODO |
## H3 — El canje no quema el enlace
| P.H3.M1 | verificar la postulacion local ANTES de consumir en identidad | falla local => consumirInvitacion nunca se llama; reintento idempotente | webTest + test de integracion | TODO |
## H4 — Rastro seguro
| P.H4.M1 | `enlaceDeInvitacionValido` registra clase/codigo sin datos personales | log con clase y estado HTTP | prueba unitaria | TODO |
