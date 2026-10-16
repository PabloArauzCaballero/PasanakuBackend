# Reporte — Código de acceso por correo, reusando `notificaciones`

> **AVANCE: 0 / 13 — 0 %.** Solo descubrimiento y plan; no se escribió código. TEST ya permite entrar con `000000` (perfil `local`, verificado en el VPS).

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama(s): ninguna todavía. La rama actual `pablo/fix/alta-correo-duplicado-2026-10-15` tiene cambios ajenos sin commit: este trabajo arranca en una rama nueva.
- Peldaño de evidencia alcanzado: `DISCOVERED`

## Completado
ninguna

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M1 – H2.S2.M5 (11 microtareas) | TODO | Arrancar ejecución; H2.S2 espera la confirmación de A3 (H2.S1.M1) |
| H3.S1.M1 | TODO | — |
| H3.S1.M2 | TODO | Pablo: credenciales SMTP en Coolify y cambio de `PERFIL_SPRING` (después de arreglar TOTP, ver riesgos) |

## Evidencia
```text
$ ssh root@<VPS TEST> 'docker inspect … identidad … | grep SPRING_PROFILES; docker logs … | grep -c "SEGUNDO FACTOR DE DESARROLLO"'
contenedor=identidad-n3wymuuo076w9prwo5i293bx-20261016T065500
SPRING_PROFILES_ACTIVE=local
1
… b.a.i.i.DesafioDeDesarrollo : desafio b1f5ec6f-… de desarrollo emitido para el usuario <enmascarado>
```
(El id de usuario se enmascaró.)

## No cubierto
- No se probó un inicio de sesión real con `000000` en TEST durante esta sesión.

## Desvíos del plan
- La persona pidió primero un perfil `test` con código fijo. No se creó: TEST ya corre con `local`, que hace exactamente eso (`despliegue/TEST.md:85-91`).

## Riesgos residuales
- `DesafioLocal` valida los factores TOTP contra un código al azar: fuera de `local`, ningún operador con TOTP puede entrar. Está fuera de alcance; requiere un trabajo propio antes de cambiar `PERFIL_SPRING`.
- El camino de `notificaciones` (consumidor → CU-80 → CU-83 → adaptador) no está conectado para ningún aviso, no solo para este.

## Decisiones y ambigüedades
- A1 (operador con `CORREO`: no), A2 (enrolamiento por semilla) y A3 (el código no se persiste): ver PLAN.md. Confirmar con Pablo.
