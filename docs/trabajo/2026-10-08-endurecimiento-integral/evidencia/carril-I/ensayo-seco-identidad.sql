-- ENSAYO EN SECO (identidad): la lectura de VerificarTitularidad con el ROL REAL svc_identidad y un contexto de SERVICIO. Solo lectura.
BEGIN;
SET LOCAL ROLE svc_identidad;
SELECT set_config('app.rol', 'SERVICIO_ENTREGAS', true), set_config('app.usuario_id', '00000000-0000-0000-0000-0000000000aa', true) \gset
\set u '63c73dab-7ab5-45e3-8d89-626c09cfcd61'
SELECT 'ANTES (contexto del servicio, politica de fila): filas de usuario visibles', count(*) FROM identidad.usuario WHERE id = :'u'::uuid;
SELECT 'ANTES: documentos visibles', count(*) FROM identidad.documento_identidad WHERE usuario_id = :'u'::uuid;
SELECT set_config('app.rol', 'sistema', true) \gset
SELECT 'DESPUES (lectura de sistema acotada): filas de usuario visibles', count(*) FROM identidad.usuario WHERE id = :'u'::uuid;
SELECT 'DESPUES: documentos visibles', count(*) FROM identidad.documento_identidad WHERE usuario_id = :'u'::uuid;
ROLLBACK;
