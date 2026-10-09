-- Catálogos mínimos — también se aplican en producción
--   psql -d pasanaku -v ON_ERROR_STOP=1 -f sql/60_semillas/sembrar.sql
-- GENERADO desde seeders/ — no editar a mano.

--
-- El search_path va ACA y no se hereda: este archivo corre en su propia sesion
-- de psql y nombra las tablas sin su esquema. Sin esta linea depende de que la
-- base lo traiga puesto (ALTER DATABASE) o de que quien lo invoque lo pase por
-- la conexion, y falla con «relation does not exist» donde no sea asi.
\set ON_ERROR_STOP on
SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public;
BEGIN;

\ir 01-plan-de-cuentas.sql
\ir 02-politicas.sql
\ir 03-limites-operativos.sql
\ir 04-tarifario.sql
\ir 05-impuestos.sql
\ir 06-umbrales-uif.sql
\ir 07-reportes-regulatorios.sql
\ir 08-gobierno-y-licencia.sql
\ir 09-reglas-operativas.sql
\ir 10-roles-y-permisos.sql
\ir 11-contratos-de-adhesion.sql
\ir 12-calendario-habil.sql
\ir 13-politicas-de-token.sql
\ir 14-proveedores-externos.sql
\ir 15-eventos-y-plantillas.sql
\ir 16-reputacion-y-scoring.sql
\ir 17-organizador-y-emparejamiento.sql
\ir 18-sanciones-y-cobranza.sql
\ir 19-reportes-y-retencion.sql
\ir 20-control-interno-y-continuidad.sql
\ir 21-contabilidad-y-publicidad.sql

COMMIT;
