# H13.S1.M5 · Alarmas y procedimientos

CA del plan: «Dada brecha de caja, atraso, cola detenida o conciliación vencida, cuando ocurre, entonces alerta al
responsable y activa procedimiento» (E-H13-05: inyección de incidente y registro de respuesta).

## Qué se construyó

| Pieza | Archivo | Rol |
|---|---|---|
| Reglas (datos) | `despliegue/alertas/reglas.yml` | 4 alarmas: detector SQL de **solo lectura**, severidad, responsable y escalamiento **por rol del catálogo sembrado**, evento de aviso, procedimiento. Umbrales **sintéticos** (`apto_produccion: false`) |
| Evaluador | `despliegue/alertas/evaluar_alarmas.py` | Corre los detectores, junta evidencia (ids acortados, sin datos de personas), resuelve al responsable por rol en `identidad.asignacion_rol` y escala si nadie lo tiene, arma el cuerpo `POST /notificaciones`, y deja constancia (JSONL): `alarma_disparada` al abrirse, `alarma_resuelta` con duración al cerrarse; una abierta no se repite. Códigos de salida: 0 / 3 alarmas / 4 **alarma sin destinatario** / 2 error |
| Procedimientos | `docs/operacion/alarmas/ALR-01…04-*.md` | Síntomas, diagnóstico, acciones seguras (qué NO hacer incluido), recuperación, validación, escalamiento |
| Contrato (verificable) | `plataforma/comun-pruebas/.../seguridad/AlarmasContratoTest` (7) | 4 alarmas presentes; umbrales sintéticos no se declaran aptos; severidad y roles del catálogo (responsable ≠ escalamiento); procedimiento existe y tiene las 6 secciones; detectores solo `SELECT`, con parámetros declarados y **tablas que existen en `sql/`**; la cola cubre el outbox de los 15 servicios; el cuerpo del aviso coincide con `EntradaNotificacion` |
| Doble del lado receptor | `servicios/notificaciones/.../web/AlarmasOperativasWebTest` (4) | Las cuatro alarmas entran por `POST /notificaciones` con 202 y llegan intactas al caso de uso; límites; inválidos (sin destinatario, campo de más, `datos` no objeto); **un participante no puede fabricar una alarma** (403 salvo `SOPORTE`, 401 sin sesión) |
| Inyección de incidentes | `despliegue/ensayos/alarmas/ensayo_alarmas.sh` | PostgreSQL desechable (5439), esquema real: ver abajo |

## Qué demuestra el ensayo de inyección (salida literal: `alarmas-ensayo.log`, `alarmas-registro.jsonl`)

0. Con todo en orden **no suena nada** (rc 0).
1. Se inyectan los cuatro incidentes (cuenta de custodia con 900 contra 1 000 de dinero electrónico; una obligación con el fin de
   gracia 15 días atrás; un evento pendiente hace 1 h y otro `TOMADO` por un relevo muerto; una segunda cuenta sin conciliar) y
   **suenan las cuatro**, cada una con severidad, conteo, evidencia, responsable por rol y procedimiento.
2. El responsable se resuelve **por rol**: `TESORERIA` (ALR-01, ALR-04), `SOPORTE` (ALR-02); ALR-03 apunta a `ADMIN_PLATAFORMA`, que el ensayo deja **sin
   persona a propósito**, así que **escala** a `RESPONSABLE_SEGURIDAD`.
3. El cuerpo de cada aviso se valida contra `EntradaNotificacion` de `notificaciones.yaml` (tres claves exactas, `destinatarioId` UUID, `datos` objeto de cadenas).
4. Una alarma abierta **no se duplica** en el registro (se informa como vigente).
5. Se **responde** según el procedimiento (reponer fondos y re-conciliar; pagar; publicar la cola; conciliar la segunda cuenta) y las
   cuatro pasan a `alarma_resuelta` con su duración.
6. Con todos los roles revocados la alarma **falla en voz alta** (rc 4): sin nadie a quien avisar es un incidente en sí mismo.

## Lo que NO está (y a quién consultarle)

- **Los umbrales son sintéticos.** Cuántos días de atraso, minutos de cola o horas sin conciliar son tolerables es decisión de
  Operaciones y Tesorería (`apto_produccion: false` hasta que la fuente exista).
- **Quién es el responsable de cada alarma** (el rol) es una propuesta: confirmarla con Dirección de Operaciones.
- **No hay planificador.** Nada ejecuta `evaluar_alarmas.py` cada N minutos en el despliegue (no hay Prometheus/Alertmanager en el repo, ni job en
  los servicios): hoy es una herramienta de operación. Programarla (cron del host, job de Coolify) o llevarla a métricas es trabajo de infraestructura.
- **La entrega real del aviso** depende de que `notificaciones` tenga sembrados los eventos `OPERACION_BRECHA_DE_CAJA`, `OPERACION_ATRASO_DE_APORTES`,
  `OPERACION_COLA_DETENIDA` y `OPERACION_CONCILIACION_VENCIDA` en `evento_notificable` y sus plantillas
  (`seeders/minimos/15-eventos-y-plantillas.json`, fuera de este carril): hoy no existen. El evaluador **no llama** al servicio; arma el cuerpo
  y la prueba del lado receptor es un doble.
- El detector de brecha usa la **última** conciliación por cuenta y no descuenta `saldo_en_transito`; el de atraso no distingue grupos con
  política de mora distinta; el de cola mira los outbox desde un rol con lectura de los 15 esquemas, que ningún servicio tiene (invariante 11):
  tiene que correr con un rol de operación de solo lectura, no con el de un servicio.
