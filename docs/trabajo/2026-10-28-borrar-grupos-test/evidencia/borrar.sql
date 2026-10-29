\set ON_ERROR_STOP on
\pset format unaligned
\pset fieldsep ' | '
create temp view conteo as select * from (values
 ('grupos.grupo',(select count(*) from grupos.grupo)),('grupos.participante',(select count(*) from grupos.participante)),
 ('grupos.cupo',(select count(*) from grupos.cupo)),('grupos.aceptacion_reglamento',(select count(*) from grupos.aceptacion_reglamento)),
 ('grupos.solicitud_ingreso',(select count(*) from grupos.solicitud_ingreso)),('grupos.enlace_grupo',(select count(*) from grupos.enlace_grupo)),
 ('grupos.reglamento_grupo',(select count(*) from grupos.reglamento_grupo)),('grupos.historial_estado_grupo',(select count(*) from grupos.historial_estado_grupo)),
 ('grupos.configuracion_grupo',(select count(*) from grupos.configuracion_grupo)),('OUT grupos.alta_grupo (append-only)',(select count(*) from grupos.alta_grupo)),
 ('tarifas.tarifa_congelada_grupo',(select count(*) from tarifas.tarifa_congelada_grupo)),
 ('nucleo_financiero.cuenta_billetera(grupo)',(select count(*) from nucleo_financiero.cuenta_billetera where grupo_id is not null)),
 ('OUT nucleo_financiero.cuenta_billetera(total)',(select count(*) from nucleo_financiero.cuenta_billetera)),
 ('OUT grupos.evento_dominio',(select count(*) from grupos.evento_dominio)),('OUT grupos.criterio_emparejamiento',(select count(*) from grupos.criterio_emparejamiento)),
 ('OUT identidad.usuario',(select count(*) from identidad.usuario))) v(tabla,filas);
\echo == ANTES
select * from conteo;
begin;
delete from grupos.aceptacion_reglamento;
delete from grupos.solicitud_ingreso;
delete from grupos.enlace_grupo;
delete from grupos.cupo;
delete from grupos.participante;
delete from grupos.reglamento_grupo;
delete from grupos.historial_estado_grupo;
delete from grupos.configuracion_grupo;
delete from tarifas.tarifa_congelada_grupo;
delete from nucleo_financiero.cuenta_billetera where grupo_id is not null;
delete from grupos.grupo;
commit;
\echo == DESPUES
select * from conteo;
