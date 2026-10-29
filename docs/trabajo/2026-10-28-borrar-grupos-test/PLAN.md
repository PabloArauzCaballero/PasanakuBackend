# Plan — Borrar los grupos de la base de TEST

- Fecha: 2026-10-28 · Repos afectados: ninguno (operación sobre la base de TEST `aportaya-postgres`, base `pasanaku`) · Predecesor: ninguno
- Resultado observable: en TEST no queda ningún grupo; la app y el backoffice muestran la lista de grupos vacía.
- Kill-test: `select count(*) from grupos.grupo` > 0, o alguna fila con `grupo_id` no nulo en las tablas inventariadas.
- Pedido por Pablo el 2026-10-28 («borra la base de datos, los grupos»); base TEST confirmada por él.

## Alcance
- IN (filas de los 3 grupos existentes): `grupos.aceptacion_reglamento` (5), `grupos.solicitud_ingreso` (4),
  `grupos.enlace_grupo` (2), `grupos.cupo` (30), `grupos.participante` (5), `grupos.reglamento_grupo` (3),
  `grupos.historial_estado_grupo` (3), `grupos.configuracion_grupo` (3), `grupos.alta_grupo` (3),
  `tarifas.tarifa_congelada_grupo` (3), `nucleo_financiero.cuenta_billetera` con `grupo_id` (3, sin transacciones), `grupos.grupo` (3).
- OUT: `grupos.evento_dominio` (72) y `grupos.evento_consumido` (8) — bitácora del outbox, ya publicada;
  `grupos.criterio_emparejamiento` (1) — no referencia a un grupo; `grupos.shedlock`; usuarios e identidad.
- Inventario: ninguna FK entrante a `grupos.*`; sin triggers en las tablas IN; 0 asientos, 0 bitácora, 0 transacciones de billetera con `grupo_id`.
- Ambigüedad: «borra los grupos» se interpreta como borrar los grupos y todo lo que depende de ellos, no las cuentas de usuario. Confirmar con Pablo.

## H1 — TEST sin grupos
**CA:** Dado TEST con 3 grupos, cuando se ejecuta el borrado, entonces no queda ninguna fila de las tablas IN y nada fuera de alcance cambió.
**DoD:** conteos antes/después pegados en `evidencia/`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Backup de las tablas IN | El dump existe en el VPS y lista las 12 tablas | `pg_dump --data-only` + `grep -c COPY` = 12 | HECHO (12) |
| H1.S1.M2 | Borrado en una transacción | Conteos IN = 0; conteos OUT iguales que antes | script con conteos antes/después | HECHO (evidencia/intento-2-commit.txt) |
