# Plan — Código de acceso (segundo factor) por correo, reusando `notificaciones`

- Fecha: 2026-10-16 · Repos afectados: Pasanaku (backend + esquema + despliegue) · Predecesor: `2026-10-15-alta-correo-duplicado`
- Resultado observable: una persona participante con factor `CORREO` inicia sesión en TEST y recibe en su
  bandeja de correo real el código de 6 dígitos; con ese código entra.
- Kill-test: iniciar sesión en TEST con perfil distinto de `local` y comprobar que no llega ningún correo
  (hoy no llega: ver Hallazgos).

## Hallazgos (descubrimiento, 2026-10-16)

Hechos, con ruta:

1. TEST corre con `SPRING_PROFILES_ACTIVE=local` (verificado con `docker inspect` en el VPS) → bean
   `DesafioDeDesarrollo`, código fijo `000000`. Decisión registrada en `despliegue/TEST.md:85-91`.
   **No hace falta un perfil `test` nuevo**: el pedido "primero el perfil test" ya está cubierto por el existente.
2. Fuera de `local`, `DesafioLocal` (`servicios/identidad/.../infraestructura/DesafioLocal.java:35`) genera el
   código y no lo entrega a ningún canal.
3. `notificaciones` está desplegado, pero el camino de envío no está conectado:
   - `POST /notificaciones` (`SolicitarAviso.java:42`) solo emite `notificaciones.aviso_solicitado` al outbox.
   - No hay consumidor de eventos en ningún servicio (ni `@KafkaListener` ni equivalente). Existe el
     repetidor `plataforma/comun-mensajeria/.../Relevo.java`.
   - Nada invoca a `CU80DespacharNotificacion` ni a `CU83DespacharLote`.
   - Único adaptador: `MensajeriaSimulada`. `AdaptadorMensajeria.enviar(proveedor, canal, destinatario, envioId)`
     no recibe asunto ni cuerpo.
   - Sin variables SMTP en `docker-compose.coolify.yml` (servicio `notificaciones`, línea ~301).
4. `factor_mfa.tipo` solo admite `RESPALDO, SMS, TOTP, WHATSAPP`
   (`sql/10_tablas/01_identidad_usuarios/factor_mfa.sql:16`, generado por `scripts/generar_ddl.py`).
5. `CU80` guarda el cuerpo renderizado en la bandeja (`guardarEnBandeja`) → con el código adentro violaría 90.2.9.
6. `DesafioLocal.validar` compara también los factores `TOTP` contra un número al azar: fuera de `local`, un
   operador con TOTP no puede entrar. **Fuera de alcance**, se anota (ver OUT).

## Alcance

- IN: factor `CORREO` en el modelo y el DDL generado; identidad pide el aviso a `notificaciones` por HTTP
  (`ClienteDeServicio`, patrón de `grupos/.../HechosPorHttp.java`); consumidor de `aviso_solicitado` → CU-80;
  disparador de CU-83; `AdaptadorMensajeria` con asunto/cuerpo; adaptador SMTP para `CORREO`
  (`spring-boot-starter-mail`, BOM de Boot 3.5.16); plantilla aprobada `CODIGO_ACCESO` por `CORREO`;
  variables `SPRING_MAIL_*` en el compose generado.
- OUT: SMS/WhatsApp (apagados por contrato, `docs/Contrato de implementación para IA.md:85`); validación TOTP real
  (hallazgo 6, va como trabajo propio); cambiar `PERFIL_SPRING` de TEST (lo hace Pablo cuando cargue las credenciales);
  pantallas de enrolamiento del factor en móvil y backoffice.
- Ambigüedades registradas:
  - A1. ¿Puede un **operador** usar `CORREO`? Supuesto: **no**. `FactorAdmisible` solo admite TOTP/RESPALDO
    para operadores (R-SEG-10). Confirmar con Pablo.
  - A2. ¿Hay enrolamiento por API? Hoy no existe endpoint de enrolamiento de factores: solo semillas. Supuesto:
    el factor `CORREO` de TEST se enrola por semilla/SQL del generador. Confirmar con Pablo.
  - A3. El código no puede quedar en texto plano en el outbox ni en la bandeja (90.2.9). Supuesto: el evento
    viaja con `datos.codigo` **solo** en la llamada HTTP y en el envío SMTP. Para `CODIGO_ACCESO`, la bandeja guarda
    el texto con el código enmascarado y el outbox guarda el evento sin el código; el despacho lo recibe por memoria
    en el mismo proceso. Diseño exacto en H2.S1.M1, a confirmar con Pablo antes de H2.S2.

