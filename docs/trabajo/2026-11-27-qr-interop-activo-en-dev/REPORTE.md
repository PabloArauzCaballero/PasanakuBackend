> **AVANCE: 3 / 5 — 60,0 %.**

# Reporte — QR_INTEROP activo solo en la semilla de dev/TEST

- Fecha: 2026-11-27 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/qr-interop-activo-en-dev` (desde `origin/dev`)
- Peldaño de evidencia alcanzado (regla 30): **TESTED** para el SQL de la semilla (PostgreSQL 16 real, mismos pasos que los pasos 8/9/10 de la CI).
  El guion de TEST: solo `WRITTEN` + `bash -n` (no se pudo ejecutar: sin Docker).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | Seeder `seeders/dev/19-…json` + manifiesto + SQL generado (`UPDATE proveedor_pago SET activo = TRUE WHERE codigo = 'QR_INTEROP' AND activo = FALSE`) | `python3 scripts/generar_semillas.py` | sin diff adicional tras generar |
| H1.M2 | Segunda corrida idempotente | ver [evidencia/segunda-corrida.txt](./evidencia/segunda-corrida.txt) | minimos ⇒ `activo=false`; dev ×2 ⇒ `activo=true`; el SQL de la semilla sobre base nueva con minimos: `UPDATE 1` luego `UPDATE 0`; producción rechaza `sembrar_dev.sql` (`app.entorno = produccion`); otros proveedores activos: 0 |
| H1.M4 | REPORTE de #547 a 7/7 | lectura | actualizado, con la evidencia de CI de `b9e4b424` |

## A medias
### H1.M3 — Guion `despliegue/ensayos/proveedor_qr_test.sh`
- Qué anda: sintaxis válida (`bash -n`); su bloque SQL lo escribe el generador y es el mismo que la semilla; guardas de perfil `local` y de entorno, `--estado` y `--retirar` escritos.
- Qué no anda: no se ejecutó (necesita Docker y el servidor TEST).
- Qué falta exactamente: correrlo una vez en TEST (`ssh … 'bash -s' < despliegue/ensayos/proveedor_qr_test.sh`) y pegar la salida de la segunda corrida (`cambios: 0`).
- Dónde quedó: rama `claude/qr-interop-activo-en-dev`, el archivo existe y es ejecutable.

### H1.M5 — spotless, push, PR, CI verde, aviso
- Qué anda: generador y semillas verificados en local.
- Qué no anda / falta: un push, PR contra `dev` fuera de borrador, CI verde y aviso a `@parent`.
- Dónde quedó: rama local con los cambios sin publicar al escribir esto.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Ejecutar el guion en TEST | `BLOQUEADO` (sin acceso al servidor ni Docker) | Pablo, o quien tenga ssh a TEST |
| Regenerar catálogos de operaciones | otro PR (decisión de Pablo) | — |

## Evidencia
[evidencia/segunda-corrida.txt](./evidencia/segunda-corrida.txt).

## No cubierto
- Ejecución real del guion de TEST.
- Que el aviso simulado responda `PROCESADO` en el servidor TEST una vez activado (no hay acceso al servidor).
- Producción: no se tocó `seeders/minimos/14`; se comprobó en local que `minimos` deja `activo=false` y que `sembrar_dev.sql` se rechaza con `app.entorno = produccion`.

## Desvíos del plan
- El seeder y el guion se escribieron antes de que existiera este `PLAN.md` (regla 20): el plan se redactó al cerrar el descubrimiento, antes de la verificación.

## Riesgos residuales
- Si alguien aplica el guion en un entorno con perfil `local` que sirva tráfico real, `QR_INTEROP` quedaría activo: lo mitiga que el proveedor no tiene contrato y la pasarela de aportes es un doble que no corre fuera de `local`/`test`.

## Decisiones y ambigüedades
- Activación solo en dev/TEST: decisión de Pablo (relay 2026-11-27). Producción sigue inactivo.
