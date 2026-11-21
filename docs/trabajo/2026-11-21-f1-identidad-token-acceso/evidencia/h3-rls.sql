\set ON_ERROR_STOP 1
-- F1.S3.M2 · RLS de factor_mfa para el TOTP. Datos SINTETICOS; ROLLBACK al final.
BEGIN;
SET search_path = identidad, public;
INSERT INTO usuario (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('f1000000-0000-4000-8000-000000000011','SINT-F1-TOT','Sintetico','Totp','+59170000911','1990-01-01','ACTIVO','NINGUNO','es','America/La_Paz', now());
SET ROLE svc_identidad;
\echo '--- titular: inicia el enrolamiento (fila inactiva, secreto cifrado)'
SELECT set_config('app.usuario_id','f1000000-0000-4000-8000-000000000011',true), set_config('app.rol','PARTICIPANTE',true), set_config('app.traza','00000000-0000-4000-8000-0000000000ff',true) \gset
INSERT INTO identidad.factor_mfa (id, usuario_id, tipo, secreto_cifrado, version_llave, activo, es_principal)
VALUES ('f1000000-0000-4000-8000-0000000000f1','f1000000-0000-4000-8000-000000000011','TOTP','enc:v1:sintetico',1,false,false);
\echo '--- titular: confirma'
UPDATE identidad.factor_mfa SET activo = true, es_principal = true, confirmado_en = now()
 WHERE id = 'f1000000-0000-4000-8000-0000000000f1' AND usuario_id = 'f1000000-0000-4000-8000-000000000011' AND confirmado_en IS NULL;
\echo '--- sistema (ingreso): lee el factor activo'
SELECT set_config('app.usuario_id','00000000-0000-4000-8000-000000000000',true), set_config('app.rol','sistema',true) \gset
SELECT tipo, activo, version_llave FROM identidad.factor_mfa WHERE usuario_id='f1000000-0000-4000-8000-000000000011' AND tipo='TOTP' AND activo AND confirmado_en IS NOT NULL;
\echo '--- sistema: marca el paso usado (anti-reuso); 1 fila'
UPDATE identidad.factor_mfa SET ultimo_uso_en = to_timestamp(1800000000)
 WHERE id = 'f1000000-0000-4000-8000-0000000000f1' AND (ultimo_uso_en IS NULL OR ultimo_uso_en < to_timestamp(1800000000));
\echo '--- el MISMO paso otra vez: 0 filas (codigo reutilizado)'
UPDATE identidad.factor_mfa SET ultimo_uso_en = to_timestamp(1800000000)
 WHERE id = 'f1000000-0000-4000-8000-0000000000f1' AND (ultimo_uso_en IS NULL OR ultimo_uso_en < to_timestamp(1800000000));
\echo '--- un paso ANTERIOR: 0 filas'
UPDATE identidad.factor_mfa SET ultimo_uso_en = to_timestamp(1799999970)
 WHERE id = 'f1000000-0000-4000-8000-0000000000f1' AND (ultimo_uso_en IS NULL OR ultimo_uso_en < to_timestamp(1799999970));
\echo '--- el paso SIGUIENTE: 1 fila'
UPDATE identidad.factor_mfa SET ultimo_uso_en = to_timestamp(1800000030)
 WHERE id = 'f1000000-0000-4000-8000-0000000000f1' AND (ultimo_uso_en IS NULL OR ultimo_uso_en < to_timestamp(1800000030));
\echo '--- otro usuario (participante) no ve el factor ajeno: 0 filas'
SELECT set_config('app.usuario_id','f1000000-0000-4000-8000-0000000000ee',true), set_config('app.rol','PARTICIPANTE',true) \gset
SELECT count(*) FROM identidad.factor_mfa WHERE usuario_id='f1000000-0000-4000-8000-000000000011';
RESET ROLE;
ROLLBACK;
