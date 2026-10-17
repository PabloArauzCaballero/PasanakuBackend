# Reporte — Organizador Aprendiz automático

> **AVANCE: 4 / 4 — 100 %.**

- Fecha: 2026-10-17 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/organizador-basico-auto-20261017`
- Peldaño: TESTED (integración contra PostgreSQL real + web). Sin VERIFIED hasta recorrerlo en TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Aceptar/firmar contrato Aprendiz en organizador | `:servicios:organizador:integrationTest --tests *CU90HabilitacionBasicaTest --tests *CU91Test` | 8/8 + 9/9 PASS |
| H1.S1.M2 | Token FIRMA_CONTRATO + rol ORGANIZADOR en identidad | `:servicios:identidad:integrationTest --tests *CU90RolOrganizadorTest` | 5/5 PASS |
| H1.S1.M3 | Matriz de autorización negativa | `:servicios:organizador:webTest`, `:servicios:identidad:webTest` | 41/41, 124/124 PASS |
| H1.S1.M4 | Política FIRMA_CONTRATO | `python scripts/generar_semillas.py` | 1514 filas validadas |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Texto legal del contrato | BLOQUEADO (DECISION_REQUIRED) | Negocio/cumplimiento entrega el texto definitivo |
| Recorrido en TEST con la app | TODO | Despliegue de esta rama en TEST + APK |

## No cubierto
- Llamada HTTP real identidad→organizador (cubierta con doble); se verifica en TEST.
- Integración completa de identidad (solo la clase nueva) por memoria de la máquina.

## Decisiones
- Firma = clic de aceptación con sesión autenticada (token `ENLACE`, canal `APP`, estado `CONSUMIDO`), sin OTP. Confirmar si cumplimiento exige OTP.
- Otorgante del rol ORGANIZADOR = quien aprobó el KYC (`ck_asignacion_no_autoasignada`).