## H1 — El factor `CORREO` existe y se emite hacia `notificaciones`
**CA:** Dado un participante con factor `CORREO` activo, cuando inicia sesión sin código, entonces identidad
responde `FACTOR_REQUERIDO` y `notificaciones` recibe un pedido `CODIGO_ACCESO` para ese usuario; el código
no aparece en ningún log ni tabla.
**DoD:** tests de identidad en verde + test de integración del cliente contra doble HTTP (correcto/límite/inválido).
**Estado:** TODO

### H1.S1 — Modelo y esquema
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Agregar `CORREO` a los tipos de `factor_mfa` en el modelo de `scripts/generar_ddl.py` y regenerar | El DDL generado incluye `'CORREO'` en `ck_factor_mfa_tipo` | `python3 scripts/generar_ddl.py && grep ck_factor_mfa_tipo sql/10_tablas/01_identidad_usuarios/factor_mfa.sql` | HECHO (evidencia/01) |
| H1.S1.M2 | Aplicar el esquema en local y verificar la deriva | Base local acepta `CORREO`, jOOQ regenerado, sin deriva | salida de aplicación + `INSERT` de prueba + verificación de deriva | HECHO (evidencia/07) |

### H1.S2 — Identidad envía el código con el enviador de correo que ya tiene (replanificado 2026-10-16)

**Cambio de plan:** en `origin/test` identidad ya tiene el puerto `CorreoDeVerificacion` con
`GmailApiCorreoDeVerificacion`. Se agregó el 2026-10-07 en el commit `2f1101f3` y se elige con
`NOTIFICATION_EMAIL_PROVIDER=gmail_api`. En TEST está **encendido y con las 4 credenciales cargadas**
(verificado con `docker inspect`, sin leer los valores). Se reusa ese enviador. Ver H2.

**Causa real medida en TEST** (3 días, `intento_autenticacion`): un participante sin factor recibe
`FACTOR_REQUERIDO` (16 veces), pero `CU04Autenticar.java:144` no emite nada (`factor.ifPresent`) y
cualquier código se rechaza (`orElse(false)`): `TOKEN_VENCIDO` 5 veces. Hay 8 de 11 usuarios sin factor.

**CA:** Dado un participante sin factor y con el correo verificado en el alta, cuando inicia sesión
desde un dispositivo nuevo, entonces recibe un correo con un código de 6 dígitos y con ese código
entra. El código no aparece en ningún log ni tabla.
**DoD:** `./gradlew :servicios:identidad:test` en verde (incluye el gate de deriva `EsquemaAlDiaRepositorioTest`).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `CorreoDeVerificacion.enviarCodigoDeAcceso` + texto propio de acceso en `PlantillaCorreoDeVerificacion` (los 2 adaptadores) | El HTML de acceso dice "iniciar sesión", no "confirmar que este correo te pertenece" | `--tests '*GmailApiCorreoDeVerificacion*' --tests '*PlantillaCorreo*'` | HECHO (evidencia/03) |
| H1.S2.M2 | `DesafioDeFactor.emitir(usuario, tipo, destino)` (por defecto, como el actual) + `DesafioLocal` envía `CORREO` | correcto: 1 envío y el código valida · límite: un segundo pedido reemplaza al primero, vencido no valida · inválido: falla del proveedor → el desafío no queda vivo y no hay 500 | `--tests '*DesafioPorCorreo*'` | HECHO (evidencia/04) |
| H1.S2.M3 | `DesafioDeDesarrollo` delega `CORREO` en `DesafioLocal` (sale el correo real) y sigue aceptando `000000` | En perfil `local`: 1 envío, valida el código real y también `000000` | `--tests '*DesafioDeDesarrollo*'` | HECHO (evidencia/05) |
| H1.S2.M4 | `CorreoDeAccesoRepositorio.correoVerificadoDe(usuario)` (se separó de `VerificacionCorreoRepositorio` por el límite de 300 líneas): token `VERIFICACION_CORREO` consumido, vinculado y con huella igual a `usuario.correo` | Vinculado → correo · sin token → vacío · correo cambiado después → vacío | test de integración contra Postgres real | HECHO (evidencia/06) |
| H1.S2.M5 | CU-04: participante sin factor y con correo verificado → `CORREO`; operador sin factor → `FACTOR_NO_ENROLADO` (sin cambios) | Login sin código → `FACTOR_REQUERIDO` + 1 envío; con código → sesión abierta | `--tests '*CU04*'` | HECHO (evidencia/06) |
| H1.S2.M6 | Regresión del módulo identidad | Suite en verde | `./gradlew :servicios:identidad:test` | HECHO (evidencia/07) |

