# Reporte — El autodespliegue de TEST no puede decir «listo» si no cambió de commit

> **AVANCE: 1 / 2 — 50,0 %.**

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/autodespliegue-checkout-20261030` (desde `origin/test`)
- Peldaño: `TESTED` en un clon descartable · el guion del host se instala a mano (`despliegue/host/README.md`), así que en TEST rige recién cuando se instale.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | Checkout con `-f` y verificación `HEAD = REMOTO`; si falla, `FALLA` en el log y `exit 1` (no hay «listo» ni despliegue) | clon descartable: línea vieja vs bloque nuevo extraído del guion | PASS — `evidencia/prueba-checkout.txt` |

## A medias
### H1.M2 — PR e instalación
- Qué anda: el cambio en `despliegue/host/reconstruir-si-cambio.sh`, `bash -n` OK, prueba en verde.
- Qué no anda: el host sigue con la copia vieja hasta que se instale.
- Qué falta exactamente: merge de los PRs a `test` y `dev`; en el host, `install -m 755 despliegue/host/reconstruir-si-cambio.sh /opt/aportaya/bin/` (a mano por diseño, revisión de seguridad 2026-10-28).
- Dónde quedó: rama `claude/autodespliegue-checkout-20261030`, compila (`bash -n`).

## Pendiente
ninguna

## Evidencia
```text
== 1) linea VIEJA con clon sucio
error: Your local changes to the following files would be overwritten by checkout:
exit 1 · HEAD=a9ae333 (REMOTO=556be69)      ← el guion viejo seguía igual desde acá
== 2) bloque NUEVO con clon sucio
siguio · HEAD=556be69 · porcelain=0
== 3) bloque NUEVO con checkout imposible (sha inexistente)
[marca] FALLA el checkout de 0000000; el arbol sigue en 556be69
exit 1
```
TEST (solo lectura, 2026-10-30): `/opt/aportaya/repo` en `9f642eb` con 4 `clientes/dart/*/lib/*.dart` modificados; el log decía «listo en c3fd625» con «encolado … en 9f642eb».

## No cubierto
- Una corrida completa del guion nuevo en el host (necesita instalarlo).
- `shellcheck` no está instalado en esta máquina.

## Desvíos del plan
ninguno

## Riesgos residuales
- `-f` pisa cambios locales en archivos versionados del clon de despliegue. Nadie debe editar a mano en `/opt/aportaya/repo`.
- Que la generación reescriba archivos versionados de `clientes/dart` sigue pasando; ahora no traba el despliegue, pero indica que lo versionado ahí queda atrasado respecto del contrato.

## Decisiones y ambigüedades
- Se eligió `-f` en vez de limpiar solo `clientes/dart`: cubre cualquier archivo generado que se versione, no solo los de hoy. Confirmar con Pablo si prefiere el alcance acotado.
