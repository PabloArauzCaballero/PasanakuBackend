# Reporte — Bandeja de postulaciones a organizador (CU-90)

> **AVANCE: 5 / 6 — 83,3 %.**

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/postulaciones-organizador-20261029` (desde `origin/test` 310124ed)
- Peldaño de evidencia alcanzado: frontend `TESTED` contra HTTP simulado (sin integración con `organizador` ni ejercicio en TEST).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Dominio `cu90-postulaciones.ts`: bandeja por cursor, expediente, resolver y revisar con clave por contenido | `ng test --include=rutas/postulaciones/** --include=nucleo/matriz-de-roles.spec.ts` | PASS 53/53 — `evidencia/vitest-dirigido.txt` |
| H1.S1.M2 | Bandeja: estado legible, «Espera tu decisión», filtro en la URL → `estados`, cursor, vacío y error | ídem | PASS (5 casos) |
| H1.S1.M3 | Expediente + formulario según `acciones`: aprobar con cuerpo exacto y clave; revocar por `/revision`; motivo vacío o de 1001 no se manda; AP-CU90-04 en español conservando lo escrito; AP-CU90-08 recarga; 503 reenvía con la MISMA clave | ídem | PASS (7 casos) |
| H1.S1.M6 | Traducciones `AP-CU90-03/04/05/07/08` | ídem | PASS |
| H1.S1.M5 | Typecheck backoffice | `yarn typecheck` | EXIT 0, 0 errores |

## A medias
### H1.S1.M4 — Ruta, permiso de sección y menú
- Qué anda: `/postulaciones-organizador` con `canMatch` `ver:postulaciones` → `ADMIN_PLATAFORMA`, entrada en Operación y en el tablero; la matriz de roles prueba que SOPORTE no entra y ADMIN_PLATAFORMA sí (PASS).
- Qué no anda: `menu-y-guardias.spec.ts` falla en los casos «todos · PLATAFORMA» y «todos · oficial» (vencen a los 5 s con rebotes en cadena).
- Qué falta exactamente: nada de este cambio. **El árbol limpio de `origin/test` (310124ed) falla los mismos 2 casos** (85/87, 57 s; `evidencia/menu-y-guardias-arbol-limpio.txt`); a la mañana, sobre c6f9b28f, pasaba 87/87 en 10 s. Lo introdujo algo posterior (probablemente #243, pantallas de Sistemas). Hay que diagnosticarlo aparte.
- Dónde quedó: rama `claude/postulaciones-organizador-20261029`, compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Ejercicio en TEST | TODO | Desplegar y que exista una postulación PENDIENTE (TEST 2026-10-29: 0 en `organizador.solicitud_organizador`); crearla requiere un participante que postule desde la app |

## Evidencia
```text
yarn typecheck → EXIT 0 (0 errores)
postulaciones + matriz de roles: Test Files 3 passed (3) · Tests 53 passed (53)
menu-y-guardias con el cambio:  Tests 2 failed | 138 passed (140)  (corrida conjunta)
menu-y-guardias árbol limpio:   Tests 2 failed | 85 passed (87) · 57.11s
```

## No cubierto
- Integración real con `organizador` (todo contra HTTP simulado, declarado en cada spec).
- Prueba visual en tres viewports y dos temas.
- Accesibilidad automática (no se agregó spec a11y para estas pantallas).
- `medidos` del contrato (opcional): no se envía.

## Regresión introducida y corregida (2026-10-29)
- Tras mergear #249, la regresión completa del backoffice en `test` (ea6dd5ed) dio **1 failed | 1284 passed**: `nucleo/menu.spec.ts` exige máximo 8 pestañas por sección y Operación quedó con 9 al sumar «Postulaciones a organizador». Fue un defecto de este trabajo (no se corrió la regresión completa antes del PR).
- Corrección: «Admisiones a grupos» y «Postulaciones a organizador» pasan a un subgrupo «Admisiones» dentro de Operación (mismo patrón que «Riesgo y seguridad»). Regresión completa: **182 archivos, 1285/1285 en verde** (`evidencia/regresion-con-arreglo.txt`).

## Desvíos del plan
- Los tipos se importan de `clientes/angular/organizador/model/models` y no del índice raíz: con el cliente regenerado en el worktree, el índice raíz no exponía los modelos nuevos (`PaginaDeBandeja`, `SalidaHabilitacion`) aunque `model/models.ts` sí. Hay precedente de importar por `model/` (`rutas/publicidad/campanas/formulario-de-campana.ts`). Causa raíz no determinada; se anota.
- Se agregó H1.S1.M6 (traducciones de errores), no prevista.

## Riesgos residuales
- La bandeja trae como mucho 50 por página y solo avanza («Primeras / Siguientes»): el contrato es por cursor hacia adelante.
- El contrato no trae el nombre de la persona que postuló: se muestra su identificador (no se inventa un nombre).

## Decisiones y ambigüedades
- Ubicación en Operación, junto a «Admisiones a grupos», con permiso de sección propio (`ver:postulaciones`), igual que admisiones. Confirmar con Pablo si prefiere otra sección.
- Seguridad (90.6): amenaza = autoaprobación / pisar la decisión de otra persona; control en el servidor (`RutasDeHabilitacion.java`, AP-CU90-04 y AP-CU90-08 con `revisionEsperada`); la UI lo explica (tests de AP-CU90-04 y AP-CU90-08 en verde). Prueba negativa de sección: SOPORTE no alcanza `ver:postulaciones`.
