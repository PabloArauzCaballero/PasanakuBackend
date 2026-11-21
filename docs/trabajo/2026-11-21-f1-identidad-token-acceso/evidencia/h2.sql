\set ON_ERROR_STOP 1
-- F1.S3.M1 · verificacion SQL contra la base local. Datos SINTETICOS; todo se revierte (ROLLBACK).
BEGIN;
SET search_path = identidad, public;
INSERT INTO usuario (id, codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('f1000000-0000-4000-8000-000000000001','SINT-F1-SUS','Sintetico','Suspendido','+59170000901','1990-01-01','SUSPENDIDO','NINGUNO','es','America/La_Paz', now()),
       ('f1000000-0000-4000-8000-000000000002','SINT-F1-INT','Sintetico','Interno','+59170000902','1990-01-01','ACTIVO','NINGUNO','es','America/La_Paz', now()),
       ('f1000000-0000-4000-8000-0000000000ee','SINT-F1-OPE','Sintetico','Operador','+59170000903','1990-01-01','ACTIVO','NINGUNO','es','America/La_Paz', now());
INSERT INTO dispositivo (id, usuario_id, huella, plataforma, modelo, version_app, ultimo_uso_en)
VALUES ('f1000000-0000-4000-8000-0000000000d1','f1000000-0000-4000-8000-000000000001','huella-sintetica','WEB','navegador','1.0', now());
INSERT INTO sesion (id, usuario_id, dispositivo_id, ip_origen, iniciada_en, expira_en, ultima_actividad_en)
VALUES ('f1000000-0000-4000-8000-0000000000a1','f1000000-0000-4000-8000-000000000001','f1000000-0000-4000-8000-0000000000d1','127.0.0.1', now(), now()+interval '8 hours', now());
INSERT INTO bloqueo_cuenta (usuario_id, motivo, bloqueada_en, desbloquea_en)
VALUES ('f1000000-0000-4000-8000-000000000002','INTENTOS_FALLIDOS', now(), now()+interval '30 minutes');

-- 1) RenovarSesion corre con el contexto del sistema (ContextoSesion.deSistema).
SET ROLE svc_identidad;
SELECT set_config('app.usuario_id','00000000-0000-4000-8000-000000000000',true), set_config('app.rol','sistema',true), set_config('app.traza','00000000-0000-4000-8000-0000000000ff',true) \gset
\echo '--- estadoDe (lo que lee PuedeAutenticarse) del suspendido'
SELECT estado FROM identidad.usuario WHERE id = 'f1000000-0000-4000-8000-000000000001';
\echo '--- revocacion de la sesion en RenovarSesion (misma sentencia que el codigo)'
UPDATE identidad.sesion
   SET revocada_en = now()::timestamptz, motivo_revocacion = 'CUENTA_NO_HABILITADA'
 WHERE id = 'f1000000-0000-4000-8000-0000000000a1' AND revocada_en IS NULL;
SELECT id, revocada_en IS NOT NULL AS revocada, motivo_revocacion FROM identidad.sesion WHERE id='f1000000-0000-4000-8000-0000000000a1';
\echo '--- segunda corrida: idempotente (0 filas)'
UPDATE identidad.sesion SET revocada_en = now(), motivo_revocacion = 'CUENTA_NO_HABILITADA'
 WHERE id = 'f1000000-0000-4000-8000-0000000000a1' AND revocada_en IS NULL;

-- 2) Desbloquear un interno: contexto de un operador de backoffice.
SELECT set_config('app.usuario_id','f1000000-0000-4000-8000-0000000000ee',true), set_config('app.rol','BACKOFFICE',true) \gset
\echo '--- tieneBloqueoVigente ANTES (true esperado)'
SELECT EXISTS (SELECT 1 FROM identidad.bloqueo_cuenta WHERE usuario_id='f1000000-0000-4000-8000-000000000002' AND liberada_en IS NULL AND (desbloquea_en IS NULL OR desbloquea_en > now())) AS bloqueo_vigente;
UPDATE identidad.bloqueo_cuenta SET liberada_en = now(), liberada_por = 'f1000000-0000-4000-8000-0000000000ee'
 WHERE usuario_id = 'f1000000-0000-4000-8000-000000000002' AND liberada_en IS NULL;
\echo '--- tieneBloqueoVigente DESPUES (false esperado)'
SELECT EXISTS (SELECT 1 FROM identidad.bloqueo_cuenta WHERE usuario_id='f1000000-0000-4000-8000-000000000002' AND liberada_en IS NULL AND (desbloquea_en IS NULL OR desbloquea_en > now())) AS bloqueo_vigente;
RESET ROLE;
ROLLBACK;
