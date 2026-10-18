# Plan — Verificación de identidad diferida (backend `identidad`)

- Fecha: 2026-10-17 · Repos afectados: PasanakuBackend (rama `claude/kyc-diferido-20261017`) y la app (`alta-atlas-sesion-work/apps/movil`, plan propio en `docs/trabajo/2026-10-17-kyc-diferido/PLAN.md` de ese repo) · Predecesor: `2026-10-17-correcciones-app-v5` (app)
- Aclaración del usuario (2026-10-17): el paso de KYC sigue en el registro, con un botón «Saltar por ahora»; quien lo salta crea la cuenta y la app lo lleva a completar la verificación al intentar una transacción o entrar a un pasanaku.
- Resultado observable: una persona crea su cuenta con nombre, nacimiento, celular, correo confirmado y contraseña (sin carnet ni fotos), entra a la app, y recién cuando quiere crear o unirse a un grupo envía su carnet y fotos para la verificación.
- Kill-test: `POST /usuarios` sin `documento` → si responde 400/422, no está hecho.

## Alcance
- IN:
  - `POST /usuarios` acepta el alta sin `documento`: la cuenta nace `INCOMPLETO_KYC` (estado que ya existe en `sql/10_tablas/01_identidad_usuarios/usuario.sql:25`), sin documento ni verificación.
  - `POST /usuarios/{usuarioId}/verificacion` (nuevo; mismo patrón de acceso que `actualizarFechaExpiracionDocumento`: sesión propia y titularidad) recibe el `Documento`, lo guarda, abre la verificación BASICO y pasa la cuenta a `PENDIENTE_VERIFICACION`.
  - `GET /usuarios/yo/perfil` agrega `estadoDeCuenta`, para que la app sepa si falta verificar o si está en revisión.
  - El ingreso trata `INCOMPLETO_KYC` igual que `PENDIENTE_VERIFICACION` (entra sin factor, sin permisos de dinero).
- OUT: cambiar quién aprueba (sigue manual en el backoffice); cambiar la apertura de billetera (`AperturaDeBilleteras`); proveedor de KYC automático; notificaciones al aprobar.
- Ambigüedades registradas:
  - El pedido dice «crear o unirse a un grupo». Supuesto: se exige la verificación para toda operación con dinero (grupos y billetera), porque el servidor ya niega esas operaciones sin aprobación; la app lo explica en el momento. Confirmar con el dueño del producto.
  - Qué datos quedan en el alta rápida. Supuesto: nombres, apellidos y fecha de nacimiento (columnas NOT NULL en `usuario.sql`; cambiar el esquema queda fuera). Confirmar con Producto/Cumplimiento.

## H1 — El servidor permite el alta sin carnet y la verificación después
**CA:** Dado un alta sin documento, cuando se crea, entonces la cuenta queda `INCOMPLETO_KYC`, puede ingresar, y al enviar el documento con su sesión pasa a `PENDIENTE_VERIFICACION` con su verificación abierta.
**DoD:** `gradlew :servicios:identidad:webTest` en verde; test de integración del caso; CI del PR en verde; desplegado en TEST y ejercitado contra la API real.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.M1 | Contrato: `documento` opcional en `EntradaRegistro`; `INCOMPLETO_KYC` en `SalidaRegistro.estado`; `POST /usuarios/{id}/verificacion`; `estadoDeCuenta` en `MiPerfil` | El generador compila | `gradlew :servicios:identidad:compileJava` → BUILD SUCCESSFUL | HECHO |
| H1.M2 | CU-01 sin documento crea `INCOMPLETO_KYC` sin documento ni verificación; con documento, igual que hoy | Test de integración de ambos caminos | `gradlew :servicios:identidad:integrationTest --tests '*CU01*'` (CI si no hay Docker) | TODO |
| H1.M3 | Caso de uso `IniciarVerificacion`: valida vigencia y duplicado, guarda documento, abre verificación, transición con precondición en el UPDATE | 1.ª vez pasa a PENDIENTE; 2.ª vez 422; documento ajeno duplicado 422 | test de integración `CU02IniciarVerificacionTest` | TODO |
| H1.M4 | Controlador: sesión + titularidad; matriz negativa (sin token 401, otro usuario 403) | webTest de autorización | `gradlew :servicios:identidad:webTest` → 0 fallos | HECHO |
| H1.M5 | Ingreso: `INCOMPLETO_KYC` entra como pendiente | test de CU04 | integración CU04 | TODO |
| H1.M6 | `estadoDeCuenta` en `GET /usuarios/yo/perfil` | webTest + integración | igual que M4 | HECHO |
| H1.M7 | Postman regenerado, PR, CI verde, fusión en `test`, despliegue TEST | Endpoint responde en TEST | `curl` contra TEST → 202 con `INCOMPLETO_KYC` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Algún servicio deja operar a un `INCOMPLETO_KYC` | Persona sin verificar con dinero | Probar en TEST crear grupo y recargar con una cuenta sin verificar; debe negarse |
| Docker local no disponible para integración | Sin evidencia local | Correr en CI y pegar el resultado |
