-- GU-17 · operadores con la fecha de nacimiento ficticia pasan a «sin dato».
-- Generado por scripts/generar_ddl.py — no editar a mano.
--
-- No hay migraciones versionadas en este repositorio (docs/operacion/schema-changes.md): esta es la
-- correccion de datos con nombre explicito, idempotente y acotada por el corte de abajo.
-- Solo toca a quien cumple LAS TRES: fecha exactamente 1990-01-01, es personal interno y se
-- registro antes de 2026-11-26 03:17:43+00. Segunda corrida: 0 filas.
--
-- REVERSA (a mano, solo si hubiera que deshacerla; restituye lo que sigue «sin dato»):
--   UPDATE identidad.usuario u SET fecha_nacimiento = DATE '1990-01-01'
--    WHERE u.fecha_nacimiento IS NULL AND u.fecha_registro < TIMESTAMPTZ '2026-11-26 03:17:43+00'
--      AND EXISTS (SELECT 1 FROM identidad.usuario_interno ui WHERE ui.usuario_id = u.id);

-- aplicar.sql corre en una transaccion: sin esto, las filas corregidas dejan eventos diferidos del
-- trigger de R-SEG-18 y el siguiente ALTER TABLE falla («pending trigger events»). Los operadores
-- cumplen la regla, asi que se comprueba ya.
SET CONSTRAINTS ALL IMMEDIATE;
UPDATE identidad.usuario u
   SET fecha_nacimiento = NULL
 WHERE u.fecha_nacimiento = DATE '1990-01-01'
   AND u.fecha_registro < TIMESTAMPTZ '2026-11-26 03:17:43+00'
   AND EXISTS (SELECT 1 FROM identidad.usuario_interno ui WHERE ui.usuario_id = u.id);
SET CONSTRAINTS ALL DEFERRED;
