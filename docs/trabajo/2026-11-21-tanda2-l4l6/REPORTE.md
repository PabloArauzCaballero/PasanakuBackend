# Reporte — Tanda 2, carril L4L6: identidad, cumplimiento y frontends compartidos

> **AVANCE: 1 / 4 — 25,0 %** de las microtareas detalladas del plan (T2.M1–T2.M4). El resto de F1/F2 de L4 y L6 sigue en TODO.

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 50cf3be9
- Ramas y PR (apilados, se mergean en orden):
  1. `correcciones/tanda2-l4l6-f1s3m1-token-vigente` — F1.S3.M1 (resto)
- Peldaño por área: identidad **TESTED** (unitarias, web y barrido verdes; mutante detectado). No VERIFIED: no se levantó el servicio con dos procesos reales.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M1 (F1.S3.M1, resto) | La vida del token de acceso es `15 min − 60 s` (tolerancia por omisión de `JwtTimestampValidator`, verificada en el jar 6.5.11). Un servidor de recurso con el validador por omisión rechaza el token al minuto 15 + 1 s. Con eso, sumado a lo de #412 (suspender revoca sesiones, el refresco se niega con `PuedeAutenticarse`), una cuenta suspendida o dada de baja pierde todo acceso en ≤ 15 min. | `./gradlew :servicios:identidad:test --tests '*VidaDelAcceso*'` | PASS 2/2; mutante (15 min) → 2 fallos. [`evidencia/t2-m1-token-vigente.txt`](./evidencia/t2-m1-token-vigente.txt) |

## A medias

### F1.S3.M1 — revocación inmediata (no la de ≤ 15 min)
- Qué anda: el tope de 15 min (T2.M1) y todo lo de #412.
- Qué no anda: dentro de esos 14 min, el token de una cuenta suspendida sigue sirviendo en los otros servicios, también para operaciones de dinero.
- Qué falta exactamente: F1.L4.M15 — en `plataforma/comun-web` (fuera de mi alcance), validar `iss`/`aud` y, en las operaciones de dinero, consultar si `sesion_id` sigue vigente (introspección contra identidad o lista de revocación por evento `identidad.sesion_revocada`). Identidad ya emite `sesion_id` en el token.
- Dónde quedó: sin cambios para esto.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| T2.M2–T2.M4 (F1.S4.M1 `CampoMonto`) | TODO | Siguiente PR |
| Resto de F1.L4, F1.L6, F2.L4, F2.L6 | TODO | Se toman en el orden del PLAN |

## Evidencia
- [`evidencia/t2-m1-token-vigente.txt`](./evidencia/t2-m1-token-vigente.txt)
- Verificadores de bóveda: `verificar_boveda`, `verificar_seguridad`, `verificar_carriles`, `verificar_maqueta`, `verificar_criterios` → `TODO OK` / sin divergencias.

## No cubierto
- No se probó con dos servicios reales (identidad emisor + otro validando por JWKS); la prueba decodifica con `NimbusJwtDecoder` y la clave pública, que es el mismo código.
- Las pruebas de integración con Testcontainers (`BaseDePrueba`) no corren acá: **NO COMPROBADO — requiere Docker (máquina de Pablo)**. Comando: `./gradlew :servicios:identidad:integrationTest`.

## Desvíos del plan
- Maven Central respondió 429 en este contenedor. Se usó un init script **local, no versionado**, que apunta al espejo oficial de Google de Maven Central. El build del repo no se tocó.

## Riesgos residuales
- Los clientes renuevan el acceso un minuto antes que antes (14 min). La app renueva por `expiraEn` de la respuesta, no por un 15 fijo (`apps/movil/.../cu04_autenticar.dart` solo lo mencionaba en un comentario, actualizado).

## Decisiones y ambigüedades
| # | Tema | Supuesto tomado | A quién confirmar |
|---|---|---|---|
| AMB-T2-1 | «Un token vigente deja de servir en ≤ 15 min» | Vida + tolerancia del decodificador ≤ 15 min; la revocación inmediata es F1.L4.M15 | Pablo |
