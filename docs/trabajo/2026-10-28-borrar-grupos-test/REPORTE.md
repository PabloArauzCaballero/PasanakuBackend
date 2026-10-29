# Reporte — Borrar los grupos de la base de TEST

> **AVANCE: 2 / 2 — 100 %.**

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama(s): ninguna (operación de datos en TEST)
- Peldaño de evidencia alcanzado: `VERIFIED` sobre la base (conteos reales antes/después). No se miró la app ni el backoffice.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Backup de las 12 tablas en `/opt/aportaya/backups/grupos-antes-de-borrar-20261028.sql` (modo 600, 36201 bytes) | `pg_dump --data-only -t … ; grep -c '^COPY'` | PASS: 12 |
| H1.S1.M2 | TEST sin grupos: 0 en `grupos.grupo` y en todas sus dependientes | `psql -f evidencia/borrar.sql` | PASS: [intento-2-commit.txt](evidencia/intento-2-commit.txt) |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
DELETE 5 (aceptacion_reglamento) · 4 (solicitud_ingreso) · 2 (enlace_grupo) · 30 (cupo) · 5 (participante)
DELETE 3 (reglamento_grupo) · 3 (historial_estado_grupo) · 3 (configuracion_grupo) · 3 (tarifa_congelada_grupo)
DELETE 3 (cuenta_billetera con grupo_id) · 3 (grupo) · COMMIT
DESPUES: todas las tablas IN = 0; alta_grupo 3; cuenta_billetera total 17→14; evento_dominio 72; criterio_emparejamiento 1; identidad.usuario 7
```
- [intento-1-revertido.txt](evidencia/intento-1-revertido.txt): el primer intento falló en `alta_grupo` (`R-AUD-01: alta_grupo es append-only`) y se revirtió entero. Después del fallo se comprobó: seguían 3 grupos, 30 cupos y 5 participantes.

## No cubierto
- No se abrió la app ni el backoffice para ver la lista vacía.
- Los 72 eventos de `grupos.evento_dominio` siguen mencionando los grupos borrados. Si algún consumidor reprocesara el outbox, recibiría eventos de grupos que ya no existen.

## Desvíos del plan
- El inventario del plan decía «ninguna FK entrante» y «sin triggers». Era falso: la consulta filtraba por `regclass::text`, que omite el esquema. Había FKs (de ahí el orden de borrado) y un trigger append-only en `alta_grupo`.
- `grupos.alta_grupo` pasó de IN a OUT por decisión de Pablo: el trigger de auditoría no se desactivó.

## Riesgos residuales
- Quedan 3 filas en `grupos.alta_grupo` con `grupo_id` de grupos inexistentes. Esa columna no tiene FK, así que no rompe ninguna restricción.

## Decisiones y ambigüedades
- «Borrar los grupos» se tomó como los grupos y todo lo que depende de ellos, sin tocar usuarios. Confirmarlo con Pablo.
- Respetar el append-only de `alta_grupo`: lo decidió Pablo el 2026-10-28.