## H2 — Camino de despacho de `notificaciones`
**Estado:** DESCARTADO (2026-10-16). Se reusa el enviador de correo que ya tiene identidad (H1.S2), en
TEST y encendido. Pasar el código por `notificaciones` además lo dejaría en texto plano en el outbox y
en la bandeja (90.2.9). Que `notificaciones` no despache nada (no hay consumidor, CU-80 y CU-83 no se
invocan, el adaptador es simulado) se anota como deuda en el REPORTE. No es parte de este trabajo.
Las microtareas H2.S1.M1 y H2.S2.M1–M5 quedan DESCARTADAS por esta razón; lo decidió el agente con la
indicación de Pablo de "reutilizar el servicio que ya está".

## H3 — TEST
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | PR a `test` y reconstruir `identidad` en el host | La imagen `aportaya/identidad:test` es del commit nuevo | `docker images` + log del despliegue | TODO |
| H3.S1.M2 | Login real en TEST con un participante con correo verificado | Llega el correo y con ese código se abre la sesión | `intento_autenticacion` EXITOSO + confirmación de Pablo | TODO |

## H4 — El alta corta en el paso del correo si ese correo ya tiene cuenta (agregado 2026-10-16, pedido de Pablo)
**Problema:** `POST /usuarios/verificaciones/correo` envía el código a un correo que ya tiene cuenta. El
rechazo (`AP-CU01-03`, `campo: correo`) recién llega en el último paso (`CU01RegistrarUsuario.java:120`).
**CA:** Dado un correo que ya tiene cuenta, cuando la persona lo escribe y pide el código, entonces el
servidor responde 422 `AP-CU01-03` con `detalle.campo = correo`, no se envía ningún correo y la app no
avanza. La app ya hace esto: `notifier_alta.dart:46` devuelve `false` y muestra el mensaje.
**DoD:** test de integración en verde + regresión de identidad.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `SolicitarVerificacionCorreo` pregunta `RegistroRepositorio.correoYaRegistrado` antes de preparar | correcto: correo libre → 202 y 1 envío · inválido: correo con cuenta → `AP-CU01-03` + `campo=correo` y 0 envíos · límite: el mismo correo con otra capitalización se comporta igual que en CU-01 (comparación exacta; se anota) | `integrationTest --tests '*CU01VerificacionCorreo*'` | HECHO (evidencia/08) |
| H4.S1.M2 | OpenAPI: `AP-CU01-03` en los códigos del 422 de la ruta | El YAML lo lista | `grep AP-CU01-03` en la ruta | HECHO (evidencia/08) |

## H5 — Publicación (pedido de Pablo)
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | PR y merge a `test` y a `dev` | Los dos merges hechos | `gh pr view` → MERGED | TODO |
| H5.S1.M2 | Redespliegue en Contabo | `aportaya/identidad:test` es del último commit y el contenedor está `healthy` | `docker images` + `docker ps` en el VPS | TODO |

## Limpieza de usuarios en TEST (pedido de Pablo) — BLOQUEADO
Backup verificado (`/opt/aportaya/respaldos/pasanaku-antes-de-limpiar-usuarios-20261016.dump`, restaurado
en una base de ensayo: 11 usuarios). La simulación por claves foráneas está hecha. Ejecutarla exige borrar
filas de `comun.bitacora_evento` (append-only) y desactivar sus triggers. El clasificador de permisos de
Claude Code lo bloqueó como alteración de auditoría. Lo destraba: Pablo autoriza explícitamente esa acción,
o decide otra estrategia. Es una decisión destructiva sobre algo compartido, por eso no se simula (regla 65).

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Quitar `local` de TEST quita también el `000000` a operadores con TOTP (hallazgo 6) | Backoffice inaccesible | No cambiar `PERFIL_SPRING` hasta arreglar la validación TOTP |
| `DesafioLocal` guarda los desafíos en memoria | Con más de una réplica, el código no valida | TEST tiene una sola réplica; se anota como deuda |
| Credenciales SMTP | H3.S1.M2 depende de Pablo | Todo lo anterior se cierra contra Mailpit (regla 65) |
